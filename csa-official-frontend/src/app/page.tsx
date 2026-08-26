"use client"

import Image from "next/image"
import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import {
  ArrowRight,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Crown,
  FileText,
  GitPullRequest,
  HardDrive,
  Sparkles,
  Trophy,
  Users,
} from "lucide-react"
import { toast } from "sonner"

import { ContributorWall } from "@/components/business/community/ContributorWall"
import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"
import { Button } from "@/components/ui/button"
import { useIsClient } from "@/hooks/use-is-client"
import { excerptText, resolveAssetUrl } from "@/lib/format"
import {
  publicService,
  type CarouselItem,
  type ContributionRankItem,
  type ContributorVo,
} from "@/services/public"
import { useAuthStore } from "@/store/useAuthStore"

const platformItems = [
  {
    title: "资源沉淀",
    desc: "课程资料、工具包和协会模板集中维护。",
    icon: HardDrive,
  },
  {
    title: "比赛活动",
    desc: "活动信息面向访客展示，管理入口留给组织成员。",
    icon: Trophy,
  },
  {
    title: "简历工作区",
    desc: "核心成员维护项目经历，部长在审核工作台处理提交。",
    icon: FileText,
  },
  {
    title: "组织协作",
    desc: "提案、部门和公开内容逐级授权，减少散落沟通。",
    icon: GitPullRequest,
  },
]

function safeCarouselTarget(value: string | null) {
  if (!value) return null
  if (value.startsWith("/") && !value.startsWith("//")) return value

  try {
    const url = new URL(value)
    return url.protocol === "https:" || url.protocol === "http:" ? value : null
  } catch {
    return null
  }
}

function formatScore(value: number | string) {
  const score = Number(value)
  if (!Number.isFinite(score)) return String(value)
  return Number.isInteger(score) ? String(score) : score.toFixed(1)
}

export default function HomePage() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [about, setAbout] = useState("")
  const [contributors, setContributors] = useState<ContributorVo[]>([])
  const [carouselItems, setCarouselItems] = useState<CarouselItem[]>([])
  const [activeCarouselIndex, setActiveCarouselIndex] = useState(0)
  const [contributionRank, setContributionRank] = useState<ContributionRankItem[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadHomeData() {
      setLoading(true)
      try {
        const results = await Promise.allSettled([
          publicService.getAbout(),
          publicService.getContributors(),
          publicService.getCarousel(),
          publicService.getContributionRank(5),
        ])

        if (cancelled) {
          return
        }

        if (results[0].status === "fulfilled") setAbout(results[0].value)
        if (results[1].status === "fulfilled") setContributors(results[1].value)
        if (results[2].status === "fulfilled") setCarouselItems(results[2].value)
        if (results[3].status === "fulfilled") setContributionRank(results[3].value)

        if (results.some((result) => result.status === "rejected")) {
          toast.error("部分首页数据加载失败，已展示可用内容")
        }
      } catch (error) {
        if (!cancelled) {
          toast.error(error instanceof Error ? error.message : "首页数据加载失败")
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadHomeData()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (carouselItems.length <= 1) return

    const timer = window.setInterval(() => {
      setActiveCarouselIndex((current) => (current + 1) % carouselItems.length)
    }, 6000)

    return () => window.clearInterval(timer)
  }, [carouselItems.length])

  const roleStats = useMemo(() => {
    const leaderCount = contributors.filter((item) => item.roleLevel >= 3).length
    const coreCount = contributors.filter((item) => item.roleLevel >= 2).length

    return {
      total: contributors.length,
      leaders: leaderCount,
      core: coreCount,
    }
  }, [contributors])

  const aboutSummary =
    excerptText(about, 210) ||
    "CSA 把资源沉淀、比赛组织、简历维护和内部协作收在同一套平台里，让成员能更快找到入口，也让协会的技术资产继续留下来。"

  const primaryHref = isClient && user ? "/dashboard" : "/login"
  const primaryLabel = isClient && user ? "进入控制台" : "登录平台"
  const activeCarousel = carouselItems.length
    ? carouselItems[activeCarouselIndex % carouselItems.length]
    : null
  const carouselTarget = safeCarouselTarget(activeCarousel?.targetUrl ?? null)

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main>
        <section className="relative overflow-hidden border-b bg-[linear-gradient(135deg,#ffffff_0%,#eef4ff_48%,#edfff8_100%)]">
          <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent" />
          <div className="mx-auto grid max-w-7xl gap-10 px-4 py-14 md:px-8 lg:grid-cols-[0.95fr_1.05fr] lg:py-20">
            <div className="flex flex-col justify-center">
              <h1 className="max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-foreground sm:text-5xl">
                CSA 计协协作平台
              </h1>
              <p className="mt-5 max-w-2xl text-base leading-8 text-muted-foreground">
                面向协会成员和访客的统一入口：公开信息对外展示，资源、比赛、简历和提案按身份进入对应工作区，让协作像一套清晰的产品流。
              </p>

              <div className="mt-8 flex flex-wrap gap-3">
                <Button asChild size="lg">
                  <Link href={primaryHref}>
                    {primaryLabel}
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button asChild variant="outline" size="lg">
                  <Link href="/resources">浏览资源库</Link>
                </Button>
              </div>

              <div className="mt-10 grid gap-3 sm:grid-cols-3">
                {[
                  ["公开成员", String(roleStats.total || "--")],
                  ["组织负责人", String(roleStats.leaders || "--")],
                  ["核心成员", String(roleStats.core || "--")],
                ].map(([label, value]) => (
                  <div
                    key={label}
                    className="rounded-lg border bg-white/80 px-4 py-5 shadow-sm backdrop-blur"
                  >
                    <p className="text-sm text-muted-foreground">{label}</p>
                    <p className="mt-2 text-2xl font-semibold">{value}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-lg border bg-white/85 p-4 shadow-[0_24px_80px_rgba(37,56,112,0.14)] backdrop-blur">
              <div className="flex items-center justify-between border-b pb-4">
                <div>
                  <p className="text-sm font-medium">今日工作台</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    从公开页面到内部协作的一张入口图
                  </p>
                </div>
              </div>

              <div className="mt-5 grid gap-3">
                {platformItems.map((item) => {
                  const Icon = item.icon

                  return (
                    <div
                      key={item.title}
                      className="grid grid-cols-[auto_1fr] gap-3 rounded-lg border bg-white p-4 shadow-sm transition-transform hover:-translate-y-0.5"
                    >
                      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-accent text-accent-foreground">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <p className="text-sm font-medium">{item.title}</p>
                        <p className="mt-1 text-xs leading-6 text-muted-foreground">
                          {item.desc}
                        </p>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </section>

        {activeCarousel ? (
          <section className="relative min-h-[360px] overflow-hidden border-b bg-neutral-950 text-white">
            <Image
              src={resolveAssetUrl(activeCarousel.imgUrl)}
              alt={activeCarousel.title}
              fill
              priority
              unoptimized
              sizes="100vw"
              className="object-cover"
            />
            <div className="absolute inset-0 bg-black/55" />
            <div className="relative mx-auto flex min-h-[360px] max-w-7xl items-end px-4 py-10 md:px-8">
              <div className="flex w-full flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
                <div className="max-w-3xl">
                  <p className="text-sm font-medium text-white/70">协会动态</p>
                  <h2 className="mt-3 text-3xl font-semibold leading-tight sm:text-4xl">
                    {activeCarousel.title}
                  </h2>
                  {carouselTarget ? (
                    <Button asChild className="mt-6 bg-white text-neutral-950 hover:bg-white/90">
                      <a
                        href={carouselTarget}
                        target={carouselTarget.startsWith("http") ? "_blank" : undefined}
                        rel={carouselTarget.startsWith("http") ? "noopener noreferrer" : undefined}
                      >
                        查看详情
                        <ArrowRight />
                      </a>
                    </Button>
                  ) : null}
                </div>

                {carouselItems.length > 1 ? (
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      aria-label="上一张轮播图"
                      title="上一张轮播图"
                      className="border-white/40 bg-black/20 text-white hover:bg-white hover:text-neutral-950"
                      onClick={() =>
                        setActiveCarouselIndex((current) =>
                          (current - 1 + carouselItems.length) % carouselItems.length
                        )
                      }
                    >
                      <ChevronLeft />
                    </Button>
                    <span className="min-w-16 text-center text-sm text-white/80">
                      {activeCarouselIndex + 1} / {carouselItems.length}
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      aria-label="下一张轮播图"
                      title="下一张轮播图"
                      className="border-white/40 bg-black/20 text-white hover:bg-white hover:text-neutral-950"
                      onClick={() =>
                        setActiveCarouselIndex((current) =>
                          (current + 1) % carouselItems.length
                        )
                      }
                    >
                      <ChevronRight />
                    </Button>
                  </div>
                ) : null}
              </div>
            </div>
          </section>
        ) : null}

        <section className="mx-auto max-w-7xl bg-background px-4 py-14 md:px-8">
          <div className="grid gap-10 lg:grid-cols-[0.85fr_1.15fr]">
            <div className="space-y-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <BookOpen className="h-5 w-5" />
              </div>
              <h2 className="text-3xl font-semibold tracking-tight">
                把公开信息讲清楚，也把内部协作接住
              </h2>
              <p className="text-sm leading-8 text-muted-foreground">{aboutSummary}</p>
              <Button asChild variant="outline">
                <Link href="/about">
                  查看完整介绍
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              {[
                {
                  title: "访客入口",
                  desc: "浏览协会介绍、比赛活动和核心成员。",
                },
                {
                  title: "成员路径",
                  desc: "拿到邀请码后进入资源库，开始使用沉淀内容。",
                },
                {
                  title: "核心协作",
                  desc: "简历、提案和公开内容维护逐级向上开放。",
                },
              ].map((item) => (
                <div key={item.title} className="rounded-lg border bg-card p-5">
                  <Sparkles className="h-4 w-4 text-primary" />
                  <h3 className="mt-4 font-semibold">{item.title}</h3>
                  <p className="mt-2 text-sm leading-7 text-muted-foreground">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="border-t bg-muted/30 py-14">
          <div className="mx-auto max-w-7xl px-4 md:px-8">
            <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-amber-500/10 text-amber-700">
                  <Crown className="h-5 w-5" />
                </div>
                <h2 className="text-3xl font-semibold tracking-tight">贡献排行榜</h2>
                <p className="mt-2 text-sm text-muted-foreground">
                  按已记录贡献积分与贡献次数排序
                </p>
              </div>
              <Button asChild variant="outline">
                <Link href="/contributors">
                  查看成员名单
                  <ArrowRight />
                </Link>
              </Button>
            </div>

            {loading ? (
              <div className="h-32 animate-pulse rounded-lg border bg-background" />
            ) : contributionRank.length ? (
              <div className="divide-y rounded-lg border bg-background">
                {contributionRank.map((item, index) => (
                  <article
                    key={item.userId}
                    className="grid grid-cols-[2.5rem_3rem_minmax(0,1fr)_auto] items-center gap-3 px-4 py-4 sm:grid-cols-[3rem_3rem_minmax(0,1fr)_7rem_7rem] sm:px-6"
                  >
                    <span className="text-center text-lg font-semibold text-muted-foreground">
                      {index + 1}
                    </span>
                    <div className="relative h-11 w-11 overflow-hidden rounded-full border bg-secondary">
                      {item.avatar ? (
                        <Image
                          src={resolveAssetUrl(item.avatar)}
                          alt={item.realName || item.username || "贡献者"}
                          fill
                          unoptimized
                          sizes="44px"
                          className="object-cover"
                        />
                      ) : (
                        <div className="flex h-full items-center justify-center text-sm font-semibold text-primary">
                          {(item.realName || item.username || "U").slice(0, 1)}
                        </div>
                      )}
                    </div>
                    <div className="min-w-0">
                      <p className="truncate font-semibold">
                        {item.realName || item.username || "未命名成员"}
                      </p>
                      <p className="truncate text-sm text-muted-foreground">
                        {item.deptName || "协会协作组"}
                      </p>
                    </div>
                    <div className="hidden text-right sm:block">
                      <p className="text-xs text-muted-foreground">贡献次数</p>
                      <p className="mt-1 font-medium">{item.contributionCount}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">积分</p>
                      <p className="mt-1 text-lg font-semibold text-primary">
                        {formatScore(item.score)}
                      </p>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <div className="rounded-lg border border-dashed px-6 py-10 text-sm text-muted-foreground">
                暂时还没有可展示的贡献记录。
              </div>
            )}
          </div>
        </section>

        <section className="border-t bg-[linear-gradient(180deg,#f8fbff_0%,#ffffff_100%)] py-14">
          <div className="mx-auto max-w-7xl px-4 md:px-8">
            <div className="mb-8 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Users className="h-5 w-5" />
                </div>
                <h2 className="text-3xl font-semibold tracking-tight">
                  正在把这件事推进下去的人
                </h2>
              </div>
              <Button asChild variant="outline">
                <Link href="/contributors">
                  查看完整名单
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </div>

            <ContributorWall members={contributors} loading={loading} limit={4} />
          </div>
        </section>
      </main>

      <Footer />
    </div>
  )
}
