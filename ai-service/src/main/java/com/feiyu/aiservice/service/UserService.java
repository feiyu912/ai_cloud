package com.feiyu.aiservice.service;

import com.feiyu.aiservice.entity.User;

public interface UserService {
    User findByUsername(String username);
    boolean register(String username, String password);
    User login(String username, String password);
} 