<template>
  <div class="register-page">
    <div class="register-box">
      <h2>注册</h2>
      <el-form @submit.prevent="onRegister">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onRegister" style="width:100%">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="register-link">
        已有账号？<a @click="goLogin">去登录</a>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const username = ref('')
const password = ref('')
const router = useRouter()

async function onRegister() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  const res = await fetch('/ai/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: username.value, password: password.value })
  })
  const data = await res.json()
  if (data.success) {
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } else {
    ElMessage.error(data.msg || '注册失败')
  }
}
function goLogin() {
  router.push('/login')
}
</script>
<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
}
.register-box {
  width: 340px;
  padding: 36px 32px 24px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 16px 0 rgba(25,118,210,0.08);
  text-align: center;
}
.register-link {
  margin-top: 18px;
  color: #90a4ae;
  font-size: 14px;
}
.register-link a {
  color: #1976d2;
  cursor: pointer;
  margin-left: 4px;
}
</style> 