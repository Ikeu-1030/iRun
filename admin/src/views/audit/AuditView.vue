<template>
  <div class="page">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户实名认证" name="userCert">
          <el-table :data="userTable" v-loading="userLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" min-width="100" />
            <el-table-column prop="nickname" label="昵称" min-width="100" />
            <el-table-column prop="realName" label="真实姓名" min-width="90" />
            <el-table-column prop="studentId" label="学号" min-width="110" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column label="认证照片" width="90">
              <template #default="{ row }">
                <el-button v-if="row.certifyImg" link type="primary" @click="previewImg(row.certifyImg)">查看</el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="150" >
              <template #default="{ row }">
                <el-button type="success" link @click="approveUser(row)">通过</el-button>
                <el-button type="danger" link @click="rejectUser(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="userPage" v-model:page-size="userSize"
            :total="userTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
            @change="fetchUsers" class="pagination"
          />
        </el-tab-pane>

        <el-tab-pane label="跑腿员认证" name="runnerCert">
          <el-table :data="runnerTable" v-loading="runnerLoading" stripe>
            <el-table-column prop="profileId" label="档案ID" width="70" />
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="realName" label="真实姓名" min-width="100" />
            <el-table-column prop="creditScore" label="信用分" width="80" />
            <el-table-column label="操作" min-width="150" >
              <template #default="{ row }">
                <el-button type="success" link @click="approveRunner(row)">通过</el-button>
                <el-button type="danger" link @click="rejectRunner(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="runnerPage" v-model:page-size="runnerSize"
            :total="runnerTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
            @change="fetchRunners" class="pagination"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="rejectDialog.visible" title="驳回原因" width="400px">
      <el-input v-model="rejectDialog.remark" placeholder="请填写驳回原因" type="textarea" :rows="3" />
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject" :disabled="!rejectDialog.remark.trim()">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imgVisible" title="认证照片" width="500px">
      <img :src="imgUrl" style="width:100%" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, reviewUserCertification } from '@/api/users'
import { listRunners, reviewRunnerCert } from '@/api/runners'

const activeTab = ref('userCert')

const userLoading = ref(false)
const userTable = ref<any[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userSize = ref(10)

async function fetchUsers() {
  userLoading.value = true
  try {
    const res = await listUsers({ isCertify: 1, page: userPage.value, size: userSize.value }) as any
    userTable.value = res.records
    userTotal.value = res.total
  } finally { userLoading.value = false }
}

async function approveUser(row: any) {
  await ElMessageBox.confirm(`确认通过用户 "${row.nickname || row.username}" 的实名认证？`, '提示', { type: 'success' })
  await reviewUserCertification(row.id, 2)
  ElMessage.success('已通过认证')
  fetchUsers()
}

function rejectUser(row: any) {
  rejectDialog.value.type = 'user'
  rejectDialog.value.id = row.id
  rejectDialog.value.remark = ''
  rejectDialog.value.visible = true
}

const runnerLoading = ref(false)
const runnerTable = ref<any[]>([])
const runnerTotal = ref(0)
const runnerPage = ref(1)
const runnerSize = ref(10)

async function fetchRunners() {
  runnerLoading.value = true
  try {
    const res = await listRunners({ verifyStatus: 1, page: runnerPage.value, size: runnerSize.value }) as any
    runnerTable.value = res.records
    runnerTotal.value = res.total
  } finally { runnerLoading.value = false }
}

async function approveRunner(row: any) {
  await ElMessageBox.confirm(`确认通过跑腿员 "${row.nickname}" 的认证？`, '提示', { type: 'success' })
  await reviewRunnerCert(row.profileId, 2)
  ElMessage.success('已通过认证')
  fetchRunners()
}

function rejectRunner(row: any) {
  rejectDialog.value.type = 'runner'
  rejectDialog.value.id = row.profileId
  rejectDialog.value.remark = ''
  rejectDialog.value.visible = true
}

const rejectDialog = ref({ visible: false, type: '' as string, id: 0, remark: '' })

async function confirmReject() {
  if (rejectDialog.value.type === 'user') {
    await reviewUserCertification(rejectDialog.value.id, 3, rejectDialog.value.remark)
    fetchUsers()
  } else {
    await reviewRunnerCert(rejectDialog.value.id, 3, rejectDialog.value.remark)
    fetchRunners()
  }
  ElMessage.success('已驳回')
  rejectDialog.value.visible = false
}

const imgVisible = ref(false)
const imgUrl = ref('')
function previewImg(url: string) { imgUrl.value = url; imgVisible.value = true }

onMounted(() => { fetchUsers(); fetchRunners() })
</script>

<style scoped>
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
