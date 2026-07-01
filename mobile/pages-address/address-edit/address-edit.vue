<template>
  <view class="page">
    <uni-nav-bar :title="isEdit ? '编辑地址' : '新增地址'" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="form-card">
        <view class="form-field">
          <text class="field-label">联系人</text>
          <input class="field-input" v-model="form.contactName" placeholder="请输入联系人姓名" />
        </view>
        <view class="form-field">
          <text class="field-label">联系电话</text>
          <input class="field-input" name="tel" v-model="form.contactPhone" placeholder="请输入联系电话" />
        </view>
        <view class="form-field">
          <text class="field-label">性别</text>
          <picker mode="selector" :range="['男', '女']" @change="onSexChange">
            <view class="field-select"><text>{{ form.sex || '请选择' }}</text><text class="select-arrow">▼</text></view>
          </picker>
        </view>

        <!-- 查看模式：直接显示原始地址文本 -->
        <template v-if="isView">
          <view class="form-field form-field--last">
            <text class="field-label">详细地址</text>
            <text class="field-value-text">{{ form.detail || '未设置' }}</text>
          </view>
        </template>

        <!-- 编辑模式：地址类型切换 + 表单 -->
        <template v-else>
          <view class="address-type-toggle">
            <view class="toggle-item" :class="{ 'toggle-item--active': addressType === 'preset' }" @click="addressType = 'preset'">
              <text>校园地址</text>
            </view>
            <view class="toggle-item" :class="{ 'toggle-item--active': addressType === 'custom' }" @click="addressType = 'custom'">
              <text>自定义</text>
            </view>
          </view>

          <template v-if="addressType === 'preset'">
            <view class="form-field">
              <text class="field-label">地点</text>
              <picker mode="selector" :range="areaNames" :value="selectedAreaIndex" @change="onAreaChange">
                <view class="field-select"><text :class="{ 'field-select-placeholder': selectedAreaIndex < 0 }">{{ selectedAreaIndex >= 0 ? areaNames[selectedAreaIndex] : '请选择校区及区域' }}</text><text class="select-arrow">▼</text></view>
              </picker>
            </view>
            <view class="form-field" v-if="selectedAreaIndex >= 0">
              <text class="field-label">{{ areaBuildingLabel }}</text>
              <picker mode="selector" :range="buildingNames" :value="selectedBuildingIndex" @change="onBuildingChange">
                <view class="field-select"><text :class="{ 'field-select-placeholder': selectedBuildingIndex < 0 }">{{ selectedBuildingIndex >= 0 ? buildingNames[selectedBuildingIndex] : '请选择具体位置' }}</text><text class="select-arrow">▼</text></view>
              </picker>
            </view>
            <view class="form-field" v-if="selectedBuildingIndex >= 0">
              <text class="field-label">楼层</text>
              <picker mode="selector" :range="floorOptions" :value="selectedFloor - 1" @change="onFloorChange">
                <view class="field-select"><text>{{ selectedFloor + '层' }}</text><text class="select-arrow">▼</text></view>
              </picker>
            </view>
            <view class="form-field" v-if="selectedBuildingIndex >= 0">
              <text class="field-label">详细地址</text>
              <input class="field-input" v-model="roomDetail" placeholder="请输入具体地址门牌号信息" />
            </view>
          </template>

          <template v-if="addressType === 'custom'">
            <view class="form-field form-field--last">
              <text class="field-label">详细地址</text>
              <textarea class="field-textarea" v-model="form.detail" placeholder="请输入自定义地址" />
            </view>
          </template>
        </template>

        <view class="form-field form-field--last" v-if="!isView">
          <view class="default-row" @click="form.isDefault = form.isDefault === 1 ? 0 : 1">
            <text class="default-label">设为默认地址</text>
            <iconpark-icon :name="form.isDefault === 1 ? 'checkbox-filled' : 'circle'" size="22" :color="form.isDefault === 1 ? '#FF6B4A' : '#D4D2CC'" />
          </view>
        </view>
      </view>

      <view v-if="!isView" class="save-btn" @click="onSave"><text>{{ saving ? '保存中…' : '保存' }}</text></view>
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressApi } from '@/api'
import { CAMPUS_AREAS, DEFAULT_MAX_FLOOR, buildAddressString, reverseParseAddress, getBuildingDisplay } from '@/utils/campus-data.js'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const isEdit = ref(false)
const editId = ref('')
const isView = ref(false)
const saving = ref(false)

const form = reactive({ contactName: '', contactPhone: '', sex: '男', detail: '', isDefault: 0 })

// 结构化地址字段
const selectedAreaIndex = ref(-1)
const selectedBuildingIndex = ref(-1)
const selectedFloor = ref(1)         // 楼层数值 1-6，默认1层
const roomDetail = ref('')
const addressType = ref('preset') // 'preset' | 'custom'

const areaNames = CAMPUS_AREAS.map(a => a.name)

const areaBuildingLabel = computed(() => {
  if (selectedAreaIndex.value < 0) return '建筑'
  const area = CAMPUS_AREAS[selectedAreaIndex.value]
  if (area.id === 'cd_teaching') return '教学楼'
  if (area.id === 'cd_library') return '图书馆'
  return '宿舍楼'
})

const buildingNames = computed(() => {
  if (selectedAreaIndex.value < 0) return []
  const buildings = CAMPUS_AREAS[selectedAreaIndex.value].buildings
  return buildings.map(b => getBuildingDisplay(b))
})

const floorOptions = ref(['1层', '2层', '3层', '4层', '5层', '6层'])

function updateFloorOptions(area) {
  const max = (area && area.maxFloor) ? area.maxFloor : DEFAULT_MAX_FLOOR
  const opts = []
  for (let i = 1; i <= max; i++) opts.push(i + '层')
  floorOptions.value = opts
}

onLoad((options) => {
  const id = options?.id
  if (id) {
    isEdit.value = true
    editId.value = id
    isView.value = options?.view === '1'
    loadAddress(id)
  }
})

async function loadAddress(id) {
  try {
    const list = await addressApi.getAddressList()
    const addr = (list || []).find(a => String(a.id) === String(id))
    if (addr) {
      form.contactName = addr.contactName || ''
      form.contactPhone = addr.contactPhone || ''
      form.sex = addr.sex || '男'
      form.isDefault = addr.isDefault || 0
      form.detail = addr.detail || ''

      // 尝试反向解析结构化地址（仅编辑模式需要）
      const parsed = reverseParseAddress(addr.detail || '')
      if (parsed && parsed.areaIdx >= 0) {
        addressType.value = 'preset'
        selectedAreaIndex.value = parsed.areaIdx
        updateFloorOptions(CAMPUS_AREAS[parsed.areaIdx])
        if (parsed.buildingIdx >= 0) {
          selectedBuildingIndex.value = parsed.buildingIdx
          selectedFloor.value = parsed.floor || 1
          roomDetail.value = parsed.roomDetail || ''
        }
      } else if (addr.detail) {
        addressType.value = 'custom'
        form.detail = addr.detail
      }
    }
  } catch (e) { /* handled */ }
}

function onSexChange(e) { form.sex = ['男', '女'][e.detail.value] }

function onAreaChange(e) {
  selectedAreaIndex.value = Number(e.detail.value)
  selectedBuildingIndex.value = -1
  selectedFloor.value = 1
  roomDetail.value = ''
  updateFloorOptions(CAMPUS_AREAS[Number(e.detail.value)])
}

function onBuildingChange(e) {
  selectedBuildingIndex.value = Number(e.detail.value)
  selectedFloor.value = 1
  roomDetail.value = ''
}

function onFloorChange(e) {
  selectedFloor.value = Number(e.detail.value) + 1
}

async function onSave() {
  if (!form.contactName.trim()) { uni.showToast({ title: '请输入联系人', icon: 'none' }); return }
  if (!form.contactPhone.trim()) { uni.showToast({ title: '请输入联系电话', icon: 'none' }); return }

  if (addressType.value === 'custom') {
    if (!form.detail.trim()) { uni.showToast({ title: '请输入详细地址', icon: 'none' }); return }
  } else {
    if (selectedAreaIndex.value < 0) { uni.showToast({ title: '请选择地点', icon: 'none' }); return }
    if (selectedBuildingIndex.value < 0) { uni.showToast({ title: '请选择具体位置', icon: 'none' }); return }
    const area = CAMPUS_AREAS[selectedAreaIndex.value]
    const building = area.buildings[selectedBuildingIndex.value]
    form.detail = buildAddressString(area.name, building, selectedFloor.value, roomDetail.value.trim() || '')
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await addressApi.updateAddress(editId.value, { ...form })
    } else {
      await addressApi.saveAddress({ ...form })
    }
    uni.showToast({ title: isEdit.value ? '已更新' : '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ }
  saving.value = false
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}
.form-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:0;margin-top:16rpx;box-shadow:var(--shadow-sm);overflow:hidden}
.address-type-toggle{display:flex;margin:0 32rpx;padding:8rpx;background:var(--surface);border-radius:14rpx;gap:8rpx}
.toggle-item{flex:1;text-align:center;padding:16rpx 0;border-radius:12rpx;font-size:26rpx;color:var(--text-secondary);transition:all .2s}
.toggle-item--active{background:var(--primary);color:#fff;font-weight:600}
.form-field{padding:28rpx 32rpx;border-bottom:1rpx solid var(--outline-light)}
.form-field--last{border-bottom:none}
.field-label{font-size:26rpx;color:var(--text-secondary);display:block;margin-bottom:12rpx}
.field-input{width:100%;height:80rpx;background:var(--surface);border-radius:14rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.field-textarea{width:100%;min-height:120rpx;background:var(--surface);border-radius:14rpx;padding:20rpx 22rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.field-select{width:100%;height:80rpx;background:var(--surface);border-radius:14rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-secondary);display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.field-select-placeholder{color:var(--text-tertiary)}
.select-arrow{font-size:24rpx;color:var(--text-secondary)}
.field-hint{font-size:22rpx;color:var(--warning);margin-top:10rpx;display:block}
.field-value-text{font-size:28rpx;color:var(--text-primary);display:block;padding:20rpx 22rpx;background:var(--surface);border-radius:14rpx;line-height:1.6}
.default-row{display:flex;align-items:center;justify-content:space-between}
.default-label{font-size:28rpx;color:var(--text-primary)}
.save-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:48rpx;box-shadow:var(--shadow-sm)}
.save-btn:active{transform:scale(.95)}
.save-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
