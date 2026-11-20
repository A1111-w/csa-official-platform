<script setup lang="ts">
import { resolveAssetUrl } from '@/lib/format'
import { getRoleLabel } from '@/lib/access'
import type { ContributorVo } from '@/services/public'
defineProps<{ members: ContributorVo[]; loading?: boolean }>()
function initial(name: string) {
  const lastNamePart = name?.trim().split(/\s+·\s+/).at(-1)
  return Array.from(lastNamePart || 'C')[0]
}
</script>

<template>
  <p v-if="loading" role="status" class="muted">正在加载成员信息…</p>
  <div v-else-if="members.length" class="member-grid">
    <article v-for="member in members" :key="member.id" class="member-card">
      <img v-if="member.avatar" :src="resolveAssetUrl(member.avatar)" :alt="member.realName || '成员头像'" loading="lazy" class="avatar" />
      <span v-else class="avatar fallback" aria-hidden="true">{{ initial(member.realName) }}</span>
      <div class="member-copy">
        <h3>{{ member.realName || '未公开姓名' }}</h3>
        <p>{{ member.title || getRoleLabel(member.roleLevel) }}</p>
        <p class="muted">{{ member.deptName || '部门未公开' }}<template v-if="member.title"> · {{ getRoleLabel(member.roleLevel) }}</template></p>
        <slot name="contribution" :member="member" />
      </div>
    </article>
  </div>
  <p v-else class="empty">暂时还没有公开的核心成员信息。</p>
</template>

<style scoped>
.member-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 17rem), 1fr)); column-gap: clamp(1rem, 3vw, 3rem); }
.member-card { --member-ink: var(--accent); display: flex; align-items: center; gap: 1rem; padding: 1.15rem 0; border-bottom: 1px solid var(--border); min-width: 0; }
.member-card:nth-child(3n+2) { --member-ink: color-mix(in srgb, var(--text) 55%, #a982dc); }
.member-card:nth-child(3n+3) { --member-ink: color-mix(in srgb, var(--text) 55%, #c28c55); }
.member-copy { min-width: 0; overflow-wrap: anywhere; }
.member-card h3 { font-size: 1.07rem; margin: 0 0 .15rem; }
.member-card p { margin: 0; font-size: .87rem; line-height: 1.65; }
.avatar { width: 3rem; height: 3rem; object-fit: cover; border-radius: 50%; flex-shrink: 0; }
.fallback { display: grid; place-items: center; border: 1px solid color-mix(in srgb, var(--member-ink) 45%, transparent); background: radial-gradient(circle at 24% 20%, color-mix(in srgb, var(--member-ink) 19%, transparent), transparent 74%), var(--surface); color: var(--member-ink); font-size: 1.2rem; }
</style>
