<template>
  <section class="error-page page-card">
    <div class="error-code">404</div>
    <h2>页面不存在</h2>
    <p>地址可能输入错误，或者这个功能还没有开放。</p>
    <div class="actions">
      <el-button type="primary" @click="router.push('/dashboard')">返回工作台</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.error-page {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.error-code {
  color: #409eff;
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
}

h2 {
  margin: 18px 0 8px;
  font-size: 24px;
}

p {
  margin: 0;
  color: #667085;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
