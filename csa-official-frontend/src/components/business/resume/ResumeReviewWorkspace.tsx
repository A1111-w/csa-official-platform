"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import {
  Building2,
  Check,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Eye,
  FileText,
  Loader2,
  RefreshCw,
  UserRound,
  X,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime } from "@/lib/format"
import {
  RESUME_STATUS,
  isPendingResumeReview,
  resumeService,
  type ResumeReviewDetail,
  type ResumeReviewListItem,
  type ResumeReviewStatus,
} from "@/services/resume"
import { useAuthStore } from "@/store/useAuthStore"

const PAGE_SIZE = 12

const REVIEW_FILTERS: Array<{ value: ResumeReviewStatus; label: string }> = [
  { value: RESUME_STATUS.PENDING, label: "待审核" },
  { value: RESUME_STATUS.APPROVED, label: "已通过" },
  { value: RESUME_STATUS.REJECTED, label: "已驳回" },
]

function statusLabel(status: number) {
  if (status === RESUME_STATUS.APPROVED) return "已通过"
  if (status === RESUME_STATUS.REJECTED) return "已驳回"
  return "待审核"
}

function statusClassName(status: number) {
  if (status === RESUME_STATUS.APPROVED) {
    return "bg-emerald-500/10 text-emerald-700"
  }
  if (status === RESUME_STATUS.REJECTED) {
    return "bg-rose-500/10 text-rose-700"
  }
  return "bg-amber-500/10 text-amber-700"
}

function applicantName(item: Pick<ResumeReviewListItem, "realName" | "username">) {
  return item.realName || item.username || "未命名成员"
}

export function ResumeReviewWorkspace() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [status, setStatus] = useState<ResumeReviewStatus>(RESUME_STATUS.PENDING)
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [items, setItems] = useState<ResumeReviewListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [reviewing, setReviewing] = useState(false)
  const [detail, setDetail] = useState<ResumeReviewDetail | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState("")

  const canReview = Boolean(user) && hasRoleLevel(user?.roleLevel, 3)

  const currentFilterLabel = useMemo(
    () => REVIEW_FILTERS.find((filter) => filter.value === status)?.label || "简历",
    [status]
  )

  const loadReviews = useCallback(
    async (requestedPage: number, requestedStatus: ResumeReviewStatus) => {
      setLoading(true)
      try {
        let response = await resumeService.listReviews({
          page: requestedPage,
          size: PAGE_SIZE,
          status: requestedStatus,
        })
        const lastPage = Math.max(response.pages || 1, 1)

        if (requestedPage > lastPage) {
          response = await resumeService.listReviews({
            page: lastPage,
            size: PAGE_SIZE,
            status: requestedStatus,
          })
          setPage(lastPage)
        }

        setItems(response.records)
        setTotal(response.total)
        setPages(Math.max(response.pages || 1, 1))
      } catch (error) {
        setItems([])
        setTotal(0)
        setPages(1)
        toast.error(error instanceof Error ? error.message : "审核队列加载失败")
      } finally {
        setLoading(false)
      }
    },
    []
  )

  useEffect(() => {
    if (!isClient || !canReview) {
      setLoading(false)
      return
    }

    void loadReviews(page, status)
  }, [canReview, isClient, loadReviews, page, status])

  function changeStatus(nextStatus: ResumeReviewStatus) {
    setStatus(nextStatus)
    setPage(1)
  }

  async function openDetail(item: ResumeReviewListItem) {
    setDialogOpen(true)
    setDetail(null)
    setRejectReason("")
    setDetailLoading(true)
    try {
      const response = await resumeService.reviewDetail(item.id)
      setDetail(response)
      setRejectReason(response.rejectReason || "")
    } catch (error) {
      setDialogOpen(false)
      toast.error(error instanceof Error ? error.message : "简历详情加载失败")
    } finally {
      setDetailLoading(false)
    }
  }

  async function submitDecision(pass: boolean) {
    if (!detail) return

    const normalizedReason = rejectReason.trim()
    if (!pass && !normalizedReason) {
      toast.error("驳回时必须填写原因")
      return
    }

    setReviewing(true)
    try {
      await resumeService.audit({
        resumeId: detail.id,
        pass,
        reason: pass ? undefined : normalizedReason,
      })
      toast.success(pass ? "简历已通过" : "简历已驳回")
      setDialogOpen(false)
      setDetail(null)

      const nextPage = items.length === 1 && page > 1 ? page - 1 : page
      if (nextPage !== page) setPage(nextPage)
      await loadReviews(nextPage, status)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "审核提交失败")
    } finally {
      setReviewing(false)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[520px] w-full rounded-lg" />
  }

  if (!canReview) {
    return (
      <div className="rounded-lg border bg-background p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">无权访问简历审核</h1>
        <p className="mt-3 text-sm text-muted-foreground">该页面仅对部长及以上角色开放。</p>
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">简历审核</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {currentFilterLabel} {total} 份
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => void loadReviews(page, status)}
          disabled={loading}
        >
          <RefreshCw className={loading ? "animate-spin" : ""} />
          刷新
        </Button>
      </header>

      <div className="flex flex-wrap gap-2 border-b pb-4" role="tablist" aria-label="审核状态">
        {REVIEW_FILTERS.map((filter) => (
          <Button
            key={filter.value}
            role="tab"
            aria-selected={status === filter.value}
            variant={status === filter.value ? "default" : "outline"}
            size="sm"
            onClick={() => changeStatus(filter.value)}
          >
            {filter.label}
          </Button>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {loading ? (
          Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-52 w-full rounded-lg" />
          ))
        ) : items.length ? (
          items.map((item) => (
            <article key={item.id} className="rounded-lg border bg-background p-5 shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <UserRound className="h-4 w-4 shrink-0 text-muted-foreground" />
                    <h2 className="truncate font-semibold">{applicantName(item)}</h2>
                  </div>
                  <p className="mt-1 truncate text-sm text-muted-foreground">
                    @{item.username || "unknown"} · #{item.applicantId}
                  </p>
                </div>
                <span
                  className={`shrink-0 rounded-full px-2.5 py-1 text-xs ${statusClassName(item.status)}`}
                >
                  {statusLabel(item.status)}
                </span>
              </div>

              <div className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
                <Building2 className="h-4 w-4 shrink-0" />
                <span className="truncate">{item.departmentName || "未分配部门"}</span>
              </div>

              <p className="mt-4 line-clamp-3 min-h-[4.5rem] break-words text-sm leading-6 text-muted-foreground">
                {item.contentSummary || "仅提交了仓库链接"}
              </p>

              <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t pt-4">
                <span className="text-xs text-muted-foreground">
                  更新于 {formatDateTime(item.updateTime || item.createTime)}
                </span>
                <Button variant="outline" size="sm" onClick={() => void openDetail(item)}>
                  <Eye />
                  查看审核
                </Button>
              </div>
            </article>
          ))
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-14 text-center text-sm text-muted-foreground lg:col-span-2">
            当前状态下没有简历。
          </div>
        )}
      </div>

      <div className="flex items-center justify-between border-t pt-4">
        <Button
          variant="outline"
          disabled={page <= 1 || loading}
          onClick={() => setPage((current) => Math.max(current - 1, 1))}
        >
          <ChevronLeft />
          上一页
        </Button>
        <span className="text-sm text-muted-foreground">
          第 {page} / {pages} 页
        </span>
        <Button
          variant="outline"
          disabled={page >= pages || loading}
          onClick={() => setPage((current) => current + 1)}
        >
          下一页
          <ChevronRight />
        </Button>
      </div>

      <Dialog
        open={dialogOpen}
        onOpenChange={(open) => {
          if (!reviewing) setDialogOpen(open)
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
          {detailLoading || !detail ? (
            <div className="flex min-h-64 items-center justify-center">
              <Loader2 className="h-7 w-7 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <>
              <DialogHeader>
                <DialogTitle>{applicantName(detail)}</DialogTitle>
                <DialogDescription>
                  @{detail.username || "unknown"} · #{detail.applicantId} · {detail.departmentName || "未分配部门"}
                </DialogDescription>
              </DialogHeader>

              <div className="grid gap-3 border-y py-4 text-sm sm:grid-cols-2">
                <div>
                  <span className="text-muted-foreground">学号</span>
                  <p className="mt-1 break-words font-medium">{detail.studentId || "未填写"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">邮箱</span>
                  <p className="mt-1 break-words font-medium">{detail.email || "未填写"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">学院</span>
                  <p className="mt-1 break-words font-medium">{detail.college || "未填写"}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">班级</span>
                  <p className="mt-1 break-words font-medium">{detail.className || "未填写"}</p>
                </div>
              </div>

              {detail.gitRepoUrl ? (
                <a
                  href={detail.gitRepoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex min-w-0 items-center gap-2 rounded-lg border px-4 py-3 text-sm text-primary hover:bg-muted/40"
                >
                  <ExternalLink className="h-4 w-4 shrink-0" />
                  <span className="truncate">{detail.gitRepoUrl}</span>
                </a>
              ) : null}

              <section>
                <div className="mb-3 flex items-center gap-2">
                  <FileText className="h-4 w-4 text-muted-foreground" />
                  <h3 className="font-semibold">简历内容</h3>
                </div>
                <pre className="max-h-80 overflow-auto whitespace-pre-wrap break-words rounded-lg border bg-muted/30 p-4 font-sans text-sm leading-7">
                  {detail.content || "未填写文字内容"}
                </pre>
              </section>

              {detail.status === RESUME_STATUS.REJECTED && detail.rejectReason ? (
                <section className="rounded-lg border border-rose-500/30 bg-rose-500/5 p-4 text-sm">
                  <p className="font-semibold text-rose-700">驳回原因</p>
                  <p className="mt-2 whitespace-pre-wrap break-words text-rose-700/90">
                    {detail.rejectReason}
                  </p>
                </section>
              ) : null}

              {detail.auditTime ? (
                <p className="text-xs text-muted-foreground">
                  {detail.auditorName || "审核人"} · {formatDateTime(detail.auditTime)}
                </p>
              ) : null}

              {isPendingResumeReview(detail.status) ? (
                <div className="space-y-2">
                  <label htmlFor="reject-reason" className="text-sm font-medium">
                    驳回原因
                  </label>
                  <Textarea
                    id="reject-reason"
                    maxLength={500}
                    className="min-h-28"
                    value={rejectReason}
                    onChange={(event) => setRejectReason(event.target.value)}
                    placeholder="驳回时必填，最多 500 字"
                    disabled={reviewing}
                  />
                  <p className="text-right text-xs text-muted-foreground">
                    {rejectReason.length}/500
                  </p>
                </div>
              ) : null}

              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={reviewing}>
                  关闭
                </Button>
                {isPendingResumeReview(detail.status) ? (
                  <>
                    <Button
                      variant="destructive"
                      onClick={() => void submitDecision(false)}
                      disabled={reviewing || !rejectReason.trim()}
                    >
                      {reviewing ? <Loader2 className="animate-spin" /> : <X />}
                      驳回
                    </Button>
                    <Button onClick={() => void submitDecision(true)} disabled={reviewing}>
                      {reviewing ? <Loader2 className="animate-spin" /> : <Check />}
                      通过
                    </Button>
                  </>
                ) : null}
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
