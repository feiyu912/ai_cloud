<template>
  <div class="kb-detail-page">
    <div class="kb-detail-header">
      <el-button class="kb-back-btn" type="default" @click="goBack">
        <el-icon style="vertical-align: middle; margin-right: 4px;"><ArrowLeft /></el-icon>
        <span style="vertical-align: middle;">返回知识库</span>
      </el-button>
    </div>
    <el-row>
      <el-col :span="4">
        <el-menu :default-active="activeTab" @select="activeTab = $event" class="kb-detail-menu">
          <el-menu-item index="files">数据集文档</el-menu-item>
          <el-menu-item index="retrieval">检索测试</el-menu-item>
        </el-menu>
      </el-col>
      <el-col :span="20">
        <div v-if="activeTab === 'files'">
          <kb-files-tab :dataset-id="datasetId" />
        </div>
        <div v-else-if="activeTab === 'retrieval'">
          <kb-retrieval-tab :dataset-id="datasetId" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KbFilesTab from '../components/KbFilesTab.vue'
import KbRetrievalTab from '../components/KbRetrievalTab.vue'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const datasetId = route.params.datasetId
const activeTab = ref('files')

function goBack() {
  router.push('/kb')
}
</script>

<style scoped>
.kb-detail-page {
  padding: 32px;
}
.kb-detail-header {
  margin-bottom: 8px;
}
.kb-back-btn {
  display: inline-flex;
  align-items: center;
  font-size: 16px;
  padding: 8px 24px;
  border-radius: 8px;
  background: #f4f8ff;
  color: #409eff;
  border: 1px solid #dbeafe;
  transition: background 0.2s;
}
.kb-back-btn:hover {
  background: #e0edff;
  color: #337ecc;
}
.kb-detail-menu {
  min-height: 400px;
}
</style> 