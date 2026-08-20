package com.wwweinuo.cloudmall.order.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.order.mapper.OrderItemMapper;
import com.wwweinuo.cloudmall.order.mapper.OrderMapper;
import com.wwweinuo.cloudmall.order.model.CreateOrderRequest;
import com.wwweinuo.cloudmall.order.model.Order;
import com.wwweinuo.cloudmall.order.model.OrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final OrderItemMapper orderItemMapper;

    public OrderService(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        if (request == null || request.userId() == null || request.productId() == null) {
            throw new IllegalArgumentException("用户和商品不能为空");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("订单数量必须大于 0");
        }

        BigDecimal unitPrice = request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        String productName = request.productName() == null ? "待商品服务返回" : request.productName();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(request.userId());
        order.setTotalAmount(subtotal);
        order.setStatus("CREATED");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setDeleted(0);
        save(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(request.productId());
        item.setProductName(productName);
        item.setUnitPrice(unitPrice);
        item.setQuantity(request.quantity());
        item.setSubtotal(subtotal);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        orderItemMapper.insert(item);
        return order;
    }
}
