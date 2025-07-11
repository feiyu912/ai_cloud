<template>
  <div class="kb-files-tab">
    <div class="kb-files-header">
      <el-button type="primary" @click="showUploadDialog = true">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
    </div>
    <div class="kb-files-table-wrapper">
      <el-table :data="files" style="width: 100%; margin-top: 16px;">
        <el-table-column prop="name" label="名称">
          <template #default="scope">
            <el-link type="primary" @click="onFileClick(scope.row)">{{ scope.row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="chunk_count" label="分块数" />
        <el-table-column prop="create_date" label="上传日期" />
        <el-table-column prop="chunk_method" label="切片方法" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" type="danger" @click="deleteFile(scope.row)">删除</el-button>
            <el-button size="small" @click="downloadFile(scope.row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 上传文件对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
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
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile">
            上传
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElLink } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'

const props = defineProps({
  datasetId: String
})

const files = ref([])
const showUploadDialog = ref(false)
const fileList = ref([])
const selectedFile = ref(null)
const uploading = ref(false)
const uploadRef = ref()

watch(() => props.datasetId, () => {
  loadFiles()
}, { immediate: true })

async function loadFiles() {
  if (!props.datasetId) return
  try {
    const res = await fetch(`/ai/ragflow/datasets/${props.datasetId}/documents`)
    const data = await res.json()
    if (data.success || data.code === 0) {
      files.value = data.data?.docs || []
    }
  } catch (e) {
    ElMessage.error('加载文档失败')
  }
}

function onFileChange(file) {
  selectedFile.value = file.raw
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  try {
    const token = localStorage.getItem('access_token')
    const res = await fetch(`/ai/ragflow/datasets/${props.datasetId}/upload`, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      body: formData
    })
    const data = await res.json()
    if (data.success || data.code === 0) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      fileList.value = []
      selectedFile.value = null
      loadFiles()
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

async function deleteFile(row) {
  try {
    const res = await fetch(`/ai/ragflow/datasets/${props.datasetId}/documents`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ids: [row.id] })
    })
    const data = await res.json()
    if (data.success || data.code === 0) {
      ElMessage.success('删除成功')
      loadFiles()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function onFileClick(row) {
  // TODO: 跳转到chunk界面，后续实现
  ElMessage.info(`点击了文档：${row.name}`)
}

async function downloadFile(row) {
  try {
    const url = `/ai/ragflow/datasets/${props.datasetId}/documents/${row.id}`
    const res = await fetch(url, {
      method: 'GET',
    })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = window.URL.createObjectURL(blob)
    a.download = row.name || 'document'
    a.click()
    window.URL.revokeObjectURL(a.href)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}
</script>

<style scoped>
.kb-files-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin: 24px 24px 0 0;
}
.kb-files-table-wrapper {
  padding-left: 24px;
}
</style> 