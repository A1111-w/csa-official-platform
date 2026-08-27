"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import {
  Award,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  RotateCcw,
  Search,
  UserRoundSearch,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime } from "@/lib/format"
import {
  contributionService,
  type ContributionAwardItem,
  type ContributionSource,
  type ContributionType,
} from "@/services/contribution"
import { userService, type UserDirectoryItem } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"

const PAGE_SIZE = 20
const MEMBER_RESULT_LIMIT = 50

const CONTRIBUTION_TYPES: Array<{
  value: ContributionType
  label: string
  defaultScore: string
}> = [
  { value: "DEV", label: "官网建设", defaultScore: "10" },
  { value: "RES", label: "资源贡献", defaultScore: "1" },
  { value: "COMP", label: "发布比赛", defaultScore: "1" },
  { value: "OPS", label: "首页维护", defaultScore: "1" },
]

interface HistoryFilters {
  keyword: string
  type: "" | ContributionType
  source: "" | ContributionSource
}

const DEFAULT_FILTERS: HistoryFilters = {
  keyword: "",
  type: "",
  source: "MANUAL",
}

function displayName(member: UserDirectoryItem) {
  return member.realName || member.username
}

function formatScore(value: number | string) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toLocaleString("zh-CN") : String(value)
}

function sourceClassName(source: string) {
  if (source === "MANUAL") return "bg-amber-500/10 text-amber-700"
  if (source === "AUTO") return "bg-emerald-500/10 text-emerald-700"
  return "bg-secondary text-secondary-foreground"
}

export function ContributionAwardWorkspace() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [memberKeyword, setMemberKeyword] = useState("")
  const [memberLoading, setMemberLoading] = useState(false)
  const [memberResults, setMemberResults] = useState<UserDirectoryItem[]>([])
  const [selectedMember, setSelectedMember] = useState<UserDirectoryItem | null>(null)
  const [type, setType] = useState<ContributionType>("DEV")
  const [score, setScore] = useState("10")
  const [reason, setReason] = useState("")
  const [submitting, setSubmitting] = useState(false)

  const [loadingHistory, setLoadingHistory] = useState(true)
  const [items, setItems] = useState<ContributionAwardItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [filters, setFilters] = useState<HistoryFilters>(DEFAULT_FILTERS)
  const [appliedFilters, setAppliedFilters] = useState<HistoryFilters>(DEFAULT_FILTERS)

  const canManageContributions = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)
  const selectedType = useMemo(
    () => CONTRIBUTION_TYPES.find((item) => item.value === type),
    [type]
  )

  const loadHistory = useCallback(
    async (requestedPage: number, currentFilters: HistoryFilters) => {
      setLoadingHistory(true)
      try {
        let response = await contributionService.listAwards({
          page: requestedPage,
          size: PAGE_SIZE,
          keyword: currentFilters.keyword || undefined,
          type: currentFilters.type || undefined,
          source: currentFilters.source || undefined,
        })
        const lastPage = Math.max(response.pages || 1, 1)

        if (requestedPage > lastPage) {
          response = await contributionService.listAwards({
            page: lastPage,
            size: PAGE_SIZE,
            keyword: currentFilters.keyword || undefined,
            type: currentFilters.type || undefined,
            source: currentFilters.source || undefined,
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
        toast.error(error instanceof Error ? error.message : "贡献记录加载失败")
      } finally {
        setLoadingHistory(false)
      }
    },
    []
  )

  useEffect(() => {
    if (!isClient || !canManageContributions) {
      setLoadingHistory(false)
      return
    }

    void loadHistory(page, appliedFilters)
  }, [appliedFilters, canManageContributions, isClient, loadHistory, page])

  async function searchMembers() {
    const keyword = memberKeyword.trim()
    if (!keyword) {
      toast.error("请输入姓名、账号或邮箱")
      return
    }

    setMemberLoading(true)
    try {
      const response = await userService.list({
        keyword,
        size: MEMBER_RESULT_LIMIT,
      })
      setMemberResults(response)
      if (!response.length) {
        toast.info("没有找到匹配成员")
      }
    } catch (error) {
      setMemberResults([])
      toast.error(error instanceof Error ? error.message : "成员搜索失败")
    } finally {
      setMemberLoading(false)
    }
  }

  function changeType(nextType: ContributionType) {
    const option = CONTRIBUTION_TYPES.find((item) => item.value === nextType)
    setType(nextType)
    setScore(option?.defaultScore || "1")
  }

  async function submitAward() {
    const numericScore = Number(score)
    if (!selectedMember) {
      toast.error("请先选择成员")
      return
    }
    if (!Number.isFinite(numericScore) || numericScore <= 0) {
      toast.error("贡献分值必须大于 0")
      return
    }
    if (!reason.trim()) {
      toast.error("请填写贡献说明")
      return
    }

    const confirmed = window.confirm(
      `确认给 ${displayName(selectedMember)} 记录 ${formatScore(numericScore)} 分${selectedType?.label || "贡献"}吗？`
    )
    if (!confirmed) return

    setSubmitting(true)
    try {
      await contributionService.award({
        userId: selectedMember.id,
        type,
        score: numericScore,
        reason: reason.trim(),
      })
      toast.success("贡献记录已写入")
      setReason("")
      if (page === 1) {
        await loadHistory(1, appliedFilters)
      } else {
        setPage(1)
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "贡献记录写入失败")
    } finally {
      setSubmitting(false)
    }
  }

  function applyFilters() {
    setPage(1)
    setAppliedFilters({
      keyword: filters.keyword.trim(),
      type: filters.type,
      source: filters.source,
    })
  }

  function resetFilters() {
    setFilters(DEFAULT_FILTERS)
    setAppliedFilters(DEFAULT_FILTERS)
    setPage(1)
  }

  if (!isClient) {
    return <Skeleton className="h-[720px] w-full rounded-lg" />
  }

  if (!canManageContributions) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">无权管理贡献记录</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          该页面仅向会长及以上角色开放。
        </p>
      </section>
    )
  }

  return (
    <section className="space-y-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-primary">
            <Award className="h-5 w-5" />
            <span className="text-sm font-medium">组织贡献</span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">贡献记录管理</h1>
          <p className="mt-2 text-sm text-muted-foreground">共 {total} 条记录</p>
        </div>
        <Button
          variant="outline"
          onClick={() => void loadHistory(page, appliedFilters)}
          disabled={loadingHistory}
        >
          <RefreshCw className={loadingHistory ? "animate-spin" : ""} />
          刷新记录
        </Button>
      </header>

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <section className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <UserRoundSearch className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-semibold">选择成员</h2>
          </div>

          <div className="mt-5 flex gap-2">
            <Input
              value={memberKeyword}
              onChange={(event) => setMemberKeyword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") void searchMembers()
              }}
              placeholder="姓名、账号或邮箱"
              maxLength={64}
            />
            <Button
              variant="outline"
              onClick={() => void searchMembers()}
              disabled={memberLoading}
              aria-label="搜索成员"
              title="搜索成员"
            >
              {memberLoading ? <Loader2 className="animate-spin" /> : <Search />}
            </Button>
          </div>

          <div className="mt-4 max-h-80 space-y-2 overflow-auto pr-1">
            {memberResults.map((member) => {
              const selected = selectedMember?.id === member.id
              return (
                <button
                  key={member.id}
                  type="button"
                  onClick={() => setSelectedMember(member)}
                  className={`w-full rounded-lg border p-4 text-left transition-colors ${
                    selected
                      ? "border-primary bg-primary/5"
                      : "bg-background hover:border-primary/30"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate font-medium">{displayName(member)}</p>
                      <p className="mt-1 truncate text-sm text-muted-foreground">
                        @{member.username} · {member.departmentName || "未分配部门"}
                      </p>
                    </div>
                    <span className="shrink-0 text-xs text-muted-foreground">#{member.id}</span>
                  </div>
                </button>
              )
            })}
            {!memberLoading && !memberResults.length ? (
              <div className="rounded-lg border border-dashed px-4 py-10 text-center text-sm text-muted-foreground">
                搜索后选择成员
              </div>
            ) : null}
          </div>
        </section>

        <section className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <h2 className="text-xl font-semibold">新增人工记录</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {selectedMember
                  ? `${displayName(selectedMember)} · @${selectedMember.username}`
                  : "尚未选择成员"}
              </p>
            </div>
            {selectedMember ? (
              <Button variant="ghost" size="sm" onClick={() => setSelectedMember(null)}>
                清除选择
              </Button>
            ) : null}
          </div>

          <div className="mt-6 space-y-5">
            <fieldset>
              <legend className="text-sm font-medium">贡献类型</legend>
              <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                {CONTRIBUTION_TYPES.map((option) => (
                  <Button
                    key={option.value}
                    type="button"
                    variant={type === option.value ? "default" : "outline"}
                    onClick={() => changeType(option.value)}
                  >
                    {option.label}
                  </Button>
                ))}
              </div>
            </fieldset>

            <label className="block space-y-2 text-sm">
              <span className="font-medium">贡献分值</span>
              <Input
                type="number"
                min="0.01"
                max="99999999.99"
                step="0.01"
                value={score}
                onChange={(event) => setScore(event.target.value)}
              />
            </label>

            <label className="block space-y-2 text-sm">
              <span className="font-medium">贡献说明</span>
              <Textarea
                className="min-h-32"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                maxLength={500}
                placeholder="记录具体工作和结果"
              />
              <span className="block text-right text-xs text-muted-foreground">
                {reason.length} / 500
              </span>
            </label>

            <Button
              className="w-full"
              onClick={() => void submitAward()}
              disabled={submitting || !selectedMember}
            >
              {submitting ? <Loader2 className="animate-spin" /> : <Award />}
              确认记录
            </Button>
          </div>
        </section>
      </div>

      <section className="space-y-4">
        <div className="grid gap-3 border-y py-5 lg:grid-cols-[1fr_0.7fr_0.7fr_auto_auto]">
          <Input
            value={filters.keyword}
            onChange={(event) =>
              setFilters((current) => ({ ...current, keyword: event.target.value }))
            }
            onKeyDown={(event) => {
              if (event.key === "Enter") applyFilters()
            }}
            placeholder="姓名、账号或学号"
            maxLength={64}
          />
          <select
            value={filters.type}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                type: event.target.value as HistoryFilters["type"],
              }))
            }
            className="h-9 rounded-md border bg-background px-3 text-sm"
            aria-label="贡献类型"
          >
            <option value="">全部类型</option>
            {CONTRIBUTION_TYPES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            value={filters.source}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                source: event.target.value as HistoryFilters["source"],
              }))
            }
            className="h-9 rounded-md border bg-background px-3 text-sm"
            aria-label="记录来源"
          >
            <option value="">全部来源</option>
            <option value="MANUAL">人工补录</option>
            <option value="AUTO">系统自动</option>
            <option value="LEGACY">历史记录</option>
          </select>
          <Button onClick={applyFilters} disabled={loadingHistory}>
            <Search />
            查询
          </Button>
          <Button variant="outline" onClick={resetFilters} disabled={loadingHistory}>
            <RotateCcw />
            重置
          </Button>
        </div>

        {loadingHistory ? (
          Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))
        ) : items.length ? (
          <div className="divide-y rounded-lg border bg-card shadow-sm">
            {items.map((item) => (
              <article
                key={item.id}
                className="grid gap-4 p-5 md:grid-cols-[minmax(0,1fr)_8rem_8rem] md:items-center"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-semibold">
                      {item.realName || item.username || `成员 #${item.userId}`}
                    </h3>
                    <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs text-primary">
                      {item.typeLabel || item.type}
                    </span>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs ${sourceClassName(item.source)}`}
                    >
                      {item.sourceLabel || item.source}
                    </span>
                  </div>
                  <p className="mt-2 break-words text-sm text-muted-foreground">
                    {item.reason || "无说明"}
                  </p>
                  <p className="mt-2 text-xs text-muted-foreground">
                    {item.departmentName || "未分配部门"} · 操作人 {item.awardedByUsername || "历史未知"}
                  </p>
                </div>
                <div className="md:text-right">
                  <p className="text-xs text-muted-foreground">分值</p>
                  <p className="mt-1 text-xl font-semibold text-primary">
                    {formatScore(item.score)}
                  </p>
                </div>
                <div className="text-xs text-muted-foreground md:text-right">
                  {formatDateTime(item.createTime)}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-14 text-center text-sm text-muted-foreground">
            当前筛选条件下没有贡献记录。
          </div>
        )}

        <div className="flex items-center justify-between border-t pt-4">
          <Button
            variant="outline"
            disabled={page <= 1 || loadingHistory}
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
            disabled={page >= pages || loadingHistory}
            onClick={() => setPage((current) => current + 1)}
          >
            下一页
            <ChevronRight />
          </Button>
        </div>
      </section>
    </section>
  )
}
