<template>
  <div class="kb-list-page">
    <div class="kb-list-header">
      <h2>知识库</h2>
      <el-button type="primary" @click="showCreateDatasetDialog = true">
        <el-icon><Plus /></el-icon>
        创建知识库
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
            <el-button size="small" type="danger" @click.stop="deleteDataset(dataset.id)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>
    <!-- 创建知识库对话框 -->
    <el-dialog v-model="showCreateDatasetDialog" title="创建知识库" width="400px">
      <el-form :model="createDatasetForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createDatasetForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createDatasetForm.description" type="textarea" placeholder="请输入知识库描述" />
        </el-form-item>
        <el-form-item label="嵌入模型">
          <el-input v-model="createDatasetForm.embedding_model" placeholder="如 text-embedding-v4@Tongyi-Qianwen" />
        </el-form-item>
        <el-form-item label="切片方法">
          <el-select v-model="createDatasetForm.chunk_method" placeholder="请选择切片方法">
            <el-option label="General" value="naive" />
            <el-option label="Book" value="book" />
            <el-option label="Email" value="email" />
            <el-option label="Laws" value="laws" />
            <el-option label="Manual" value="manual" />
            <el-option label="One" value="one" />
            <el-option label="Paper" value="paper" />
            <el-option label="Picture" value="picture" />
            <el-option label="Presentation" value="presentation" />
            <el-option label="QA" value="qa" />
            <el-option label="Table" value="table" />
            <el-option label="Tag" value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="分块配置">
          <el-input v-model="createDatasetForm.parser_config" type="textarea" placeholder='请输入JSON格式的分块配置，如 {"chunk_token_num":128, ...}' />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const datasets = ref([])
const showCreateDatasetDialog = ref(false)
const createDatasetForm = ref({ name: '', description: '', embedding_model: '', chunk_method: '', parser_config: '' })
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
    console.error('加载知识库失败:', e)
  }
}

function goToDetail(datasetId) {
  router.push(`/kb/${datasetId}`)
}

async function createDataset() {
  if (!createDatasetForm.value.name) return
  creatingDataset.value = true
  try {
    const token = localStorage.getItem('access_token')
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }
    // 只传name字段
    const body = {
      name: createDatasetForm.value.name
    }
    const res = await fetch('/ai/ragflow/datasets', {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (data.success || data.code === 0) {
      ElMessage.success('创建成功')
      showCreateDatasetDialog.value = false
      createDatasetForm.value = { name: '' }
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

async function deleteDataset(id) {
  try {
    await ElMessageBox.confirm('确定要删除该知识库吗？此操作不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const token = localStorage.getItem('access_token')
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }
    const res = await fetch('/ai/ragflow/datasets', {
      method: 'DELETE',
      headers,
      body: JSON.stringify({ ids: [id] })
    })
    const data = await res.json()
    if (data.success || data.code === 0) {
      ElMessage.success('删除成功')
      loadDatasets()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
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