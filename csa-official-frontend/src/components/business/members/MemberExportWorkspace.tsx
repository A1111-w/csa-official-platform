"use client"

import { useState } from "react"
import { Download, FileSpreadsheet, Loader2, RotateCcw } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleLabel, hasRoleLevel } from "@/lib/access"
import { userService, type MemberExportPayload } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"

const EXPORT_COLUMNS = [
  { key: "realName", label: "姓名", defaultSelected: true },
  { key: "studentId", label: "学号", defaultSelected: true },
  { key: "college", label: "学院", defaultSelected: true },
  { key: "className", label: "班级", defaultSelected: true },
  { key: "phone", label: "手机号", defaultSelected: true },
  { key: "roleLevel", label: "角色等级", defaultSelected: true },
  { key: "username", label: "系统账号", defaultSelected: false },
  { key: "contact", label: "其他联系方式", defaultSelected: false },
  { key: "usedInviteCode", label: "已用邀请码", defaultSelected: false },
  { key: "createTime", label: "注册时间", defaultSelected: false },
  { key: "merchantNo", label: "支付单号", defaultSelected: false },
] as const

interface ExportFilters {
  startDate: string
  endDate: string
  college: string
  className: string
  realName: string
  studentId: string
  roleLevel: string
  inviteCode: string
}

const EMPTY_FILTERS: ExportFilters = {
  startDate: "",
  endDate: "",
  college: "",
  className: "",
  realName: "",
  studentId: "",
  roleLevel: "",
  inviteCode: "",
}

const DEFAULT_COLUMNS = EXPORT_COLUMNS
  .filter((column) => column.defaultSelected)
  .map((column) => column.key)

function trimOrUndefined(value: string) {
  const normalized = value.trim()
  return normalized || undefined
}

export function MemberExportWorkspace() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [exporting, setExporting] = useState(false)
  const [filters, setFilters] = useState<ExportFilters>(EMPTY_FILTERS)
  const [columns, setColumns] = useState<string[]>(DEFAULT_COLUMNS)

  const canExport = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)

  function toggleColumn(key: string) {
    setColumns((current) =>
      current.includes(key)
        ? current.filter((column) => column !== key)
        : [...current, key]
    )
  }

  function resetForm() {
    setFilters(EMPTY_FILTERS)
    setColumns(DEFAULT_COLUMNS)
  }

  async function handleExport() {
    if (!columns.length) {
      toast.error("请至少选择一个导出列")
      return
    }
    if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
      toast.error("开始日期不能晚于结束日期")
      return
    }

    const payload: MemberExportPayload = {
      columns,
      startTime: filters.startDate ? `${filters.startDate} 00:00:00` : undefined,
      endTime: filters.endDate ? `${filters.endDate} 23:59:59` : undefined,
      college: trimOrUndefined(filters.college),
      className: trimOrUndefined(filters.className),
      realName: trimOrUndefined(filters.realName),
      studentId: trimOrUndefined(filters.studentId),
      roleLevel: filters.roleLevel ? Number(filters.roleLevel) : undefined,
      inviteCode: trimOrUndefined(filters.inviteCode),
    }

    setExporting(true)
    try {
      const file = await userService.exportMembers(payload)
      const downloadUrl = URL.createObjectURL(file)
      const link = document.createElement("a")
      link.href = downloadUrl
      link.download = `CSA-members-${new Date().toISOString().slice(0, 10)}.xlsx`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(downloadUrl)
      toast.success("成员名单已导出")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "成员名单导出失败")
    } finally {
      setExporting(false)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[620px] w-full rounded-lg" />
  }

  if (!canExport) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">无权导出成员数据</h1>
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
            <FileSpreadsheet className="h-5 w-5" />
            <span className="text-sm font-medium">成员数据</span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">成员名单导出</h1>
          <p className="mt-2 max-w-3xl text-sm leading-7 text-muted-foreground">
            导出操作会写入管理审计日志。密码、Token 和验证码不在可选列中。
          </p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline" onClick={resetForm} disabled={exporting}>
            <RotateCcw />
            重置
          </Button>
          <Button onClick={() => void handleExport()} disabled={exporting || !columns.length}>
            {exporting ? <Loader2 className="animate-spin" /> : <Download />}
            导出 Excel
          </Button>
        </div>
      </header>

      <fieldset className="space-y-4 border-y py-6">
        <legend className="px-2 text-sm font-semibold">导出列</legend>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {EXPORT_COLUMNS.map((column) => (
            <label
              key={column.key}
              className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg border bg-card px-4 py-3 text-sm"
            >
              <input
                type="checkbox"
                checked={columns.includes(column.key)}
                onChange={() => toggleColumn(column.key)}
                className="h-4 w-4 accent-primary"
              />
              <span>{column.label}</span>
            </label>
          ))}
        </div>
        <p className="text-xs text-muted-foreground">已选择 {columns.length} 列</p>
      </fieldset>

      <div>
        <h2 className="text-lg font-semibold">筛选条件</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <label className="space-y-2 text-sm">
            <span className="font-medium">开始日期</span>
            <Input
              type="date"
              value={filters.startDate}
              onChange={(event) =>
                setFilters((current) => ({ ...current, startDate: event.target.value }))
              }
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">结束日期</span>
            <Input
              type="date"
              value={filters.endDate}
              onChange={(event) =>
                setFilters((current) => ({ ...current, endDate: event.target.value }))
              }
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">角色等级</span>
            <select
              value={filters.roleLevel}
              onChange={(event) =>
                setFilters((current) => ({ ...current, roleLevel: event.target.value }))
              }
              className="h-9 w-full rounded-md border bg-background px-3 text-sm"
            >
              <option value="">全部成员</option>
              {[1, 2, 3, 4, 5].map((level) => (
                <option key={level} value={level}>
                  {getRoleLabel(level)} (Level {level})
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">学院</span>
            <Input
              value={filters.college}
              onChange={(event) =>
                setFilters((current) => ({ ...current, college: event.target.value }))
              }
              placeholder="精确匹配学院"
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">班级</span>
            <Input
              value={filters.className}
              onChange={(event) =>
                setFilters((current) => ({ ...current, className: event.target.value }))
              }
              placeholder="精确匹配班级"
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">姓名</span>
            <Input
              value={filters.realName}
              onChange={(event) =>
                setFilters((current) => ({ ...current, realName: event.target.value }))
              }
              placeholder="支持模糊查询"
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">学号</span>
            <Input
              value={filters.studentId}
              onChange={(event) =>
                setFilters((current) => ({ ...current, studentId: event.target.value }))
              }
              placeholder="精确匹配学号"
            />
          </label>
          <label className="space-y-2 text-sm">
            <span className="font-medium">邀请码</span>
            <Input
              value={filters.inviteCode}
              onChange={(event) =>
                setFilters((current) => ({ ...current, inviteCode: event.target.value }))
              }
              placeholder="精确匹配已用邀请码"
            />
          </label>
        </div>
      </div>
    </section>
  )
}
