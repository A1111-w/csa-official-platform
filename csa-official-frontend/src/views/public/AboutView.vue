<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { sanitizeHtml } from '@/lib/sanitize-html'
import { publicService, type ContributorVo } from '@/services/public'
import MemberCards from './MemberCards.vue'
import PublicSkyHeading from './PublicSkyHeading.vue'
const projectLabImage = '/brand/csa-project-lab.webp'
const about = ref('')
const members = ref<ContributorVo[]>([])
const loading = ref(true)
const error = ref('')
let alive = true
const html = computed(() => sanitizeHtml(about.value))
async function load() {
  loading.value = true; error.value = ''
  const results = await Promise.allSettled([publicService.getAbout(), publicService.getContributors()])
  if (!alive) return
  about.value = results[0].status === 'fulfilled' ? results[0].value : ''
  members.value = results[1].status === 'fulfilled' ? results[1].value : []
  if (results.some(result => result.status === 'rejected')) error.value = '部分协会信息加载失败，请重试。'
  loading.value = false
}
onMounted(load)
onUnmounted(() => { alive = false })
</script>
<template>
  <div class="atlas-route">
    <PublicSkyHeading motif="circuit" title="关于 CSA" description="了解协会的日常，也认识参与建设的核心成员。" />
    <div class="page atlas-body about-spread">
      <div v-if="error" role="alert" class="error toolbar">{{ error }}<button class="secondary" :disabled="loading" @click="load">重试</button></div>
      <section class="about-story" aria-labelledby="association-introduction">
        <div class="section-rule"><h2 id="association-introduction">协会介绍</h2></div>
        <img class="story-image" :src="projectLabImage" alt="项目协作场景插画，同学围桌交流开发思路" loading="lazy" />
        <p v-if="loading" role="status">正在加载协会介绍…</p>
        <div v-if="html" class="prose" v-html="html" />
        <p v-else-if="!loading && !error" class="empty">暂无协会介绍。</p>
      </section>
      <section class="about-people" aria-labelledby="association-team">
        <div class="section-rule"><h2 id="association-team">核心成员</h2><RouterLink to="/contributors">查看全部成员 →</RouterLink></div>
        <MemberCards :members="members" :loading="loading" />
      </section>
    </div>
  </div>
</template>
<style scoped>
.about-spread { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(18rem, 1fr); gap: clamp(2.3rem, 6vw, 7rem); align-items: start; }
.about-spread > .error { grid-column: 1 / -1; }
.about-story, .about-people { min-width: 0; }
.story-image { display: block; width: 100%; aspect-ratio: 2.4; object-fit: cover; object-position: center 50%; margin-top: 1.15rem; }
.prose { padding-top: clamp(1.25rem, 3vw, 2.5rem); overflow-wrap: anywhere; max-width: 44em; }
.prose :deep(p) { margin: 0 0 1.2em; line-height: 1.95; color: var(--muted); }
.prose :deep(p:first-child) { font-size: clamp(1.15rem, 1.55vw, 1.6rem); line-height: 1.72; color: var(--text); }
.prose :deep(a) { text-decoration: underline; }
.prose :deep(pre) { overflow: auto; }
.about-people :deep(.member-grid) { grid-template-columns: 1fr; gap: 0; }
@media (max-width: 780px) { .about-spread { grid-template-columns: minmax(0, 1fr); } }
</style>
