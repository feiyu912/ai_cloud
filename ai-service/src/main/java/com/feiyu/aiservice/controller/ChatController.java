package com.feiyu.aiservice.controller;

import com.feiyu.aiservice.entity.ChatMessage;
import com.feiyu.aiservice.entity.ChatSession;
import com.feiyu.aiservice.mcp.impl.TimeTool;
import com.feiyu.aiservice.service.ChatMessageService;
// ChatSessionFileService已删除，使用RAGFlow远程服务
import com.feiyu.aiservice.service.ChatSessionService;
import com.feiyu.aiservice.service.RAGFlowService;
import com.feiyu.aiservice.util.PromptBuilder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
    public List<Map<String, Object>> searchSessionAndGlobal(@PathVariable("sessionId") Long sessionId, @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String question = (String) body.get("question");
            List<String> datasetIds = (List<String>) body.get("dataset_ids");
            List<Map<String, Object>> ragflowResults = ragFlowService.queryKnowledge(question, datasetIds);
            results.addAll(ragflowResults);
            System.out.println("[DEBUG] RAGFlow检索到 " + ragflowResults.size() + " 条结果");
        } catch (Exception e) {
            System.err.println("[DEBUG] RAGFlow检索失败: " + e.getMessage());
        }
        return results;
    }

    @PostMapping("/session/{id}/chat")
    public Map<String, Object> chatWithSession(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long sessionId,
            @RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Map.of("success", false, "msg", "问题不能为空");
        }

        // ====== 恢复 RAG 检索和拼接 ======
        Long userId = UserController.getUserIdByToken(token);
        Object datasetIdsObj = body.get("dataset_ids");
        List<String> datasetIds = null;
        if (datasetIdsObj instanceof List<?>) {
            datasetIds = (List<String>) datasetIdsObj;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        if (datasetIds != null && !datasetIds.isEmpty()) {
            try {
                List<Map<String, Object>> ragflowResults = ragFlowService.queryKnowledge(question, datasetIds);
                filtered.addAll(ragflowResults);
                System.out.println("[DEBUG] RAGFlow检索到 " + ragflowResults.size() + " 条结果");
            } catch (Exception e) {
                System.err.println("[DEBUG] RAGFlow检索失败: " + e.getMessage());
            }
        }
        // 拼接 prompt
        List<String> contextList = new ArrayList<>();
        for (Map<String, Object> item : filtered) {
            Object content = item.get("content");
            if (content != null) contextList.add(content.toString());
        }
        String context = PromptBuilder.buildContext(contextList, 32000, 2000);
        String finalPrompt = question + (context.isEmpty() ? "" : "\n\n相关知识：\n" + context);
        // 打印 prompt 长度和内容
        System.out.println("[DEBUG] prompt长度: " + finalPrompt.length());
        System.out.println("[DEBUG] prompt内容: " + (finalPrompt.length() > 1000 ? finalPrompt.substring(0, 500) + "...\n..." + finalPrompt.substring(finalPrompt.length() - 500) : finalPrompt));

        String aiReply;
        try {
            String apiKey = "sk-a75e2109f45e4704b3c14cb25e245186"; // 建议从配置读取
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = "{\n" +
                "  \"model\": \"qwen-turbo\",\n" +
                "  \"input\": {\n" +
                "    \"messages\": [\n" +
                "      {\"role\": \"user\", \"content\": " + objectMapper.writeValueAsString(finalPrompt) + "}\n" +
                "    ]\n" +
                "  }\n" +
                "}";
            System.out.println("[DEBUG] 实际请求体: " + jsonBody);

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                .build();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("DashScope API error: " + response.code() + " " + response.message());
                }
                String respBody = response.body().string();
                JsonNode root = objectMapper.readTree(respBody);
                aiReply = root.path("output").path("text").asText();
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] DashScope调用失败: " + e.getMessage());
            return Map.of("success", false, "msg", "AI回复失败: " + e.getMessage());
        }

        // 保存 AI 回复到数据库
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String referenceJson = objectMapper.writeValueAsString(filtered);
            boolean ok = chatMessageService.addMessage(sessionId, "assistant", aiReply, referenceJson);
            if (!ok) {
                System.err.println("[DEBUG] AI回复保存失败");
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] reference序列化失败: " + e.getMessage());
        }
        return Map.of("success", true, "reply", aiReply, "reference", filtered);
    }
}