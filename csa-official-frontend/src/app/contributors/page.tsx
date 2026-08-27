"use client"

import { useEffect, useMemo, useState } from "react"
import { Users } from "lucide-react"
import { toast } from "sonner"

import { ContributorWall } from "@/components/business/community/ContributorWall"
import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"
import { publicService, type ContributorVo } from "@/services/public"

export default function ContributorsPage() {
  const [loading, setLoading] = useState(true)
  const [contributors, setContributors] = useState<ContributorVo[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadData() {
      setLoading(true)
      try {
        const response = await publicService.getContributors()
        if (!cancelled) {
          setContributors(response)
        }
      } catch (error) {
        if (!cancelled) {
          toast.error(error instanceof Error ? error.message : "成员名单加载失败")
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

  const deptCount = useMemo(
    () => new Set(contributors.map((item) => item.deptName).filter(Boolean)).size,
    [contributors]
  )

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-12 md:px-8">
        <section className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Users className="h-5 w-5" />
            </div>
            <h1 className="text-4xl font-semibold tracking-tight">
              核心成员与公开贡献者
            </h1>
            <p className="mt-3 max-w-3xl text-sm leading-8 text-muted-foreground">
              这不是静态名单，而是协会当前对外可见的核心协作层。角色、部门和头衔都直接从后端数据读取。
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-lg border bg-card px-5 py-4 shadow-sm">
              <p className="text-sm text-muted-foreground">公开成员数</p>
              <p className="mt-2 text-2xl font-semibold">{contributors.length || "--"}</p>
            </div>
            <div className="rounded-lg border bg-card px-5 py-4 shadow-sm">
              <p className="text-sm text-muted-foreground">涉及部门</p>
              <p className="mt-2 text-2xl font-semibold">{deptCount || "--"}</p>
            </div>
          </div>
        </section>

        <section className="mt-10">
          <ContributorWall members={contributors} loading={loading} limit={contributors.length || 8} />
        </section>
      </main>
      <Footer />
    </div>
  )
}
