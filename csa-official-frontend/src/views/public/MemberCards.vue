<script setup lang="ts">
import { resolveAssetUrl } from '@/lib/format'
import type { ContributorVo } from '@/services/public'
defineProps<{ members: ContributorVo[]; loading?: boolean }>()
</script>

<template>
  <p v-if="loading" role="status" class="muted">正在加载成员信息…</p>
  <div v-else-if="members.length" class="member-grid">
    <article v-for="member in members" :key="member.id" class="panel member-card">
      <img v-if="member.avatar" :src="resolveAssetUrl(member.avatar)" :alt="member.realName || '成员头像'" loading="lazy" class="avatar" />
      <span v-else class="avatar fallback" aria-hidden="true">{{ (member.realName || 'C').slice(0, 1) }}</span>
      <div><h3>{{ member.realName || '未公开姓名' }}</h3><p>{{ member.title || '核心成员' }}</p><p class="muted">{{ member.deptName || '协会协作组' }} · Level {{ member.roleLevel }}</p></div>
    </article>
  </div>
  <p v-else class="empty">暂时还没有公开的核心成员信息。</p>
</template>

<style scoped>
.member-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:1rem}.member-card{display:flex;align-items:center;gap:1rem}.member-card h3{margin:0}.member-card p{margin:.4rem 0;font-size:.9rem}.avatar{width:56px;height:56px;object-fit:cover;border-radius:50%;flex-shrink:0}.fallback{display:grid;place-items:center;background:var(--soft);color:var(--accent);font-weight:700}
</style>
