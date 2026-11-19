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
      <p>选择星点，进入对应的协会服务。</p>
    </div>

    <div class="constellation">
      <div class="atlas-orbits" aria-hidden="true"></div>
      <ul class="map-links" role="list">
        <li v-for="destination in destinations" :key="destination.id" class="map-node" :class="`node-${destination.id}`">
          <span class="trace" aria-hidden="true"></span>
          <RouterLink :to="destination.to" class="node-link">
            <span class="star" aria-hidden="true"></span>
            <span class="node-label">{{ destination.label }}</span>
          </RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.star-map {
  display: grid;
  grid-template-columns: minmax(12.5rem, .44fr) minmax(0, 1fr);
  align-items: center;
  gap: clamp(.5rem, 3vw, 3rem);
  border-block-end: 1px solid #8496b02c;
  padding-block: clamp(.35rem, 1.5vw, 1.4rem);
  color: var(--text);
}
.map-copy h2 {
  font-size: clamp(1.65rem, 2.5vw, 2.8rem);
  font-weight: 550;
  letter-spacing: -.035em;
  line-height: 1.25;
}
.map-copy p {
  margin-top: .7rem;
  color: var(--muted);
  font-size: .9rem;
}
.constellation {
  position: relative;
  isolation: isolate;
  min-height: clamp(14.5rem, 19vw, 18rem);
}
.atlas-orbits {
  position: absolute;
  z-index: -1;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(circle at 27% 72%, #e4e6ff85 0 1px, transparent 1.5px),
    radial-gradient(circle at 76% 41%, #e4e6ff75 0 1.3px, transparent 2px),
    radial-gradient(circle at 48% 21%, #e4e6ffaa 0 .75px, transparent 1.5px),
    radial-gradient(circle at 15% 41%, #e4e6ff8a 0 .8px, transparent 1.5px),
    radial-gradient(circle at 86% 22%, #e4e6ff75 0 1px, transparent 1.5px),
    radial-gradient(circle at 60% 67%, #e4e6ffaa 0 .7px, transparent 1.4px),
    radial-gradient(ellipse at 53% 52%, #26395e30, transparent 68%);
}
.atlas-orbits::before, .atlas-orbits::after {
  position: absolute;
  content: '';
  border: 1px solid #aeaecc21;
  border-radius: 50%;
  rotate: -9deg;
}
.atlas-orbits::before { inset: 22% -8% 23% -9%; border-style: dashed; opacity: .62; }
.atlas-orbits::after { inset: 17% -12% 11% -17%; border-color: #aeaecc14; }
.map-links { margin: 0; padding: 0; list-style: none; }
.map-node { position: absolute; inset: 0; pointer-events: none; }
.node-competitions { --x: 9%; --y: 58%; --starlight: #f3c19c; }
.node-resources { --x: 28%; --y: 26%; --from-x: 9%; --from-y: 58%; --starlight: #b9ece2; }
.node-resume { --x: 47%; --y: 52%; --from-x: 28%; --from-y: 26%; --starlight: #c2d6fa; }
.node-agent { --x: 68%; --y: 23%; --from-x: 47%; --from-y: 52%; --starlight: #ddc5f6; }
.node-contributors { --x: 86%; --y: 56%; --from-x: 68%; --from-y: 23%; --starlight: #f1d5b6; }
.trace {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, #c6c5df33, #d1d6e38c 50%, #d9d5e54a);
  clip-path: polygon(
    var(--from-x) calc(var(--from-y) - .23%), var(--x) calc(var(--y) - .23%),
    var(--x) calc(var(--y) + .23%), var(--from-x) calc(var(--from-y) + .23%)
  );
}
.node-competitions .trace { display: none; }
.node-link {
  position: absolute;
  top: var(--y);
  left: var(--x);
  display: flex;
  min-height: 46px;
  align-items: center;
  flex-direction: column;
  gap: .62rem;
  padding: 4px .25rem .35rem;
  border-radius: .25rem;
  color: var(--text);
  font-size: clamp(.78rem, .92vw, .95rem);
  font-weight: 500;
  line-height: 1.35;
  white-space: nowrap;
  text-decoration: none;
  translate: -50% -11px;
  pointer-events: auto;
}
.star {
  position: relative;
  flex: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle, #fffaf2 0 8%, var(--starlight) 12% 23%, transparent 31%);
  filter: drop-shadow(0 0 3px var(--starlight));
}
.star::before, .star::after { position: absolute; content: ''; }
.star::before {
  inset: -17%;
  background: var(--starlight);
  clip-path: polygon(50% 0, 57% 42%, 100% 50%, 57% 57%, 50% 100%, 42% 57%, 0 50%, 42% 42%);
  opacity: .9;
}
.star::after {
  inset: -5px;
  border: 1px solid var(--starlight);
  border-radius: 50%;
  opacity: 0;
  scale: .6;
  transition: scale .24s ease, opacity .24s ease;
}
.node-link:is(:hover, :focus-visible) { color: var(--starlight); text-decoration: none; }
.node-link:is(:hover, :focus-visible) .star::after { scale: 1; opacity: .7; }
.node-link:focus-visible { outline: 2px solid var(--starlight); outline-offset: 4px; }
.map-node:has(.node-link:is(:hover, :focus-visible)) .trace { background: var(--starlight); }
:root[data-theme=dark] .atlas-orbits { opacity: .8; }
:root[data-theme=light] .node-competitions { --starlight: #96501f; }
:root[data-theme=light] .node-resources { --starlight: #08776c; }
:root[data-theme=light] .node-resume { --starlight: #386c97; }
:root[data-theme=light] .node-agent { --starlight: #8a4997; }
:root[data-theme=light] .node-contributors { --starlight: #856034; }
:root[data-theme=light] .trace { background: #66808a77; }
@media (min-width: 621px) and (max-width: 880px) {
  .star-map { grid-template-columns: 1fr; gap: 0; }
  .constellation { min-height: 12rem; }
}
@media (max-width: 620px) {
  .star-map { display: block; padding-block: 1rem 1.2rem; }
  .constellation { min-height: 0; margin-top: 1.2rem; }
  .atlas-orbits { opacity: .66; }
  .map-links { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 1rem; }
  .map-node { position: relative; }
  .node-link {
    position: relative;
    inset: auto;
    width: 100%;
    flex-direction: row;
    gap: .55rem;
    border-radius: 0;
    border-top: 1px solid #aab5c533;
    padding: .85rem 0;
    translate: none;
    font-size: .82rem;
    white-space: normal;
  }
  .node-contributors { grid-column: 1 / -1; }
  .trace { display: none; }
  .star { width: .7rem; height: .7rem; }
}
@media (prefers-reduced-motion: reduce) {
  .star::after { transition: none; }
}
</style>
