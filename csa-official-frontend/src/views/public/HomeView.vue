<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { excerptText, resolveAssetUrl } from '@/lib/format'
import { useAuthStore } from '@/store/useAuthStore'
import { publicService, type CarouselItem, type ContributorVo, type ContributionRankItem } from '@/services/public'
import MemberCards from './MemberCards.vue'

const auth = useAuthStore()
const loading = ref(true)
const error = ref('')
const about = ref('')
const contributors = ref<ContributorVo[]>([])
const membersLoaded = ref(false)
const carousel = ref<CarouselItem[]>([])
const rank = ref<ContributionRankItem[]>([])
const index = ref(0)
const paused = ref(false)
let alive = true
let timer: ReturnType<typeof setInterval> | undefined
const active = computed(() => carousel.value[index.value % carousel.value.length])
const target = computed(() => {
  const raw = active.value?.targetUrl?.trim()
  if (!raw) return null
  try {
    const url = new URL(raw, window.location.origin)
    if (!['https:', 'http:'].includes(url.protocol)) return null
    if (raw.startsWith('/') && !raw.startsWith('//') && !raw.includes('\\')) return raw
    return /^https?:\/\//i.test(raw) ? url.href : null
  } catch { return null }
})
const leaderCount = computed(() => contributors.value.filter(member => member.roleLevel >= 3).length)
const coreCount = computed(() => contributors.value.filter(member => member.roleLevel >= 2).length)
const entryItems = [
  { title: '资源沉淀', text: '课程资料、工具包与协会模板，随时查阅。', href: '/resources', number: '01' },
  { title: '比赛活动', text: '发现比赛机会，让项目与实践相遇。', href: '/competitions', number: '02' },
  { title: '简历工作区', text: '记录项目经历，提交简历并跟进审核。', href: '/dashboard/resume', number: '03' },
  { title: 'Agent 实验室', text: '从知识问答到证据引用，探索智能协作。', href: '/dashboard/ai', number: '04' },
]
async function load() {
  loading.value = true
  error.value = ''
  const results = await Promise.allSettled([publicService.getAbout(), publicService.getContributors(), publicService.getCarousel(), publicService.getContributionRank(5)])
  if (!alive) return
  about.value = results[0].status === 'fulfilled' ? results[0].value : ''
  contributors.value = results[1].status === 'fulfilled' ? results[1].value : []
  membersLoaded.value = results[1].status === 'fulfilled'
  carousel.value = results[2].status === 'fulfilled' ? results[2].value : []
  rank.value = results[3].status === 'fulfilled' ? results[3].value : []
  index.value = 0
  if (results.some(result => result.status === 'rejected')) error.value = '部分首页数据加载失败，下面展示已获取的内容。'
  loading.value = false
}
function step(direction: number) { if (carousel.value.length) index.value = (index.value + direction + carousel.value.length) % carousel.value.length }
function score(value: number | string) { const n = Number(value); return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 1 }) : String(value) }
onMounted(() => {
  void load()
  timer = setInterval(() => { if (!paused.value && carousel.value.length > 1 && !document.hidden) step(1) }, 6000)
})
onUnmounted(() => { alive = false; clearInterval(timer) })
</script>

<template>
  <div class="page home">
    <section class="hero">
      <div>
        <p class="eyebrow">COMPUTER SCIENCE ASSOCIATION</p>
        <h1>一起探索，<br />把想法变成作品。</h1>
        <p class="hero-copy">CSA 计协协作平台，连接热爱技术的你与伙伴。从知识积累、竞赛实践，到项目展示与协会协作，在这里找到下一步。</p>
        <div class="actions"><RouterLink class="button" :to="auth.user ? '/dashboard' : '/login'">{{ auth.user ? '进入工作台' : '登录平台' }} →</RouterLink><RouterLink class="button secondary" to="/about">认识 CSA</RouterLink></div>
      </div>
      <div class="hero-card">
        <span class="eyebrow">在这里，开始连接</span>
        <RouterLink v-for="item in entryItems" :key="item.href" :to="item.href" class="entry"><span class="entry-no">{{ item.number }}</span><div><h2>{{ item.title }}</h2><p>{{ item.text }}</p></div><span aria-hidden="true">↗</span></RouterLink>
      </div>
    </section>
    <div v-if="error" class="error toolbar" role="alert"><span>{{ error }}</span><button class="secondary" :disabled="loading" @click="load">重新加载</button></div>
    <p v-if="loading" role="status" class="muted">正在加载协会动态…</p>
    <section v-if="active" class="carousel panel" aria-label="协会动态" @mouseenter="paused = true" @mouseleave="paused = false" @focusin="paused = true">
      <img :src="resolveAssetUrl(active.imgUrl)" :alt="active.title" class="carousel-img" />
      <div class="carousel-caption"><span>协会动态</span><h2>{{ active.title }}</h2><a v-if="target" :href="target" :target="target.startsWith('http') ? '_blank' : undefined" rel="noopener noreferrer" class="button secondary">查看详情 ↗</a></div>
      <div v-if="carousel.length > 1" class="carousel-controls"><button class="secondary" aria-label="上一张轮播图" @click="step(-1)">←</button><span>{{ index + 1 }} / {{ carousel.length }}</span><button class="secondary" aria-label="下一张轮播图" @click="step(1)">→</button><button class="secondary" :aria-pressed="paused" @click="paused = !paused">{{ paused ? '播放' : '暂停' }}</button></div>
    </section>
    <section class="about-section panel">
      <div><p class="eyebrow">ABOUT CSA</p><h2>让每一次协作，都留下积累。</h2><p>{{ excerptText(about, 210) || 'CSA 将学习资源、比赛组织、简历维护和内部协作汇聚在同一平台，帮助成员找到伙伴与成长方向。' }}</p><RouterLink to="/about">查看完整介绍 →</RouterLink></div>
      <div class="stats"><div><strong>{{ loading || !membersLoaded ? '—' : contributors.length }}</strong><span>公开成员</span></div><div><strong>{{ loading || !membersLoaded ? '—' : leaderCount }}</strong><span>组织负责人</span></div><div><strong>{{ loading || !membersLoaded ? '—' : coreCount }}</strong><span>核心成员</span></div></div>
    </section>
    <section class="stack">
      <div class="toolbar"><div><p class="eyebrow">CONTRIBUTIONS</p><h2>每一份贡献，都被看见</h2><p class="muted">按已记录贡献积分与贡献次数排序</p></div><RouterLink to="/contributors">查看成员名单 →</RouterLink></div>
      <div v-if="rank.length" class="panel table-wrap"><table><thead><tr><th>排名</th><th>成员</th><th>部门</th><th>贡献次数</th><th>积分</th></tr></thead><tbody><tr v-for="(item, position) in rank" :key="item.userId"><td>{{ position + 1 }}</td><td>{{ item.realName || item.username || '未命名成员' }}</td><td>{{ item.deptName || '协会协作组' }}</td><td>{{ item.contributionCount }}</td><td><strong>{{ score(item.score) }}</strong></td></tr></tbody></table></div>
      <p v-else-if="!loading" class="empty">暂时没有可展示的贡献记录。</p>
    </section>
    <section class="stack"><div class="toolbar"><div><p class="eyebrow">OUR COMMUNITY</p><h2>与同行者一起成长</h2></div><RouterLink to="/contributors">全部成员 →</RouterLink></div><MemberCards :members="contributors.slice(0, 8)" :loading="loading" /></section>
  </div>
</template>

<style scoped>
.home{display:grid;gap:3rem}.hero{display:grid;grid-template-columns:1.1fr 1fr;gap:3.5rem;align-items:center;padding:2.5rem 0}.eyebrow{font-size:.72rem;letter-spacing:.16em;font-weight:700;color:var(--accent)}.hero h1{font-size:clamp(2.4rem,4.2vw,4.2rem);line-height:1.2;letter-spacing:-.04em;margin:1.2rem 0}.hero-copy{max-width:38rem;line-height:1.9;color:var(--muted);margin-bottom:2rem}.hero-card{background:var(--soft);border:1px solid var(--border);padding:1.7rem;border-radius:1.3rem;box-shadow:var(--shadow)}.entry{display:flex;align-items:center;gap:1rem;border-top:1px solid var(--border);padding:1.1rem 0;color:inherit;text-decoration:none}.entry:first-of-type{margin-top:1rem}.entry>div{flex:1}.entry h2{font-size:1.05rem;margin:0 0 .35rem}.entry p{font-size:.85rem;color:var(--muted);margin:0}.entry-no{font-size:.72rem;color:var(--muted)}.carousel{position:relative;min-height:350px;overflow:hidden;background:#1a3931;color:white;padding:0}.carousel-img{width:100%;height:350px;object-fit:cover;opacity:.55}.carousel-caption{position:absolute;left:2rem;bottom:4rem}.carousel-caption h2{font-size:2rem}.carousel-controls{position:absolute;right:1rem;bottom:1rem;display:flex;align-items:center;gap:.7rem}.about-section{display:grid;grid-template-columns:1.2fr 1fr;gap:2rem;align-items:center}.about-section p{line-height:1.8}.stats{display:flex;justify-content:space-around;gap:1rem}.stats>div{display:grid;gap:.5rem}.stats strong{font-size:2rem}.stats span{color:var(--muted);font-size:.9rem}@media(max-width:760px){.hero,.about-section{grid-template-columns:1fr;gap:1.8rem}.hero{padding-top:0}.stats{justify-content:space-between}.home{gap:2rem}.carousel-caption{left:1rem;right:1rem}.carousel-caption h2{font-size:1.5rem}}
</style>
