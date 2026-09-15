package com.erikjarquin.ventas.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.ventas.exceptions.PaymentException;
import com.erikjarquin.ventas.exceptions.SaleException;
import com.erikjarquin.ventas.mapper.SaleMapper;
import com.erikjarquin.ventas.model.dto.Payment.CardPaymentRequest;
import com.erikjarquin.ventas.model.dto.Payment.CardPaymentResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleDetailHistoryResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleHistoryResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleItemRequest;
import com.erikjarquin.ventas.model.dto.Sale.SaleRequest;
import com.erikjarquin.ventas.model.dto.Sale.SaleResponse;
import com.erikjarquin.ventas.model.entity.CashRegisterEntity;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.entity.SaleDetailEntity;
import com.erikjarquin.ventas.model.entity.SaleEntity;
import com.erikjarquin.ventas.model.enums.PaymentMethod;
import com.erikjarquin.ventas.model.enums.PaymentStatus;
import com.erikjarquin.ventas.repository.CashRegisterRepository;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.repository.SaleRepository;
import com.erikjarquin.ventas.service.PaymentService;
import com.erikjarquin.ventas.service.SaleService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del flujo COMPLETO de una venta.
 *
 * <p>Pasos (todo dentro de una transacción con rollback):
 *  1. Valida la solicitud y que exista una caja abierta.
 *  2. Crea la venta como PENDING (para obtener ID).
 *  3. Calcula el total en memoria validando productos y stock.
 *  4. Procesa el pago: efectivo (valida cambio) o tarjeta (delega en
 *     PaymentService / terminal).
 *  5. Solo si el pago fue exitoso: aprueba la venta y DESCUENTA stock.
 *
 * <p>Si el pago con tarjeta falla, la excepción hace rollback total de la
 * transacción (la venta PENDING y el pago no quedan persistidos).
 */
@Slf4j
@Service
@Transactional
public class SaleImpl implements SaleService {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final CashRegisterRepository cashRepository;
    private final SaleMapper mapper;
    private final PaymentService paymentService;

    public SaleImpl(
        ProductRepository productRepository,
        SaleRepository saleRepository,
        CashRegisterRepository cashRepository,
        SaleMapper mapper,
        PaymentService paymentService
    ){
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.cashRepository = cashRepository;
        this.mapper = mapper;
        this.paymentService=paymentService;
    }

    @Override //
    @Transactional(rollbackFor = Exception.class) //
    public SaleResponse processSale(SaleRequest request){
        log.info("Iniciando proceso de venta. Método de pago: {}", request.getPaymentMethod());

        /*VALIDACIONES INICIALES*/
        validateRequest(request);

        /*VERIFICAR CAJA ABIERTA*/
        CashRegisterEntity cash = getActiveCashRegister();

        /*CREAR VENTA (sin detalles todavía)*/
        SaleEntity sale = createBaseSale(request, cash);
        sale.setPaymentStatus(PaymentStatus.PENDING); //Estado inicial

        //Guardar a venta para obtener un ID
        SaleEntity savedSale = saleRepository.save(sale);
        log.info("Venta creada con ID: {} (estado PENDING)", savedSale.getId());

        /*PROCESAR PRODUCTOS Y CALCULAR TOTAL*/
        ProcessedProducts processed = processProductsInMemory(request.getItems(), sale);
        savedSale.setDetails(processed.getDetails());
        savedSale.setTotal(processed.getTotal());

        //Actualizar la venta con el total calculado
        saleRepository.save(savedSale);

        //Procesar pago según el método y guardar la respuesta
        CardPaymentResponse paymentResponse = null;

        if(request.getPaymentMethod() == PaymentMethod.CASH){
            //Procesar pago en efectivo
            processCashPayment(request, savedSale);
        } else if(request.getPaymentMethod() == PaymentMethod.DEBIT || request.getPaymentMethod() == PaymentMethod.CREDIT){
            //Procesar pago con tarjeta y guardar respuesta
            paymentResponse = processCardPaymentWithResponse(request, savedSale);
        } else {
            throw new SaleException("Método de pago no soportado: " + request.getPaymentMethod());
        }

        /*Solo si llegamos aquí, el pago fue exitoso*/
        // a) Guardar la venta
        savedSale.setPaymentStatus(PaymentStatus.APPROVED);

        // b) Descontar de stock
        for(SaleDetailEntity detail : savedSale.getDetails()){
            ProductEntity product = detail.getProduct();
            product.setStock(product.getStock() - detail.getQuantity());
            productRepository.save(product);
        }

        SaleEntity finalSale = saleRepository.save(savedSale);

        log.info("Venta completada exitosamente. ID: {}, Total: ${}", savedSale.getId(), savedSale.getTotal());

        /* RETORNAR RESPUESTA CON DATOS DE TARJETA SI APLICA */
        SaleResponse response = mapper.toResponse(finalSale);
        
        // Si fue pago con tarjeta, incluir los datos de la respuesta del pago
        if(paymentResponse != null){
            response.setCardPaymentResponse(paymentResponse);
            response.setAuthorizationCode(paymentResponse.getAuthorizationCode());
            response.setLastFourDigits(paymentResponse.getLastFourDigits());
        }
        
        return response;
    }

    // ====== Métodos privados =====
    //Validar el pago
    private void validateRequest(SaleRequest request){
        if(request == null){
            throw new SaleException("La solicitud de venta es obligatoria");        
        }

        if(request.getPaymentMethod() == null){
            throw new SaleException("Debe seleccionar un método de pago");
        }

        if(request.getItems() == null || request.getItems().isEmpty()){
            throw new SaleException("La venta no tiene productos");
        }
    }

    //Ver caja activa
    private CashRegisterEntity getActiveCashRegister(){
        return cashRepository.findByActiveTrue().orElseThrow(() -> new SaleException("No existe una caja abierta"));
    }

    //Crear la base de la venta
    private SaleEntity createBaseSale(SaleRequest request, CashRegisterEntity cash){
        SaleEntity sale = new SaleEntity();
        sale.setSaleDate(LocalDateTime.now());
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setCashRegister(cash);
        sale.setPaymentStatus(PaymentStatus.PENDING);
        return sale;
    }

    //Procesar pago con tarjeta
    private CardPaymentResponse processCardPaymentWithResponse(SaleRequest request, SaleEntity sale){
        log.info("Procesando pago con tarjeta para venta ID: {}", sale.getId());

        if(request.getCardPayment() == null){
            throw new SaleException("Debe proporcionar datos de la tarjeta");
        }

        try{
            // Preparar request para PaymentService
            CardPaymentRequest cardRequest = request.getCardPayment();
            cardRequest.setSaleId(sale.getId()); // Ya tiene ID porque guardamos antes
            cardRequest.setPaymentMethod(request.getPaymentMethod());

            // Procesar el pago con tarjeta
            CardPaymentResponse paymentResponse = paymentService.processCardPayment(cardRequest);

            // Verificar si fue aprobado
            if(paymentResponse.getStatus() == PaymentStatus.APPROVED){
                log.info("Pago con tarjeta aprobado. Código: {}", paymentResponse.getAuthorizationCode());
                return paymentResponse;
            } else {
                // Si no fue aprobado, marcar como rechazado y lanzar excepción
                sale.setPaymentStatus(PaymentStatus.REJECTED);
                saleRepository.save(sale);
                throw new PaymentException("Pago con tarjeta rechazado: " + paymentResponse.getMessage());
            }

        } catch (PaymentException e){
            log.error("Error en pago con tarjeta: {}", e.getMessage());
            // Marcar la venta como rechazada
            sale.setPaymentStatus(PaymentStatus.REJECTED);
            saleRepository.save(sale);
            throw e; // Relanzar para rollback
        } catch (Exception e){
            log.error("Error inesperado en pago con tarjeta: {}", e.getMessage());
            sale.setPaymentStatus(PaymentStatus.REJECTED);
            saleRepository.save(sale);
            throw new SaleException("Error al procesar pago con tarjeta: " + e.getMessage(), e);
        }
    }

    //No se guarda la venta en BD solo se calcula
    private ProcessedProducts processProductsInMemory(List<SaleItemRequest> items, SaleEntity sale){
        List<SaleDetailEntity> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for(SaleItemRequest item : items){
            validateItem(item);
            ProductEntity product = findProduct(item.getProductId());
            validateStock(product, item.getQuantity());

            //Solo calcular, no guardar
            BigDecimal subtotal = calculateSubtotal(product, item.getQuantity());
            total = total.add(subtotal);

            SaleDetailEntity detail = createDetail(sale, product, item, subtotal);
            details.add(detail);
        }

        return new ProcessedProducts(details, total);
    }

    //Se validan los productos
    private void validateItem(SaleItemRequest item){
        if(item == null){
            throw new SaleException("La venta contiene un producto inválido");
        }

        if(item.getProductId() == null){
            throw new SaleException("El producto es obligatorio");
        }

        if(item.getQuantity() == null || item.getQuantity() <= 0){
            throw new SaleException("La cantidad del producto debe ser mayor a 0");
        }
    }

    //Encontrar producto
    private ProductEntity findProduct(Long productId){
        return productRepository.findById(productId).orElseThrow(() -> new SaleException("Producto no encontrado: " + productId));
    }

    //Validar stock en el inventario
    private void validateStock(ProductEntity product, Integer quantity){
        if(product.getStock() < quantity){
            throw new SaleException("Stock insuficiente para el producto: " + product.getName() + 
            ". Disponible: " + product.getStock() + ", Solicitado: " + quantity);
        }
    }

    //Calcular el subtotal
    private BigDecimal calculateSubtotal(ProductEntity product, Integer quantity){
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    //Crear el detalle de la venta 
    private SaleDetailEntity createDetail(SaleEntity sale, ProductEntity product, SaleItemRequest item, BigDecimal subtotal){
        SaleDetailEntity detail = new SaleDetailEntity();
        detail.setSale(sale);
        detail.setProduct(product);
        detail.setQuantity(item.getQuantity());
        detail.setUnitPrice(product.getPrice());
        detail.setSubTotal(subtotal);
        return detail;
    }

    //Procesar el pago en efectivo
    private void processCashPayment(SaleRequest request, SaleEntity sale){
        log.info("Procesando pago con efectivo para venta ID: {}", sale.getId());

        if(request.getCashReceived() == null){
            throw new SaleException("Debe indicar el efectivo recibido");
        }

        if(request.getCashReceived().compareTo(BigDecimal.ZERO) <= 0){
            throw new SaleException("El efectivo recibido debe ser mayor a cero");
        }

        if(request.getCashReceived().compareTo(sale.getTotal()) <0){
            throw new SaleException("Pago insuficiente. Total: $" + sale.getTotal() + ", Recibido: $" + request.getCashReceived());
        }

        BigDecimal change = request.getCashReceived().subtract(sale.getTotal());
        sale.setCashReceived(request.getCashReceived());
        sale.setChangeAmount(change);
        sale.setPaymentStatus(PaymentStatus.APPROVED);

        log.info("Pago en efectivo aprobado. Cambio: {}", change);
    }

    //===== CLASE AUXILIAR =====
    private static class ProcessedProducts {
        private final List<SaleDetailEntity> details;
        private final BigDecimal total;

        public ProcessedProducts(List<SaleDetailEntity> details, BigDecimal total){
            this.details=details;
            this.total=total;
        }

        public List<SaleDetailEntity> getDetails(){
            return details;
        }

        public BigDecimal getTotal(){
            return total;
        }
    }

    //===== MÉTODOS DE CONSULTA ======
    //Consultar las ventas
    @Override
    @Transactional(readOnly = true)
    public List<SaleHistoryResponse> getSales(){
        return saleRepository.findAll().stream().map(mapper::toHistoryResponse).toList();
    }

    //Consultar la venta por ID
    @Override
    public SaleDetailHistoryResponse getSaleById(Long saleId){
        SaleEntity sale = saleRepository.findById(saleId).orElseThrow(
            () -> new SaleException("Venta no encontrada con ID: " + saleId));

        return mapper.toDetailResponse(sale);
    }
}
