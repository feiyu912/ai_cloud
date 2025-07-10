package com.feiyu.aiservice.controller;

import com.feiyu.aiservice.service.RAGFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ragflow")
public class RAGFlowController {
    
    @Autowired
    private RAGFlowService ragFlowService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = ragFlowService.healthCheck();
        Map<String, Object> response = new HashMap<>();
        response.put("healthy", isHealthy);
        response.put("service", "ragflow");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建数据集
     */
    @PostMapping("/datasets")
    public ResponseEntity<Map<String, Object>> createDataset(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "数据集名称不能为空"));
        }
        
        Map<String, Object> dataset = ragFlowService.createDataset(name, description);
        if (dataset != null) {
            return ResponseEntity.ok(Map.of("success", true, "data", dataset));
        } else {
            return ResponseEntity.status(500).body(Map.of("error", "创建数据集失败"));
        }
    }
    
    /**
     * 获取数据集列表
     */
    @GetMapping("/datasets")
    public ResponseEntity<Map<String, Object>> listDatasets() {
        List<Map<String, Object>> datasets = ragFlowService.listDatasets();
        return ResponseEntity.ok(Map.of("success", true, "data", datasets));
    }
    
    /**
     * 知识库检索
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "问题不能为空"));
        }
        
        List<Map<String, Object>> results = ragFlowService.queryKnowledge(question);
        return ResponseEntity.ok(Map.of("success", true, "data", results));
    }
} 