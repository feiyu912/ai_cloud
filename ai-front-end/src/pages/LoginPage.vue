<template>
  <div class="login-page">
    <div class="login-box">
      <h2>登录</h2>
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-link">
        还没有账号？<a @click="goRegister">去注册</a>
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

async function onLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  const res = await fetch('/ai/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: username.value, password: password.value })
  })
  const data = await res.json()
  if (data.success) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', username.value)
    ElMessage.success('登录成功')
    router.push('/')
  } else {
    ElMessage.error(data.msg || '登录失败')
  }
}
function goRegister() {
  router.push('/register')
}
</script>
<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
}
.login-box {
  width: 340px;
  padding: 36px 32px 24px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 16px 0 rgba(25,118,210,0.08);
  text-align: center;
}
.login-link {
  margin-top: 18px;
  color: #90a4ae;
  font-size: 14px;
}
.login-link a {
  color: #1976d2;
  cursor: pointer;
  margin-left: 4px;
}
</style> 