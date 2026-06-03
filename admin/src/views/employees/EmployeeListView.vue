<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>员工管理</span>
        <el-button type="primary" style="float:right" @click="openCreate">新增员工</el-button>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ ADMIN_ROLES[row.role as keyof typeof ADMIN_ROLES] }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <template v-if="row.role === 1">
              <span class="text-gray">-</span>
            </template>
            <template v-else>
              <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 1" type="warning" link @click="toggleStatus(row, false)">停用</el-button>
              <el-button v-else type="success" link @click="toggleStatus(row, true)">启用</el-button>
              <el-button type="warning" link @click="resetPwd(row)">重置密码</el-button>
              <el-button type="danger" link @click="deleteEmp(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        @change="fetchData" class="pagination"
      />
    </el-card>

    <el-dialog v-model="formDialog.visible" :title="formDialog.isEdit ? '编辑员工' : '新增员工'" width="500px">
      <el-form :model="formDialog" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="formDialog.username" :disabled="formDialog.isEdit" />
        </el-form-item>
        <el-form-item label="姓名" required><el-input v-model="formDialog.name" /></el-form-item>
        <el-form-item label="密码" :required="!formDialog.isEdit">
          <el-input v-model="formDialog.password" type="password" :placeholder="formDialog.isEdit ? '留空不修改' : ''" />
        </el-form-item>
        <el-form-item label="手机号" required><el-input v-model="formDialog.phone" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="formDialog.sex" style="width:100%">
            <el-option label="男" value="男" /><el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号"><el-input v-model="formDialog.idNumber" /></el-form-item>
        <el-form-item label="角色">
          <el-input value="普通管理员" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="400px">
      <el-input v-model="pwdDialog.newPassword" type="password" placeholder="新密码" />
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPwd">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listEmployees, createEmployee, updateEmployee, toggleEmployeeStatus, resetEmployeePassword, deleteEmployee } from '@/api/employees'
import { ADMIN_ROLES } from '@/utils/constants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const formDialog = reactive<any>({
  visible: false, isEdit: false, id: null,
  username: '', name: '', password: '', phone: '', sex: '', idNumber: ''
})

const pwdDialog = reactive({ visible: false, id: 0, newPassword: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await listEmployees({ page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(formDialog, { visible: true, isEdit: false, id: null, username: '', name: '', password: '', phone: '', sex: '', idNumber: '' })
}

function openEdit(row: any) {
  Object.assign(formDialog, { visible: true, isEdit: true, id: row.id, username: row.username, name: row.name, password: '', phone: row.phone, sex: row.sex, idNumber: '' })
}

async function submitForm() {
  if (formDialog.isEdit) {
    await updateEmployee(formDialog.id, {
      name: formDialog.name, phone: formDialog.phone, sex: formDialog.sex, role: 2
    })
    ElMessage.success('更新成功')
  } else {
    await createEmployee({
      username: formDialog.username, name: formDialog.name,
      password: formDialog.password, phone: formDialog.phone,
      sex: formDialog.sex, idNumber: formDialog.idNumber, role: 2
    })
    ElMessage.success('创建成功')
  }
  formDialog.visible = false
  fetchData()
}

async function toggleStatus(row: any, enabled: boolean) {
  await ElMessageBox.confirm(`确认${enabled ? '启用' : '停用'}员工 "${row.name}"？`, '提示', { type: 'warning' })
  try {
    await toggleEmployeeStatus(row.id, enabled)
    ElMessage.success('操作成功')
    fetchData()
  } catch { /* error handled by request interceptor */ }
}

function resetPwd(row: any) { pwdDialog.id = row.id; pwdDialog.newPassword = ''; pwdDialog.visible = true }

async function submitResetPwd() {
  await ElMessageBox.confirm('确认重置该员工的密码？重置后需使用新密码登录。', '警告', { type: 'warning' })
  await resetEmployeePassword(pwdDialog.id, { newPassword: pwdDialog.newPassword })
  ElMessage.success('密码已重置')
  pwdDialog.visible = false
}

async function deleteEmp(row: any) {
  await ElMessageBox.confirm(`确认删除员工 "${row.name}"？此操作不可撤销。`, '警告', { type: 'error' })
  await deleteEmployee(row.id)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.text-gray { color: var(--text-placeholder); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
