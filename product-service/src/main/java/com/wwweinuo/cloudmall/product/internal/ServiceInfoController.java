package com.wwweinuo.cloudmall.product.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceInfoController {

    @GetMapping("/internal/info")
    public String info() {
        return "product-service";
    }
}
