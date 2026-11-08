<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getSafeRedirect } from '@/lib/navigation'
const props = defineProps<{ kind: string }>()
const route = useRoute()
const title = computed(() => props.kind === 'forbidden' ? '你暂时没有访问权限' : props.kind === 'session' ? '暂时无法验证会话' : '页面未找到')
const text = computed(() => props.kind === 'forbidden' ? '此工作区需要更高的角色等级。请联系协会管理员核对身份。' : props.kind === 'session' ? '请检查后端服务或网络连接，然后重试。' : '链接可能已更改，请从首页继续浏览。')
</script>
<template><section class="page panel status-page"><span class="eyebrow">{{ kind === 'forbidden' ? '403' : kind === 'session' ? '连接异常' : '404' }}</span><h1>{{ title }}</h1><p>{{ text }}</p><div class="actions"><RouterLink v-if="kind === 'session'" class="button" :to="getSafeRedirect(String(route.query.redirect || ''))">重新验证</RouterLink><RouterLink class="button secondary" to="/">返回首页</RouterLink></div></section></template>
