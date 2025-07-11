package com.feiyu.aiservice.controller;

import com.feiyu.aiservice.entity.ChatMessage;
import com.feiyu.aiservice.entity.ChatSession;
import com.feiyu.aiservice.mcp.impl.TimeTool;
import com.feiyu.aiservice.service.ChatMessageService;
// ChatSessionFileService已删除，使用RAGFlow远程服务
import com.feiyu.aiservice.service.ChatSessionService;
import com.feiyu.aiservice.service.RAGFlowService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private ChatMessageService chatMessageService;
    // ChatSessionFileService已删除，使用RAGFlow远程服务
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private TimeTool timeTool;
    @Autowired
    private MethodToolCallbackProvider mcpToolCallbacks;
    @Autowired
    private RAGFlowService ragFlowService;

    // 获取当前用户所有会话
    @GetMapping("/sessions")
    public List<ChatSession> getSessions(@RequestHeader("Authorization") String token) {
        Long userId = UserController.getUserIdByToken(token);
        return chatSessionService.getSessionsByUserId(userId);
    }

    // 获取某会话所有消息
    @GetMapping("/session/{id}")
    public List<ChatMessage> getSessionMessages(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId) {
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (session == null) return Collections.emptyList();
        return chatMessageService.getMessagesBySessionId(sessionId, session.getUserId());
    }

    // 向会话发送消息
    @PostMapping("/session/{id}/message")
    public Map<String, Object> sendMessage(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId, @RequestBody Map<String, Object> body) {
        Long userId = UserController.getUserIdByToken(token);
        String role = (String) body.get("role");
        String content = (String) body.get("content");
        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) reference = "[]";
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        boolean hasSession = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
        if (!hasSession) return Map.of("success", false, "msg", "无权访问该会话");
        boolean ok = chatMessageService.addMessage(sessionId, role, content, reference);
        if (ok) return Map.of("success", true);
        return Map.of("success", false, "msg", "消息发送失败");
    }

    // 新建会话
    @PostMapping("/session")
    public Map<String, Object> createSession(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {
        Long userId = UserController.getUserIdByToken(token);
        String title = body.getOrDefault("title", "新会话");
        ChatSession session = chatSessionService.createSession(userId, title);
        if (session != null) return Map.of("success", true, "id", session.getId());
        return Map.of("success", false, "msg", "新建会话失败");
    }

    // 会话重命名
    @PostMapping("/session/{id}/rename")
    public Map<String, Object> renameSession(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId, @RequestBody Map<String, String> body) {
        Long userId = UserController.getUserIdByToken(token);
        String title = body.getOrDefault("title", "");
        boolean ok = chatSessionService.renameSession(sessionId, userId, title);
        if (ok) return Map.of("success", true);
        return Map.of("success", false, "msg", "重命名失败");
    }

    // 会话删除
    @DeleteMapping("/session/{id}")
    public Map<String, Object> deleteSession(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId) {
        Long userId = UserController.getUserIdByToken(token);
        boolean ok = chatSessionService.deleteSession(sessionId, userId);
        if (ok) {
            chatMessageService.deleteMessagesBySessionId(sessionId);
            // 如需同步删除远程知识库，可在此调用 ragFlowService
            return Map.of("success", true);
        }
        return Map.of("success", false, "msg", "删除失败");
    }

    // 对话专属文件上传（两步走：先上传，再解析/入库）
    @PostMapping("/session/{sessionId}/upload")
    public ResponseEntity<?> uploadSessionFile(@RequestHeader("Authorization") String token,
                                               @RequestHeader(value = "Cookie", required = false) String cookie,
                                               @RequestParam("datasetId") String datasetId,
                                               @PathVariable("sessionId") Long sessionId,
                                               @RequestParam("file") MultipartFile file) {
        Long userId = UserController.getUserIdByToken(token);
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        boolean hasSession = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
        if (!hasSession) return ResponseEntity.status(403).body("无权访问该会话");
        try {
            // 这里 token 作为 authorization 传递给 ragflow，cookie 也传递
            String result = ragFlowService.uploadDocument(file, datasetId, token, cookie);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传失败: " + e.getMessage());
        }
    }

    // 对话检索接口：只通过RAGFlowService远程检索
    @PostMapping("/session/{sessionId}/search")
    public List<Map<String, Object>> searchSessionAndGlobal(@PathVariable("sessionId") Long sessionId, @RequestBody String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<Map<String, Object>> ragflowResults = ragFlowService.queryKnowledge(query);
            results.addAll(ragflowResults);
            System.out.println("[DEBUG] RAGFlow检索到 " + ragflowResults.size() + " 条结果");
        } catch (Exception e) {
            System.err.println("[DEBUG] RAGFlow检索失败: " + e.getMessage());
        }
        return results;
    }
}