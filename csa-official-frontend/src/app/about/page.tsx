"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { ArrowRight, ShieldCheck } from "lucide-react"
import { toast } from "sonner"

import { ContributorWall } from "@/components/business/community/ContributorWall"
import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { sanitizeHtml } from "@/lib/sanitize-html"
import { publicService, type ContributorVo } from "@/services/public"

const emptyAboutHtml = "<p>暂无协会介绍。</p>"

export default function AboutPage() {
  const [loading, setLoading] = useState(true)
  const [about, setAbout] = useState("")
  const [contributors, setContributors] = useState<ContributorVo[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadData() {
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
          toast.error(error instanceof Error ? error.message : "关于页加载失败")
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadData()

    return () => {
      cancelled = true
    }
  }, [])

  const sanitizedAbout = useMemo(
    () => sanitizeHtml(about) || emptyAboutHtml,
    [about]
  )

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-12 md:px-8">
        <section className="grid gap-8 lg:grid-cols-[0.88fr_1.12fr]">
          <div className="space-y-5">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <h1 className="text-4xl font-semibold tracking-tight">
              关于 CSA，也关于推动它的人
            </h1>
            <p className="text-sm leading-8 text-muted-foreground">
              协会介绍不只是一段口号，它也承接公开信息、成员结构和真实可用的工作面板。
            </p>
            <Button asChild variant="outline">
              <Link href="/contributors">
                去看核心成员
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </div>

          <div className="rounded-lg border bg-card p-6 shadow-sm">
            {loading ? (
              <Skeleton className="h-64 w-full rounded-lg" />
            ) : (
              <div
                className="space-y-4 text-sm leading-8 text-muted-foreground [&_a]:text-primary [&_a]:underline [&_blockquote]:border-l [&_blockquote]:pl-4 [&_h1]:text-2xl [&_h1]:font-semibold [&_h1]:text-foreground [&_h2]:text-xl [&_h2]:font-semibold [&_h2]:text-foreground [&_h3]:text-lg [&_h3]:font-semibold [&_h3]:text-foreground [&_ol]:list-decimal [&_ol]:space-y-2 [&_ol]:pl-5 [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-5"
                dangerouslySetInnerHTML={{ __html: sanitizedAbout }}
              />
            )}
          </div>
        </section>

        <section className="mt-14">
          <div className="mb-8">
            <h2 className="text-3xl font-semibold tracking-tight">核心成员</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              展示当前公开的核心成员与负责人名单。
            </p>
          </div>

          <ContributorWall members={contributors} loading={loading} limit={contributors.length || 4} />
        </section>
      </main>
      <Footer />
    </div>
  )
}
