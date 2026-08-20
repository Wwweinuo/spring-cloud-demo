package com.wwweinuo.cloudmall.product.controller;

import com.wwweinuo.cloudmall.common.response.Result;
import com.wwweinuo.cloudmall.product.model.Product;
import com.wwweinuo.cloudmall.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.failure("商品不存在: " + id);
        }
        return Result.success(product);
    }
}
