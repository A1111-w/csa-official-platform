"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import {
  ArrowRight,
  BookOpen,
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
import { excerptText } from "@/lib/format"
import { publicService, type ContributorVo } from "@/services/public"
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

export default function HomePage() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [about, setAbout] = useState("")
  const [contributors, setContributors] = useState<ContributorVo[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadHomeData() {
      setLoading(true)
      try {
        const [aboutContent, contributorList] = await Promise.all([
          publicService.getAbout(),
          publicService.getContributors(),
        ])

        if (cancelled) {
          return
        }

        setAbout(aboutContent)
        setContributors(contributorList)
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
