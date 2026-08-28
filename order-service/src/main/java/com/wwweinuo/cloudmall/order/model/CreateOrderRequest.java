package com.wwweinuo.cloudmall.order.model;

import java.math.BigDecimal;

/**
 * 创建订单请求。
 *
 * productName 和 unitPrice 为兼容旧请求保留的字段；
 * 订单服务接入 OpenFeign 后，以 product-service 返回的商品信息为准。
 */
public record CreateOrderRequest(
        Long userId,
        Long productId,
        Integer quantity,
        String productName,
        BigDecimal unitPrice
) {
}
