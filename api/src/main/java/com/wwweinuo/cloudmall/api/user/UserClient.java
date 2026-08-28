package com.wwweinuo.cloudmall.api.user;

import com.wwweinuo.cloudmall.api.user.dto.UserDTO;
import com.wwweinuo.cloudmall.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service 对外提供的 Feign 调用契约。
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/users/{id}")
    Result<UserDTO> getById(@PathVariable("id") Long id);
}
