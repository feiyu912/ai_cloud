<template>
  <div class="upload-page">
    <h2>文档上传</h2>
    <div class="upload-form">
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
        <el-form-item label="选择文件">
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
        <el-form-item>
          <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile || !uploadForm.datasetId">
            上传
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-if="result" :class="['result-message', resultColor]">{{ result }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const uploadForm = ref({
  datasetId: ''
})
const selectedFile = ref(null)
const fileList = ref([])
const result = ref('')
const resultColor = ref('black')
const uploading = ref(false)
const uploadRef = ref()
const datasets = ref([])

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

function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

async function handleUpload() {
  if (!selectedFile.value) {
    result.value = '请先选择文件！'
    resultColor.value = 'red'
    return
  }
  if (!uploadForm.value.datasetId) {
    result.value = '请选择数据集！'
    resultColor.value = 'red'
    return
  }
  
  uploading.value = true
  result.value = ''
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('datasetId', uploadForm.value.datasetId)
    
    const res = await fetch('/ai/api/chat/session/1/upload', {
      method: 'POST',
      body: formData
    })
    
    const text = await res.text()
    result.value = text
    resultColor.value = res.ok ? 'green' : 'red'
    
    if (res.ok) {
      ElMessage.success('文件上传成功！')
      // 清空选择
      selectedFile.value = null
      fileList.value = []
      uploadForm.value.datasetId = ''
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    }
  } catch (e) {
    result.value = '上传失败：' + e.message
    resultColor.value = 'red'
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-page {
  max-width: 600px;
  margin: 32px auto;
  padding: 24px;
}

.upload-form {
  margin: 24px 0;
}

.result-message {
  margin-top: 16px;
  padding: 12px;
  border-radius: 4px;
  font-weight: 500;
}

.result-message.green {
  background-color: #f0f9ff;
  color: #059669;
  border: 1px solid #34d399;
}

.result-message.red {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #f87171;
}
</style> 