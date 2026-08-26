"use client"

import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react"
import Image from "next/image"
import Link from "next/link"
import {
  CalendarRange,
  ChevronLeft,
  ChevronRight,
  Edit3,
  Flag,
  ImageIcon,
  Loader2,
  ShieldPlus,
  Upload,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleLabel, hasRoleLevel } from "@/lib/access"
import { excerptText, formatDateTime, resolveAssetUrl } from "@/lib/format"
import { competitionService, type CompetitionItem } from "@/services/competition"
import { fileService } from "@/services/file"
import { userService, type UserDirectoryItem } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"
import { ApiError } from "@/types/api"

interface CompetitionBoardProps {
  variant?: "public" | "dashboard"
}

const pageSize = 6
const competitionStatuses = [
  { value: 0, label: "未发布" },
  { value: 1, label: "进行中" },
  { value: 2, label: "已结束" },
]

type CompetitionDraft = {
  id: number | null
  title: string
  content: string
  coverImg: string
  startTime: string
  endTime: string
  status: number
}

function createEmptyDraft(): CompetitionDraft {
  return {
    id: null,
    title: "",
    content: "",
    coverImg: "",
    startTime: "",
    endTime: "",
    status: 1,
  }
}

function normalizeCompetitionStatus(status: CompetitionItem["status"]) {
  if (status == null) {
    return "待更新"
  }

  if (status === 0 || status === "0" || String(status).toUpperCase().includes("UNPUBLISHED")) {
    return "未发布"
  }

  if (status === 1 || status === "1" || String(status).toUpperCase().includes("ONGOING")) {
    return "进行中"
  }

  if (status === 2 || status === "2" || String(status).toUpperCase().includes("FINISHED")) {
    return "已结束"
  }

  return String(status)
}

function toDateTimeInputValue(value?: string | null) {
  if (!value) {
    return ""
  }

  const normalized = value.includes("T") ? value : value.replace(" ", "T")
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return normalized.slice(0, 16)
  }

  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16)
}

export function CompetitionBoard({ variant = "public" }: CompetitionBoardProps) {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [granting, setGranting] = useState(false)
  const [membersLoading, setMembersLoading] = useState(false)
  const [loadingDetailId, setLoadingDetailId] = useState<number | null>(null)
  const [items, setItems] = useState<CompetitionItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [draft, setDraft] = useState<CompetitionDraft>(createEmptyDraft())
  const [members, setMembers] = useState<UserDirectoryItem[]>([])
  const [memberKeyword, setMemberKeyword] = useState("")
  const [grantTarget, setGrantTarget] = useState<CompetitionItem | null>(null)
  const [selectedEditorId, setSelectedEditorId] = useState<number | null>(null)
  const [publicListRequiresAuth, setPublicListRequiresAuth] = useState(false)

  const canManageCompetitions = Boolean(user) && hasRoleLevel(user?.roleLevel, 3)

  const filteredMembers = useMemo(() => {
    const keyword = memberKeyword.trim().toLowerCase()
    const list = members.filter((member) => (member.roleLevel ?? 0) >= 1)

    if (!keyword) {
      return list
    }

    return list.filter((member) =>
      [member.realName, member.username, member.email, member.departmentName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    )
  }, [memberKeyword, members])

  const loadCompetitions = useCallback(async (nextPage = page) => {
    setLoading(true)
    try {
      setPublicListRequiresAuth(false)
      const listCompetitions =
        variant === "public" ? competitionService.listPublic : competitionService.list
      let response = await listCompetitions({ page: nextPage, size: pageSize })
      const nextPages = Math.max(response.pages || 1, 1)

      if (nextPage > nextPages) {
        response = await listCompetitions({ page: nextPages, size: pageSize })
        setPage(nextPages)
      }

      setItems(response.records)
      setPages(Math.max(response.pages || 1, 1))
    } catch (error) {
      if (
        variant === "public" &&
        error instanceof ApiError &&
        (error.status === 401 || error.code === 401)
      ) {
        setItems([])
        setPages(1)
        setPublicListRequiresAuth(true)
        return
      }

      toast.error(error instanceof Error ? error.message : "比赛列表加载失败")
    } finally {
      setLoading(false)
    }
  }, [page, variant])

  const loadMembers = useCallback(async () => {
    if (!canManageCompetitions) {
      return
    }

    setMembersLoading(true)
    try {
      const response = await userService.list({
        minRoleLevel: 1,
        size: 120,
      })
      setMembers(response)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "成员目录加载失败")
    } finally {
      setMembersLoading(false)
    }
  }, [canManageCompetitions])

  useEffect(() => {
    void loadCompetitions(page)
  }, [loadCompetitions, page])

  useEffect(() => {
    if (!isClient || !canManageCompetitions || variant !== "dashboard") {
      return
    }

    void loadMembers()
  }, [canManageCompetitions, isClient, loadMembers, variant])

  function resetDraft() {
    setDraft(createEmptyDraft())
  }

  // 列表接口只返回摘要，所以编辑前要按 id 把完整正文拉回来，
  // 否则会用摘要覆盖掉原文，一保存就把正文截断了。
  async function handleEdit(item: CompetitionItem) {
    setLoadingDetailId(item.id)
    try {
      const detail = await competitionService.detail(item.id)
      setDraft({
        id: detail.id,
        title: detail.title,
        content: detail.content || "",
        coverImg: detail.coverImg || "",
        startTime: toDateTimeInputValue(detail.startTime),
        endTime: toDateTimeInputValue(detail.endTime),
        status:
          typeof detail.status === "number"
            ? detail.status
            : Number.parseInt(String(detail.status ?? 1), 10) || 1,
      })
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "加载比赛详情失败")
    } finally {
      setLoadingDetailId(null)
    }
  }

  async function handleUploadCover(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""

    if (!file) {
      return
    }

    setUploading(true)
    try {
      const uploadedPath = await fileService.upload(file)
      setDraft((current) => ({ ...current, coverImg: uploadedPath }))
      toast.success("封面已上传")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "封面上传失败")
    } finally {
      setUploading(false)
    }
  }

  async function handleSave() {
    if (!draft.title.trim()) {
      toast.error("请先填写比赛标题")
      return
    }

    if (!draft.content.trim()) {
      toast.error("请先填写比赛详情")
      return
    }

    if (draft.startTime && draft.endTime && new Date(draft.endTime) < new Date(draft.startTime)) {
      toast.error("结束时间不能早于开始时间")
      return
    }

    setSaving(true)
    try {
      await competitionService.save({
        id: draft.id ?? undefined,
        title: draft.title.trim(),
        content: draft.content.trim(),
        coverImg: draft.coverImg?.trim() || null,
        startTime: draft.startTime || null,
        endTime: draft.endTime || null,
        status: draft.status ?? 1,
      })

      toast.success(draft.id ? "比赛已更新" : "比赛已发布")
      resetDraft()
      setPage(1)
      await loadCompetitions(1)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "比赛保存失败")
    } finally {
      setSaving(false)
    }
  }

  async function handleGrantEditor() {
    if (!grantTarget?.id || !selectedEditorId) {
      toast.error("请先选择比赛和要授权的成员")
      return
    }

    setGranting(true)
    try {
      await competitionService.grantEditor({
        compId: grantTarget.id,
        targetUserId: selectedEditorId,
      })
      toast.success("协作者权限已发出")
      setSelectedEditorId(null)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "授权失败")
    } finally {
      setGranting(false)
    }
  }

  if (!isClient && variant === "dashboard") {
    return <Skeleton className="h-[520px] w-full rounded-lg" />
  }

  return (
    <section className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">
            {variant === "dashboard" ? "比赛管理视图" : "近期比赛与活动"}
          </h2>
          <p className="mt-2 text-sm leading-7 text-muted-foreground">
            {variant === "dashboard"
              ? "部长以上可发布比赛、更新内容，并为负责人分配协作编辑权限。"
              : "公开展示协会相关比赛、训练营和项目型活动。"}
          </p>
        </div>
      </div>

      {variant === "dashboard" && canManageCompetitions ? (
        <div className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
          <div className="rounded-lg border bg-card p-6 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="font-semibold">{draft.id ? "编辑比赛" : "发布比赛"}</h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  支持上传封面、维护时间窗口，并同步设置当前状态。
                </p>
              </div>
              {draft.id ? (
                <Button variant="outline" onClick={resetDraft}>
                  取消编辑
                </Button>
              ) : null}
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <Input
                placeholder="比赛标题"
                value={draft.title}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, title: event.target.value }))
                }
              />
              <div className="flex flex-wrap gap-2">
                {competitionStatuses.map((status) => (
                  <Button
                    key={status.value}
                    variant={draft.status === status.value ? "default" : "outline"}
                    size="sm"
                    onClick={() =>
                      setDraft((current) => ({ ...current, status: status.value }))
                    }
                  >
                    {status.label}
                  </Button>
                ))}
              </div>
            </div>

            <Textarea
              className="mt-4 min-h-40"
              placeholder="介绍比赛背景、报名方式、准备要求和时间安排"
              value={draft.content}
              onChange={(event) =>
                setDraft((current) => ({ ...current, content: event.target.value }))
              }
            />

            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <Input
                type="datetime-local"
                value={draft.startTime || ""}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, startTime: event.target.value }))
                }
              />
              <Input
                type="datetime-local"
                value={draft.endTime || ""}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, endTime: event.target.value }))
                }
              />
            </div>

            <div className="mt-4 grid gap-4 md:grid-cols-[1fr_auto]">
              <Input
                placeholder="封面地址，可手填也可上传"
                value={draft.coverImg || ""}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, coverImg: event.target.value }))
                }
              />
              <label className="flex cursor-pointer items-center gap-2 rounded-md border border-dashed px-4 text-sm text-muted-foreground">
                {uploading ? (
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                ) : (
                  <Upload className="h-4 w-4 text-primary" />
                )}
                <span>{uploading ? "上传中..." : "上传封面"}</span>
                <input
                  type="file"
                  className="w-[1px] opacity-0"
                  onChange={handleUploadCover}
                  disabled={uploading}
                />
              </label>
            </div>

            {draft.coverImg ? (
              <div className="relative mt-4 aspect-[16/7] overflow-hidden rounded-lg border">
                <Image
                  src={resolveAssetUrl(draft.coverImg)}
                  alt="比赛封面预览"
                  fill
                  unoptimized
                  sizes="(max-width: 1024px) 100vw, 50vw"
                  className="object-cover"
                />
              </div>
            ) : null}

            <div className="mt-5 flex justify-end gap-3">
              <Button variant="outline" onClick={resetDraft} disabled={saving || uploading}>
                清空表单
              </Button>
              <Button onClick={handleSave} disabled={saving || uploading}>
                {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {draft.id ? "保存修改" : "发布比赛"}
              </Button>
            </div>
          </div>

          <div className="rounded-lg border bg-card p-6 shadow-sm">
            <div className="flex items-center gap-2">
              <ShieldPlus className="h-4 w-4 text-primary" />
              <h3 className="font-semibold">协作者授权</h3>
            </div>
            <p className="mt-2 text-sm leading-7 text-muted-foreground">
              选中一条你拥有授权权的比赛后，可以给成员开放协作编辑权限。
            </p>

            <div className="mt-5 rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">当前比赛</p>
              <p className="mt-2 font-semibold">
                {grantTarget?.title || "先在下方比赛卡片里选择“授权协作”"}
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                {grantTarget
                  ? `${normalizeCompetitionStatus(grantTarget.status)} · 发布者 #${grantTarget.publisherId ?? "--"}`
                  : "只有发布者本人、会长或 Root 会看到授权按钮。"}
              </p>
            </div>

            <Input
              className="mt-4"
              placeholder="搜索成员姓名、账号、邮箱或部门"
              value={memberKeyword}
              onChange={(event) => setMemberKeyword(event.target.value)}
            />

            <div className="mt-4 max-h-[340px] space-y-3 overflow-auto pr-1">
              {membersLoading ? (
                Array.from({ length: 4 }).map((_, index) => (
                  <Skeleton key={index} className="h-20 w-full rounded-lg" />
                ))
              ) : filteredMembers.length ? (
                filteredMembers.map((member) => {
                  const selected = selectedEditorId === member.id

                  return (
                    <button
                      key={member.id}
                      type="button"
                      className={`w-full rounded-lg border p-4 text-left transition-colors ${
                        selected
                          ? "border-primary bg-primary/5"
                          : "border-border hover:border-primary/30"
                      }`}
                      onClick={() => setSelectedEditorId(member.id)}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="font-semibold">
                            {member.realName || member.username}
                          </p>
                          <p className="mt-1 text-sm text-muted-foreground">
                            @{member.username}
                            {member.departmentName ? ` · ${member.departmentName}` : ""}
                          </p>
                        </div>
                        <span className="rounded-full bg-secondary px-2.5 py-1 text-xs text-secondary-foreground">
                          {getRoleLabel(member.roleLevel ?? 0)}
                        </span>
                      </div>
                    </button>
                  )
                })
              ) : (
                <div className="rounded-lg border border-dashed px-4 py-10 text-sm text-muted-foreground">
                  没找到符合条件的成员。
                </div>
              )}
            </div>

            <div className="mt-5 flex justify-end">
              <Button
                onClick={handleGrantEditor}
                disabled={!grantTarget?.id || !selectedEditorId || granting}
              >
                {granting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                确认授权
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {publicListRequiresAuth ? (
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h3 className="text-lg font-semibold">比赛列表需要登录后查看</h3>
              <p className="mt-2 max-w-2xl text-sm leading-7 text-muted-foreground">
                当前线上旧后端会保护比赛列表接口。你仍然可以先登录进入平台，后续新后端上线后这里会直接展示公开比赛活动。
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button asChild>
                <Link href="/login?redirect=%2Fcompetitions">登录后查看</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href="/register">注册账号</Link>
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {!publicListRequiresAuth ? (
        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
        {loading ? (
          Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-[320px] w-full rounded-lg" />
          ))
        ) : items.length ? (
          items.map((item) => (
            <article key={item.id} className="overflow-hidden rounded-lg border bg-card shadow-sm">
              <div className="relative aspect-[16/9] border-b bg-secondary/60">
                {item.coverImg ? (
                  <Image
                    src={resolveAssetUrl(item.coverImg)}
                    alt={item.title}
                    fill
                    unoptimized
                    sizes="(max-width: 1024px) 100vw, (max-width: 1280px) 50vw, 33vw"
                    className="object-cover"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center gap-3 text-muted-foreground">
                    <ImageIcon className="h-5 w-5" />
                    <span className="text-sm">暂无封面</span>
                  </div>
                )}
              </div>

              <div className="space-y-4 p-5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs text-primary">
                    {normalizeCompetitionStatus(item.status)}
                  </span>
                  {item.startTime ? (
                    <span className="text-xs text-muted-foreground">
                      {formatDateTime(item.startTime)}
                    </span>
                  ) : null}
                </div>

                <div>
                  <h3 className="text-lg font-semibold">{item.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    {excerptText(item.summary, 120) || "暂无活动详情。"}
                  </p>
                </div>

                <div className="space-y-2 text-sm text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <CalendarRange className="h-4 w-4" />
                    <span>
                      {item.startTime ? formatDateTime(item.startTime) : "待定"} 至{" "}
                      {item.endTime ? formatDateTime(item.endTime) : "待定"}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Flag className="h-4 w-4" />
                    <span>最近更新 {formatDateTime(item.updateTime || item.createTime)}</span>
                  </div>
                </div>

                {variant === "dashboard" && canManageCompetitions ? (
                  <div className="flex flex-wrap gap-3 border-t pt-4">
                    {item.canEdit ? (
                      <Button
                        variant="outline"
                        disabled={loadingDetailId === item.id}
                        onClick={() => void handleEdit(item)}
                      >
                        <Edit3 className="h-4 w-4" />
                        {loadingDetailId === item.id ? "加载中..." : "编辑"}
                      </Button>
                    ) : null}
                    {item.canGrant ? (
                      <Button
                        variant="outline"
                        onClick={() => {
                          setGrantTarget(item)
                          setSelectedEditorId(null)
                        }}
                      >
                        <ShieldPlus className="h-4 w-4" />
                        授权协作
                      </Button>
                    ) : null}
                  </div>
                ) : null}
              </div>
            </article>
          ))
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-12 text-sm text-muted-foreground lg:col-span-2 xl:col-span-3">
            目前还没有公开的比赛数据。
          </div>
        )}
        </div>
      ) : null}

      {!publicListRequiresAuth ? (
        <div className="flex items-center justify-between">
        <Button
          variant="outline"
          disabled={page <= 1 || loading}
          onClick={() => setPage((current) => Math.max(current - 1, 1))}
        >
          <ChevronLeft className="h-4 w-4" />
          上一页
        </Button>
        <div className="text-sm text-muted-foreground">
          第 {page} / {pages} 页
        </div>
        <Button
          variant="outline"
          disabled={page >= pages || loading}
          onClick={() => setPage((current) => current + 1)}
        >
          下一页
          <ChevronRight className="h-4 w-4" />
        </Button>
        </div>
      ) : null}
    </section>
  )
}
