"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { ArrowRight, Mail, Settings, ShieldCheck, UserCircle2 } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleDescription, getRoleLabel, hasRoleLevel } from "@/lib/access"
import { formatBalance } from "@/lib/format"
import { userService } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"
import type { UserInfo } from "@/types/user"

export function ProfilePanel() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [profile, setProfile] = useState<UserInfo | null>(null)

  useEffect(() => {
    if (!isClient || !user) {
      return
    }

    let cancelled = false

    async function loadProfile() {
      setLoading(true)
      try {
        const response = await userService.getInfo()
        if (!cancelled) {
          setProfile(response)
        }
      } catch (error) {
        if (!cancelled) {
          toast.error(error instanceof Error ? error.message : "用户信息加载失败")
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      cancelled = true
    }
  }, [isClient, user])

  if (!isClient) {
    return <Skeleton className="h-[360px] w-full rounded-lg" />
  }

  if (!user) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <UserCircle2 className="h-5 w-5" />
          </div>
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-semibold">请先登录后查看个人资料</h2>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                登录后可以查看当前角色等级、邮箱、学籍信息以及协会内权限说明。
              </p>
            </div>
            <Button asChild>
              <Link href="/login">去登录</Link>
            </Button>
          </div>
        </div>
      </section>
    )
  }

  const data = profile ?? {
    id: 0,
    username: user?.username ?? "--",
    roleLevel: user?.roleLevel ?? 0,
    realName: null,
    avatar: null,
    email: null,
    phone: null,
    contact: null,
    positionType: null,
    balance: null,
    college: null,
    className: null,
    studentId: null,
  }

  const canEditResume = hasRoleLevel(data.roleLevel, 2)

  return (
    <section className="space-y-6">
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-3">
              <h2 className="text-2xl font-semibold">
                {data.realName || data.username}
              </h2>
              <span className="rounded-full bg-primary/10 px-3 py-1 text-sm text-primary">
                {getRoleLabel(data.roleLevel)}
              </span>
            </div>
            <p className="max-w-2xl text-sm leading-7 text-muted-foreground">
              {getRoleDescription(data.roleLevel)}
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Button asChild variant="outline">
              <Link href="/resources">
                打开资源库
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/dashboard/settings">
                账户设置
                <Settings className="h-4 w-4" />
              </Link>
            </Button>
            {canEditResume ? (
              <Button asChild>
                <Link href="/dashboard/resume">
                  维护简历
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            ) : null}
          </div>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {[
          ["账号", data.username],
          ["邮箱", data.email || "--"],
          ["余额", formatBalance(data.balance)],
          ["用户 ID", data.id ? String(data.id) : "--"],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg border bg-card p-5 shadow-sm">
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-3 text-lg font-semibold">{value}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <Mail className="h-4 w-4 text-primary" />
            <h3 className="font-semibold">联系信息</h3>
          </div>
          <dl className="mt-5 space-y-4 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">邮箱</dt>
              <dd>{data.email || "--"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">手机号</dt>
              <dd>{data.phone || "--"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">QQ / 微信</dt>
              <dd>{data.contact || "--"}</dd>
            </div>
          </dl>
        </div>

        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-primary" />
            <h3 className="font-semibold">学籍与协会信息</h3>
          </div>
          <dl className="mt-5 space-y-4 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">学院</dt>
              <dd>{data.college || "--"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">班级</dt>
              <dd>{data.className || "--"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">学号</dt>
              <dd>{data.studentId || "--"}</dd>
            </div>
          </dl>
        </div>
      </div>

      {loading ? <Skeleton className="h-40 w-full rounded-lg" /> : null}
    </section>
  )
}
