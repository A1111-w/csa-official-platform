<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { publicService, type ContributorVo } from '@/services/public'
import MemberCards from './MemberCards.vue'
import PublicSkyHeading from './PublicSkyHeading.vue'
const members = ref<ContributorVo[]>([])
const loading = ref(true)
const error = ref('')
let alive = true
const departments = computed(() => new Set(members.value.map(member => member.deptName).filter(Boolean)).size)
async function load() {
  loading.value = true; error.value = ''
  try { const data = await publicService.getContributors(); if (alive) members.value = data }
  catch (reason) { if (alive) { members.value = []; error.value = reason instanceof Error ? reason.message : '成员名单加载失败' } }
  finally { if (alive) loading.value = false }
}
onMounted(load)
onUnmounted(() => { alive = false })
</script>
<template><div class="atlas-route">
  <PublicSkyHeading motif="network" title="成员与贡献者" description="认识参与协会建设的核心成员和公开贡献者。" />
  <section class="page atlas-body" aria-labelledby="directory-title">
    <div class="section-rule">
      <h2 id="directory-title">成员名录</h2>
      <p v-if="!loading && !error" class="directory-stats">公开成员 <strong>{{ members.length }}</strong> 人 <span>／</span> 涉及部门 <strong>{{ departments }}</strong> 个</p>
    </div>
    <div v-if="error" class="error toolbar" role="alert">{{ error }}<button class="secondary" :disabled="loading" @click="load">重新加载</button></div>
    <MemberCards :members="members" :loading="loading" />
  </section>
</div></template>
<style scoped>
.directory-stats { margin: 0; color: var(--muted); font-size: .88rem; }
.directory-stats strong { color: var(--text); font-variant-numeric: tabular-nums; }
.directory-stats span { padding-inline: .35rem; opacity: .6; }
</style>
