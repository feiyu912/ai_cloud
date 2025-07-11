package com.feiyu.aiservice.service.impl;

import com.feiyu.aiservice.service.RAGFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RAGFlowServiceImpl implements RAGFlowService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ragflow.base-url:http://192.168.100.128:80}")
    private String ragflowBaseUrl;
    
    @Value("${ragflow.api-key:}")
    private String ragflowApiKey;
    
    @Override
    public List<Map<String, Object>> queryKnowledge(String question) {
        try {
            // 使用官方API: POST /v1/retrieval
            String url = ragflowBaseUrl + "/v1/retrieval";
            Map<String, Object> request = new HashMap<>();
            request.put("question", question);
            request.put("top_k", 5);
            request.put("similarity_threshold", 0.2);
            request.put("vector_similarity_weight", 0.3);
            request.put("highlight", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (ragflowApiKey != null && !ragflowApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + ragflowApiKey);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data.containsKey("chunks")) {
                        List<Map<String, Object>> chunks = (List<Map<String, Object>>) data.get("chunks");
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (Map<String, Object> chunk : chunks) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("source", "ragflow");
                            result.put("text", chunk.get("content"));
                            result.put("score", chunk.get("similarity"));
                            result.put("highlight", chunk.get("highlight"));
                            result.put("document_id", chunk.get("document_id"));
                            result.put("document_name", chunk.get("document_keyword"));
                            results.add(result);
                        }
                        return results;
                    }
                }
            }
            System.err.println("[RAGFlow] 未获取到知识库数据，返回空数组");
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[RAGFlow] 知识库检索失败: " + e.getMessage());
            return Collections.singletonList(Map.of(
                "source", "ragflow",
                "text", "知识库服务异常，请稍后重试",
                "score", 0
            ));
        }
    }
    
    @Override
    public String uploadDocument(MultipartFile file, String datasetId, String authorization, String cookie) {
        try {
            // 1. 上传文档
            String uploadUrl = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/documents";
            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (ragflowApiKey != null && !ragflowApiKey.isEmpty()) {
                uploadHeaders.set("Authorization", "Bearer " + ragflowApiKey);
            } else if (authorization != null && !authorization.isEmpty()) {
                uploadHeaders.set("Authorization", authorization);
            }
            if (cookie != null && !cookie.isEmpty()) {
                uploadHeaders.set("Cookie", cookie);
            }
            MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
            uploadBody.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(uploadBody, uploadHeaders);
            ResponseEntity<Map> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.POST, uploadRequest, Map.class);
            if (uploadResponse.getStatusCode() != HttpStatus.OK || uploadResponse.getBody() == null) {
                return "上传失败: " + (uploadResponse.getBody() != null ? uploadResponse.getBody().toString() : "无返回");
            }

            // 2. 获取文档列表，拿到最新文档ID
            String listUrl = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/documents?page=1&page_size=1";
            HttpHeaders listHeaders = new HttpHeaders();
            listHeaders.set("Authorization", "Bearer " + ragflowApiKey);
            HttpEntity<String> listRequest = new HttpEntity<>(listHeaders);
            ResponseEntity<Map> listResponse = restTemplate.exchange(listUrl, HttpMethod.GET, listRequest, Map.class);
            String documentId = null;
            if (listResponse.getStatusCode() == HttpStatus.OK && listResponse.getBody() != null) {
                Map<String, Object> body = listResponse.getBody();
                if (body.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    List<Map<String, Object>> docs = (List<Map<String, Object>>) data.get("docs");
                    if (docs != null && !docs.isEmpty()) {
                        documentId = (String) docs.get(0).get("id");
                    }
                }
            }
            if (documentId == null) {
                return "上传成功但未获取到文档ID";
            }

            // 3. 自动调用解析接口
            String parseUrl = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/chunks";
            HttpHeaders parseHeaders = new HttpHeaders();
            parseHeaders.setContentType(MediaType.APPLICATION_JSON);
            parseHeaders.set("Authorization", "Bearer " + ragflowApiKey);
            Map<String, Object> parseBody = new HashMap<>();
            parseBody.put("document_ids", Collections.singletonList(documentId));
            HttpEntity<Map<String, Object>> parseRequest = new HttpEntity<>(parseBody, parseHeaders);
            ResponseEntity<Map> parseResponse = restTemplate.exchange(parseUrl, HttpMethod.POST, parseRequest, Map.class);
            if (parseResponse.getStatusCode() == HttpStatus.OK) {
                return "文档上传并解析成功，文档ID: " + documentId;
            } else {
                return "文档上传成功但解析失败: " + (parseResponse.getBody() != null ? parseResponse.getBody().toString() : "无返回");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "上传或解析异常: " + e.getMessage();
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            // 使用数据集列表API作为健康检查
            String url = ragflowBaseUrl + "/v1/datasets?page=1&page_size=1";
            HttpHeaders headers = new HttpHeaders();
            if (ragflowApiKey != null && !ragflowApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + ragflowApiKey);
            }
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println("[RAGFlow] 健康检查失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建数据集
     */
    public Map<String, Object> createDataset(String name, String description) {
        try {
            String url = ragflowBaseUrl + "/v1/datasets";
            Map<String, Object> request = new HashMap<>();
            request.put("name", name);
            request.put("description", description);
            request.put("embedding_model", "BAAI/bge-large-zh-v1.5@BAAI");
            request.put("chunk_method", "naive");
            request.put("permission", "me");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (ragflowApiKey != null && !ragflowApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + ragflowApiKey);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    return (Map<String, Object>) body.get("data");
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("[RAGFlow] 创建数据集失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取数据集（知识库）列表
     */
    public List<Map<String, Object>> listDatasets() {
        System.out.println("当前ragflowApiKey: [" + ragflowApiKey + "]");
        try {
            String url = ragflowBaseUrl + "/api/v1/datasets?page=1&page_size=100";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + ragflowApiKey);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                System.out.println("ragflow原始返回: " + response.getBody());
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    return (List<Map<String, Object>>) body.get("data");
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[RAGFlow] 获取数据集列表失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> listDocuments(String datasetId, Integer page, Integer pageSize, String keywords) {
        try {
            String url = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/documents";
            StringBuilder params = new StringBuilder();
            if (page != null) params.append("?page=" + page);
            if (pageSize != null) params.append((params.length() > 0 ? "&" : "?") + "page_size=" + pageSize);
            if (keywords != null && !keywords.isEmpty()) params.append((params.length() > 0 ? "&" : "?") + "keywords=" + keywords);
            url += params.toString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + ragflowApiKey);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data.containsKey("docs")) {
                        return (List<Map<String, Object>>) data.get("docs");
                    }
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[RAGFlow] 获取文档列表失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ResponseEntity<byte[]> downloadDocument(String datasetId, String documentId) {
        try {
            String url = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/documents/" + documentId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + ragflowApiKey);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public boolean deleteDocuments(String datasetId, List<String> documentIds) {
        try {
            String url = ragflowBaseUrl + "/api/v1/datasets/" + datasetId + "/documents";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + ragflowApiKey);
            Map<String, Object> body = new HashMap<>();
            body.put("ids", documentIds);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object code = response.getBody().get("code");
                return code != null && code.toString().equals("0");
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
} 