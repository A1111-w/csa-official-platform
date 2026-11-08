<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { accountService } from '@/services/account'
import { publicService } from '@/services/public'
import { useAuthStore } from '@/store/useAuthStore'
import { sanitizeHtml } from '@/lib/sanitize-html'
import { notify } from '@/lib/notify'
import { downloadJson, errorText } from './helpers'

const auth = useAuthStore()
const router = useRouter()
const canEdit = computed(() => (auth.user?.roleLevel ?? 0) >= 4)
const passwords = reactive({ current: '', next: '', confirm: '' })
const pending = ref('')
const content = ref('')
const loadingAbout = ref(false)
const aboutError = ref('')
const preview = computed(() => sanitizeHtml(content.value || '<p>这里会实时预览协会介绍内容。</p>'))
async function finishSession() {
  auth.logout()
  await router.replace('/login')
}
async function changePassword() {
  if (pending.value) return
  if (!passwords.current || !/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,64}$/.test(passwords.next)) {
    notify.error('新密码需包含字母和数字，长度 8–64 位'); return
  }
  if (passwords.next !== passwords.confirm) { notify.error('两次输入的新密码不一致'); return }
  pending.value = 'password'
  try {
    await accountService.changePassword({ currentPassword: passwords.current, newPassword: passwords.next })
    passwords.current = passwords.next = passwords.confirm = ''
    notify.success('密码已修改，请重新登录')
    await finishSession()
  } catch (e) { notify.error(errorText(e)) }
  finally { pending.value = '' }
}
async function accountAction(action: 'sessions' | 'deactivate' | 'delete' | 'export') {
  if (pending.value) return
  const messages = {
    sessions: '吊销全部会话后，所有设备都需要重新登录。继续吗？',
    deactivate: '停用后账号将不能登录，但数据会按保留策略处理。继续吗？',
    delete: '提交删除申请后账号会立即退出；保留期结束后系统会按策略匿名化账号及关联个人字段。继续吗？',
  }
  if (action !== 'export' && !window.confirm(messages[action])) return
  pending.value = action
  try {
    if (action === 'export') {
      downloadJson(await accountService.exportData(), `csa-personal-data-${new Date().toISOString().slice(0, 10)}.json`)
      notify.success('个人数据已导出')
    } else {
      if (action === 'sessions') await accountService.revokeSessions()
      if (action === 'deactivate') await accountService.deactivate()
      if (action === 'delete') await accountService.requestDeletion()
      notify.success('操作已完成，请重新登录')
      await finishSession()
    }
  } catch (e) { notify.error(errorText(e)) }
  finally { pending.value = '' }
}
async function loadAbout() {
  if (!canEdit.value) return
  loadingAbout.value = true
  aboutError.value = ''
  try { content.value = await publicService.getAbout() }
  catch (e) { aboutError.value = errorText(e) }
  finally { loadingAbout.value = false }
}
async function saveAbout() {
  if (!canEdit.value || pending.value || loadingAbout.value || aboutError.value) return
  pending.value = 'about'
  try { await publicService.updateAbout(content.value); notify.success('协会介绍已更新') }
  catch (e) { notify.error(errorText(e)) }
  finally { pending.value = '' }
}
onMounted(loadAbout)
</script>

<template>
  <section class="page stack">
    <header><h1>账户与公开设置</h1><p class="muted">管理账号安全、个人数据与协会公开介绍。</p></header>
    <div class="grid">
      <form class="panel stack" @submit.prevent="changePassword">
        <h2>修改密码</h2><p class="muted">修改密码会立即吊销旧会话。</p>
        <label class="field">当前密码<input v-model="passwords.current" type="password" autocomplete="current-password" required :disabled="!!pending" /></label>
        <label class="field">新密码<input v-model="passwords.next" type="password" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="!!pending" /></label>
        <p class="muted">8–64 位，只包含字母和数字，且两者都必须有。</p>
        <label class="field">确认新密码<input v-model="passwords.confirm" type="password" autocomplete="new-password" required :disabled="!!pending" /></label>
        <button :disabled="!!pending">{{ pending === 'password' ? '正在修改…' : '修改密码' }}</button>
      </form>
      <div class="stack">
        <section class="panel"><h2>个人数据</h2><p>下载当前账号的资料、简历、文件元数据和安全事件摘要。密码、Token 和验证码不会被导出。</p><button class="secondary" :disabled="!!pending" @click="accountAction('export')">导出个人数据</button></section>
        <section class="panel"><h2>会话管理</h2><p>发现异常登录时，可以让所有设备重新认证。</p><button class="secondary" :disabled="!!pending" @click="accountAction('sessions')">吊销全部会话</button></section>
      </div>
    </div>
    <section class="panel"><h2>停用或删除账号</h2><p>两种操作都会立即退出。删除申请会进入数据保留期，期满后后台任务匿名化账号及关联个人字段。</p><div class="actions"><button class="secondary" :disabled="!!pending" @click="accountAction('deactivate')">停用账号</button><button class="danger" :disabled="!!pending" @click="accountAction('delete')">提交删除申请</button></div></section>
    <section v-if="canEdit" class="panel stack">
      <div class="toolbar"><h2>协会介绍内容管理</h2><button class="secondary" :disabled="loadingAbout || !!pending" @click="loadAbout">重新加载</button></div>
      <p class="muted">支持基础 HTML。保存后同步更新首页和关于页；危险标签和事件属性会被清洗。</p>
      <p v-if="aboutError" class="error" role="alert">{{ aboutError }}，请重新加载后编辑。</p>
      <p v-if="loadingAbout" role="status">正在加载介绍…</p>
      <form v-else @submit.prevent="saveAbout">
        <div class="grid"><label class="field">编辑内容<textarea v-model="content" rows="16" :disabled="!!pending || !!aboutError" /></label><div><h3>页面预览</h3><div class="rich-text" v-html="preview" /></div></div>
        <button :disabled="!!pending || !!aboutError">{{ pending === 'about' ? '保存中…' : '保存修改' }}</button>
      </form>
    </section>
    <p v-else class="panel muted">协会介绍编辑器仅向会长及以上开放。</p>
  </section>
</template>
