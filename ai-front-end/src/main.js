import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import ChatPage from './pages/ChatPage.vue'
import KnowledgeBaseListPage from './pages/KnowledgeBaseListPage.vue'
import KnowledgeBaseDetailPage from './pages/KnowledgeBaseDetailPage.vue'
import LoginPage from './pages/LoginPage.vue'
import RegisterPage from './pages/RegisterPage.vue'

const routes = [
  { path: '/', component: ChatPage, name: 'Chat', meta: { requiresAuth: true } },
  { path: '/kb', component: KnowledgeBaseListPage, name: 'KnowledgeBaseList', meta: { requiresAuth: true } },
  { path: '/kb/:datasetId', component: KnowledgeBaseDetailPage, name: 'KnowledgeBaseDetail', meta: { requiresAuth: true } },
  { path: '/knowledge-base', redirect: '/kb' },
  { path: '/login', component: LoginPage, name: 'Login' },
  { path: '/register', component: RegisterPage, name: 'Register' },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

createApp(App).use(router).use(ElementPlus).mount('#app')
