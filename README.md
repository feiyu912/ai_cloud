# AI Cloud 智能云服务平台

## 项目概述

AI Cloud 是一个基于微服务架构的智能云服务平台，集成了先进的人工智能能力，提供知识库管理、智能聊天、文档分析等功能。项目采用前后端分离架构，后端使用 Spring Boot + Spring Cloud 构建，前端采用 Vue 3 + Element Plus 开发，同时集成了 RAGFlow 远程服务实现高效的知识库管理。

## 项目架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   前端应用      │────▶│   API网关       │────▶│   AI服务        │────▶│   RAGFlow       │
│  (Vue 3)        │◀────│ (Gateway)       │◀────│  (Spring Boot)  │◀────│  远程服务       │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
                                                           ▲
                                                           │
                                                  ┌────────┴────────┐
                                                  │                 │
                                          ┌────────┴─┐     ┌───────┴──────┐
                                          │  MySQL   │     │    Redis     │
                                          └──────────┘     └──────────────┘
```

### 服务组件

1. **ai-front-end** - 前端应用
   - 基于 Vue 3 + Vite 构建
   - 使用 Element Plus 组件库
   - 提供用户界面和交互功能

2. **gateway-service** - API网关服务
   - 基于 Spring Cloud Gateway
   - 提供服务路由和负载均衡
   - 集成 Nacos 服务发现

3. **ai-service** - AI核心服务
   - 基于 Spring Boot 构建
   - 提供聊天、知识库检索、文档管理等功能
   - 集成 Spring AI 实现 AI 能力
   - 连接 RAGFlow 远程服务实现知识库管理

## 技术栈

### 后端技术

- **框架**: Spring Boot 3.3.4, Spring Cloud 2023.0.3, Spring Cloud Alibaba 2023.0.3.2
- **AI能力**: Spring AI 1.0.0
- **服务治理**: Nacos 服务发现与配置管理
- **数据库**: MySQL 8.0, Redis
- **ORM**: MyBatis
- **构建工具**: Maven
- **JDK版本**: Java 21

### 前端技术

- **框架**: Vue 3
- **构建工具**: Vite 5.2.0
- **UI组件库**: Element Plus 2.7.2
- **HTTP客户端**: Axios 1.10.0
- **路由**: Vue Router 4.2.5
- **Markdown渲染**: Marked 16.1.1

## 主要功能

### 1. 聊天功能
- 创建、管理和删除聊天会话
- 发送和接收消息
- 会话消息历史记录
- 支持工具调用能力

### 2. 知识库管理
- 数据集创建与管理
- 文档上传与解析
- 语义搜索与知识检索
- 检索结果高亮显示

### 3. 工具集成
- 时间工具
- 随机数生成工具
- 文件系统工具
- 单位转换工具
- MySQL数据库服务

### 4. 微服务能力
- 服务注册与发现
- 配置中心管理
- API网关路由
- 负载均衡

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Node.js 16+
- npm 8+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.2.0+
- RAGFlow 服务

### 后端部署

#### 1. 配置Nacos

启动Nacos服务，并确保服务可访问。

#### 2. 配置RAGFlow

在 `ai-service/src/main/resources/application.yml` 中配置RAGFlow服务：

```yaml
ragflow:
  base-url: http://192.168.100.128:80  # RAGFlow服务地址
  api-key: ${RAGFLOW_API_KEY:}         # API Key（可选）
```

#### 3. 构建与启动服务

```bash
# 在项目根目录执行
mvn clean install

# 启动网关服务
cd gateway-service
mvn spring-boot:run

# 启动AI服务
cd ../ai-service
mvn spring-boot:run
```

### 前端部署

```bash
# 进入前端目录
cd ai-front-end

# 安装依赖
npm install

# 开发环境启动
npm run dev

# 生产环境构建
npm run build
```

## API接口说明

### 1. 聊天相关接口
- `GET /chat/sessions` - 获取用户会话列表
- `POST /chat/session` - 创建新会话
- `GET /chat/session/{id}` - 获取会话消息
- `POST /chat/session/{id}/message` - 发送消息
- `POST /chat/session/{id}/rename` - 重命名会话
- `DELETE /chat/session/{id}` - 删除会话

### 2. RAGFlow相关接口
- `GET /api/ragflow/health` - 健康检查
- `GET /api/ragflow/datasets` - 获取数据集列表
- `POST /api/ragflow/datasets` - 创建数据集
- `POST /api/ragflow/search` - 语义搜索
- `POST /api/chat/session/{sessionId}/upload` - 文档上传

## 数据流程

### 1. 文档上传流程
```
前端 → Gateway → ai-service → RAGFlow (上传+解析)
```

### 2. 知识检索流程
```
前端 → Gateway → ai-service → RAGFlow (语义搜索) → 前端展示
```

### 3. 聊天流程
```
前端发送消息 → Gateway → ai-service → ChatModel处理 → 前端展示
```

## 配置管理

### 环境变量

- `RAGFLOW_API_KEY` - RAGFlow API Key（可选）

### 配置文件

- `application.yml` - 主要配置文件
- Nacos配置中心 - 分布式配置管理

## 项目迁移说明

本项目已完成从本地Redis向量存储到RAGFlow远程服务的迁移，主要变更包括：

1. **后端变更**:
   - 移除了本地Redis向量存储相关代码
   - 新增RAGFlow服务集成
   - 更新API接口适配RAGFlow

2. **前端变更**:
   - 重构知识库页面
   - 增强文档上传功能
   - 更新API调用适配后端变更

## 使用示例

### 1. 创建数据集

```bash
curl -X POST http://localhost:8081/api/ragflow/datasets \
  -H "Content-Type: application/json" \
  -d '{"name": "我的知识库", "description": "项目文档"}'
```

### 2. 上传文档

```bash
curl -X POST http://localhost:8081/api/chat/session/1/upload \
  -F "file=@document.pdf" \
  -F "datasetId=your_dataset_id"
```

### 3. 检索知识

```bash
curl -X POST http://localhost:8081/api/ragflow/search \
  -H "Content-Type: application/json" \
  -d '{"question": "什么是RAGFlow？"}'
```

## 注意事项

1. **RAGFlow服务**：确保RAGFlow服务正常运行且可访问
2. **API Key**：如需认证，请正确配置API Key
3. **文件上传**：支持最大50MB的文件，格式包括PDF、TXT、DOC、DOCX等
4. **异步处理**：文档上传后会异步解析和向量化，可能需要一些时间
5. **服务依赖**：确保Nacos、MySQL、Redis等服务正常运行

## 开发说明

### 代码结构

- `ai-service/` - AI核心服务
  - `src/main/java/com/feiyu/aiservice/` - Java源代码
  - `src/main/resources/` - 配置文件

- `gateway-service/` - API网关服务
  - `src/main/java/com/feiyu/gateway/` - Java源代码
  - `src/main/resources/` - 配置文件

- `ai-front-end/` - 前端应用
  - `src/` - 前端源代码
  - `public/` - 静态资源

### 开发规范

- 遵循Spring Boot和Vue 3最佳实践
- 使用Lombok简化Java代码
- 保持代码风格一致
- 编写单元测试和集成测试

## 许可证

[MIT](LICENSE)

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启一个Pull Request

## 联系方式

如有问题或建议，请通过以下方式联系我们：

- 项目地址：[AI Cloud](https://github.com/yourusername/ai_cloud)
- 邮箱：your_email@example.com
