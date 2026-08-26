"use client"

import Link from "next/link"
import {
  ArrowRight,
  ClipboardCheck,
  FileText,
  HardDrive,
  Settings,
  Trophy,
  Users2,
  Vote,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleDescription, getRoleLabel, hasRoleLevel } from "@/lib/access"
import { useAuthStore } from "@/store/useAuthStore"

export default function DashboardPage() {
  const { user } = useAuthStore()
  const isClient = useIsClient()

  if (!isClient || !user) return null

  const quickActions = [
    {
      href: "/dashboard/resources",
      title: "资源库",
      description: "查看协会资料、模板与工具集合。",
      icon: HardDrive,
      minLevel: 1,
    },
    {
      href: "/dashboard/resume",
      title: "我的简历",
      description: "维护 Markdown 简历并提交审核。",
      icon: FileText,
      minLevel: 2,
    },
    {
      href: "/dashboard/resume-reviews",
      title: "简历审核",
      description: "处理成员提交的简历审核队列。",
      icon: ClipboardCheck,
      minLevel: 3,
    },
    {
      href: "/dashboard/competitions",
      title: "比赛看板",
      description: "浏览当前比赛安排与活动信息。",
      icon: Trophy,
      minLevel: 3,
    },
    {
      href: "/dashboard/vote",
      title: "提案中心",
      description: "发起提案并参与组织投票。",
      icon: Vote,
      minLevel: 3,
    },
    {
      href: "/dashboard/departments",
      title: "部门人事",
      description: "查看部门负责人并完成部长任命。",
      icon: Users2,
      minLevel: 4,
    },
    {
      href: "/dashboard/settings",
      title: "公开设置",
      description: "维护首页与关于页展示内容。",
      icon: Settings,
      minLevel: 4,
    },
  ].filter((item) => hasRoleLevel(user.roleLevel, item.minLevel))

  return (
    <div className="space-y-6">
      <section className="rounded-lg border bg-background p-6 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">控制台概览</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-muted-foreground">
              你好，{user.username}。当前身份是 {getRoleLabel(user.roleLevel)}，
              {getRoleDescription(user.roleLevel)}
            </p>
          </div>
          <Button asChild>
            <Link href="/">
              返回首页
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {[
          ["身份等级", getRoleLabel(user.roleLevel)],
          ["登录账号", user.username],
          ["可用入口", String(quickActions.length)],
          ["当前状态", "运行正常"],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg border bg-background p-5 shadow-sm">
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-3 text-xl font-semibold">{value}</p>
          </div>
        ))}
      </section>

      <section className="space-y-4">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
          <h2 className="text-xl font-semibold">快捷入口</h2>
          <p className="text-sm text-muted-foreground">
            按当前角色展示已解锁的工作区
          </p>
        </div>

        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {quickActions.map((item) => {
            const Icon = item.icon

            return (
              <Link
                key={item.href}
                href={item.href}
                className="rounded-lg border bg-background p-5 shadow-sm transition-colors hover:border-primary/40"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="rounded-lg bg-primary/10 p-3 text-primary">
                    <Icon className="h-5 w-5" />
                  </div>
                  <ArrowRight className="h-4 w-4 text-muted-foreground" />
                </div>
                <h3 className="mt-5 font-semibold">{item.title}</h3>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  {item.description}
                </p>
              </Link>
            )
          })}
        </div>
      </section>

      <section className="rounded-lg border bg-background p-6 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold">系统状态</h2>
            <p className="mt-2 text-sm leading-7 text-muted-foreground">
              当前账号会话已通过后端校验，资源、竞赛、提案和部门入口会按身份等级开放。
            </p>
          </div>
          <Button asChild variant="outline">
            <Link href="/dashboard/profile">
              查看个人资料
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </div>
      </section>
    </div>
  )
}
