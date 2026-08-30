package com.wwweinuo.cloudmall.product.controller;

import com.wwweinuo.cloudmall.common.response.Result;
import com.wwweinuo.cloudmall.api.product.dto.ProductDTO;
import com.wwweinuo.cloudmall.product.model.Product;
import com.wwweinuo.cloudmall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @Value("${server.port}")
    private String port;

    @GetMapping("/{id}")
    public Result<ProductDTO> getById(@PathVariable Long id) {
        log.info("商品服务实例 port={} 收到请求，productId={}", port, id);
        Product product = productService.getById(id);
        if (product == null) {
            return Result.failure("商品不存在: " + id);
        }
        return Result.success(new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStatus()
        ));
    }
}
