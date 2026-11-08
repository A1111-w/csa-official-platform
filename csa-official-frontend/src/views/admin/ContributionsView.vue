<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { contributionService, type ContributionAwardItem, type ContributionType, type ContributionSource } from '@/services/contribution'
import { userService, type UserDirectoryItem } from '@/services/user'
import { useAuthStore } from '@/store/useAuthStore'
import { formatDateTime } from '@/lib/format'
import { notify } from '@/lib/notify'
import { errorText } from './helpers'

const auth = useAuthStore()
const canManage = computed(() => (auth.user?.roleLevel ?? 0) >= 4)
const types: { value: ContributionType; label: string; score: number }[] = [
  { value: 'DEV', label: '官网建设', score: 10 }, { value: 'RES', label: '资源贡献', score: 1 },
  { value: 'COMP', label: '发布比赛', score: 1 }, { value: 'OPS', label: '首页维护', score: 1 },
]
const memberKeyword = ref(''), memberLoading = ref(false), memberError = ref('')
const members = ref<UserDirectoryItem[]>([]), selected = ref<UserDirectoryItem | null>(null)
const award = reactive({ type: 'DEV' as ContributionType, score: 10, reason: '' })
const submitting = ref(false), loading = ref(false), error = ref('')
const items = ref<ContributionAwardItem[]>([]), page = ref(1), pages = ref(1), total = ref(0)
const filters = reactive({ keyword: '', type: '' as '' | ContributionType, source: 'MANUAL' as '' | ContributionSource })
const applied = ref({ ...filters })
async function load(requestedPage = page.value) {
  if (!canManage.value) return
  loading.value = true; error.value = ''
  try {
    const params = { size: 20, keyword: applied.value.keyword || undefined, type: applied.value.type || undefined, source: applied.value.source || undefined }
    let response = await contributionService.listAwards({ ...params, page: requestedPage })
    const lastPage = Math.max(response.pages || 1, 1)
    if (requestedPage > lastPage) response = await contributionService.listAwards({ ...params, page: lastPage })
    items.value = response.records; total.value = response.total; pages.value = lastPage; page.value = Math.min(lastPage, requestedPage)
  } catch (e) { error.value = errorText(e); items.value = []; total.value = 0; pages.value = 1 }
  finally { loading.value = false }
}
async function searchMembers() {
  if (!canManage.value || memberLoading.value) return
  if (!memberKeyword.value.trim()) { notify.error('请输入姓名、账号或邮箱'); return }
  memberLoading.value = true; memberError.value = ''
  try {
    members.value = await userService.list({ keyword: memberKeyword.value.trim(), size: 50 })
    if (!members.value.length) memberError.value = '没有找到匹配成员'
  } catch (e) { memberError.value = errorText(e); members.value = [] }
  finally { memberLoading.value = false }
}
function changeType() { award.score = types.find(t => t.value === award.type)?.score || 1 }
async function submitAward() {
  if (!canManage.value || submitting.value) return
  if (!selected.value) { notify.error('请先选择成员'); return }
  if (!Number.isFinite(Number(award.score)) || Number(award.score) <= 0) { notify.error('贡献分值必须大于 0'); return }
  if (!award.reason.trim()) { notify.error('请填写贡献说明'); return }
  if (!window.confirm(`确认给 ${selected.value.realName || selected.value.username} 记录 ${award.score} 分${types.find(t => t.value === award.type)?.label}吗？`)) return
  submitting.value = true
  try {
    await contributionService.award({ userId: selected.value.id, type: award.type, score: Number(award.score), reason: award.reason.trim() })
    award.reason = ''; notify.success('贡献记录已写入'); await load(1)
  } catch (e) { notify.error(errorText(e)) }
  finally { submitting.value = false }
}
function search() { applied.value = { ...filters, keyword: filters.keyword.trim() }; void load(1) }
function reset() { Object.assign(filters, { keyword: '', type: '', source: 'MANUAL' }); search() }
onMounted(() => load())
</script>

<template>
  <section class="page stack">
    <header><h1>贡献记录管理</h1><p class="muted">共 {{ total }} 条记录</p></header>
    <p v-if="!canManage" class="panel">该页面仅向会长及以上开放。</p>
    <template v-else>
      <div class="grid">
        <section class="panel stack"><h2>选择成员</h2><form class="toolbar" @submit.prevent="searchMembers"><label class="field">姓名、账号或邮箱<input v-model="memberKeyword" maxlength="64" /></label><button class="secondary" :disabled="memberLoading">{{ memberLoading ? '搜索中…' : '搜索成员' }}</button></form><p v-if="memberError" role="status">{{ memberError }}</p><div class="member-results stack"><button v-for="member in members" :key="member.id" class="secondary member-option" :aria-pressed="selected?.id === member.id" :disabled="submitting" @click="selected = member"><strong>{{ member.realName || member.username }}</strong> · {{ member.username }}<small>{{ member.email || '未留邮箱' }} · {{ member.departmentName || '未分配部门' }}</small></button></div></section>
        <form class="panel stack" @submit.prevent="submitAward"><div class="toolbar"><h2>记录贡献</h2><button v-if="selected" type="button" class="secondary" :disabled="submitting" @click="selected = null">清除选择</button></div><p v-if="selected">当前成员：<strong>{{ selected.realName || selected.username }}</strong>（ID {{ selected.id }}）</p><p v-else class="muted">先搜索并选择一名成员。</p><label class="field">贡献类型<select v-model="award.type" :disabled="submitting" @change="changeType"><option v-for="item in types" :key="item.value" :value="item.value">{{ item.label }}</option></select></label><label class="field">贡献分值<input v-model.number="award.score" type="number" min="0.01" max="99999999.99" step="0.01" required :disabled="submitting" /></label><label class="field">贡献说明<textarea v-model="award.reason" required maxlength="500" rows="3" :disabled="submitting" /></label><button :disabled="submitting || !selected">{{ submitting ? '提交中…' : '记录贡献' }}</button></form>
      </div>
      <section class="panel stack"><div class="toolbar"><h2>贡献历史</h2><button class="secondary" :disabled="loading" @click="load()">刷新记录</button></div><form class="form-grid" @submit.prevent="search"><label class="field">成员或说明关键词<input v-model="filters.keyword" maxlength="64" /></label><label class="field">类型<select v-model="filters.type"><option value="">全部类型</option><option v-for="item in types" :key="item.value" :value="item.value">{{ item.label }}</option></select></label><label class="field">来源<select v-model="filters.source"><option value="">全部来源</option><option value="MANUAL">手动记录</option><option value="AUTO">自动记录</option><option value="LEGACY">历史记录</option></select></label><div class="actions"><button :disabled="loading">查询</button><button class="secondary" type="button" :disabled="loading" @click="reset">重置</button></div></form>
        <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载记录…</p><div v-else class="table-wrap"><table><thead><tr><th>成员</th><th>类型 / 来源</th><th>分值</th><th>说明</th><th>记录人 / 时间</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td>{{ item.realName || item.username || item.userId }}<small>{{ item.departmentName || '未分配部门' }}</small></td><td>{{ item.typeLabel }}<small>{{ item.sourceLabel }}</small></td><td>{{ item.score }}</td><td>{{ item.reason || '—' }}</td><td>{{ item.awardedByUsername || '系统' }}<small>{{ formatDateTime(item.createTime) }}</small></td></tr><tr v-if="!items.length"><td colspan="5" class="empty">暂无符合条件的贡献记录。</td></tr></tbody></table></div>
        <nav class="toolbar" aria-label="贡献记录分页"><button class="secondary" :disabled="loading || page <= 1" @click="load(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="loading || page >= pages" @click="load(page + 1)">下一页</button></nav>
      </section>
    </template>
  </section>
</template>
<style scoped>.member-results { max-height: 20rem; overflow-y: auto; } .member-option { text-align: left; } .member-option[aria-pressed="true"] { outline: 2px solid var(--accent); } small { display: block; }</style>
