<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getRoleLabel } from '@/lib/access'
import { publicService, type ContributorVo, type ContributionRankItem } from '@/services/public'
import MemberCards from './MemberCards.vue'
import PublicSkyHeading from './PublicSkyHeading.vue'

const members = ref<ContributorVo[]>([])
const ranking = ref<ContributionRankItem[]>([])
const contributionByMember = computed(() => new Map(ranking.value.map((entry, index) => [
  entry.userId, `公开榜第 ${index + 1} 名，累计 ${entry.score} 分，已记录 ${entry.contributionCount} 次贡献。`,
])))
const loading = ref(true)
const rankLoading = ref(true)
const error = ref('')
const rankError = ref('')
const search = ref('')
const department = ref('')
const groups = computed(() => {
  const counts = new Map<string, number>()
  for (const member of members.value) {
    const name = member.deptName?.trim()
    if (name) counts.set(name, (counts.get(name) || 0) + 1)
  }
  return [...counts].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'zh-CN'))
})
const filtered = computed(() => {
  const text = search.value.trim().toLocaleLowerCase()
  return members.value.filter(member => (!department.value || member.deptName?.trim() === department.value)
    && [member.realName, member.title, member.deptName, getRoleLabel(member.roleLevel)].some(value => value?.toLocaleLowerCase().includes(text)))
})
function clearFilters() {
  search.value = ''
  department.value = ''
}
async function loadMembers() {
  loading.value = true
  error.value = ''
  try { members.value = await publicService.getContributors() }
  catch (reason) { error.value = reason instanceof Error ? reason.message : '成员名单加载失败' }
  finally { loading.value = false }
}
async function loadRanking() {
  rankLoading.value = true
  rankError.value = ''
  try { ranking.value = await publicService.getContributionRank(10) }
  catch (reason) { rankError.value = reason instanceof Error ? reason.message : '贡献榜加载失败' }
  finally { rankLoading.value = false }
}
onMounted(() => { void loadMembers(); void loadRanking() })
</script>

<template>
  <div class="atlas-route">
    <PublicSkyHeading motif="network" title="成员与贡献者" description="从部门与分工找到协作伙伴，查看协会已记录的贡献。" />
    <div class="page atlas-body people-spread">
      <section aria-labelledby="directory-title">
        <header class="section-rule"><h2 id="directory-title">成员名录</h2><span v-if="!error && !loading">共 {{ members.length }} 位公开成员</span></header>
        <div v-if="error" class="error toolbar" role="alert">{{ error }}<button class="secondary" @click="loadMembers">重新加载成员</button></div>
        <p v-else-if="loading" role="status">正在加载成员信息…</p>
        <template v-else-if="members.length">
          <label class="member-search">查找成员<input v-model="search" type="search" placeholder="输入姓名、分工或部门" /></label>
          <fieldset v-if="groups.length" class="department-filter">
            <legend>按部门浏览</legend>
            <button type="button" :aria-pressed="!department" @click="department = ''">全部 <small>{{ members.length }}</small></button>
            <button v-for="[name, count] in groups" :key="name" type="button" :aria-pressed="department === name" @click="department = name">{{ name }} <small>{{ count }}</small></button>
          </fieldset>
          <p v-if="search || department" class="result-count" role="status">找到 {{ filtered.length }} 位成员 <button class="button-link" @click="clearFilters">清除筛选</button></p>
          <MemberCards v-if="filtered.length" :members="filtered">
            <template #contribution="{ member }">
              <details v-if="!rankError && contributionByMember.has(member.id)" class="member-contribution">
                <summary :aria-label="(member.realName || '此成员') + '的贡献概况'">贡献概况</summary>
                <p>{{ contributionByMember.get(member.id) }}</p>
              </details>
            </template>
          </MemberCards>
          <p v-else class="empty">没有符合当前条件的成员，可以改用姓名或分工查找。</p>
        </template>
        <p v-else class="empty">暂时还没有公开的核心成员信息。</p>
      </section>

      <section class="leaderboard" aria-labelledby="rank-heading">
        <header class="section-rule"><h2 id="rank-heading">贡献积分榜</h2><span>积分前十</span></header>
        <p class="rank-intro">按已记录的贡献积分排名，旁列个人贡献次数。</p>
        <p v-if="rankLoading" role="status">正在加载贡献积分…</p>
        <div v-else-if="rankError" class="error toolbar" role="alert">{{ rankError }}<button class="secondary" @click="loadRanking">重新加载积分榜</button></div>
        <ol v-else-if="ranking.length">
          <li v-for="(entry, i) in ranking" :key="entry.userId">
            <span class="place" aria-hidden="true">{{ String(i + 1).padStart(2, '0') }}</span>
            <div><strong>{{ entry.realName || entry.username || '未公开姓名' }}</strong><p>{{ entry.deptName || '部门未公开' }} · {{ entry.contributionCount }} 次贡献</p></div>
            <div class="points">{{ entry.score }} <small>分</small></div>
          </li>
        </ol>
        <p v-else class="empty">暂时还没有可展示的贡献记录。</p>
      </section>

      <section class="participate" aria-labelledby="participate-title">
        <h2 id="participate-title">开始参与协作</h2>
        <RouterLink to="/about"><strong>了解协会分工 <span>↗</span></strong><small>根据自己的兴趣选择参与方向</small></RouterLink>
        <RouterLink to="/competitions"><strong>寻找近期活动 <span>↗</span></strong><small>查看比赛安排与活动详情</small></RouterLink>
        <RouterLink to="/resources"><strong>准备学习资料 <span>↗</span></strong><small>按课程、题解或项目查找资源</small></RouterLink>
      </section>
    </div>
  </div>
</template>

<style scoped>
.people-spread { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(18rem, .82fr); align-items: start; gap: clamp(2rem, 4vw, 4rem); }
.people-spread section { min-width: 0; }
.section-rule span, .department-filter legend, .result-count { font-size: .84rem; color: var(--muted); }
.member-search { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: 1rem; margin: 1.25rem 0 1rem; }
.member-search input { margin: 0; }
.department-filter { display: flex; flex-wrap: wrap; gap: .45rem; }
.department-filter legend { margin-bottom: .4rem; }
.department-filter button { color: var(--text); background: transparent; border: 1px solid var(--border); border-radius: 2rem; min-height: 2.55rem; padding: .35rem .85rem; }
.department-filter small { color: inherit; opacity: .86; font-variant-numeric: tabular-nums; }
.department-filter button[aria-pressed=true] { color: var(--accent); border-color: var(--accent); background: var(--soft); }
.people-spread :deep(.member-grid) { margin-top: .85rem; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); column-gap: 1.2rem; }
.result-count { margin-top: .7rem; }
.people-spread :deep(.member-card) { align-items: flex-start; }
.member-contribution { margin-top: .25rem; padding: 0; border: 0; background: transparent; }
.member-contribution summary { color: var(--accent); font-size: .83rem; width: fit-content; cursor: pointer; padding-block: .32rem; }
.member-contribution p { margin-block-start: .3rem; color: var(--text); font-size: .86rem; }
.leaderboard { border: 1px solid var(--border); border-top: 2px solid var(--metal); padding: clamp(1rem, 2.2vw, 2rem); background: linear-gradient(125deg, color-mix(in srgb, var(--metal) 6%, var(--surface)), var(--surface) 65%); }
.leaderboard ol { margin: .9rem 0 0; padding: 0; list-style: none; }
.leaderboard li { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: .8rem; align-items: center; padding: .8rem 0; border-bottom: 1px solid var(--border); overflow-wrap: anywhere; }
.leaderboard li:last-child { border-bottom: 0; }
.leaderboard li p, .rank-intro { font-size: .86rem; }
.rank-intro { margin-top: .8rem; }
.place { font: italic 1.6rem/1 Georgia, serif; color: var(--muted); }
.leaderboard li:first-child .place, .points { color: var(--metal); }
.points { font-weight: 700; font-variant-numeric: tabular-nums; }
.participate { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border-block: 1px solid var(--border); align-items: stretch; }
.participate h2, .participate a { padding: 1.35rem .9rem; }
.participate h2 { align-self: center; padding-left: 0; font-size: clamp(1.2rem, 2vw, 1.65rem); }
.participate a { border-left: 1px solid var(--border); display: grid; align-content: center; gap: .25rem; }
.participate a span { margin-inline-start: .3rem; }
@media(max-width: 850px) { .people-spread { grid-template-columns: 1fr; } .participate { grid-template-columns: repeat(3, minmax(0, 1fr)); } .participate h2 { grid-column: 1/-1; } }
@media(max-width: 580px) { .participate { grid-template-columns: 1fr; } .participate a { border-left: 0; border-top: 1px solid var(--border); padding-inline: 0; } }
</style>
