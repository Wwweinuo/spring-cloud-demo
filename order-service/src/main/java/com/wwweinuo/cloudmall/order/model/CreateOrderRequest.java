package com.wwweinuo.cloudmall.order.model;

import java.math.BigDecimal;

/**
 * 创建订单请求。
 *
 * productName 和 unitPrice 是当前未接入 OpenFeign 时的临时快照字段；
 * 后续由订单服务远程查询 product-service 后再填充。
 */
public record CreateOrderRequest(
        Long userId,
        Long productId,
        Integer quantity,
        String productName,
        BigDecimal unitPrice
) {
}
