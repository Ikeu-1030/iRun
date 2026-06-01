<template>
  <view class="page">
    <uni-nav-bar title="个人信息设置" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="avatar-section">
        <view class="avatar-wrap" @click="onChangeAvatar">
          <view class="avatar-img">
            <image v-if="avatarPath" class="avatar-image" :src="avatarPath" mode="aspectFill" />
            <text v-else class="avatar-text">{{ store.avatarText }}</text>
            <view class="avatar-camera-overlay"><iconpark-icon name="camera" size="20" color="#fff" /></view>
          </view>
        </view>
        <view class="change-avatar-btn" @click="onChangeAvatar"><text>修改头像</text></view>
      </view>

      <view class="info-card">
        <view class="info-row">
          <view class="info-label-row">
            <text class="info-label info-label--auto">用户名</text>
            <view class="locked-icon-wrap" @click.stop="showUsernameHint">
              <iconpark-icon name="info-filled" size="18" color="#F59E0B" />
            </view>
          </view>
          <text class="info-value-locked">{{ store.userInfo.username || '未设置' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">昵称</text>
          <input class="info-input" v-model="nickname" placeholder="输入昵称" />
        </view>
        <picker mode="selector" :range="['男', '女']" :value="sex === '女' ? 1 : 0" @change="onSexChange">
          <view class="info-row">
            <text class="info-label">性别</text>
            <view class="info-value-row">
              <text>{{ sex }}</text>
              <iconpark-icon name="right" size="16" color="#D4D2CC" />
            </view>
          </view>
        </picker>
        <picker mode="selector" :range="colleges" :value="collegeIndex" @change="onCollegeChange">
          <view class="info-row">
            <text class="info-label">学院</text>
            <view class="info-value-row">
              <text>{{ campus }}</text>
              <iconpark-icon name="right" size="16" color="#D4D2CC" />
            </view>
          </view>
        </picker>
        <view class="info-row info-row--bio">
          <text class="info-label info-label--block">个性签名</text>
          <textarea class="info-textarea" v-model="signature" placeholder="写点什么介绍一下自己吧…" />
        </view>
      </view>

      <view class="save-btn" @click="onSave"><text>{{ saving ? '保存中…' : '保存修改' }}</text></view>
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useStore } from '@/store/index.js'
import { commonApi } from '@/api'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const avatarPath = ref(store.userInfo.avatarUrl || '')
const nickname = ref(store.userInfo.nickname || '')
const sex = ref(store.userInfo.sex || '男')
const campus = ref(store.userInfo.campus || '计算机学院')
const signature = ref(store.userInfo.signature || '')
const saving = ref(false)

const colleges = ['计算机学院', '工学院', '文理学院', '商学院', '艺术与设计学院']
const collegeIndex = computed(() => colleges.indexOf(campus.value))

async function onChangeAvatar() {
  const res = await new Promise(r => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: r2 => r(r2.tempFilePaths[0]),
      fail: () => r(null)
    })
  })
  if (!res) return
  try {
    const url = await commonApi.uploadFile(res)
    if (url) {
      avatarPath.value = url
      uni.showToast({ title: '头像上传成功', icon: 'success' })
    }
  } catch (e) {
    console.error('profile-settings avatar upload:', e)
    uni.showToast({ title: '头像上传失败', icon: 'none' })
  }
}

function onSexChange(e) {
  sex.value = ['男', '女'][e.detail.value]
}
function onCollegeChange(e) {
  campus.value = colleges[e.detail.value]
}

async function onSave() {
  saving.value = true
  try {
    await store.updateProfile({
      nickname: nickname.value,
      avatarUrl: avatarPath.value,
      campus: campus.value,
      signature: signature.value,
      sex: sex.value
    })
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ }
  saving.value = false
}

function showUsernameHint() {
  uni.showModal({
    title: '用户名',
    content: '用户名注册后不支持修改',
    showCancel: false,
    confirmText: '知道了'
  })
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}
.info-card{background:var(--surface-raised);border-radius:var(--radius-lg);overflow:hidden;box-shadow:var(--shadow-sm)}
.avatar-section{display:flex;flex-direction:column;align-items:center;padding:40rpx 0 20rpx}
.avatar-wrap{width:168rpx;height:168rpx;border-radius:50%;overflow:hidden;border:4rpx solid var(--primary-container)}
.avatar-img{width:100%;height:100%;background:var(--primary);display:flex;align-items:center;justify-content:center;position:relative}
.avatar-image{width:100%;height:100%;object-fit:cover}
.avatar-text{font-size:56rpx;font-weight:700;color:#fff}
.avatar-camera-overlay{position:absolute;bottom:0;left:0;right:0;background:rgba(0,0,0,.3);padding:6rpx 0;display:flex;align-items:center;justify-content:center}
.change-avatar-btn{margin-top:20rpx;padding:12rpx 28rpx;border-radius:24rpx}
.change-avatar-btn:active{background:var(--surface-hover)}
.change-avatar-btn text{font-size:28rpx;color:var(--primary);font-weight:500}

.info-row{display:flex;align-items:center;padding:44rpx 36rpx;border-bottom:1rpx solid var(--outline-light);min-height:120rpx;width:100%;box-sizing:border-box}
.info-row:active{background:var(--surface-hover)}
.info-label-row{display:flex;align-items:center;gap:6rpx}
.locked-icon-wrap{display:flex;align-items:center;padding:4rpx}
.locked-icon-wrap:active{opacity:.6}
.info-value-locked{font-size:28rpx;font-weight:500;color:var(--text-primary);flex:1;text-align:right}
.info-row--bio{flex-direction:column;align-items:stretch;padding:36rpx 36rpx 40rpx;border-bottom:none}
.info-label{width:180rpx;font-size:30rpx;font-weight:500;color:var(--text-primary);flex-shrink:0}
.info-label--auto{width:auto}
.info-label--block{width:100%;margin-bottom:18rpx}
.info-input{flex:1;font-size:28rpx;color:var(--text-primary);text-align:right;height:60rpx}
.info-value-row{flex:1;display:flex;align-items:center;justify-content:flex-end;gap:10rpx}
.info-value-row text{font-size:28rpx;color:var(--text-primary)}
.info-textarea{width:100%;font-size:28rpx;color:var(--text-primary);min-height:150rpx;padding:20rpx 24rpx;background:var(--surface);border-radius:16rpx;box-sizing:border-box}

.save-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:48rpx;box-shadow:var(--shadow-sm)}
.save-btn:active{transform:scale(.95)}
.save-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
