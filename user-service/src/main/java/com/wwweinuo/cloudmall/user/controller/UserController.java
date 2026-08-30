package com.wwweinuo.cloudmall.user.controller;

import com.wwweinuo.cloudmall.common.response.Result;
import com.wwweinuo.cloudmall.api.user.dto.UserDTO;
import com.wwweinuo.cloudmall.user.model.User;
import com.wwweinuo.cloudmall.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.failure("用户不存在: " + id);
        }
        return Result.success(new UserDTO(user.getId(), user.getUsername(), user.getStatus()));
    }
}
