<template>
  <div class="chat-layout">
    <div class="chat-sessions">
      <div class="session-header">
        <span style="font-weight:600; font-size:1.1rem;">历史会话</span>
        <el-button circle size="small" type="primary" @click="handleCreateClick" style="margin-left:auto; font-size:15px; font-weight:500; letter-spacing:2px; width:40px; height:40px; display:flex; align-items:center; justify-content:center;">新建</el-button>
      </div>
      <el-scrollbar class="session-list">
        <div v-for="session in sessions" :key="session.id" :class="['session-item', session.id === currentSessionId ? 'active' : '']">
          <div class="session-item-main" @click="switchSession(session.id)">
            <span v-if="editSessionId !== session.id" class="session-title">{{ session.title }}</span>
            <el-input v-else v-model="editSessionTitle" size="small" @blur="saveSessionTitle(session)" @keyup.enter="saveSessionTitle(session)" style="width:110px;" />
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <el-popconfirm title="确定删除该会话？" @confirm="deleteSession(session.id)">
            <template #reference>
              <span class="session-action session-x" @click.stop>x</span>
            </template>
          </el-popconfirm>
        </div>
      </el-scrollbar>
    </div>
    <div class="chat-page">
              <div class="chat-header">
          <el-icon style="vertical-align: middle;"><ChatLineRound /></el-icon>
          <span style="margin-left: 8px; font-size: 1.2rem; font-weight: 600; color: #1976d2;">AI 对话</span>
          <!-- 使用过的MCP工具统计 -->
          <div class="mcp-stats" v-if="usedMcpTools.length > 0">
            <el-tooltip content="本会话使用过的MCP工具" placement="bottom">
              <el-button size="small" type="info" @click="showUsedToolsDialog = true">
                <el-icon><Tools /></el-icon>
                已用工具 ({{ usedMcpTools.length }})
              </el-button>
            </el-tooltip>
          </div>
        </div>
      <div class="chat-messages">
        <div v-for="(msg, idx) in messages" :key="idx">
          <!-- 只在有参考内容的AI回复上方显示参考栏 -->
          <template v-if="msg.role === 'assistant' && msg.reference && msg.reference.length">
            <div class="reference-bar" style="margin-bottom: 8px;">
              <b>本轮AI参考（共{{ msg.reference.filter(item => item.similarity === undefined || item.similarity >= 0.4).length }}条）</b>
              <el-button type="link" @click="msg.referenceCollapsed = !msg.referenceCollapsed" style="margin-left: 8px;">
                {{ msg.referenceCollapsed ? '展开' : '收起' }}
              </el-button>
              <div v-show="!msg.referenceCollapsed" style="display: flex; flex-wrap: wrap; gap: 12px;">
                <div
                  v-for="(item, ridx) in msg.reference.filter(item => item.similarity === undefined || item.similarity >= 0.4)"
                  :key="ridx"
                  class="ref-item"
                  style="background: #f8faff; border: 1px solid #dbeafe; border-radius: 8px; padding: 12px 16px; margin-bottom: 0; min-width: 260px; max-width: 100%; box-sizing: border-box; flex: 1 1 320px; position: relative;"
                >
                  <div style="font-size: 13px; color: #1976d2; margin-bottom: 4px;">
                    <span v-if="item.similarity !== undefined">相似度: {{ (item.similarity * 100).toFixed(1) }}%</span>
                    <span v-if="item.document_keyword || item.document_name || item.file_name" style="margin-left: 12px;">
                      来源: {{ item.document_keyword || item.document_name || item.file_name }}
                    </span>
                    <el-button
                      size="small"
                      type="link"
                      style="position: absolute; top: 8px; right: 8px;"
                      @click="item.collapsed = !item.collapsed"
                    >
                      {{ item.collapsed ? '展开' : '收起' }}
                    </el-button>
                  </div>
                  <div style="font-size: 15px; color: #333; white-space: pre-line;">
                    {{ item.collapsed ? (item.content ? item.content.slice(0, 50) + (item.content.length > 50 ? '...' : '') : '') : item.content }}
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div :class="['msg', msg.role]">
            <div class="msg-bubble">
              <b>{{ msg.role === 'user' ? '我' : 'AI' }}：</b>
              <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)" class="ai-markdown"></div>
              <span v-else>{{ msg.content }}</span>
            </div>
            <!-- MCP工具调用信息显示 -->
            <div v-if="msg.role === 'assistant' && msg.mcpContent" class="mcp-content">
              <div class="mcp-header">
                <el-icon><Tools /></el-icon>
                <span>MCP工具调用</span>
              </div>
              <div class="mcp-body">
                <pre>{{ msg.mcpContent }}</pre>
              </div>
            </div>
            <!-- 知识库引用内容显示 -->
            <!-- <div v-if="msg.role === 'assistant' && msg.reference && msg.reference.length" class="reference-bar">
              <div v-for="ref in msg.reference" :key="ref.text" class="ref-item">
                <span :class="ref.source === 'global' ? 'ref-global' : 'ref-session'">
                  {{ ref.text }}
                </span>
              </div>
            </div> -->
          </div>
        </div>
        <div v-if="streaming" class="msg assistant">
          <div class="msg-bubble">
            <b>AI：</b>
            <div v-html="renderMarkdown(streamingContent)" class="ai-markdown"></div>
          </div>
        </div>
      </div>
      <div class="chat-input-container compact">
        <div class="chat-input-bar-centered">
          <el-input v-model="input" :disabled="streaming" placeholder="请输入你的问题..." clearable @keyup.enter.native="sendMessage" style="width: 400px; margin-right: 12px;" />
          <el-button type="primary" :disabled="!input || streaming || !currentSessionId" @click="sendMessage" style="margin-right: 12px;">发送</el-button>
          <el-button type="success" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <!-- MCP工具多选下拉 -->
          <el-select
            v-model="selectedTools"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择工具（可多选）"
            style="min-width: 220px; max-width: 320px; margin-left: 16px; border-radius: 8px;"
            filterable
            clearable
            tag-type="success"
          >
            <el-option
              v-for="tool in mcpTools"
              :key="tool.name"
              :label="tool.description"
              :value="tool.name"
            >
              <el-icon style="margin-right: 6px;"><Tools /></el-icon>
              {{ tool.description }}
            </el-option>
          </el-select>
          <el-switch v-model="useStream" active-text="流式" inactive-text="非流式" style="margin-left: 16px;" />
        </div>
        <!-- 上传文件对话框 -->
        <el-dialog v-model="showUploadDialog" title="上传文件" width="500px">
          <div class="upload-area">
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
          </div>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="showUploadDialog = false">取消</el-button>
              <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile">
                上传
              </el-button>
            </span>
          </template>
        </el-dialog>
        
        <!-- 使用过的MCP工具对话框 -->
        <el-dialog v-model="showUsedToolsDialog" title="本会话使用过的MCP工具" width="600px">
          <div class="used-tools-list">
            <div v-for="tool in usedMcpTools" :key="tool.name" class="used-tool-item">
              <div class="tool-info">
                <el-icon><Tools /></el-icon>
                <span class="tool-name">{{ tool.name }}</span>
                <span class="tool-description">{{ tool.description }}</span>
              </div>
              <div class="tool-usage">
                <el-tag type="success" size="small">使用 {{ tool.usageCount }} 次</el-tag>
                <span class="last-used">最后使用: {{ formatTime(tool.lastUsed) }}</span>
              </div>
            </div>
            <el-empty v-if="usedMcpTools.length === 0" description="暂无使用过的工具" />
          </div>
        </el-dialog>

        <!-- 新建会话弹窗 -->
        <el-dialog v-model="showCreateSessionDialog" title="新建会话" width="500px">
          <el-form :model="newSessionForm" label-width="80px">
            <el-form-item label="会话名称">
              <el-input v-model="newSessionForm.title" placeholder="请输入会话名称" />
            </el-form-item>
            <el-form-item label="选择知识库" required>
              <el-select
                v-model="newSessionForm.datasetId"
                placeholder="请选择知识库"
                style="width: 100%;"
                @change="val => console.log('选择了知识库', val)"
              >
                <el-option
                  v-for="dataset in datasets"
                  :key="dataset.id"
                  :label="dataset.name"
                  :value="dataset.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="showCreateSessionDialog = false">取消</el-button>
              <el-button type="primary" @click="createSession" :disabled="!newSessionForm.datasetId">
                创建
              </el-button>
            </span>
          </template>
        </el-dialog>
      </div>
      <div class="upload-tip">支持上传文件</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ChatLineRound, Upload, UploadFilled, Tools } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import axios from 'axios'
// 引入marked用于Markdown渲染
import { marked } from 'marked'

const input = ref('')
const messages = ref([])
const streaming = ref(false)
const streamingContent = ref('')
const sessions = ref([])
const currentSessionId = ref(null)
const editSessionId = ref(null)
const editSessionTitle = ref('')
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const uploading = ref(false)
const uploadRef = ref()

// 新建会话弹窗相关变量
const showCreateSessionDialog = ref(false)
const newSessionForm = ref({
  title: '',
  datasetId: ''
})
// MCP工具相关
const mcpTools = ref([]);
const selectedTools = ref([]);
const showUsedToolsDialog = ref(false);
const usedMcpTools = ref([]);
const selectedDatasetIds = ref([]) // 多选知识库ID数组
const datasets = ref([]) // 所有知识库
const useStream = ref(true) // 默认流式

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleString()
}

async function fetchSessions() {
  const token = localStorage.getItem('token')
  const res = await fetch('/ai/chat/sessions', { headers: { Authorization: token } })
  sessions.value = await res.json()
  if (sessions.value.length && !currentSessionId.value) {
    switchSession(sessions.value[0].id)
  }
}

async function switchSession(id) {
  currentSessionId.value = id
  await fetchMessages()
}

async function fetchMessages() {
  const token = localStorage.getItem('token')
  const res = await fetch(`/ai/chat/session/${currentSessionId.value}`, { headers: { Authorization: token } })
  const rawMsgs = await res.json()
  // 解析 reference 字段
  messages.value = rawMsgs.map(msg => {
    let reference = []
    try {
      if (msg.reference) {
        const ref = JSON.parse(msg.reference)
        reference = Array.isArray(ref) ? ref : [ref]
        // 为每条参考内容加collapsed属性，默认折叠
        reference = reference.map(item => ({ ...item, collapsed: true }))
      }
    } catch (e) {}
    return { ...msg, reference, referenceCollapsed: false }
  })
  
  // 统计使用过的MCP工具
  updateUsedMcpTools()
  
  // 调试输出
  console.log('messages:', messages.value)
  window._msgs = messages
}

async function createSession() {
  console.log('创建会话，选中的知识库ID：', newSessionForm.value.datasetId)
  if (!newSessionForm.value.datasetId) {
    ElMessage.warning('请选择知识库')
    return
  }
  const token = localStorage.getItem('token')
  const res = await fetch('/ai/chat/session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({ title: newSessionForm.value.title, dataset_id: newSessionForm.value.datasetId })
  })
  const data = await res.json()
  if (data.success) {
    await fetchSessions()
    currentSessionId.value = data.id
    messages.value = []
    // 关闭弹窗并清空表单
    showCreateSessionDialog.value = false
    newSessionForm.value.title = ''
    newSessionForm.value.datasetId = ''
  } else {
    ElMessage.error(data.msg || '新建会话失败')
  }
}

// 只保留换行和去除**的处理
function removeMarkdownBold(content) {
  return content.replace(/\*\*/g, '').replace(/(\d+\.)/g, '\n$1');
}

// Markdown渲染方法
function renderMarkdown(content) {
  return marked.parse(content || '')
}

async function sendMessage() {
  if (!input.value.trim() || !currentSessionId.value) return
  streamingContent.value = ''
  messages.value.push({ role: 'user', content: input.value })
  streaming.value = true
  const prompt = input.value
  input.value = ''

  try {
    // 1. 先保存用户消息到数据库
    const token = localStorage.getItem('token')
    await fetch(`/ai/chat/session/${currentSessionId.value}/message`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: token },
      body: JSON.stringify({ role: 'user', content: prompt })
    })

    // 2. 先获取AI参考内容
    const refRes = await fetch(`/ai/chat/session/${currentSessionId.value}/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: token },
      body: JSON.stringify({
        question: prompt,
        dataset_ids: selectedDatasetIds.value // 这里传所有知识库ID
      })
    })
    const thisReference = await refRes.json()
    let fixedReference = thisReference
    if (!Array.isArray(thisReference) || thisReference.length === 0) {
      fixedReference = [{ source: 'global', text: '【无检索结果，已兜底，后端返回空数组】' }]
    }

    // 3. 根据useStream选择流式或非流式
    const requestBody = {
      question: prompt,
      dataset_ids: selectedDatasetIds.value, // 自动传所有知识库ID
      tools: selectedTools.value // 传所选MCP工具名
    }
    if (useStream.value) {
      // 流式请求
      const res = await fetch(`/ai/chat/session/${currentSessionId.value}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        },
        body: JSON.stringify(requestBody)
      })
      if (!res.body) throw new Error('无响应流')
      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let done = false
      let buffer = ''
      streamingContent.value = ''
      let finalReply = null
      let finalReference = null
      while (!done) {
        const { value, done: doneReading } = await reader.read()
        done = doneReading
        if (value) {
          buffer += decoder.decode(value, { stream: true })
          let lines = buffer.split(/\r?\n/)
          buffer = lines.pop()
          for (const line of lines) {
            // console.log('SSE原始行:', line);
            const dataMatch = line.match(/data:(.*)$/)
            if (dataMatch && dataMatch[1]) {
              const data = dataMatch[1].trim()
              // 只处理JSON对象
              if (data.startsWith('{')) {
                try {
                  const obj = JSON.parse(data)
                  if (obj.output && obj.output.text) {
                    streamingContent.value += obj.output.text
                  }
                  // 收到最终完整内容
                  if (obj.reply) {
                    finalReply = obj.reply
                    finalReference = obj.reference || []
                  }
                } catch (e) {
                  // 忽略解析失败
                }
              }
            }
          }
        }
      }
      // 流结束后，直接将最终内容push进历史消息，避免内容消失
      if (finalReply !== null) {
        messages.value.push({ role: 'assistant', content: finalReply, reference: finalReference })
      } else if (streamingContent.value) {
        messages.value.push({ role: 'assistant', content: streamingContent.value, reference: [] })
      }
      // 不再依赖fetchMessages刷新，避免流式内容消失
      streaming.value = false
      streamingContent.value = ''
    } else {
      // 非流式请求
      const res = await fetch(`/ai/chat/session/${currentSessionId.value}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        },
        body: JSON.stringify(requestBody)
      })
      const data = await res.json()
      if (data && data.reply) {
        messages.value.push({ role: 'assistant', content: data.reply, reference: data.reference || [] })
      } else {
        ElMessage.error('AI回复失败')
      }
      await fetchMessages()
      streaming.value = false
      streamingContent.value = ''
    }
  } catch (e) {
    streaming.value = false
    streamingContent.value = ''
    ElMessage.error('AI回复失败')
  }
}

function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!currentSessionId.value) {
    ElMessage.warning('请先选择或新建会话')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const token = localStorage.getItem('token')
    // 调试用，打印关键信息
    console.log('token:', token)
    console.log('sessionId:', currentSessionId.value)
    console.log('file:', selectedFile.value)
    const res = await axios.post(`/ai/chat/session/${currentSessionId.value}/upload`, formData, {
      headers: {
        Authorization: token
      }
    })
    if (res.status === 200) {
      ElNotification.success({
        title: '上传成功',
        message: '文件上传成功！本对话知识已更新，下次提问可直接用新文件内容',
        duration: 3000
      })
      showUploadDialog.value = false
      selectedFile.value = null
      fileList.value = []
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    } else {
      ElMessage.error('上传失败：' + (res.data || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data || e.message))
  } finally {
    uploading.value = false
  }
}

function startEditSession(session) {
  editSessionId.value = session.id
  editSessionTitle.value = session.title
}

async function saveSessionTitle(session) {
  if (!editSessionTitle.value.trim() || editSessionTitle.value === session.title) {
    editSessionId.value = null
    return
  }
  const token = localStorage.getItem('token')
  const res = await fetch(`/ai/chat/session/${session.id}/rename`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({ title: editSessionTitle.value })
  })
  const data = await res.json()
  if (data.success) {
    session.title = editSessionTitle.value
    ElMessage.success('重命名成功')
    await fetchSessions()
  } else {
    ElMessage.error(data.msg || '重命名失败')
  }
  editSessionId.value = null
}

async function deleteSession(id) {
  const token = localStorage.getItem('token')
  const res = await fetch(`/ai/chat/session/${id}`, {
    method: 'DELETE',
    headers: { Authorization: token }
  })
  const data = await res.json()
  if (data.success) {
    ElMessage.success('删除成功')
    await fetchSessions()
    if (currentSessionId.value === id) {
      if (sessions.value.length) {
        currentSessionId.value = sessions.value[0].id
        await fetchMessages()
      } else {
        currentSessionId.value = null
        messages.value = []
      }
    }
  } else {
    ElMessage.error(data.msg || '删除失败')
  }
}

function handleCreateClick() {
  console.log('点击新建前', showCreateSessionDialog.value)
  showCreateSessionDialog.value = true
  console.log('点击新建后', showCreateSessionDialog.value)
}

onMounted(() => {
  fetchSessions()
  fetchMcpTools()
  fetchDatasets() // 页面加载时自动获取知识库列表并默认全选
})

async function fetchMcpTools() {
  try {
    const res = await fetch('/ai/mcp/tools');
    const allTools = await res.json();
    // 只保留本地工具
    mcpTools.value = allTools.filter(tool => [
      'time-mcp',
      'random-mcp',
      'filesystem-mcp',
      'unitconvert-mcp',
      'mysql-mcp'
    ].includes(tool.name));
  } catch (e) {
    mcpTools.value = [];
  }
}

// 统计使用过的MCP工具
function updateUsedMcpTools() {
  const toolUsage = new Map();
  
  // 遍历所有AI消息，统计MCP工具使用情况
  messages.value.forEach(msg => {
    if (msg.role === 'assistant' && msg.mcpContent) {
      // 解析mcpContent，提取工具名称
      const toolNames = extractToolNamesFromMcpContent(msg.mcpContent);
      toolNames.forEach(toolName => {
        if (!toolUsage.has(toolName)) {
          toolUsage.set(toolName, {
            name: toolName,
            description: getToolDescription(toolName),
            usageCount: 0,
            lastUsed: msg.createdAt || new Date()
          });
        }
        const tool = toolUsage.get(toolName);
        tool.usageCount++;
        if (msg.createdAt && new Date(msg.createdAt) > new Date(tool.lastUsed)) {
          tool.lastUsed = msg.createdAt;
        }
      });
    }
  });
  
  usedMcpTools.value = Array.from(toolUsage.values());
}

// 从MCP内容中提取工具名称
function extractToolNamesFromMcpContent(mcpContent) {
  const toolNames = [];
  // 匹配【工具】xxx-mcp: 格式
  const regex = /【工具】([\w\-]+-mcp):/g;
  let match;
  while ((match = regex.exec(mcpContent)) !== null) {
    toolNames.push(match[1]);
  }
  return toolNames;
}

// 获取工具描述
function getToolDescription(toolName) {
  const tool = mcpTools.value.find(t => t.name === toolName);
  return tool ? tool.description : toolName;
}

// 确保 datasets 和 selectedDatasetIds 已声明
// 只补充 fetchDatasets 方法
async function fetchDatasets() {
  const token = localStorage.getItem('token')
  const res = await fetch('/ai/ragflow/datasets', { headers: { Authorization: token } })
  const data = await res.json()
  if (data && data.data && data.data.length) {
    datasets.value = data.data
    // 自动全选所有知识库ID
    selectedDatasetIds.value = data.data.map(ds => ds.id)
  }
}

watch(messages, (val) => {
  if (val && val.length) {
    val.forEach((msg, idx) => {
      if (msg.reference && msg.reference.length) {
        msg.reference.forEach((item, ridx) => {
          console.log('AI参考[' + idx + '-' + ridx + ']:', item);
        });
      }
    });
  }
});
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  background: #f5f6fa;
}
.chat-sessions {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  padding: 0;
  flex-shrink: 0;
}
.session-header {
  display: flex;
  align-items: center;
  padding: 18px 16px 10px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8fafc;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.session-item {
  position: relative;
  padding: 14px 18px 10px 18px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background 0.2s, border-color 0.2s;
  display: flex;
  flex-direction: column;
  background: #fff;
  margin-bottom: 6px;
  border-radius: 8px;
}
.session-item.active {
  background: #e3f2fd;
  border-left: 3px solid #1976d2;
}
.session-item-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.session-title {
  font-size: 1rem;
  font-weight: 500;
  color: #1976d2;
  margin-bottom: 2px;
  word-break: break-all;
}
.session-time {
  font-size: 12px;
  color: #90a4ae;
}
.chat-page {
  flex: 1 1 0%;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6fa;
  min-width: 0;
  max-width: 100vw;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  flex-shrink: 0;
  z-index: 10;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 100px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px; /* 原来16px，减半 */
  background: #f4f6fa;
}

.chat-input-container.compact {
  padding: 8px 24px;
  box-shadow: 0 -1px 4px rgba(0,0,0,0.04);
  width: 100%;
  background: #fff;
}
.chat-input-bar-centered {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 0 auto;
  padding: 16px 0;
}
.chat-input.compact {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  gap: 8px;
  padding: 4px 0;
  border-radius: 8px;
  box-shadow: none;
  margin: 0;
}
.chat-input.compact .el-form-item {
  margin-bottom: 0;
}
.chat-input.compact .el-form-item.input {
  flex: 1 1 0%;
  min-width: 0;
}
.chat-input.compact .el-form-item.button,
.chat-input.compact .el-form-item.upload {
  flex: none;
}
.chat-input.compact .el-input__wrapper {
  min-height: 32px;
  width: 100%;
}
.chat-input.compact .el-button {
  height: 32px;
  font-size: 15px;
  padding: 0 14px;
  min-width: 64px;
}

.msg {
  display: flex;
  justify-content: flex-start;
}

.msg.user {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 80%;
  padding: 14px 28px; /* 原来14px 18px，减半 */
  border-radius: 16px;
  font-size: 1.08rem;
  line-height: 1.7;
  background: #e3f2fd;
  color: #1976d2;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.06);
  word-break: break-word;
  white-space: pre-line;
}

.msg.user .msg-bubble {
  background: #1976d2;
  color: #fff;
  box-shadow: 0 2px 8px 0 rgba(25,118,210,0.12);
}

.msg.assistant .msg-bubble {
  background: #e3f2fd;
  color: #1976d2;
}

.cursor {
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.upload-demo .el-button {
  margin-left: 0;
  border-radius: 50%;
  height: 40px;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
  background: #f4f6fa;
  border: 1px solid #dbeafe;
  transition: background 0.2s;
}

.upload-demo .el-button:hover {
  background: #e3f2fd;
  border-color: #90caf9;
}

.upload-tip {
  text-align: right;
  color: #90a4ae;
  font-size: 13px;
  margin-top: 4px;
  margin-right: 8px;
}

.session-action {
  color: #90a4ae;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
}

.session-action:hover {
  color: #1976d2;
}

.session-x {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #e57373;
  user-select: none;
  transition: color 0.2s;
  z-index: 2;
}

.session-x:hover {
  color: #d32f2f;
}

.upload-area {
  padding: 20px 0;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.reference-bar {
  background: #f4f8fb;
  color: #1976d2;
  font-size: 14px;
  padding: 8px 18px;
  border-radius: 8px;
  margin-bottom: 10px;
}
.ref-item {
  margin-right: 8px;
}
.ref-global {
  color: #888;
}
.ref-session {
  color: #888;
}

/* MCP工具调用内容样式 */
.mcp-content {
  margin-top: 12px;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f8faff;
  overflow: hidden;
}

.mcp-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 14px;
  font-weight: 500;
}

.mcp-body {
  padding: 12px 16px;
  background: #f8faff;
}

.mcp-body pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 使用过的MCP工具样式 */
.mcp-stats {
  margin-left: auto;
}

.used-tools-list {
  max-height: 400px;
  overflow-y: auto;
}

.used-tool-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f8fafc;
}

.tool-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.tool-name {
  font-weight: 500;
  color: #1976d2;
  min-width: 120px;
}

.tool-description {
  color: #666;
  font-size: 14px;
}

.tool-usage {
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-used {
  color: #90a4ae;
  font-size: 12px;
}

.ai-markdown h1, .ai-markdown h2, .ai-markdown h3, .ai-markdown h4 {
  color: #1976d2;
  margin: 16px 0 8px 0;
}
.ai-markdown ul, .ai-markdown ol {
  margin: 8px 0 8px 24px;
}
.ai-markdown blockquote {
  border-left: 4px solid #90caf9;
  background: #f4f8fb;
  color: #555;
  margin: 8px 0;
  padding: 8px 16px;
}
.ai-markdown code {
  background: #f0f0f0;
  color: #d32f2f;
  padding: 2px 4px;
  border-radius: 4px;
}
.ai-markdown pre {
  background: #f8faff;
  color: #374151;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}
:deep(.ai-markdown) p,
:deep(.ai-markdown) ul,
:deep(.ai-markdown) ol,
:deep(.ai-markdown) li {
  margin-top: 2px !important;
  margin-bottom: 2px !important;
  line-height: 1.4 !important;
}
</style> 