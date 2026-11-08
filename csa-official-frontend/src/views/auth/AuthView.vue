<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authService } from '@/services/auth'
import { publicService } from '@/services/public'
import { getSafeRedirect } from '@/lib/navigation'
import { notify } from '@/lib/notify'
import { useAuthStore } from '@/store/useAuthStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mode = computed(() => route.path === '/register' ? 'register' : route.path === '/forgot-password' ? 'forgot' : route.path === '/reset-password' ? 'reset' : 'login')
const titles = { login: '欢迎回来', register: '加入 CSA', forgot: '找回密码', reset: '设置新密码' }
const submits = { login: '立即登录', register: '立即注册', forgot: '发送重置验证码', reset: '确认重置密码' }
const form = reactive({ username: '', password: '', confirmPassword: '', email: '', code: '', realName: '', studentId: '', college: '', className: '', inviteCode: '', merchantNo: '', privacyConsent: false })
const registerType = ref<'invite' | 'guest'>('invite')
const busy = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')
const errors = reactive<Record<string, string>>({})
const showPassword = ref(false)
const showConfirm = ref(false)
const countdown = ref(0)
const privacyLoading = ref(false)
const privacyVersion = ref<string | null>(null)
const privacyError = ref('')
let generation = 0
let countdownTimer: ReturnType<typeof setInterval> | undefined
let alive = true

function clearErrors() { for (const key of Object.keys(errors)) delete errors[key] }
function validEmail() { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()) }
async function loadPrivacy() {
  const current = generation
  privacyLoading.value = true; privacyError.value = ''; privacyVersion.value = null
  try {
    const notice = await publicService.getPrivacyNotice()
    if (alive && current === generation) {
      if (!notice.policyVersion?.trim()) throw new Error('隐私政策版本为空')
      privacyVersion.value = notice.policyVersion
    }
  } catch (reason) { if (alive && current === generation) privacyError.value = reason instanceof Error ? reason.message : '隐私政策暂时无法加载' }
  finally { if (alive && current === generation) privacyLoading.value = false }
}
watch(mode, () => {
  generation++
  busy.value = false; sending.value = false; error.value = ''; success.value = ''; clearErrors()
  form.password = ''; form.confirmPassword = ''; form.code = ''; form.privacyConsent = false
  showPassword.value = false; showConfirm.value = false
  countdown.value = 0; clearInterval(countdownTimer)
  if (mode.value === 'register') void loadPrivacy()
}, { immediate: true })

function validate() {
  clearErrors()
  if (mode.value === 'login') {
    if (!form.username.trim()) errors.username = '请输入用户名'
    if (!form.password) errors.password = '请输入密码'
    return !Object.keys(errors).length
  }
  if (!validEmail()) errors.email = '请输入有效邮箱'
  if (mode.value === 'forgot') return !Object.keys(errors).length
  if (!/^\d{6}$/.test(form.code)) errors.code = '验证码必须是 6 位数字'
  if (mode.value === 'register') {
    if (!/^[A-Za-z0-9_]{4,20}$/.test(form.username)) errors.username = '用户名需为 4–20 位字母、数字或下划线'
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/.test(form.password)) errors.password = '密码需包含字母和数字，长度 6–20 位'
    if (!form.privacyConsent) errors.privacy = '请先阅读并同意隐私政策'
    if (!privacyVersion.value) errors.privacy = '隐私政策版本尚未加载，请重试'
  } else if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,64}$/.test(form.password)) errors.password = '密码需包含字母和数字，长度 8–64 位'
  if (form.password !== form.confirmPassword) errors.confirmPassword = '两次输入的密码不一致'
  return !Object.keys(errors).length
}
async function sendCode() {
  if (sending.value || countdown.value > 0 || busy.value) return
  if (!validEmail()) { errors.email = '请先输入有效的邮箱地址'; return }
  delete errors.email
  const current = generation
  sending.value = true; error.value = ''; success.value = ''
  try {
    await authService.sendCode(form.email.trim())
    if (!alive || current !== generation) return
    success.value = '验证码已发送，请查收邮件'; notify.success(success.value)
    countdown.value = 60
    clearInterval(countdownTimer)
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(countdownTimer) }, 1000)
  } catch (reason) { if (alive && current === generation) error.value = reason instanceof Error ? reason.message : '验证码发送失败' }
  finally { if (alive && current === generation) sending.value = false }
}
async function submit() {
  if (busy.value || !validate()) return
  const current = generation
  const submittedMode = mode.value
  busy.value = true; error.value = ''; success.value = ''
  try {
    if (submittedMode === 'login') {
      const user = await authService.login({ username: form.username.trim(), password: form.password })
      if (!alive || current !== generation) return
      auth.setLogin(user)
      notify.success(`欢迎回来，${user.username}`)
      await router.push(getSafeRedirect(typeof route.query.redirect === 'string' ? route.query.redirect : undefined))
    } else if (submittedMode === 'register') {
      await authService.register({
        username: form.username, password: form.password, email: form.email.trim(), code: form.code,
        realName: form.realName, studentId: form.studentId, college: form.college, className: form.className,
        inviteCode: registerType.value === 'invite' ? form.inviteCode : undefined,
        merchantNo: registerType.value === 'guest' ? form.merchantNo : undefined,
        privacyConsentVersion: privacyVersion.value!,
      })
      if (!alive || current !== generation) return
      notify.success('注册成功，请登录'); await router.push('/login')
    } else if (submittedMode === 'forgot') {
      await authService.forgotPassword({ email: form.email.trim() })
      if (!alive || current !== generation) return
      success.value = '如果账号存在，重置验证码已发送。请查收邮件后设置新密码。'
      notify.success(success.value)
    } else {
      await authService.resetPassword({ email: form.email.trim(), code: form.code, newPassword: form.password })
      if (!alive || current !== generation) return
      notify.success('密码已重置，请重新登录'); await router.push('/login')
    }
  } catch (reason) {
    if (!alive || current !== generation) return
    error.value = reason instanceof Error ? reason.message : '请求失败，请稍后重试'
    if (submittedMode === 'login' && /账号|密码/.test(error.value)) { errors.password = '账号或密码错误，请检查后重试'; form.password = '' }
  } finally { if (alive && current === generation) busy.value = false }
}
onUnmounted(() => { alive = false; generation++; clearInterval(countdownTimer) })
</script>

<template>
  <div class="page auth-layout">
    <aside class="auth-intro"><p class="eyebrow">CSA · CONNECT & CREATE</p><h1>{{ titles[mode] }}</h1><p>在技术探索中相遇，<br />在共同创作中成长。</p><div class="auth-art" aria-hidden="true"><span>&lt;/&gt;</span><span class="art-dot">✦</span></div><RouterLink to="/">← 返回首页</RouterLink></aside>
    <section class="panel auth-card">
      <h2>{{ mode === 'login' ? '登录你的账号' : mode === 'register' ? '创建协会账号' : titles[mode] }}</h2>
      <p class="muted">{{ mode === 'register' ? '带 * 的为必填项，其他资料可以稍后补充。' : mode === 'forgot' ? '输入绑定邮箱，获取重置验证码。' : mode === 'reset' ? '请输入邮箱中的验证码和新的密码。' : '继续你的学习与协作。' }}</p>
      <div v-if="error" role="alert" class="error">{{ error }}</div>
      <p v-if="success" role="status" class="success">{{ success }}</p>
      <form class="stack" novalidate :aria-busy="busy" @submit.prevent="submit">
        <fieldset :disabled="busy" class="auth-fields">
          <label v-if="mode === 'login' || mode === 'register'" class="field">{{ mode === 'register' ? '用户名 *' : '账号' }}<input v-model="form.username" name="username" autocomplete="username" :maxlength="mode === 'register' ? 20 : 100" :aria-invalid="!!errors.username" aria-describedby="username-error" /><small v-if="errors.username" id="username-error" class="error">{{ errors.username }}</small></label>
          <label v-if="mode !== 'login'" class="field">绑定邮箱 *<input v-model="form.email" name="email" type="email" autocomplete="email" :aria-invalid="!!errors.email" aria-describedby="email-error" /><small v-if="errors.email" id="email-error" class="error">{{ errors.email }}</small></label>
          <div v-if="mode === 'register' || mode === 'reset'" class="field"><label for="auth-code">{{ mode === 'reset' ? '重置验证码 *' : '邮箱验证码 *' }}</label><div class="toolbar code-row"><input id="auth-code" v-model="form.code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" :aria-invalid="!!errors.code" aria-describedby="code-error" /><button v-if="mode === 'register'" type="button" class="secondary" :disabled="sending || countdown > 0" @click="sendCode">{{ sending ? '发送中…' : countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}</button></div><small v-if="errors.code" id="code-error" class="error">{{ errors.code }}</small></div>
          <div v-if="mode !== 'forgot'" class="field"><label for="auth-password">{{ mode === 'reset' ? '新密码 *' : '密码' + (mode === 'register' ? ' *' : '') }}</label><div class="password-row"><input id="auth-password" v-model="form.password" name="password" :type="showPassword ? 'text' : 'password'" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" :aria-invalid="!!errors.password" aria-describedby="password-help password-error" /><button type="button" class="secondary" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showPassword" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></div><small v-if="mode !== 'login'" id="password-help" class="muted">{{ mode === 'register' ? '6–20' : '8–64' }} 位字母和数字，必须同时包含两者。</small><small v-if="errors.password" id="password-error" class="error">{{ errors.password }}</small></div>
          <div v-if="mode === 'register' || mode === 'reset'" class="field"><label for="auth-confirm">确认密码 *</label><div class="password-row"><input id="auth-confirm" v-model="form.confirmPassword" name="confirmPassword" :type="showConfirm ? 'text' : 'password'" autocomplete="new-password" :aria-invalid="!!errors.confirmPassword" aria-describedby="confirm-error" /><button type="button" class="secondary" :aria-label="showConfirm ? '隐藏确认密码' : '显示确认密码'" :aria-pressed="showConfirm" @click="showConfirm = !showConfirm">{{ showConfirm ? '隐藏' : '显示' }}</button></div><small v-if="errors.confirmPassword" id="confirm-error" class="error">{{ errors.confirmPassword }}</small></div>
          <template v-if="mode === 'register'">
            <label class="field">真实姓名（选填）<input v-model="form.realName" name="realName" autocomplete="name" /></label>
            <label class="field">注册方式<select v-model="registerType" name="registerType"><option value="invite">邀请码注册</option><option value="guest">普通注册</option></select></label>
            <label v-if="registerType === 'invite'" class="field">邀请码（选填）<input v-model="form.inviteCode" name="inviteCode" /><small class="muted">协会邀请码可用于开通相应会员身份。</small></label>
            <template v-else><label class="field">支付单号（选填）<input v-model="form.merchantNo" name="merchantNo" /><small class="muted">仅用于辅助人工核验。未填写则默认创建游客账号。</small></label><div class="form-grid"><label class="field">学院<input v-model="form.college" name="college" /></label><label class="field">班级<input v-model="form.className" name="className" /></label><label class="field">学号<input v-model="form.studentId" name="studentId" /></label></div></template>
            <div class="privacy-consent"><label class="checkbox-row"><input v-model="form.privacyConsent" name="privacyConsent" type="checkbox" :disabled="privacyLoading || !privacyVersion" />我已阅读并同意 <RouterLink to="/privacy" target="_blank" rel="noopener">隐私政策</RouterLink></label><small class="muted">当前版本：{{ privacyLoading ? '加载中…' : privacyVersion || '暂未加载' }}</small><p v-if="privacyError" class="error" role="alert">{{ privacyError }} <button type="button" class="secondary" @click="loadPrivacy">重试加载</button></p><small v-if="errors.privacy" class="error">{{ errors.privacy }}</small></div>
          </template>
          <button type="submit" :disabled="busy || (mode === 'register' && (privacyLoading || !privacyVersion))">{{ busy ? '处理中…' : submits[mode] }}</button>
        </fieldset>
      </form>
      <nav class="auth-links" aria-label="账号帮助"><template v-if="mode === 'login'"><RouterLink to="/register">注册新账号</RouterLink><RouterLink to="/forgot-password">忘记密码？</RouterLink></template><template v-else><RouterLink to="/login">返回登录</RouterLink><RouterLink v-if="mode === 'forgot'" to="/reset-password">已收到验证码，重置密码</RouterLink><RouterLink v-if="mode === 'reset'" to="/forgot-password">重新发送验证码</RouterLink></template></nav>
    </section>
  </div>
</template>

<style scoped>
.auth-layout{display:grid;grid-template-columns:.9fr 1.1fr;gap:4rem;align-items:start;max-width:1080px}.auth-intro{padding:3rem 0;position:sticky;top:5rem}.auth-intro h1{font-size:3rem;margin:1.5rem 0}.auth-intro>p:not(.eyebrow){font-size:1.3rem;color:var(--muted);line-height:1.9}.eyebrow{font-size:.73rem;letter-spacing:.14em;color:var(--accent);font-weight:700}.auth-art{height:180px;display:flex;align-items:center;gap:2rem;color:var(--accent)}.auth-art>span:first-child{font-size:5rem;font-weight:700;transform:rotate(-8deg)}.art-dot{font-size:3rem;color:var(--accent)}.auth-card{padding:2rem}.auth-card h2{margin-top:0}.auth-card>p{margin-bottom:1.5rem}.auth-fields{border:0;padding:0;margin:0;min-width:0;display:grid;gap:1.2rem}.password-row{display:flex;gap:.4rem}.password-row input,.code-row input{min-width:0;flex:1}.password-row button{white-space:nowrap}.code-row{flex-wrap:nowrap}.privacy-consent{background:var(--soft);padding:1rem;border-radius:.7rem;display:grid;gap:.5rem}.checkbox-row{display:flex;align-items:center;gap:.45rem;flex-wrap:wrap;font-size:.9rem}.checkbox-row input{width:1rem;height:1rem;flex-shrink:0}.auth-links{display:flex;flex-wrap:wrap;gap:1rem;justify-content:space-between;margin-top:1.5rem;font-size:.9rem}.field .error{padding:0;background:transparent;border:0}.auth-card>.error{margin-bottom:1rem}@media(max-width:760px){.auth-layout{grid-template-columns:1fr;gap:1rem}.auth-intro{position:static;padding:0}.auth-intro h1{font-size:2rem}.auth-intro>p:not(.eyebrow),.auth-art{display:none}.auth-card{padding:1.2rem}}
</style>
