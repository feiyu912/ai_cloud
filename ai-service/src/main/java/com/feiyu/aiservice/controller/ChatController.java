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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

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
    private com.feiyu.aiservice.mcp.impl.TimeTool timeTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.RandomTool randomTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.FileSystemTool fileSystemTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.UnitConvertTool unitConvertTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.MysqlServiceImpl mysqlService;
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
        // ====== 后端分流：MCP工具问题直接返回 ======
        if (isTimeQuestion(question)) {
            String timeResult = timeTool.getCurrentTime();
            return Map.of("success", true, "reply", "当前时间：" + timeResult, "reference", java.util.List.of());
        }
        if (isRandomQuestion(question)) {
            String rand1 = randomTool.generateRandom(1, 100);
            String rand2 = randomTool.generateRandom(1, 100);
            return Map.of("success", true, "reply", "生成的两个随机数为：" + rand1 + ", " + rand2, "reference", java.util.List.of());
        }
        if (isFileSystemQuestion(question)) {
            String fsResult = fileSystemTool.listFiles(".");
            return Map.of("success", true, "reply", "当前目录文件列表：" + fsResult, "reference", java.util.List.of());
        }
        if (isUnitConvertQuestion(question)) {
            String ucResult = unitConvertTool.convert(1.0, "m2ft");
            return Map.of("success", true, "reply", "1米约等于：" + ucResult, "reference", java.util.List.of());
        }
        if (isMysqlQuestion(question)) {
            java.util.List<String> dbNames = mysqlService.listAllDatabaseNames();
            String mysqlResult = String.join(", ", dbNames);
            return Map.of("success", true, "reply", "MySQL数据库列表：" + mysqlResult, "reference", java.util.List.of());
        }
        // ====== 其它问题走原有大模型流程 ======
        // ====== 检索知识库，拼接prompt ======
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
        // 只保留 similarity >= 0.4 的片段用于prompt
        List<Map<String, Object>> filteredHighSim = filtered.stream()
            .filter(item -> {
                Object sim = item.get("similarity");
                if (sim instanceof Number) {
                    return ((Number) sim).doubleValue() >= 0.4;
                }
                try {
                    return Double.parseDouble(sim.toString()) >= 0.4;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(java.util.stream.Collectors.toList());
        // 拼接 prompt 只用高相似度内容
        List<String> contextList = new ArrayList<>();
        for (Map<String, Object> item : filteredHighSim) {
            Object content = item.get("content");
            if (content != null) contextList.add(content.toString());
        }
        String context = PromptBuilder.buildContext(contextList, 32000, 2000);
        // ====== 拼接MCP工具结果到prompt前面 ======
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

    @PostMapping("/session/{id}/chat/stream")
    public ResponseBodyEmitter chatWithSessionStream(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long sessionId,
            @RequestBody Map<String, Object> body) {
        // 设置超时时间为5分钟，防止流式响应被提前关闭
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(300_000L);
        new Thread(() -> {
            StringBuilder aiReplyBuilder = new StringBuilder();
            try {
                String question = (String) body.get("question");
                if (question == null || question.trim().isEmpty()) {
                    emitter.send("data: 问题不能为空\n");
                    emitter.completeWithError(new Exception("问题不能为空"));
                    return;
                }
                // ====== 后端分流：MCP工具问题直接返回 ======
                if (isTimeQuestion(question)) {
                    String timeResult = timeTool.getCurrentTime();
                    emitter.send("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "reply", "当前时间：" + timeResult,
                        "reference", java.util.List.of(),
                        "success", true
                    )) + "\n");
                    emitter.complete();
                    return;
                }
                if (isRandomQuestion(question)) {
                    String rand1 = randomTool.generateRandom(1, 100);
                    String rand2 = randomTool.generateRandom(1, 100);
                    emitter.send("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "reply", "生成的两个随机数为：" + rand1 + ", " + rand2,
                        "reference", java.util.List.of(),
                        "success", true
                    )) + "\n");
                    emitter.complete();
                    return;
                }
                if (isFileSystemQuestion(question)) {
                    String fsResult = fileSystemTool.listFiles(".");
                    emitter.send("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "reply", "当前目录文件列表：" + fsResult,
                        "reference", java.util.List.of(),
                        "success", true
                    )) + "\n");
                    emitter.complete();
                    return;
                }
                if (isUnitConvertQuestion(question)) {
                    String ucResult = unitConvertTool.convert(1.0, "m2ft");
                    emitter.send("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "reply", "1米约等于：" + ucResult,
                        "reference", java.util.List.of(),
                        "success", true
                    )) + "\n");
                    emitter.complete();
                    return;
                }
                if (isMysqlQuestion(question)) {
                    java.util.List<String> dbNames = mysqlService.listAllDatabaseNames();
                    String mysqlResult = String.join(", ", dbNames);
                    emitter.send("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "reply", "MySQL数据库列表：" + mysqlResult,
                        "reference", java.util.List.of(),
                        "success", true
                    )) + "\n");
                    emitter.complete();
                    return;
                }
                // ====== 其它问题走原有大模型流程 ======
                // ====== 检索知识库，拼接prompt ======
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
                    } catch (Exception e) {
                        emitter.send("data: [RAGFlow检索失败] " + e.getMessage() + "\n");
                        emitter.completeWithError(e);
                        return;
                    }
                }
                // 只保留 similarity >= 0.4 的片段用于prompt
                List<Map<String, Object>> filteredHighSim = filtered.stream()
                    .filter(item -> {
                        Object sim = item.get("similarity");
                        if (sim instanceof Number) {
                            return ((Number) sim).doubleValue() >= 0.4;
                        }
                        try {
                            return Double.parseDouble(sim.toString()) >= 0.4;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
                // 拼接 prompt 只用高相似度内容
                List<String> contextList = new ArrayList<>();
                for (Map<String, Object> item : filteredHighSim) {
                    Object content = item.get("content");
                    if (content != null) contextList.add(content.toString());
                }
                String context = PromptBuilder.buildContext(contextList, 32000, 2000);
                String finalPrompt = question + (context.isEmpty() ? "" : "\n\n相关知识：\n" + context);

                // ====== DashScope流式调用（OkHttp） ======
                String apiKey = "sk-a75e2109f45e4704b3c14cb25e245186"; // 建议从配置读取
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonBody = "{\n" +
                    "  \"model\": \"qwen-max-latest\",\n" +
                    "  \"input\": {\n" +
                    "    \"messages\": [\n" +
                    "      {\"role\": \"user\", \"content\": " + objectMapper.writeValueAsString(finalPrompt) + "}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  \"parameters\": {\n" +
                    "    \"incremental_output\": true\n" +
                    "  }\n" +
                    "}";
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                okhttp3.RequestBody bodyReq = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), jsonBody
                );
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(bodyReq)
                    .build();
                okhttp3.Response response = client.newCall(request).execute();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body().byteStream(), java.nio.charset.StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[DEBUG] 流式原始行: " + line);
                    // 只处理data:开头的行，其他直接跳过
                    if (!line.trim().isEmpty() && line.trim().startsWith("data:")) {
                        String jsonStr = line.trim().substring(5).trim();
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(jsonStr);
                            String text = node.path("output").path("text").asText();
                            if (text != null && !text.isEmpty()) {
                                emitter.send("data: " + objectMapper.writeValueAsString(java.util.Map.of("output", java.util.Map.of("text", text))) + "\n");
                                aiReplyBuilder.append(text);
                                System.out.println("[DEBUG] 当前拼接AI回复: " + aiReplyBuilder.toString());
                            }
                        } catch (Exception ex) {
                            System.err.println("[DEBUG] 解析流式JSON失败: " + ex.getMessage());
                        }
                    }
                }
                // ====== 流式结束后，保存AI回复和reference到数据库 ======
                try {
                    String aiReply = aiReplyBuilder.toString();
                    String referenceJson = objectMapper.writeValueAsString(filtered);
                    System.out.println("[DEBUG] 最终拼接AI回复: [" + aiReply + "]");
                    System.out.println("[DEBUG] reference: [" + referenceJson + "]");
                    boolean ok = chatMessageService.addMessage(sessionId, "assistant", aiReply, referenceJson);
                    System.out.println("[DEBUG] 保存AI消息到数据库结果: " + ok);
                    if (!ok) {
                        System.err.println("[DEBUG] 流式AI回复保存失败");
                    }
                    emitter.send("data: " + objectMapper.writeValueAsString(java.util.Map.of(
                        "reply", aiReply,
                        "reference", filtered,
                        "success", ok
                    )) + "\n");
                } catch (Exception e) {
                    System.err.println("[DEBUG] 流式reference序列化失败: " + e.getMessage());
                }
                emitter.complete(); // 标记完成
            } catch (Exception e) {
                try { emitter.send("data: [ERROR] " + e.getMessage() + "\n"); } catch (Exception ignore) {}
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    // MCP 工具问题类型判断（扩展同义表达）
    private static final String[] TIME_KEYWORDS = {
        "时间", "几点", "当前时间", "现在时间", "what time", "time now", "time is it"
    };
    private static final String[] RANDOM_KEYWORDS = {
        "随机", "随机数", "random", "generate random", "随机生成"
    };
    private static final String[] FILESYSTEM_KEYWORDS = {
        "文件", "目录", "file", "folder", "list files", "文件列表", "当前目录"
    };
    private static final String[] UNITCONVERT_KEYWORDS = {
        "米转英尺", "米换算英尺", "m2ft", "unit convert", "单位换算", "米", "英尺"
    };
    private static final String[] MYSQL_KEYWORDS = {
        "mysql", "数据库", "database", "show databases", "所有数据库", "列出数据库"
    };

    private boolean isTimeQuestion(String question) {
        return containsAnyKeyword(question, TIME_KEYWORDS);
    }
    private boolean isRandomQuestion(String question) {
        return containsAnyKeyword(question, RANDOM_KEYWORDS);
    }
    private boolean isFileSystemQuestion(String question) {
        return containsAnyKeyword(question, FILESYSTEM_KEYWORDS);
    }
    private boolean isUnitConvertQuestion(String question) {
        // 必须同时包含“米”和“英尺”或其它换算关键词
        if (question == null) return false;
        String q = question.toLowerCase();
        boolean hasMeter = q.contains("米") || q.contains("m");
        boolean hasFeet = q.contains("英尺") || q.contains("ft");
        boolean hasOther = containsAnyKeyword(question, UNITCONVERT_KEYWORDS);
        return (hasMeter && hasFeet) || hasOther;
    }
    private boolean isMysqlQuestion(String question) {
        return containsAnyKeyword(question, MYSQL_KEYWORDS);
    }
    private boolean containsAnyKeyword(String question, String[] keywords) {
        if (question == null) return false;
        String q = question.toLowerCase();
        for (String kw : keywords) {
            if (q.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}