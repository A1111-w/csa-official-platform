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
const dark = ref(false)
const workspace = computed(() => route.path.startsWith('/dashboard'))
const isHome = computed(() => route.path === '/')
const menu = computed(() => dashboardMenu.filter(item => (auth.user?.roleLevel ?? -1) >= item.minLevel))
watch(() => route.path, () => { menuOpen.value = false })
function dismissMenu() {
  if (menuOpen.value) {
    menuOpen.value = false
    menuToggle.value?.focus()
  }
}
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
  <header class="site-header" :class="{ 'home-header': isHome }" @keydown.esc="dismissMenu">
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
  <div :class="workspace ? 'workspace-shell' : 'public-shell'">
    <aside v-if="workspace" class="sidebar" aria-label="工作台导航">
      <div class="sidebar-heading"><span class="eyebrow">WORKSPACE</span><strong>{{ auth.user?.username }}</strong><span class="muted">{{ getRoleLabel(auth.user?.roleLevel) }}</span></div>
      <nav><RouterLink v-for="(item, index) in menu" :key="item.href" :to="item.href" :class="{ selected: route.path === item.href }"><span class="menu-number">{{ String(index + 1).padStart(2, '0') }}</span>{{ item.title }}</RouterLink></nav>
      <div class="sidebar-note">一起学习 · 创造 · 分享</div>
    </aside>
    <main id="main" tabindex="-1"><RouterView :key="route.path" /></main>
  </div>
  <footer v-if="!workspace" class="site-footer" :class="{ 'home-footer': isHome }"><span><strong>CSA</strong> · 计算机协会</span><div><RouterLink to="/about">关于协会</RouterLink><RouterLink to="/privacy">隐私政策</RouterLink></div></footer>
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
.home-header, .home-footer {
  --nav-edge: max(clamp(1rem, 5vw, 5rem), calc((100% - 1480px) / 2));
  padding-inline: var(--nav-edge);
  background: #080e1e;
  color: #ece8df;
  border-color: #8496b02c;
}
.home-header { height: 68px; }
.home-header .brand { gap: .6rem; color: #f2eee6; }
.home-header .orbital-mark { color: #d9c8a9; }
.home-header .brand-name {
  display: flex;
  align-items: center;
  gap: .85rem;
  font-size: 1.45rem;
}
.home-header .brand-sub {
  border-left: 1px solid #d6dadd36;
  margin: 0;
  padding: .06rem 0 .06rem .85rem;
  color: #d5d7dc;
  font-size: .75rem;
  font-weight: 500;
  letter-spacing: .12em;
  white-space: nowrap;
}
.home-header nav { gap: clamp(1rem, 2.45vw, 2.55rem); }
.home-header nav a {
  position: relative;
  padding: .85rem 0;
  color: #b8bfcd;
  font-size: .87rem;
  font-weight: 500;
}
.home-header nav a:is(:hover, :focus-visible, .router-link-exact-active) { color: #f7efdf; text-decoration: none; }
.home-header nav a.router-link-exact-active::after {
  position: absolute;
  bottom: .25rem;
  left: calc(50% - .6rem);
  width: 1.2rem;
  height: 2px;
  border-radius: 2px;
  background: #d9c8a9;
  content: '';
}
.home-header .header-actions { gap: .7rem; }
.home-header .header-actions .button, .home-header .header-actions button, .home-header .mobile-nav-toggle {
  min-height: 38px;
  padding: .45rem 1rem;
  border: 1px solid #a4adbb54;
  border-radius: 99px;
  background: transparent;
  color: #e6e5e2;
  font-size: .8rem;
  font-weight: 550;
}
.home-header .header-actions .icon-button {
  width: 38px;
  border-color: transparent;
  padding: .4rem;
  color: #c3c9d4;
}
.home-header .header-actions :is(button, a):hover, .home-header .mobile-nav-toggle:hover {
  border-color: #e1d6c29c;
  background: #d7d4e013;
  filter: none;
}
.home-footer { padding-block: 2rem; color: #bec3ce; }
.home-footer a { color: #e9dfcc; }
:root[data-theme=dark] :is(.home-header, .home-footer) { background: #050a15; }
:root[data-theme=dark] .home-header .orbital-mark { color: #b8d8eb; }
@media (max-width: 760px) {
  .home-header { height: auto; min-height: 60px; padding-block: .55rem; gap: .35rem; }
  .home-header .brand { gap: .28rem; }
  .home-header .brand-name { font-size: 1.22rem; }
  .home-header .orbital-mark { width: 2rem; height: 1.8rem; }
  .home-header .brand-sub { display: none; }
  .home-header .header-actions { gap: .12rem; }
  .home-header .header-actions .button, .home-header .header-actions button, .home-header .mobile-nav-toggle {
    min-height: 36px;
    padding: .35rem .63rem;
    font-size: .75rem;
  }
  .home-header .header-actions .icon-button { width: 2rem; padding: .25rem; }
  .home-header nav.open { justify-content: space-between; gap: .25rem; padding: .3rem 0 .45rem; border-top: 1px solid #bec6d326; }
  .home-header nav a { padding: .66rem .25rem; }
}
</style>
