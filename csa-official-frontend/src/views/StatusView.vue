<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { dashboardMenu } from '@/config/menu'
import { getRoleLabel } from '@/lib/access'
import { getSafeRedirect } from '@/lib/navigation'
import { useAuthStore } from '@/store/useAuthStore'
const props = defineProps<{ kind: string }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const title = computed(() => props.kind === 'forbidden' ? '你暂时没有访问权限' : props.kind === 'session' ? '暂时无法验证会话' : '页面未找到')
const text = computed(() => props.kind === 'forbidden' ? '此工作区需要更高的角色等级。请联系协会管理员核对身份。' : props.kind === 'session' ? '请检查后端服务或网络连接，然后重试。' : '链接可能已更改，请从首页继续浏览。')
const menu = computed(() => dashboardMenu.filter(item => auth.user && item.minLevel <= auth.user.roleLevel))
const keyword = ref('')
const publicPages = [
  { href: '/about', title: '关于协会', description: '协会介绍与参与方向' },
  { href: '/competitions', title: '比赛与活动', description: '竞赛日程、活动详情和协作项目' },
  { href: '/resources', title: '学习资源', description: '资料库与文件下载入口' },
  { href: '/contributors', title: '成员与贡献者', description: '成员名录、部门与贡献排行' },
  { href: '/privacy', title: '隐私政策', description: '资料收集、保存期限与联系方式' },
]
const results = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return publicPages.filter(page => [page.title, page.description, page.href].some(value => value.toLowerCase().includes(query)))
})
const retryTarget = computed(() => {
  const target = getSafeRedirect(typeof route.query.redirect === 'string' ? route.query.redirect : undefined)
  return router.resolve(target).meta.requiresAuth ? target : '/dashboard'
})
const retrying = ref(false)
const retryMessage = ref('')
const online = ref(navigator.onLine)
function updateNetwork() { online.value = navigator.onLine }
onMounted(() => {
  if (props.kind !== 'session') return
  window.addEventListener('online', updateNetwork)
  window.addEventListener('offline', updateNetwork)
})
onUnmounted(() => {
  window.removeEventListener('online', updateNetwork)
  window.removeEventListener('offline', updateNetwork)
})
async function retry(event: MouseEvent, navigate: (event: MouseEvent) => Promise<unknown>) {
  if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey || event.button !== 0) return
  if (retrying.value) { event.preventDefault(); return }
  retrying.value = true
  retryMessage.value = '正在通过原页面重新验证会话…'
  try {
    await navigate(event)
    if (route.path === '/session-error') retryMessage.value = '这次仍未能验证会话，请稍后再试。'
  } catch { retryMessage.value = '这次请求未完成，请再试一次。' }
  finally { retrying.value = false }
}
</script>
<template>
  <section class="status-scene" :data-state="kind" aria-labelledby="status-title">
    <div class="status-content">
      <p class="status-code">{{ kind === 'forbidden' ? '403' : kind === 'session' ? '会话连接' : '404' }}</p>
      <h1 id="status-title">{{ title }}</h1>
      <p class="status-description">{{ text }}</p>
      <div class="actions">
        <RouterLink v-if="kind === 'session'" v-slot="{ href, navigate }" :to="retryTarget" custom>
          <a class="button" :href="href" :aria-busy="retrying" @click="retry($event, navigate)">重新验证</a>
        </RouterLink>
        <RouterLink class="button" :class="{ secondary: kind === 'session' }" to="/">返回首页</RouterLink>
      </div>
    </div>
  </section>
  <section v-if="kind === 'missing'" class="status-tools" aria-labelledby="site-search-title">
    <div class="tools-title"><h2 id="site-search-title">找回想去的栏目</h2><p>按页面名称、用途或路径查找公开栏目。</p></div>
    <div>
      <label class="field">搜索站点栏目<input v-model="keyword" type="search" placeholder="例如：成员、竞赛、/resources" /></label>
      <p class="result-count" aria-live="polite">找到 {{ results.length }} 个栏目</p>
      <ul v-if="results.length" class="route-list"><li v-for="item in results" :key="item.href"><RouterLink :to="item.href"><strong>{{ item.title }} <span aria-hidden="true">↗</span></strong><small>{{ item.description }}</small></RouterLink></li></ul>
      <button v-else type="button" class="secondary" @click="keyword = ''">清空搜索，查看全部</button>
    </div>
  </section>
  <section v-else-if="kind === 'forbidden'" class="status-tools" aria-labelledby="permission-title">
    <div class="tools-title"><h2 id="permission-title">我可以访问哪些页面？</h2><p>工作台按当前账号的协会身份开放入口。</p></div>
    <div v-if="auth.user">
      <p class="role-line">当前账号 <strong>{{ auth.user.username }}</strong> · {{ getRoleLabel(auth.user.roleLevel) }}</p>
      <details><summary>查看 {{ menu.length }} 个可用工作台入口</summary><ul class="route-list"><li v-for="item in menu" :key="item.href"><RouterLink :to="item.href"><strong>{{ item.title }} <span aria-hidden="true">↗</span></strong><small>{{ item.description }}</small></RouterLink></li></ul></details>
    </div>
    <div v-else><p class="role-line">登录后可核对账号身份和可用栏目。</p><RouterLink class="button secondary" to="/login">前往登录</RouterLink></div>
  </section>
  <section v-else class="status-tools" aria-labelledby="connection-title">
    <div class="tools-title"><h2 id="connection-title">连接检查</h2><p>恢复连接后，重新验证会回到刚才访问的工作区。</p></div>
    <div>
      <dl class="connection-facts"><div><dt>浏览器网络</dt><dd>{{ online ? '在线' : '离线，请检查设备网络' }}</dd></div><div><dt>重试目标</dt><dd>{{ router.resolve(retryTarget).meta.title || '工作台' }}</dd></div></dl>
      <p v-if="retryMessage" role="status" class="retry-message">{{ retryMessage }}</p>
      <details><summary>等待期间可以去哪里？</summary><div class="public-alternatives"><RouterLink to="/about">了解协会 ↗</RouterLink><RouterLink to="/competitions">浏览比赛与活动 ↗</RouterLink></div></details>
    </div>
  </section>
</template>

<style scoped>
.status-scene {
  --star: #c2d0ff;
  --scene: url('/brand/csa-mirror-constellation.webp');
  --focus: 72%;
  --shade: linear-gradient(90deg, #070c1eed, #070c1eae 36%, #070c1e24 65%, transparent 85%);
  display: grid; align-items: center; grid-template-columns: minmax(0, 1fr) minmax(0, .8fr);
  position: relative; overflow: hidden; isolation: isolate;
  width: 100%; min-height: clamp(22rem, 44svh, 34rem);
  padding: 3rem max(clamp(1rem, 5vw, 5rem), calc((100% - 1360px) / 2));
  background: var(--shade), var(--scene) var(--focus) center / cover no-repeat, #080d1d;
  color: #f9f6f1;
}
.status-scene[data-state='forbidden'] { --star: #a1edca; --scene: url('/brand/csa-magnetic-veil.webp'); --focus: 75%; }
.status-scene[data-state='session'] { --star: #8ce4de; --scene: url('/brand/csa-signal-array.webp'); --focus: 76%; }
.status-code { color: var(--star); font: italic clamp(1.4rem, 3vw, 2.6rem)/1.15 Georgia, serif; }
.status-content { z-index: 1; min-width: 0; }
.status-content h1 { font-size: clamp(2.2rem, 4.5vw, 4.6rem); margin-block: .55rem 1.15rem; }
.status-description { max-width: 34em; color: #dce2ee; }
.status-content .actions { margin-top: 1.6rem; gap: .7rem; }
.status-content .button { min-height: 44px; color: #111f29; background: #c8eee6; border-radius: 99px; padding-inline: 1.4rem; }
.status-content .secondary { color: #f2f4f5; background: #111b30c9; border-color: #8c97aa; }
.status-tools {
  width: min(100% - 2 * var(--atlas-gutter, 1rem), 1360px);
  margin-inline: auto; padding-block: clamp(1.8rem, 4vw, 4rem);
  display: grid; grid-template-columns: minmax(0, .8fr) minmax(0, 1.2fr); gap: clamp(1rem, 4vw, 4rem);
}
.tools-title h2 { font-size: clamp(1.4rem, 2vw, 2rem); }
.tools-title p, .route-list small, .result-count, .connection-facts dt { color: var(--muted); }
.tools-title p { margin-top: .5rem; max-width: 29em; }
.status-tools summary { cursor: pointer; padding: .75rem 0; color: var(--accent); }
.status-tools details { border-block: 1px solid var(--border); }
.status-tools .role-line { overflow-wrap: anywhere; margin-bottom: .85rem; }
.status-tools .field { max-width: 33rem; }
.result-count { font-size: .86rem; margin: .8rem 0 .4rem; }
.route-list { margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 0 1.4rem; list-style: none; }
.route-list a { display: grid; gap: .15rem; padding: .75rem 0; border-bottom: 1px solid var(--border); }
.route-list a:hover strong, .route-list a:focus-visible strong { text-decoration: underline; }
.route-list strong { font-size: .96rem; color: var(--text); }
.route-list strong span { color: var(--accent); }
.route-list small { font-size: .8rem; }
.connection-facts { display: flex; gap: 1rem 2rem; flex-wrap: wrap; margin: 0 0 1rem; }
.connection-facts dt { font-size: .84rem; }
.connection-facts dd { margin: .25rem 0 0; font-weight: 650; }
.public-alternatives { display: flex; gap: .4rem 1.2rem; flex-wrap: wrap; padding-bottom: 1rem; }
.retry-message { margin: .7rem 0; }
@media (max-width: 700px) {
  .status-scene {
    --shade: linear-gradient(0deg, #070c1ef7 4%, #070c1ee6 24%, #070c1e45 64%, transparent);
    grid-template-columns: 1fr; min-height: clamp(22rem, 50svh, 30rem); align-content: end; padding-bottom: 2rem;
  }
  .status-content h1 { font-size: clamp(1.9rem, 8vw, 3.1rem); }
  .status-tools { grid-template-columns: 1fr; }
  .route-list { grid-template-columns: minmax(0, 1fr); }
}
</style>
