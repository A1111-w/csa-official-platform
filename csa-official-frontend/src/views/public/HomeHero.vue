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
      <canvas ref="canvas" class="hero-planets" data-horizon-scene aria-hidden="true"></canvas>
      <div class="hero-mask" aria-hidden="true"></div>
      <div class="hero-content">
        <p class="hero-kicker">CSA 计算机协会</p>
        <h1>点亮你的<br /><em>技术宇宙。</em></h1>
        <p class="hero-summary">从竞赛、开源与实验室开始，找到一起把想法做成作品的人。</p>
        <div class="hero-actions">
          <RouterLink class="button hero-primary" :to="signedIn ? '/dashboard' : '/login'">
            {{ signedIn ? '进入工作台' : '登录平台' }} <span aria-hidden="true">↗</span>
          </RouterLink>
          <RouterLink class="button hero-secondary" to="/about">认识 CSA</RouterLink>
        </div>
      </div>
      <a class="hero-jump" href="#home-activity">浏览协会动态 <span aria-hidden="true">↓</span></a>
    </div>
    <div class="hero-controls">
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
  </section>
</template>

<style scoped>
.hero {
  --instrument-bg: #0c1830;
  --instrument-line: #486989;
  --instrument-text: #f0f7ff;
  --instrument-muted: #b4cbe3;
  --instrument-key: #a1eaff;
  --sky-highlight: color-mix(in srgb, var(--sky-color) 48%, #f2feff);
  overflow: clip;
  border: 1px solid var(--instrument-line);
  border-radius: 1.35rem;
  box-shadow: 0 25px 68px #122d482e;
  background: var(--instrument-bg);
  color: var(--instrument-text);
  color-scheme: dark;
}

.hero[data-daypart='day'] {
  --instrument-bg: #eaf8fc;
  --instrument-line: #8bafc2;
  --instrument-text: #123149;
  --instrument-muted: #3a596e;
  --instrument-key: #085474;
  color-scheme: light;
}

.hero[data-daypart='dawn'] {
  --instrument-bg: #fff0e8;
  --instrument-line: #c59b9b;
  --instrument-text: #3b2a44;
  --instrument-muted: #62495f;
  --instrument-key: #67406b;
  color-scheme: light;
}

.hero[data-daypart='dusk'] {
  --instrument-bg: #211b36;
  --instrument-line: #745e91;
  --instrument-text: #fff5fc;
  --instrument-muted: #e1cfe7;
  --instrument-key: #ffcfec;
}

.hero-sky {
  min-block-size: clamp(28rem, 45vw, 36rem);
  display: grid;
  align-items: center;
  position: relative;
  isolation: isolate;
  background: #15253f;
  overflow: hidden;
}

.hero-sky[tabindex] { cursor: grab; touch-action: pan-y; }
.hero-sky[tabindex]:active { cursor: grabbing; }
.hero-sky:focus-visible { outline: 3px solid #d3f6ff; outline-offset: -5px; }

.hero-landscape, .hero-planets, .hero-mask {
  position: absolute;
  inset: 0;
  inline-size: 100%;
  block-size: 100%;
  pointer-events: none;
}

.hero-landscape {
  background:
    url('/brand/csa-horizon-landscape.webp') center 54% / cover no-repeat,
    linear-gradient(156deg, #132148, #315f7d 58%, #102b3a);
  filter: var(--land-grade);
  transform: translate(var(--art-x, 0), var(--art-y, 0)) scale(1.055);
  transition: filter .22s ease;
}

.hero-landscape::before, .hero-landscape::after {
  content: '';
  position: absolute;
  inset: 0;
  mix-blend-mode: screen;
}

.hero-landscape::before {
  background: radial-gradient(ellipse at var(--sun-x) 7%, var(--sky-highlight), var(--sky-color) 24%, transparent 68%);
  opacity: calc(.15 + var(--daylight) * .29);
}

.hero-landscape::after {
  background: radial-gradient(ellipse at var(--sun-x) 60%, #ff9b89, var(--sky-color) 28%, transparent 59%);
  opacity: calc(.55 * var(--twilight));
}

.hero-mask {
  background:
    linear-gradient(90deg, #08172beb, #0b1b35b8 27%, #10274542 52%, transparent 79%),
    linear-gradient(0deg, #061729b8, transparent 34%);
}

.hero-content {
  position: relative;
  max-inline-size: min(54%, 40rem);
  padding: 3.5rem 0 4.2rem;
  margin-inline-start: clamp(1.5rem, 5vw, 4rem);
  color: white;
}

.hero-kicker {
  color: #c3f3ff;
  font-size: .8rem;
  font-weight: 750;
  letter-spacing: .12em;
}

.hero h1 {
  margin: .7rem 0 0;
  font-size: clamp(3.1rem, 7.2vw, 6.1rem);
  font-weight: 850;
  line-height: 1.01;
  letter-spacing: -.065em;
  text-shadow: 0 4px 26px #051127b9;
}

.hero h1 em { color: var(--sky-highlight); font-style: normal; }
.hero-summary { max-inline-size: 31ch; margin-block: 1.0rem; color: #eef7ff; }
.hero-actions { display: flex; gap: .65rem; flex-wrap: wrap; }
.hero-actions .button { min-block-size: 44px; padding: .55rem .95rem; }
.hero-primary { background: var(--sky-highlight); border-color: #fff9; color: #061e2d; }
.hero-secondary { background: #08172acf; border-color: #bfdcf59c; color: #f2f9ff; }
.hero-actions .button:focus-visible, .hero-jump:focus-visible { outline-color: #c7f6ff; }

.hero-jump {
  position: absolute;
  inset: auto clamp(1.4rem, 4vw, 3.6rem) 1.35rem auto;
  display: inline-flex;
  gap: .55rem;
  align-items: center;
  min-block-size: 44px;
  color: #f5fcff;
  font-size: .85rem;
  font-weight: 700;
  text-shadow: 0 1px 8px #071529, 0 1px 16px #071529;
}

.hero-controls {
  display: grid;
  grid-template-columns: minmax(12rem, 1fr) auto;
  align-items: center;
  column-gap: 1.2rem;
  row-gap: .3rem;
  padding: .7rem clamp(1rem, 4.2vw, 3.25rem);
  background: radial-gradient(ellipse at 70% -95%, color-mix(in srgb, var(--sky-color) 53%, transparent), transparent 72%);
}

.hero-time { min-inline-size: 0; }
.hero-time-caption { display: flex; align-items: baseline; justify-content: space-between; gap: .5rem; }
.hero-time-caption label, .hero-time-caption output { font-size: .78rem; font-variant-numeric: tabular-nums; }
.hero-time-caption label { font-weight: 700; }
.hero-time-caption output { color: var(--instrument-muted); }

.hero-time input[type='range'] {
  display: block;
  min-height: 26px;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  accent-color: var(--instrument-key);
  cursor: pointer;
}

.hero-buttons { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: .35rem; }
.hero-buttons button {
  min-block-size: 42px;
  padding: .37rem .7rem;
  border: 1px solid var(--instrument-line);
  background: color-mix(in srgb, var(--instrument-bg) 85%, var(--sky-color));
  color: var(--instrument-text);
  font-size: .78rem;
}

.hero-buttons button[aria-pressed='true'] {
  background: color-mix(in srgb, var(--instrument-bg) 75%, var(--instrument-key));
  border-color: var(--instrument-key);
}

.hero-buttons button:focus-visible, .hero-time input:focus-visible { outline-color: var(--instrument-key); }
.hero-help { grid-column: 1 / -1; color: var(--instrument-muted); font-size: .75rem; line-height: 1.55; }

@media (max-width: 720px) {
  .hero-sky { min-block-size: 32rem; align-items: end; }
  .hero-landscape { background-position: 62% 54%; }
  .hero-mask {
    background:
      linear-gradient(0deg, #07162cf2, #0b1d37dc 25%, #0d233d5e 62%, transparent 83%),
      linear-gradient(90deg, #0b1b3875, transparent 76%);
  }
  .hero-content { max-inline-size: none; margin-inline: 1.1rem; padding: 0 0 4.4rem; }
  .hero h1 { font-size: clamp(2.7rem, 11.5vw, 4.8rem); }
  .hero-summary { margin-block: .65rem .85rem; font-size: .9rem; }
  .hero-jump { inset: auto auto .7rem 1.1rem; }
  .hero-controls { grid-template-columns: minmax(0, 1fr); row-gap: .45rem; }
  .hero-buttons { justify-content: flex-start; }
}

@media (pointer: coarse) {
  .hero-time input[type='range'] { min-block-size: 36px; }
  .hero-buttons button { min-block-size: 44px; }
}

@media (prefers-reduced-motion: reduce) {
  .hero-landscape, .hero-actions .button, .hero-buttons button { transition: none; }
}
</style>
