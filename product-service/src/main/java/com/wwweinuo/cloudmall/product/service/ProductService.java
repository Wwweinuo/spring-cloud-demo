package com.wwweinuo.cloudmall.product.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.product.mapper.ProductMapper;
import com.wwweinuo.cloudmall.product.model.Product;
import org.springframework.stereotype.Service;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {
}
