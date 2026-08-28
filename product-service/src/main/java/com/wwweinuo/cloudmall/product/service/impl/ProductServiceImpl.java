package com.wwweinuo.cloudmall.product.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.product.mapper.ProductMapper;
import com.wwweinuo.cloudmall.product.model.Product;
import com.wwweinuo.cloudmall.product.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
