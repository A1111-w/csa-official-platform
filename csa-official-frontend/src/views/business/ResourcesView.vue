<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { resourceService, type ResourceItem } from '@/services/resource'
import { fileService } from '@/services/file'
import { formatDateTime, resolveAssetUrl } from '@/lib/format'
import { notify } from '@/lib/notify'
import PublicSkyHeading from '@/views/public/PublicSkyHeading.vue'

const auth = useAuthStore()
const route = useRoute()
const dashboard = computed(() => route.path.startsWith('/dashboard'))
const canView = computed(() => (auth.user?.roleLevel ?? -1) >= 1)
const canManage = computed(() => dashboard.value && (auth.user?.roleLevel ?? -1) >= 3)
const canManageItem = (item: ResourceItem) => canManage.value && ((auth.user?.roleLevel ?? -1) >= 4 || auth.profile?.id === item.uploaderId)
const guides = [
  {
    category: '课程资料', title: '搭一张社团活动介绍页',
    steps: ['新建 index.html，写出活动标题、一段介绍和一组要带的物品。', '用 CSS 设置正文宽度，在手机宽度下检查标题与列表是否完整显示。'],
    outcome: '一张可在本机浏览器打开的活动页。',
    source: 'MDN · Web 入门', href: 'https://developer.mozilla.org/zh-CN/docs/Learn_web_development/Getting_started/Your_first_website',
  },
  {
    category: '比赛题解', title: '用三组输入复盘奇偶判断',
    steps: ['阅读 Product 题面，用任一种熟悉的语言读入两个整数并输出乘积的奇偶性。', '分别测试两个奇数、一奇一偶、两个偶数，给每组输入写下预期输出和判断依据。'],
    outcome: '一份可运行的解答，以及三条能重复验证的测试样例。',
    source: 'AtCoder · ABC086A Product', href: 'https://atcoder.jp/contests/abs/tasks/abc086_a',
  },
  {
    category: '项目模板', title: '做一个可筛选的活动清单',
    steps: ['从 Vue 快速上手进入在线演练场，准备三条带活动名与状态的数据。', '把活动渲染为列表，增加「只看进行中」的切换，确认切换后能恢复全部活动。'],
    outcome: '一个能演示筛选行为的小组件，可继续作为项目起点。',
    source: 'Vue 官方文档 · 快速上手', href: 'https://cn.vuejs.org/guide/quick-start',
  },
  {
    category: '工具软件', title: '追踪一份 README 的修改',
    steps: ['在新的练习目录里初始化 Git，写一段 README 后运行 git status 查看状态。', '暂存 README，再补写一句，分别比较 git diff 和 git diff --staged 的输出。'],
    outcome: '能指出工作区的新改动，以及已经放入暂存区的内容。',
    source: 'Pro Git · 记录每次更新到仓库', href: 'https://git-scm.com/book/zh/v2/Git-基础-记录每次更新到仓库',
  },
]
const initialCategory = computed(() => typeof route.query?.category === 'string' && guides.some(guide => guide.category === route.query.category) ? route.query.category : '')
const guideIndex = ref(0)
const selectedGuide = computed(() => guides[guideIndex.value]!)
const returnToGuide = computed(() => '/resources?category=' + encodeURIComponent(selectedGuide.value.category))
const pageSize = 8
const items = ref<ResourceItem[]>([])
const page = ref(1), pages = ref(1), total = ref(0), category = ref('')
const categoryOptions = ref<string[]>([])
const loading = ref(false), saving = ref(false), uploading = ref(false), error = ref('')
const mutating = ref<number | null>(null)
const empty = () => ({ id: undefined as number | undefined, title: '', summary: '', fileUrl: '', category: '' })
const draft = ref(empty())
const categories = computed(() => [...new Set([...guides.map(guide => guide.category), ...categoryOptions.value, category.value, draft.value.category, ...items.value.map(i => i.category)].filter(Boolean))])
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
watch([canView, initialCategory], ([allowed, requested]) => {
  category.value = requested
  if (requested) guideIndex.value = guides.findIndex(guide => guide.category === requested)
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
  <section :class="dashboard ? 'page stack' : 'atlas-route'">
    <header v-if="dashboard"><p class="eyebrow">RESOURCES</p><h1>资源库</h1><p class="muted">课程资料、比赛题解与项目模板，让每一次积累都能被找到。</p></header>
    <PublicSkyHeading v-else motif="layers" title="资源库" description="查找课程资料、比赛题解、项目模板和工具。" />
    <div :class="dashboard ? 'workspace-flow' : 'page atlas-body stack'">
    <details class="public-guide" :open="!canView"><summary>公开学习指南</summary>
      <section class="public-shelves" aria-labelledby="shelf-title">
      <div><h2 id="shelf-title">选一个入门练习</h2><p>用开放文档独立完成一项小任务。</p><div class="shelf-picker" role="group" aria-label="资料方向">
        <button v-for="(guide, i) in guides" :key="guide.category" class="secondary" :aria-pressed="guideIndex === i" @click="guideIndex = i">{{ guide.category }}</button>
      </div></div>
      <article class="shelf-guide" aria-live="polite"><h3>{{ selectedGuide.title }}</h3>
        <ol><li v-for="step in selectedGuide.steps" :key="step">{{ step }}</li></ol>
        <p><strong>完成标志</strong> {{ selectedGuide.outcome }}</p>
        <a :href="selectedGuide.href" target="_blank" rel="noopener noreferrer">{{ selectedGuide.source }} ↗</a>
      </article>
      </section>
    </details>
    <section v-if="!canView" class="resource-access"><div><h2>资源库需要会员身份</h2>
        <p>{{ auth.user ? '当前账号尚未达到会员等级，请联系协会负责人。' : '登录会员账号后可以查看所选分类的实际目录与下载地址。' }}</p>
      </div><RouterLink v-if="!auth.user" class="button" :to="{ path: '/login', query: { redirect: returnToGuide } }">登录后查看{{ selectedGuide.category }} <span aria-hidden="true">↗</span></RouterLink>
    </section>
    <template v-else>
      <form v-if="canManage" class="panel stack" @submit.prevent="save">
        <h2>{{ draft.id ? '编辑资源' : '发布资源' }}</h2>
        <div class="form-grid"><label class="field">资源标题<input v-model="draft.title" :disabled="saving || uploading" required maxlength="200" /></label><label class="field">分类<input v-model="draft.category" :disabled="saving || uploading" maxlength="64" list="resource-categories" /><datalist id="resource-categories"><option v-for="value in categories" :key="value" :value="value" /></datalist></label></div>
        <label class="field">资源简介<textarea v-model="draft.summary" :disabled="saving || uploading" maxlength="1000" rows="3" /></label>
        <label class="field">下载地址<input v-model="draft.fileUrl" :disabled="saving || uploading" maxlength="500" required placeholder="上传文件或填写下载地址" /></label>
        <label class="field">上传文件<input type="file" :disabled="uploading || saving" @change="upload" /></label>
        <p v-if="uploading" role="status">正在上传文件…</p>
        <details class="resource-proof"><summary>预览资源卡片</summary><article>
          <span class="badge">{{ draft.category || '未分类' }}</span><h3>{{ draft.title || '资源标题待填写' }}</h3><p>{{ draft.summary || '资源简介待填写' }}</p>
        </article></details>
        <div class="actions"><button :disabled="saving || uploading">{{ saving ? '保存中…' : draft.id ? '保存修改' : '发布资源' }}</button><button type="button" class="secondary" :disabled="saving || uploading" @click="draft = empty()">{{ draft.id ? '取消编辑' : '清空表单' }}</button></div>
      </form>
      <div class="toolbar resource-filter"><label>分类 <select v-model="category" :disabled="loading" @change="loadResources(1)"><option value="">全部分类</option><option v-for="value in categories" :key="value" :value="value">{{ value }}</option></select></label><span class="count muted">共 {{ total }} 份资源</span><button class="secondary" :disabled="loading" @click="loadResources()">刷新</button></div>
      <p v-if="error" class="error" role="alert">{{ error }} <button class="secondary" @click="loadResources()">重试</button></p>
      <p v-if="loading" role="status">正在加载资源…</p>
      <div v-else-if="!items.length && !error" class="panel empty">当前分类暂无资源。</div>
      <div v-else class="resource-grid">
        <article v-for="item in items" :key="item.id" class="resource-item">
          <div class="resource-copy">
            <span class="badge">{{ item.category || '未分类' }}</span>
            <h2>{{ item.title }}</h2><p>{{ item.summary || '暂无简介' }}</p>
          </div>
          <div class="resource-meta">
            <span>{{ formatDateTime(item.createTime) }} · 下载 {{ item.downloadCount || 0 }} 次</span>
            <div class="actions">
              <button @click="download(item)">打开资源</button>
              <template v-if="canManageItem(item)">
                <button class="secondary" :disabled="saving || uploading" @click="draft = { id: item.id, title: item.title, summary: item.summary || '', fileUrl: item.fileUrl, category: item.category || '' }">编辑</button>
                <button class="danger" :disabled="mutating !== null || saving || uploading" @click="remove(item)">删除</button>
              </template>
            </div>
          </div>
        </article>
      </div>
      <nav v-if="pages > 1" class="toolbar collection-pagination" aria-label="资源分页"><button class="secondary" :disabled="page <= 1 || loading" @click="loadResources(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button class="secondary" :disabled="page >= pages || loading" @click="loadResources(page + 1)">下一页</button></nav>
    </template>
    </div>
  </section>
</template>

<style scoped>
.workspace-flow { display: contents; }
.public-guide > summary { cursor: pointer; color: var(--accent); font-weight: 650; padding-block: .55rem; }
.public-shelves { display: grid; grid-template-columns: minmax(0,.8fr) minmax(0,1.2fr); gap: 1.4rem 3rem; padding-block: 1.2rem; border-top: 1px solid var(--border); }
.shelf-picker { display: flex; gap: .5rem; flex-wrap: wrap; margin-top: 1rem; }
.shelf-picker button[aria-pressed=true] { border-color: var(--accent); color: var(--accent); }
.shelf-guide { padding: 1.3rem; border-left: 3px solid var(--accent); background: var(--surface); align-self: start; }
.shelf-guide p { margin-top: .35rem; }
.shelf-guide ol { color: var(--muted); padding-left: 1.4em; display: grid; gap: .55rem; }
.shelf-guide p strong { color: var(--text); font-size: .85rem; }
.shelf-guide a { display: inline-block; margin-top: .7rem; }
.resource-access { grid-column: 1/-1; }
.resource-proof summary { color: var(--accent); cursor: pointer; }
.resource-proof article { margin-top: .6rem; padding: 1rem; background: var(--bg); border: 1px solid var(--border); overflow-wrap: anywhere; }
.resource-proof h3 { margin-top: .4rem; }
@media(max-width: 660px) { .public-shelves { grid-template-columns: 1fr; } }
.resource-access { border-block: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1.4rem; padding: clamp(1.5rem, 3.5vw, 3.5rem) 0; }
.resource-access h2 { font-size: clamp(1.35rem, 2.2vw, 2rem); }
.resource-filter { border-bottom: 1px solid var(--border); padding-bottom: 1rem; margin: 0; gap: .85rem; }
.resource-filter label { display: flex; align-items: center; gap: .6rem; }
.resource-filter .count { margin-inline-start: auto; font-size: .9rem; }
.resource-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 24rem), 1fr)); gap: 1rem clamp(1.5rem, 3.5vw, 3rem); }
.resource-item { display: flex; min-width: 0; flex-direction: column; justify-content: space-between; gap: 1.6rem; padding: 1rem .1rem 1.5rem; border-bottom: 1px solid var(--border); overflow-wrap: anywhere; }
.resource-copy h2 { font-size: clamp(1.15rem, 1.6vw, 1.6rem); margin-top: .65rem; }
.resource-copy p { font-size: .95rem; margin-top: .36rem; }
.resource-meta > span { color: var(--muted); font-size: .83rem; }
.resource-meta .actions { margin-top: .75rem; }
@media (max-width: 480px) { .resource-filter .count { margin-inline-start: 0; } }
</style>
