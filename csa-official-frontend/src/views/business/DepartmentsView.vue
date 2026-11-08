<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { deptService, type DeptItem } from '@/services/dept'
import { userService, type UserDirectoryItem } from '@/services/user'
import { getRoleLabel } from '@/lib/access'
import { notify } from '@/lib/notify'

const auth = useAuthStore(), canManage = computed(() => (auth.user?.roleLevel ?? -1) >= 4)
const departments = ref<DeptItem[]>([]), members = ref<UserDirectoryItem[]>([]), selectedId = ref<number | null>(null)
const loading = ref(false), appointing = ref<number | null>(null), error = ref(''), keyword = ref('')
const selected = computed(() => departments.value.find(d => d.id === selectedId.value))
const leaderMap = computed(() => new Map(members.value.map(m => [m.id, m])))
const filtered = computed(() => [...members.value].sort((a, b) => (b.roleLevel ?? -1) - (a.roleLevel ?? -1) || a.username.localeCompare(b.username)).filter(m => [m.realName, m.username, m.email, m.departmentName].some(v => v?.toLowerCase().includes(keyword.value.trim().toLowerCase()))))
function restriction(member: UserDirectoryItem) {
  if (member.roleLevel == null || !Number.isFinite(member.roleLevel)) return '角色信息不完整，请刷新后再任命'
  if ((member.roleLevel ?? 0) >= 4) return '会长或 Root 不参与部长任命'
  if (member.roleLevel === 3 && member.departmentId != null && member.departmentId !== selectedId.value) return '已担任其他部门部长，需先卸任'
  if (selected.value?.leaderId === member.id) return '当前负责人'
  return ''
}
async function load() {
  if (!canManage.value) return
  loading.value = true; error.value = ''
  try {
    const [depts, users] = await Promise.all([deptService.list(), userService.list({ size: 150 })])
    departments.value = depts; members.value = users
    if (!depts.some(d => d.id === selectedId.value)) selectedId.value = depts[0]?.id ?? null
  } catch (e) { error.value = e instanceof Error ? e.message : '部门数据加载失败' }
  finally { loading.value = false }
}
watch(canManage, allowed => { if (allowed) void load() }, { immediate: true })
async function appoint(member: UserDirectoryItem) {
  if (!canManage.value || !selected.value || appointing.value !== null || restriction(member)) return
  const dept = selected.value
  if (!window.confirm(`确认任命 ${member.realName || member.username} 为 ${dept.name} 部长吗？原部长将卸任。`)) return
  appointing.value = member.id
  try { await deptService.appoint(dept.id, member.id); notify.success('人事任命已生效'); await load() }
  catch (e) { notify.error(e instanceof Error ? e.message : '任命失败') }
  finally { appointing.value = null }
}
</script>

<template>
  <section class="page stack"><header><p class="eyebrow">DEPARTMENTS</p><h1>部门人事</h1><p class="muted">查看部门负责人，并为合适的成员分配管理职责。</p></header>
    <div v-if="!canManage" class="panel">部门人事页仅向会长及以上角色开放。</div>
    <template v-else><div class="toolbar"><span>{{ departments.length }} 个部门 · 当前载入 {{ members.length }} 名成员</span><button class="secondary" :disabled="loading || appointing !== null" @click="load">刷新</button></div><p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载部门与成员…</p><div v-else-if="!departments.length && !error" class="panel empty">暂无部门。</div>
      <div class="grid department-grid"><article v-for="dept in departments" :key="dept.id" class="panel stack" :class="{ selected: selectedId === dept.id }"><h2>{{ dept.name }}</h2><p>{{ dept.intro || '暂无部门简介' }}</p><p class="muted">当前负责人：{{ dept.leaderId ? (leaderMap.get(dept.leaderId)?.realName || leaderMap.get(dept.leaderId)?.username || `成员 #${dept.leaderId}`) : '暂未任命' }}</p><button class="secondary" :aria-pressed="selectedId === dept.id" :disabled="appointing !== null" @click="selectedId = dept.id">{{ selectedId === dept.id ? '已选择此部门' : '选择部门' }}</button></article></div>
      <section v-if="selected" class="panel stack"><h2>{{ selected.name }} · 部长候选成员</h2><p class="muted">任命时会处理原部长卸任与部门归属变更；已担任其他部门部长的成员需先卸任。搜索范围为本次加载的最多 150 名成员。</p><label class="field">搜索成员<input v-model="keyword" placeholder="姓名、账号、邮箱或部门" /></label><div class="table-wrap"><table><thead><tr><th>成员</th><th>当前角色</th><th>部门</th><th>任命</th></tr></thead><tbody><tr v-for="member in filtered" :key="member.id"><td>{{ member.realName || member.username }}<br /><small class="muted">@{{ member.username }}</small></td><td>{{ getRoleLabel(member.roleLevel) }}</td><td>{{ member.departmentName || '未分配' }}</td><td><span v-if="restriction(member)" class="muted">{{ restriction(member) }}</span><button v-else :disabled="appointing !== null || loading" @click="appoint(member)">{{ appointing === member.id ? '任命中…' : '任命为部长' }}</button></td></tr></tbody></table></div><p v-if="!filtered.length" class="empty">没有匹配的成员。</p></section>
    </template>
  </section>
</template>
<style scoped>.department-grid{grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),1fr))}.selected{outline:2px solid var(--primary,#2563eb)}</style>
