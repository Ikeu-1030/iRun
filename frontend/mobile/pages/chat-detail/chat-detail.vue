<template>
  <view class="page">
    <uni-nav-bar :title="peerNickname" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="msg-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" :scroll-into-view="scrollToId" @scrolltolower="loadMore">
      <view v-if="loadingMore" class="load-more"><text>加载更多…</text></view>

      <view v-for="msg in messages" :key="msg.messageId || msg.localId" :id="'msg-' + (msg.messageId || msg.localId)">
        <view class="msg-time" v-if="msg.showTime">
          <text>{{ msg.createdAt }}</text>
        </view>

        <!-- 对方消息 (left) -->
        <view v-if="msg.senderId !== myUserId" class="msg-row msg-row--left">
          <!-- 多选模式下的勾选框 -->
          <view v-if="multiSelectMode" class="select-check" :class="{ 'select-check--active': selectedIds.has(msg.messageId) }" @click="toggleSelect(msg)">
            <iconpark-icon v-if="selectedIds.has(msg.messageId)" name="checkbox-filled" size="22" color="#FF6B4A" />
            <text v-else class="select-circle"></text>
          </view>
          <image v-if="msg.senderAvatar && !multiSelectMode" class="msg-avatar" :src="normalizeUrl(msg.senderAvatar)" mode="aspectFill" />
          <view v-else-if="!multiSelectMode" class="msg-avatar msg-avatar--fallback">{{ peerInitial }}</view>
          <view class="msg-bubble msg-bubble--left" :class="{ 'msg-bubble--recalled': msg.isRecalled }" @longpress="onLongPress(msg)">
            <text v-if="msg.isRecalled" class="recalled-text">{{ msg.content }}</text>
            <text v-else>{{ msg.content }}</text>
          </view>
        </view>

        <!-- 我的消息 (right) -->
        <view v-else class="msg-row msg-row--right">
          <view class="msg-bubble msg-bubble--right" :class="{ 'msg-bubble--recalled': msg.isRecalled }" :style="{ background: selectedIds.has(msg.messageId) ? '#608de6' : '' }" @longpress="onLongPress(msg)">
            <text v-if="msg.isRecalled" class="recalled-text">{{ msg.content }}</text>
            <text v-else>{{ msg.content }}</text>
          </view>
          <image v-if="myAvatar && !multiSelectMode" class="msg-avatar" :src="myAvatar" mode="aspectFill" />
          <view v-else-if="!multiSelectMode" class="msg-avatar msg-avatar--me">{{ myInitial }}</view>
          <!-- 多选模式下的勾选框 -->
          <view v-if="multiSelectMode" class="select-check" :class="{ 'select-check--active': selectedIds.has(msg.messageId) }" @click="toggleSelect(msg)">
            <iconpark-icon v-if="selectedIds.has(msg.messageId)" name="checkbox-filled" size="22" color="#FF6B4A" />
            <text v-else class="select-circle"></text>
          </view>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 多选模式底部栏 -->
    <view v-if="multiSelectMode" class="input-bar safe-area-bottom multi-bar">
      <view class="multi-cancel-btn" @click="exitMultiSelect"><text>取消</text></view>
      <view class="multi-count"><text>已选 {{ selectedIds.size }} 条</text></view>
      <view class="multi-del-btn" :class="{ 'multi-del-btn--disabled': selectedIds.size === 0 }" @click="onBatchDelete">
        <text>删除所选</text>
      </view>
    </view>

    <!-- 输入栏（非多选模式） -->
    <view v-else class="input-bar safe-area-bottom">
      <input class="input-field" v-model="inputText" placeholder="输入消息…" confirm-name="send" @confirm="onSend" />
      <view class="send-btn" :class="{ 'send-btn--disabled': !inputText.trim() || sending }" @click="onSend">
        <text>{{ sending ? '…' : '发送' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { useChatStore } from '@/store/chat.js'
import { handlePageError, ErrorType } from '@/utils/error'
import { showToast } from '@/utils/toast'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const store = useStore()
const chatStore = useChatStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44 - 100

const peerUserId = ref(0)
const peerNickname = ref('')
const peerAvatar = ref('')
const inputText = ref('')
const sending = ref(false)
const scrollToId = ref('')
const page = ref(1)
const loadingMore = ref(false)

const myUserId = ref(store.userId)
const myAvatar = ref(store.userInfo.avatarUrl || '')
const myInitial = computed(() => store.avatarText)
const peerInitial = computed(() => (peerNickname.value || '用').charAt(0))

const messages = computed(() => chatStore.getMessages(peerUserId.value))
const hasMore = computed(() => {
  const m = chatStore.hasMore
  // Pinia store's Map is not reactive via .get(); use the message count as heuristic
  return messages.value.length > 0 && messages.value.length % 20 === 0
})

onLoad(async (options) => {
  peerUserId.value = Number(options?.userId) || 0
  peerNickname.value = decodeURIComponent(options?.nickname || '') || '聊天'
  peerAvatar.value = normalizeUrl(decodeURIComponent(options?.avatar || ''))
  myUserId.value = store.userId || 0

  // 确保全局 STOMP 已连接
  if (!chatStore.wsConnected && store.token) {
    await chatStore.connectStomp(store.token, store.userId)
  }
  // 标记已读
  chatStore.markRead(peerUserId.value)
  // 加载历史
  loadHistory()
})

async function loadHistory() {
  loadingMore.value = page.value > 1
  try {
    await chatStore.loadHistory(peerUserId.value, page.value)
    if (page.value === 1) scrollToBottom()
  } catch (e) { /* handled */ }
  loadingMore.value = false
}

function loadMore() {
  if (loadingMore.value) return
  const cachedMsgs = chatStore.getMessages(peerUserId.value)
  // 如果缓存消息数 >= page * 20，说明已加载完
  if (cachedMsgs.length >= page.value * 20) {
    page.value++
    loadHistory()
  }
}

async function onSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  if (!chatStore.wsConnected) {
    showToast('消息未连接，请稍后重试')
    return
  }

  const localId = 'local-' + Date.now()
  const localMsg = {
    localId,
    messageId: null,
    senderId: myUserId.value,
    senderNickname: store.userInfo.nickname || '',
    senderAvatar: store.userInfo.avatarUrl || '',
    receiverId: peerUserId.value,
    content: text,
    messageType: 1,
    createdAt: new Date().toLocaleString('zh-CN')
  }
  chatStore.addLocalMessage(localMsg)
  inputText.value = ''
  scrollToBottom()

  sending.value = true
  try {
    console.log('[Chat] Sending via STOMP:', { receiverId: peerUserId.value, content: text })
    chatStore.stompClient.send('/app/chat.send', {}, JSON.stringify({
      receiverId: peerUserId.value,
      content: text,
      messageType: 1
    }))
    console.log('[Chat] STOMP send call completed')
  } catch (e) {
    console.error('[Chat] Send failed:', e)
    chatStore.removeLocalMessage(localId, peerUserId.value)
    uni.showToast({ title: '发送失败，请重试', icon: 'none' })
  }
  sending.value = false
}

function scrollToBottom() {
  nextTick(() => {
    const msgs = chatStore.getMessages(peerUserId.value)
    const last = msgs[msgs.length - 1]
    if (last) {
      scrollToId.value = 'msg-' + (last.messageId || last.localId)
    }
  })
}

// 收到新消息时自动滚动
watch(
  () => messages.value.length,
  () => { scrollToBottom() }
)

// ---- 多选模式 ----
const multiSelectMode = ref(false)
const selectedIds = ref(new Set())

function enterMultiSelect() {
  multiSelectMode.value = true
  selectedIds.value = new Set()
}

function exitMultiSelect() {
  multiSelectMode.value = false
  selectedIds.value = new Set()
}

function toggleSelect(msg) {
  if (!msg.messageId) return
  const s = new Set(selectedIds.value)
  if (s.has(msg.messageId)) {
    s.delete(msg.messageId)
  } else {
    s.add(msg.messageId)
  }
  selectedIds.value = s
}

async function onBatchDelete() {
  if (selectedIds.value.size === 0) return
  const res = await new Promise(r => {
    uni.showModal({
      title: '批量删除',
      content: `确定删除选中的 ${selectedIds.value.size} 条消息吗？`,
      success: r2 => r(r2.confirm)
    })
  })
  if (!res) return
  await chatStore.batchDelete([...selectedIds.value])
  exitMultiSelect()
  uni.showToast({ title: '已删除', icon: 'success' })
}

// ---- 长按菜单 ----
function onLongPress(msg) {
  if (multiSelectMode.value) return
  if (!msg.messageId) return // 本地乐观消息无 messageId

  const isOwn = msg.senderId === myUserId.value
  const isRecalled = msg.isRecalled
  const isWithin5Min = isOwn && !isRecalled && msg.createdAt && (() => {
    try {
      return Date.now() - new Date(msg.createdAt).getTime() < 5 * 60 * 1000
    } catch (e) { return false }
  })()

  const items = ['复制']
  if (isOwn && !isRecalled) items.push('删除')
  if (isOwn && isWithin5Min) items.push('撤回')
  items.push('多选')

  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      const action = items[res.tapIndex]
      if (action === '复制') copyMessage(msg)
      else if (action === '删除') confirmDelete(msg)
      else if (action === '撤回') confirmRecall(msg)
      else if (action === '多选') enterMultiSelect()
    }
  })
}

function copyMessage(msg) {
  uni.setClipboardData({
    data: msg.isRecalled ? '[消息已撤回]' : msg.content,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  })
}

async function confirmDelete(msg) {
  const res = await new Promise(r => {
    uni.showModal({
      title: '删除消息',
      content: '删除后仅自己不可见',
      success: r2 => r(r2.confirm)
    })
  })
  if (!res) return
  try {
    await chatStore.deleteMessage(msg.messageId)
    uni.showToast({ title: '已删除', icon: 'success' })
  } catch (e) {
    handlePageError(e, {
      toastAlreadyShown: true,
      customHandlers: {
        [ErrorType.BUSINESS]: (err) => {
          if (err.message.includes('只能操作')) {
            showToast('只能删除自己发送的消息')
          }
        }
      }
    })
  }
}

async function confirmRecall(msg) {
  const res = await new Promise(r => {
    uni.showModal({
      title: '撤回消息',
      content: '撤回后双方均会显示"消息已撤回"',
      success: r2 => r(r2.confirm)
    })
  })
  if (!res) return
  try {
    await chatStore.recallMessage(msg.messageId)
    uni.showToast({ title: '已撤回', icon: 'success' })
  } catch (e) {
    handlePageError(e, {
      toastAlreadyShown: true,
      customHandlers: {
        [ErrorType.BUSINESS]: (err) => {
          if (err.message.includes('超过5分钟')) {
            showToast('消息已超过5分钟，无法撤回')
          } else if (err.message.includes('只能操作')) {
            showToast('只能撤回自己发送的消息')
          }
        }
      }
    })
  }
}

function onBack() {
  chatStore.markRead(peerUserId.value)
  uni.navigateBack()
}

onUnload(() => {
  chatStore.markRead(peerUserId.value)
})
</script>


<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--surface);overflow:hidden}

.msg-scroll{box-sizing:border-box;width:100%;flex:1;padding:16rpx 24rpx}

.load-more{text-align:center;padding:16rpx}
.load-more text{font-size:22rpx;color:var(--text-tertiary)}

.msg-time{text-align:center;padding:16rpx 0}
.msg-time text{font-size:22rpx;color:var(--text-tertiary);background:var(--surface-hover);padding:4rpx 16rpx;border-radius:8rpx}

.msg-row{display:flex;margin-bottom:20rpx;align-items:flex-start}
.msg-row--left{justify-content:flex-start}
.msg-row--right{justify-content:flex-end}

.msg-avatar{width:72rpx;height:72rpx;border-radius:50%;flex-shrink:0}
.msg-avatar--fallback{background:linear-gradient(135deg,#4facfe,#43e97b);display:flex;align-items:center;justify-content:center;font-size:28rpx;font-weight:600;color:#fff}
.msg-avatar--me{background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:28rpx;font-weight:600;color:#fff}
.msg-row--left .msg-avatar{margin-right:14rpx}
.msg-row--right .msg-avatar{margin-left:14rpx}

.msg-bubble{max-width:480rpx;padding:18rpx 24rpx;border-radius:var(--radius-card);font-size:28rpx;line-height:1.5;word-break:break-all}
.msg-bubble--left{background:var(--surface-raised);color:var(--text-primary);border-bottom-left-radius:6rpx}
.msg-bubble--right{background:var(--primary);color:#fff;border-bottom-right-radius:6rpx}

.input-bar{display:flex;align-items:center;padding:16rpx 24rpx;background:var(--surface-raised);border-top:1rpx solid var(--surface-hover);padding-bottom:calc(16rpx + env(safe-area-inset-bottom));gap:14rpx}
.input-field{flex:1;height:72rpx;background:var(--surface);border-radius:36rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary)}
.send-btn{padding:14rpx 28rpx;background:var(--primary);border-radius:36rpx;flex-shrink:0}
.send-btn--disabled{background:var(--outline);pointer-events:none}
.send-btn text{font-size:26rpx;color:#fff;font-weight:500}
.send-btn:active{transform:scale(.95)}
.bottom-placeholder{height:120rpx}

/* 撤回消息样式 */
.msg-bubble--recalled{background:var(--surface-hover)!important;border:1rpx dashed var(--outline)}
.msg-bubble--recalled.msg-bubble--right{color:var(--text-secondary)!important}
.recalled-text{font-style:italic;color:var(--text-tertiary);font-size:26rpx}

/* 多选模式 */
.select-check{width:44rpx;height:44rpx;display:flex;align-items:center;justify-content:center;flex-shrink:0;margin:0 10rpx}
.select-circle{width:22rpx;height:22rpx;border-radius:50%;border:2rpx solid var(--outline);display:block}
.select-check--active .select-circle{border-color:var(--primary);background:var(--primary)}

.multi-bar{justify-content:space-between}
.multi-cancel-btn{padding:14rpx 20rpx;flex-shrink:0}
.multi-cancel-btn text{font-size:28rpx;color:var(--text-secondary)}
.multi-count{flex:1;text-align:center}
.multi-count text{font-size:26rpx;color:var(--text-secondary)}
.multi-del-btn{padding:14rpx 28rpx;background:var(--error);border-radius:36rpx;flex-shrink:0}
.multi-del-btn:active{transform:scale(.95)}
.multi-del-btn text{font-size:26rpx;color:#fff;font-weight:500}
.multi-del-btn--disabled{background:var(--outline)}
</style>
