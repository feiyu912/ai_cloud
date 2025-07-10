<template>
  <div class="kb-page">
    <div class="kb-search-bar">
      <el-input v-model="query" placeholder="请输入要检索的内容..." clearable @keyup.enter.native="searchKb" style="width: 400px;" />
      <el-button type="primary" @click="searchKb">搜索</el-button>
      <el-button type="success" @click="showUploadDialog = true">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
      <el-button type="info" @click="checkRAGFlowHealth">
        <el-icon><Connection /></el-icon>
        检查服务状态
      </el-button>
      <el-button type="warning" @click="showDatasetDialog = true">
        <el-icon><Folder /></el-icon>
        管理数据集
      </el-button>
    </div>
    
    <!-- 上传文件对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档到知识库" width="500px">
      <div class="upload-area">
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="选择数据集">
            <el-select v-model="uploadForm.datasetId" placeholder="请选择数据集" style="width: 100%;">
              <el-option
                v-for="dataset in datasets"
                :key="dataset.id"
                :label="dataset.name"
                :value="dataset.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上传文件">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :on-change="onFileChange"
              :file-list="fileList"
              drag
              accept=".txt,.pdf,.doc,.docx"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 txt、pdf、doc、docx 格式文件
                </div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile || !uploadForm.datasetId">
            上传
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 数据集管理对话框 -->
    <el-dialog v-model="showDatasetDialog" title="数据集管理" width="600px">
      <div class="dataset-management">
        <div class="dataset-header">
          <h3>现有数据集</h3>
          <el-button type="primary" @click="showCreateDatasetDialog = true">
            <el-icon><Plus /></el-icon>
            创建数据集
          </el-button>
        </div>
        <div class="dataset-list">
          <el-card v-for="dataset in datasets" :key="dataset.id" class="dataset-item">
            <div class="dataset-info">
              <h4>{{ dataset.name }}</h4>
              <p>{{ dataset.description || '暂无描述' }}</p>
              <div class="dataset-stats">
                <el-tag size="small">文档: {{ dataset.document_count || 0 }}</el-tag>
                <el-tag size="small" type="success">片段: {{ dataset.chunk_count || 0 }}</el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-dialog>

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

    <div class="kb-result-list">
      <el-empty v-if="!loading && results.length === 0 && searched" description="未找到相关内容" />
      <el-skeleton v-if="loading" rows="4" animated />
      <el-card v-for="(item, idx) in results" :key="idx" class="kb-result-item">
        <div v-html="item.highlight || item.text"></div>
        <div class="kb-score">相关度：{{ (item.score * 100).toFixed(2) }}%</div>
        <div class="kb-source">来源：{{ item.document_name || '未知文档' }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled, Connection, Folder, Plus } from '@element-plus/icons-vue'

const query = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)

// 上传相关状态
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const uploading = ref(false)
const uploadRef = ref()
const uploadForm = ref({
  datasetId: ''
})

// 数据集管理相关状态
const showDatasetDialog = ref(false)
const showCreateDatasetDialog = ref(false)
const datasets = ref([])
const createDatasetForm = ref({
  name: '',
  description: ''
})
const creatingDataset = ref(false)

onMounted(() => {
  loadDatasets()
})

async function loadDatasets() {
  try {
    const res = await fetch('/ai/api/ragflow/datasets')
    const data = await res.json()
    if (data.success) {
      datasets.value = data.data || []
    }
  } catch (e) {
    console.error('加载数据集失败:', e)
  }
}

async function searchKb() {
  if (!query.value.trim()) {
    ElMessage.warning('请输入检索内容')
    return
  }
  loading.value = true
  searched.value = true
  results.value = []
  try {
    const res = await fetch('/ai/api/ragflow/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        question: query.value
      })
    })
    const data = await res.json()
    if (data.success) {
      results.value = data.data || []
    } else {
      ElMessage.error(data.error || '检索失败')
    }
  } catch (e) {
    ElMessage.error('检索失败')
  } finally {
    loading.value = false
  }
}

// 文件选择处理
function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

// 检查RAGFlow健康状态
async function checkRAGFlowHealth() {
  try {
    const res = await fetch('/ai/api/ragflow/health')
    const data = await res.json()
    if (data.healthy) {
      ElMessage.success('RAGFlow服务正常')
    } else {
      ElMessage.error('RAGFlow服务不可用')
    }
  } catch (e) {
    ElMessage.error('RAGFlow连接失败')
  }
}

// 上传文件处理
async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!uploadForm.value.datasetId) {
    ElMessage.warning('请选择数据集')
    return
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('datasetId', uploadForm.value.datasetId)
    
    const res = await fetch('/ai/api/chat/session/1/upload', {
      method: 'POST',
      body: formData
    })
    
    if (res.ok) {
      const text = await res.text()
      ElMessage.success('文件上传成功！')
      showUploadDialog.value = false
      // 清空选择
      selectedFile.value = null
      fileList.value = []
      uploadForm.value.datasetId = ''
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    } else {
      const errorText = await res.text()
      ElMessage.error('上传失败：' + errorText)
    }
  } catch (e) {
    ElMessage.error('上传失败：' + e.message)
  } finally {
    uploading.value = false
  }
}

// 创建数据集
async function createDataset() {
  if (!createDatasetForm.value.name.trim()) {
    ElMessage.warning('请输入数据集名称')
    return
  }
  
  creatingDataset.value = true
  try {
    const res = await fetch('/ai/api/ragflow/datasets', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(createDatasetForm.value)
    })
    
    const data = await res.json()
    if (data.success) {
      ElMessage.success('数据集创建成功！')
      showCreateDatasetDialog.value = false
      createDatasetForm.value = { name: '', description: '' }
      loadDatasets() // 重新加载数据集列表
    } else {
      ElMessage.error('创建失败：' + (data.error || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('创建失败：' + e.message)
  } finally {
    creatingDataset.value = false
  }
}
</script>

<style scoped>
.kb-page {
  max-width: 800px;
  margin: 32px auto;
  padding: 24px 0;
}
.kb-search-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  gap: 12px;
  flex-wrap: wrap;
}
.kb-result-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.kb-result-item {
  font-size: 1.08rem;
  line-height: 1.7;
  background: #f8fafc;
  border-radius: 10px;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.04);
  padding: 18px 22px;
}
.kb-score {
  color: #90a4ae;
  font-size: 13px;
  margin-top: 6px;
}
.kb-source {
  color: #607d8b;
  font-size: 12px;
  margin-top: 4px;
}

.upload-area {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dataset-management {
  padding: 20px 0;
}

.dataset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dataset-header h3 {
  margin: 0;
}

.dataset-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dataset-item {
  border: 1px solid #e0e0e0;
}

.dataset-info h4 {
  margin: 0 0 8px 0;
  color: #1976d2;
}

.dataset-info p {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
}

.dataset-stats {
  display: flex;
  gap: 8px;
}
</style> 