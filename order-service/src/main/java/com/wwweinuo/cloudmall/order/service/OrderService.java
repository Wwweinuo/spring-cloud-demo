package com.wwweinuo.cloudmall.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wwweinuo.cloudmall.order.model.CreateOrderRequest;
import com.wwweinuo.cloudmall.order.model.Order;

public interface OrderService extends IService<Order> {

    Order create(CreateOrderRequest request);
}
