<template>
  <div class="kb-retrieval-tab">
    <el-form :model="form" label-width="120px" class="kb-retrieval-form">
      <el-form-item label="检索问题" required>
        <el-input v-model="form.question" placeholder="请输入检索问题" clearable />
      </el-form-item>
      <el-form-item label="选择文档">
        <el-select v-model="form.document_ids" multiple filterable placeholder="可多选文档">
          <el-option v-for="doc in documents" :key="doc.id" :label="doc.name" :value="doc.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="相似度阈值">
        <el-slider v-model="form.similarity_threshold" :min="0" :max="1" :step="0.01" style="width: 200px;" />
        <span style="margin-left: 8px;">{{ form.similarity_threshold }}</span>
      </el-form-item>
      <el-form-item label="关键词相似度权重">
        <el-slider v-model="form.vector_similarity_weight" :min="0" :max="1" :step="0.01" style="width: 200px;" />
        <span style="margin-left: 8px;">{{ form.vector_similarity_weight }}</span>
      </el-form-item>
      <el-form-item label="Rerank模型">
        <el-input v-model="form.rerank_id" placeholder="可选，填写模型ID" />
      </el-form-item>
      <el-form-item label="是否高亮">
        <el-switch v-model="form.highlight" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleRetrieval" :loading="loading">检索</el-button>
      </el-form-item>
    </el-form>
    <div class="kb-retrieval-result">
      <el-empty v-if="!loading && results.length === 0 && searched" description="未找到相关内容" />
      <el-skeleton v-if="loading" rows="4" animated />
      <el-card v-for="(item, idx) in results" :key="idx" class="kb-retrieval-item">
        <div v-html="item.highlight || item.content"></div>
        <div class="kb-score">相关度：{{ (item.similarity * 100).toFixed(2) }}%</div>
        <div class="kb-source">来源：{{ item.document_keyword || '未知文档' }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  datasetId: String
})

const form = ref({
  question: '',
  document_ids: [],
  similarity_threshold: 0.2,
  vector_similarity_weight: 0.3,
  rerank_id: '',
  highlight: false
})
const documents = ref([])
const results = ref([])
const loading = ref(false)
const searched = ref(false)

onMounted(() => {
  loadDocuments()
})

watch(() => props.datasetId, () => {
  loadDocuments()
})

async function loadDocuments() {
  if (!props.datasetId) return
  try {
    const res = await fetch(`/ai/ragflow/datasets/${props.datasetId}/documents`)
    const data = await res.json()
    if (data.success || data.code === 0) {
      documents.value = data.data?.docs || []
    }
  } catch (e) {
    ElMessage.error('加载文档失败')
  }
}

async function handleRetrieval() {
  if (!form.value.question.trim()) {
    ElMessage.warning('请输入检索问题')
    return
  }
  loading.value = true
  searched.value = true
  results.value = []
  try {
    const body = {
      question: form.value.question,
      dataset_ids: [props.datasetId],
      document_ids: form.value.document_ids,
      similarity_threshold: form.value.similarity_threshold,
      vector_similarity_weight: form.value.vector_similarity_weight,
      rerank_id: form.value.rerank_id,
      highlight: form.value.highlight
    }
    const res = await fetch('/ai/ragflow/retrieval', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if ((data.success || data.code === 0) && data.data) {
      results.value = data.data.chunks || []
    } else {
      ElMessage.error(data.message || '检索失败')
    }
  } catch (e) {
    ElMessage.error('检索失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.kb-retrieval-form {
  max-width: 600px;
  margin-bottom: 24px;
}
.kb-retrieval-result {
  margin-top: 24px;
}
.kb-retrieval-item {
  margin-bottom: 16px;
}
</style> 