<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { competitionService, type CompetitionItem, type CompetitionDetailItem } from '@/services/competition'
import { userService, type UserDirectoryItem } from '@/services/user'
import { fileService } from '@/services/file'
import { formatDateTime, resolveAssetUrl } from '@/lib/format'
import { sanitizeHtml } from '@/lib/sanitize-html'
import { notify } from '@/lib/notify'
import { ApiError } from '@/types/api'

const route = useRoute(), auth = useAuthStore()
const dashboard = computed(() => route.path.startsWith('/dashboard'))
const canManage = computed(() => dashboard.value && (auth.user?.roleLevel ?? -1) >= 3)
const items = ref<CompetitionItem[]>([]), members = ref<UserDirectoryItem[]>([])
const page = ref(1), pages = ref(1), loading = ref(false), saving = ref(false), uploading = ref(false), granting = ref(false), error = ref(''), needsLogin = ref(false)
const empty = () => ({ id: undefined as number | undefined, title: '', content: '', coverImg: '', startTime: '', endTime: '', status: 1 })
const draft = ref(empty()), grantTarget = ref<CompetitionItem | null>(null), editorId = ref<number | null>(null), keyword = ref('')
const detail = ref<CompetitionDetailItem | null>(null), detailBusy = ref(false)
const filteredMembers = computed(() => members.value.filter(m => (m.roleLevel ?? 0) >= 1 && [m.realName, m.username, m.email, m.departmentName].some(v => v?.toLowerCase().includes(keyword.value.trim().toLowerCase()))))
const statuses = [{ value: 0, label: '未发布' }, { value: 1, label: '进行中' }, { value: 2, label: '已结束' }]
const statusLabel = (value: CompetitionItem['status']) => statuses.find(s => s.value === Number(value))?.label || String(value ?? '待更新')
function dateInput(value: string | null) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? value.slice(0, 16) : new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}
let request = 0
async function load(nextPage = page.value) {
  const id = ++request; loading.value = true; error.value = ''; needsLogin.value = false
  const list = dashboard.value ? competitionService.list : competitionService.listPublic
  try {
    let response = await list({ page: nextPage, size: 6 })
    if (nextPage > Math.max(response.pages || 1, 1)) { nextPage = Math.max(response.pages || 1, 1); response = await list({ page: nextPage, size: 6 }) }
    if (id !== request) return
    items.value = response.records; pages.value = Math.max(response.pages || 1, 1); page.value = nextPage
  } catch (e) {
    if (id !== request) return
    items.value = []
    if (!dashboard.value && e instanceof ApiError && (e.status === 401 || e.code === 401)) needsLogin.value = true
    else error.value = e instanceof Error ? e.message : '比赛加载失败'
  } finally { if (id === request) loading.value = false }
}
watch(dashboard, () => { draft.value = empty(); grantTarget.value = null; detail.value = null; void load(1) }, { immediate: true })
watch(canManage, async allowed => { if (allowed) { try { members.value = await userService.list({ minRoleLevel: 1, size: 120 }) } catch (e) { notify.error(e instanceof Error ? e.message : '成员加载失败') } } }, { immediate: true })
async function openDetail(item: CompetitionItem, edit = false) {
  if (detailBusy.value) return
  const fromDashboard = dashboard.value
  detailBusy.value = true
  try {
    const result = await (dashboard.value ? competitionService.detail(item.id) : competitionService.detailPublic(item.id))
    if (fromDashboard !== dashboard.value) return
    // 列表只有摘要；编辑必须读取全文并再次核对当前权限。
    if (edit) {
      if (result.canEdit !== true) throw new Error('你已无权编辑此比赛，请刷新列表')
      if (result.status == null || ![0, 1, 2, '0', '1', '2'].includes(result.status)) throw new Error('比赛状态未知，请刷新后重试')
      draft.value = { id: result.id, title: result.title, content: result.content || '', coverImg: result.coverImg || '', startTime: dateInput(result.startTime), endTime: dateInput(result.endTime), status: Number(result.status) }
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    else detail.value = result
  } catch (e) { notify.error(e instanceof Error ? e.message : '详情加载失败') }
  finally { detailBusy.value = false }
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''
  if (!file) return
  uploading.value = true
  try { draft.value.coverImg = await fileService.upload(file); notify.success('封面已上传') }
  catch (e) { notify.error(e instanceof Error ? e.message : '上传失败') }
  finally { uploading.value = false }
}
async function save() {
  if (!canManage.value || saving.value) return
  if (!draft.value.title.trim() || !draft.value.content.trim()) { notify.error('请填写比赛标题与正文'); return }
  if (draft.value.startTime && draft.value.endTime && new Date(draft.value.endTime) < new Date(draft.value.startTime)) { notify.error('结束时间不能早于开始时间'); return }
  saving.value = true
  try { await competitionService.save({ ...draft.value, title: draft.value.title.trim(), content: draft.value.content.trim(), startTime: draft.value.startTime || null, endTime: draft.value.endTime || null, coverImg: draft.value.coverImg.trim() || null }); draft.value = empty(); notify.success('比赛已保存'); await load(1) }
  catch (e) { notify.error(e instanceof Error ? e.message : '保存失败') }
  finally { saving.value = false }
}
async function grant() {
  if (!grantTarget.value?.canGrant || !editorId.value || granting.value) return
  granting.value = true
  try { await competitionService.grantEditor({ compId: grantTarget.value.id, targetUserId: editorId.value }); editorId.value = null; notify.success('协作者已授权') }
  catch (e) { notify.error(e instanceof Error ? e.message : '授权失败') }
  finally { granting.value = false }
}
</script>

<template>
  <section class="page stack">
    <header><p class="eyebrow">COMPETITIONS</p><h1>{{ dashboard ? '比赛管理' : '比赛与活动' }}</h1><p class="muted">发现比赛、参与训练，在真实项目中共同成长。</p></header>
    <form v-if="canManage" class="panel stack" @submit.prevent="save"><h2>{{ draft.id ? '编辑比赛' : '发布比赛' }}</h2><div class="form-grid"><label class="field">比赛标题<input v-model="draft.title" :disabled="saving || uploading || detailBusy" required /></label><label class="field">状态<select v-model.number="draft.status" :disabled="saving || uploading || detailBusy"><option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option></select></label></div><label class="field">比赛详情<textarea v-model="draft.content" :disabled="saving || uploading || detailBusy" rows="7" required /></label><div class="form-grid"><label class="field">开始时间<input v-model="draft.startTime" :disabled="saving || uploading || detailBusy" type="datetime-local" /></label><label class="field">结束时间<input v-model="draft.endTime" :disabled="saving || uploading || detailBusy" type="datetime-local" /></label></div><label class="field">封面地址<input v-model="draft.coverImg" :disabled="saving || uploading || detailBusy" /></label><label class="field">上传封面<input type="file" accept="image/*" :disabled="uploading || saving" @change="upload" /></label><img v-if="resolveAssetUrl(draft.coverImg)" :src="resolveAssetUrl(draft.coverImg)" class="cover" alt="比赛封面预览" /><div class="actions"><button :disabled="saving || uploading">{{ saving ? '保存中…' : draft.id ? '保存修改' : '发布比赛' }}</button><button type="button" class="secondary" :disabled="saving || uploading" @click="draft = empty()">取消 / 清空</button></div></form>
    <form v-if="canManage && grantTarget" class="panel stack" @submit.prevent="grant"><h2>协作者授权：{{ grantTarget.title }}</h2><label class="field">搜索成员<input v-model="keyword" placeholder="姓名、账号、邮箱或部门" /></label><label class="field">选择协作者<select v-model.number="editorId" required><option :value="null" disabled>请选择</option><option v-for="m in filteredMembers" :key="m.id" :value="m.id">{{ m.realName || m.username }} · @{{ m.username }} · {{ m.departmentName || '未分配部门' }}</option></select></label><p v-if="!filteredMembers.length" class="muted">没有匹配的成员。</p><div class="actions"><button :disabled="granting || !editorId">确认授权</button><button type="button" class="secondary" :disabled="granting" @click="grantTarget = null">取消</button></div></form>
    <p v-if="error" class="error" role="alert">{{ error }} <button class="secondary" @click="load()">重试</button></p>
    <div v-if="needsLogin" class="panel"><p>当前服务要求登录后查看比赛。</p><RouterLink class="button" to="/login?redirect=%2Fcompetitions">登录后查看</RouterLink></div>
    <p v-else-if="loading" role="status">正在加载比赛…</p>
    <div v-else-if="!items.length && !error" class="panel empty">暂无比赛与活动。</div>
    <div v-else class="grid competition-grid"><article v-for="item in items" :key="item.id" class="panel stack"><img v-if="resolveAssetUrl(item.coverImg)" :src="resolveAssetUrl(item.coverImg)" :alt="item.title" class="cover" loading="lazy" /><span class="badge">{{ statusLabel(item.status) }}</span><h2>{{ item.title }}</h2><p>{{ item.summary || '暂无活动简介' }}</p><p class="muted">{{ formatDateTime(item.startTime) }} 至 {{ formatDateTime(item.endTime) }}</p><div class="actions"><button class="secondary" :disabled="detailBusy" @click="openDetail(item)">查看详情</button><button v-if="canManage && item.canEdit" :disabled="detailBusy || saving || uploading" @click="openDetail(item, true)">编辑</button><button v-if="canManage && item.canGrant" class="secondary" :disabled="granting" @click="grantTarget = item; editorId = null">授权协作</button></div></article></div>
    <nav v-if="!needsLogin" class="toolbar" aria-label="比赛分页"><button class="secondary" :disabled="page <= 1 || loading" @click="load(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="page >= pages || loading" @click="load(page + 1)">下一页</button></nav>
    <section v-if="detail" class="panel stack" aria-label="比赛详情"><div class="toolbar"><h2>{{ detail.title }}</h2><button class="secondary" @click="detail = null">关闭详情</button></div><div class="prose" v-html="sanitizeHtml(detail.content)" /></section>
  </section>
</template>

<style scoped>.competition-grid{grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr))}.cover{width:100%;max-height:260px;object-fit:cover;border-radius:12px}</style>
