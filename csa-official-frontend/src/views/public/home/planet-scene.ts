import type { Daypart } from './solar-theme'

type Color = number[]
type ColorKey = 'shadow' | 'sea' | 'bright' | 'ice' | 'rim' | 'ring' | 'ember'
type Palette = Record<ColorKey, Color> & { light: number[]; ambient: number; halo: number; stars: number }
type Lighting = { key: string | number; palette: Palette }
type Star = { x: number; y: number; radius: number; brightness: number; phase: number }
type Point = [number, number]
type SphereSample = { offset: number; u: number; row: number; nx: number; ny: number; z: number; rim: number; opacity: number }
type Sphere = { sprite: HTMLCanvasElement; drawing: CanvasRenderingContext2D; pixels: ImageData; samples: SphereSample[]; moon: boolean; key: string }

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
    dawn: { shadow: [24, 39, 68], sea: [99, 191, 232], bright: [222, 249, 243], ice: [181, 156, 219], rim: [213, 242, 222], ring: [194, 232, 234], ember: [255, 213, 157], light: [-.66, -.38, .59], ambient: .25, halo: .83, stars: .44 },
    day: { shadow: [20, 66, 100], sea: [88, 203, 237], bright: [205, 251, 255], ice: [151, 193, 237], rim: [196, 253, 255], ring: [183, 228, 249], ember: [249, 246, 220], light: [-.37, -.72, .59], ambient: .31, halo: .65, stars: .20 },
    dusk: { shadow: [35, 24, 63], sea: [151, 143, 222], bright: [236, 219, 255], ice: [213, 169, 226], rim: [243, 199, 231], ring: [217, 178, 230], ember: [255, 194, 162], light: [.38, -.54, .65], ambient: .23, halo: 1.06, stars: .62 },
    night: { shadow: [6, 23, 51], sea: [42, 160, 210], bright: [155, 248, 255], ice: [100, 132, 199], rim: [139, 239, 254], ring: [117, 212, 248], ember: [183, 186, 254], light: [-.51, -.50, .59], ambient: .18, halo: 1.15, stars: .80 },
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
      halo: mix(previous.halo, next.halo, blend),
      stars: mix(previous.stars, next.stars, blend),
    }
    paletteCache = { key: sample, palette }
    return paletteCache
  }

  const mapWidth = 512
  const mapHeight = 252
  const bands = new Float32Array(mapWidth * mapHeight)
  const clouds = new Float32Array(mapWidth * mapHeight)
  let surfaceReady = false

  function generateSurface() {
    if (surfaceReady) return
    for (let y = 0; y < mapHeight; y += 1) {
      const latitude = (y / (mapHeight - 1) - .5) * Math.PI
      const latitudeRipple = Math.sin(latitude * 14)
      for (let x = 0; x < mapWidth; x += 1) {
        const longitude = x / mapWidth * tau
        const flow = latitude + .053 * Math.sin(longitude * 3.0 + latitude * 6.4)
          + .028 * Math.sin(longitude * 7.0 - latitude * 11.8)
          + .011 * Math.sin(longitude * 15.0 + latitude * 24.6)
        const vapor = .41 * Math.sin(flow * 17 + .55 * latitudeRipple)
          + .225 * Math.sin(flow * 39 + Math.sin(longitude * 8 - latitude * 7.2) * .7)
          + .115 * Math.sin(flow * 69 + longitude * 3 + .64 * Math.sin(longitude * 13.0))
          + .055 * Math.sin(flow * 133 + longitude * 12 - Math.sin(latitude * 19))
        const vaporScatter = .27 * Math.sin(longitude * 7.0 + latitude * 12.71)
          + .19 * Math.sin(longitude * 17 + latitude * 31.9)
          + .12 * Math.sin(longitude * 29 - latitude * 49.7)
          + .07 * Math.sin(longitude * 67 + latitude * 79.2)
          + .036 * Math.sin(longitude * 139 - latitude * 134.5)
        const stormWarp = .12 * Math.sin(longitude * 3 - latitude * 16.9) * Math.sin(longitude * 5 + latitude * 7.3)
        const index = y * mapWidth + x
        bands[index] = clamp(.51 + vapor * .49 + vaporScatter * .16 + stormWarp, 0, 1)
        clouds[index] = clamp(.47 + vaporScatter * .7 + .16 * Math.sin(flow * 22 + longitude * 4.1), 0, 1)
      }
    }
    surfaceReady = true
  }

  function makeSphere(size: number, moon = false): Sphere | null {
    const sprite = document.createElement('canvas')
    sprite.width = size
    sprite.height = size
    const drawing = sprite.getContext('2d')
    if (!drawing) return null
    const pixels = drawing.createImageData(size, size)
    const samples: SphereSample[] = []
    const axis = -.23
    const axisCos = Math.cos(axis)
    const axisSin = Math.sin(axis)
    const radius = (size - 2) / 2
    for (let y = 0; y < size; y += 1) {
      const ny = (y + .5 - size / 2) / radius
      for (let x = 0; x < size; x += 1) {
        const nx = (x + .5 - size / 2) / radius
        const square = nx * nx + ny * ny
        if (square > 1.007) continue
        const z = Math.sqrt(Math.max(0, 1 - square))
        const sx = nx * axisCos - ny * axisSin
        const sy = nx * axisSin + ny * axisCos
        const rim = Math.pow(1 - z, 3.8)
        const u = Math.round((Math.atan2(sx, z) / tau + .5) * mapWidth)
        const v = Math.round((Math.asin(clamp(sy, -1, 1)) / Math.PI + .5) * (mapHeight - 1))
        const opacity = Math.round(clamp((1 - Math.sqrt(square)) * radius + .54, 0, 1) * 255)
        samples.push({ offset: (y * size + x) * 4, u, row: v * mapWidth, nx, ny, z, rim, opacity })
      }
    }
    return { sprite, drawing, pixels, samples, moon, key: '' }
  }

  function rasterSphere(target: Sphere, phase: number, { key: paletteKey, palette }: Lighting) {
    const longitudeStep = Math.round(phase * 148)
    const key = `${paletteKey}/${longitudeStep}`
    if (target.key === key) return target.sprite
    const { samples, pixels, drawing, moon } = target
    const [lightX, lightY, lightZ] = palette.light
    const ambient = moon ? palette.ambient * .52 : palette.ambient
    const shift = ((Math.round(longitudeStep / 148 * mapWidth) % mapWidth) + mapWidth) % mapWidth
    for (const { offset, u, row, nx, ny, z, rim, opacity } of samples) {
      const index = row + (u + shift) % mapWidth
      const pattern = bands[index]
      const cloud = clouds[index]
      const diffuse = Math.sqrt(Math.max(0, nx * lightX + ny * lightY + z * lightZ * (moon ? .82 : 1)))
      const intensity = ambient + (1 - ambient) * diffuse
      const light = moon ? Math.min(1, intensity * (.79 + cloud * .30)) : intensity
      const veil = moon ? clamp(cloud * .73, 0, 1) : clamp(.18 + pattern * .68 + (cloud - .5) * .28, 0, 1)
      const iridescence = moon ? .22 : clamp(.56 - pattern * .72, .02, .35)
      const litRim = rim * (.33 + .67 * Math.max(0, (nx * lightX + ny * lightY) * 1.4))
      for (let channel = 0; channel < 3; channel += 1) {
        const hue = mix(palette.sea[channel], palette.ice[channel], iridescence)
        const base = mix(hue, palette.bright[channel], veil)
        const shaded = mix(palette.shadow[channel] * (moon ? .54 : .77), base, light)
        const atmosphere = litRim * (moon ? .69 : .53) + (1 - z) * .06
        pixels.data[offset + channel] = mix(shaded, palette.rim[channel], atmosphere)
      }
      pixels.data[offset + 3] = opacity
    }
    drawing.putImageData(pixels, 0, 0)
    target.key = key
    return target.sprite
  }

  let width = 0
  let height = 0
  let pixelRatio = 1
  let stars: Star[] = []
  let letterDots: Point[] = []
  let letterWidth = 0
  let letterHeight = 0
  let planet: Sphere | null = null
  let satellite: Sphere | null = null

  function resize() {
    const rect = canvas.getBoundingClientRect()
    const nextWidth = Math.round(rect.width)
    const nextHeight = Math.round(rect.height)
    if (!nextWidth || !nextHeight) return false
    const nextRatio = Math.min(window.devicePixelRatio || 1, 1.8)
    const mobile = nextWidth < 600
    if (width === nextWidth && height === nextHeight && pixelRatio === nextRatio && planet && satellite) return true
    width = nextWidth
    height = nextHeight
    pixelRatio = nextRatio
    canvas.width = Math.round(width * pixelRatio)
    canvas.height = Math.round(height * pixelRatio)
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    context.imageSmoothingQuality = 'high'
    generateSurface()
    const spriteSize = mobile ? 168 : 230
    if (!planet || planet.sprite.width !== spriteSize) planet = makeSphere(spriteSize)
    if (!satellite) satellite = makeSphere(72, true)

    let seed = 7919
    const random = () => ((seed = (seed * 1664525 + 1013904223) >>> 0) / 4294967296)
    stars = Array.from({ length: mobile ? 38 : 64 }, (_, i) => ({
      x: .02 + random() * .97,
      y: .02 + random() * .57,
      radius: i % 13 === 0 ? 1.22 : .36 + random() * .55,
      brightness: .28 + random() * .72,
      phase: random() * tau,
    }))
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
    return Boolean(planet && satellite)
  }

  const ringLayers = [
    { offset: 1.25, width: .009, alpha: .22 },
    { offset: 1.315, width: .014, alpha: .35 },
    { offset: 1.36, width: .016, alpha: .44 },
    { offset: 1.405, width: .034, alpha: .63 },
    { offset: 1.444, width: .026, alpha: .81 },
    { offset: 1.474, width: .024, alpha: .90 },
    { offset: 1.501, width: .020, alpha: .74 },
    { offset: 1.526, width: .012, alpha: .59 },
    { offset: 1.561, width: .009, alpha: .20 },
    { offset: 1.613, width: .017, alpha: .58 },
    { offset: 1.642, width: .021, alpha: .73 },
    { offset: 1.678, width: .016, alpha: .48 },
    { offset: 1.724, width: .011, alpha: .42 },
    { offset: 1.781, width: .007, alpha: .28 },
    { offset: 1.833, width: .004, alpha: .18 },
  ]

  function paintHalo(x: number, y: number, radius: number, palette: Palette, strength: number) {
    const glow = context.createRadialGradient(x, y, radius * .72, x, y, radius * 1.66)
    glow.addColorStop(0, color(palette.rim, .28 * strength))
    glow.addColorStop(.36, color(palette.sea, .17 * strength))
    glow.addColorStop(.72, color(palette.ice, .07 * strength))
    glow.addColorStop(1, color(palette.ice, 0))
    context.fillStyle = glow
    context.fillRect(x - radius * 1.68, y - radius * 1.68, radius * 3.36, radius * 3.36)
  }

  function paintRings(x: number, y: number, radius: number, squash: number, tilt: number, palette: Palette, front: boolean) {
    const spread = radius * 1.85
    const gradient = context.createLinearGradient(x - spread, y + spread * .27, x + spread, y - spread * .27)
    const gain = front ? 1 : .61
    gradient.addColorStop(0, color(palette.ring, .15 * gain))
    gradient.addColorStop(.17, color(palette.rim, .74 * gain))
    gradient.addColorStop(.34, color(palette.ring, .91 * gain))
    gradient.addColorStop(.54, color(palette.ember, .96 * gain))
    gradient.addColorStop(.69, color(palette.ring, .78 * gain))
    gradient.addColorStop(.9, color(palette.ice, .54 * gain))
    gradient.addColorStop(1, color(palette.rim, .14 * gain))
    context.strokeStyle = gradient
    context.lineCap = 'butt'
    for (const ring of ringLayers) {
      const span = radius * ring.offset
      context.globalAlpha = ring.alpha
      context.lineWidth = Math.max(.55, radius * ring.width)
      context.beginPath()
      context.ellipse(x, y, span, span * squash, tilt, front ? -.004 : Math.PI - .004, front ? Math.PI + .004 : tau + .004)
      context.stroke()
    }
    context.globalAlpha = 1
  }

  function paintStars(palette: Palette, time: number, offsetX: number, offsetY: number) {
    context.fillStyle = color(palette.rim)
    for (const { x, y, radius, brightness, phase } of stars) {
      const sx = x * width + offsetX * .17
      const sy = y * height + offsetY * .19
      const twinkle = .56 + .44 * Math.pow((1 + Math.sin(time * 1.12 + phase)) / 2, 2)
      context.globalAlpha = palette.stars * brightness * twinkle
      context.beginPath()
      context.arc(sx, sy, radius, 0, tau)
      context.fill()
      if (radius > 1.2) {
        context.globalAlpha *= .31
        context.fillRect(sx - 3.1, sy - .38, 6.2, .76)
        context.fillRect(sx - .38, sy - 3.1, .76, 6.2)
      }
    }
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
    const x = width * (mobile ? .73 : .788)
    const y = height * (mobile ? .427 : .80)
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
    if (!planet || !satellite) return
    const lighting = paletteAt(minute, daypart)
    const { palette } = lighting
    const mobile = width < 600
    const offset = (amount: number) => Math.abs(amount) <= 2 ? amount * (mobile ? 12 : 19) : clamp(amount, -40, 40)
    const offsetX = offset(parallaxX)
    const offsetY = offset(parallaxY)
    const radius = mobile ? Math.min(width * .245, height * .205) : Math.min(width * .154, height * .29)
    const x = width * (mobile ? .665 : .752) + offsetX * .58
    const y = height * (mobile ? .285 : .428) + offsetY * .58
    const tilt = -.209 + clamp(pitch * .13, -.26, .26) + clamp(yaw * .041, -.14, .14)
    const squash = clamp(.286 + pitch * .101, .146, .52)
    const moonX = width * (mobile ? .88 : .902) - offsetX * .26
    const moonY = height * (mobile ? .135 : .174) - offsetY * .22
    const moonRadius = radius * (mobile ? .183 : .20)
    context.clearRect(0, 0, width, height)
    paintStars(palette, time, offsetX, offsetY)
    paintHalo(moonX, moonY, moonRadius, palette, .71)
    const moonSprite = rasterSphere(satellite, .24 + yaw * .032 + time * .002, lighting)
    context.drawImage(moonSprite, moonX - moonRadius, moonY - moonRadius, moonRadius * 2, moonRadius * 2)
    paintHalo(x, y, radius, palette, palette.halo)
    paintRings(x, y, radius, squash, tilt, palette, false)
    const planetSprite = rasterSphere(planet, yaw / tau + time * .0069, lighting)
    context.drawImage(planetSprite, x - radius, y - radius, radius * 2, radius * 2)
    paintRings(x, y, radius, squash, tilt, palette, true)
    paintConstellation(formed, palette)
    paintRipples(ripples, palette)
  }

  return { resize, paint }
}
