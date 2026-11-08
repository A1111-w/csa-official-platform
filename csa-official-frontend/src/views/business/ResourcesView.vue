<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { resourceService, type ResourceItem } from '@/services/resource'
import { fileService } from '@/services/file'
import { formatDateTime, resolveAssetUrl } from '@/lib/format'
import { notify } from '@/lib/notify'

const auth = useAuthStore()
const route = useRoute()
const canView = computed(() => (auth.user?.roleLevel ?? -1) >= 1)
const canManage = computed(() => route.path.startsWith('/dashboard') && (auth.user?.roleLevel ?? -1) >= 3)
const canManageItem = (item: ResourceItem) => canManage.value && ((auth.user?.roleLevel ?? -1) >= 4 || auth.profile?.id === item.uploaderId)
const pageSize = 8
const items = ref<ResourceItem[]>([])
const page = ref(1), pages = ref(1), total = ref(0), category = ref('')
const categoryOptions = ref<string[]>([])
const loading = ref(false), saving = ref(false), uploading = ref(false), error = ref('')
const mutating = ref<number | null>(null)
const empty = () => ({ id: undefined as number | undefined, title: '', summary: '', fileUrl: '', category: '' })
const draft = ref(empty())
const categories = computed(() => [...new Set(['课程资料', '比赛题解', '项目模板', '工具软件', ...categoryOptions.value, category.value, draft.value.category, ...items.value.map(i => i.category)].filter(Boolean))])
// 请求序号防止旧分类的慢响应覆盖用户刚选中的分类。
let request = 0
async function loadResources(nextPage = page.value) {
  if (!canView.value) return
  const id = ++request
  loading.value = true; error.value = ''
  try {
    let response = await resourceService.list({ page: nextPage, size: pageSize, category: category.value || undefined })
    if (nextPage > Math.max(response.pages || 1, 1)) {
      nextPage = Math.max(response.pages || 1, 1)
      response = await resourceService.list({ page: nextPage, size: pageSize, category: category.value || undefined })
    }
    if (id !== request) return
    page.value = nextPage; items.value = response.records; pages.value = Math.max(response.pages || 1, 1); total.value = response.total || 0
  } catch (e) { if (id === request) error.value = e instanceof Error ? e.message : '资源加载失败' }
  finally { if (id === request) loading.value = false }
}
async function loadCategories() {
  try { categoryOptions.value = await resourceService.listCategories() }
  catch (e) { notify.error(e instanceof Error ? e.message : '分类加载失败') }
}
watch(canView, allowed => {
  if (allowed) { void loadResources(1); void loadCategories() }
  else { ++request; items.value = []; loading.value = false }
}, { immediate: true })
async function upload(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''
  if (!file) return
  uploading.value = true
  try { draft.value.fileUrl = await fileService.upload(file); notify.success('文件已上传，请保存资源') }
  catch (e) { notify.error(e instanceof Error ? e.message : '上传失败') }
  finally { uploading.value = false }
}
async function save() {
  if (saving.value || !canManage.value) return
  if (!draft.value.title.trim() || !draft.value.fileUrl.trim()) { notify.error('请填写标题和下载地址'); return }
  saving.value = true
  try {
    await resourceService.save({ ...draft.value, title: draft.value.title.trim(), fileUrl: draft.value.fileUrl.trim() })
    draft.value = empty(); notify.success('资源已保存'); await Promise.all([loadResources(1), loadCategories()])
  } catch (e) { notify.error(e instanceof Error ? e.message : '保存失败') }
  finally { saving.value = false }
}
async function remove(item: ResourceItem) {
  if (saving.value || uploading.value || mutating.value !== null || !canManageItem(item) || !window.confirm(`确认删除资源“${item.title}”吗？`)) return
  mutating.value = item.id
  try { await resourceService.remove(item.id); if (draft.value.id === item.id) draft.value = empty(); notify.success('资源已删除'); await Promise.all([loadResources(), loadCategories()]) }
  catch (e) { notify.error(e instanceof Error ? e.message : '删除失败') }
  finally { mutating.value = null }
}
async function download(item: ResourceItem) {
  const url = resolveAssetUrl(item.fileUrl)
  if (!url) { notify.error('没有有效的下载地址'); return }
  window.open(url, '_blank', 'noopener,noreferrer')
  try { await resourceService.trackDownload(item.id); item.downloadCount = (item.downloadCount || 0) + 1 }
  catch (e) { notify.info(e instanceof Error ? e.message : '资源已打开，下载计数暂未更新') }
}
</script>

<template>
  <section class="page stack">
    <header><p class="eyebrow">RESOURCES</p><h1>资源库</h1><p class="muted">课程资料、比赛题解与项目模板，让每一次积累都能被找到。</p></header>
    <div v-if="!canView" class="panel empty"><h2>资源库需要会员身份</h2><p>登录会员账号后即可浏览与下载资料。</p><RouterLink v-if="!auth.user" class="button" to="/login?redirect=%2Fresources">登录后查看</RouterLink><p v-else>当前账号尚未达到会员等级，请联系协会负责人。</p></div>
    <template v-else>
      <form v-if="canManage" class="panel stack" @submit.prevent="save">
        <h2>{{ draft.id ? '编辑资源' : '发布资源' }}</h2>
        <div class="form-grid"><label class="field">资源标题<input v-model="draft.title" :disabled="saving || uploading" required maxlength="200" /></label><label class="field">分类<input v-model="draft.category" :disabled="saving || uploading" maxlength="64" list="resource-categories" /><datalist id="resource-categories"><option v-for="value in categories" :key="value" :value="value" /></datalist></label></div>
        <label class="field">资源简介<textarea v-model="draft.summary" :disabled="saving || uploading" maxlength="1000" rows="3" /></label>
        <label class="field">下载地址<input v-model="draft.fileUrl" :disabled="saving || uploading" maxlength="500" required placeholder="上传文件或填写下载地址" /></label>
        <label class="field">上传文件<input type="file" :disabled="uploading || saving" @change="upload" /></label>
        <p v-if="uploading" role="status">正在上传文件…</p>
        <div class="actions"><button :disabled="saving || uploading">{{ saving ? '保存中…' : draft.id ? '保存修改' : '发布资源' }}</button><button type="button" class="secondary" :disabled="saving || uploading" @click="draft = empty()">{{ draft.id ? '取消编辑' : '清空表单' }}</button></div>
      </form>
      <div class="toolbar"><label>分类 <select v-model="category" :disabled="loading" @change="loadResources(1)"><option value="">全部分类</option><option v-for="value in categories" :key="value" :value="value">{{ value }}</option></select></label><span class="muted">共 {{ total }} 份资源</span><button class="secondary" :disabled="loading" @click="loadResources()">刷新</button></div>
      <p v-if="error" class="error" role="alert">{{ error }} <button class="secondary" @click="loadResources()">重试</button></p>
      <p v-if="loading" role="status">正在加载资源…</p>
      <div v-else-if="!items.length && !error" class="panel empty">当前分类暂无资源。</div>
      <div v-else class="grid resource-grid"><article v-for="item in items" :key="item.id" class="panel stack"><span class="badge">{{ item.category || '未分类' }}</span><h2>{{ item.title }}</h2><p>{{ item.summary || '暂无简介' }}</p><p class="muted">{{ formatDateTime(item.createTime) }} · 下载 {{ item.downloadCount || 0 }} 次</p><div class="actions"><button @click="download(item)">打开资源</button><template v-if="canManageItem(item)"><button class="secondary" :disabled="saving || uploading" @click="draft = { id: item.id, title: item.title, summary: item.summary || '', fileUrl: item.fileUrl, category: item.category || '' }">编辑</button><button class="danger" :disabled="mutating !== null || saving || uploading" @click="remove(item)">删除</button></template></div></article></div>
      <nav class="toolbar" aria-label="资源分页"><button class="secondary" :disabled="page <= 1 || loading" @click="loadResources(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="page >= pages || loading" @click="loadResources(page + 1)">下一页</button></nav>
    </template>
  </section>
</template>

<style scoped>.resource-grid{grid-template-columns:repeat(auto-fit,minmax(min(100%,280px),1fr))}</style>
