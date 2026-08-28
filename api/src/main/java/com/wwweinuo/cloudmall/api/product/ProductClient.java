package com.wwweinuo.cloudmall.api.product;

import com.wwweinuo.cloudmall.api.product.dto.ProductDTO;
import com.wwweinuo.cloudmall.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * product-service 对外提供的 Feign 调用契约。
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    Result<ProductDTO> getById(@PathVariable("id") Long id);
}
