"use client"

import Image from "next/image"
import { type ChangeEvent, useCallback, useEffect, useMemo, useState } from "react"
import {
  ExternalLink,
  ImagePlus,
  Images,
  Loader2,
  Pencil,
  Power,
  RefreshCw,
  RotateCcw,
  Save,
  Trash2,
  Upload,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { formatDateTime, resolveAssetUrl } from "@/lib/format"
import {
  carouselService,
  type CarouselAdminItem,
  type SaveCarouselPayload,
} from "@/services/carousel"
import { fileService } from "@/services/file"
import { useAuthStore } from "@/store/useAuthStore"

interface CarouselDraft {
  id: number | null
  title: string
  imgUrl: string
  targetUrl: string
  sortOrder: string
  status: number
}

const EMPTY_DRAFT: CarouselDraft = {
  id: null,
  title: "",
  imgUrl: "",
  targetUrl: "",
  sortOrder: "0",
  status: 1,
}

function draftFromItem(item: CarouselAdminItem): CarouselDraft {
  return {
    id: item.id,
    title: item.title,
    imgUrl: item.imgUrl,
    targetUrl: item.targetUrl || "",
    sortOrder: String(item.sortOrder ?? 0),
    status: item.status === 1 ? 1 : 0,
  }
}

function targetLabel(value: string | null) {
  return value?.trim() || "不设置跳转"
}

export function CarouselManagementWorkspace() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [mutatingId, setMutatingId] = useState<number | null>(null)
  const [items, setItems] = useState<CarouselAdminItem[]>([])
  const [draft, setDraft] = useState<CarouselDraft>(EMPTY_DRAFT)

  const canManage = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)
  const enabledCount = useMemo(
    () => items.filter((item) => item.status === 1).length,
    [items]
  )

  const loadItems = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await carouselService.list())
    } catch (error) {
      setItems([])
      toast.error(error instanceof Error ? error.message : "轮播图列表加载失败")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!isClient || !canManage) {
      setLoading(false)
      return
    }

    void loadItems()
  }, [canManage, isClient, loadItems])

  function resetDraft() {
    setDraft(EMPTY_DRAFT)
  }

  function editItem(item: CarouselAdminItem) {
    setDraft(draftFromItem(item))
    document.getElementById("carousel-editor")?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    })
  }

  async function uploadImage(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""

    if (!file) return
    if (!file.type.startsWith("image/")) {
      toast.error("请选择 JPG、PNG 或 GIF 图片")
      return
    }

    setUploading(true)
    try {
      const uploadedPath = await fileService.upload(file)
      setDraft((current) => ({ ...current, imgUrl: uploadedPath }))
      toast.success("轮播图片已上传")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "轮播图片上传失败")
    } finally {
      setUploading(false)
    }
  }

  function buildPayload(source: CarouselDraft): SaveCarouselPayload | null {
    const title = source.title.trim()
    const imgUrl = source.imgUrl.trim()
    const targetUrl = source.targetUrl.trim()
    const sortOrder = Number(source.sortOrder)

    if (!title) {
      toast.error("请填写轮播标题")
      return null
    }
    if (!imgUrl) {
      toast.error("请上传图片或填写图片地址")
      return null
    }
    if (!Number.isInteger(sortOrder) || sortOrder < -100000 || sortOrder > 100000) {
      toast.error("排序值必须是 -100000 到 100000 之间的整数")
      return null
    }

    return {
      id: source.id ?? undefined,
      title,
      imgUrl,
      targetUrl: targetUrl || null,
      sortOrder,
      status: source.status === 1 ? 1 : 0,
    }
  }

  async function saveDraft() {
    const payload = buildPayload(draft)
    if (!payload) return

    setSaving(true)
    try {
      await carouselService.save(payload)
      toast.success(draft.id ? "轮播图已更新" : "轮播图已创建")
      resetDraft()
      await loadItems()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "轮播图保存失败")
    } finally {
      setSaving(false)
    }
  }

  async function toggleItem(item: CarouselAdminItem) {
    const payload = buildPayload({
      ...draftFromItem(item),
      status: item.status === 1 ? 0 : 1,
    })
    if (!payload) return

    setMutatingId(item.id)
    try {
      await carouselService.save(payload)
      toast.success(item.status === 1 ? "轮播图已停用" : "轮播图已启用")
      if (draft.id === item.id) {
        setDraft((current) => ({ ...current, status: payload.status }))
      }
      await loadItems()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "轮播图状态更新失败")
    } finally {
      setMutatingId(null)
    }
  }

  async function deleteItem(item: CarouselAdminItem) {
    if (!window.confirm(`确认删除轮播图“${item.title}”吗？`)) return

    setMutatingId(item.id)
    try {
      await carouselService.remove(item.id)
      toast.success("轮播图已删除")
      if (draft.id === item.id) resetDraft()
      await loadItems()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "轮播图删除失败")
    } finally {
      setMutatingId(null)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[760px] w-full rounded-lg" />
  }

  if (!canManage) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">无权管理首页轮播图</h1>
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
            <Images className="h-5 w-5" />
            <span className="text-sm font-medium">公开内容</span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">首页轮播管理</h1>
          <p className="mt-2 max-w-3xl text-sm leading-7 text-muted-foreground">
            维护首页公开轮播的图片、跳转地址、显示顺序和启用状态。保存后会立即清理公开缓存。
          </p>
        </div>
        <Button variant="outline" onClick={() => void loadItems()} disabled={loading}>
          <RefreshCw className={loading ? "animate-spin" : ""} />
          刷新列表
        </Button>
      </header>

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          ["轮播总数", String(items.length)],
          ["当前启用", String(enabledCount)],
          ["当前模式", draft.id ? "编辑中" : "新建中"],
        ].map(([label, value]) => (
          <div key={label} className="border-y py-4">
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-2 text-2xl font-semibold">{value}</p>
          </div>
        ))}
      </div>

      <div id="carousel-editor" className="grid scroll-mt-24 gap-6 xl:grid-cols-[1fr_0.9fr]">
        <div className="space-y-5 border-y py-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-xl font-semibold">{draft.id ? "编辑轮播图" : "新增轮播图"}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                排序值越小越靠前；停用后不会出现在公开首页。
              </p>
            </div>
            {draft.id ? (
              <Button variant="outline" size="sm" onClick={resetDraft}>
                取消编辑
              </Button>
            ) : null}
          </div>

          <div className="grid gap-4 md:grid-cols-[1fr_160px]">
            <label className="space-y-2 text-sm">
              <span className="font-medium">标题</span>
              <Input
                value={draft.title}
                maxLength={128}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, title: event.target.value }))
                }
                placeholder="例如：2026 校内算法赛报名开启"
              />
            </label>
            <label className="space-y-2 text-sm">
              <span className="font-medium">排序值</span>
              <Input
                type="number"
                min={-100000}
                max={100000}
                step={1}
                value={draft.sortOrder}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, sortOrder: event.target.value }))
                }
              />
            </label>
          </div>

          <label className="space-y-2 text-sm">
            <span className="font-medium">跳转地址</span>
            <Input
              value={draft.targetUrl}
              maxLength={500}
              onChange={(event) =>
                setDraft((current) => ({ ...current, targetUrl: event.target.value }))
              }
              placeholder="站内路径 /competitions，或 https:// 开头的外链"
            />
          </label>

          <div className="space-y-2 text-sm">
            <span className="font-medium">轮播图片</span>
            <div className="grid gap-3 md:grid-cols-[1fr_auto]">
              <Input
                value={draft.imgUrl}
                maxLength={500}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, imgUrl: event.target.value }))
                }
                placeholder="上传图片，或填写 https:// 图片地址"
              />
              <label className="flex h-9 cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed px-4 text-sm text-muted-foreground">
                {uploading ? <Loader2 className="animate-spin" /> : <Upload />}
                <span>{uploading ? "上传中" : "上传图片"}</span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/gif,.jpg,.jpeg,.png,.gif"
                  className="sr-only"
                  onChange={uploadImage}
                  disabled={uploading}
                />
              </label>
            </div>
          </div>

          <label className="flex min-h-11 cursor-pointer items-center gap-3 rounded-md border px-4 py-3 text-sm">
            <input
              type="checkbox"
              checked={draft.status === 1}
              onChange={(event) =>
                setDraft((current) => ({ ...current, status: event.target.checked ? 1 : 0 }))
              }
              className="h-4 w-4 accent-primary"
            />
            <span>
              <span className="font-medium">在首页启用</span>
              <span className="ml-2 text-muted-foreground">关闭后仍保留记录，可稍后重新启用</span>
            </span>
          </label>

          <div className="flex flex-wrap justify-end gap-3">
            <Button variant="outline" onClick={resetDraft} disabled={saving || uploading}>
              <RotateCcw />
              清空表单
            </Button>
            <Button onClick={() => void saveDraft()} disabled={saving || uploading}>
              {saving ? <Loader2 className="animate-spin" /> : <Save />}
              {draft.id ? "保存修改" : "创建轮播"}
            </Button>
          </div>
        </div>

        <div className="space-y-3 border-y py-6">
          <div>
            <h2 className="text-xl font-semibold">当前预览</h2>
            <p className="mt-2 text-sm text-muted-foreground">上传后的本地图片在保存前仅当前账号可见。</p>
          </div>
          <div className="relative aspect-[3/1] min-h-56 overflow-hidden rounded-lg border bg-muted">
            {draft.imgUrl ? (
              <Image
                src={resolveAssetUrl(draft.imgUrl)}
                alt={draft.title || "轮播图预览"}
                fill
                unoptimized
                sizes="(max-width: 1280px) 100vw, 45vw"
                className="object-cover"
              />
            ) : (
              <div className="flex h-full min-h-56 flex-col items-center justify-center gap-3 text-muted-foreground">
                <ImagePlus className="h-8 w-8" />
                <span className="text-sm">上传或填写图片地址后显示预览</span>
              </div>
            )}
            {draft.imgUrl ? <div className="absolute inset-0 bg-black/45" /> : null}
            {draft.imgUrl ? (
              <div className="absolute inset-x-0 bottom-0 p-5 text-white">
                <p className="text-xl font-semibold">{draft.title || "未填写标题"}</p>
                <p className="mt-2 truncate text-sm text-white/70">
                  {targetLabel(draft.targetUrl)}
                </p>
              </div>
            ) : null}
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold">已有轮播图</h2>
            <p className="mt-2 text-sm text-muted-foreground">列表按排序值升序展示。</p>
          </div>
        </div>

        {loading ? (
          <div className="grid gap-4 lg:grid-cols-2">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-64 w-full rounded-lg" />
            ))}
          </div>
        ) : items.length ? (
          <div className="grid gap-4 lg:grid-cols-2">
            {items.map((item) => {
              const busy = mutatingId === item.id
              const externalTarget = item.targetUrl?.startsWith("http")

              return (
                <article key={item.id} className="overflow-hidden rounded-lg border bg-card shadow-sm">
                  <div className="relative aspect-[3/1] min-h-44 bg-muted">
                    <Image
                      src={resolveAssetUrl(item.imgUrl)}
                      alt={item.title}
                      fill
                      unoptimized
                      sizes="(max-width: 1024px) 100vw, 50vw"
                      className={`object-cover ${item.status === 1 ? "" : "grayscale"}`}
                    />
                    <div className="absolute inset-0 bg-black/35" />
                    <div className="absolute inset-x-0 bottom-0 p-4 text-white">
                      <div className="flex items-end justify-between gap-3">
                        <h3 className="text-lg font-semibold">{item.title}</h3>
                        <span className="shrink-0 rounded-full bg-black/45 px-2.5 py-1 text-xs">
                          {item.status === 1 ? "已启用" : "已停用"}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4 p-5">
                    <dl className="grid gap-3 text-sm sm:grid-cols-2">
                      <div>
                        <dt className="text-muted-foreground">排序值</dt>
                        <dd className="mt-1 font-medium">{item.sortOrder ?? 0}</dd>
                      </div>
                      <div>
                        <dt className="text-muted-foreground">最近更新</dt>
                        <dd className="mt-1 font-medium">{formatDateTime(item.updateTime)}</dd>
                      </div>
                      <div className="sm:col-span-2">
                        <dt className="text-muted-foreground">跳转地址</dt>
                        <dd className="mt-1 flex min-w-0 items-center gap-2">
                          <span className="truncate font-mono text-xs">{targetLabel(item.targetUrl)}</span>
                          {item.targetUrl ? (
                            <a
                              href={item.targetUrl}
                              target={externalTarget ? "_blank" : undefined}
                              rel={externalTarget ? "noopener noreferrer" : undefined}
                              className="shrink-0 text-primary"
                              aria-label={`打开 ${item.title} 的跳转地址`}
                              title="打开跳转地址"
                            >
                              <ExternalLink className="h-4 w-4" />
                            </a>
                          ) : null}
                        </dd>
                      </div>
                    </dl>

                    <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
                      <Button variant="outline" size="sm" onClick={() => editItem(item)} disabled={busy}>
                        <Pencil />
                        编辑
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => void toggleItem(item)}
                        disabled={busy}
                      >
                        {busy ? <Loader2 className="animate-spin" /> : <Power />}
                        {item.status === 1 ? "停用" : "启用"}
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => void deleteItem(item)}
                        disabled={busy}
                      >
                        <Trash2 />
                        删除
                      </Button>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed px-6 py-16 text-center text-sm text-muted-foreground">
            还没有轮播图，请先在上方创建第一条记录。
          </div>
        )}
      </div>
    </section>
  )
}
