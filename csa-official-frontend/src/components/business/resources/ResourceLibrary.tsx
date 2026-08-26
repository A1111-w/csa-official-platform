"use client"

import Link from "next/link"
import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react"
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Download,
  Edit3,
  FolderPlus,
  Link2,
  Loader2,
  Lock,
  Trash2,
  Upload,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime, resolveAssetUrl } from "@/lib/format"
import { fileService } from "@/services/file"
import { resourceService, type ResourceItem } from "@/services/resource"
import { useAuthStore } from "@/store/useAuthStore"

interface ResourceLibraryProps {
  variant?: "public" | "dashboard"
}

const pageSize = 8
const suggestedCategories = ["课程资料", "比赛题解", "项目模板", "工具软件"]

type ResourceDraft = {
  id: number | null
  title: string
  summary: string
  category: string
  fileUrl: string
}

function createEmptyDraft(): ResourceDraft {
  return {
    id: null,
    title: "",
    summary: "",
    category: "",
    fileUrl: "",
  }
}

export function ResourceLibrary({ variant = "public" }: ResourceLibraryProps) {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [mutatingId, setMutatingId] = useState<number | null>(null)
  const [items, setItems] = useState<ResourceItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [activeCategory, setActiveCategory] = useState("")
  const [categoryOptions, setCategoryOptions] = useState<string[]>([])
  const [draft, setDraft] = useState<ResourceDraft>(createEmptyDraft())

  const roleLevel = user?.roleLevel ?? null
  const canViewResources = Boolean(user) && hasRoleLevel(roleLevel, 1)
  const canManageResources = Boolean(user) && hasRoleLevel(roleLevel, 3)

  const categories = useMemo(
    () =>
      Array.from(
        new Set(
          [
            ...suggestedCategories,
            ...categoryOptions,
            activeCategory,
            draft.category || "",
            ...items.map((item) => item.category),
          ]
            .map((value) => value.trim())
            .filter(Boolean)
        )
      ),
    [activeCategory, categoryOptions, draft.category, items]
  )

  const loadResources = useCallback(
    async (nextPage = page, nextCategory = activeCategory) => {
      setLoading(true)
      try {
        let response = await resourceService.list({
          page: nextPage,
          size: pageSize,
          category: nextCategory || undefined,
        })
        const nextPages = Math.max(response.pages || 1, 1)

        if (nextPage > nextPages) {
          response = await resourceService.list({
            page: nextPages,
            size: pageSize,
            category: nextCategory || undefined,
          })
          setPage(nextPages)
        }

        setItems(response.records)
        setPages(Math.max(response.pages || 1, 1))
        setTotal(response.total || 0)
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "资源加载失败")
      } finally {
        setLoading(false)
      }
    },
    [activeCategory, page]
  )

  const loadCategories = useCallback(async () => {
    try {
      const response = await resourceService.listCategories()
      setCategoryOptions(response)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "资源分类加载失败")
    }
  }, [])

  useEffect(() => {
    if (!isClient || !canViewResources) {
      return
    }

    void loadResources(page, activeCategory)
  }, [activeCategory, canViewResources, isClient, loadResources, page])

  useEffect(() => {
    if (!isClient || !canViewResources) {
      return
    }

    void loadCategories()
  }, [canViewResources, isClient, loadCategories])

  function resetDraft() {
    setDraft(createEmptyDraft())
  }

  function handleEdit(item: ResourceItem) {
    setDraft({
      id: item.id,
      title: item.title,
      summary: item.summary || "",
      category: item.category || "",
      fileUrl: item.fileUrl || "",
    })
  }

  async function handleUploadFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""

    if (!file) {
      return
    }

    setUploading(true)
    try {
      const uploadedPath = await fileService.upload(file)
      setDraft((current) => ({ ...current, fileUrl: uploadedPath }))
      toast.success("文件已上传，可以继续保存资源")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "文件上传失败")
    } finally {
      setUploading(false)
    }
  }

  async function handleSave() {
    if (!draft.title.trim()) {
      toast.error("请先填写资源标题")
      return
    }

    if (!draft.fileUrl.trim()) {
      toast.error("请先上传文件或填写下载地址")
      return
    }

    setSaving(true)
    try {
      await resourceService.save({
        id: draft.id ?? undefined,
        title: draft.title.trim(),
        summary: draft.summary?.trim() || undefined,
        category: draft.category?.trim() || undefined,
        fileUrl: draft.fileUrl.trim(),
      })

      toast.success(draft.id ? "资源已更新" : "资源已发布")
      resetDraft()
      setPage(1)
      await Promise.all([loadResources(1, activeCategory), loadCategories()])
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "资源保存失败")
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(item: ResourceItem) {
    const confirmed = window.confirm(`确认删除资源“${item.title}”吗？`)
    if (!confirmed) {
      return
    }

    setMutatingId(item.id)
    try {
      await resourceService.remove(item.id)
      toast.success("资源已删除")

      if (draft.id === item.id) {
        resetDraft()
      }

      await Promise.all([loadResources(), loadCategories()])
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "资源删除失败")
    } finally {
      setMutatingId(null)
    }
  }

  async function handleDownload(item: ResourceItem) {
    if (!item.fileUrl) {
      toast.error("当前资源还没有可用的下载地址")
      return
    }

    window.open(resolveAssetUrl(item.fileUrl), "_blank", "noopener,noreferrer")

    try {
      await resourceService.trackDownload(item.id)
      toast.success(`已打开 ${item.title}`)
      setItems((current) =>
        current.map((resource) =>
          resource.id === item.id
            ? {
                ...resource,
                downloadCount: (resource.downloadCount ?? 0) + 1,
              }
            : resource
        )
      )
    } catch (error) {
      toast.message(error instanceof Error ? error.message : "资源已打开")
    }
  }

  if (!isClient) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, index) => (
          <Skeleton key={index} className="h-28 w-full rounded-lg" />
        ))}
      </div>
    )
  }

  if (!user) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <Lock className="h-5 w-5" />
          </div>
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-semibold">资源库需要会员身份</h2>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                登录后可以查看协会沉淀的课程资料、项目参考、常用工具与比赛素材。
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button asChild>
                <Link href="/login">登录后查看</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href="/register">注册账号</Link>
              </Button>
            </div>
          </div>
        </div>
      </section>
    )
  }

  if (!hasRoleLevel(roleLevel, 1)) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-amber-500/10 p-3 text-amber-600">
            <BookOpen className="h-5 w-5" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">当前账号还是游客身份</h2>
            <p className="text-sm leading-7 text-muted-foreground">
              资源库面向会员开放。完成邀请码升级或通过协会审核后，这里会自动解锁。
            </p>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">
            {variant === "dashboard" ? "资源库工作台" : "协会资源库"}
          </h2>
          <p className="mt-2 text-sm leading-7 text-muted-foreground">
            {variant === "dashboard"
              ? "按分类查阅资料，部长以上可在同页直接维护资源。"
              : "会员可查看课程资料、工具包、项目模板与协会沉淀。"}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          <span>共 {total} 条资源</span>
          {canManageResources ? (
            <span className="rounded-full bg-primary/10 px-3 py-1 text-primary">
              资源管理权限已开启
            </span>
          ) : null}
        </div>
      </div>

      {variant === "dashboard" && canManageResources ? (
        <div className="grid gap-4 xl:grid-cols-[0.8fr_1.2fr]">
          <div className="rounded-lg border bg-card p-6 shadow-sm">
            <div className="flex items-center gap-2">
              <FolderPlus className="h-4 w-4 text-primary" />
              <h3 className="font-semibold">资源管理</h3>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-3">
              {[
                ["可见资源", String(total || 0)],
                ["当前分类", activeCategory || "全部"],
                ["工作模式", draft.id ? "编辑中" : "新建中"],
              ].map(([label, value]) => (
                <div key={label} className="rounded-lg border p-4">
                  <p className="text-xs text-muted-foreground">{label}</p>
                  <p className="mt-2 text-lg font-semibold">{value}</p>
                </div>
              ))}
            </div>
            <p className="mt-5 text-sm leading-7 text-muted-foreground">
              上传附件后会自动回填下载地址，也可以直接粘贴已有资源链接。
              列表里的资源支持随时回填到右侧继续修改。
            </p>
          </div>

          <div className="rounded-lg border bg-card p-6 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="font-semibold">{draft.id ? "编辑资源" : "发布资源"}</h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  标题、分类和下载地址会直接同步到资源列表。
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
                placeholder="资源标题"
                value={draft.title}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, title: event.target.value }))
                }
              />
              <div className="space-y-2">
                <Input
                  list="resource-category-options"
                  placeholder="资源分类"
                  value={draft.category || ""}
                  onChange={(event) =>
                    setDraft((current) => ({ ...current, category: event.target.value }))
                  }
                />
                <datalist id="resource-category-options">
                  {categories.map((category) => (
                    <option key={category} value={category} />
                  ))}
                </datalist>
              </div>
            </div>

            <Textarea
              className="mt-4 min-h-28"
              placeholder="摘要描述，告诉成员这份资源适合用在什么场景"
              value={draft.summary || ""}
              onChange={(event) =>
                setDraft((current) => ({ ...current, summary: event.target.value }))
              }
            />

            <div className="mt-4 grid gap-4 md:grid-cols-[1fr_auto]">
              <Input
                placeholder="下载地址或上传后的文件路径"
                value={draft.fileUrl}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, fileUrl: event.target.value }))
                }
              />
              <label className="flex cursor-pointer items-center gap-2 rounded-md border border-dashed px-4 text-sm text-muted-foreground">
                {uploading ? (
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                ) : (
                  <Upload className="h-4 w-4 text-primary" />
                )}
                <span>{uploading ? "上传中..." : "上传附件"}</span>
                <input
                  type="file"
                  className="w-[1px] opacity-0"
                  onChange={handleUploadFile}
                  disabled={uploading}
                />
              </label>
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              <span>支持手填外链，也支持上传到后端文件服务。</span>
              {draft.fileUrl ? (
                <button
                  type="button"
                  className="inline-flex items-center gap-1 text-primary"
                  onClick={() =>
                    window.open(resolveAssetUrl(draft.fileUrl), "_blank", "noopener,noreferrer")
                  }
                >
                  <Link2 className="h-4 w-4" />
                  预览当前地址
                </button>
              ) : null}
            </div>

            <div className="mt-5 flex flex-wrap justify-end gap-3">
              <Button variant="outline" onClick={resetDraft} disabled={saving || uploading}>
                清空表单
              </Button>
              <Button onClick={handleSave} disabled={saving || uploading}>
                {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {draft.id ? "保存修改" : "发布资源"}
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {categories.length ? (
        <div className="flex flex-wrap gap-2">
          <Button
            variant={activeCategory ? "outline" : "default"}
            size="sm"
            onClick={() => {
              setActiveCategory("")
              setPage(1)
            }}
          >
            全部
          </Button>
          {categories.map((category) => (
            <Button
              key={category}
              variant={activeCategory === category ? "default" : "outline"}
              size="sm"
              onClick={() => {
                setActiveCategory(category)
                setPage(1)
              }}
            >
              {category}
            </Button>
          ))}
        </div>
      ) : null}

      <div className="space-y-3">
        {loading ? (
          Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))
        ) : items.length ? (
          items.map((item) => (
            <article key={item.id} className="rounded-lg border bg-card p-5 shadow-sm">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0 space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-semibold">{item.title}</h3>
                    {item.category ? (
                      <span className="rounded-full bg-secondary px-2.5 py-1 text-xs text-secondary-foreground">
                        {item.category}
                      </span>
                    ) : null}
                  </div>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {item.summary || "暂无摘要描述。"}
                  </p>
                  <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                    <span>上传时间 {formatDateTime(item.createTime)}</span>
                    <span>下载 {item.downloadCount ?? 0} 次</span>
                    <span>上传者 #{item.uploaderId ?? "--"}</span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <Button onClick={() => handleDownload(item)}>
                    <Download className="h-4 w-4" />
                    下载资源
                  </Button>
                  {canManageResources ? (
                    <>
                      <Button variant="outline" onClick={() => handleEdit(item)}>
                        <Edit3 className="h-4 w-4" />
                        编辑
                      </Button>
                      <Button
                        variant="destructive"
                        disabled={mutatingId === item.id}
                        onClick={() => handleDelete(item)}
                      >
                        {mutatingId === item.id ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                        删除
                      </Button>
                    </>
                  ) : null}
                </div>
              </div>
            </article>
          ))
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-12 text-sm text-muted-foreground">
            当前筛选条件下还没有资源。
          </div>
        )}
      </div>

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

      {variant === "dashboard" && !canManageResources ? (
        <div className="rounded-lg border bg-card px-4 py-3 text-sm text-muted-foreground">
          当前账号可浏览与下载资源；部长及以上会在本页自动解锁资源管理区。
        </div>
      ) : null}
    </section>
  )
}
