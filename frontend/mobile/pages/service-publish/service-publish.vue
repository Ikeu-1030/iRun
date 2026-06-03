<template>
  <view class="page">
    <uni-nav-bar title="发布需求" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-header">
        <text class="page-title">{{ pageTitle }}</text>
        <text class="page-subtitle">{{ pageSubtitle }}</text>
      </view>

      <!-- ===== 代取快递 type=1 ===== -->
      <template v-if="taskType === 1">
        <view class="form-card">
          <view class="card-title">取件信息</view>
          <view class="form-label">取件驿站</view>
          <view class="addr-row">
            <view class="addr-badge addr-badge--pickup">取</view>
            <view class="form-addr-card" @click="onSelectStation">
              <text :class="{ 'addr-main': true, 'addr-placeholder': !pickupAddress }">{{ pickupAddress || '选择快递所在的驿站或快递柜' }}</text>
              <iconpark-icon name="right" size="18" color="#D4D2CC" />
            </view>
          </view>
          <view class="form-label">取件码</view>
          <input class="form-input form-input--large" placeholder="输入取件码，如：1-2-3456" v-model="pickupCode" />
          <view class="form-label">快递大小与数量（最多3件）</view>
          <view class="package-qty-row" v-for="pkg in packageSizes" :key="pkg.key">
            <text class="package-label">{{ pkg.label }}</text>
            <text class="package-fee">¥{{ pkg.baseFee }}/件</text>
            <view class="qty-stepper">
              <view class="qty-btn" :class="{ 'qty-btn--disabled': packageQtys[pkg.key] <= 0 }" @click="decreaseQty(pkg.key)">−</view>
              <text class="qty-value">{{ packageQtys[pkg.key] }}</text>
              <view class="qty-btn" :class="{ 'qty-btn--disabled': totalPackageQty >= 3 }" @click="increaseQty(pkg.key)">+</view>
            </view>
          </view>
          <view class="package-hint" v-if="totalPackageQty >= 3">
            <iconpark-icon name="info-filled" size="16" color="#F59E0B" />
            <text>最多选择3件快递</text>
          </view>
        </view>
        <view class="form-card">
          <view class="card-title">配送信息</view>
          <view class="addr-row">
            <view class="addr-badge addr-badge--deliver">收</view>
            <view class="form-addr-card" @click="onSelectAddress">
              <view class="addr-info">
                <text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text>
              </view>
              <text class="select-arrow">›</text>
            </view>
          </view>
        </view>
        <view class="form-card">
          <view class="card-title">需求描述</view>
          <textarea class="form-textarea" placeholder="补充快递相关信息，如：快递公司、包裹内容等…" v-model="description" />
        </view>
      </template>

      <!-- ===== 代拿餐食 type=2 ===== -->
      <template v-if="taskType === 2">
        <view class="form-card">
          <view class="card-title">取餐信息</view>
          <view class="form-label">餐饮类型</view>
          <view class="chip-row">
            <view class="chip" :class="{ 'chip--active': subType === 21 }" @click="subType = 21">校内餐饮</view>
            <view class="chip" :class="{ 'chip--active': subType === 22 }" @click="subType = 22">校外餐饮</view>
          </view>

          <!-- 校外餐饮 → 选择取餐地点 -->
          <template v-if="subType === 22">
            <view class="form-label">取餐地点</view>
            <view class="addr-row">
              <view class="addr-badge addr-badge--pickup">取</view>
              <picker mode="selector" :range="offCampusLocations" @change="onOffCampusLocation" style="flex:1">
                <view class="form-select">
                  <text :class="{ 'form-select-placeholder': !pickupAddress }">{{ pickupAddress || '选择外卖柜地点' }}</text>
                  <text class="select-arrow">▼</text>
                </view>
              </picker>
            </view>
            <view v-if="pickupAddress === '自定义'" class="form-label">自定义取餐地点</view>
            <input v-if="pickupAddress === '自定义'" class="form-input" placeholder="请输入具体取餐地点" v-model="customPickupAddress" />
          </template>

          <!-- 校内餐饮 → 选择餐厅 -->
          <template v-if="subType === 21">
            <view class="form-label">餐厅名称</view>
            <view class="addr-row">
              <view class="addr-badge addr-badge--pickup">取</view>
              <picker mode="selector" :range="restaurants" @change="onRestaurantChange" style="flex:1">
                <view class="form-select">
                  <text :class="{ 'form-select-placeholder': !pickupAddress }">{{ pickupAddress || '请选择取餐餐厅' }}</text>
                  <text class="select-arrow">▼</text>
                </view>
              </picker>
            </view>
            <view class="form-label">商家信息</view>
            <input class="form-input" placeholder="如：XX店/XX窗口" v-model="merchantInfo" />
          </template>

          <view class="form-label">{{ subType === 22 ? '需求详情' : '餐品详情' }}</view>
          <textarea class="form-textarea" :placeholder="subType === 22 ? '请描述你的取餐需求，如：XX餐厅取餐…' : '例如：一份大碗牛肉面，多加香菜，微辣…'" v-model="description" />
          <view class="form-label">取餐码 / 订单号</view>
          <input class="form-input" placeholder="输入取餐号码，如：A042" v-model="pickupCode" />
        </view>
        <view class="form-card">
          <view class="card-title">配送信息</view>
          <view class="addr-row">
            <view class="addr-badge addr-badge--deliver">收</view>
            <view class="form-addr-card" @click="onSelectAddress">
              <view class="addr-info">
                <text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text>
              </view>
              <text class="select-arrow">›</text>
            </view>
          </view>
        </view>
        <view v-if="subType === 22" class="form-card">
          <view class="card-title">备注（仅接单员可见）</view>
          <textarea class="form-textarea" placeholder="接单成功后对方可见的私密信息，如：口味偏好、门牌号…" v-model="privateFoodNote" />
        </view>
      </template>

      <!-- ===== 校内代办 type=3 ===== -->
      <template v-if="taskType === 3">
        <view class="form-card">
          <view class="card-title">代办类型</view>
          <view class="chip-row">
            <view v-for="et in errandTypes" :key="et.value" class="chip" :class="{ 'chip--active': subType === et.value }" @click="subType = et.value">{{ et.label }}</view>
          </view>
        </view>
        <view v-if="subType !== null && subType !== 33 && subType !== 35" class="form-card">
          <view class="card-title">任务描述</view>
          <textarea class="form-textarea" placeholder="详细说明您的代办需求…" v-model="description" />
        </view>
        <!-- 物品急送 subType=33 -->
        <template v-if="subType === 33">
          <view class="form-card">
            <view class="card-title">物品信息</view>
            <view class="form-label">物品名称</view>
            <input class="form-input" placeholder="例如：文件、钥匙、快递" v-model="description" />
            <view class="form-label">重量估算</view>
            <view class="chip-row">
              <view v-for="w in weights" :key="w.label" class="chip" :class="{ 'chip--active': itemWeight === w.label }" @click="itemWeight = w.label">{{ w.label }}</view>
            </view>
          </view>
          <view class="form-card">
            <view class="card-title">取件信息</view>
            <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="从哪里取？" v-model="pickupAddress" style="flex:1" /></view>
          </view>
          <view class="form-card">
            <view class="card-title">配送信息</view>
            <view class="addr-row">
              <view class="addr-badge addr-badge--deliver">收</view>
              <view class="form-addr-card" @click="onSelectAddress">
                <view class="addr-info"><text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text></view>
                <text class="select-arrow">›</text>
              </view>
            </view>
          </view>
          <view class="form-card">
            <view class="card-title">备注说明</view>
            <textarea class="form-textarea" placeholder="补充说明，如：急用、指定品牌…" v-model="remark" />
          </view>
          <view class="form-card">
            <view class="card-title">任务描述（仅接单员可见）</view>
            <textarea class="form-textarea" placeholder="接单成功后对方可见的私密信息…" v-model="privateRemark" />
          </view>
        </template>
        <!-- 图书馆还书 subType=32 -->
        <view v-if="subType === 32" class="form-card">
          <view class="card-title">取件地点</view>
          <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="如：东区宿舍3号楼 501" v-model="pickupAddress" style="flex:1" /></view>
        </view>
        <view v-if="subType === 32" class="form-card">
          <view class="card-title">还书信息</view>
          <view class="form-label">书本数量</view>
          <view class="qty-stepper" style="justify-content:flex-start">
            <view class="qty-btn" :class="{ 'qty-btn--disabled': bookCount <= 1 }" @click="bookCount > 1 && bookCount--">−</view>
            <text class="qty-value">{{ bookCount }}</text>
            <view class="qty-btn" :class="{ 'qty-btn--disabled': bookCount >= 5 }" @click="bookCount < 5 && bookCount++">+</view>
          </view>
          <view class="info-hint" style="margin-top:16rpx">
            <iconpark-icon name="info" size="18" color="#FF6B4A" />
            <text>还书地点默认为图书馆，系统将自动匹配</text>
          </view>
        </view>
        <view v-if="subType === 32" class="form-card">
          <view class="card-title">联系信息</view>
          <input class="form-input" placeholder="联系人姓名" style="margin-bottom:16rpx" v-model="deliveryContactName" />
          <input class="form-input" placeholder="联系电话" name="number" v-model="deliveryContactPhone" />
        </view>
        <!-- 帮扔杂物 subType=34 -->
        <view v-if="subType === 34" class="form-card">
          <view class="card-title">取件地点</view>
          <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="如：东区宿舍3号楼 501" v-model="pickupAddress" style="flex:1" /></view>
          <view class="info-hint" style="margin-top:16rpx">
            <iconpark-icon name="info" size="18" color="#FF6B4A" />
            <text>无需填写配送终点，跑腿员将帮您带下楼</text>
          </view>
        </view>
        <view v-if="subType === 34" class="form-card">
          <view class="card-title">联系信息</view>
          <input class="form-input" placeholder="联系人姓名" style="margin-bottom:16rpx" v-model="deliveryContactName" />
          <input class="form-input" placeholder="联系电话" name="number" v-model="deliveryContactPhone" />
        </view>
        <!-- 办事代排 subType=35 -->
        <template v-if="subType === 35">
          <view class="form-card">
            <view class="card-title">服务时长</view>
            <view class="chip-row">
              <view v-for="d in serviceDurationOptions" :key="d.value" class="chip" :class="{ 'chip--active': selectedDuration === d.value }" @click="selectedDuration = d.value">{{ d.label }}</view>
            </view>
            <template v-if="selectedDuration === -1">
              <view class="form-label" style="margin-top:18rpx">自定义时长（分钟）</view>
              <picker mode="selector" :range="customDurationOptions" @change="onCustomDuration">
                <view class="form-select">
                  <text :class="{ 'form-select-placeholder': !customMinutes }">{{ customMinutes ? customMinutes + '分钟' : '请选择时长' }}</text>
                  <text class="select-arrow">▼</text>
                </view>
              </picker>
            </template>
            <view class="info-hint" style="margin-top:12rpx">
              <iconpark-icon name="info" size="18" color="#FF6B4A" />
              <text>服务费 = ¥5 / 10分钟</text>
            </view>
          </view>
          <view class="form-card">
            <view class="card-title">任务描述（公开）</view>
            <textarea class="form-textarea" placeholder="所有用户可见的任务描述…" v-model="description" />
          </view>
          <view class="form-card">
            <view class="card-title">任务描述（仅接单员可见）</view>
            <textarea class="form-textarea" placeholder="接单成功后对方可见的私密信息，如：办事窗口、注意事项…" v-model="privateDescription" />
          </view>
          <view class="form-card">
            <view class="card-title">服务地点</view>
            <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="如：行政楼1楼教务处" v-model="pickupAddress" style="flex:1" /></view>
            <view class="info-hint">
              <iconpark-icon name="info" size="18" color="#FF6B4A" />
              <text>无需填写配送终点，跑腿员将在指定地点排队代办</text>
            </view>
          </view>
          <view class="form-card">
            <view class="card-title">联系信息</view>
            <input class="form-input" placeholder="联系人姓名" style="margin-bottom:16rpx" v-model="deliveryContactName" />
            <input class="form-input" placeholder="联系电话" name="number" v-model="deliveryContactPhone" />
          </view>
        </template>

        <!-- 自定义代办（subType=null） -->
        <template v-if="subType === null">
          <view class="form-card">
            <view class="card-title">任务描述（公开）</view>
            <textarea class="form-textarea" placeholder="所有用户可见的任务描述…" v-model="description" />
          </view>
          <view class="form-card">
            <view class="card-title">任务描述（仅接单员可见）</view>
            <textarea class="form-textarea" placeholder="接单成功后对方可见的私密信息，如：取件码、联系方式…" v-model="privateDescription" />
          </view>
          <view class="form-card">
            <view class="card-title">取件信息</view>
            <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="从哪里取？" v-model="pickupAddress" style="flex:1" /></view>
          </view>
          <view class="form-card">
            <view class="card-title">配送信息</view>
            <view class="addr-row">
              <view class="addr-badge addr-badge--deliver">收</view>
              <view class="form-addr-card" @click="onSelectAddress">
                <view class="addr-info"><text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text></view>
                <text class="select-arrow">›</text>
              </view>
            </view>
          </view>
        </template>

        <!-- 截止时间（所有校内代办子选项） -->
        <view class="form-card">
          <view class="card-title">截止时间（选填）</view>
          <view class="time-picker-row">
            <picker mode="date" :value="deadlineDate" :start="minDate" @change="onDeadlineDateChange">
              <view class="form-select form-select--half"><text>{{ deadlineDate || '选择日期' }}</text><text class="select-arrow">▼</text></view>
            </picker>
            <picker mode="time" :value="deadlineTime" :start="minTime" @change="onDeadlineTimeChange">
              <view class="form-select form-select--half"><text>{{ deadlineTime || '选择时间' }}</text><text class="select-arrow">▼</text></view>
            </picker>
          </view>
          <view class="deadline-hint">
            <iconpark-icon name="info-filled" size="16" color="#F59E0B" />
            <text>可选，默认截止时间为60分钟</text>
          </view>
        </view>
      </template>

      <!-- ===== 代购物品 type=4 ===== -->
      <template v-if="taskType === 4">
        <view class="form-card">
          <view class="card-title">代购类型</view>
          <view class="chip-row">
            <view class="chip" :class="{ 'chip--active': subType === 43 }" @click="subType = 43">校内代购</view>
            <view class="chip" :class="{ 'chip--active': subType === 44 }" @click="subType = 44">校外代购</view>
          </view>
        </view>
        <view class="form-card">
          <view class="card-title">商品填写</view>
          <view v-for="(item, index) in productItems" :key="index" class="product-item-row">
            <view class="product-item-header">
              <text class="product-item-index">商品{{ index + 1 }}</text>
              <view v-if="productItems.length > 1" class="product-item-remove" @click="removeProduct(index)">
                <iconpark-icon name="closeempty" size="18" color="#ba1a1a" />
              </view>
            </view>
            <input class="form-input" placeholder="商品名称" v-model="item.name" style="margin-bottom:12rpx" />
            <view class="qty-row">
              <text class="qty-label">数量</text>
              <view class="qty-stepper">
                <view class="qty-btn" :class="{ 'qty-btn--disabled': item.qty <= 1 }" @click="decreaseProductQty(index)">−</view>
                <text class="qty-value">{{ item.qty }}</text>
                <view class="qty-btn" :class="{ 'qty-btn--disabled': item.qty >= 10 }" @click="increaseProductQty(index)">+</view>
              </view>
            </view>
          </view>
          <view class="add-product-btn" @click="addProduct">
            <iconpark-icon name="plusempty" size="20" color="#FF6B4A" />
            <text>添加商品</text>
          </view>
        </view>
        <view class="form-card">
          <view class="card-title">购买地址</view>
          <view class="addr-row"><view class="addr-badge addr-badge--pickup">取</view><input class="form-input" placeholder="从哪里购买？如：蓝区校园超市" v-model="pickupAddress" style="flex:1" /></view>
        </view>
        <view class="form-card">
          <view class="card-title">配送信息</view>
          <view class="addr-row">
            <view class="addr-badge addr-badge--deliver">收</view>
            <view class="form-addr-card" @click="onSelectAddress">
              <view class="addr-info">
                <text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text>
              </view>
              <text class="select-arrow">›</text>
            </view>
          </view>
        </view>
        <view class="form-card">
          <view class="card-title">备注说明（公开）</view>
          <textarea class="form-textarea" placeholder="所有用户可见的备注说明…" v-model="description" />
        </view>
        <view class="form-card">
          <view class="card-title">备注说明（仅接单员可见）</view>
          <textarea class="form-textarea" placeholder="接单成功后对方可见的私密信息，如：指定品牌、口味…" v-model="privateDescription" />
        </view>
        <!-- 截止时间 -->
        <view class="form-card">
          <view class="card-title">截止时间（选填）</view>
          <view class="time-picker-row">
            <picker mode="date" :value="deadlineDate" :start="minDate" @change="onDeadlineDateChange">
              <view class="form-select form-select--half"><text>{{ deadlineDate || '选择日期' }}</text><text class="select-arrow">▼</text></view>
            </picker>
            <picker mode="time" :value="deadlineTime" :start="minTime" @change="onDeadlineTimeChange">
              <view class="form-select form-select--half"><text>{{ deadlineTime || '选择时间' }}</text><text class="select-arrow">▼</text></view>
            </picker>
          </view>
          <view class="deadline-hint">
            <iconpark-icon name="info-filled" size="16" color="#F59E0B" />
            <text>可选，默认截止时间为60分钟</text>
          </view>
        </view>
      </template>

      <!-- ===== 上传图片（图书馆还书、下楼顺便扔不需要） ===== -->
      <view v-if="taskType !== 3 || (subType !== 32 && subType !== 34)" class="form-card">
        <view class="card-title">上传信息</view>
        <UploadGrid v-model="uploadedUrls" :maxCount="3" />
      </view>

      <!-- ===== 接单限制 ===== -->
      <view class="form-card">
        <view class="card-title">接单限制（选填）</view>
        <view class="chip-row">
          <view class="chip" :class="{ 'chip--active': requireSex === undefined }" @click="requireSex = undefined">不限</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '男' }" @click="requireSex = '男'">仅男生</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '女' }" @click="requireSex = '女'">仅女生</view>
        </view>
      </view>

      <!-- ===== 费用 ===== -->
      <view class="form-card form-card--pay">
        <view class="card-title">费用明细</view>
        <view class="fee-row">
          <text class="fee-label">{{ taskType === 3 && subType === 35 ? '基础服务费' : '基础配送费' }}</text>
          <text class="fee-value">¥ {{ baseFee.toFixed(2) }}</text>
        </view>
        <template v-if="taskType === 4">
          <view class="form-label" style="margin-top:18rpx">预估商品费</view>
          <view class="custom-bounty-row">
            <text class="custom-bounty-unit">¥</text>
            <input class="custom-bounty-input" name="digit" v-model.number="estimatedProductFee" placeholder="输入预估商品费用" />
          </view>
          <view class="info-hint" style="margin-top:12rpx">
            <iconpark-icon name="info" size="18" color="#FF6B4A" />
            <text>配送员垫付商品费，送达后当面结算</text>
          </view>
          <view class="fee-divider"></view>
        </template>
        <view class="fee-divider" v-if="taskType !== 4"></view>
        <view class="form-label">赏金（提高接单率）</view>
        <view class="chip-row bounty-chips">
          <view class="chip" :class="{ 'chip--active': reward === 2 && !showCustomBounty }" @click="setReward(2)">¥2</view>
          <view class="chip" :class="{ 'chip--active': reward === 5 && !showCustomBounty }" @click="setReward(5)">¥5</view>
          <view class="chip" :class="{ 'chip--active': reward === 10 && !showCustomBounty }" @click="setReward(10)">¥10</view>
          <view class="chip" :class="{ 'chip--active': showCustomBounty }" @click="toggleCustomReward">自定义</view>
        </view>
        <view class="custom-bounty-row" v-if="showCustomBounty">
          <text class="custom-bounty-unit">¥</text>
          <input class="custom-bounty-input" name="digit" v-model.number="customBounty" placeholder="输入赏金金额" @input="onCustomBountyInput" />
        </view>
        <view class="fee-divider"></view>
        <view class="fee-row fee-row--total">
          <text class="fee-label">合计支付</text>
          <text class="fee-total">¥ {{ totalReward.toFixed(2) }}</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-bar-row">
        <view class="bottom-total">
          <text class="total-label">合计支付</text>
          <text class="total-price">¥ {{ totalReward.toFixed(2) }}</text>
        </view>
        <view class="submit-btn" :class="{ 'submit-btn--disabled': submitting }" @click="onSubmit"><text>{{ submitting ? '发布中…' : '发布需求' }}</text></view>
      </view>
    </view>

    <PayPasswordDialog />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { taskApi } from '@/api'
import { TASK_TYPES, TYPE_TO_API, SUBTYPE_TO_VALUE } from '@/utils/constants.js'
// encodeDescription removed — backend reads publicDesc + privateNote directly
import { promptPayPassword } from '@/utils/pay-password.js'
import { guideToSetPayPassword } from '@/utils/error'
import { useSubmitLock } from '@/utils/submit-guard'
import PayPasswordDialog from '@/components/pay-password-dialog/pay-password-dialog.vue'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const taskType = ref(1)
const subType = ref(11)
const description = ref('')
const privateDescription = ref('')
const remark = ref('')
const privateRemark = ref('')
const pickupCode = ref('')
const restaurants = ['正阳餐厅', '正阳餐厅后花园餐厅', '霞光餐厅', '晨曦餐厅一楼', '晨曦餐厅二楼', '蓝区体育场小吃街', '霞光片区小吃街', '晨曦片区小吃街']
const pickupAddress = ref('')
const merchantInfo = ref('')
const customPickupAddress = ref('')
const deliveryAddressId = ref(null)
const deliveryLabel = ref('')
const deliveryContactName = ref('')
const deliveryContactPhone = ref('')
const requireSex = ref(undefined) // undefined=不限, '男'=仅男生, '女'=仅女生
const reward = ref(0)
const customBounty = ref(0)
const uploadedUrls = ref([])
const deadlineDate = ref('')
const deadlineTime = ref('')

const minDate = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const minTime = computed(() => {
  const today = minDate
  if (deadlineDate.value && deadlineDate.value !== today) return ''
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
})
const itemWeight = ref('< 1kg')
const bookCount = ref(1)
const privateFoodNote = ref('')
const estimatedProductFee = ref(0)
const productItems = ref([{ name: '', qty: 1 }])
const { lock, unlock, locked: submitting } = useSubmitLock()
const showCustomBounty = ref(false)

// 办事代排 subType=35 服务时长
const serviceDurationOptions = [
  { value: 10, label: '10分钟' },
  { value: 30, label: '30分钟' },
  { value: 60, label: '1小时' },
  { value: -1, label: '自定义' }
]
const selectedDuration = ref(10)
const customMinutes = ref(10)
const customDurationOptions = Array.from({ length: 12 }, (_, i) => `${(i + 1) * 10}分钟`)
function onCustomDuration(e) {
  customMinutes.value = (Number(e.detail.value) + 1) * 10
}
const serviceDurationFee = computed(() => {
  if (selectedDuration.value === -1) {
    const mins = Math.max(10, customMinutes.value || 10)
    return Math.ceil(mins / 10) * 5
  }
  return Math.ceil(selectedDuration.value / 10) * 5
})

// 代取快递批量选择
const packageSizes = [
  { key: 'small', label: '小件', baseFee: 3 },
  { key: 'medium', label: '中件', baseFee: 6 },
  { key: 'large', label: '大件', baseFee: 10 }
]
const packageQtys = ref({ small: 0, medium: 0, large: 0 })
const totalPackageQty = computed(() => packageQtys.value.small + packageQtys.value.medium + packageQtys.value.large)
function increaseQty(key) {
  if (totalPackageQty.value >= 3) return
  packageQtys.value[key]++
}
function decreaseQty(key) {
  if (packageQtys.value[key] <= 0) return
  packageQtys.value[key]--
}

const offCampusLocations = ['西北门外卖柜', '东北1门外卖柜', '东南门外卖柜', '西南1门外卖柜', '西南3门外卖柜', '自定义']

const pageTitle = computed(() => TASK_TYPES[taskType.value] || '发布需求')
const pageSubtitleMap = { 1: '快递急需取？我们来帮您！', 2: '想吃啥就下单，即刻为你送达宿舍。', 3: '快速发布需求，帮取马即刻接单。', 4: '超市代购一键下单，极速配送到寝。' }
const pageSubtitle = computed(() => pageSubtitleMap[taskType.value] || '')

const baseFee = computed(() => {
  if (taskType.value === 1) {
    return packageQtys.value.small * 3 + packageQtys.value.medium * 6 + packageQtys.value.large * 10
  }
  if (taskType.value === 3 && subType.value === 35) return serviceDurationFee.value
  return 5
})

const totalReward = computed(() => {
  const bounty = showCustomBounty.value ? customBounty.value || 0 : reward.value
  const productFee = taskType.value === 4 ? (estimatedProductFee.value || 0) : 0
  return baseFee.value + productFee + bounty
})

const skipDelivery = computed(() => {
  return (taskType.value === 3 && (subType.value === 32 || subType.value === 34 || subType.value === 35))
})

const errandTypes = [
  { value: 33, label: '物品急送' },
  { value: 35, label: '办事代排' },
  { value: 32, label: '图书馆还书' },
  { value: 34, label: '帮扔杂物' },
  { value: null, label: '自定义' }
]
const weights = [{ label: '< 1kg' }, { label: '1-3kg' }, { label: '> 3kg' }]

onLoad((options) => {
  const t = Number(options?.type) || 1
  taskType.value = t
  const defaultSubMap = { 1: 11, 2: 21, 3: 33, 4: 43 }
  subType.value = defaultSubMap[t] || 11
  if (t === 2) pickupAddress.value = ''
})

function setReward(val) {
  showCustomBounty.value = false
  reward.value = val
}

function toggleCustomReward() {
  showCustomBounty.value = !showCustomBounty.value
  if (showCustomBounty.value && customBounty.value === 0) {
    customBounty.value = reward.value
  }
}

function onCustomBountyInput() { /* 自定义赏金由 totalReward 计算 */ }

function onRestaurantChange(e) {
  pickupAddress.value = restaurants[Number(e.detail.value)]
}

function onOffCampusLocation(e) {
  const val = offCampusLocations[e.detail.value]
  if (val === '自定义') {
    pickupAddress.value = '自定义'
  } else {
    pickupAddress.value = val
  }
}


function onBack() { uni.navigateBack() }

function onSelectStation() {
  uni.$off('stationSelected')
  uni.$on('stationSelected', (name) => {
    pickupAddress.value = name
    uni.$off('stationSelected')
  })
  uni.navigateTo({ url: '/pages/station-select/station-select' })
}

function onSelectAddress() {
  uni.$off('addressSelected')
  uni.$on('addressSelected', (addr) => {
    deliveryAddressId.value = addr.id
    deliveryLabel.value = `${addr.contactName} ${addr.detail}`
    deliveryContactName.value = addr.contactName || ''
    deliveryContactPhone.value = addr.contactPhone || ''
    uni.$off('addressSelected')
  })
  uni.navigateTo({ url: '/pages/address-list/address-list?selectMode=1' })
}

onUnmounted(() => {
  uni.$off('stationSelected')
  uni.$off('addressSelected')
})

function onDeadlineDateChange(e) {
  deadlineDate.value = e.detail.value
}

function onDeadlineTimeChange(e) {
  deadlineTime.value = e.detail.value
}

function addProduct() {
  productItems.value.push({ name: '', qty: 1 })
}

function removeProduct(index) {
  if (productItems.value.length <= 1) return
  productItems.value.splice(index, 1)
}

function decreaseProductQty(index) {
  if (productItems.value[index].qty <= 1) return
  productItems.value[index].qty--
}

function increaseProductQty(index) {
  if (productItems.value[index].qty >= 10) return
  productItems.value[index].qty++
}

async function onSubmit() {
  // 校验
  if (taskType.value === 1) {
    if (totalPackageQty.value === 0) {
      uni.showToast({ title: '请至少选择1件快递', icon: 'none' })
      return
    }
  }
  if (taskType.value === 4) {
    const validProducts = productItems.value.filter(p => p.name.trim())
    if (validProducts.length === 0) {
      uni.showToast({ title: '请至少填写1件商品', icon: 'none' })
      return
    }
    if (!pickupAddress.value) {
      uni.showToast({ title: '请填写购买地址', icon: 'none' })
      return
    }
  }
  if (taskType.value === 3) {
    if (subType.value === 33 && !description.value) {
      uni.showToast({ title: '请填写物品名称', icon: 'none' })
      return
    }
    if (subType.value === 35 && !description.value) {
      uni.showToast({ title: '请填写任务描述', icon: 'none' })
      return
    }
  }
  if (taskType.value === 2 && subType.value === 22) {
    if (!pickupAddress.value || pickupAddress.value === '自定义' && !customPickupAddress.value) {
      uni.showToast({ title: '请选择取餐地点', icon: 'none' })
      return
    }
  }

  // 弹出支付密码输入框（发布任务需付款）
  const pw = await promptPayPassword('支付赏金')
  if (!pw) return

  if (!lock()) return
  try {
    // 计算实际取件地址
    let actualPickupAddress = pickupAddress.value
    if (taskType.value === 2 && subType.value === 22 && pickupAddress.value === '自定义') {
      actualPickupAddress = customPickupAddress.value
    }

    // 计算 expireMinutes（从截止时间推算）
    let expireMinutes
    if (deadlineDate.value && deadlineTime.value) {
      const [year, month, day] = deadlineDate.value.split('-').map(Number)
      const [hour, minute] = deadlineTime.value.split(':').map(Number)
      const deadlineMs = new Date(year, month - 1, day, hour, minute).getTime()
      const nowMs = Date.now()
      const diffMinutes = Math.max(10, Math.ceil((deadlineMs - nowMs) / 60000))
      expireMinutes = Math.min(diffMinutes, 1440)
    }

    // 构建 taskSpecs
    let subTypeValue
    let taskSpecsStr
    if (taskType.value === 1) {
      const packages = []
      if (packageQtys.value.small > 0) packages.push({ 规格: '小件', 费用: 3, 数量: packageQtys.value.small })
      if (packageQtys.value.medium > 0) packages.push({ 规格: '中件', 费用: 6, 数量: packageQtys.value.medium })
      if (packageQtys.value.large > 0) packages.push({ 规格: '大件', 费用: 10, 数量: packageQtys.value.large })
      taskSpecsStr = JSON.stringify({ 包裹列表: packages })
      subTypeValue = undefined
    } else if (taskType.value === 3 && subType.value === 33) {
      const name = description.value || '急送物品'
      taskSpecsStr = JSON.stringify({ 物品名称: name, 重量: itemWeight.value })
    } else if (taskType.value === 3 && subType.value === 35) {
      const duration = selectedDuration.value === -1
        ? Math.max(10, customMinutes.value || 10)
        : selectedDuration.value
      const fee = Math.ceil(duration / 10) * 5
      const endTime = new Date(Date.now() + duration * 60000)
      const pad = n => String(n).padStart(2, '0')
      const serviceEndTime = `${endTime.getFullYear()}-${pad(endTime.getMonth() + 1)}-${pad(endTime.getDate())}T${pad(endTime.getHours())}:${pad(endTime.getMinutes())}:${pad(endTime.getSeconds())}`
      taskSpecsStr = JSON.stringify({
        服务时长: duration,
        时长标签: `${duration}分钟`,
        基础服务费: fee,
        serviceEndTime
      })
    } else if (taskType.value === 3 && subType.value === 32) {
      taskSpecsStr = JSON.stringify({ 书本数量: bookCount.value })
    } else if (taskType.value === 4) {
      const validProducts = productItems.value.filter(p => p.name.trim())
      const items = validProducts.map(p => ({ 名称: p.name.trim(), 数量: p.qty }))
      taskSpecsStr = JSON.stringify({
        商品列表: items,
        预估商品费: estimatedProductFee.value > 0 ? Number(estimatedProductFee.value) : 0
      })
      subTypeValue = SUBTYPE_TO_VALUE[subType.value]
    }

    // 办事代排不传 deliveryAddressId，后端标记无需送达
    const skipDeliveryPayload = taskType.value === 3 && subType.value === 35

    // 公开描述 & 私密备注（新字段，优先于 description）
    let publicDesc, privateNote
    if (taskType.value === 1) {
      publicDesc = description.value || ''
      privateNote = undefined
    } else if (taskType.value === 3 && subType.value === 33) {
      publicDesc = remark.value || ''
      privateNote = privateRemark.value || undefined
    } else if (taskType.value === 3 && (subType.value === 35 || subType.value === null)) {
      publicDesc = description.value || ''
      privateNote = privateDescription.value || undefined
    } else if (taskType.value === 3 && subType.value === 32) {
      publicDesc = description.value || ''
      privateNote = undefined
    } else if (taskType.value === 4) {
      publicDesc = description.value || ''
      privateNote = privateDescription.value || undefined
    } else if (taskType.value === 2 && subType.value === 22) {
      publicDesc = description.value || ''
      privateNote = privateFoodNote.value || undefined
    } else if (taskType.value === 2 && subType.value === 21) {
      publicDesc = merchantInfo.value ? `商家：${merchantInfo.value}；${description.value || ''}` : (description.value || '')
      privateNote = undefined
    } else {
      publicDesc = description.value || ''
      privateNote = undefined
    }

    const payload = {
      type: TYPE_TO_API[taskType.value],
      subType: subTypeValue || (taskType.value === 1 || taskType.value === 4 ? undefined : SUBTYPE_TO_VALUE[subType.value]) || undefined,
      publicDesc: publicDesc || undefined,
      privateNote: privateNote || undefined,
      taskSpecs: taskSpecsStr || undefined,
      reward: parseFloat(totalReward.value.toFixed(2)),
      payPassword: pw,
      pickupCode: pickupCode.value || undefined,
      pickupAddress: actualPickupAddress || undefined,
      deliveryAddressId: skipDeliveryPayload ? undefined : (deliveryAddressId.value || undefined),
      expireMinutes: expireMinutes || undefined,
      contactName: deliveryContactName.value || undefined,
      contactPhone: deliveryContactPhone.value || undefined,
      requireSex: requireSex.value,
      imageUrls: uploadedUrls.value.length > 0 ? [...uploadedUrls.value] : undefined
    }

    await taskApi.publishTask(payload)
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/order-success/order-success?reward=${totalReward.value}&type=${taskType.value}`
      })
    }, 800)
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) {
      guideToSetPayPassword()
    }
  } finally {
    unlock()
  }
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:180rpx}
.page-header{margin-top:16rpx;margin-bottom:20rpx}
.page-title{font-size:44rpx;font-weight:700;color:var(--text-primary);display:block}
.page-subtitle{font-size:26rpx;color:var(--text-secondary);margin-top:6rpx;display:block}

.form-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:28rpx;margin-bottom:20rpx;box-shadow:var(--shadow-sm);border:1rpx solid var(--outline-light)}
.form-card--pay{border:2rpx solid var(--primary-container)}
.form-card--half{flex:1;min-width:0}
.form-row{display:flex;gap:20rpx;margin-bottom:20rpx}
.card-title{font-size:30rpx;font-weight:600;color:var(--text-primary);margin-bottom:20rpx;padding-bottom:16rpx;border-bottom:1rpx solid var(--outline-light);display:flex;align-items:center;gap:10rpx}
.form-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);margin-bottom:10rpx;margin-top:18rpx}
.form-label:first-child{margin-top:0}
.form-input{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.form-input--large{height:100rpx;font-size:30rpx;font-weight:600;letter-spacing:4rpx}
.form-textarea{width:100%;background:var(--surface);border-radius:20rpx;padding:20rpx 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box;min-height:150rpx}
.form-select{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-secondary);display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.form-select-placeholder{color:var(--text-tertiary)}
.select-arrow{font-size:32rpx;color:var(--text-secondary)}
.form-addr-card{width:100%;background:var(--surface);border-radius:20rpx;padding:22rpx 28rpx;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.addr-main{font-size:28rpx;color:var(--text-primary);display:block}
.addr-placeholder{color:var(--text-tertiary)}
.info-hint{display:flex;align-items:center;gap:10rpx;padding:16rpx;background:var(--primary-container);border-radius:12rpx}
.info-hint text{font-size:24rpx;color:var(--primary)}

.chip-row{display:flex;flex-wrap:wrap;gap:14rpx}
.chip{padding:14rpx 28rpx;border-radius:48rpx;font-size:26rpx;font-weight:500;color:var(--text-secondary);background:var(--surface);text-align:center}
.chip--active{background:var(--primary);color:#fff;font-weight:600}

.addr-row{display:flex;align-items:center;gap:14rpx}
.addr-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0}
.addr-badge--pickup{background:var(--text-primary)}
.addr-badge--deliver{background:var(--primary)}
.route-warning{display:flex;align-items:center;gap:8rpx;margin-top:12rpx;padding:12rpx 16rpx;background:#fff7ed;border-radius:12rpx}
.route-warning text{font-size:24rpx;color:#ad6200}

.fee-row{display:flex;align-items:center;justify-content:space-between;padding:8rpx 0}
.fee-row--total{margin-top:8rpx}
.fee-label{font-size:28rpx;color:var(--text-primary)}
.fee-value{font-size:28rpx;font-weight:600;color:var(--text-secondary)}
.fee-total{font-size:38rpx;font-weight:700;color:var(--primary)}
.fee-divider{height:1rpx;background:rgba(0,0,0,.04);margin:20rpx 0}
.custom-bounty-row{display:flex;align-items:center;margin-top:16rpx;background:var(--surface);border-radius:20rpx;padding:14rpx 24rpx}
.custom-bounty-unit{font-size:32rpx;font-weight:700;color:var(--primary);margin-right:8rpx}
.custom-bounty-input{flex:1;font-size:28rpx;color:var(--text-primary);background:transparent}

.time-picker-row{display:flex;gap:16rpx}
.form-select--half{flex:1}
.deadline-hint{display:flex;align-items:center;gap:8rpx;margin-top:16rpx;padding:12rpx 16rpx;background:#fff7ed;border-radius:12rpx}
.deadline-hint text{font-size:22rpx;color:#ad6200}

.bottom-bar{position:fixed;bottom:0;left:0;width:100%;background:var(--surface-raised);border-top:1rpx solid rgba(0,0,0,.06);padding:20rpx 32rpx;box-sizing:border-box;box-shadow:var(--shadow-md);z-index:50;padding-bottom:calc(20rpx + env(safe-area-inset-bottom))}
.bottom-bar-row{display:flex;align-items:center;justify-content:space-between;gap:28rpx}
.bottom-total{flex-shrink:0}
.total-label{font-size:22rpx;color:var(--text-secondary);display:block}
.total-price{font-size:38rpx;font-weight:700;color:var(--primary)}
.submit-btn{flex:1;height:92rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-sm)}
.submit-btn:active{transform:scale(.95)}
.submit-btn--disabled{pointer-events:none;opacity:.6}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}
.package-qty-row{display:flex;align-items:center;gap:16rpx;padding:16rpx 0;border-bottom:1rpx solid var(--surface-hover)}
.package-qty-row:last-of-type{border-bottom:none}
.package-label{font-size:28rpx;font-weight:500;color:var(--text-primary);min-width:80rpx}
.package-fee{font-size:24rpx;color:var(--text-secondary);min-width:100rpx}
.qty-stepper{display:flex;align-items:center;gap:0;margin-left:auto}
.qty-btn{width:60rpx;height:56rpx;display:flex;align-items:center;justify-content:center;background:var(--surface);font-size:32rpx;font-weight:600;color:var(--primary);user-select:none}
.qty-btn:first-child{border-radius:16rpx 0 0 16rpx}
.qty-btn:last-child{border-radius:0 16rpx 16rpx 0}
.qty-btn--disabled{color:var(--text-tertiary);pointer-events:none}
.qty-btn:active:not(.qty-btn--disabled){background:var(--outline-light)}
.qty-value{width:72rpx;text-align:center;font-size:30rpx;font-weight:600;color:var(--text-primary)}
.package-hint{display:flex;align-items:center;gap:8rpx;margin-top:16rpx;padding:12rpx 16rpx;background:#fff7ed;border-radius:12rpx}
.package-hint text{font-size:22rpx;color:#ad6200}

.product-item-row{background:var(--surface);border-radius:20rpx;padding:20rpx 24rpx;margin-bottom:16rpx}
.product-item-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12rpx}
.product-item-index{font-size:26rpx;font-weight:600;color:var(--text-primary)}
.product-item-remove{width:44rpx;height:44rpx;display:flex;align-items:center;justify-content:center}
.quantity-row{display:flex;align-items:center}
.qty-row{display:flex;align-items:center;justify-content:space-between}
.qty-label{font-size:26rpx;font-weight:500;color:var(--text-secondary)}
.form-input--qty{width:160rpx}
.add-product-btn{display:flex;align-items:center;justify-content:center;gap:8rpx;padding:20rpx;border:2rpx dashed var(--outline);border-radius:20rpx;margin-top:12rpx}
.add-product-btn text{font-size:26rpx;color:var(--primary);font-weight:500}
.add-product-btn:active{background:var(--primary-container)}

.bottom-placeholder{height:180rpx}
</style>
