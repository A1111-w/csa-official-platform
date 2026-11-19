<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { createPlanetScene, type PlanetRipple } from './home/planet-scene'
import { beijingMinute, limit, solarTheme } from './home/solar-theme'

defineProps<{ signedIn: boolean }>()

const sky = ref<HTMLDivElement | null>(null)
const landscape = ref<HTMLDivElement | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const minute = ref(beijingMinute(new Date()))
const followsClock = ref(true)
const paused = ref(false)
const reduceMotion = ref(false)
const sceneAvailable = ref(false)
const constellation = ref(false)
const theme = computed(() => solarTheme(minute.value))
const moving = computed(() => !paused.value && !reduceMotion.value)
const motionState = computed(() => reduceMotion.value ? 'reduced' : paused.value ? 'paused' : 'playing')

const initialView = { yaw: 0, pitch: -.18, parallaxX: 0, parallaxY: 0 }
const view = { ...initialView }
const goal = { ...initialView }
let scene: ReturnType<typeof createPlanetScene> = null
let resizeObserver: ResizeObserver | undefined
let intersectionObserver: IntersectionObserver | undefined
let motionPreference: MediaQueryList | undefined
let clockTimer: ReturnType<typeof setTimeout> | undefined
let frame = 0
let previousFrameTime = 0
let drawnGradeX = ''
let drawnGradeY = ''
let rotationClock = 0
let formed = 0
let ripples: PlanetRipple[] = []
let drag: { id: number; x: number; y: number; distance: number } | null = null
let visible = true

function synchronizeClock() {
  clearTimeout(clockTimer)
  if (!followsClock.value || document.hidden) return
  const now = new Date()
  minute.value = beijingMinute(now)
  clockTimer = setTimeout(synchronizeClock, 60_020 - now.getTime() % 60_000)
}

function previewTime(event: Event) {
  if (!(event.target instanceof HTMLInputElement)) return
  followsClock.value = false
  clearTimeout(clockTimer)
  minute.value = Number(event.target.value)
}

function followClock() {
  followsClock.value = true
  synchronizeClock()
}

function closeSettings(event: KeyboardEvent) {
  const settings = event.currentTarget
  if (!(settings instanceof HTMLDetailsElement) || !settings.open) return
  settings.open = false
  settings.querySelector('summary')?.focus()
}

function paintScene() {
  if (!scene || !visible || document.hidden) return
  const gradeX = `${(view.parallaxX * -13).toFixed(2)}px`
  const gradeY = `${(view.parallaxY * -9).toFixed(2)}px`
  if (drawnGradeX !== gradeX || drawnGradeY !== gradeY) {
    landscape.value?.style.setProperty('--art-x', gradeX)
    landscape.value?.style.setProperty('--art-y', gradeY)
    drawnGradeX = gradeX
    drawnGradeY = gradeY
  }
  scene.paint({ ...view, time: rotationClock, daypart: theme.value.daypart, minute: minute.value, ripples, formed })
}

function animate(now: number) {
  frame = 0
  if (!scene || !visible || document.hidden || !moving.value) return
  // Keep one animation loop and cap its raster redraws to about 30 FPS.
  const elapsed = previousFrameTime ? now - previousFrameTime : 32
  if (elapsed >= 31) {
    previousFrameTime = now
    const step = Math.min(elapsed / 1000, .055)
    rotationClock += step
    if (!drag) goal.yaw += .09 * step
    view.yaw += (goal.yaw - view.yaw) * .32
    view.pitch += (goal.pitch - view.pitch) * .32
    view.parallaxX += (goal.parallaxX - view.parallaxX) * .26
    view.parallaxY += (goal.parallaxY - view.parallaxY) * .26
    formed += (Number(constellation.value) - formed) * .16
    ripples = ripples.map(ripple => ({ ...ripple, age: ripple.age + step })).filter(ripple => ripple.age < 1.8)
    paintScene()
  }
  frame = requestAnimationFrame(animate)
}

function updateScene() {
  if (!scene || !visible || document.hidden) return
  if (!moving.value) {
    Object.assign(view, goal)
    formed = Number(constellation.value)
    paintScene()
    return
  }
  if (frame) return
  previousFrameTime = 0
  frame = requestAnimationFrame(animate)
}

function stopAnimation() {
  if (frame) cancelAnimationFrame(frame)
  frame = 0
}

function handleVisibility() {
  synchronizeClock()
  if (document.hidden) stopAnimation()
  else updateScene()
}

function updateMotionPreference() {
  reduceMotion.value = motionPreference?.matches ?? false
}

function resetView() {
  Object.assign(view, initialView)
  Object.assign(goal, initialView)
  rotationClock = 0
  ripples = []
  paintScene()
}

function pulse(x: number, y: number) {
  if (!moving.value) return
  ripples = [...ripples.slice(-2), { x, y, age: 0 }]
  updateScene()
}

function startDrag(event: PointerEvent) {
  if (!scene || drag || !event.isPrimary || event.button !== 0) return
  if (event.target instanceof Element && event.target.closest('a, button, input')) return
  drag = { id: event.pointerId, x: event.clientX, y: event.clientY, distance: 0 }
  sky.value?.setPointerCapture(event.pointerId)
}

function movePointer(event: PointerEvent) {
  if (!scene || !sky.value) return
  if (moving.value && event.pointerType === 'mouse') {
    const bounds = sky.value.getBoundingClientRect()
    goal.parallaxX = limit((event.clientX - bounds.left) / bounds.width * 2 - 1, -1, 1)
    goal.parallaxY = limit((event.clientY - bounds.top) / bounds.height * 2 - 1, -1, 1)
  }
  if (drag?.id === event.pointerId) {
    const dx = event.clientX - drag.x
    const dy = event.clientY - drag.y
    drag.x = event.clientX
    drag.y = event.clientY
    drag.distance += Math.hypot(dx, dy)
    goal.yaw += dx * .0125
    goal.pitch = limit(goal.pitch + dy * .007, -.9, .9)
  }
  updateScene()
}

function leaveSky() {
  goal.parallaxX = 0
  goal.parallaxY = 0
  updateScene()
}

function endDrag(event: PointerEvent) {
  if (drag?.id !== event.pointerId || !sky.value) return
  const isTap = drag.distance < 8
  drag = null
  if (isTap && event.type === 'pointerup') {
    const { top, left } = sky.value.getBoundingClientRect()
    pulse(event.clientX - left, event.clientY - top)
  }
}

function useKeyboard(event: KeyboardEvent) {
  if (event.target !== sky.value || !scene || !sky.value) return
  const steps: Record<string, [number, number]> = {
    ArrowLeft: [-.23, 0], ArrowRight: [.23, 0], ArrowUp: [0, -.16], ArrowDown: [0, .16],
  }
  const step = steps[event.key]
  if (step) {
    event.preventDefault()
    goal.yaw += step[0]
    goal.pitch = limit(goal.pitch + step[1], -.9, .9)
    updateScene()
  } else if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    pulse(sky.value.clientWidth * .74, sky.value.clientHeight * .40)
  } else if (event.key === 'Home') {
    event.preventDefault()
    resetView()
  }
}

watch(minute, paintScene)
watch(constellation, updateScene)
watch(moving, () => {
  if (!moving.value) {
    stopAnimation()
    Object.assign(goal, view, { parallaxX: 0, parallaxY: 0 })
    ripples = []
  }
  updateScene()
})

onMounted(() => {
  synchronizeClock()
  document.addEventListener('visibilitychange', handleVisibility)
  if (typeof window.matchMedia === 'function') {
    motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)')
    updateMotionPreference()
    motionPreference.addEventListener?.('change', updateMotionPreference)
  }
  if (!canvas.value) return
  scene = createPlanetScene(canvas.value)
  sceneAvailable.value = Boolean(scene)
  if (!scene) return
  resizeScene()
  updateScene()
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(resizeScene)
    resizeObserver.observe(canvas.value)
  } else window.addEventListener('resize', resizeScene)
  if (typeof IntersectionObserver !== 'undefined') {
    intersectionObserver = new IntersectionObserver(([entry]) => {
      visible = entry.isIntersecting
      if (visible) updateScene()
      else stopAnimation()
    })
    intersectionObserver.observe(canvas.value)
  }
})

function resizeScene() {
  scene?.resize()
  paintScene()
}

onUnmounted(() => {
  clearTimeout(clockTimer)
  stopAnimation()
  resizeObserver?.disconnect()
  intersectionObserver?.disconnect()
  motionPreference?.removeEventListener?.('change', updateMotionPreference)
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('resize', resizeScene)
  scene = null
})
</script>

<template>
  <section class="hero" :style="theme.style" :data-daypart="theme.daypart" :data-motion="motionState">
    <div
      ref="sky"
      class="hero-sky"
      role="region"
      aria-label="可交互的星空"
      :aria-describedby="sceneAvailable ? 'home-sky-help' : undefined"
      :tabindex="sceneAvailable ? 0 : undefined"
      @pointerdown="startDrag"
      @pointermove="movePointer"
      @pointerup="endDrag"
      @pointercancel="endDrag"
      @lostpointercapture="endDrag"
      @pointerleave="leaveSky"
      @keydown="useKeyboard"
    >
      <div ref="landscape" class="hero-landscape" aria-hidden="true"></div>
      <div class="hero-mask" aria-hidden="true"></div>
      <canvas ref="canvas" class="hero-planets" data-horizon-scene aria-hidden="true"></canvas>
      <div class="hero-frame">
        <div class="hero-content">
          <p class="hero-english" lang="en">Computer Science <em>Association</em></p>
          <h1>计算机协会</h1>
          <p class="hero-summary">交流技术，组队参赛，<br class="mobile-break" />一起把开源项目做出来。</p>
          <div class="hero-actions">
            <RouterLink class="button hero-primary" :to="signedIn ? '/dashboard' : '/login'">
              {{ signedIn ? '进入工作台' : '登录平台' }} <span aria-hidden="true">↗</span>
            </RouterLink>
            <RouterLink class="hero-secondary" to="/about">认识 CSA <span aria-hidden="true">→</span></RouterLink>
          </div>
        </div>
      </div>
    </div>

    <div class="hero-rail">
      <a class="hero-jump" href="#home-activity">
        <span class="jump-arrow" aria-hidden="true">↓</span><span>浏览协会动态</span>
      </a>
      <div class="hero-observe">
        <span class="sky-clock">
          <span>{{ followsClock ? '北京时间' : '预览时间' }}</span>
          <time>{{ theme.label.split(' · ')[0] }}</time>
        </span>
        <details class="sky-settings" @keydown.esc="closeSettings">
          <summary>
            <span class="settings-mark" aria-hidden="true"></span>
            <span>星空设置</span>
          </summary>
          <div class="settings-panel">
            <div class="hero-time">
              <div class="hero-time-caption">
                <label for="home-hero-time">时段预览</label>
                <output for="home-hero-time">{{ theme.label }}</output>
              </div>
              <input id="home-hero-time" type="range" min="0" max="1439" :value="minute" :aria-valuetext="theme.label" @input="previewTime" />
            </div>
            <div class="hero-buttons">
              <button type="button" :aria-pressed="followsClock" @click="followClock">跟随北京时间</button>
              <template v-if="sceneAvailable">
                <button v-if="!reduceMotion" type="button" @click="paused = !paused">{{ paused ? '播放星空动效' : '暂停星空动效' }}</button>
                <button type="button" :aria-pressed="constellation" @click="constellation = !constellation">聚成 CSA</button>
                <button type="button" @click="resetView">重置视角</button>
              </template>
            </div>
            <p v-if="sceneAvailable" id="home-sky-help" class="hero-help">
              {{ moving ? '拖动或按方向键旋转星空，点击或按回车触发星波。' : '拖动或按方向键，手动调整视角。' }}
            </p>
          </div>
        </details>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  --sky-ink: #080e1c;
  --sky-ice: #c6f6f1;
  --sky-gutter: clamp(1rem, 5vw, 5rem);
  --sky-inner: min(1480px, 100% - 2 * var(--sky-gutter));
  position: relative;
  isolation: isolate;
  color: #f5f7ff;
  background: var(--sky-ink);
  color-scheme: dark;
}

.hero-sky {
  display: grid;
  align-items: center;
  min-block-size: clamp(34rem, 72svh, 49rem);
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background: var(--sky-ink);
}

.hero-sky[tabindex] { cursor: grab; touch-action: pan-y; }
.hero-sky[tabindex]:active { cursor: grabbing; }
.hero-sky:focus-visible { outline: 2px solid var(--sky-ice); outline-offset: -5px; }

.hero-landscape, .hero-mask, .hero-planets {
  position: absolute;
  inset: 0;
  inline-size: 100%;
  block-size: 100%;
  pointer-events: none;
}

.hero-landscape {
  --horizon-position: center 63%;
  background: #0b152b url('/brand/csa-ring-horizon-midnight.webp') var(--horizon-position) / cover no-repeat;
  transform: translate(var(--art-x, 0), var(--art-y, 0)) scale(1.035);
}
.hero-landscape::before, .hero-landscape::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.hero-landscape::before {
  background:
    linear-gradient(166deg, transparent 30%, color-mix(in srgb, var(--sky-color) 14%, transparent) 76%, transparent),
    url('/brand/csa-ring-horizon-dawn.webp') var(--horizon-position) / cover no-repeat;
  opacity: max(var(--daylight), calc(var(--twilight) * .92));
  transition: opacity .65s ease-out;
}
.hero-landscape::after {
  background: radial-gradient(ellipse at var(--sun-x) 78%, var(--sky-color), transparent 66%);
  mix-blend-mode: screen;
  opacity: calc(var(--daylight) * .13 + var(--twilight) * .09);
}
.hero-mask {
  background:
    linear-gradient(180deg, #080e1c26, transparent 17%, transparent 78%, #080e1cd6),
    linear-gradient(90deg, #080e1c65, #080e1c22 41%, transparent 61%);
}
.hero-frame {
  position: relative;
  width: var(--sky-inner);
  margin-inline: auto;
  padding: 4rem 0 6.25rem;
  pointer-events: none;
}
.hero-content {
  width: max-content;
  max-width: min(55%, 45rem);
  color: #faf8f4;
}
.hero-english {
  display: flex;
  align-items: baseline;
  gap: .27em;
  color: #d9eae7;
  font-family: Baskervville, Georgia, 'Times New Roman', serif;
  font-size: clamp(1.3rem, 2.2vw, 2.3rem);
  line-height: 1.2;
  letter-spacing: -.035em;
}
.hero-english em { color: #e6d6bc; font-synthesis: style; }
.hero h1 {
  margin: .16em 0 0 -.045em;
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, sans-serif;
  font-weight: 650;
  font-size: clamp(4rem, 6.1vw, 6.75rem);
  line-height: 1.18;
  letter-spacing: -.05em;
  color: #faf8f4;
  text-shadow: 0 2px 22px #080e1c54;
}
.hero-summary {
  margin-block: clamp(.95rem, 1.8vw, 1.65rem) 1.65rem;
  color: #e0e7eb;
  font-size: clamp(.94rem, 1.25vw, 1.12rem);
  line-height: 1.75;
  letter-spacing: .045em;
}
.mobile-break { display: none; }
.hero-actions { display: flex; align-items: center; gap: clamp(1.1rem, 2.2vw, 2.05rem); }
.hero-actions a { pointer-events: auto; }
.hero-primary {
  min-block-size: 44px;
  gap: 1.15rem;
  padding: .64rem 1.2rem;
  border: 1px solid #defff7a3;
  border-radius: 999px;
  background: var(--sky-ice);
  color: #101e26;
  font-size: .91rem;
  font-weight: 650;
}
.hero-primary span { font-size: 1.05em; line-height: 1; }
.hero-primary:hover { background: #eafff9; }
.hero-secondary {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  gap: .6rem;
  color: #edf2f6;
  font-size: .92rem;
}
.hero-secondary span { color: var(--sky-ice); }
.hero-primary:focus-visible, .hero-secondary:focus-visible, .hero-jump:focus-visible,
.sky-settings summary:focus-visible, .hero-buttons button:focus-visible, .hero-time input:focus-visible {
  outline-color: var(--sky-ice);
}

.hero-rail {
  position: absolute;
  z-index: 1;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: var(--sky-inner);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 4.75rem;
  gap: 1rem;
  border-top: 1px solid #c7e7e233;
  pointer-events: none;
}
.hero-rail a, .sky-settings { pointer-events: auto; }
.hero-jump, .hero-observe, .sky-clock, .sky-settings summary { display: inline-flex; align-items: center; }
.hero-jump { gap: .65rem; min-height: 44px; color: #dce8ec; font-size: .82rem; }
.jump-arrow { color: var(--sky-ice); font-size: 1.2rem; line-height: 1; }
.hero-observe { gap: clamp(.8rem, 2vw, 1.7rem); }
.sky-clock { gap: .58rem; color: #b8c8cf; font-size: .78rem; }
.sky-clock time { font-family: 'IBM Plex Mono', Consolas, monospace; color: #eaf1ef; font-size: .89rem; letter-spacing: .055em; }
.sky-settings { position: relative; color: #eef5f7; margin: 0; padding: 0; border: 0; }
.sky-settings summary {
  min-block-size: 44px;
  margin: 0;
  padding-inline: .8rem;
  border: 1px solid #aec6df44;
  border-radius: 2rem;
  background: #0c162c70;
  list-style: none;
  gap: .55rem;
  font-size: .8rem;
  cursor: pointer;
}
.sky-settings summary::-webkit-details-marker { display: none; }
.sky-settings summary:hover { color: var(--sky-ice); }
.settings-mark {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  background:
    radial-gradient(circle at 33% 31%, currentColor 0 2px, transparent 2.5px),
    radial-gradient(circle at 72% 71%, currentColor 0 2px, transparent 2.5px),
    linear-gradient(currentColor, currentColor) center 31% / 98% 1px no-repeat,
    linear-gradient(currentColor, currentColor) center 71% / 98% 1px no-repeat;
}
.settings-panel {
  position: absolute;
  inset: auto 0 calc(100% + .45rem) auto;
  width: min(20rem, calc(100vw - 2 * var(--sky-gutter)));
  padding: 1.15rem;
  border: 1px solid #6e909a8c;
  border-radius: .8rem;
  background: #0b1524f2;
  box-shadow: 0 12px 40px #02061061;
  backdrop-filter: blur(18px);
}
.hero-time-caption { display: flex; justify-content: space-between; gap: .5rem; align-items: baseline; }
.hero-time-caption label { color: #f0f7fa; font-size: .84rem; font-weight: 650; }
.hero-time-caption output { color: #cad8df; font-size: .78rem; font-variant-numeric: tabular-nums; }
.hero-time input[type='range'] {
  display: block;
  min-height: 34px;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  accent-color: var(--sky-ice);
  cursor: pointer;
}
.hero-buttons { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .4rem; }
.hero-buttons button {
  min-height: 40px;
  padding: .3rem .4rem;
  border: 1px solid #849fa650;
  background: #ffffff09;
  color: #e6eff2;
  font-size: .76rem;
  font-weight: 500;
}
.hero-buttons button[aria-pressed='true'] { background: #afdfd91c; border-color: #8bc5bf8a; }
.hero-help { color: #b3c6ce; margin-top: .65rem; font-size: .72rem; line-height: 1.55; }

@media (min-aspect-ratio: 5/2) {
  .hero-sky { min-height: clamp(32rem, 75svh, 42rem); }
}
@media (max-width: 720px) {
  .hero-sky { align-items: end; min-height: clamp(35rem, 77svh, 42rem); }
  .hero-landscape { --horizon-position: 80% center; }
  .hero-mask {
    background: linear-gradient(180deg, #080e1c38, transparent 18%, transparent 42%, #080e1cc7 76%, #080e1c 100%);
  }
  .hero-frame { padding: 0 0 6.3rem; }
  .hero-content { width: 100%; max-width: none; }
  .hero h1 { font-size: clamp(2.8rem, 12.8vw, 4.8rem); }
  .hero-english { font-size: clamp(1.15rem, 4.7vw, 1.55rem); }
  .hero-summary { margin: .85rem 0 1.15rem; font-size: .9rem; }
  .hero-rail { min-height: 3.7rem; }
  .hero-observe { gap: .8rem; }
  .sky-clock { gap: .35rem; }
  .hero-jump, .sky-settings summary, .sky-clock { font-size: .73rem; }
  .hero-buttons button { min-height: 44px; }
}
@media (max-width: 370px) {
  .mobile-break { display: initial; }
  .sky-clock { flex-direction: column; align-items: end; gap: 0; line-height: 1.3; }
  .sky-clock time { font-size: .8rem; }
}
@media (prefers-reduced-motion: reduce) {
  .hero-landscape::before, .hero-actions .button, .hero-buttons button { transition: none; }
}
</style>
