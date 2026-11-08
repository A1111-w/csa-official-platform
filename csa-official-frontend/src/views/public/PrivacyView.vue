<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { publicService, type PrivacyNotice } from '@/services/public'
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
<template><div class="page stack">
  <header><h1>隐私说明</h1><p class="muted">了解平台收集哪些信息、使用目的、保存期限，以及你可以行使的权利。</p><p v-if="notice">当前版本：{{ notice.policyVersion }}</p></header>
  <p v-if="loading" role="status">正在加载隐私说明…</p>
  <div v-if="error" role="alert" class="error toolbar">{{ error }}<button class="secondary" :disabled="loading" @click="load">重新加载</button></div>
  <div v-if="notice" class="privacy-grid">
    <section class="panel"><h2>收集的信息</h2><ul><li v-for="field in notice.collectedFields" :key="field">{{ fields[field] || field }}</li></ul></section>
    <section class="panel"><h2>使用目的</h2><dl><template v-for="(value, key) in notice.purposes" :key="key"><dt>{{ purposes[key] || key }}</dt><dd>{{ value }}</dd></template></dl></section>
    <section class="panel"><h2>保存期限</h2><dl><template v-for="(value, key) in notice.retention" :key="key"><dt>{{ retention[key] || key }}</dt><dd>{{ value }}</dd></template></dl></section>
    <section class="panel"><h2>你的权利</h2><ul><li v-for="right in notice.userRights" :key="right">{{ rights[right] || right }}</li></ul></section>
    <section class="panel privacy-contact"><h2>联系与操作</h2><p>隐私相关问题请联系：{{ notice.contactEmail }}。登录后可以在账户设置中导出数据、吊销会话、停用账号或提交删除申请。</p><RouterLink class="button secondary" to="/dashboard/settings">打开账户设置</RouterLink></section>
  </div>
</div></template>
<style scoped>.privacy-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}.privacy-contact{grid-column:1/-1}li{margin:.6rem 0}dt{font-weight:600;margin-top:1rem}dd{margin:.4rem 0;line-height:1.8;color:var(--muted)}@media(max-width:650px){.privacy-grid{grid-template-columns:1fr}}</style>
