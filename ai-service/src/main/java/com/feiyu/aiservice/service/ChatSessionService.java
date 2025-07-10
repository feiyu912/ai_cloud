package com.feiyu.aiservice.service;

import com.feiyu.aiservice.entity.ChatSession;

import java.util.List;

public interface ChatSessionService  {
    List<ChatSession> getSessionsByUserId(Long userId);
    ChatSession createSession(Long userId, String title);
    boolean renameSession(Long sessionId, Long userId, String title);
    boolean deleteSession(Long sessionId, Long userId);
    ChatSession getSessionById(Long sessionId);
} 