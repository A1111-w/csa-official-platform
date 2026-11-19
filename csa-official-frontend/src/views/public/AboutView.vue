<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { sanitizeHtml } from '@/lib/sanitize-html'
import { publicService, type ContributorVo } from '@/services/public'
import MemberCards from './MemberCards.vue'
import PublicSkyHeading from './PublicSkyHeading.vue'
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
  <div class="page stack">
    <PublicSkyHeading motif="circuit" title="关于 CSA" description="了解协会的日常，也认识参与建设的核心成员。" />
    <p v-if="loading" role="status">正在加载协会介绍…</p>
    <div v-if="error" role="alert" class="error toolbar">{{ error }}<button class="secondary" :disabled="loading" @click="load">重试</button></div>
    <section v-if="html" class="panel prose" v-html="html" />
    <p v-else-if="!loading && !error" class="empty">暂无协会介绍。</p>
    <div class="toolbar"><h2>核心成员</h2><RouterLink to="/contributors">查看全部成员 →</RouterLink></div>
    <MemberCards :members="members" :loading="loading" />
  </div>
</template>
<style scoped>.prose{line-height:1.9}.prose :deep(a){text-decoration:underline}.prose :deep(pre){overflow:auto}</style>
