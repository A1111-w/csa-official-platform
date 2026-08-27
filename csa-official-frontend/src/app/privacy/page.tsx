"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Clock3, FileKey2, Info, Mail, ShieldCheck } from "lucide-react"
import { toast } from "sonner"

import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { publicService, type PrivacyNotice } from "@/services/public"

const fieldLabels: Record<string, string> = {
  username: "登录账号",
  realName: "真实姓名",
  email: "邮箱",
  studentId: "学号",
  college: "学院",
  className: "班级",
  phone: "手机号",
  contact: "其他联系方式",
  avatar: "头像",
  resume: "简历内容和仓库地址",
  uploadedFiles: "上传文件元数据",
  securityEvents: "安全与管理事件",
}

const rightLabels: Record<string, string> = {
  access: "查阅个人信息",
  export: "导出个人数据",
  correction: "申请更正信息",
  sessionRevocation: "吊销全部会话",
  deactivation: "停用账号",
  deletionRequest: "提交删除申请",
}

function purposeLabel(key: string) {
  if (key === "account") return "账号与权限"
  if (key === "operations") return "协会业务"
  return "安全保障"
}

function retentionLabel(key: string) {
  if (key === "activeAccount") return "正常账号"
  if (key === "deletionRequest") return "删除申请"
  if (key === "audit") return "审计记录"
  return "备份副本"
}

export default function PrivacyPage() {
  const [notice, setNotice] = useState<PrivacyNotice | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    publicService.getPrivacyNotice()
      .then((response) => {
        if (!cancelled) setNotice(response)
      })
      .catch((error) => {
        if (!cancelled) toast.error(error instanceof Error ? error.message : "隐私说明加载失败")
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-5xl px-4 py-12 md:px-8">
        <section className="max-w-3xl">
          <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <h1 className="text-4xl font-semibold tracking-tight">隐私说明</h1>
          <p className="mt-3 text-sm leading-8 text-muted-foreground">
            这里公开说明协会平台收集哪些信息、为什么使用、保存多久，以及账号持有人的可用权利。
          </p>
          {notice ? (
            <p className="mt-4 text-xs text-muted-foreground">当前版本：{notice.policyVersion}</p>
          ) : null}
        </section>

        {loading ? (
          <div className="mt-10 grid gap-4 md:grid-cols-2">
            <Skeleton className="h-64 w-full rounded-lg" />
            <Skeleton className="h-64 w-full rounded-lg" />
          </div>
        ) : notice ? (
          <div className="mt-10 grid gap-4 md:grid-cols-2">
            <section className="rounded-lg border bg-card p-6 shadow-sm">
              <div className="flex items-center gap-2">
                <FileKey2 className="h-4 w-4 text-primary" />
                <h2 className="font-semibold">收集的信息</h2>
              </div>
              <ul className="mt-5 grid gap-3 text-sm text-muted-foreground sm:grid-cols-2">
                {notice.collectedFields.map((field) => <li key={field}>· {fieldLabels[field] ?? field}</li>)}
              </ul>
            </section>

            <section className="rounded-lg border bg-card p-6 shadow-sm">
              <div className="flex items-center gap-2">
                <Info className="h-4 w-4 text-primary" />
                <h2 className="font-semibold">使用目的</h2>
              </div>
              <dl className="mt-5 space-y-4 text-sm">
                {Object.entries(notice.purposes).map(([key, value]) => (
                  <div key={key}>
                    <dt className="font-medium">{purposeLabel(key)}</dt>
                    <dd className="mt-1 leading-7 text-muted-foreground">{value}</dd>
                  </div>
                ))}
              </dl>
            </section>

            <section className="rounded-lg border bg-card p-6 shadow-sm">
              <div className="flex items-center gap-2">
                <Clock3 className="h-4 w-4 text-primary" />
                <h2 className="font-semibold">保存期限</h2>
              </div>
              <dl className="mt-5 space-y-4 text-sm">
                {Object.entries(notice.retention).map(([key, value]) => (
                  <div key={key}>
                    <dt className="font-medium">{retentionLabel(key)}</dt>
                    <dd className="mt-1 leading-7 text-muted-foreground">{value}</dd>
                  </div>
                ))}
              </dl>
            </section>

            <section className="rounded-lg border bg-card p-6 shadow-sm">
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <h2 className="font-semibold">你的权利</h2>
              </div>
              <ul className="mt-5 space-y-3 text-sm text-muted-foreground">
                {notice.userRights.map((right) => <li key={right}>· {rightLabels[right] ?? right}</li>)}
              </ul>
            </section>

            <section className="rounded-lg border bg-card p-6 shadow-sm md:col-span-2">
              <div className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-primary" />
                <h2 className="font-semibold">联系与操作</h2>
              </div>
              <p className="mt-4 text-sm leading-7 text-muted-foreground">
                隐私相关问题请联系：{notice.contactEmail}。登录后可以在账户设置中导出数据、吊销会话、停用账号或提交删除申请。
              </p>
              <Button asChild className="mt-5" variant="outline">
                <Link href="/dashboard/settings">打开账户设置</Link>
              </Button>
            </section>
          </div>
        ) : (
          <Alert variant="destructive" className="mt-10">
            <Info />
            <AlertTitle>暂时无法加载隐私说明</AlertTitle>
            <AlertDescription>请稍后重试，或通过站点公开联系方式联系我们。</AlertDescription>
          </Alert>
        )}
      </main>
      <Footer />
    </div>
  )
}
