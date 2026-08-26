"use client"

import type { CSSProperties } from "react"
import { useEffect, useMemo, useState } from "react"
import { Sparkles } from "lucide-react"

import { cn } from "@/lib/utils"

export type AuthSceneMood = "idle" | "username" | "password" | "peek" | "error" | "success"

type AnimatedAuthSceneProps = {
  mood?: AuthSceneMood
  title: string
  description: string
  label?: string
  compact?: boolean
}

type LookState = {
  faceX: number
  faceY: number
  bodySkew: number
  pupilX: number
  pupilY: number
}

type EyeOffset = {
  x: number
  y: number
}

const neutralLook: LookState = {
  faceX: 0,
  faceY: 0,
  bodySkew: 0,
  pupilX: 0,
  pupilY: 0,
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function transformOffset(offset: EyeOffset) {
  return `translate(${offset.x}px, ${offset.y}px)`
}

export function AnimatedAuthScene({
  mood = "idle",
  title,
  description,
  label = "登录",
  compact = false,
}: AnimatedAuthSceneProps) {
  const [look, setLook] = useState<LookState>(neutralLook)
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false)
  const [purpleBlinking, setPurpleBlinking] = useState(false)
  const [blackBlinking, setBlackBlinking] = useState(false)
  const [lookingAtEachOther, setLookingAtEachOther] = useState(false)
  const [purplePeeking, setPurplePeeking] = useState(false)

  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)")
    const syncMotionPreference = () => setPrefersReducedMotion(media.matches)

    syncMotionPreference()
    media.addEventListener("change", syncMotionPreference)
    return () => media.removeEventListener("change", syncMotionPreference)
  }, [])

  useEffect(() => {
    if (prefersReducedMotion) {
      return
    }

    let cancelled = false
    let blinkTimer: number
    let resetTimer: number

    const schedule = () => {
      blinkTimer = window.setTimeout(() => {
        if (cancelled) return
        setPurpleBlinking(true)
        resetTimer = window.setTimeout(() => {
          setPurpleBlinking(false)
          schedule()
        }, 150)
      }, Math.random() * 4000 + 3000)
    }

    schedule()
    return () => {
      cancelled = true
      window.clearTimeout(blinkTimer)
      window.clearTimeout(resetTimer)
    }
  }, [prefersReducedMotion])

  useEffect(() => {
    if (prefersReducedMotion) {
      return
    }

    let cancelled = false
    let blinkTimer: number
    let resetTimer: number

    const schedule = () => {
      blinkTimer = window.setTimeout(() => {
        if (cancelled) return
        setBlackBlinking(true)
        resetTimer = window.setTimeout(() => {
          setBlackBlinking(false)
          schedule()
        }, 150)
      }, Math.random() * 4000 + 3000)
    }

    schedule()
    return () => {
      cancelled = true
      window.clearTimeout(blinkTimer)
      window.clearTimeout(resetTimer)
    }
  }, [prefersReducedMotion])

  useEffect(() => {
    if (mood !== "username" || prefersReducedMotion) {
      return
    }

    const startTimer = window.setTimeout(() => setLookingAtEachOther(true), 0)
    const endTimer = window.setTimeout(() => setLookingAtEachOther(false), 800)
    return () => {
      window.clearTimeout(startTimer)
      window.clearTimeout(endTimer)
    }
  }, [mood, prefersReducedMotion])

  useEffect(() => {
    if (mood !== "peek" || prefersReducedMotion) {
      return
    }

    let cancelled = false
    let peekTimer: number
    let resetTimer: number

    const schedule = () => {
      peekTimer = window.setTimeout(() => {
        if (cancelled) return
        setPurplePeeking(true)
        resetTimer = window.setTimeout(() => {
          setPurplePeeking(false)
          schedule()
        }, 800)
      }, Math.random() * 3000 + 2000)
    }

    schedule()
    return () => {
      cancelled = true
      window.clearTimeout(peekTimer)
      window.clearTimeout(resetTimer)
    }
  }, [mood, prefersReducedMotion])

  const state = useMemo(() => {
    const isTyping = mood === "username"
    const isLookingAway = mood === "password"
    const isShowingPassword = mood === "peek"
    const isError = mood === "error"

    return {
      isTyping,
      isLookingAway,
      isShowingPassword,
      isError,
      isSuccess: mood === "success",
      shouldMeetEyes: isTyping && lookingAtEachOther && !prefersReducedMotion,
    }
  }, [lookingAtEachOther, mood, prefersReducedMotion])

  const idleSkew = prefersReducedMotion ? 0 : look.bodySkew
  const idleFaceX = prefersReducedMotion ? 0 : look.faceX
  const idleFaceY = prefersReducedMotion ? 0 : look.faceY

  const pupilOffset = (maxDistance: number, force?: EyeOffset) => {
    if (force) {
      return force
    }
    if (prefersReducedMotion) {
      return { x: 0, y: 0 }
    }
    return {
      x: look.pupilX * maxDistance,
      y: look.pupilY * maxDistance,
    }
  }

  const purpleTall = state.isTyping || state.isLookingAway
  const purpleTransform = state.isShowingPassword
    ? "skewX(0deg)"
    : state.isLookingAway
      ? "skewX(-14deg) translateX(-20px)"
      : state.isTyping
        ? `skewX(${idleSkew - 12}deg) translateX(40px)`
        : `skewX(${idleSkew}deg)`

  const blackTransform = state.isShowingPassword
    ? "skewX(0deg)"
    : state.isLookingAway
      ? "skewX(12deg) translateX(-10px)"
      : state.shouldMeetEyes
        ? `skewX(${idleSkew * 1.5 + 10}deg) translateX(20px)`
        : state.isTyping
          ? `skewX(${idleSkew * 1.5}deg)`
          : `skewX(${idleSkew}deg)`

  const sharedFaceClass = state.isError ? "auth-face-shake" : undefined

  const purpleFace = state.isError
    ? { left: 30, top: 55, force: { x: -3, y: 4 } }
    : state.isLookingAway
      ? { left: 20, top: 25, force: { x: -5, y: -5 } }
      : state.isShowingPassword
        ? { left: 20, top: 35, force: purplePeeking ? { x: 4, y: 5 } : { x: -4, y: -4 } }
        : state.shouldMeetEyes
          ? { left: 55, top: 65, force: { x: 3, y: 4 } }
          : { left: 45 + idleFaceX, top: 40 + idleFaceY, force: undefined }

  const blackFace = state.isError
    ? { left: 15, top: 40, force: { x: -3, y: 4 } }
    : state.isLookingAway
      ? { left: 10, top: 20, force: { x: -4, y: -5 } }
      : state.isShowingPassword
        ? { left: 10, top: 28, force: { x: -4, y: -4 } }
        : state.shouldMeetEyes
          ? { left: 32, top: 12, force: { x: 0, y: -4 } }
          : { left: 26 + idleFaceX, top: 32 + idleFaceY, force: undefined }

  const orangeFace = state.isError
    ? { left: 60, top: 95, force: { x: -3, y: 4 } }
    : state.isLookingAway
      ? { left: 50, top: 75, force: { x: -5, y: -5 } }
      : state.isShowingPassword
        ? { left: 50, top: 85, force: { x: -5, y: -4 } }
        : { left: 82 + idleFaceX, top: 90 + idleFaceY, force: undefined }

  const yellowFace = state.isError
    ? { left: 35, top: 45, force: { x: -3, y: 4 } }
    : state.isLookingAway
      ? { left: 20, top: 30, force: { x: -5, y: -5 } }
      : state.isShowingPassword
        ? { left: 20, top: 35, force: { x: -5, y: -4 } }
        : { left: 52 + idleFaceX, top: 40 + idleFaceY, force: undefined }

  const yellowMouthStyle: CSSProperties = state.isError
    ? { left: 30, top: 92, transform: "rotate(-8deg)" }
    : state.isLookingAway
      ? { left: 15, top: 78, transform: "rotate(0deg)" }
      : state.isShowingPassword
        ? { left: 10, top: 88, transform: "rotate(0deg)" }
        : { left: 40 + idleFaceX, top: 88 + idleFaceY, transform: "rotate(0deg)" }

  return (
    <section
      className={cn(
        "auth-scene relative flex min-h-[360px] flex-col justify-between overflow-hidden bg-[linear-gradient(135deg,#d4d0dc_0%,#c8c4d0_48%,#bbb7c5_100%)] px-6 py-6 text-foreground sm:min-h-[430px] sm:px-8 lg:min-h-full lg:px-10 lg:py-10",
        compact && "auth-scene-compact lg:justify-start",
        mood === "error" && "auth-scene-error",
        mood === "password" && "auth-scene-private",
        mood === "peek" && "auth-scene-peek",
        mood === "success" && "auth-scene-success"
      )}
      onMouseMove={(event) => {
        if (prefersReducedMotion) {
          return
        }

        const rect = event.currentTarget.getBoundingClientRect()
        const relativeX = clamp(((event.clientX - rect.left) / rect.width - 0.5) * 2, -1, 1)
        const relativeY = clamp(((event.clientY - rect.top) / rect.height - 0.5) * 2, -1, 1)

        setLook({
          faceX: relativeX * 15,
          faceY: relativeY * 10,
          bodySkew: -relativeX * 6,
          pupilX: relativeX,
          pupilY: relativeY,
        })
      }}
      onMouseLeave={() => setLook(neutralLook)}
    >
      <div className="auth-panel-wash" />

      <div className="relative z-10">
        <div className="inline-flex items-center gap-2 text-sm font-medium text-white/90">
          <Sparkles className="h-4 w-4" />
          {label}
        </div>
        <h1 className="mt-8 max-w-md text-3xl font-semibold leading-tight text-slate-950 lg:mt-10 lg:text-4xl">
          {title}
        </h1>
        <p className="mt-5 max-w-lg text-sm leading-7 text-slate-700">
          {description}
        </p>
      </div>

      <div className="auth-stage" aria-hidden="true">
        <div className="auth-ground" />

        <div
          className="auth-character auth-character-purple"
          style={{ height: purpleTall ? 410 : 370, transform: purpleTransform }}
        >
          <EyePair
            className={sharedFaceClass}
            left={purpleFace.left}
            top={purpleFace.top}
            gap={28}
            blink={purpleBlinking}
            offset={pupilOffset(5, purpleFace.force)}
          />
        </div>

        <div
          className="auth-character auth-character-black"
          style={{ transform: blackTransform }}
        >
          <EyePair
            className={sharedFaceClass}
            left={blackFace.left}
            top={blackFace.top}
            gap={20}
            eyeSize={16}
            pupilSize={6}
            blink={blackBlinking}
            offset={pupilOffset(4, blackFace.force)}
          />
        </div>

        <div
          className="auth-character auth-character-orange"
          style={{
            transform: state.isShowingPassword ? "skewX(0deg)" : `skewX(${idleSkew}deg)`,
          }}
        >
          <BareEyePair
            className={sharedFaceClass}
            left={orangeFace.left}
            top={orangeFace.top}
            gap={28}
            offset={pupilOffset(5, orangeFace.force)}
          />
          <div
            className={cn(
              "auth-orange-mouth",
              state.isError && "auth-orange-mouth-visible auth-face-shake"
            )}
            style={{
              left: state.isError ? 80 + idleFaceX : 90,
              top: state.isError ? 130 : 120,
            }}
          />
        </div>

        <div
          className="auth-character auth-character-yellow"
          style={{
            transform: state.isShowingPassword ? "skewX(0deg)" : `skewX(${idleSkew}deg)`,
          }}
        >
          <BareEyePair
            className={sharedFaceClass}
            left={yellowFace.left}
            top={yellowFace.top}
            gap={20}
            offset={pupilOffset(5, yellowFace.force)}
          />
          <div
            className={cn("auth-yellow-mouth", sharedFaceClass)}
            style={yellowMouthStyle}
          />
        </div>
      </div>

    </section>
  )
}

function EyePair({
  left,
  top,
  gap,
  offset,
  className,
  eyeSize = 18,
  pupilSize = 7,
  blink = false,
}: {
  left: number
  top: number
  gap: number
  offset: EyeOffset
  className?: string
  eyeSize?: number
  pupilSize?: number
  blink?: boolean
}) {
  const eyeStyle = {
    width: eyeSize,
    height: blink ? 2 : eyeSize,
  } as CSSProperties

  const pupilStyle = {
    width: pupilSize,
    height: pupilSize,
    transform: transformOffset(offset),
  } as CSSProperties

  return (
    <div className={cn("auth-eye-group", className)} style={{ left, top, gap }}>
      <span className="auth-eyeball" style={eyeStyle}>
        {!blink && <span className="auth-pupil" style={pupilStyle} />}
      </span>
      <span className="auth-eyeball" style={eyeStyle}>
        {!blink && <span className="auth-pupil" style={pupilStyle} />}
      </span>
    </div>
  )
}

function BareEyePair({
  left,
  top,
  gap,
  offset,
  className,
}: {
  left: number
  top: number
  gap: number
  offset: EyeOffset
  className?: string
}) {
  const pupilStyle = {
    transform: transformOffset(offset),
  } as CSSProperties

  return (
    <div className={cn("auth-eye-group", className)} style={{ left, top, gap }}>
      <span className="auth-bare-pupil" style={pupilStyle} />
      <span className="auth-bare-pupil" style={pupilStyle} />
    </div>
  )
}
