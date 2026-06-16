<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="product">
        <div class="brand-mark">SO</div>
        <h1>Smart Office</h1>
        <p>企业智能审批与考勤管理系统</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="密码" show-password type="password" />
        </el-form-item>
        <el-button :loading="loading" type="primary" class="login-button" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="demo-users">测试账号：admin / manager / employee / finance，密码 123456</div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.replace((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f8fafc 50%, #eef2ff 100%);
}

.login-panel {
  width: min(420px, calc(100vw - 32px));
  padding: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 45px rgb(15 23 42 / 10%);
}

.product {
  margin-bottom: 28px;
  text-align: center;
}

.brand-mark {
  display: grid;
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  place-items: center;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
}

.product h1 {
  margin: 0;
  font-size: 26px;
}

.product p,
.demo-users {
  color: #6b7280;
}

.demo-users {
  margin-top: 18px;
  font-size: 13px;
  text-align: center;
}

.login-button {
  width: 100%;
}
</style>
