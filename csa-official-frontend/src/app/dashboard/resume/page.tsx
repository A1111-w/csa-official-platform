"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import {
  AlertCircle,
  CheckCircle2,
  GitBranch,
  Loader2,
  RefreshCw,
  Save,
  Send,
} from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime } from "@/lib/format"
import {
  RESUME_STATUS,
  resumeService,
  type ResumeData,
  type ResumeGitSyncData,
  type ResumeGitSyncStatus,
} from "@/services/resume"
import { useAuthStore } from "@/store/useAuthStore"

function validateGitUrl(url: string) {
  if (!url) {
    return true
  }

  return /^https:\/\/[^ "]+$/.test(url)
}

function syncStatusLabel(status: ResumeGitSyncStatus | undefined) {
  if (status === "SYNCING") return "同步中"
  if (status === "SUCCEEDED") return "同步成功"
  if (status === "FAILED") return "同步失败"
  return "尚未同步"
}

function syncStatusClassName(status: ResumeGitSyncStatus | undefined) {
  if (status === "SUCCEEDED") return "bg-emerald-500/10 text-emerald-700"
  if (status === "FAILED") return "bg-rose-500/10 text-rose-700"
  if (status === "SYNCING") return "bg-amber-500/10 text-amber-700"
  return "bg-secondary text-secondary-foreground"
}

function formatBytes(size: number | null | undefined) {
  if (size == null) return null
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function syncErrorLabel(errorCode: string | null | undefined) {
  if (!errorCode) return "仓库同步失败，请稍后重试。"
  if (errorCode === "GIT_REPOSITORY_TOO_LARGE") return "仓库超过服务器允许的大小。"
  if (errorCode === "GIT_SYNC_QUEUE_FULL") return "同步任务较多，请稍后重试。"
  if (errorCode === "UPSTREAM_ERROR") return "远程仓库暂时无法访问。"
  return `同步失败（${errorCode}）`
}

function getStatusBadge(status: number) {
  if (status === RESUME_STATUS.APPROVED) {
    return (
      <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-sm text-emerald-600">
        已通过
      </span>
    )
  }
  if (status === RESUME_STATUS.REJECTED) {
    return (
      <span className="rounded-full bg-rose-500/10 px-3 py-1 text-sm text-rose-600">
        已驳回
      </span>
    )
  }
  if (status === RESUME_STATUS.PENDING) {
    return (
      <span className="rounded-full bg-amber-500/10 px-3 py-1 text-sm text-amber-600">
        审核中
      </span>
    )
  }
  return (
    <span className="rounded-full bg-secondary px-3 py-1 text-sm text-secondary-foreground">
      草稿
    </span>
  )
}

export default function ResumePage() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [syncSubmitting, setSyncSubmitting] = useState(false)
  const [resume, setResume] = useState<ResumeData | null>(null)
  const [gitSync, setGitSync] = useState<ResumeGitSyncData | null>(null)
  const [gitUrl, setGitUrl] = useState("")
  const [content, setContent] = useState("")

  const canAccess = Boolean(user) && hasRoleLevel(user?.roleLevel, 2)

  async function loadData() {
    try {
      const [data, syncStatus] = await Promise.all([
        resumeService.getMyResume(),
        resumeService.getGitSyncStatus(),
      ])
      setResume(data)
      setGitSync(syncStatus)
      setGitUrl(data?.gitRepoUrl || "")
      setContent(data?.content || "")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "简历加载失败")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isClient || !canAccess) {
      setLoading(false)
      return
    }

    void loadData()
  }, [canAccess, isClient])

  useEffect(() => {
    if (!canAccess || gitSync?.status !== "SYNCING") return

    const timer = window.setInterval(async () => {
      try {
        const status = await resumeService.getGitSyncStatus()
        setGitSync(status)
        if (status.status !== "SYNCING") {
          const data = await resumeService.getMyResume()
          setResume(data)
        }
      } catch {
        // Keep the last known state; the user can retry with the refresh button.
      }
    }, 2000)

    return () => window.clearInterval(timer)
  }, [canAccess, gitSync?.status])

  async function handleSave() {
    if (gitUrl && !validateGitUrl(gitUrl)) {
      toast.error("Git 链接格式不正确，请输入完整的 HTTP/HTTPS 地址")
      return
    }

    if (!gitUrl.trim() && !content.trim()) {
      toast.error("请至少填写仓库链接或简历内容")
      return
    }

    setSubmitting(true)
    try {
      await resumeService.save({
        content: content.trim(),
        gitRepoUrl: gitUrl.trim(),
      })
      toast.success("简历草稿已保存")
      await loadData()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "保存失败")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSubmit() {
    if (!content.trim() && !gitUrl.trim()) {
      toast.error("请先补充简历内容后再提交")
      return
    }

    setSubmitting(true)
    try {
      await resumeService.save({
        content: content.trim(),
        gitRepoUrl: gitUrl.trim(),
      })
      await resumeService.submit()
      toast.success("已提交审核，请耐心等待部长批阅")
      await loadData()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "提交失败")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleGitSync() {
    const normalizedGitUrl = gitUrl.trim()
    if (!normalizedGitUrl || !validateGitUrl(normalizedGitUrl)) {
      toast.error("请先填写有效的 HTTPS 仓库地址")
      return
    }

    setSyncSubmitting(true)
    try {
      if (!isPending && normalizedGitUrl !== (resume?.gitRepoUrl || "")) {
        await resumeService.save({
          content: content.trim(),
          gitRepoUrl: normalizedGitUrl,
        })
      }
      const status = await resumeService.syncGitRepository()
      setGitSync(status)
      toast.success("仓库同步任务已启动")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "仓库同步启动失败")
    } finally {
      setSyncSubmitting(false)
    }
  }

  async function refreshGitSyncStatus() {
    try {
      setGitSync(await resumeService.getGitSyncStatus())
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "同步状态刷新失败")
    }
  }

  if (!isClient) {
    return null
  }

  if (!user) {
    return (
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">请先登录</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          登录后才可以维护自己的简历工作区。
        </p>
        <div className="mt-5">
          <Button asChild>
            <Link href="/login">去登录</Link>
          </Button>
        </div>
      </div>
    )
  }

  if (!hasRoleLevel(user?.roleLevel, 2)) {
    return (
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">当前角色还未解锁简历工作区</h1>
        <p className="mt-3 text-sm leading-7 text-muted-foreground">
          简历模块面向核心成员及以上开放。完成角色升级后，这里会自动开放。
        </p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const status = resume?.status ?? RESUME_STATUS.DRAFT
  const isPending = status === RESUME_STATUS.PENDING

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">我的简历</h1>
          <p className="mt-2 text-sm leading-7 text-muted-foreground">
            用 Markdown 维护项目经历、技术栈与代表作品，再提交给部长审核。
          </p>
        </div>
        {getStatusBadge(status)}
      </div>

      {status === RESUME_STATUS.REJECTED ? (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>审核未通过</AlertTitle>
          <AlertDescription>
            {resume?.rejectReason || "暂无驳回原因，请联系审核人。"}
          </AlertDescription>
        </Alert>
      ) : null}

      {status === RESUME_STATUS.APPROVED ? (
        <Alert className="border-emerald-500/40 bg-emerald-500/5 text-emerald-700">
          <CheckCircle2 className="h-4 w-4" />
          <AlertTitle>简历已通过审核</AlertTitle>
          <AlertDescription>
            你仍然可以继续编辑，但再次提交后会重新进入审核队列。
          </AlertDescription>
        </Alert>
      ) : null}

      {status === RESUME_STATUS.PENDING ? (
        <Alert className="border-amber-500/40 bg-amber-500/5 text-amber-700">
          <Loader2 className="h-4 w-4 animate-spin" />
          <AlertTitle>简历审核中</AlertTitle>
          <AlertDescription>
            审核期间暂时锁定编辑，请等待结果通知。
          </AlertDescription>
        </Alert>
      ) : null}

      <Card className="rounded-lg">
        <CardHeader>
          <CardTitle>简历内容</CardTitle>
          <CardDescription>
            建议包含个人简介、技术栈、项目经历、比赛成果，以及对应仓库链接。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="gitUrl" className="flex items-center gap-2">
              <GitBranch className="h-4 w-4" />
              项目仓库链接
            </Label>
            <div className="flex flex-col gap-3 sm:flex-row">
              <Input
                id="gitUrl"
                value={gitUrl}
                onChange={(event) => setGitUrl(event.target.value)}
                placeholder="例如 https://github.com/yourname/project"
                disabled={isPending}
                className={
                  gitUrl && !validateGitUrl(gitUrl)
                    ? "border-rose-500 focus-visible:ring-rose-500"
                    : ""
                }
              />
              {gitUrl && validateGitUrl(gitUrl) ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => window.open(gitUrl, "_blank", "noopener,noreferrer")}
                >
                  打开链接
                </Button>
              ) : null}
            </div>
            {gitUrl && !validateGitUrl(gitUrl) ? (
              <p className="text-xs text-rose-600">
                  链接格式错误，请输入完整的 HTTPS 地址。
              </p>
            ) : null}
          </div>

          <div className="space-y-4 rounded-lg border bg-muted/30 p-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-medium">仓库同步</p>
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs ${syncStatusClassName(gitSync?.status)}`}
                  >
                    {syncStatusLabel(gitSync?.status)}
                  </span>
                </div>
                <p className="mt-2 text-xs text-muted-foreground">
                  同步后审核人可以确认仓库分支、提交版本与大小。
                </p>
              </div>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  title="刷新同步状态"
                  aria-label="刷新同步状态"
                  onClick={() => void refreshGitSyncStatus()}
                  disabled={gitSync?.status === "SYNCING"}
                >
                  <RefreshCw className={gitSync?.status === "SYNCING" ? "animate-spin" : ""} />
                </Button>
                <Button
                  type="button"
                  onClick={() => void handleGitSync()}
                  disabled={
                    syncSubmitting ||
                    gitSync?.status === "SYNCING" ||
                    !gitUrl.trim() ||
                    !validateGitUrl(gitUrl.trim())
                  }
                >
                  {syncSubmitting || gitSync?.status === "SYNCING" ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    <GitBranch />
                  )}
                  同步仓库
                </Button>
              </div>
            </div>

            {gitSync?.status === "SUCCEEDED" ? (
              <dl className="grid gap-3 border-t pt-4 text-sm sm:grid-cols-3">
                <div>
                  <dt className="text-xs text-muted-foreground">分支</dt>
                  <dd className="mt-1 truncate font-mono">{gitSync.branch || "-"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">提交</dt>
                  <dd className="mt-1 truncate font-mono" title={gitSync.commit || undefined}>
                    {gitSync.commit?.slice(0, 12) || "-"}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">仓库大小</dt>
                  <dd className="mt-1">{formatBytes(gitSync.sizeBytes) || "-"}</dd>
                </div>
              </dl>
            ) : null}

            {gitSync?.status === "FAILED" ? (
              <p className="border-t pt-4 text-sm text-rose-700">
                {syncErrorLabel(gitSync.errorCode)}
              </p>
            ) : null}

            {gitSync?.completedAt ? (
              <p className="text-xs text-muted-foreground">
                最近完成：{formatDateTime(gitSync.completedAt)}
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="content">详细介绍（Markdown）</Label>
            <Textarea
              id="content"
              className="min-h-[360px] font-mono text-sm leading-7"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              disabled={isPending}
              placeholder={[
                "# 个人简介",
                "- 擅长方向：前端 / Java / 算法 / 运维",
                "",
                "# 项目经历",
                "- 项目名：你做了什么，解决了什么问题",
                "",
                "# 比赛与协作",
                "- 参加过哪些比赛、承担过哪些协会工作",
              ].join("\n")}
            />
          </div>

          <div className="rounded-lg border bg-muted/40 p-4 text-sm text-muted-foreground">
            <p className="font-medium text-foreground">填写建议</p>
            <ul className="mt-3 space-y-2">
              <li>优先放最能代表你技术深度的项目，而不是罗列太多名字。</li>
              <li>如果提供仓库链接，请确保是公开仓库，方便审核时直接查看代码。</li>
              <li>比赛、活动和协会协作内容都可以写进来，重点写清你负责了什么。</li>
            </ul>
          </div>
        </CardContent>
        <CardFooter className="flex flex-col gap-4 border-t bg-muted/30 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-muted-foreground">
            {resume?.updateTime ? `上次保存：${formatDateTime(resume.updateTime)}` : "还没有保存记录"}
          </p>

          <div className="flex gap-3">
            <Button variant="outline" onClick={handleSave} disabled={submitting || isPending}>
              <Save className="h-4 w-4" />
              保存草稿
            </Button>
            <Button onClick={handleSubmit} disabled={submitting || isPending}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              提交审核
            </Button>
          </div>
        </CardFooter>
      </Card>
    </div>
  )
}
