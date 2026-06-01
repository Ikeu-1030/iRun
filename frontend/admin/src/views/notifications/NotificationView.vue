<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>消息管理</span>
        <el-button type="primary" style="float:right" @click="openSendDialog">发送通知</el-button>
      </template>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.isBroadcast ? '广播通知' : '发送通知'" width="500px">
      <el-radio-group v-model="dialog.isBroadcast" style="margin-bottom:16px">
        <el-radio :value="false">指定用户</el-radio>
        <el-radio :value="true">全部广播</el-radio>
      </el-radio-group>
      <el-input v-if="!dialog.isBroadcast" v-model="dialog.userIdsStr" placeholder="用户ID，多个用逗号分隔" style="margin-bottom:16px" />
      <el-select v-model="dialog.type" placeholder="通知类型" style="width:100%;margin-bottom:16px">
        <el-option v-for="(label, key) in NOTIFICATION_TYPES" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-input v-model="dialog.title" placeholder="标题" style="margin-bottom:16px" />
      <el-input v-model="dialog.content" placeholder="内容" type="textarea" :rows="4" />
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitSend" :loading="sending">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sendNotification, broadcastNotification } from '@/api/notifications'
import { NOTIFICATION_TYPES } from '@/utils/constants'

const sending = ref(false)
const dialog = reactive({ visible: false, isBroadcast: false, userIdsStr: '', type: 1, title: '', content: '' })

function openSendDialog() {
  dialog.visible = true; dialog.isBroadcast = false; dialog.userIdsStr = ''; dialog.type = 1; dialog.title = ''; dialog.content = ''
}

async function submitSend() {
  const title = dialog.isBroadcast ? '确认向全部用户发送该通知？' : '确认向指定用户发送该通知？'
  await ElMessageBox.confirm(title, '提示', { type: 'warning' })
  sending.value = true
  try {
    if (dialog.isBroadcast) {
      await broadcastNotification({ type: dialog.type, title: dialog.title, content: dialog.content })
      ElMessage.success('广播成功')
    } else {
      const ids = dialog.userIdsStr.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
      if (ids.length === 0) { ElMessage.warning('请输入有效的用户ID'); return }
      await sendNotification({ userIds: ids, type: dialog.type, title: dialog.title, content: dialog.content })
      ElMessage.success('发送成功')
    }
    dialog.visible = false
  } finally { sending.value = false }
}
</script>
