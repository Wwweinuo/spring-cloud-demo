package com.wwweinuo.cloudmall.order.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.common.response.Result;
import com.wwweinuo.cloudmall.api.product.ProductClient;
import com.wwweinuo.cloudmall.api.product.dto.ProductDTO;
import com.wwweinuo.cloudmall.api.user.UserClient;
import com.wwweinuo.cloudmall.api.user.dto.UserDTO;
import com.wwweinuo.cloudmall.order.mapper.OrderItemMapper;
import com.wwweinuo.cloudmall.order.mapper.OrderMapper;
import com.wwweinuo.cloudmall.order.model.CreateOrderRequest;
import com.wwweinuo.cloudmall.order.model.Order;
import com.wwweinuo.cloudmall.order.model.OrderItem;
import com.wwweinuo.cloudmall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;
    private final UserClient userClient;
    private final ProductClient productClient;

    @Override
    @Transactional
    public Order create(CreateOrderRequest request) {
        if (request == null || request.userId() == null || request.productId() == null) {
            throw new IllegalArgumentException("用户和商品不能为空");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("订单数量必须大于 0");
        }

        UserDTO user = getUser(request.userId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + request.userId());
        }
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new IllegalArgumentException("用户状态不可创建订单: " + request.userId());
        }

        ProductDTO product = getProduct(request.productId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + request.productId());
        }
        if (!"ON_SALE".equalsIgnoreCase(product.status())) {
            throw new IllegalArgumentException("商品当前未上架: " + request.productId());
        }

        BigDecimal unitPrice = product.price() == null ? BigDecimal.ZERO : product.price();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
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
        item.setProductName(product.name());
        item.setUnitPrice(unitPrice);
        item.setQuantity(request.quantity());
        item.setSubtotal(subtotal);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        orderItemMapper.insert(item);
        return order;
    }

    private UserDTO getUser(Long userId) {
        Result<UserDTO> result = userClient.getById(userId);
        return result == null ? null : result.data();
    }

    private ProductDTO getProduct(Long productId) {
        Result<ProductDTO> result = productClient.getById(productId);
        return result == null ? null : result.data();
    }
}
