<template>
  <div class="kb-list-page">
    <div class="kb-list-header">
      <h2>知识库</h2>
      <el-button type="primary" @click="showCreateDatasetDialog = true">
        <el-icon><Plus /></el-icon>
        创建数据集
      </el-button>
    </div>
    <div class="kb-dataset-list">
      <el-card v-for="dataset in datasets" :key="dataset.id" class="kb-dataset-card" @click="goToDetail(dataset.id)">
        <div class="kb-dataset-info">
          <h3>{{ dataset.name }}</h3>
          <p>{{ dataset.description || '暂无描述' }}</p>
          <div class="kb-dataset-stats">
            <el-tag size="small">文档: {{ dataset.document_count || 0 }}</el-tag>
            <el-tag size="small" type="success">片段: {{ dataset.chunk_count || 0 }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>
    <!-- 创建数据集对话框 -->
    <el-dialog v-model="showCreateDatasetDialog" title="创建数据集" width="400px">
      <el-form :model="createDatasetForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createDatasetForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createDatasetForm.description" type="textarea" placeholder="请输入数据集描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDatasetDialog = false">取消</el-button>
          <el-button type="primary" @click="createDataset" :loading="creatingDataset" :disabled="!createDatasetForm.name">
            创建
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const datasets = ref([])
const showCreateDatasetDialog = ref(false)
const createDatasetForm = ref({ name: '', description: '' })
const creatingDataset = ref(false)

onMounted(() => {
  loadDatasets()
})

async function loadDatasets() {
  try {
    const res = await fetch('/ai/ragflow/datasets')
    const data = await res.json()
    if (data.success) {
      datasets.value = data.data || []
    }
  } catch (e) {
    console.error('加载数据集失败:', e)
  }
}

function goToDetail(datasetId) {
  router.push(`/kb/${datasetId}`)
}

async function createDataset() {
  if (!createDatasetForm.value.name) return
  creatingDataset.value = true
  try {
    const res = await fetch('/ai/ragflow/datasets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: createDatasetForm.value.name,
        description: createDatasetForm.value.description
      })
    })
    const data = await res.json()
    if (data.success || data.code === 0) {
      ElMessage.success('创建成功')
      showCreateDatasetDialog.value = false
      createDatasetForm.value = { name: '', description: '' }
      loadDatasets()
    } else {
      ElMessage.error(data.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    creatingDataset.value = false
  }
}
</script>

<style scoped>
.kb-list-page {
  padding: 32px;
}
.kb-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.kb-dataset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}
.kb-dataset-card {
  width: 320px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.kb-dataset-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
}
.kb-dataset-info h3 {
  margin: 0 0 8px 0;
}
.kb-dataset-stats {
  margin-top: 8px;
}
</style> 