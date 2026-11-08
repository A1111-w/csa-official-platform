<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getRoleDescription, getRoleLabel } from '@/lib/access'
import { formatBalance } from '@/lib/format'
import { userService } from '@/services/user'
import { useAuthStore } from '@/store/useAuthStore'
import type { UserInfo } from '@/types/user'
import { errorText } from './helpers'

const auth = useAuthStore()
const profile = ref<UserInfo | null>(null)
const loading = ref(false)
const error = ref('')
const fields = computed(() => profile.value ? [
  ['账号', profile.value.username], ['邮箱', profile.value.email], ['余额', formatBalance(profile.value.balance)],
  ['用户 ID', profile.value.id], ['手机号', profile.value.phone], ['QQ / 微信', profile.value.contact],
  ['学院', profile.value.college], ['班级', profile.value.className], ['学号', profile.value.studentId],
] : [])
async function load() {
  loading.value = true
  error.value = ''
  try { profile.value = await userService.getInfo() }
  catch (e) { error.value = errorText(e) }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="page stack">
    <header class="panel">
      <h1>{{ profile?.realName || auth.user?.username || '个人资料' }}</h1>
      <span class="badge">{{ getRoleLabel(profile?.roleLevel ?? auth.user?.roleLevel) }}</span>
      <p class="muted">{{ getRoleDescription(profile?.roleLevel ?? auth.user?.roleLevel) }}</p>
      <div class="actions">
        <RouterLink class="button secondary" to="/resources">打开资源库</RouterLink>
        <RouterLink class="button secondary" to="/dashboard/settings">账户设置</RouterLink>
        <RouterLink v-if="(auth.user?.roleLevel ?? 0) >= 2" class="button" to="/dashboard/resume">维护简历</RouterLink>
        <button class="secondary" :disabled="loading" @click="load">刷新资料</button>
      </div>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在加载资料…</p>
    <dl v-else class="grid">
      <div v-for="[label, value] in fields" :key="String(label)" class="panel">
        <dt class="muted">{{ label }}</dt><dd>{{ value || '—' }}</dd>
      </div>
    </dl>
  </section>
</template>
