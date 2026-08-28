package com.wwweinuo.cloudmall.api.product.dto;

import java.math.BigDecimal;

/**
 * product-service 对外返回的商品数据，不暴露数据库实体。
 */
public record ProductDTO(
        Long id,
        String name,
        BigDecimal price,
        String status
) {
}
