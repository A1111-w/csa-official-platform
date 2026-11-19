import type { Daypart } from './solar-theme'

type Color = number[]
type ColorKey = 'shadow' | 'sea' | 'bright' | 'ice' | 'rim' | 'ring' | 'ember'
type Palette = Record<ColorKey, Color> & { light: number[]; ambient: number; stars: number }
type Lighting = { key: string | number; palette: Palette }
type Star = { x: number; y: number; radius: number; phase: number }
type Point = [number, number]

export type PlanetRipple = { x: number; y: number; age: number }
export type PlanetFrame = {
  yaw: number
  pitch: number
  parallaxX: number
  parallaxY: number
  time: number
  daypart: Daypart
  minute: number
  ripples: PlanetRipple[]
  formed: number
}

const letterPaths: Point[][] = [
  [[.27, .30], [.21, .17], [.105, .27], [.064, .49], [.107, .73], [.21, .83], [.277, .69]],
  [[.59, .28], [.52, .15], [.425, .27], [.409, .42], [.553, .56], [.603, .71], [.548, .85], [.433, .77], [.403, .67]],
  [[.702, .83], [.814, .17], [.959, .83]],
  [[.743, .60], [.914, .60]],
]

export function createPlanetScene(canvas: HTMLCanvasElement) {
  if (typeof CanvasRenderingContext2D === 'undefined') return null
  const drawingContext = canvas.getContext('2d')
  if (!drawingContext) return null
  const context: CanvasRenderingContext2D = drawingContext
  const tau = Math.PI * 2
  const clamp = (value: number, minimum: number, maximum: number) => Math.min(maximum, Math.max(minimum, value))
  const mix = (a: number, b: number, ratio: number) => a + (b - a) * ratio
  const color = (rgb: Color, alpha = 1) => `rgba(${rgb[0]},${rgb[1]},${rgb[2]},${alpha})`
  const palettes: Record<Daypart, Palette> = {
    dawn: { shadow: [13, 19, 39], sea: [177, 126, 112], bright: [255, 229, 193], ice: [173, 137, 154], rim: [203, 221, 250], ring: [255, 229, 197], ember: [255, 187, 157], light: [-.79, -.56, .245], ambient: .11, stars: .78 },
    day: { shadow: [20, 28, 47], sea: [170, 149, 136], bright: [255, 244, 210], ice: [148, 158, 179], rim: [187, 228, 248], ring: [250, 233, 204], ember: [255, 207, 143], light: [-.65, -.63, .425], ambient: .14, stars: .57 },
    dusk: { shadow: [24, 17, 40], sea: [190, 128, 111], bright: [255, 226, 189], ice: [162, 127, 148], rim: [240, 205, 226], ring: [255, 214, 175], ember: [255, 171, 124], light: [-.81, -.53, .249], ambient: .11, stars: .88 },
    night: { shadow: [12, 14, 32], sea: [170, 134, 132], bright: [255, 228, 196], ice: [150, 130, 161], rim: [194, 208, 245], ring: [244, 220, 192], ember: [255, 197, 136], light: [-.85, -.48, .216], ambient: .10, stars: 1 },
  }
  const colorKeys: ColorKey[] = ['shadow', 'sea', 'bright', 'ice', 'rim', 'ring', 'ember']
  const solarStops: [number, Daypart][] = [
    [0, 'night'], [265, 'night'], [425, 'dawn'], [675, 'day'],
    [875, 'day'], [1095, 'dusk'], [1320, 'night'], [1440, 'night'],
  ]
  let paletteCache: Lighting | undefined

  function paletteAt(minute: number, daypart: Daypart): Lighting {
    if (!Number.isFinite(minute)) {
      const key = Object.hasOwn(palettes, daypart) ? daypart : 'night'
      return { key, palette: palettes[key] }
    }
    const sample = (Math.round(clamp(minute, 0, 1439) / 3) * 3) % 1440
    if (paletteCache?.key === sample) return paletteCache
    const upper = solarStops.findIndex(([time]) => time > sample)
    const [start, before] = solarStops[upper - 1]
    const [end, after] = solarStops[upper]
    if (before === after) return { key: before, palette: palettes[before] }
    const time = (sample - start) / (end - start)
    const blend = time * time * (3 - 2 * time)
    const previous = palettes[before]
    const next = palettes[after]
    const colors = { ...previous }
    for (const name of colorKeys) {
      colors[name] = previous[name].map((channel, index) => Math.round(mix(channel, next[name][index], blend)))
    }
    const palette: Palette = {
      ...colors,
      light: previous.light.map((strength, index) => mix(strength, next.light[index], blend)),
      ambient: mix(previous.ambient, next.ambient, blend),
      stars: mix(previous.stars, next.stars, blend),
    }
    paletteCache = { key: sample, palette }
    return paletteCache
  }


  // Ring optical density is sampled along the equatorial plane. The transparent
  // Cassini division and dim C ring stay visible against both sky and planet.
  const ringInner = 1.20
  const ringOuter = 2.32
  const ringSamples = 1024
  const ringDensity = new Float32Array(ringSamples)
  const ringTone = new Float32Array(ringSamples)
  function smooth(min: number, max: number, value: number) {
    const t = clamp((value - min) / (max - min), 0, 1)
    return t * t * (3 - 2 * t)
  }
  for (let i = 0; i < ringSamples; i++) {
    const r = mix(ringInner, ringOuter, i / (ringSamples - 1))
    const cRing = .27 * smooth(1.20, 1.48, r) * (1 - smooth(1.51, 1.55, r))
    const bRing = .94 * smooth(1.51, 1.57, r) * (1 - smooth(1.921, 1.945, r))
    const aRing = .67 * smooth(1.984, 1.998, r) * (1 - smooth(2.22, 2.26, r))
    const fRing = .3 * Math.exp(-((r - 2.293) ** 2) / .000011)
    const lanes = .84 + .085 * Math.sin(r * 125 + Math.sin(r * 49)) + .065 * Math.sin(r * 376) + .035 * Math.sin(r * 853)
    ringDensity[i] = (cRing + bRing + aRing + fRing) * lanes
    ringTone[i] = clamp(.31 + .28 * Math.cos(r * 26) + .22 * lanes + .15 * Math.sin(r * 113), 0, 1)
  }

  const planetSprite = document.createElement('canvas')
  const sphereContext = planetSprite.getContext('2d')
  if (!sphereContext) return null
  const sphereDrawing: CanvasRenderingContext2D = sphereContext
  let spriteKey = ''
  let spritePixels: ImageData
  let spriteScale = 0
  let width = 0
  let height = 0
  let pixelRatio = 1
  let stars: Star[] = []
  let beacons: Star[] = []
  let letterDots: Point[] = []
  let letterWidth = 0
  let letterHeight = 0
  const starMap = document.createElement('canvas')
  const starContext = starMap.getContext('2d')
  if (!starContext) return null
  const starDrawing: CanvasRenderingContext2D = starContext
  const starColors = ['#f5ebcf', '#adcfff', '#f7d3b4', '#ebf4ff', '#d2c5ff']
  const starPadding = 12

  function makeStars() {
    const scale = Math.min(pixelRatio, 1.5)
    const sw = width + 2 * starPadding
    const sh = height + 2 * starPadding
    starMap.width = Math.ceil(sw * scale)
    starMap.height = Math.ceil(sh * scale)
    starDrawing.setTransform(scale, 0, 0, scale, starPadding * scale, starPadding * scale)
    let seed = 7919
    const random = () => ((seed = (seed * 1664525 + 1013904223) >>> 0) / 4294967296)
    const count = clamp(Math.round(width * height / 1020), 180, 1400)
    stars = []
    beacons = []
    for (let i = 0; i < count; i++) {
      const x = random()
      const onBand = i % 3 === 0
      const y = onBand ? .03 + x * .27 + (random() + random() + random() - 1.5) * .13 : random() * .79
      const rank = random()
      const radius = .33 + Math.pow(rank, 7) * 1.02
      const phase = random() * tau
      const star = { x, y, radius, phase }
      stars.push(star)
      const colorIndex = Math.floor(random() * starColors.length)
      starDrawing.fillStyle = starColors[colorIndex]
      const fade = 1 - smooth(.54, .79, y)
      starDrawing.globalAlpha = (.3 + .65 * rank) * fade
      starDrawing.beginPath()
      starDrawing.arc(x * width, y * height, radius, 0, tau)
      starDrawing.fill()
      if (rank > .98 && y < .53) beacons.push(star)
    }
    starDrawing.globalAlpha = 1
    // A small pair of connected angle brackets references the association's
    // coding work. Unconnected stars and the active CSA formation stay distinct.
    for (const points of [
      [[.486, .133], [.463, .182], [.478, .224]],
      [[.514, .152], [.53, .185], [.511, .222]],
      [[.876, .315], [.91, .273], [.929, .291], [.954, .25]],
    ]) {
      starDrawing.beginPath()
      points.forEach(([x, y], i) => {
        if (!i) starDrawing.moveTo(x * width, y * height)
        else starDrawing.lineTo(x * width, y * height)
      })
      starDrawing.strokeStyle = 'rgba(174,199,239,.22)'
      starDrawing.lineWidth = .65
      starDrawing.stroke()
      for (const [x, y] of points) {
        starDrawing.fillStyle = 'rgba(225,225,247,.82)'
        starDrawing.beginPath()
        starDrawing.arc(x * width, y * height, 1.1, 0, tau)
        starDrawing.fill()
      }
    }
  }

  function sphereRadius() {
    return width < 600 ? Math.min(width * .125, height * .101) : Math.min(width * .083, height * .133, 111)
  }

  function resize() {
    const rect = canvas.getBoundingClientRect()
    const nextWidth = Math.round(rect.width)
    const nextHeight = Math.round(rect.height)
    if (!nextWidth || !nextHeight) return false
    const nextRatio = Math.min(window.devicePixelRatio || 1, 1.75)
    if (width === nextWidth && height === nextHeight && pixelRatio === nextRatio) return true
    width = nextWidth
    height = nextHeight
    pixelRatio = nextRatio
    canvas.width = Math.round(width * pixelRatio)
    canvas.height = Math.round(height * pixelRatio)
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    const mobile = width < 600
    spriteScale = Math.ceil(sphereRadius() * Math.max(1.2, pixelRatio))
    planetSprite.width = Math.ceil(spriteScale * 4.72)
    planetSprite.height = Math.ceil(spriteScale * 2.48)
    spritePixels = sphereDrawing.createImageData(planetSprite.width, planetSprite.height)
    spriteKey = ''
    makeStars()
    letterWidth = Math.min(width * (mobile ? .44 : .22), 260)
    letterHeight = letterWidth / 3.1
    letterDots = letterPaths.flatMap(path => {
      const dots = path.slice(1).flatMap((end, index) => {
        const start = path[index]
        const distance = Math.hypot((end[0] - start[0]) * letterWidth, (end[1] - start[1]) * letterHeight)
        const count = Math.max(2, Math.ceil(distance / (mobile ? 4.4 : 5)))
        return Array.from({ length: count }, (_, step): Point => {
          const ratio = step / count
          return [mix(start[0], end[0], ratio), mix(start[1], end[1], ratio)]
        })
      })
      return [...dots, path[path.length - 1]]
    })

    return true
  }

  function ringIndex(r: number) {
    return Math.round((r - ringInner) / (ringOuter - ringInner) * (ringSamples - 1))
  }

  function renderPlanet(light: Lighting, squash: number, spin: number) {
    const key = [light.key, squash.toFixed(2), spin].join('/')
    if (spriteKey === key) return
    spriteKey = key
    const pixels = spritePixels.data
    const sw = planetSprite.width
    const sh = planetSprite.height
    const p = light.palette
    const [lx, ly, lz] = p.light
    const c = Math.sqrt(1 - squash * squash)
    // World-space plane normal points towards the observer. At a given screen
    // pixel we compare the ring's z with the globe's z for true occultation.
    const lightDotNormal = -c * ly + squash * lz
    const grainScale = 1 / spriteScale
    for (let py = 0; py < sh; py++) {
      const y = (py + .5 - sh / 2) * grainScale
      const ringZ = c * y / squash
      const ringY = y / squash
      for (let px = 0; px < sw; px++) {
        const x = (px + .5 - sw / 2) * grainScale
        const idx = (py * sw + px) * 4
        const r2 = x * x + (y / .946) ** 2
        const onDisc = r2 < 1
        const z = onDisc ? Math.sqrt(1 - r2) : 0
        const ringDistance = Math.sqrt(x * x + ringY * ringY)
        const ringSample = ringIndex(ringDistance)
        const density = ringDensity[ringSample] || 0
        const foregroundRing = density > .015 && (!onDisc || ringZ > z)
        if (!onDisc && !foregroundRing) {
          pixels[idx + 3] = 0
          continue
        }

        let surfaceLight = 0
        let surfaceTone = 0
        let limb = 0
        let sphereAlpha = 0
        if (onDisc) {
          const latitude = Math.asin(clamp(-c * y + squash * z, -1, 1))
          const longitude = Math.atan2(x, z) + spin * .24
          const drift = .009 * Math.sin(longitude * 4 + latitude * 9) + .005 * Math.sin(longitude * 12 - latitude * 24)
          const lat = latitude + drift
          surfaceTone = .50 + .16 * Math.sin(lat * 20) + .09 * Math.sin(lat * 45 + .7)
            + .06 * Math.sin(lat * 81) + .026 * Math.sin(lat * 163 + longitude)
          const incidence = x * lx + y * ly + z * lz
          surfaceLight = p.ambient + (1 - p.ambient) * Math.pow(Math.max(0, incidence), .62)
          const t = -(-c * y + squash * z) / lightDotNormal
          if (t > 0) {
            const intersection = Math.hypot(x + t * lx, y + t * ly, z + t * lz)
            surfaceLight *= 1 - .71 * (ringDensity[ringIndex(intersection)] || 0)
          }
          limb = (1 - z) ** 4 * Math.max(0, incidence + .15) * .32
          sphereAlpha = clamp((1 - Math.sqrt(r2)) * spriteScale + .5, 0, 1)
        }

        let ringLight = 0
        let ringAlpha = 0
        if (foregroundRing) {
          const projection = x * lx + y * ly + ringZ * lz
          const separation = ringDistance * ringDistance - projection * projection
          const shadow = projection < 0 ? smooth(.83, 1.1, separation) : 1
          const grain = Math.sin(px * 53.27 + py * 77.13) * .035
          ringLight = (.22 + .78 * shadow) * (.91 + grain)
          ringAlpha = clamp(density * 1.12, 0, .98)
        }
        const alpha = ringAlpha + sphereAlpha * (1 - ringAlpha)
        for (let k = 0; k < 3; k++) {
          const base = mix(p.sea[k], p.bright[k], surfaceTone)
          const discColor = mix(mix(p.shadow[k], base, surfaceLight), p.rim[k], limb)
          const dust = foregroundRing ? mix(p.ice[k], p.ring[k], ringTone[ringSample]) * ringLight : 0
          pixels[idx + k] = (dust * ringAlpha + discColor * sphereAlpha * (1 - ringAlpha)) / alpha
        }
        pixels[idx + 3] = alpha * 255
      }
    }
    sphereDrawing.putImageData(spritePixels, 0, 0)
  }

  function paintMoon(x: number, y: number, r: number, palette: Palette) {
    const light = context.createRadialGradient(x - .72 * r, y - .42 * r, .08 * r, x - .42 * r, y - .32 * r, r * 1.18)
    light.addColorStop(0, '#fff2df')
    light.addColorStop(.36, color(palette.ring))
    light.addColorStop(.56, '#95877e')
    light.addColorStop(.69, '#544d5d')
    light.addColorStop(.76, '#272839')
    light.addColorStop(1, '#171a2b')
    context.fillStyle = light
    context.beginPath()
    context.arc(x, y, r, 0, tau)
    context.fill()
    context.fillStyle = 'rgba(80,67,70,.22)'
    for (const [dx, dy, size] of [[-.45, -.4, .16], [-.62, -.04, .12], [-.21, -.58, .1], [-.33, .05, .13]]) {
      context.beginPath()
      context.arc(x + dx * r, y + dy * r, size * r, 0, tau)
      context.fill()
    }
  }

  function paintStars(palette: Palette, time: number, offsetX: number, offsetY: number) {
    const dx = offsetX * .19
    const dy = offsetY * .16
    context.globalAlpha = palette.stars
    context.drawImage(starMap, dx - starPadding, dy - starPadding, width + starPadding * 2, height + starPadding * 2)
    context.fillStyle = '#e7e7ff'
    for (const { x, y, phase } of beacons) {
      const sx = x * width + dx
      const sy = y * height + dy
      context.globalAlpha = palette.stars * (.32 + .16 * Math.sin(time * 1.1 + phase))
      context.fillRect(sx - 3.4, sy - .36, 6.8, .72)
      context.fillRect(sx - .33, sy - 2.6, .66, 5.2)
    }
    context.globalAlpha = 1
    const phase = (time + 6) % 14
    if (phase >= 1.2) return
    const t = phase / 1.2
    const pass = Math.floor((time + 6) / 14)
    const sx = width * (.42 + (pass * .217 % .30) + t * .09)
    const sy = height * (.06 + .12 * t)
    const length = Math.min(88, width * .09) * Math.sin(Math.PI * t)
    const tail = context.createLinearGradient(sx - length, sy - length * .48, sx, sy)
    tail.addColorStop(0, 'rgba(147,183,242,0)')
    tail.addColorStop(.75, 'rgba(192,207,249,.25)')
    tail.addColorStop(1, 'rgba(255,240,220,.85)')
    context.strokeStyle = tail
    context.globalAlpha = Math.sin(Math.PI * t) * palette.stars
    context.lineWidth = 1.15
    context.beginPath()
    context.moveTo(sx - length, sy - length * .48)
    context.lineTo(sx, sy)
    context.stroke()
    context.globalAlpha = 1
  }
  function paintRipples(ripples: PlanetRipple[], palette: Palette) {
    for (const { x, y, age } of ripples) {
      if (age < 0 || age > 1.9 || !Number.isFinite(x) || !Number.isFinite(y)) continue
      const strength = (1 - age / 1.9) ** 2
      const size = 6 + age * 114
      const prism = context.createLinearGradient(x - size, y, x + size, y)
      prism.addColorStop(0, color(palette.sea, strength * .53))
      prism.addColorStop(.28, color(palette.rim, strength * .85))
      prism.addColorStop(.56, color(palette.ember, strength * .78))
      prism.addColorStop(1, color(palette.ice, strength * .25))
      context.strokeStyle = prism
      context.lineWidth = 1.05 + strength * 1.55
      context.beginPath()
      context.ellipse(x, y, size, size * .44, -.08, 0, tau)
      context.stroke()
      if (age > .11) {
        context.strokeStyle = color(palette.sea, strength * .39)
        context.beginPath()
        context.ellipse(x, y, size * .72, size * .32, -.08, 0, tau)
        context.stroke()
      }
      for (let point = 0; point < 14; point += 1) {
        const angle = (point * .618) % 1 * tau
        const distance = size * (.78 + point % 3 * .23)
        const pointX = x + Math.cos(angle) * distance
        const pointY = y + Math.sin(angle) * distance * .48
        const hue = [palette.rim, palette.ember, palette.ice][point % 3]
        context.fillStyle = color(hue, strength * .74)
        context.beginPath()
        context.arc(pointX, pointY, .75 + strength * 1.1, 0, tau)
        context.fill()
      }
    }
  }

  function paintConstellation(progress: number, palette: Palette) {
    if (progress < .008) return
    const mobile = width < 600
    const x = width * (mobile ? .71 : .74)
    const y = height * (mobile ? .40 : .67)
    const left = x - letterWidth / 2
    const top = y - letterHeight / 2
    const glow = context.createRadialGradient(x, y, letterHeight * .35, x, y, letterWidth * .70)
    glow.addColorStop(0, color(palette.shadow, progress * .88))
    glow.addColorStop(.57, color(palette.shadow, progress * .56))
    glow.addColorStop(1, color(palette.shadow, 0))
    context.fillStyle = glow
    context.fillRect(left - letterWidth * .23, top - letterHeight, letterWidth * 1.46, letterHeight * 3)

    const clarity = clamp((progress - .73) / .27, 0, 1)
    context.strokeStyle = color(palette.rim, clarity * .72)
    context.lineWidth = mobile ? .75 : 1.10
    for (const path of letterPaths) {
      context.beginPath()
      context.moveTo(left + path[0][0] * letterWidth, top + path[0][1] * letterHeight)
      for (const [u, v] of path.slice(1)) context.lineTo(left + u * letterWidth, top + v * letterHeight)
      context.stroke()
    }

    for (const [i, [u, v]] of letterDots.entries()) {
      const star = stars[i % stars.length]
      const position = Math.min(1, progress * (1.03 + i % 7 * .04))
      const shed = (1 - position) * Math.sin(i * 1.94 + position * Math.PI) * (mobile ? 14 : 32)
      const pointX = mix(star.x * width, left + u * letterWidth, position) + shed
      const pointY = mix(star.y * height, top + v * letterHeight, position)
      const hue = i % 9 === 0 ? palette.ember : palette.bright
      const size = i % 9 === 0 ? (mobile ? 1.8 : 2.4) : (mobile ? 1.12 : 1.5)
      context.fillStyle = color(hue, .94 * progress)
      context.beginPath()
      context.arc(pointX, pointY, size, 0, tau)
      context.fill()
    }
  }


  function paint({ yaw, pitch, parallaxX, parallaxY, time, daypart, minute, ripples, formed }: PlanetFrame) {
    if ((!width || !height) && !resize()) return
    const lighting = paletteAt(minute, daypart)
    const { palette } = lighting
    const mobile = width < 600
    const dx = clamp(parallaxX, -1, 1) * (mobile ? 9 : 16)
    const dy = clamp(parallaxY, -1, 1) * (mobile ? 9 : 16)
    const radius = sphereRadius()
    const x = width * (mobile ? .686 : .726) + dx * .62
    const y = height * (mobile ? .235 : .36) + dy * .62
    const tilt = -.39 + clamp(pitch * .11, -.17, .17) + Math.sin(yaw * .5) * .10
    const squash = Math.round(clamp(.315 + pitch * .12, .16, .455) * 100) / 100
    const spin = Math.round(yaw * .62 + time * .011)
    context.clearRect(0, 0, width, height)
    paintStars(palette, time, dx, dy)
    paintMoon(x + radius * 1.35 - dx * .18, y - radius * 1.21 - dy * .17, Math.max(4, radius * .092), palette)
    renderPlanet(lighting, squash, spin)
    context.save()
    context.translate(x, y)
    context.rotate(tilt)
    context.imageSmoothingQuality = 'high'
    const spriteWidth = planetSprite.width / spriteScale * radius
    const spriteHeight = planetSprite.height / spriteScale * radius
    context.drawImage(planetSprite, -spriteWidth / 2, -spriteHeight / 2, spriteWidth, spriteHeight)
    context.restore()
    paintConstellation(formed, palette)
    paintRipples(ripples, palette)
  }

  return { resize, paint }
}
