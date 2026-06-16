<template>
  <section class="page-card attendance">
    <header>
      <h2>今日考勤</h2>
      <el-button @click="load">刷新</el-button>
    </header>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="上班时间">{{ today.checkInTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上班状态">{{ today.checkInStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下班时间">{{ today.checkOutTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下班状态">{{ today.checkOutStatus || '-' }}</el-descriptions-item>
    </el-descriptions>
    <div class="actions">
      <el-button type="primary" @click="handleCheckIn">上班打卡</el-button>
      <el-button type="success" @click="handleCheckOut">下班打卡</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkIn, checkOut, getTodayAttendance } from '@/api/attendance'
import type { AttendanceToday } from '@/types/api'

const today = ref<AttendanceToday>({})

async function load() {
  today.value = await getTodayAttendance()
}

async function handleCheckIn() {
  today.value = await checkIn()
  ElMessage.success('上班打卡成功')
}

async function handleCheckOut() {
  today.value = await checkOut()
  ElMessage.success('下班打卡成功')
}

onMounted(load)
</script>

<style scoped>
.attendance header,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
}

.attendance h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.actions {
  justify-content: flex-start;
  margin-top: 18px;
}
</style>
