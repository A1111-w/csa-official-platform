<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { publicService, type ContributorVo } from '@/services/public'
import MemberCards from './MemberCards.vue'
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
<template><div class="page stack">
  <header><p class="muted">OUR COMMUNITY</p><h1>核心成员与公开贡献者</h1><p class="muted">认识一起参与建设的伙伴，发现相同的兴趣与方向。</p></header>
  <div v-if="error" class="error toolbar" role="alert">{{ error }}<button class="secondary" :disabled="loading" @click="load">重新加载</button></div>
  <div v-if="!loading && !error" class="toolbar panel"><span>公开成员 <strong>{{ members.length }}</strong> 人</span><span>涉及部门 <strong>{{ departments }}</strong> 个</span></div>
  <MemberCards :members="members" :loading="loading" />
</div></template>
