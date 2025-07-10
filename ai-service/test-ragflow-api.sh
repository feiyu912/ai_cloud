#!/bin/bash

# RAGFlow API 测试脚本
# 使用方法: ./test-ragflow-api.sh

BASE_URL="http://localhost:8081"

echo "=== RAGFlow API 测试 ==="

# 1. 健康检查
echo "1. 测试健康检查..."
curl -X GET "$BASE_URL/api/ragflow/health" \
  -H "Content-Type: application/json" \
  -w "\n状态码: %{http_code}\n\n"

# 2. 获取数据集列表
echo "2. 获取数据集列表..."
curl -X GET "$BASE_URL/api/ragflow/datasets" \
  -H "Content-Type: application/json" \
  -w "\n状态码: %{http_code}\n\n"

# 3. 创建数据集
echo "3. 创建测试数据集..."
DATASET_RESPONSE=$(curl -s -X POST "$BASE_URL/api/ragflow/datasets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试数据集",
    "description": "用于API测试的数据集"
  }')

echo "$DATASET_RESPONSE"
echo "状态码: $?"

# 提取数据集ID（如果创建成功）
DATASET_ID=$(echo "$DATASET_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
if [ -n "$DATASET_ID" ]; then
    echo "数据集ID: $DATASET_ID"
    
    # 4. 测试知识库检索
    echo "4. 测试知识库检索..."
    curl -X POST "$BASE_URL/api/ragflow/search" \
      -H "Content-Type: application/json" \
      -d '{
        "question": "什么是人工智能？"
      }' \
      -w "\n状态码: %{http_code}\n\n"
    
    # 5. 测试文档上传（需要实际文件）
    echo "5. 测试文档上传..."
    echo "注意: 需要提供实际的PDF或TXT文件"
    echo "示例命令:"
    echo "curl -X POST \"$BASE_URL/api/chat/session/1/upload\" \\"
    echo "  -F \"file=@test.pdf\" \\"
    echo "  -F \"datasetId=$DATASET_ID\""
    
else
    echo "数据集创建失败，跳过后续测试"
fi

echo "=== 测试完成 ===" 