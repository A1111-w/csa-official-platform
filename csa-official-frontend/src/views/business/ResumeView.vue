<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { resumeService, RESUME_STATUS, type ResumeData, type ResumeGitSyncData } from '@/services/resume'
import { formatDateTime } from '@/lib/format'
import { notify } from '@/lib/notify'

const auth = useAuthStore(), canAccess = computed(() => (auth.user?.roleLevel ?? -1) >= 2)
const resume = ref<ResumeData | null>(null), gitSync = ref<ResumeGitSyncData | null>(null)
const gitUrl = ref(''), content = ref(''), loading = ref(false), saving = ref(false), syncing = ref(false), error = ref('')
// 没有简历才视作新草稿；已有记录缺少状态时不能默认允许写入。
const status = computed(() => resume.value == null ? RESUME_STATUS.DRAFT : resume.value.status)
const pending = computed(() => status.value === RESUME_STATUS.PENDING)
const editable = computed(() => [RESUME_STATUS.DRAFT, RESUME_STATUS.APPROVED, RESUME_STATUS.REJECTED].some(value => value === status.value))
const statusLabel = computed(() => ['草稿', '审核中', '已通过', '已驳回'][status.value] || '未知')
const syncLabel = computed(() => ({ NOT_SYNCED: '尚未同步', SYNCING: '同步中', SUCCEEDED: '同步成功', FAILED: '同步失败' }[gitSync.value?.status || 'NOT_SYNCED']))
const syncErrors: Record<string, string> = { GIT_REPOSITORY_TOO_LARGE: '仓库超过服务器允许的大小。', GIT_SYNC_QUEUE_FULL: '同步任务较多，请稍后重试。', UPSTREAM_ERROR: '远程仓库暂时无法访问。' }
const syncError = computed(() => syncErrors[gitSync.value?.errorCode || ''] || `同步失败（${gitSync.value?.errorCode || '未知原因'}），请稍后重试。`)
function validGitUrl(value: string) { try { const url = new URL(value); return value.length <= 255 && url.protocol === 'https:' && !url.username && !url.password && !url.search && !url.hash } catch { return false } }
function validate() {
  if (gitUrl.value.trim() && !validGitUrl(gitUrl.value.trim())) { notify.error('请填写有效的 HTTPS 仓库地址'); return false }
  if (!content.value.trim() && !gitUrl.value.trim()) { notify.error('请至少填写简历内容或仓库链接'); return false }
  return true
}
function formatBytes(bytes: number | null | undefined) { return bytes == null ? '—' : bytes < 1024 ? `${bytes} B` : bytes < 1048576 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1048576).toFixed(1)} MB` }
async function load() {
  if (!canAccess.value) return
  loading.value = true; error.value = ''
  try {
    const [data, sync] = await Promise.all([resumeService.getMyResume(), resumeService.getGitSyncStatus()])
    resume.value = data; gitSync.value = sync; gitUrl.value = data?.gitRepoUrl || ''; content.value = data?.content || ''
  } catch (e) { error.value = e instanceof Error ? e.message : '简历加载失败' }
  finally { loading.value = false }
}
watch(canAccess, allowed => { if (allowed) void load() }, { immediate: true })
let timer: ReturnType<typeof setTimeout> | undefined
let stopped = false
async function poll() {
  if (stopped || !canAccess.value || gitSync.value?.status !== 'SYNCING') return
  try {
    const sync = await resumeService.getGitSyncStatus()
    if (stopped) return
    gitSync.value = sync
    if (sync.status !== 'SYNCING') resume.value = await resumeService.getMyResume()
  } catch { /* Keep the last known state; refresh is available. */ }
  if (!stopped && gitSync.value?.status === 'SYNCING') timer = setTimeout(poll, 2000)
}
watch(() => gitSync.value?.status, value => { clearTimeout(timer); if (value === 'SYNCING') timer = setTimeout(poll, 2000) })
onBeforeUnmount(() => { stopped = true; clearTimeout(timer) })
async function save(submit = false) {
  if (saving.value || !editable.value || !canAccess.value || !validate()) return
  saving.value = true
  try {
    await resumeService.save({ content: content.value.trim(), gitRepoUrl: gitUrl.value.trim() })
    if (submit) await resumeService.submit()
    notify.success(submit ? '已提交审核，请等待审核结果' : '简历草稿已保存'); await load()
  } catch (e) { notify.error(e instanceof Error ? e.message : '保存或提交失败') }
  finally { saving.value = false }
}
async function syncRepository() {
  if (!canAccess.value || syncing.value || (!editable.value && !pending.value) || gitSync.value?.status === 'SYNCING') return
  if (!validGitUrl(gitUrl.value.trim())) { notify.error('请先填写有效的 HTTPS 仓库地址'); return }
  syncing.value = true
  try {
    if (!pending.value && gitUrl.value.trim() !== (resume.value?.gitRepoUrl || '')) await resumeService.save({ content: content.value.trim(), gitRepoUrl: gitUrl.value.trim() })
    gitSync.value = await resumeService.syncGitRepository(); notify.success('仓库同步任务已启动')
  } catch (e) { notify.error(e instanceof Error ? e.message : '同步启动失败') }
  finally { syncing.value = false }
}
async function refreshSync() {
  try { gitSync.value = await resumeService.getGitSyncStatus() }
  catch (e) { notify.error(e instanceof Error ? e.message : '同步状态刷新失败') }
}
</script>

<template>
  <section class="page stack">
    <header><p class="eyebrow">MY RESUME</p><h1>我的简历</h1><p class="muted">记录项目经历与代表作品，提交给部长审核。</p></header>
    <div v-if="!canAccess" class="panel">简历工作区面向核心成员及以上角色开放。</div>
    <p v-else-if="loading" role="status">正在加载简历…</p>
    <div v-else-if="error" class="panel error" role="alert">{{ error }} <button class="secondary" @click="load">重试</button></div>
    <template v-else>
      <div class="toolbar"><span class="badge">{{ statusLabel }}</span><span class="muted">上次保存：{{ formatDateTime(resume?.updateTime) }}</span></div>
      <p v-if="pending" class="panel" role="status">审核期间暂时锁定编辑，请等待审核结果。</p>
      <p v-if="status === RESUME_STATUS.REJECTED" class="error" role="alert">审核未通过：{{ resume?.rejectReason || '暂无原因，请联系审核人。' }}</p>
      <p v-if="status === RESUME_STATUS.APPROVED" class="success">简历已通过审核。再次提交后将重新进入审核队列。</p>
      <p v-if="!editable && !pending" class="error">简历状态未知，暂不能修改。请刷新后确认状态。</p>
      <form class="panel stack" @submit.prevent="save(false)">
        <label class="field">项目仓库链接<input v-model="gitUrl" maxlength="255" placeholder="https://github.com/yourname/project" :disabled="!editable || saving || syncing" /></label><a v-if="validGitUrl(gitUrl.trim())" :href="gitUrl.trim()" target="_blank" rel="noopener noreferrer">打开项目仓库 ↗</a>
        <label class="field">详细介绍（Markdown）<textarea v-model="content" maxlength="50000" rows="16" :disabled="!editable || saving || syncing" placeholder="# 个人简介&#10;- 擅长方向&#10;&#10;# 项目经历&#10;- 你解决了什么问题，承担了哪些工作&#10;&#10;# 比赛与协作" /></label>
        <p class="muted">优先写清最能代表技术深度的项目、个人职责与结果；仓库需对审核人可访问。</p>
        <div class="actions"><button class="secondary" :disabled="saving || syncing || !editable">保存草稿</button><button type="button" :disabled="saving || syncing || !editable" @click="save(true)">{{ saving ? '处理中…' : '提交审核' }}</button></div>
      </form>
      <section class="panel stack"><div class="toolbar"><h2>仓库同步</h2><span class="badge">{{ syncLabel }}</span></div><p class="muted">同步后审核人可以核对仓库分支、提交版本和大小。</p><p v-if="gitSync && !gitSync.configured" class="muted">服务器尚未配置仓库同步，请联系管理员。</p><div class="actions"><button :disabled="saving || syncing || gitSync?.status === 'SYNCING' || !gitSync?.configured || (!editable && !pending) || !validGitUrl(gitUrl.trim())" @click="syncRepository">{{ syncing ? '启动中…' : '同步仓库' }}</button><button class="secondary" @click="refreshSync">刷新同步状态</button></div><dl v-if="gitSync?.status === 'SUCCEEDED'" class="form-grid"><div><dt>分支</dt><dd>{{ gitSync.branch || '—' }}</dd></div><div><dt>提交</dt><dd class="commit">{{ gitSync.commit || '—' }}</dd></div><div><dt>仓库大小</dt><dd>{{ formatBytes(gitSync.sizeBytes) }}</dd></div></dl><p v-if="gitSync?.status === 'FAILED'" class="error">{{ syncError }}</p><p v-if="gitSync?.completedAt" class="muted">最近完成：{{ formatDateTime(gitSync.completedAt) }}</p></section>
    </template>
  </section>
</template>
<style scoped>.commit{overflow-wrap:anywhere}textarea{font-family:ui-monospace,monospace}</style>
