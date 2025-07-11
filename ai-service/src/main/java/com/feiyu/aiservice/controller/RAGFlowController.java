package com.feiyu.aiservice.controller;

import com.feiyu.aiservice.service.RAGFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ragflow")
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
    public ResponseEntity<Map<String, Object>> createDataset(@RequestBody Map<String, Object> request) {
        if (!request.containsKey("name") || request.get("name") == null || request.get("name").toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "数据集名称不能为空"));
        }
        // 只传name字段
        Map<String, Object> onlyName = Map.of("name", request.get("name"));
        Map<String, Object> dataset = ragFlowService.createDataset(onlyName);
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

    /**
     * 全局知识库上传文档
     */
    @PostMapping("/datasets/{datasetId}/upload")
    public ResponseEntity<?> uploadToDataset(
            @PathVariable String datasetId,
            @RequestParam("file") MultipartFile file) {
        String result = ragFlowService.uploadDocument(file, datasetId, null, null);
        if (result.contains("成功")) {
            return ResponseEntity.ok(Map.of("success", true, "message", result));
        } else {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", result));
        }
    }

    /**
     * 获取指定数据集下的文档列表
     */
    @GetMapping("/datasets/{datasetId}/documents")
    public ResponseEntity<Map<String, Object>> listDocuments(
            @PathVariable String datasetId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "keywords", required = false) String keywords) {
        List<Map<String, Object>> docs = ragFlowService.listDocuments(datasetId, page, pageSize, keywords);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("docs", docs)));
    }

    /**
     * 下载指定文档
     */
    @GetMapping("/datasets/{datasetId}/documents/{documentId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String datasetId, @PathVariable String documentId) {
        return ragFlowService.downloadDocument(datasetId, documentId);
    }

    /**
     * 删除指定文档
     */
    @DeleteMapping("/datasets/{datasetId}/documents")
    public ResponseEntity<Map<String, Object>> deleteDocuments(
            @PathVariable String datasetId,
            @RequestBody Map<String, List<String>> request) {
        List<String> ids = request.get("ids");
        boolean success = ragFlowService.deleteDocuments(datasetId, ids);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "删除失败"));
        }
    }
} 