"use client"

import { useEffect, useMemo, useState } from "react"
import { Save, ScrollText } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { sanitizeHtml } from "@/lib/sanitize-html"
import { publicService } from "@/services/public"
import { useAuthStore } from "@/store/useAuthStore"

export function AboutEditor() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [content, setContent] = useState("")

  const canEdit = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)

  const preview = useMemo(() => {
    if (!content.trim()) {
      return "<p>这里会实时预览协会介绍内容。</p>"
    }

    return sanitizeHtml(content)
  }, [content])

  useEffect(() => {
    if (!isClient || !canEdit) {
      return
    }

    let cancelled = false

    async function loadAbout() {
      setLoading(true)
      try {
        const response = await publicService.getAbout()
        if (!cancelled) {
          setContent(response)
        }
      } catch (error) {
        if (!cancelled) {
          toast.error(error instanceof Error ? error.message : "内容加载失败")
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadAbout()

    return () => {
      cancelled = true
    }
  }, [canEdit, isClient])

  async function handleSave() {
    setSaving(true)
    try {
      await publicService.updateAbout(content)
      toast.success("协会介绍已更新")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "保存失败")
    } finally {
      setSaving(false)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[520px] w-full rounded-lg" />
  }

  if (!canEdit) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <ScrollText className="h-5 w-5" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">协会介绍编辑器仅向会长开放</h2>
            <p className="text-sm text-muted-foreground">
              这里用于维护首页和关于页展示的公开介绍内容。
            </p>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">协会介绍内容管理</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            支持基础 HTML，保存后会同步更新首页和关于页的介绍区。危险标签和事件属性会被清洗。
          </p>
        </div>
        <Button onClick={handleSave} disabled={saving || loading}>
          <Save className="h-4 w-4" />
          保存修改
        </Button>
      </div>

      {loading ? (
        <Skeleton className="h-[460px] w-full rounded-lg" />
      ) : (
        <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-lg border bg-card p-5 shadow-sm">
            <p className="mb-3 text-sm font-medium text-foreground">编辑内容</p>
            <Textarea
              className="min-h-[420px] font-mono text-sm leading-6"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="<p>在这里编写协会介绍...</p>"
            />
          </div>

          <div className="rounded-lg border bg-card p-5 shadow-sm">
            <p className="mb-3 text-sm font-medium text-foreground">页面预览</p>
            <div
              className="space-y-4 text-sm leading-8 text-muted-foreground [&_a]:text-primary [&_a]:underline [&_h1]:text-2xl [&_h1]:font-semibold [&_h1]:text-foreground [&_h2]:text-xl [&_h2]:font-semibold [&_h2]:text-foreground [&_ul]:list-disc [&_ul]:pl-5"
              dangerouslySetInnerHTML={{ __html: preview }}
            />
          </div>
        </div>
      )}
    </section>
  )
}
