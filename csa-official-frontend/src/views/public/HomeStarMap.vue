<script setup lang="ts">
import { RouterLink } from 'vue-router'

const destinations = [
  { id: 'competitions', to: '/competitions', label: '竞赛活动' },
  { id: 'resources', to: '/resources', label: '资源库' },
  { id: 'resume', to: '/dashboard/resume', label: '简历工作区' },
  { id: 'agent', to: '/dashboard/ai', label: 'Agent 实验室' },
  { id: 'contributors', to: '/contributors', label: '公开成员与贡献榜' },
] as const
</script>

<template>
  <section class="star-map" aria-labelledby="home-star-map-title">
    <div class="map-copy">
      <h2 id="home-star-map-title">协会功能星图</h2>
      <p>从一个节点出发，找资料、参加竞赛，或和同伴一起做项目。</p>
    </div>

    <div class="constellation">
      <div class="map-core" aria-hidden="true">
        <span class="core-label">CSA</span>
      </div>
      <ul class="map-links" role="list">
        <li v-for="destination in destinations" :key="destination.id" class="map-node" :class="`node-${destination.id}`">
          <span class="node-wire" aria-hidden="true"></span>
          <span class="node-signal" aria-hidden="true"></span>
          <RouterLink class="node-link" :to="destination.to">
            <span class="node-light" aria-hidden="true"></span>
            <span>{{ destination.label }}</span>
            <span class="node-arrow" aria-hidden="true">↗</span>
          </RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.star-map {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(13rem, .75fr) minmax(0, 1.65fr);
  align-items: center;
  gap: .75rem;
  padding: 1.15rem clamp(1rem, 3.6vw, 2.75rem);
  border: 1px solid #2b4d5e;
  border-radius: 1.5rem;
  background:
    radial-gradient(ellipse at 7% 95%, #b552352d, transparent 49%),
    radial-gradient(ellipse at 70% 12%, #22655268, transparent 55%),
    #0b1a2c;
  color: #ecffff;
  box-shadow: 0 1.15rem 2.2rem #07142127;
}

.star-map::before {
  position: absolute;
  z-index: -1;
  inset: 0;
  border-radius: inherit;
  background:
    radial-gradient(circle at 17% 31%, #acf8ed76 0 1px, transparent 1.6px) 0 0 / 93px 67px,
    radial-gradient(circle at 68% 57%, #add4f03c 0 1px, transparent 1.6px) 18px 7px / 71px 87px,
    linear-gradient(#8bcadd12 1px, transparent 1px) 50% 50% / 2rem 2rem,
    linear-gradient(90deg, #8bcadd12 1px, transparent 1px) 50% 50% / 2rem 2rem;
  mask-image: radial-gradient(ellipse at 69% 54%, #000 20%, #0007 65%, transparent 91%);
  content: '';
  pointer-events: none;
}

.map-copy h2 {
  color: #f2ffff;
  font-size: clamp(1.65rem, 3.6vw, 3.4rem);
  line-height: 1.15;
  letter-spacing: -.045em;
}

.map-copy p {
  max-width: 20em;
  margin-top: .9rem;
  color: #bbd7dd;
}

.constellation {
  position: relative;
  isolation: isolate;
  min-width: 0;
  min-height: 21.5rem;
}

.map-core {
  position: absolute;
  z-index: 1;
  top: 48%;
  left: 50%;
  display: grid;
  place-items: center;
  width: clamp(8.5rem, 18vw, 12.5rem);
  aspect-ratio: 1;
  border: 1px solid #7bfada60;
  border-radius: 50%;
  background: radial-gradient(circle, #0f4241 0 38%, #1a79663b 39% 53%, #24866d1c 54%, transparent 71%);
  box-shadow: 0 0 2rem #3edfbe30;
  translate: -50% -50%;
  pointer-events: none;
}

.map-core::before,
.map-core::after {
  position: absolute;
  border-radius: 50%;
  content: '';
}

.map-core::before {
  inset: 2%;
  background: repeating-conic-gradient(from 1deg, #a0ffeaa3 0 1deg, transparent 1deg 12deg);
  mask-image: radial-gradient(circle, transparent 66%, #000 68.5%, transparent 74%);
}

.map-core::after {
  inset: 12%;
  border: 1px dashed #8bf7d06d;
  box-shadow: inset 0 0 1rem #4cfbc126;
}

.core-label {
  color: #c9fff2;
  font: 700 clamp(1.2rem, 3.4vw, 2rem) / 1 'Cascadia Code', Consolas, monospace;
  letter-spacing: .16em;
  text-indent: .16em;
  text-shadow: 0 0 .8rem #82ffc59c;
}

.map-links {
  margin: 0;
  padding: 0;
  list-style: none;
}

.map-node {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.node-competitions {
  --node-x: 15%;
  --node-y: 22%;
  --bend-x: 33%;
  --bend-y: 22%;
  --node-color: #ffcb92;
  --wire-shape: polygon(15% 21.8%, 33% 21.8%, 50% 47.8%, 50% 48.2%, 33% 22.2%, 15% 22.2%);
}

.node-resources {
  --node-x: 85%;
  --node-y: 17%;
  --bend-x: 68%;
  --bend-y: 17%;
  --node-color: #a2ffe6;
  --wire-shape: polygon(50% 47.8%, 68% 16.8%, 85% 16.8%, 85% 17.2%, 68% 17.2%, 50% 48.2%);
}

.node-resume {
  --node-x: 15%;
  --node-y: 75%;
  --bend-x: 34%;
  --bend-y: 75%;
  --node-color: #a9dcff;
  --wire-shape: polygon(15% 74.8%, 34% 74.8%, 50% 47.8%, 50% 48.2%, 34% 75.2%, 15% 75.2%);
}

.node-agent {
  --node-x: 85%;
  --node-y: 72%;
  --bend-x: 68%;
  --bend-y: 72%;
  --node-color: #ddc1ff;
  --wire-shape: polygon(50% 47.8%, 68% 71.8%, 85% 71.8%, 85% 72.2%, 68% 72.2%, 50% 48.2%);
}

.node-contributors {
  --node-x: 50%;
  --node-y: 90%;
  --bend-x: 50%;
  --bend-y: 75%;
  --node-color: #91f0ff;
  --wire-shape: polygon(49.9% 48%, 50.1% 48%, 50.1% 90%, 49.9% 90%);
}

.node-wire {
  position: absolute;
  inset: 0;
  background: var(--node-color);
  clip-path: var(--wire-shape);
  opacity: .48;
  transition: opacity .2s ease;
}

.node-signal {
  position: absolute;
  z-index: 2;
  top: 48%;
  left: 50%;
  width: .36rem;
  aspect-ratio: 1;
  border-radius: 50%;
  background: var(--node-color);
  box-shadow: 0 0 .65rem var(--node-color);
  opacity: 0;
  translate: -50% -50%;
}

.node-link {
  position: absolute;
  z-index: 3;
  top: var(--node-y);
  left: var(--node-x);
  display: flex;
  min-height: 2.95rem;
  align-items: center;
  gap: .56rem;
  padding: .52rem .7rem;
  border: 1px solid color-mix(in srgb, var(--node-color) 39%, #253947);
  border-radius: .53rem;
  background: #112a38;
  color: #f3fdff;
  box-shadow: 0 .3rem 1rem #02111b75;
  font-size: .9rem;
  font-weight: 650;
  line-height: 1.35;
  white-space: nowrap;
  translate: -50% -50%;
  pointer-events: auto;
  transition: border-color .2s ease, background .2s ease, box-shadow .2s ease, scale .13s ease;
}

.node-light {
  width: .36rem;
  aspect-ratio: 1;
  flex: none;
  border-radius: 2px;
  background: var(--node-color);
  box-shadow: 0 0 .45rem var(--node-color);
}

.node-arrow { margin-left: auto; color: var(--node-color); }

.map-node:has(.node-link:is(:hover, :focus-visible)) .node-wire { opacity: 1; }

.map-node:has(.node-link:is(:hover, :focus-visible)) .node-signal {
  animation: star-route-signal 1s ease-out 2;
}

.node-link:is(:hover, :focus-visible) {
  border-color: var(--node-color);
  background: color-mix(in srgb, #112a38 82%, var(--node-color));
  box-shadow: 0 .35rem 1.1rem #02111b80, 0 0 1rem color-mix(in srgb, var(--node-color) 25%, transparent);
  text-decoration: none;
}

.node-link:focus-visible { outline-color: var(--node-color); }

.node-link:active {
  background: color-mix(in srgb, #112a38 72%, var(--node-color));
  scale: .97;
}

@keyframes star-route-signal {
  0% { top: 48%; left: 50%; opacity: 0; }
  18% { opacity: 1; }
  59% { top: var(--bend-y); left: var(--bend-x); opacity: 1; }
  100% { top: var(--node-y); left: var(--node-x); opacity: .1; }
}

@media (max-width: 860px) {
  .star-map { grid-template-columns: 1fr; padding: 1.5rem 1.15rem 1rem; }
  .map-copy p { margin-top: .5rem; }
}

@media (max-width: 620px) {
  .star-map { gap: .9rem; padding: 1.25rem .9rem 1rem; }
  .constellation { min-height: 0; }
  .map-core { position: relative; top: auto; left: auto; width: 7.4rem; margin: 0 auto 1rem; translate: none; }
  .map-links { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .55rem; }
  .map-node { position: static; display: flex; }
  .node-contributors { grid-column: 1 / -1; }
  .node-wire, .node-signal { display: none; }
  .node-link { position: static; width: 100%; font-size: .8rem; white-space: normal; translate: none; }
}

@media (prefers-reduced-motion: reduce) {
  .node-wire, .node-link { transition: none; }
  .map-node:has(.node-link:is(:hover, :focus-visible)) .node-signal { animation: none; }
  .node-link:active { scale: none; }
}
</style>
