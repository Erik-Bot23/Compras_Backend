package com.erikjarquin.ventas.mapper;

import com.erikjarquin.ventas.model.dto.Products.ProductDto;
import com.erikjarquin.ventas.model.entity.ProductEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mapper producto → ProductDto (componente de Spring).
 *
 * <p>buildImgUrl antepone {@code app.upload-url} al nombre guardado en BD para
 * devolver una URL COMPLETA consumible por <img [src]> en el frontend Angular.
 * Si no hay imagen o no hay URL configurada, devuelve el valor tal cual.
 */
@Component
public class ProductMapper {

    private final String uploadUrl;

    public ProductMapper(@Value("${app.upload-url:}") String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public ProductDto toDto(ProductEntity entity){
        if(entity==null) return null;

        ProductDto dto = new ProductDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPrice(entity.getPrice());
        dto.setCost(entity.getCost());
        dto.setStock(entity.getStock());
        dto.setImg(buildImgUrl(entity.getImg()));

        if(entity.getCategory() != null){
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }

        dto.setSku(entity.getSku());
        dto.setBarcode(entity.getBarcode());

        return dto;
    }

    private String buildImgUrl(String storedName){
        if(!StringUtils.hasText(storedName) || !StringUtils.hasText(uploadUrl)){
            return storedName;
        }
        return uploadUrl + "/" + storedName;
    }
}