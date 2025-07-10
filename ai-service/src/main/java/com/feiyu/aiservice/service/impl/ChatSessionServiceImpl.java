package com.feiyu.aiservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feiyu.aiservice.entity.ChatSession;
import com.feiyu.aiservice.mapper.ChatSessionMapper;
import com.feiyu.aiservice.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Override
    public List<ChatSession> getSessionsByUserId(Long userId) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("updated_at");
        return chatSessionMapper.selectList(wrapper);
    }

    @Override
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        int res = chatSessionMapper.insert(session);
        return res > 0 ? session : null;
    }

    @Override
    public boolean renameSession(Long sessionId, Long userId, String title) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) return false;
        session.setTitle(title);
        return chatSessionMapper.updateById(session) > 0;
    }

    @Override
    public boolean deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) return false;
        return chatSessionMapper.deleteById(sessionId) > 0;
    }

    @Override
    public ChatSession getSessionById(Long sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }
} 