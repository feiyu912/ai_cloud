package com.feiyu.aiservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feiyu.aiservice.entity.ChatMessage;
import com.feiyu.aiservice.mapper.ChatMessageMapper;
import com.feiyu.aiservice.mapper.ChatSessionMapper;
import com.feiyu.aiservice.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    // 这里假设有一个简单的内存存储，实际应接入数据库
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    public List<ChatMessage> getMessagesBySessionId(Long sessionId, Long userId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        // 不加 userId 条件，兼容所有历史数据
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        for (ChatMessage msg : messages) {
            if (msg.getReference() == null) msg.setReference("[]");
            if (msg.getMcpContent() == null) msg.setMcpContent("");
        }
        return messages;
    }

    @Override
    public boolean addMessage(Long sessionId, String role, String content, String reference) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setReference(reference);
        return chatMessageMapper.insert(msg) > 0;
    }

    @Override
    public boolean addMessage(Long sessionId, String role, String content, String reference, String mcpContent) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setReference(reference);
        msg.setMcpContent(mcpContent);
        return chatMessageMapper.insert(msg) > 0;
    }

    @Override
    public void deleteMessagesBySessionId(Long sessionId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        chatMessageMapper.delete(wrapper);
    }
} 