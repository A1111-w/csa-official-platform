<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'

type Motif = 'circuit' | 'network' | 'meteors' | 'layers' | 'shield'
type Point = readonly [number, number]

const props = defineProps<{ motif: Motif; title: string; description: string }>()
const art = ref<HTMLCanvasElement | null>(null)
const palettes: Record<Motif, [string, string]> = {
  circuit: ['#84ffe3', '#89baff'], network: ['#edb0ff', '#9dbdff'],
  meteors: ['#ffc17e', '#ff83a8'], layers: ['#9dd3ff', '#baacff'], shield: ['#a2eed0', '#c8d98c'],
}

let ctx: CanvasRenderingContext2D | null = null
let frame = 0
let visible = true
let elapsed = 0
let previous = 0
let preference: MediaQueryList | undefined
let observer: IntersectionObserver | undefined

function line(context: CanvasRenderingContext2D, points: readonly Point[], color: string, width = 1) {
  context.beginPath()
  points.forEach(([x, y], index) => { if (index === 0) context.moveTo(x, y); else context.lineTo(x, y) })
  context.strokeStyle = color
  context.lineWidth = width
  context.stroke()
}
function dot(context: CanvasRenderingContext2D, x: number, y: number, radius: number, color: string) {
  context.fillStyle = color
  context.beginPath()
  context.arc(x, y, radius, 0, 2 * Math.PI)
  context.fill()
}

const network: Point[] = [[46,79],[84,44],[101,113],[147,73],[172,28],[184,122],[218,91],[257,51]]
const links = [[0,1],[0,2],[1,2],[1,3],[2,3],[2,5],[3,4],[3,5],[3,6],[4,7],[5,6],[6,7]]
const shield: Point[] = [[154,26],[204,44],[201,82],[184,105],[154,124],[124,105],[107,82],[104,44],[154,26]]

function draw() {
  const context = ctx
  if (!context) return
  const [light, secondary] = palettes[props.motif]
  const t = elapsed / 1000
  context.clearRect(0, 0, 300, 150)
  for (let i = 0; i < 48; i++) {
    const x = (i * 103 + i * i * 17 + 23) % 300
    const y = (i * 47 + i * i * 3 + 29) % 150
    dot(context, x, y, i % 7 ? .55 : 1.05, i % 7 ? light + '74' : secondary + 'aa')
  }

  if (props.motif === 'circuit') {
    for (const side of [-1, 1]) {
      for (let lane = 0; lane < 4; lane++) {
        const y = 51 + lane * 15
        const edge = 150 + side * 25
        const elbow = edge + side * (21 + (lane % 2) * 14)
        const end = 150 + side * (91 + lane * 5)
        const channel = y + (lane - 1.5) * 13
        const trace: Point[] = [[edge,y],[elbow,y],[elbow + side * Math.abs(channel - y),channel],[end,channel]]
        line(context, trace, (lane % 2 ? light : secondary) + '80')
        dot(context, end, channel, 2, light)
        const travel = (t * .33 + lane * .28 + (side + 1) * .18) % 1
        const segment = Math.floor(travel * 3)
        const mix = travel * 3 - segment
        const [a,b] = [trace[segment],trace[segment + 1]]
        dot(context, a[0] + (b[0] - a[0]) * mix, a[1] + (b[1] - a[1]) * mix, 1.8, '#efffff')
      }
    }
    context.strokeStyle = light
    context.strokeRect(125, 49, 50, 50)
    context.strokeStyle = secondary + '9c'
    context.strokeRect(132, 56, 36, 36)
    for (let x = 141; x <= 159; x += 9) for (let y = 65; y <= 83; y += 9) dot(context, x, y, 1.5, light)
  } else if (props.motif === 'network') {
    links.forEach(([a,b]) => line(context, [network[a],network[b]], secondary + '76'))
    network.forEach(([x,y], i) => {
      const pulse = (t * .29 + i * .23) % 1
      context.globalAlpha = .46 * (1 - pulse)
      context.strokeStyle = i % 2 ? secondary : light
      context.beginPath()
      context.arc(x, y, 4 + pulse * 9, 0, 2 * Math.PI)
      context.stroke()
      context.globalAlpha = 1
      dot(context, x, y, i === 3 ? 4 : 2.5, i % 2 ? light : '#f9e3ff')
    })
  } else if (props.motif === 'meteors') {
    for (let i = 0; i < 6; i++) {
      const travel = (t * (.10 + i * .013) + i * .217) % 1
      const x = 320 - travel * 340
      const y = 4 + i * 12 + travel * 85
      const length = 24 + (i % 3) * 19
      const trail = context.createLinearGradient(x,y,x + length,y - length * .41)
      trail.addColorStop(0, i % 2 ? light : secondary)
      trail.addColorStop(1, '#ffffff00')
      context.strokeStyle = trail
      context.lineWidth = i % 3 === 0 ? 1.8 : 1.1
      context.beginPath()
      context.moveTo(x, y)
      context.lineTo(x + length, y - length * .41)
      context.stroke()
      dot(context, x, y, i % 3 ? 1.3 : 2.3, '#fff4d8')
    }
  } else if (props.motif === 'layers') {
    for (let i = 0; i < 4; i++) {
      const y = 99 - i * 18 + Math.sin(t + i * .55) * 2.5
      const sheet: Point[] = [[80,y],[155,y - 28],[230,y],[155,y + 28],[80,y]]
      line(context, sheet, i === 3 ? light : secondary + 'ad', i === 3 ? 1.5 : 1)
      context.fillStyle = i === 3 ? light + '16' : secondary + '09'
      context.fill()
      dot(context, 80, y, 2, light)
      dot(context, 230, y, 2, secondary)
      dot(context, 155, y + 28, 2, '#e2f7ff')
    }
  } else {
    line(context, shield, light, 1.4)
    line(context, shield.map(([x,y]): Point => [154 + (x - 154) * .82,74 + (y - 74) * .82]), secondary + '87')
    const tick = .5 + .5 * Math.cos(t * .8)
    line(context, [[139,75],[150,85],[172,62]], '#d7ffe6', 2)
    shield.slice(0,-1).forEach(([x,y]) => dot(context,x,y,1.8,light))
    context.save()
    context.beginPath()
    shield.forEach(([x,y],i) => { if (!i) context.moveTo(x,y); else context.lineTo(x,y) })
    context.clip()
    const y = 30 + tick * 90
    const glow = context.createLinearGradient(0, y - 12, 0, y)
    glow.addColorStop(0, '#a2eed000')
    glow.addColorStop(1, '#a2eed033')
    context.fillStyle = glow
    context.fillRect(100, y - 12, 110, 12)
    line(context, [[100,y],[208,y]], light + '60')
    context.restore()
  }
}

function tick(now: number) {
  if (!previous || now - previous >= 40) {
    if (previous) elapsed += Math.min(now - previous, 100)
    previous = now
    draw()
  }
  frame = requestAnimationFrame(tick)
}
function synchronize() {
  if (frame) cancelAnimationFrame(frame)
  frame = 0
  previous = 0
  if (!ctx || !visible || document.hidden) return
  draw()
  if (!preference?.matches) frame = requestAnimationFrame(tick)
}
onMounted(() => {
  if (!art.value || typeof CanvasRenderingContext2D === 'undefined') return
  ctx = art.value.getContext('2d')
  if (!ctx) return
  ctx.scale(2, 2)
  preference = window.matchMedia?.('(prefers-reduced-motion: reduce)')
  preference?.addEventListener('change', synchronize)
  document.addEventListener('visibilitychange', synchronize)
  if (typeof IntersectionObserver !== 'undefined') {
    observer = new IntersectionObserver(([entry]) => { visible = entry.isIntersecting; synchronize() })
    observer.observe(art.value)
  }
  synchronize()
})
watch(() => props.motif, synchronize)
onUnmounted(() => {
  if (frame) cancelAnimationFrame(frame)
  observer?.disconnect()
  preference?.removeEventListener('change', synchronize)
  document.removeEventListener('visibilitychange', synchronize)
})
</script>

<template>
  <header class="public-sky-heading" :data-motif="motif">
    <div class="heading-copy">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <slot />
    </div>
    <canvas ref="art" width="600" height="300" aria-hidden="true"></canvas>
  </header>
</template>

<style scoped>
.public-sky-heading {
  --glow: #367c80;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(13rem, .75fr);
  align-items: center;
  gap: 1rem;
  position: relative;
  overflow: hidden;
  min-height: 10rem;
  padding: clamp(1.25rem, 3vw, 2.35rem);
  isolation: isolate;
  background: radial-gradient(ellipse at 82% 95%, color-mix(in srgb, var(--glow) 64%, transparent), transparent 59%), #101724;
  border-bottom: 1px solid color-mix(in srgb, var(--glow) 75%, #fff);
}
.public-sky-heading[data-motif='network'] { --glow: #63476f; }
.public-sky-heading[data-motif='meteors'] { --glow: #8c4b3c; }
.public-sky-heading[data-motif='layers'] { --glow: #4c5584; }
.public-sky-heading[data-motif='shield'] { --glow: #3c654f; }
.heading-copy { position: relative; z-index: 1; }
.heading-copy h1 { color: #f6f8f8; }
.heading-copy p, .heading-copy :slotted(p) { color: #d3dee5; font-size: .95rem; }
.public-sky-heading canvas { display: block; width: min(100%, 20rem); justify-self: end; height: auto; pointer-events: none; }
@media (max-width: 620px) {
  .public-sky-heading { grid-template-columns: 1fr; min-height: 10.5rem; align-items: end; }
  .public-sky-heading canvas { position: absolute; right: .2rem; top: -.8rem; width: 11.5rem; opacity: .76; }
  .heading-copy { padding-top: 3rem; }
  .heading-copy p, .heading-copy :slotted(p) { font-size: .86rem; }
}
</style>
