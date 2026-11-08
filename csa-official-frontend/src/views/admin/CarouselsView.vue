<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { carouselService, type CarouselAdminItem, type SaveCarouselPayload } from '@/services/carousel'
import { fileService } from '@/services/file'
import { useAuthStore } from '@/store/useAuthStore'
import { formatDateTime, resolveAssetUrl } from '@/lib/format'
import { notify } from '@/lib/notify'
import { errorText } from './helpers'

const auth = useAuthStore()
const canManage = computed(() => (auth.user?.roleLevel ?? 0) >= 4)
const items = ref<CarouselAdminItem[]>([])
const loading = ref(false), saving = ref(false), uploading = ref(false), error = ref('')
const mutating = ref<number | null>(null)
const emptyDraft = (): SaveCarouselPayload => ({ title: '', imgUrl: '', targetUrl: '', sortOrder: 0, status: 1 })
const draft = ref(emptyDraft())
const busy = computed(() => saving.value || uploading.value || mutating.value !== null)
const enabledCount = computed(() => items.value.filter(item => item.status === 1).length)
function safeTarget(value?: string | null) {
  if (!value?.trim()) return ''
  try {
    const parsed = new URL(value.trim(), window.location.origin)
    return ['http:', 'https:'].includes(parsed.protocol) ? parsed.href : ''
  } catch { return '' }
}
async function load() {
  if (!canManage.value) return
  loading.value = true; error.value = ''
  try { items.value = await carouselService.list() }
  catch (e) { error.value = errorText(e); items.value = [] }
  finally { loading.value = false }
}
function edit(item: CarouselAdminItem) {
  draft.value = { id: item.id, title: item.title, imgUrl: item.imgUrl, targetUrl: item.targetUrl || '', sortOrder: item.sortOrder ?? 0, status: item.status === 1 ? 1 : 0 }
  document.getElementById('carousel-editor')?.scrollIntoView({ behavior: 'smooth' })
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]; input.value = ''
  if (!file || !canManage.value || busy.value) return
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) { notify.error('请选择 JPG、PNG 或 GIF 图片'); return }
  uploading.value = true
  try { draft.value.imgUrl = await fileService.upload(file); notify.success('轮播图片已上传') }
  catch (e) { notify.error(errorText(e)) }
  finally { uploading.value = false }
}
async function save() {
  if (!canManage.value || busy.value) return
  const payload = { ...draft.value, title: draft.value.title.trim(), imgUrl: draft.value.imgUrl.trim(), targetUrl: draft.value.targetUrl?.trim() || null, sortOrder: Number(draft.value.sortOrder) }
  if (!payload.title || !payload.imgUrl) { notify.error('请填写标题并上传图片或填写图片地址'); return }
  if (!Number.isInteger(payload.sortOrder) || Math.abs(payload.sortOrder) > 100000) { notify.error('排序值必须是 -100000 到 100000 之间的整数'); return }
  if (payload.targetUrl && !safeTarget(payload.targetUrl)) { notify.error('跳转地址须为站内路径或 HTTP(S) 地址'); return }
  saving.value = true
  try {
    await carouselService.save(payload)
    notify.success(payload.id ? '轮播图已更新' : '轮播图已创建'); draft.value = emptyDraft(); await load()
  } catch (e) { notify.error(errorText(e)) }
  finally { saving.value = false }
}
async function mutate(item: CarouselAdminItem, action: 'toggle' | 'delete') {
  if (!canManage.value || busy.value) return
  if (action === 'delete' && !window.confirm(`确认删除轮播图“${item.title}”吗？`)) return
  mutating.value = item.id
  try {
    if (action === 'delete') {
      await carouselService.remove(item.id)
      if (draft.value.id === item.id) draft.value = emptyDraft()
      notify.success('轮播图已删除')
    } else {
      const status = item.status === 1 ? 0 : 1
      await carouselService.save({ id: item.id, title: item.title, imgUrl: item.imgUrl, targetUrl: item.targetUrl, sortOrder: item.sortOrder, status })
      if (draft.value.id === item.id) draft.value.status = status
      notify.success(status === 1 ? '轮播图已启用' : '轮播图已停用')
    }
    await load()
  } catch (e) { notify.error(errorText(e)) }
  finally { mutating.value = null }
}
onMounted(load)
</script>

<template>
  <section class="page stack">
    <header class="toolbar"><div><h1>首页轮播管理</h1><p class="muted">维护图片、跳转地址、显示顺序和启用状态。保存后清理公开缓存。</p></div><button v-if="canManage" class="secondary" :disabled="loading || busy" @click="load">刷新列表</button></header>
    <p v-if="!canManage" class="panel">该页面仅向会长及以上开放。</p>
    <template v-else>
      <div class="toolbar panel"><span>轮播总数 {{ items.length }}</span><span>当前启用 {{ enabledCount }}</span><span>{{ draft.id ? '编辑中' : '新建中' }}</span></div>
      <div id="carousel-editor" class="grid">
        <form class="panel stack" @submit.prevent="save"><div class="toolbar"><h2>{{ draft.id ? '编辑轮播图' : '新增轮播图' }}</h2><button v-if="draft.id" type="button" class="secondary" :disabled="busy" @click="draft = emptyDraft()">取消编辑</button></div>
          <fieldset class="stack" :disabled="busy"><label class="field">标题<input v-model="draft.title" maxlength="128" required /></label>
            <label class="field">排序值<input v-model.number="draft.sortOrder" type="number" min="-100000" max="100000" step="1" required /></label><p class="muted">排序值越小越靠前。</p>
            <label class="field">上传图片<input type="file" accept="image/jpeg,image/png,image/gif" @change="upload" /></label><p v-if="uploading" role="status">正在上传…</p>
            <label class="field">图片地址<input v-model="draft.imgUrl" maxlength="500" required placeholder="上传后的文件路径或 HTTPS 图片地址" /></label>
            <label class="field">跳转地址（选填）<input v-model="draft.targetUrl" maxlength="500" placeholder="/competitions 或 https://…" /></label>
            <label class="field">状态<select v-model.number="draft.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
            <div class="actions"><button>{{ saving ? '保存中…' : '保存轮播图' }}</button><button type="button" class="secondary" @click="draft = emptyDraft()">重置</button></div>
          </fieldset>
        </form>
        <section class="panel stack"><h2>图片预览</h2><img v-if="resolveAssetUrl(draft.imgUrl)" class="preview-image" :src="resolveAssetUrl(draft.imgUrl)" :alt="draft.title || '轮播图片预览'" /><div v-else class="empty">上传图片或填写图片地址后预览。</div><h3>{{ draft.title || '轮播标题' }}</h3><a v-if="safeTarget(draft.targetUrl)" :href="safeTarget(draft.targetUrl)" target="_blank" rel="noopener noreferrer">打开目标页面 ↗</a><p v-else class="muted">不设置跳转</p></section>
      </div>
      <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在加载轮播列表…</p>
      <section v-else class="panel table-wrap"><h2>轮播列表</h2><table><thead><tr><th>图片 / 标题</th><th>跳转</th><th>排序</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td><img v-if="resolveAssetUrl(item.imgUrl)" class="thumbnail" :src="resolveAssetUrl(item.imgUrl)" :alt="item.title" />{{ item.title }}</td><td><a v-if="safeTarget(item.targetUrl)" :href="safeTarget(item.targetUrl)" target="_blank" rel="noopener noreferrer">{{ item.targetUrl }}</a><span v-else>不设置跳转</span></td><td>{{ item.sortOrder }}</td><td>{{ item.status === 1 ? '启用' : '停用' }}</td><td>{{ formatDateTime(item.updateTime || item.createTime) }}</td><td><div class="actions"><button class="secondary" :disabled="busy" @click="edit(item)">编辑</button><button class="secondary" :disabled="busy" @click="mutate(item, 'toggle')">{{ item.status === 1 ? '停用' : '启用' }}</button><button class="danger" :disabled="busy" @click="mutate(item, 'delete')">删除</button></div></td></tr><tr v-if="!items.length"><td colspan="6" class="empty">尚未添加轮播图。</td></tr></tbody></table></section>
    </template>
  </section>
</template>
<style scoped>.preview-image { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; border-radius: 12px; } .thumbnail { width: 110px; height: 64px; object-fit: cover; display: block; margin-bottom: 8px; border-radius: 6px; } #carousel-editor { scroll-margin-top: 2rem; }</style>
