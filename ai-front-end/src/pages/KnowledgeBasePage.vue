<template>
  <div style="padding: 40px; text-align: center;">
    <el-result icon="info" title="页面已迁移" sub-title="请前往新的知识库列表页进行操作。">
      <template #extra>
        <el-button type="primary" @click="goToKbList">前往知识库首页</el-button>
      </template>
    </el-result>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goToKbList() {
  router.push('/kb')
}
</script> 