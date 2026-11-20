<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getRoleDescription, getRoleLabel } from '@/lib/access'
import { formatBalance, resolveAssetUrl } from '@/lib/format'
import { userService } from '@/services/user'
import { useAuthStore } from '@/store/useAuthStore'
import type { UserInfo } from '@/types/user'
import { errorText } from './helpers'

const auth = useAuthStore()
const profile = ref<UserInfo | null>(null)
const avatarFailed = ref(false)
const displayName = computed(() => profile.value?.realName || auth.user?.username || '个人资料')
const avatar = computed(() => resolveAssetUrl(profile.value?.avatar))
const loading = ref(false)
const error = ref('')
const showPrivate = ref(false)
const privateLabels = new Set(['邮箱', '手机号', 'QQ / 微信', '学号'])
const fields = computed(() => profile.value ? [
  ['账号', profile.value.username], ['邮箱', profile.value.email], ['余额', formatBalance(profile.value.balance)],
  ['用户 ID', profile.value.id], ['手机号', profile.value.phone], ['QQ / 微信', profile.value.contact],
  ['学院', profile.value.college], ['班级', profile.value.className], ['学号', profile.value.studentId],
] : [])
async function load() {
  loading.value = true
  avatarFailed.value = false
  error.value = ''
  try { profile.value = await userService.getInfo() }
  catch (e) { error.value = errorText(e) }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="page stack">
    <header class="profile-header">
      <div class="profile-identity">
        <span class="profile-portrait" aria-hidden="true">
          <img v-if="avatar && !avatarFailed" :src="avatar" alt="" @error="avatarFailed = true" />
          <template v-else>{{ displayName.slice(0,1) }}</template>
        </span>
        <div><h1>{{ displayName }}</h1><span class="badge">{{ getRoleLabel(profile?.roleLevel ?? auth.user?.roleLevel) }}</span></div>
      </div>
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
    <section v-else-if="profile" class="stack">
      <div class="toolbar">
        <h2>我的资料</h2>
        <button type="button" class="secondary" :aria-pressed="showPrivate" aria-controls="profile-fields" @click="showPrivate = !showPrivate">{{ showPrivate ? '隐藏联系方式与学号' : '显示联系方式与学号' }}</button>
      </div>
      <dl id="profile-fields" class="profile-facts">
      <div v-for="[label, value] in fields" :key="String(label)" class="panel">
        <dt class="muted">{{ label }}</dt><dd>{{ value == null || value === '' ? '未填写' : privateLabels.has(String(label)) && !showPrivate ? '已隐藏' : value }}</dd>
      </div>
      </dl>
    </section>
  </section>
</template>
