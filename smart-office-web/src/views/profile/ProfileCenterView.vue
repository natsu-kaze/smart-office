<template>
  <section class="profile-page">
    <div class="page-card profile-hero">
      <el-avatar :size="72" :src="profileForm.avatar || undefined">{{ profileInitial }}</el-avatar>
      <div>
        <h2>{{ profile?.realName || profile?.username || '未命名用户' }}</h2>
        <p>{{ profile?.username || '-' }} · {{ roleNames }}</p>
      </div>
    </div>

    <div class="profile-grid">
      <div class="page-card">
        <header class="section-header">
          <h2>基本资料</h2>
          <p>姓名、手机号、邮箱和头像会用于页面展示和后续通知触达。</p>
        </header>
        <el-form :model="profileForm" label-width="88px">
          <el-form-item label="用户名">
            <el-input :model-value="profile?.username" disabled />
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="头像">
            <div class="avatar-row">
              <el-avatar :size="48" :src="profileForm.avatar || undefined">{{ profileInitial }}</el-avatar>
              <el-input v-model="profileForm.avatar" placeholder="上传图片后自动填入，也可手动填写 URL" />
              <input ref="avatarInputRef" class="hidden-file" type="file" accept="image/png,image/jpeg" @change="handleAvatarFile" />
              <el-button :loading="uploadingAvatar" @click="avatarInputRef?.click()">上传图片</el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存资料</el-button>
            <el-button @click="loadProfile">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="page-card">
        <header class="section-header">
          <h2>账号安全</h2>
          <p>修改密码后建议重新登录，避免旧登录态继续使用。</p>
        </header>
        <el-form :model="passwordForm" label-width="88px">
          <el-form-item label="旧密码" required>
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" required>
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" required>
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" :loading="savingPassword" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, getProfile, updateProfile } from '@/api/system'
import { getFilePreviewUrl, uploadFile } from '@/api/file'
import { useAuthStore } from '@/stores/auth'
import { setStoredUser } from '@/utils/token'
import type { UserItem } from '@/types/api'

const authStore = useAuthStore()
const profile = ref<UserItem | null>(null)
const savingProfile = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)
const avatarInputRef = ref<HTMLInputElement>()

const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileInitial = computed(() => (profile.value?.realName || profile.value?.username || 'U').slice(0, 1))
const roleNames = computed(() => profile.value?.roleNames?.join('、') || authStore.user?.roles?.join('、') || '普通员工')

async function loadProfile() {
  profile.value = await getProfile()
  profileForm.realName = profile.value.realName || ''
  profileForm.phone = profile.value.phone || ''
  profileForm.email = profile.value.email || ''
  profileForm.avatar = profile.value.avatar || ''
}

async function handleAvatarFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg'].includes(file.type)) {
    ElMessage.warning('头像仅支持 PNG / JPG 图片')
    return
  }
  uploadingAvatar.value = true
  try {
    const record = await uploadFile(file, 'AVATAR')
    profileForm.avatar = record.url && !record.url.startsWith('minio://')
      ? record.url
      : await getFilePreviewUrl(record.id)
    ElMessage.success('头像图片已上传')
  } finally {
    uploadingAvatar.value = false
  }
}

async function handleSaveProfile() {
  if (!profileForm.realName.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  savingProfile.value = true
  try {
    const updated = await updateProfile({ ...profileForm })
    profile.value = updated
    if (authStore.user) {
      authStore.user = {
        ...authStore.user,
        realName: updated.realName,
        phone: updated.phone,
        email: updated.email,
        avatar: updated.avatar,
      }
      setStoredUser(authStore.user)
    }
    ElMessage.success('个人资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已修改')
  } finally {
    savingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 16px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-hero h2 {
  margin: 0;
  font-size: 22px;
}

.profile-hero p,
.section-header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.72fr);
  gap: 16px;
}

.section-header {
  margin-bottom: 18px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
}

.avatar-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
  align-items: center;
}

.hidden-file {
  display: none;
}

@media (max-width: 1000px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
