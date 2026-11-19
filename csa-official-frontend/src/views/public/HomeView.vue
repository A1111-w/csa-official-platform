<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { excerptText, resolveAssetUrl } from '@/lib/format'
import { useAuthStore } from '@/store/useAuthStore'
import { publicService, type CarouselItem, type ContributorVo, type ContributionRankItem } from '@/services/public'
import HomeHero from './HomeHero.vue'
import HomeStarMap from './HomeStarMap.vue'
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
const pointerInCarousel = ref(false)
const focusInCarousel = ref(false)
const reducedMotion = ref(false)
let alive = true
let timer: ReturnType<typeof setInterval> | undefined
let motionQuery: MediaQueryList | undefined
const active = computed(() => carousel.value[index.value])
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

async function load() {
  loading.value = true
  error.value = ''
  pointerInCarousel.value = false
  focusInCarousel.value = false
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
function step(direction: number) {
  const length = carousel.value.length
  if (length) index.value = (index.value + direction + length) % length
}
function score(value: number | string) {
  const points = Number(value)
  return Number.isFinite(points) ? points.toLocaleString('zh-CN', { maximumFractionDigits: 1 }) : String(value)
}
function updateMotion() {
  reducedMotion.value = motionQuery?.matches ?? false
}
function leaveCarousel(event: FocusEvent) {
  const carouselElement = event.currentTarget
  const next = event.relatedTarget
  focusInCarousel.value = carouselElement instanceof HTMLElement && next instanceof Node && carouselElement.contains(next)
}
onMounted(() => {
  void load()
  motionQuery = window.matchMedia?.('(prefers-reduced-motion: reduce)')
  updateMotion()
  motionQuery?.addEventListener('change', updateMotion)
  timer = setInterval(() => {
    if (!paused.value && !pointerInCarousel.value && !focusInCarousel.value && !reducedMotion.value && carousel.value.length > 1 && !document.hidden) step(1)
  }, 6000)
})
onUnmounted(() => {
  alive = false
  clearInterval(timer)
  motionQuery?.removeEventListener('change', updateMotion)
})
</script>

<template>
  <div class="home-view">
    <HomeHero :signed-in="!!auth.user" />

    <div class="page home-content">
      <HomeStarMap />

      <section id="home-activity" class="home-section" aria-labelledby="home-activity-title">
        <div class="section-heading">
          <h2 id="home-activity-title">协会动态</h2>
          <RouterLink to="/competitions">浏览竞赛 <span aria-hidden="true">↗</span></RouterLink>
        </div>
        <div v-if="error" class="home-error error" role="alert">
          <span>{{ error }}</span>
          <button type="button" class="secondary" :disabled="loading" @click="load">重新加载</button>
        </div>
        <p v-if="loading" role="status" class="muted">正在加载协会动态…</p>
        <div v-else-if="active" class="carousel"
          @mouseenter="pointerInCarousel = true" @mouseleave="pointerInCarousel = false"
          @focusin="focusInCarousel = true" @focusout="leaveCarousel"
        >
          <img :src="resolveAssetUrl(active.imgUrl)" :alt="active.title" class="carousel-img" loading="lazy" />
          <div class="carousel-caption">
            <h3>{{ active.title }}</h3>
            <a v-if="target" :href="target" :target="target.startsWith('http') ? '_blank' : undefined" rel="noopener noreferrer" class="button secondary">查看详情 <span aria-hidden="true">↗</span></a>
          </div>
          <div v-if="carousel.length > 1" class="carousel-controls">
            <button type="button" class="secondary" aria-label="上一张轮播图" @click="step(-1)">←</button>
            <span>{{ index + 1 }} / {{ carousel.length }}</span>
            <button type="button" class="secondary" aria-label="下一张轮播图" @click="step(1)">→</button>
            <button v-if="!reducedMotion" type="button" class="secondary" :aria-pressed="paused" @click="paused = !paused">{{ paused ? '播放' : '暂停' }}</button>
          </div>
        </div>
        <p v-else class="empty">暂时没有可展示的协会动态。</p>
      </section>

      <section class="home-overview" aria-labelledby="home-overview-title">
        <div class="overview-copy">
          <h2 id="home-overview-title">认识计算机协会</h2>
          <p>{{ excerptText(about, 210) || '一起学技术、参加比赛、做项目。协会成员可以在这里分享资料、寻找伙伴和记录实践经历。' }}</p>
          <RouterLink to="/about">查看协会介绍 <span aria-hidden="true">↗</span></RouterLink>
        </div>
        <dl class="overview-stats">
          <div><dt>公开成员</dt><dd>{{ loading || !membersLoaded ? '—' : contributors.length }}</dd></div>
          <div><dt>组织负责人</dt><dd>{{ loading || !membersLoaded ? '—' : leaderCount }}</dd></div>
          <div><dt>核心成员</dt><dd>{{ loading || !membersLoaded ? '—' : coreCount }}</dd></div>
        </dl>
      </section>

      <section class="home-section" aria-labelledby="home-ranking-title">
        <div class="section-heading">
          <div><h2 id="home-ranking-title">贡献排行榜</h2><p>按已记录贡献积分与贡献次数排序</p></div>
          <RouterLink to="/contributors">查看成员名单 <span aria-hidden="true">↗</span></RouterLink>
        </div>
        <div v-if="rank.length" class="table-wrap rank-table" role="region" aria-label="贡献积分表格" tabindex="0">
          <table aria-labelledby="home-ranking-title">
            <thead><tr><th scope="col">排名</th><th scope="col">成员</th><th scope="col">部门</th><th scope="col">贡献次数</th><th scope="col">积分</th></tr></thead>
            <tbody>
              <tr v-for="(item, position) in rank" :key="item.userId">
                <td><span class="rank-number">{{ position + 1 }}</span></td><td>{{ item.realName || item.username || '未命名成员' }}</td><td>{{ item.deptName || '协会协作组' }}</td><td>{{ item.contributionCount }}</td><td><strong>{{ score(item.score) }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else-if="!loading" class="empty">暂时没有可展示的贡献记录。</p>
      </section>

      <section class="home-section" aria-labelledby="home-members-title">
        <div class="section-heading">
          <h2 id="home-members-title">公开成员</h2>
          <RouterLink to="/contributors">全部成员 <span aria-hidden="true">↗</span></RouterLink>
        </div>
        <MemberCards :members="contributors.slice(0, 8)" :loading="loading" />
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  background: var(--bg);
  color: var(--text);
}
.home-content {
  display: grid;
  align-items: start;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  width: min(var(--atlas-width), calc(100% - 2 * var(--atlas-gutter)));
  max-width: none;
  gap: clamp(2.6rem, 4.3vw, 4.8rem) clamp(2.2rem, 5vw, 5rem);
  padding: .5rem 0 clamp(3.5rem, 7vw, 7.5rem);
}
.home-content > :first-child { grid-column: 1 / -1; }
.home-section { min-width: 0; scroll-margin-top: 115px; }
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  flex-wrap: wrap;
  gap: .6rem 1.4rem;
  padding-block-end: .85rem;
  border-bottom: 1px solid var(--border);
  margin-block-end: 1rem;
}
.section-heading h2, .home-overview h2 { font-size: clamp(1.4rem, 2.2vw, 2.1rem); font-weight: 550; letter-spacing: -.03em; }
.section-heading p { margin-block-start: .2rem; font-size: .875rem; }
.section-heading a, .overview-copy a { font-weight: 700; }
.home-error { display: flex; align-items: center; flex-wrap: wrap; gap: .8rem; margin-block-end: 1rem; }
.carousel { position: relative; display: flex; flex-direction: column; justify-content: end; overflow: hidden; isolation: isolate; min-height: clamp(245px, 25vw, 350px); background: #102432; color: #fff; border-radius: 3px; }
.carousel-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; z-index: -2; }
.carousel::before { position: absolute; inset: 0; z-index: -1; background: linear-gradient(0deg, #071624f5, #091c2890 39%, #101c2935 77%); content: ''; }
.carousel-caption { display: grid; justify-items: start; gap: .65rem; padding: clamp(1rem, 3vw, 2rem); min-width: 0; }
.carousel-caption h3 { font-size: clamp(1.45rem, 4.4vw, 2.85rem); overflow-wrap: anywhere; }
.carousel-controls { display: flex; align-items: center; gap: .64rem; flex-wrap: wrap; padding: 0 clamp(1rem, 3vw, 2rem) 1rem; font-size: .88rem; }
.carousel-controls button { min-height: 42px; }
.home-overview { display: grid; align-content: start; gap: 2.4rem; }
.overview-copy h2 { border-bottom: 1px solid var(--border); padding-bottom: .85rem; }
.overview-copy p { margin-block: 1.2rem .9rem; max-width: 42em; overflow-wrap: anywhere; }
.overview-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: .75rem; margin: 0; }
.overview-stats div { display: grid; min-width: 0; gap: .15rem; border-block-start: 1px solid var(--border); padding-block-start: .65rem; }
.overview-stats dt { color: var(--muted); font-size: .8rem; }
.overview-stats dd { grid-row: 1; margin: 0; color: var(--text); font-weight: 750; font-variant-numeric: tabular-nums; font-size: clamp(1.45rem, 4vw, 2.5rem); }
.rank-table { background: var(--bg); }
.rank-table table { min-width: 470px; }
.rank-table tbody tr:last-child td { border-bottom: none; }
.rank-table tbody tr:first-child { background: var(--soft); }
.rank-number { display: inline-grid; place-items: center; width: 1.8rem; height: 1.8rem; border: 1px solid var(--border); border-radius: 50%; color: var(--accent); font-weight: 750; }
.rank-table tbody tr:first-child .rank-number { background: var(--accent); color: var(--surface); border-color: var(--accent); }
.home-section :deep(.member-grid) { grid-template-columns: 1fr; gap: 0; }
.home-section :deep(.member-card) { padding: .85rem 0; background: transparent; border: 0; border-radius: 0; border-bottom: 1px solid var(--border); }
.home-section :deep(.member-card p) { display: inline; margin-inline-end: .6rem; }
.home-section :deep(.member-card .avatar) { width: 42px; height: 42px; }

@media (max-width: 860px) {
  .home-content { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 680px) {
  .home-overview { grid-template-columns: 1fr; }
  .carousel-controls button { min-width: 44px; min-height: 44px; }
}
</style>
