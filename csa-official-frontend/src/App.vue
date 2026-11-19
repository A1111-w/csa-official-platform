<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
const menuToggle = ref<HTMLButtonElement | null>(null)
const busy = ref(false)
const dark = ref(document.documentElement.dataset.theme !== 'light')
const workspace = computed(() => route.path.startsWith('/dashboard'))
const menu = computed(() => dashboardMenu.filter(item => (auth.user?.roleLevel ?? -1) >= item.minLevel))
watch(() => route.path, () => { menuOpen.value = false })
function dismissMenu() {
  if (menuOpen.value) {
    menuOpen.value = false
    menuToggle.value?.focus()
  }
}
onMounted(() => {
  if (!auth.initialized) void auth.refreshSession().catch(() => {})
})
function toggleTheme() {
  dark.value = !dark.value
  const theme = dark.value ? 'dark' : 'light'
  document.documentElement.dataset.theme = theme
  try {
    localStorage.setItem('csa-theme', theme)
  } catch { /* The current tab can still switch themes when storage is unavailable. */ }
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
  <header class="site-header atlas-header" @keydown.esc="dismissMenu">
    <RouterLink class="brand" to="/" aria-label="CSA 计算机协会首页">
      <span class="orbital-mark" aria-hidden="true"></span>
      <span class="brand-name">CSA<span class="brand-sub">计算机协会</span></span>
    </RouterLink>
    <button ref="menuToggle" class="secondary mobile-nav-toggle" :aria-expanded="menuOpen" aria-controls="site-nav" @click="menuOpen = !menuOpen">导航</button>
    <nav id="site-nav" :class="{ open: menuOpen }" aria-label="主导航" @click="menuOpen = false">
      <RouterLink to="/">首页</RouterLink><RouterLink to="/about">关于</RouterLink>
      <RouterLink to="/competitions">竞赛</RouterLink><RouterLink to="/resources">资源</RouterLink>
      <RouterLink to="/contributors">贡献者</RouterLink>
    </nav>
    <div class="header-actions">
      <button class="icon-button secondary" :aria-label="dark ? '切换浅色模式' : '切换深色模式'" @click="toggleTheme"><span class="theme-glyph" aria-hidden="true"></span></button>
      <template v-if="auth.user"><RouterLink class="button secondary" to="/dashboard">工作台</RouterLink><button class="secondary" :disabled="busy" @click="logout">退出</button></template>
      <RouterLink v-else class="button" to="/login">登录 / 加入</RouterLink>
    </div>
  </header>
  <div :class="workspace ? 'workspace-shell' : 'public-shell'" :data-workspace-page="workspace ? (route.path.split('/')[2] || 'overview') : undefined">
    <aside v-if="workspace" class="sidebar" aria-label="工作台导航">
      <div class="sidebar-heading"><span class="eyebrow">工作台</span><strong>{{ auth.user?.username }}</strong><span class="muted">{{ getRoleLabel(auth.user?.roleLevel) }}</span></div>
      <nav aria-label="工作台栏目"><RouterLink v-for="item in menu" :key="item.href" :to="item.href" :class="{ selected: route.path === item.href }">{{ item.title }}</RouterLink></nav>
      <label class="workspace-nav-select">工作台导航
        <select :value="route.path" @change="router.push(($event.target as HTMLSelectElement).value)">
          <option v-for="item in menu" :key="item.href" :value="item.href">{{ item.title }}</option>
        </select>
      </label>
    </aside>
    <main id="main" tabindex="-1"><RouterView :key="route.path" /></main>
  </div>
  <footer v-if="!workspace" class="site-footer atlas-footer"><span><strong>CSA</strong> · 计算机协会</span><div><RouterLink to="/about">关于协会</RouterLink><RouterLink to="/privacy">隐私政策</RouterLink></div></footer>
  <div class="notices" aria-live="polite" aria-atomic="false"><div v-for="notice in notifications" :key="notice.id" :class="['notice', notice.kind]" :role="notice.kind === 'error' ? 'alert' : 'status'"><span>{{ notice.message }}</span><button class="secondary" aria-label="关闭通知" @click="dismissNotice(notice.id)">×</button></div></div>
</template>

<style scoped>
.orbital-mark {
  position: relative;
  display: inline-block;
  width: 2.5rem;
  height: 2.15rem;
  color: var(--accent);
  flex: none;
  background: radial-gradient(circle at 80% 22%, currentColor 0 2px, transparent 2.6px);
}
.orbital-mark::before, .orbital-mark::after {
  position: absolute;
  content: '';
  border-radius: 50%;
}
.orbital-mark::before {
  inset: 19% 19%;
  border: 2px solid currentColor;
  border-right-color: transparent;
  rotate: -23deg;
}
.orbital-mark::after {
  inset: 35% 0;
  border: 1.4px solid currentColor;
  border-top-color: transparent;
  rotate: -29deg;
}
.brand-name { font-family: 'Segoe UI', system-ui, sans-serif; font-weight: 720; letter-spacing: -.055em; }
.theme-glyph {
  width: .95rem;
  height: .95rem;
  border: 1.4px solid currentColor;
  border-radius: 50%;
  background: linear-gradient(90deg, currentColor 50%, transparent 50%);
  rotate: -30deg;
}
:root[data-theme=dark] .theme-glyph { rotate: 150deg; }
.atlas-header, .atlas-footer {
  --nav-edge: max(var(--atlas-gutter), calc((100% - var(--atlas-width)) / 2));
  padding-inline: var(--nav-edge);
  background: var(--nav-bg);
  color: var(--text);
  border-color: color-mix(in srgb, var(--border) 64%, transparent);
}
.atlas-header { height: var(--site-header-height); }
.atlas-header .brand { gap: .6rem; color: var(--text); }
.atlas-header .orbital-mark { color: var(--metal); }
.atlas-header .brand-name {
  display: flex;
  align-items: center;
  gap: .85rem;
  font-size: 1.45rem;
}
.atlas-header .brand-sub {
  border-left: 1px solid var(--border);
  margin: 0;
  padding: .06rem 0 .06rem .85rem;
  color: var(--muted);
  font-size: .75rem;
  font-weight: 500;
  letter-spacing: .12em;
  white-space: nowrap;
}
.atlas-header nav { gap: clamp(1rem, 2.45vw, 2.55rem); }
.atlas-header nav a {
  position: relative;
  padding: .85rem 0;
  color: var(--muted);
  font-size: .87rem;
  font-weight: 500;
}
.atlas-header nav a:is(:hover, :focus-visible, .router-link-exact-active) { color: var(--text); text-decoration: none; }
.atlas-header nav a.router-link-exact-active::after {
  position: absolute;
  bottom: .25rem;
  left: calc(50% - .6rem);
  width: 1.2rem;
  height: 2px;
  border-radius: 2px;
  background: var(--metal);
  content: '';
}
.atlas-header .header-actions { gap: .7rem; }
.atlas-header .header-actions .button, .atlas-header .header-actions button, .atlas-header .mobile-nav-toggle {
  min-height: 38px;
  padding: .45rem 1rem;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: transparent;
  color: var(--text);
  font-size: .8rem;
  font-weight: 550;
}
.atlas-header .header-actions .icon-button {
  width: 38px;
  border-color: transparent;
  padding: .4rem;
  color: var(--muted);
}
.atlas-header .header-actions :is(button, a):hover, .atlas-header .mobile-nav-toggle:hover {
  border-color: var(--metal);
  background: var(--soft);
  filter: none;
}
.atlas-footer { padding-block: 2rem; color: var(--muted); }
.atlas-footer a { color: var(--metal); }
@media (max-width: 760px) {
  .atlas-header { height: auto; min-height: 60px; padding-block: .55rem; gap: .35rem; }
  .atlas-header .brand { gap: .28rem; }
  .atlas-header .brand-name { font-size: 1.22rem; }
  .atlas-header .orbital-mark { width: 2rem; height: 1.8rem; }
  .atlas-header .brand-sub { display: none; }
  .atlas-header .header-actions { gap: .12rem; }
  .atlas-header .header-actions .button, .atlas-header .header-actions button, .atlas-header .mobile-nav-toggle {
    min-height: 36px;
    padding: .35rem .63rem;
    font-size: .75rem;
  }
  .atlas-header .header-actions .icon-button { width: 2rem; padding: .25rem; }
  .atlas-header nav.open { justify-content: space-between; gap: .25rem; padding: .3rem 0 .45rem; border-top: 1px solid #bec6d326; }
  .atlas-header nav a { padding: .66rem .25rem; }
}
</style>
