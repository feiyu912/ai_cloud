package com.feiyu.aiservice.controller;

import com.feiyu.aiservice.entity.User;
import com.feiyu.aiservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    private UserService userService;

    // 简单内存token存储，生产环境请用Redis或JWT
    private static final Map<String, Long> tokenUserMap = new HashMap<>();

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        boolean ok = userService.register(username, password);
        if (ok) {
            User user = userService.findByUsername(username);
            return Map.of("success", true, "userId", user.getId());
        } else {
            return Map.of("success", false, "msg", "用户名已存在");
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        User user = userService.login(username, password);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            tokenUserMap.put(token, user.getId());
            return Map.of("success", true, "token", token, "userId", user.getId());
        }
        return Map.of("success", false, "msg", "用户名或密码错误");
    }

    // token校验接口，前端可用来判断登录状态
    @GetMapping("/check")
    public Map<String, Object> check(@RequestHeader("Authorization") String token) {
        Long userId = tokenUserMap.get(token);
        if (userId != null) {
            return Map.of("success", true, "userId", userId);
        }
        return Map.of("success", false);
    }

    // 获取当前用户ID的工具方法
    public static Long getUserIdByToken(String token) {
        return tokenUserMap.get(token);
    }
} 