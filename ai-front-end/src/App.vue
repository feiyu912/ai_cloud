<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function logout() {
  localStorage.removeItem('token')
  router.push('/login')
  window.scrollTo(0, 0)
}
// 无需引入原有组件，全部由路由切换
</script>

<template>
  <el-container style="min-height: 100vh;">
    <el-aside width="200px" style="background: #e3f2fd; color: #1976d2; display: flex; flex-direction: column;">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; font-weight: bold; letter-spacing: 2px; color: #1976d2;">AI</div>
      <el-menu :default-active="$route.path" router background-color="#e3f2fd" text-color="#1976d2" active-text-color="#1976d2" style="border-right: none;">
        <el-menu-item index="/">对话</el-menu-item>
        <el-menu-item index="/knowledge-base">知识库</el-menu-item>
      </el-menu>
      <div style="flex:1"></div>
      <el-button type="primary" @click="logout" size="small" style="margin: 20px;">退出登录</el-button>
    </el-aside>
    <el-container>
      <el-main style="background: #f5f6fa; padding: 0; min-height: 100vh;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
html, body, #app, .el-container, .el-main {
  height: 100%;
  min-height: 100%;
  padding: 0;
  margin: 0;
}
.el-aside {
  box-shadow: 2px 0 8px 0 rgba(25,118,210,0.04);
}
</style>