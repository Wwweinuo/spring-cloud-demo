package com.wwweinuo.cloudmall.user.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.user.mapper.UserMapper;
import com.wwweinuo.cloudmall.user.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
}
