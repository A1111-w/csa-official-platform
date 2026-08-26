"use client"

import { useCallback, useEffect, useState } from "react"
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
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
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime } from "@/lib/format"
import { auditService, type AuditLogItem } from "@/services/audit"
import { useAuthStore } from "@/store/useAuthStore"

const PAGE_SIZE = 20

interface AuditFilters {
  action: string
  result: string
  requestId: string
}

const EMPTY_FILTERS: AuditFilters = {
  action: "",
  result: "",
  requestId: "",
}

function resultClassName(result: string) {
  if (result === "SUCCESS") return "bg-emerald-500/10 text-emerald-700"
  if (result === "FAILURE") return "bg-rose-500/10 text-rose-700"
  return "bg-amber-500/10 text-amber-700"
}

function formatDetails(value: string | null) {
  if (!value) return "{}"

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export function AuditLogWorkspace() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [items, setItems] = useState<AuditLogItem[]>([])
  const [filters, setFilters] = useState<AuditFilters>(EMPTY_FILTERS)
  const [appliedFilters, setAppliedFilters] = useState<AuditFilters>(EMPTY_FILTERS)
  const [selected, setSelected] = useState<AuditLogItem | null>(null)

  const canViewAudit = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)

  const loadPage = useCallback(
    async (requestedPage: number, currentFilters: AuditFilters) => {
      setLoading(true)
      try {
        let response = await auditService.list({
          page: requestedPage,
          size: PAGE_SIZE,
          action: currentFilters.action || undefined,
          result: currentFilters.result || undefined,
          requestId: currentFilters.requestId || undefined,
        })
        const lastPage = Math.max(response.pages || 1, 1)

        if (requestedPage > lastPage) {
          response = await auditService.list({
            page: lastPage,
            size: PAGE_SIZE,
            action: currentFilters.action || undefined,
            result: currentFilters.result || undefined,
            requestId: currentFilters.requestId || undefined,
          })
          setPage(lastPage)
        }

        setItems(response.records)
        setTotal(response.total)
        setPages(lastPage)
      } catch (error) {
        setItems([])
        setTotal(0)
        setPages(1)
        toast.error(error instanceof Error ? error.message : "审计日志加载失败")
      } finally {
        setLoading(false)
      }
    },
    []
  )

  useEffect(() => {
    if (!isClient || !canViewAudit) {
      setLoading(false)
      return
    }

    void loadPage(page, appliedFilters)
  }, [appliedFilters, canViewAudit, isClient, loadPage, page])

  function applyFilters() {
    setPage(1)
    setAppliedFilters({
      action: filters.action.trim(),
      result: filters.result,
      requestId: filters.requestId.trim(),
    })
  }

  function resetFilters() {
    setFilters(EMPTY_FILTERS)
    setAppliedFilters(EMPTY_FILTERS)
    setPage(1)
  }

  if (!isClient) {
    return <Skeleton className="h-[620px] w-full rounded-lg" />
  }

  if (!canViewAudit) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">无权查看管理审计日志</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          该页面仅向会长及以上角色开放。
        </p>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-primary">
            <ShieldCheck className="h-5 w-5" />
            <span className="text-sm font-medium">管理审计</span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">审计日志</h1>
          <p className="mt-2 text-sm text-muted-foreground">共 {total} 条记录</p>
        </div>
        <Button
          variant="outline"
          onClick={() => void loadPage(page, appliedFilters)}
          disabled={loading}
        >
          <RefreshCw className={loading ? "animate-spin" : ""} />
          刷新
        </Button>
      </header>

      <div className="grid gap-3 border-y py-5 lg:grid-cols-[1fr_0.65fr_1fr_auto_auto]">
        <Input
          value={filters.action}
          onChange={(event) =>
            setFilters((current) => ({ ...current, action: event.target.value }))
          }
          placeholder="操作类型，例如 ROLE_CHANGE"
        />
        <select
          value={filters.result}
          onChange={(event) =>
            setFilters((current) => ({ ...current, result: event.target.value }))
          }
          className="h-9 rounded-md border bg-background px-3 text-sm"
          aria-label="审计结果"
        >
          <option value="">全部结果</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FAILURE">FAILURE</option>
          <option value="ACCEPTED">ACCEPTED</option>
        </select>
        <Input
          value={filters.requestId}
          onChange={(event) =>
            setFilters((current) => ({ ...current, requestId: event.target.value }))
          }
          placeholder="精确查询 Request ID"
          onKeyDown={(event) => {
            if (event.key === "Enter") applyFilters()
          }}
        />
        <Button onClick={applyFilters} disabled={loading}>
          <Search />
          查询
        </Button>
        <Button variant="outline" onClick={resetFilters} disabled={loading}>
          <RotateCcw />
          重置
        </Button>
      </div>

      <div className="space-y-3">
        {loading ? (
          Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))
        ) : items.length ? (
          items.map((item) => (
            <article key={item.id} className="rounded-lg border bg-card p-5 shadow-sm">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-mono text-sm font-semibold">{item.action}</h2>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs ${resultClassName(item.result)}`}
                    >
                      {item.result}
                    </span>
                  </div>
                  <p className="mt-3 text-sm text-muted-foreground">
                    {item.actorUsername || "系统任务"}
                    {item.actorUserId ? ` (#${item.actorUserId})` : ""}
                    {item.targetType ? ` · ${item.targetType}` : ""}
                    {item.targetId ? ` #${item.targetId}` : ""}
                  </p>
                  <p className="mt-2 truncate font-mono text-xs text-muted-foreground">
                    Request ID: {item.requestId || "-"}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <span className="text-xs text-muted-foreground">
                    {formatDateTime(item.createTime)}
                  </span>
                  <Button variant="outline" size="sm" onClick={() => setSelected(item)}>
                    <Eye />
                    详情
                  </Button>
                </div>
              </div>
            </article>
          ))
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-14 text-center text-sm text-muted-foreground">
            当前筛选条件下没有审计记录。
          </div>
        )}
      </div>

      <div className="flex items-center justify-between border-t pt-4">
        <Button
          variant="outline"
          disabled={page <= 1 || loading}
          onClick={() => setPage((current) => Math.max(1, current - 1))}
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

      <Dialog open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
          {selected ? (
            <>
              <DialogHeader>
                <DialogTitle className="font-mono">{selected.action}</DialogTitle>
                <DialogDescription>
                  #{selected.id} · {formatDateTime(selected.createTime)}
                </DialogDescription>
              </DialogHeader>

              <dl className="grid gap-4 border-y py-4 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-muted-foreground">执行人</dt>
                  <dd className="mt-1 break-words font-medium">
                    {selected.actorUsername || "系统任务"}
                    {selected.actorUserId ? ` (#${selected.actorUserId})` : ""}
                  </dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">结果</dt>
                  <dd className="mt-1 font-medium">{selected.result}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">目标</dt>
                  <dd className="mt-1 break-words font-medium">
                    {selected.targetType || "-"} {selected.targetId || ""}
                  </dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">IP 地址</dt>
                  <dd className="mt-1 break-words font-mono text-xs">
                    {selected.ipAddress || "-"}
                  </dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-muted-foreground">Request ID</dt>
                  <dd className="mt-1 break-all font-mono text-xs">
                    {selected.requestId || "-"}
                  </dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-muted-foreground">User Agent</dt>
                  <dd className="mt-1 break-words text-xs">{selected.userAgent || "-"}</dd>
                </div>
              </dl>

              <div>
                <p className="mb-2 text-sm font-medium">结构化详情</p>
                <pre className="max-h-72 overflow-auto whitespace-pre-wrap break-words rounded-lg border bg-muted/40 p-4 font-mono text-xs leading-6">
                  {formatDetails(selected.detailsJson)}
                </pre>
              </div>

              <DialogFooter>
                <Button variant="outline" onClick={() => setSelected(null)}>
                  关闭
                </Button>
              </DialogFooter>
            </>
          ) : null}
        </DialogContent>
      </Dialog>
    </section>
  )
}
