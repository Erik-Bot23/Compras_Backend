package com.erikjarquin.ventas.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.dto.Sale.SaleDetailHistoryResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleDetailResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleHistoryResponse;
import com.erikjarquin.ventas.model.dto.Sale.SaleResponse;
import com.erikjarquin.ventas.model.entity.SaleDetailEntity;
import com.erikjarquin.ventas.model.entity.SaleEntity;
import com.erikjarquin.ventas.model.enums.PaymentMethod;

/**
 * Mapper venta → respuestas de venta/historial/detalle (componente de Spring).
 *
 * <p>En pagos con tarjeta expone SOLO los últimos 4 dígitos y el código de
 * autorización (nunca el número completo). toDetailResponse arma el detalle
 * con historial, manejando details nulo como lista vacía.
 */
@Component
public class SaleMapper {
    public SaleResponse toResponse(SaleEntity sale){
        SaleResponse response = new SaleResponse();

        response.setSaleId(sale.getId());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setCashReceived(sale.getCashReceived());
        response.setChangeAmount(sale.getChangeAmount());
        response.setPaymentStatus(sale.getPaymentStatus());
        
        //Si el pago fue con tarjeta, obtener información del payment
        if(sale.getPaymentMethod() == PaymentMethod.DEBIT || 
            sale.getPaymentMethod() == PaymentMethod.CREDIT){
                //Opción 1: Si tienes relación OneToOne en SaleEntity
                if(sale.getPayment() != null) {
                    response.setLastFourDigits(sale.getPayment().getLastFourDigits());
                    response.setAuthorizationCode(sale.getPayment().getAuthorizationCode());
                    response.setErrorMessage(sale.getPayment().getErrorMessage());
                }
        }

        return response;
    }

    public SaleHistoryResponse toHistoryResponse(SaleEntity sale){
        SaleHistoryResponse response = new SaleHistoryResponse();

        response.setId(sale.getId());
        response.setSaleDate(sale.getSaleDate());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setCashReceived(sale.getCashReceived());
        response.setChangeAmount(sale.getChangeAmount());
        response.setPaymentStatus(sale.getPaymentStatus());

        return response;
    }

    public SaleDetailHistoryResponse toDetailResponse(SaleEntity sale){
        SaleDetailHistoryResponse response = new SaleDetailHistoryResponse();

        response.setSaleId(sale.getId());
        response.setSaleDate(sale.getSaleDate());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setPaymentStatus(sale.getPaymentStatus());

        List<SaleDetailResponse> items = sale.getDetails() == null ? List.of() : sale.getDetails().stream().map(this::toDetailItem).toList();
        response.setItems(items);

        return response;
        
    }

    private SaleDetailResponse toDetailItem(SaleDetailEntity detail){
        SaleDetailResponse item = new SaleDetailResponse();

        item.setProduct(detail.getProduct().getName());
        item.setQuantity(detail.getQuantity());
        item.setUnitPrice(detail.getUnitPrice());
        item.setSubtotal(detail.getSubTotal());

        return item;
    }
}
