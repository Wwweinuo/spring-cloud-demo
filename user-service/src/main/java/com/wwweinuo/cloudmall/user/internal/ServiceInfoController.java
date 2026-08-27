package com.wwweinuo.cloudmall.user.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class ServiceInfoController {

    @Value("${cloudmall.greeting:NOT_FOUND}")
    private String greeting;

    @GetMapping("/internal/info")
    public String info() {
        return "user-service";
    }

    @GetMapping("/internal/nacos-config")
    public String nacosConfig() {
        return greeting;
    }
}
