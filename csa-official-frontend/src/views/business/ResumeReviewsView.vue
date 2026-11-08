<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { resumeService, RESUME_STATUS, isPendingResumeReview, type ResumeReviewListItem, type ResumeReviewDetail, type ResumeReviewStatus } from '@/services/resume'
import { formatDateTime } from '@/lib/format'
import { notify } from '@/lib/notify'

const auth = useAuthStore(), canReview = computed(() => (auth.user?.roleLevel ?? -1) >= 3)
const status = ref<ResumeReviewStatus>(RESUME_STATUS.PENDING), page = ref(1), pages = ref(1), total = ref(0)
const items = ref<ResumeReviewListItem[]>([]), detail = ref<ResumeReviewDetail | null>(null), reason = ref('')
const loading = ref(false), detailLoading = ref(false), reviewing = ref(false), error = ref('')
const filters: { value: ResumeReviewStatus; label: string }[] = [{ value: RESUME_STATUS.PENDING, label: '待审核' }, { value: RESUME_STATUS.APPROVED, label: '已通过' }, { value: RESUME_STATUS.REJECTED, label: '已驳回' }]
const label = (value: number) => filters.find(f => f.value === value)?.label || '未知'
const name = (item: ResumeReviewListItem) => item.realName || item.username || '未命名成员'
function safeRepository(value: string | null) { try { const url = new URL(value || ''); return url.protocol === 'https:' && !url.username && !url.password ? url.href : '' } catch { return '' } }
let request = 0
async function load(nextPage = page.value) {
  if (!canReview.value) return
  const id = ++request; loading.value = true; error.value = ''
  try {
    let response = await resumeService.listReviews({ page: nextPage, size: 12, status: status.value })
    if (nextPage > Math.max(response.pages || 1, 1)) { nextPage = Math.max(response.pages || 1, 1); response = await resumeService.listReviews({ page: nextPage, size: 12, status: status.value }) }
    if (id !== request) return
    items.value = response.records; pages.value = Math.max(response.pages || 1, 1); total.value = response.total; page.value = nextPage
  } catch (e) { if (id === request) { items.value = []; error.value = e instanceof Error ? e.message : '审核队列加载失败' } }
  finally { if (id === request) loading.value = false }
}
watch(canReview, allowed => { if (allowed) void load(1) }, { immediate: true })
async function openDetail(item: ResumeReviewListItem) {
  if (detailLoading.value || reviewing.value) return
  detailLoading.value = true; detail.value = null; reason.value = ''
  try { detail.value = await resumeService.reviewDetail(item.id); reason.value = detail.value.rejectReason || '' }
  catch (e) { notify.error(e instanceof Error ? e.message : '详情加载失败') }
  finally { detailLoading.value = false }
}
async function decide(pass: boolean) {
  if (!detail.value || !isPendingResumeReview(detail.value.status) || reviewing.value) return
  if (!pass && !reason.value.trim()) { notify.error('驳回时必须填写原因'); return }
  if (!pass && reason.value.trim().length > 500) { notify.error('审核原因不能超过 500 个字符'); return }
  reviewing.value = true
  try {
    await resumeService.audit({ resumeId: detail.value.id, pass, reason: pass ? undefined : reason.value.trim() })
    detail.value = null; notify.success(pass ? '简历已通过' : '简历已驳回'); await load()
  } catch (e) {
    notify.error(e instanceof Error ? e.message : '审核失败，请刷新详情确认最新状态')
    // Another reviewer may have handled this resume; reload the authoritative state.
    const id = detail.value?.id
    if (id) { try { detail.value = await resumeService.reviewDetail(id) } catch { detail.value = null } }
    await load()
  } finally { reviewing.value = false }
}
</script>

<template>
  <section class="page stack"><header><p class="eyebrow">RESUME REVIEWS</p><h1>简历审核</h1><p class="muted">阅读成员经历，给出清晰、可执行的审核反馈。</p></header>
    <div v-if="!canReview" class="panel">该页面仅对部长及以上角色开放。</div>
    <template v-else>
      <div class="toolbar"><label>审核状态 <select v-model.number="status" :disabled="loading || reviewing" @change="detail = null; load(1)"><option v-for="filter in filters" :key="filter.value" :value="filter.value">{{ filter.label }}</option></select></label><span class="muted">共 {{ total }} 份</span><button class="secondary" :disabled="loading" @click="load()">刷新队列</button></div>
      <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载审核队列…</p><div v-else-if="!items.length && !error" class="panel empty">当前状态下没有简历。</div>
      <div v-else class="grid reviews-grid"><article v-for="item in items" :key="item.id" class="panel stack"><span class="badge">{{ label(item.status) }}</span><h2>{{ name(item) }}</h2><p class="muted">@{{ item.username }} · {{ item.departmentName || '未分配部门' }}</p><p>{{ item.contentSummary || '未填写正文，请查看仓库信息。' }}</p><p class="muted">更新时间：{{ formatDateTime(item.updateTime) }}</p><button class="secondary" :disabled="detailLoading || reviewing" @click="openDetail(item)">查看详情</button></article></div>
      <nav class="toolbar" aria-label="简历分页"><button class="secondary" :disabled="page <= 1 || loading" @click="load(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="page >= pages || loading" @click="load(page + 1)">下一页</button></nav>
      <p v-if="detailLoading" role="status">正在读取完整简历…</p>
      <section v-if="detail" class="panel stack" aria-label="简历详情"><div class="toolbar"><h2>{{ name(detail) }} 的简历</h2><button class="secondary" :disabled="reviewing" @click="detail = null">关闭详情</button></div><span class="badge">{{ label(detail.status) }}</span><dl class="form-grid"><div><dt>邮箱</dt><dd>{{ detail.email || '—' }}</dd></div><div><dt>学号</dt><dd>{{ detail.studentId || '—' }}</dd></div><div><dt>学院 / 班级</dt><dd>{{ detail.college || '—' }} / {{ detail.className || '—' }}</dd></div><div><dt>部门</dt><dd>{{ detail.departmentName || '—' }}</dd></div></dl><h3>简历正文</h3><pre class="resume-content">{{ detail.content || '未填写正文' }}</pre><h3>仓库信息</h3><a v-if="safeRepository(detail.gitRepoUrl)" :href="safeRepository(detail.gitRepoUrl)" target="_blank" rel="noopener noreferrer">打开仓库 ↗</a><p v-else class="muted">未提供有效的 HTTPS 仓库链接。</p><dl class="form-grid"><div><dt>同步状态</dt><dd>{{ detail.gitSyncStatus || 'NOT_SYNCED' }}</dd></div><div><dt>同步分支</dt><dd>{{ detail.gitSyncBranch || '—' }}</dd></div><div><dt>提交版本</dt><dd class="commit">{{ detail.gitSyncCommit || '—' }}</dd></div><div><dt>仓库大小</dt><dd>{{ detail.gitSyncSizeBytes == null ? '—' : `${detail.gitSyncSizeBytes} B` }}</dd></div><div><dt>同步完成</dt><dd>{{ formatDateTime(detail.gitSyncCompletedAt) }}</dd></div></dl><p v-if="detail.gitSyncErrorCode" class="error">仓库同步错误：{{ detail.gitSyncErrorCode }}</p>
        <template v-if="isPendingResumeReview(detail.status)"><label class="field">审核反馈（驳回必填）<textarea v-model="reason" rows="4" maxlength="500" :disabled="reviewing" placeholder="说明需要补充的内容及改进建议" /></label><div class="actions"><button :disabled="reviewing" @click="decide(true)">通过审核</button><button class="danger" :disabled="reviewing" @click="decide(false)">驳回并反馈</button></div></template><template v-else><p>审核人：{{ detail.auditorName || detail.auditBy || '—' }} · {{ formatDateTime(detail.auditTime) }}</p><p v-if="detail.rejectReason">驳回原因：{{ detail.rejectReason }}</p></template>
      </section>
    </template>
  </section>
</template>
<style scoped>.reviews-grid{grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr))}.resume-content{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;font-family:inherit}.commit{overflow-wrap:anywhere}</style>
