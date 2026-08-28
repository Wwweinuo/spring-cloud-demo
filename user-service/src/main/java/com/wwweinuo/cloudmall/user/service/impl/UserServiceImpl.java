package com.wwweinuo.cloudmall.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wwweinuo.cloudmall.user.mapper.UserMapper;
import com.wwweinuo.cloudmall.user.model.User;
import com.wwweinuo.cloudmall.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
