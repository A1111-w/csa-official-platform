<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { publicService, type PrivacyNotice } from '@/services/public'
import PublicSkyHeading from './PublicSkyHeading.vue'
const notice = ref<PrivacyNotice | null>(null)
const loading = ref(true)
const error = ref('')
let alive = true
const fields: Record<string, string> = { username: '登录账号', realName: '真实姓名', email: '邮箱', studentId: '学号', college: '学院', className: '班级', phone: '手机号', contact: '其他联系方式', avatar: '头像', resume: '简历内容和仓库地址', uploadedFiles: '上传文件元数据', securityEvents: '安全与管理事件' }
const rights: Record<string, string> = { access: '查阅个人信息', export: '导出个人数据', correction: '申请更正信息', sessionRevocation: '吊销全部会话', deactivation: '停用账号', deletionRequest: '提交删除申请' }
const purposes: Record<string, string> = { account: '账号与权限', operations: '协会业务', security: '安全保障' }
const retention: Record<string, string> = { activeAccount: '正常账号', deletionRequest: '删除申请', audit: '审计记录', backup: '备份副本' }
async function load() {
  loading.value = true; error.value = ''
  try { const data = await publicService.getPrivacyNotice(); if (alive) notice.value = data }
  catch (reason) { if (alive) { notice.value = null; error.value = reason instanceof Error ? reason.message : '隐私说明加载失败' } }
  finally { if (alive) loading.value = false }
}
onMounted(load)
onUnmounted(() => { alive = false })
</script>
<template><div class="atlas-route">
  <PublicSkyHeading motif="shield" title="隐私说明" description="了解平台收集哪些信息、使用目的、保存期限，以及你可以行使的权利。">
    <p v-if="notice">当前版本：{{ notice.policyVersion }}</p>
  </PublicSkyHeading>
  <div class="page atlas-body">
    <p v-if="loading" role="status">正在加载隐私说明…</p>
    <div v-if="error" role="alert" class="error toolbar">{{ error }}<button class="secondary" :disabled="loading" @click="load">重新加载</button></div>
    <div v-if="notice" class="policy">
      <nav class="policy-nav" aria-label="隐私说明目录">
        <a href="#policy-collect">收集的信息</a><a href="#policy-use">使用目的</a>
        <a href="#policy-keep">保存期限</a><a href="#policy-rights">你的权利</a><a href="#policy-contact">联系与操作</a>
      </nav>
      <div class="policy-text">
        <section id="policy-collect"><h2>收集的信息</h2><ul class="policy-tags"><li v-for="field in notice.collectedFields" :key="field">{{ fields[field] || field }}</li></ul><p v-if="!notice.collectedFields.length">当前政策未列出具体信息项。</p></section>
        <section id="policy-use"><h2>使用目的</h2><dl v-if="Object.keys(notice.purposes).length"><template v-for="(value, key) in notice.purposes" :key="key"><dt>{{ purposes[key] || key }}</dt><dd>{{ value }}</dd></template></dl><p v-else>当前政策未列出具体用途。</p></section>
        <section id="policy-keep"><h2>保存期限</h2><dl v-if="Object.keys(notice.retention).length"><template v-for="(value, key) in notice.retention" :key="key"><dt>{{ retention[key] || key }}</dt><dd>{{ value }}</dd></template></dl><p v-else>当前政策未列出具体期限。</p></section>
        <section id="policy-rights"><h2>你的权利</h2><ul class="policy-tags"><li v-for="right in notice.userRights" :key="right">{{ rights[right] || right }}</li></ul><p v-if="!notice.userRights.length">当前政策未列出具体权利项。</p></section>
        <section id="policy-contact"><h2>联系与操作</h2><p>隐私相关问题请联系：{{ notice.contactEmail }}。登录后可以在账户设置中导出数据、吊销会话、停用账号或提交删除申请。</p><RouterLink class="button secondary" to="/dashboard/settings">打开账户设置</RouterLink></section>
      </div>
    </div>
  </div>
</div></template>
<style scoped>
.policy { display: grid; grid-template-columns: minmax(11rem, .44fr) minmax(0, 1.56fr); gap: clamp(2rem, 6vw, 6rem); align-items: start; }
.policy-nav { display: grid; align-content: start; border-block: 1px solid var(--border); padding: .6rem 0; position: sticky; top: 6rem; }
.policy-nav a { padding: .6rem .25rem; font-size: .95rem; }
.policy-text section { border-block-start: 1px solid var(--border); padding: 1.3rem 0 1.8rem; scroll-margin-top: 6.5rem; overflow-wrap: anywhere; }
.policy-text h2 { margin-bottom: .8rem; font-size: clamp(1.25rem, 2vw, 1.7rem); }
.policy-text p, .policy-text dd { max-width: 48em; color: var(--muted); }
.policy-text dl { display: grid; grid-template-columns: minmax(6rem,.35fr) minmax(0,1fr); row-gap: .8rem; column-gap: 1.2rem; }
.policy-text dd { margin: 0; }
.policy-tags { list-style: none; display: flex; flex-wrap: wrap; gap: .45rem; margin: 0; padding: 0; }
.policy-tags li { font-size: .92rem; color: var(--text); background: var(--soft); border-radius: .22rem; padding: .2rem .65rem; }
.policy-text .button { margin-top: .8rem; }
@media (max-width: 680px) {
  .policy { grid-template-columns: minmax(0,1fr); gap: 1.5rem; }
  .policy-nav { display: flex; flex-wrap: wrap; position: static; gap: .25rem 1rem; }
  .policy-nav a { padding: .35rem 0; }
  .policy-text dl { grid-template-columns: minmax(0,1fr); row-gap: .25rem; }
  .policy-text dd:not(:last-child) { margin-bottom: .8rem; }
}
</style>
