<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { auditService, type AuditLogItem } from '@/services/audit'
import { useAuthStore } from '@/store/useAuthStore'
import { formatDateTime } from '@/lib/format'
import { errorText } from './helpers'

const auth = useAuthStore()
const canView = computed(() => (auth.user?.roleLevel ?? 0) >= 4)
const filters = reactive({ action: '', result: '', requestId: '' })
const applied = ref({ ...filters })
const items = ref<AuditLogItem[]>([])
const page = ref(1), pages = ref(1), total = ref(0)
const loading = ref(false), error = ref('')
let requestId = 0
async function load(requestedPage = page.value) {
  if (!canView.value) return
  const current = ++requestId
  loading.value = true; error.value = ''
  try {
    const params = { size: 20, action: applied.value.action || undefined, result: applied.value.result || undefined, requestId: applied.value.requestId || undefined }
    let response = await auditService.list({ ...params, page: requestedPage })
    const lastPage = Math.max(response.pages || 1, 1)
    const finalPage = Math.min(requestedPage, lastPage)
    if (requestedPage > lastPage) response = await auditService.list({ ...params, page: finalPage })
    if (current !== requestId) return
    items.value = response.records; total.value = response.total; pages.value = lastPage; page.value = finalPage
  } catch (e) { if (current === requestId) { error.value = errorText(e); items.value = []; total.value = 0; pages.value = 1 } }
  finally { if (current === requestId) loading.value = false }
}
function search() { applied.value = { action: filters.action.trim(), result: filters.result, requestId: filters.requestId.trim() }; void load(1) }
function reset() { Object.assign(filters, { action: '', result: '', requestId: '' }); search() }
function details(value: string | null) { try { return JSON.stringify(JSON.parse(value || '{}'), null, 2) } catch { return value } }
onMounted(() => load())
</script>

<template>
  <section class="page stack">
    <header class="toolbar"><div><h1>审计日志</h1><p class="muted">共 {{ total }} 条记录</p></div><button v-if="canView" class="secondary" :disabled="loading" @click="load()">刷新</button></header>
    <p v-if="!canView" class="panel">该页面仅向会长及以上开放。</p>
    <template v-else>
      <form class="panel form-grid" @submit.prevent="search">
        <label class="field">操作类型<input v-model="filters.action" placeholder="例如 ROLE_CHANGE" /></label>
        <label class="field">审计结果<select v-model="filters.result"><option value="">全部结果</option><option>SUCCESS</option><option>FAILURE</option><option>ACCEPTED</option></select></label>
        <label class="field">请求 ID<input v-model="filters.requestId" /></label>
        <div class="actions"><button :disabled="loading">查询</button><button type="button" class="secondary" :disabled="loading" @click="reset">重置</button></div>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载日志…</p>
      <div v-else class="panel table-wrap"><table><thead><tr><th>时间</th><th>操作人</th><th>操作</th><th>对象</th><th>结果</th><th>详情</th></tr></thead><tbody>
        <tr v-for="item in items" :key="item.id"><td>{{ formatDateTime(item.createTime) }}</td><td>{{ item.actorUsername || '—' }}<small>ID {{ item.actorUserId ?? '—' }}</small></td><td>{{ item.action }}</td><td>{{ item.targetType || '—' }} / {{ item.targetId || '—' }}</td><td><span class="badge">{{ item.result }}</span></td><td><details><summary>查看详情 #{{ item.id }}</summary><dl><dt>请求 ID</dt><dd>{{ item.requestId || '—' }}</dd><dt>IP 地址</dt><dd>{{ item.ipAddress || '—' }}</dd><dt>User-Agent</dt><dd>{{ item.userAgent || '—' }}</dd></dl><pre>{{ details(item.detailsJson) }}</pre></details></td></tr>
        <tr v-if="!items.length"><td colspan="6" class="empty">没有符合条件的日志。</td></tr>
      </tbody></table></div>
      <nav class="toolbar" aria-label="审计日志分页"><button class="secondary" :disabled="loading || page <= 1" @click="load(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="loading || page >= pages" @click="load(page + 1)">下一页</button></nav>
    </template>
  </section>
</template>
<style scoped>small { display: block; } pre, dd { white-space: pre-wrap; overflow-wrap: anywhere; max-width: 32rem; } summary { cursor: pointer; }</style>
