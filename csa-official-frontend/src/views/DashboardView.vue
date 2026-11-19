<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { dashboardMenu } from '@/config/menu'
import { getRoleDescription, getRoleLabel } from '@/lib/access'
const auth = useAuthStore()
const actions = computed(() => dashboardMenu.filter(item => item.href !== '/dashboard' && (auth.user?.roleLevel ?? -1) >= item.minLevel))
const groups = computed(() => [
  { title: '个人与学习', items: actions.value.filter(item => item.minLevel < 3) },
  { title: '项目协作', items: actions.value.filter(item => item.minLevel === 3) },
  { title: '协会管理', items: actions.value.filter(item => item.minLevel > 3) },
].filter(group => group.items.length))
</script>
<template>
  <div class="page stack">
    <header class="overview-header">
      <h1>控制台概览</h1>
      <p>你好，{{ auth.user?.username }}。{{ getRoleDescription(auth.user?.roleLevel) }}</p>
      <dl class="overview-facts">
        <div><dt>当前身份</dt><dd>{{ getRoleLabel(auth.user?.roleLevel) }}</dd></div>
        <div><dt>可用入口</dt><dd>{{ actions.length }}</dd></div>
      </dl>
    </header>
    <section v-for="group in groups" :key="group.title" class="overview-group">
      <h2>{{ group.title }}</h2>
      <div class="grid quick-links">
        <RouterLink v-for="item in group.items" :key="item.href" :to="item.href" class="panel quick-link">
          <h3>{{ item.title }} <span aria-hidden="true">↗</span></h3>
          <p>{{ item.description }}</p>
        </RouterLink>
      </div>
    </section>
  </div>
</template>
