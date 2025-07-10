# RAGFlow API 集成指南

## 概述

本项目已集成RAGFlow官方HTTP API，用于知识库检索和文档管理。所有知识库相关功能都通过远程调用RAGFlow服务实现。

## 配置

### 1. 环境变量配置

在`application.yml`中配置RAGFlow服务：

```yaml
ragflow:
  base-url: http://192.168.100.128:80  # RAGFlow服务地址
  api-key: ${RAGFLOW_API_KEY:}         # API Key（可选）
```

### 2. API Key设置

如果需要使用API Key认证，可以设置环境变量：

```bash
export RAGFLOW_API_KEY=your_api_key_here
```

## API接口

### 1. 健康检查

```http
GET /api/ragflow/health
```

**响应示例：**
```json
{
  "healthy": true,
  "service": "ragflow"
}
```

### 2. 数据集管理

#### 创建数据集

```http
POST /api/ragflow/datasets
Content-Type: application/json

{
  "name": "我的数据集",
  "description": "数据集描述"
}
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "id": "dataset_id_here",
    "name": "我的数据集",
    "description": "数据集描述",
    "embedding_model": "BAAI/bge-large-zh-v1.5@BAAI",
    "chunk_method": "naive",
    "permission": "me"
  }
}
```

#### 获取数据集列表

```http
GET /api/ragflow/datasets
```

**响应示例：**
```json
{
  "success": true,
  "data": [
    {
      "id": "dataset_id_1",
      "name": "数据集1",
      "description": "描述1",
      "chunk_count": 100,
      "document_count": 5
    }
  ]
}
```

### 3. 知识库检索

```http
POST /api/ragflow/search
Content-Type: application/json

{
  "question": "你的问题"
}
```

**响应示例：**
```json
{
  "success": true,
  "data": [
    {
      "source": "ragflow",
      "text": "检索到的内容",
      "score": 0.95,
      "highlight": "<em>高亮</em>的内容",
      "document_id": "doc_id",
      "document_name": "文档名称"
    }
  ]
}
```

### 4. 文档上传

```http
POST /api/chat/session/{sessionId}/upload
Content-Type: multipart/form-data

file: [文件]
datasetId: dataset_id_here
```

**响应示例：**
```
文档上传并解析成功，文档ID: doc_id_here
```

## 使用流程

### 1. 创建数据集

首先创建一个数据集来存储文档：

```bash
curl -X POST http://localhost:8081/api/ragflow/datasets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的知识库",
    "description": "用于存储项目文档"
  }'
```

### 2. 上传文档

将文档上传到指定数据集：

```bash
curl -X POST http://localhost:8081/api/chat/session/1/upload \
  -F "file=@document.pdf" \
  -F "datasetId=your_dataset_id"
```

### 3. 检索知识

使用知识库检索功能：

```bash
curl -X POST http://localhost:8081/api/ragflow/search \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是RAGFlow？"
  }'
```

## 错误处理

### 常见错误码

- `400`: 请求参数错误
- `401`: 未授权访问
- `403`: 访问被拒绝
- `404`: 资源不存在
- `500`: 服务器内部错误

### 错误响应格式

```json
{
  "error": "错误描述信息"
}
```

## 注意事项

1. **API Key**: 如果RAGFlow服务需要API Key认证，请确保正确设置环境变量
2. **数据集ID**: 上传文档时必须提供有效的数据集ID
3. **文件大小**: 支持最大50MB的文件上传
4. **文件格式**: 支持PDF、TXT、DOC、DOCX等常见文档格式
5. **异步处理**: 文档上传后会异步进行解析和向量化，可能需要一些时间

## 开发说明

### 核心类

- `RAGFlowService`: 服务接口
- `RAGFlowServiceImpl`: 服务实现
- `RAGFlowController`: 控制器
- `ChatController`: 聊天相关接口

### 主要功能

1. **知识库检索**: 使用RAGFlow的检索API进行语义搜索
2. **文档管理**: 支持文档上传、解析和向量化
3. **数据集管理**: 创建和管理知识库数据集
4. **健康检查**: 监控RAGFlow服务状态

### 扩展功能

可以根据需要添加以下功能：

1. 文档删除
2. 数据集删除
3. 检索结果缓存
4. 批量文档处理
5. 检索历史记录 