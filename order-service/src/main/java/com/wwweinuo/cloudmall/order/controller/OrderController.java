package com.wwweinuo.cloudmall.order.controller;

import com.wwweinuo.cloudmall.common.response.Result;
import com.wwweinuo.cloudmall.order.model.CreateOrderRequest;
import com.wwweinuo.cloudmall.order.model.Order;
import com.wwweinuo.cloudmall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Order> create(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.failure("订单不存在: " + id);
        }
        return Result.success(order);
    }
}
