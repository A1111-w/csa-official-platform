<script setup lang="ts">
import { computed, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
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
const titles = { login: '登录你的账号', register: '加入 CSA', forgot: '找回密码', reset: '设置新密码' }
const submits = { login: '立即登录', register: '立即注册', forgot: '发送重置验证码', reset: '确认重置密码' }
const form = reactive({ username: '', password: '', confirmPassword: '', email: '', code: '', realName: '', studentId: '', college: '', className: '', inviteCode: '', merchantNo: '', privacyConsent: false })
const registerType = ref<'invite' | 'guest'>('invite')
const busy = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')
const errors = reactive<Record<string, string>>({})
const formElement = ref<HTMLFormElement | null>(null)
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
  if (busy.value) return
  if (!validate()) {
    await nextTick()
    formElement.value?.querySelector<HTMLElement>('[aria-invalid="true"]:not(:disabled)')?.focus()
    return
  }
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
  <div class="page auth-layout" :data-auth-mode="mode">
    <aside class="auth-intro">
      <RouterLink class="auth-back" to="/"><span aria-hidden="true">←</span> 返回首页</RouterLink>
      <div class="orbit" aria-hidden="true"><i></i><i></i><i></i></div>
      <div class="identity"><span lang="en">Computer Science Association</span><strong>计算机协会</strong></div>
    </aside>
    <section class="auth-card" aria-labelledby="auth-title">
      <h1 id="auth-title">{{ titles[mode] }}</h1>
      <p class="muted">{{ mode === 'register' ? '带 * 的为必填项，其他资料可以稍后补充。' : mode === 'forgot' ? '输入绑定邮箱，获取重置验证码。' : mode === 'reset' ? '输入邮箱中的验证码和新密码。' : '使用协会账号进入成员工作台。' }}</p>
      <div v-if="error" role="alert" class="error">{{ error }}</div>
      <p v-if="success" role="status" class="success">{{ success }}</p>
      <form ref="formElement" class="stack" novalidate :aria-busy="busy" @submit.prevent="submit">
        <fieldset :disabled="busy" class="auth-fields">
          <label v-if="mode === 'login' || mode === 'register'" class="field">{{ mode === 'register' ? '用户名 *' : '账号' }}<input v-model="form.username" name="username" autocomplete="username" :maxlength="mode === 'register' ? 20 : 100" :aria-invalid="!!errors.username" aria-required="true" :aria-describedby="errors.username ? 'username-error' : undefined" /><small v-if="errors.username" id="username-error" class="error">{{ errors.username }}</small></label>
          <label v-if="mode !== 'login'" class="field">绑定邮箱 *<input v-model="form.email" name="email" type="email" autocomplete="email" :aria-invalid="!!errors.email" aria-required="true" :aria-describedby="errors.email ? 'email-error' : undefined" /><small v-if="errors.email" id="email-error" class="error">{{ errors.email }}</small></label>
          <div v-if="mode === 'register' || mode === 'reset'" class="field"><label for="auth-code">{{ mode === 'reset' ? '重置验证码 *' : '邮箱验证码 *' }}</label><div class="toolbar code-row"><input id="auth-code" v-model="form.code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" :aria-invalid="!!errors.code" aria-required="true" :aria-describedby="errors.code ? 'code-error' : undefined" /><button v-if="mode === 'register'" type="button" class="secondary" :disabled="sending || countdown > 0" @click="sendCode">{{ sending ? '发送中…' : countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}</button></div><small v-if="errors.code" id="code-error" class="error">{{ errors.code }}</small></div>
          <div v-if="mode !== 'forgot'" class="field"><label for="auth-password">{{ mode === 'reset' ? '新密码 *' : '密码' + (mode === 'register' ? ' *' : '') }}</label><div class="password-row"><input id="auth-password" v-model="form.password" name="password" :type="showPassword ? 'text' : 'password'" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" :aria-invalid="!!errors.password" aria-required="true" :aria-describedby="[mode !== 'login' && 'password-help', errors.password && 'password-error'].filter(Boolean).join(' ') || undefined" /><button type="button" class="secondary" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showPassword" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></div><small v-if="mode !== 'login'" id="password-help" class="muted">{{ mode === 'register' ? '6–20' : '8–64' }} 位字母和数字，必须同时包含两者。</small><small v-if="errors.password" id="password-error" class="error">{{ errors.password }}</small></div>
          <div v-if="mode === 'register' || mode === 'reset'" class="field"><label for="auth-confirm">确认密码 *</label><div class="password-row"><input id="auth-confirm" v-model="form.confirmPassword" name="confirmPassword" :type="showConfirm ? 'text' : 'password'" autocomplete="new-password" :aria-invalid="!!errors.confirmPassword" aria-required="true" :aria-describedby="errors.confirmPassword ? 'confirm-error' : undefined" /><button type="button" class="secondary" :aria-label="showConfirm ? '隐藏确认密码' : '显示确认密码'" :aria-pressed="showConfirm" @click="showConfirm = !showConfirm">{{ showConfirm ? '隐藏' : '显示' }}</button></div><small v-if="errors.confirmPassword" id="confirm-error" class="error">{{ errors.confirmPassword }}</small></div>
          <template v-if="mode === 'register'">
            <label class="field">真实姓名（选填）<input v-model="form.realName" name="realName" autocomplete="name" /></label>
            <label class="field">注册方式<select v-model="registerType" name="registerType"><option value="invite">邀请码注册</option><option value="guest">普通注册</option></select></label>
            <label v-if="registerType === 'invite'" class="field">邀请码（选填）<input v-model="form.inviteCode" name="inviteCode" /><small class="muted">协会邀请码可用于开通相应会员身份。</small></label>
            <template v-else><label class="field">支付单号（选填）<input v-model="form.merchantNo" name="merchantNo" /><small class="muted">仅用于辅助人工核验。未填写则默认创建游客账号。</small></label><div class="form-grid"><label class="field">学院<input v-model="form.college" name="college" /></label><label class="field">班级<input v-model="form.className" name="className" /></label><label class="field">学号<input v-model="form.studentId" name="studentId" /></label></div></template>
            <div class="privacy-consent"><label class="checkbox-row"><input v-model="form.privacyConsent" name="privacyConsent" type="checkbox" aria-required="true" :aria-invalid="!!errors.privacy" :aria-describedby="errors.privacy ? 'privacy-error' : undefined" :disabled="privacyLoading || !privacyVersion" />我已阅读并同意 <RouterLink to="/privacy" target="_blank" rel="noopener">隐私政策</RouterLink></label><small class="muted">当前版本：{{ privacyLoading ? '加载中…' : privacyVersion || '暂未加载' }}</small><p v-if="privacyError" class="error" role="alert">{{ privacyError }} <button type="button" class="secondary" @click="loadPrivacy">重试加载</button></p><small v-if="errors.privacy" id="privacy-error" class="error">{{ errors.privacy }}</small></div>
          </template>
          <button type="submit" :disabled="busy || (mode === 'register' && (privacyLoading || !privacyVersion))">{{ busy ? '处理中…' : submits[mode] }}</button>
        </fieldset>
      </form>
      <nav class="auth-links" aria-label="账号帮助"><template v-if="mode === 'login'"><RouterLink to="/register">注册新账号</RouterLink><RouterLink to="/forgot-password">忘记密码？</RouterLink></template><template v-else><RouterLink to="/login">返回登录</RouterLink><RouterLink v-if="mode === 'forgot'" to="/reset-password">已收到验证码，重置密码</RouterLink><RouterLink v-if="mode === 'reset'" to="/forgot-password">重新发送验证码</RouterLink></template></nav>
    </section>
  </div>
</template>

<style scoped>
.auth-layout {
  --photo-x: 69%; --starlight: #b3e3dc; --tilt: -22deg;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(21rem, .9fr);
  align-items: start; width: min(100%, 1460px); max-width: 1460px; margin-inline: auto;
  gap: clamp(2rem, 6vw, 6rem); padding: clamp(1.5rem, 4vw, 4rem) clamp(1rem, 5vw, 5rem);
}
.auth-layout[data-auth-mode="register"] { --photo-x: 55%; --starlight: #f4c799; --tilt: 21deg; }
.auth-layout[data-auth-mode="forgot"] { --photo-x: 79%; --starlight: #a4d0ff; --tilt: -49deg; }
.auth-layout[data-auth-mode="reset"] { --photo-x: 61%; --starlight: #e2b9ed; --tilt: 49deg; }
.auth-intro {
  position: sticky; top: 5.5rem; isolation: isolate; overflow: hidden;
  display: flex; flex-flow: column; justify-content: space-between;
  min-height: clamp(30rem, 65svh, 45rem); padding: clamp(1.3rem, 3vw, 3rem);
  color: #fbf8ef; border-radius: 4px;
  background: linear-gradient(0deg, #070d1cf2 0%, transparent 46%, #08102475), url('/brand/csa-observatory-sky.webp') var(--photo-x) center / cover no-repeat, #0c1528;
}
.auth-back { align-self: start; display: inline-flex; align-items: center; min-height: 44px; gap: .6em; color: #fff9ed; font-size: .9rem; }
.orbit { position: absolute; z-index: -1; inset: 17% -13% 6%; overflow: hidden; pointer-events: none; }
.orbit i { position: absolute; inset: 21% 15%; border: 1px solid color-mix(in srgb, var(--starlight) 58%, transparent); border-radius: 50%; transform: rotate(var(--tilt)); }
.orbit i:nth-child(2) { inset: 12% 23%; transform: rotate(calc(var(--tilt) + 40deg)); opacity: .65; }
.orbit i:nth-child(3) { inset: 33% 4%; opacity: .68; }
.orbit i::after { content: ''; position: absolute; inset: 50% auto auto -3px; width: 5px; height: 5px; border-radius: 50%; background: var(--starlight); box-shadow: 0 0 .65rem var(--starlight); }
@supports (offset-path: ellipse(50% 50% at 50% 50%)) {
  .orbit i::after { inset: 0 auto auto 0; offset-path: ellipse(50% 50% at 50% 50%); offset-distance: 15%; }
  .orbit i:nth-child(2)::after { offset-distance: 70%; }
  @media (prefers-reduced-motion: no-preference) {
    .orbit i::after { animation: orbit-travel 32s linear infinite; }
    .orbit i:nth-child(2)::after { animation-duration: 44s; animation-delay: -31s; }
    .orbit i:nth-child(3)::after { animation-delay: -17s; }
    [data-auth-mode='register'] .orbit i::after, [data-auth-mode='reset'] .orbit i::after { animation-direction: reverse; }
    @keyframes orbit-travel { from { offset-distance: 0%; } to { offset-distance: 100%; } }
  }
}
.identity { display: grid; gap: .15rem; }
.identity span { color: #d2e7ed; font: italic clamp(1.05rem, 2.1vw, 1.8rem)/1.4 Georgia, serif; }
.identity strong { font-size: clamp(2rem, 3.6vw, 4.1rem); line-height: 1.15; letter-spacing: -.055em; }
.auth-card { min-width: 0; padding-block: clamp(.4rem, 4vw, 4rem); width: 100%; max-width: 36rem; }
.auth-card h1 { font-size: clamp(2rem, 3vw, 3rem); }
.auth-card > p { margin: .55rem 0 1.8rem; }
.auth-fields { border: 0; padding: 0; margin: 0; min-width: 0; display: grid; gap: 1.05rem; }
.auth-fields .field { min-width: 0; }
.auth-fields :is(input:not([type='checkbox']), select) { min-height: 46px; margin: 0; }
.password-row, .code-row { display: flex; gap: .4rem; margin: 0; align-items: stretch; flex-wrap: nowrap; }
.password-row input, .code-row input { width: 0; min-width: 0; flex: 1; }
.password-row button, .code-row button { white-space: nowrap; }
.privacy-consent { background: var(--soft); padding: 1rem; border-radius: .5rem; display: grid; gap: .5rem; }
.checkbox-row { display: flex; align-items: center; gap: .45rem; flex-wrap: wrap; font-size: .9rem; }
.checkbox-row input { width: 1rem; height: 1rem; flex-shrink: 0; margin: 0; }
.auth-card button[type='submit'] { min-height: 46px; }
.auth-links { border-top: 1px solid var(--border); padding-top: .6rem; display: flex; flex-wrap: wrap; gap: .3rem 1rem; justify-content: space-between; margin-top: 1.4rem; font-size: .9rem; }
.auth-links a { display: inline-flex; align-items: center; min-height: 44px; }
.field .error { padding: 0; background: none; border: 0; }
.auth-card > .error { margin-block: .8rem; }
@media (max-width: 720px) {
  .auth-layout { grid-template-columns: minmax(0, 1fr); gap: 1rem; max-width: 39rem; padding-block: 1rem 2rem; }
  .auth-intro { position: relative; top: auto; min-height: 9rem; padding: .65rem 1.25rem 1rem; }
  .identity { gap: 0; }
  .identity span { font-size: .82rem; }
  .identity strong { font-size: 1.5rem; }
  .orbit { inset: -20% -5% -25% 43%; }
  .auth-card { max-width: none; padding: .35rem 0 0; }
  .auth-card > p { margin-bottom: 1.2rem; }
  .code-row button { padding-inline: .55rem; }
}
</style>
