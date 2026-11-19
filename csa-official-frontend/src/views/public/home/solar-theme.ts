export type Daypart = 'dawn' | 'day' | 'dusk' | 'night'

const beijingClock = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Shanghai',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const daypartNames: Record<Daypart, string> = { dawn: '清晨', day: '午间', dusk: '傍晚', night: '夜间' }
const skyColors: [number, number[]][] = [
  [0, [62, 89, 185]], [255, [68, 91, 182]], [365, [255, 136, 136]],
  [480, [247, 183, 124]], [650, [74, 200, 238]], [855, [98, 216, 216]],
  [1035, [254, 172, 100]], [1120, [219, 108, 178]], [1230, [109, 111, 224]], [1440, [62, 89, 185]],
]

export const limit = (value: number, minimum: number, maximum: number) => Math.min(maximum, Math.max(minimum, value))

function ease(value: number) {
  const progress = limit(value, 0, 1)
  return progress * progress * (3 - 2 * progress)
}

export function beijingMinute(now: Date) {
  const [hour, minute] = beijingClock.format(now).split(':').map(Number)
  return hour * 60 + minute
}

export function solarTheme(minute: number) {
  const daypart: Daypart = minute < 360 || minute >= 1200 ? 'night' : minute < 600 ? 'dawn' : minute < 1020 ? 'day' : 'dusk'
  const hourLabel = String(Math.floor(minute / 60)).padStart(2, '0')
  const minuteLabel = String(minute % 60).padStart(2, '0')
  const daylight = ease((minute - 309) / 188) * (1 - ease((minute - 976) / 231))
  const sunrise = Math.exp(-Math.pow((minute - 384) / 105, 2))
  const sunset = Math.exp(-Math.pow((minute - 1094) / 108, 2))
  const twilight = Math.max(sunrise, sunset)
  const colorIndex = skyColors.findIndex(([time]) => time > minute)
  const [start, morning] = skyColors[colorIndex - 1]
  const [end, evening] = skyColors[colorIndex]
  const colorProgress = ease((minute - start) / (end - start))
  const color = morning.map((channel, index) => Math.round(channel + (evening[index] - channel) * colorProgress))

  return {
    daypart,
    label: `${hourLabel}:${minuteLabel} · ${daypartNames[daypart]}`,
    style: {
      '--sky-color': `rgb(${color.join(' ')})`,
      '--sun-x': `${Math.round(17 + limit((minute - 360) / 840, 0, 1) * 69)}%`,
      '--daylight': daylight.toFixed(3),
      '--twilight': twilight.toFixed(3),
    },
  }
}
