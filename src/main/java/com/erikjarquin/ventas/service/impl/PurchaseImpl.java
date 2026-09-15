package com.erikjarquin.ventas.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.ventas.exceptions.PurchaseException;
import com.erikjarquin.ventas.mapper.PurchaseMapper;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseDTO;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseItemRequest;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseRequest;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.entity.ProviderEntity;
import com.erikjarquin.ventas.model.entity.PurchaseDetailEntity;
import com.erikjarquin.ventas.model.entity.PurchaseEntity;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.repository.ProviderRepository;
import com.erikjarquin.ventas.repository.PurchaseRepository;
import com.erikjarquin.ventas.service.PurchaseService;

/**
 * Implementación del módulo de compras.
 *
 * <p><b>Crear ({@code create})</b>: valida proveedor e items (400/409), arma
 * cabecera + detalles con subtotales, calcula el total, SUMA el stock de cada
 * producto y guarda su costo real (unitCost) — todo en una transacción.
 *
 * <p><b>Cancelar ({@code cancel})</b>: revierte el stock de cada producto
 * (nunca por debajo de 0) y elimina la compra. El costo se deja intacto: es el
 * último costo vigente, aunque se anule esa compra el inventario restante
 * sigue habiendo costado eso.
 */
@Service
public class PurchaseImpl implements PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProviderRepository providerRepository;
    private final ProductRepository productRepository;

    public PurchaseImpl(
            PurchaseRepository purchaseRepository,
            ProviderRepository providerRepository,
            ProductRepository productRepository){
        this.purchaseRepository = purchaseRepository;
        this.providerRepository = providerRepository;
        this.productRepository = productRepository;
    }

    //Registrar una compra (@Transactional: cabecera+detalles+stock+cost en 1 tx)
    @Override
    @Transactional
    public PurchaseDTO create(PurchaseRequest request){
        validateRequest(request);

        ProviderEntity provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new PurchaseException("El proveedor indicado no existe", HttpStatus.CONFLICT));

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseDate(request.getPurchaseDate() != null
                ? request.getPurchaseDate()
                : java.time.LocalDateTime.now());
        purchase.setProvider(provider);

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseDetailEntity> details = new ArrayList<>();

        for(PurchaseItemRequest item : request.getItems()){
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new PurchaseException(
                        "El producto con id " + item.getProductId() + " no existe",
                        HttpStatus.CONFLICT));

            BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;

            //1) Renglón de compra con su subtotal (unitCost se copia del request)
            PurchaseDetailEntity detail = new PurchaseDetailEntity();
            detail.setPurchase(purchase);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitCost(unitCost);
            detail.setSubtotal(unitCost.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, java.math.RoundingMode.HALF_UP));

            total = total.add(detail.getSubtotal());

            //2) Acumular stock y guardar el costo real del producto
            //   (este es el efecto por el que existe el módulo de compras)
            product.setStock(product.getStock() + item.getQuantity());
            product.setCost(unitCost);
            productRepository.save(product);

            details.add(detail);
        }

        purchase.setTotal(total.setScale(2, java.math.RoundingMode.HALF_UP));
        purchase.setDetails(details);

        return PurchaseMapper.toDto(purchaseRepository.save(purchase));
    }

    //Listar todas las compras (EntityGraph precarga proveedor y productos)
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseDTO> getAll(){
        return purchaseRepository.findAllByOrderByPurchaseDateDesc().stream()
                .map(PurchaseMapper::toDto)
                .toList();
    }

    //Compras de un proveedor
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseDTO> getByProvider(Long providerId){
        if(providerId != null){
            providerRepository.findById(providerId)
                    .orElseThrow(() -> new PurchaseException("El proveedor indicado no existe", HttpStatus.NOT_FOUND));
        }

        return purchaseRepository.findByProviderIdOrderByPurchaseDateDesc(providerId).stream()
                .map(PurchaseMapper::toDto)
                .toList();
    }

    //Detalle de una compra
    @Override
    @Transactional(readOnly = true)
    public PurchaseDTO getById(Long id){
        PurchaseEntity entity = purchaseRepository.findDetailedById(id)
                .orElseThrow(() -> new PurchaseException("Compra no encontrada"));
        return PurchaseMapper.toDto(entity);
    }

    //Cancelar compra: revertir stock y eliminar
    @Override
    @Transactional
    public void cancel(Long id){
        PurchaseEntity purchase = purchaseRepository.findDetailedById(id)
                .orElseThrow(() -> new PurchaseException("Compra no encontrada"));

        if(purchase.getDetails() != null){
            for(PurchaseDetailEntity detail : purchase.getDetails()){
                ProductEntity product = productRepository.findById(detail.getProduct().getId())
                        .orElseThrow(() -> new PurchaseException(
                            "El producto asociado a la compra ya no existe; no se puede cancelar",
                            HttpStatus.CONFLICT));

                int nuevoStock = product.getStock() - detail.getQuantity();
                product.setStock(Math.max(nuevoStock, 0));
                productRepository.save(product);
            }
        }

        purchaseRepository.delete(purchase);
    }

    //Validar la petición: proveedor y items obligatorios, cantidades y costos válidos (400)
    private void validateRequest(PurchaseRequest request){
        if(request.getProviderId() == null){
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        if(request.getItems() == null || request.getItems().isEmpty()){
            throw new IllegalArgumentException("La compra debe tener al menos un artículo");
        }
        for(PurchaseItemRequest item : request.getItems()){
            if(item.getProductId() == null){
                throw new IllegalArgumentException("Cada artículo debe indicar un producto");
            }
            if(item.getQuantity() == null || item.getQuantity() <= 0){
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
            if(item.getUnitCost() != null && item.getUnitCost().signum() < 0){
                throw new IllegalArgumentException("El costo unitario no puede ser negativo");
            }
        }
    }
}