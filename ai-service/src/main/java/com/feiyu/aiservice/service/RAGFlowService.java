package com.feiyu.aiservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface RAGFlowService {
    /**
     * 知识库检索
     * @param question 用户问题
     * @return 检索结果列表
     */
    List<Map<String, Object>> queryKnowledge(String question);
    
    /**
     * 文档上传到知识库
     * @param file 上传的文件
     * @param datasetId 数据集ID
     * @param authorization 授权头
     * @param cookie Cookie
     * @return 上传结果
     */
    String uploadDocument(MultipartFile file, String datasetId, String authorization, String cookie);
    
    /**
     * 健康检查
     * @return 是否可用
     */
    boolean healthCheck();
    
    /**
     * 创建数据集
     * @param name 数据集名称
     * @param description 数据集描述
     * @return 创建的数据集信息
     */
    Map<String, Object> createDataset(Map<String, Object> request);
    
    /**
     * 获取数据集列表
     * @return 数据集列表
     */
    List<Map<String, Object>> listDatasets();

    /**
     * 获取指定数据集下的文档列表
     */
    List<Map<String, Object>> listDocuments(String datasetId, Integer page, Integer pageSize, String keywords);

    /**
     * 下载指定文档
     */
    ResponseEntity<byte[]> downloadDocument(String datasetId, String documentId);

    /**
     * 删除指定文档
     */
    boolean deleteDocuments(String datasetId, List<String> documentIds);
} 