<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { voteService, type ProposalItem } from '@/services/vote'
import { formatDateTime, parseVoteResult } from '@/lib/format'
import { notify } from '@/lib/notify'

const auth = useAuthStore(), canUse = computed(() => (auth.user?.roleLevel ?? -1) >= 3)
const proposals = ref<ProposalItem[]>([]), loading = ref(false), creating = ref(false), voting = ref<number | null>(null), error = ref('')
const comments = reactive<Record<number, string>>({}), voted = reactive(new Set<number>())
const empty = () => ({ type: 'CODE_DEPLOY', title: '', reason: '' })
const draft = ref(empty())
const types = [{ value: 'CODE_DEPLOY', label: '代码发布' }, { value: 'ACTIVITY', label: '活动决策' }, { value: 'RULE_UPDATE', label: '规则调整' }]
const sorted = computed(() => [...proposals.value].sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || ''))))
const statusLabel = (status: number) => ['投票中', '已通过', '已否决'][status] || '已结束'
const open = (item: ProposalItem) => item.status === 0 && (!item.expireTime || new Date(item.expireTime).getTime() > Date.now())
async function load() {
  if (!canUse.value) return
  loading.value = true; error.value = ''
  try { proposals.value = await voteService.list() }
  catch (e) { error.value = e instanceof Error ? e.message : '提案加载失败' }
  finally { loading.value = false }
}
watch(canUse, allowed => { if (allowed) void load() }, { immediate: true })
async function create() {
  if (!canUse.value || creating.value) return
  if (!draft.value.title.trim() || !draft.value.reason.trim()) { notify.error('请补全提案标题和理由'); return }
  creating.value = true
  try { await voteService.create({ type: draft.value.type, title: draft.value.title.trim(), reason: draft.value.reason.trim() }); draft.value = empty(); notify.success('提案已创建'); await load() }
  catch (e) { notify.error(e instanceof Error ? e.message : '提案创建失败') }
  finally { creating.value = false }
}
async function vote(item: ProposalItem, agree: boolean) {
  if (!canUse.value || voting.value !== null || voted.has(item.id) || !open(item)) return
  voting.value = item.id
  try { await voteService.submit({ proposalId: item.id, agree, comment: comments[item.id]?.trim() || undefined }); voted.add(item.id); notify.success(agree ? '已提交赞成票' : '已提交反对票'); await load() }
  catch (e) { notify.error(e instanceof Error ? e.message : '投票失败'); await load() }
  finally { voting.value = null }
}
</script>

<template>
  <section class="page stack"><header><p class="eyebrow">GOVERNANCE</p><h1>提案中心</h1><p class="muted">让代码发布、活动安排和规则调整留下清楚的决策记录。</p></header>
    <div v-if="!canUse" class="panel">提案中心仅向部长及以上角色开放。</div>
    <template v-else>
      <form class="panel stack" @submit.prevent="create"><h2>发起提案</h2><div class="form-grid"><label class="field">提案类型<select v-model="draft.type" :disabled="creating"><option v-for="type in types" :key="type.value" :value="type.value">{{ type.label }}</option></select></label><label class="field">提案标题<input v-model="draft.title" :disabled="creating" required maxlength="200" /></label></div><label class="field">背景与理由<textarea v-model="draft.reason" :disabled="creating" maxlength="2000" required rows="4" placeholder="说明背景、风险和预期结果" /></label><div class="actions"><button :disabled="creating">{{ creating ? '提交中…' : '发起提案' }}</button></div></form>
      <div class="toolbar"><h2>提案记录</h2><button class="secondary" :disabled="loading" @click="load">刷新</button></div><p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载提案…</p><div v-else-if="!sorted.length && !error" class="panel empty">暂无提案。</div>
      <article v-for="item in sorted" :key="item.id" class="panel stack"><div class="toolbar"><span class="badge">{{ statusLabel(item.status) }}</span><span class="muted">{{ types.find(t => t.value === item.type)?.label || item.type }}</span></div><h2>{{ item.title }}</h2><p class="reason">{{ item.reason }}</p><p class="muted">创建于 {{ formatDateTime(item.createTime) }} · 截止 {{ formatDateTime(item.expireTime) }}</p><p v-if="parseVoteResult(item.finalResultJson)">赞成 {{ parseVoteResult(item.finalResultJson)?.agree }} 票 · 反对 {{ parseVoteResult(item.finalResultJson)?.reject }} 票</p><template v-if="open(item) && !voted.has(item.id)"><label class="field">投票说明（可选）<textarea v-model="comments[item.id]" rows="2" :disabled="voting !== null" /></label><div class="actions"><button :disabled="voting !== null" @click="vote(item, true)">赞成</button><button class="secondary" :disabled="voting !== null" @click="vote(item, false)">反对</button></div></template><p v-else-if="voted.has(item.id)" class="success">你的投票已提交。</p><p v-else class="muted">本提案已结束或截止。</p></article>
    </template>
  </section>
</template>
<style scoped>.reason{white-space:pre-wrap}</style>
