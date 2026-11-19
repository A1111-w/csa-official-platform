<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getSafeRedirect } from '@/lib/navigation'
const props = defineProps<{ kind: string }>()
const route = useRoute()
const title = computed(() => props.kind === 'forbidden' ? '你暂时没有访问权限' : props.kind === 'session' ? '暂时无法验证会话' : '页面未找到')
const text = computed(() => props.kind === 'forbidden' ? '此工作区需要更高的角色等级。请联系协会管理员核对身份。' : props.kind === 'session' ? '请检查后端服务或网络连接，然后重试。' : '链接可能已更改，请从首页继续浏览。')
</script>
<template>
  <section class="status-scene" :data-state="kind" aria-labelledby="status-title">
    <div class="status-content">
      <p class="status-code">{{ kind === 'forbidden' ? '403' : kind === 'session' ? '会话连接' : '404' }}</p>
      <h1 id="status-title">{{ title }}</h1>
      <p class="status-description">{{ text }}</p>
      <div class="actions">
        <RouterLink v-if="kind === 'session'" class="button" :to="getSafeRedirect(String(route.query.redirect || ''))">重新验证</RouterLink>
        <RouterLink class="button" :class="{ secondary: kind === 'session' }" to="/">返回首页</RouterLink>
      </div>
    </div>
    <div class="status-orbit" aria-hidden="true"><i></i><i></i><i></i></div>
  </section>
</template>

<style scoped>
.status-scene {
  --star: #c2d0ff;
  display: grid; align-items: center; grid-template-columns: minmax(0, 1fr) minmax(0, .8fr);
  position: relative; overflow: hidden; isolation: isolate;
  width: 100%; min-height: clamp(32rem, 71svh, 49rem);
  padding: 3rem max(clamp(1rem, 5vw, 5rem), calc((100% - 1360px) / 2));
  background: linear-gradient(90deg, #070c1ef2, #070c1e8a 47%, #070c1e25 77%), url('/brand/csa-observatory-sky.webp') center / cover, #080d1d;
  color: #f9f6f1;
}
.status-scene[data-state='forbidden'] { --star: #f4bf88; background-position: center, 70% center; }
.status-scene[data-state='session'] { --star: #8ce4de; background-position: center, 35% center; }
.status-code { color: var(--star); font: italic clamp(1.4rem, 3vw, 2.6rem)/1.15 Georgia, serif; }
.status-content { z-index: 1; min-width: 0; }
.status-content h1 { font-size: clamp(2.2rem, 4.5vw, 4.6rem); margin-block: .55rem 1.15rem; }
.status-description { max-width: 34em; color: #dce2ee; }
.status-content .actions { margin-top: 1.6rem; gap: .7rem; }
.status-content .button { min-height: 44px; color: #111f29; background: #c8eee6; border-radius: 99px; padding-inline: 1.4rem; }
.status-content .secondary { color: #f2f4f5; background: #111b30c9; border-color: #8c97aa; }
.status-orbit { position: relative; aspect-ratio: 1.65; align-self: center; pointer-events: none; color: var(--star); transform: rotate(-19deg); }
.status-orbit i { position: absolute; inset: 13% 0; border: 1px solid currentColor; border-radius: 50%; opacity: .8; }
.status-orbit i::after { content: ''; position: absolute; width: .45rem; height: .45rem; top: calc(50% - .225rem); left: -.225rem; background: currentColor; border-radius: 50%; box-shadow: 0 0 15px currentColor; }
.status-orbit i:nth-child(2) { inset: 2% 12%; transform: rotate(30deg); opacity: .57; }
.status-orbit i:nth-child(3) { inset: 25% 25%; opacity: .35; }
[data-state='missing'] .status-orbit i { border-right-color: transparent; }
[data-state='forbidden'] .status-orbit i:nth-child(3) { background: #0e1626; opacity: .91; box-shadow: inset 3px 2px 9px #ffc58599; }
[data-state='session'] .status-orbit i { border-style: dashed; }
@media (max-width: 700px) {
  .status-scene { grid-template-columns: 1fr; min-height: clamp(28rem, 72svh, 39rem); align-content: end; padding-bottom: 3rem; }
  .status-orbit { position: absolute; width: 60%; top: 5%; right: -4%; }
  .status-content h1 { font-size: clamp(1.9rem, 8vw, 3.1rem); }
}
</style>
