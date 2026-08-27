"use client"

import { useEffect, useMemo, useState } from "react"
import { CheckCircle2, CircleSlash2, Scale, Send } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime, parseVoteResult } from "@/lib/format"
import { voteService, type ProposalItem } from "@/services/vote"
import { useAuthStore } from "@/store/useAuthStore"

const proposalTypes = [
  { value: "CODE_DEPLOY", label: "代码发布" },
  { value: "ACTIVITY", label: "活动决策" },
  { value: "RULE_UPDATE", label: "规则调整" },
]

function getStatusMeta(status: number) {
  if (status === 1) {
    return { label: "已通过", className: "bg-emerald-500/10 text-emerald-600" }
  }
  if (status === 2) {
    return { label: "已否决", className: "bg-rose-500/10 text-rose-600" }
  }
  return { label: "投票中", className: "bg-primary/10 text-primary" }
}

export function ProposalCenter() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [submittingId, setSubmittingId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [proposals, setProposals] = useState<ProposalItem[]>([])
  const [comments, setComments] = useState<Record<number, string>>({})
  const [draft, setDraft] = useState({
    type: proposalTypes[0].value,
    title: "",
    reason: "",
  })

  const canUseVoteCenter = Boolean(user) && hasRoleLevel(user?.roleLevel, 3)

  const sortedProposals = useMemo(
    () =>
      [...proposals].sort((left, right) =>
        String(right.createTime || "").localeCompare(String(left.createTime || ""))
      ),
    [proposals]
  )

  async function loadProposals() {
    setLoading(true)
    try {
      const response = await voteService.list()
      setProposals(response)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "提案加载失败")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isClient || !canUseVoteCenter) {
      return
    }

    void loadProposals()
  }, [canUseVoteCenter, isClient])

  async function handleCreate() {
    if (!draft.title.trim() || !draft.reason.trim()) {
      toast.error("请先补全提案标题和理由")
      return
    }

    setCreating(true)
    try {
      await voteService.create({
        type: draft.type,
        title: draft.title.trim(),
        reason: draft.reason.trim(),
      })
      toast.success("提案已创建")
      setDraft({
        type: proposalTypes[0].value,
        title: "",
        reason: "",
      })
      await loadProposals()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "提案创建失败")
    } finally {
      setCreating(false)
    }
  }

  async function handleVote(proposalId: number, agree: boolean) {
    setSubmittingId(proposalId)
    try {
      await voteService.submit({
        proposalId,
        agree,
        comment: comments[proposalId]?.trim() || undefined,
      })
      toast.success(agree ? "已提交赞成票" : "已提交反对票")
      await loadProposals()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "投票提交失败")
    } finally {
      setSubmittingId(null)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[480px] w-full rounded-lg" />
  }

  if (!canUseVoteCenter) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <Scale className="h-5 w-5" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">提案中心仅向部长及以上开放</h2>
            <p className="text-sm leading-7 text-muted-foreground">
              这里用于组织层决策与内部投票。当前账号暂无访问权限。
            </p>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2">
          <Send className="h-4 w-4 text-primary" />
          <h2 className="text-xl font-semibold">发起新提案</h2>
        </div>

        <div className="mt-5 space-y-4">
          <div className="flex flex-wrap gap-2">
            {proposalTypes.map((type) => (
              <Button
                key={type.value}
                variant={draft.type === type.value ? "default" : "outline"}
                size="sm"
                onClick={() => setDraft((current) => ({ ...current, type: type.value }))}
              >
                {type.label}
              </Button>
            ))}
          </div>

          <Input
            placeholder="提案标题"
            value={draft.title}
            onChange={(event) =>
              setDraft((current) => ({ ...current, title: event.target.value }))
            }
          />

          <Textarea
            className="min-h-32"
            placeholder="说明提案背景、风险和预期结果"
            value={draft.reason}
            onChange={(event) =>
              setDraft((current) => ({ ...current, reason: event.target.value }))
            }
          />

          <div className="flex justify-end">
            <Button onClick={handleCreate} disabled={creating}>
              发起提案
            </Button>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        {loading ? (
          Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-48 w-full rounded-lg" />
          ))
        ) : sortedProposals.length ? (
          sortedProposals.map((proposal) => {
            const statusMeta = getStatusMeta(proposal.status)
            const result = parseVoteResult(proposal.finalResultJson)
            const isOpen = proposal.status === 0

            return (
              <article key={proposal.id} className="rounded-lg border bg-card p-6 shadow-sm">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`rounded-full px-2.5 py-1 text-xs ${statusMeta.className}`}>
                        {statusMeta.label}
                      </span>
                      <span className="rounded-full bg-secondary px-2.5 py-1 text-xs text-secondary-foreground">
                        {proposal.type}
                      </span>
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold">{proposal.title}</h3>
                      <p className="mt-2 text-sm leading-6 text-muted-foreground">
                        {proposal.reason}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                      <span>创建时间 {formatDateTime(proposal.createTime)}</span>
                      <span>截止时间 {formatDateTime(proposal.expireTime)}</span>
                      {result ? (
                        <span>
                          当前票数 {result.agree} / {result.reject}
                        </span>
                      ) : null}
                    </div>
                  </div>

                  <div className="w-full max-w-sm space-y-3">
                    <Textarea
                      className="min-h-24"
                      placeholder="给这次投票留一句说明"
                      value={comments[proposal.id] || ""}
                      onChange={(event) =>
                        setComments((current) => ({
                          ...current,
                          [proposal.id]: event.target.value,
                        }))
                      }
                      disabled={!isOpen}
                    />
                    <div className="grid grid-cols-2 gap-3">
                      <Button
                        variant="outline"
                        disabled={!isOpen || submittingId === proposal.id}
                        onClick={() => handleVote(proposal.id, false)}
                      >
                        <CircleSlash2 className="h-4 w-4" />
                        反对
                      </Button>
                      <Button
                        disabled={!isOpen || submittingId === proposal.id}
                        onClick={() => handleVote(proposal.id, true)}
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        赞成
                      </Button>
                    </div>
                  </div>
                </div>
              </article>
            )
          })
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-12 text-sm text-muted-foreground">
            目前还没有提案。
          </div>
        )}
      </div>
    </section>
  )
}
