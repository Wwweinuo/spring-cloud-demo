package com.wwweinuo.cloudmall.order.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceInfoController {

    @GetMapping("/internal/info")
    public String info() {
        return "order-service";
    }
}
