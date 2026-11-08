<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { authService } from '@/services/auth'
import { dashboardMenu } from '@/config/menu'
import { getRoleLabel } from '@/lib/access'
import { dismissNotice, notifications, notify } from '@/lib/notify'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)
const busy = ref(false)
const dark = ref(false)
const workspace = computed(() => route.path.startsWith('/dashboard'))
const menu = computed(() => dashboardMenu.filter(item => (auth.user?.roleLevel ?? -1) >= item.minLevel))
onMounted(() => {
  dark.value = localStorage.getItem('csa-theme') === 'dark'
  document.documentElement.dataset.theme = dark.value ? 'dark' : 'light'
  if (!auth.initialized) void auth.refreshSession().catch(() => {})
})
function toggleTheme() {
  dark.value = !dark.value
  localStorage.setItem('csa-theme', dark.value ? 'dark' : 'light')
  document.documentElement.dataset.theme = dark.value ? 'dark' : 'light'
}
async function logout() {
  busy.value = true
  try {
    await authService.logout()
    auth.logout()
    await router.push('/login')
    notify.success('已退出登录')
  } catch (error) {
    notify.error(error instanceof Error ? error.message : '退出失败，请重试')
  } finally { busy.value = false }
}
</script>

<template>
  <a class="skip-link" href="#main">跳到主要内容</a>
  <header class="site-header">
    <RouterLink class="brand" to="/" aria-label="CSA 计算机协会首页"><span class="brand-mark">C</span><span>CSA<span class="brand-sub">计算机协会</span></span></RouterLink>
    <button class="secondary mobile-nav-toggle" :aria-expanded="menuOpen" aria-controls="site-nav" @click="menuOpen = !menuOpen">导航</button>
    <nav id="site-nav" :class="{ open: menuOpen }" aria-label="主导航" @click="menuOpen = false">
      <RouterLink to="/">首页</RouterLink><RouterLink to="/about">关于</RouterLink>
      <RouterLink to="/competitions">竞赛</RouterLink><RouterLink to="/resources">资源</RouterLink>
      <RouterLink to="/contributors">贡献者</RouterLink>
    </nav>
    <div class="header-actions">
      <button class="icon-button secondary" :aria-label="dark ? '切换浅色模式' : '切换深色模式'" @click="toggleTheme">{{ dark ? '☀' : '◐' }}</button>
      <template v-if="auth.user"><RouterLink class="button secondary" to="/dashboard">工作台</RouterLink><button class="secondary" :disabled="busy" @click="logout">退出</button></template>
      <RouterLink v-else class="button" to="/login">登录 / 加入</RouterLink>
    </div>
  </header>
  <div :class="workspace ? 'workspace-shell' : 'public-shell'">
    <aside v-if="workspace" class="sidebar" aria-label="工作台导航">
      <div class="sidebar-heading"><span class="eyebrow">WORKSPACE</span><strong>{{ auth.user?.username }}</strong><span class="muted">{{ getRoleLabel(auth.user?.roleLevel) }}</span></div>
      <nav><RouterLink v-for="(item, index) in menu" :key="item.href" :to="item.href" :class="{ selected: route.path === item.href }"><span class="menu-number">{{ String(index + 1).padStart(2, '0') }}</span>{{ item.title }}</RouterLink></nav>
      <div class="sidebar-note">一起学习 · 创造 · 分享</div>
    </aside>
    <main id="main" tabindex="-1"><RouterView :key="route.path" /></main>
  </div>
  <footer v-if="!workspace" class="site-footer"><span><strong>CSA</strong> · 让想法在这里发生</span><div><RouterLink to="/about">关于协会</RouterLink><RouterLink to="/privacy">隐私政策</RouterLink></div></footer>
  <div class="notices" aria-live="polite" aria-atomic="false"><div v-for="notice in notifications" :key="notice.id" :class="['notice', notice.kind]" :role="notice.kind === 'error' ? 'alert' : 'status'"><span>{{ notice.message }}</span><button class="secondary" aria-label="关闭通知" @click="dismissNotice(notice.id)">×</button></div></div>
</template>
