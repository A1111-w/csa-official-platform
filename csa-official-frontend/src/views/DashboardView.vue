<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { dashboardMenu } from '@/config/menu'
import { getRoleDescription, getRoleLabel } from '@/lib/access'
const auth = useAuthStore()
const actions = computed(() => dashboardMenu.filter(item => item.href !== '/dashboard' && (auth.user?.roleLevel ?? -1) >= item.minLevel))
</script>
<template>
  <div class="page stack">
    <header class="page-heading"><span class="eyebrow">YOUR WORKSPACE</span><h1>控制台概览</h1><p>你好，{{ auth.user?.username }}。{{ getRoleDescription(auth.user?.roleLevel) }}</p></header>
    <div class="grid stats"><section class="panel"><span class="muted">当前身份</span><h2>{{ getRoleLabel(auth.user?.roleLevel) }}</h2></section><section class="panel"><span class="muted">可用入口</span><h2>{{ actions.length }}</h2></section><section class="panel"><span class="muted">登录状态</span><h2>会话已验证</h2></section></div>
    <h2>开始今天的协作</h2>
    <div class="grid quick-links"><RouterLink v-for="(item, index) in actions" :key="item.href" :to="item.href" class="panel quick-link"><span class="eyebrow">{{ String(index + 1).padStart(2, '0') }} <span aria-hidden="true">↗</span></span><h3>{{ item.title }}</h3><p>{{ item.description }}</p></RouterLink></div>
  </div>
</template>
