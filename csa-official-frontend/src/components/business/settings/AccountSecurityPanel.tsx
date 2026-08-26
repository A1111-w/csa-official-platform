"use client"

import { useState } from "react"
import { Download, KeyRound, Loader2, LogOut, ShieldCheck, UserRoundX } from "lucide-react"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { accountService } from "@/services/account"
import { useAuthStore } from "@/store/useAuthStore"

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,64}$/

type PendingAction = "password" | "sessions" | "export" | "deactivate" | "delete" | null

function confirmAction(message: string) {
  return typeof window === "undefined" || window.confirm(message)
}

export function AccountSecurityPanel() {
  const router = useRouter()
  const logout = useAuthStore((state) => state.logout)
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [pending, setPending] = useState<PendingAction>(null)

  function finishSession() {
    logout()
    router.replace("/login")
  }

  async function handleChangePassword() {
    if (!currentPassword || !PASSWORD_PATTERN.test(newPassword)) {
      toast.error("新密码需包含字母和数字，长度 8-64 位")
      return
    }
    if (newPassword !== confirmPassword) {
      toast.error("两次输入的新密码不一致")
      return
    }

    setPending("password")
    try {
      await accountService.changePassword({ currentPassword, newPassword })
      toast.success("密码已修改，请重新登录")
      finishSession()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "密码修改失败")
    } finally {
      setPending(null)
    }
  }

  async function handleRevokeSessions() {
    if (!confirmAction("吊销全部会话后，所有设备都需要重新登录。继续吗？")) {
      return
    }

    setPending("sessions")
    try {
      await accountService.revokeSessions()
      toast.success("所有会话已吊销，请重新登录")
      finishSession()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "会话吊销失败")
    } finally {
      setPending(null)
    }
  }

  async function handleExport() {
    setPending("export")
    try {
      const data = await accountService.exportData()
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" })
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = `csa-personal-data-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      URL.revokeObjectURL(url)
      toast.success("个人数据导出已开始")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "个人数据导出失败")
    } finally {
      setPending(null)
    }
  }

  async function handleAccountAction(action: "deactivate" | "delete") {
    const message = action === "deactivate"
      ? "停用后账号将不能登录，但数据会按保留策略处理。继续吗？"
      : "提交删除申请后账号会立即退出；保留期结束后系统会按策略匿名化账号及关联个人字段。继续吗？"

    if (!confirmAction(message)) {
      return
    }

    setPending(action)
    try {
      if (action === "deactivate") {
        await accountService.deactivate()
      } else {
        await accountService.requestDeletion()
      }
      toast.success(action === "deactivate" ? "账号已停用" : "删除申请已提交")
      finishSession()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "账号操作失败")
    } finally {
      setPending(null)
    }
  }

  return (
    <section className="space-y-6">
      <div>
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <h2 className="text-2xl font-semibold">账户与隐私</h2>
        </div>
        <p className="mt-2 text-sm leading-7 text-muted-foreground">
          修改密码会立即吊销旧会话。导出内容只包含当前账号的资料、简历、文件元数据和安全事件摘要。
        </p>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <KeyRound className="h-4 w-4 text-primary" />
            <h3 className="font-semibold">修改密码</h3>
          </div>
          <div className="mt-5 space-y-4">
            <Input
              type="password"
              autoComplete="current-password"
              placeholder="当前密码"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
            <Input
              type="password"
              autoComplete="new-password"
              placeholder="新密码（8-64 位，含字母和数字）"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            <Input
              type="password"
              autoComplete="new-password"
              placeholder="确认新密码"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
            <Button onClick={handleChangePassword} disabled={pending !== null}>
              {pending === "password" ? <Loader2 className="animate-spin" /> : <KeyRound />}
              修改密码
            </Button>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <Download className="h-4 w-4 text-primary" />
            <h3 className="font-semibold">个人数据</h3>
          </div>
          <p className="mt-3 text-sm leading-7 text-muted-foreground">
            下载 JSON 副本，用于查看和迁移自己的账户资料。密码、Token 和验证码不会被导出。
          </p>
          <Button className="mt-5" variant="outline" onClick={handleExport} disabled={pending !== null}>
            {pending === "export" ? <Loader2 className="animate-spin" /> : <Download />}
            导出个人数据
          </Button>
        </div>
      </div>

      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2">
          <LogOut className="h-4 w-4 text-primary" />
          <h3 className="font-semibold">会话管理</h3>
        </div>
        <p className="mt-3 text-sm leading-7 text-muted-foreground">
          发现异常登录时，吊销全部会话可以让所有设备重新认证。
        </p>
        <Button className="mt-5" variant="outline" onClick={handleRevokeSessions} disabled={pending !== null}>
          {pending === "sessions" ? <Loader2 className="animate-spin" /> : <LogOut />}
          吊销全部会话
        </Button>
      </div>

      <div className="rounded-lg border border-destructive/30 bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2 text-destructive">
          <UserRoundX className="h-4 w-4" />
          <h3 className="font-semibold">停用或删除账号</h3>
        </div>
        <p className="mt-3 text-sm leading-7 text-muted-foreground">
          这两个操作都会立即退出当前账号。删除申请会进入数据保留期，期满后由后台任务匿名化账号及关联个人字段。
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          <Button variant="outline" onClick={() => handleAccountAction("deactivate")} disabled={pending !== null}>
            {pending === "deactivate" ? <Loader2 className="animate-spin" /> : null}
            停用账号
          </Button>
          <Button variant="destructive" onClick={() => handleAccountAction("delete")} disabled={pending !== null}>
            {pending === "delete" ? <Loader2 className="animate-spin" /> : <UserRoundX />}
            提交删除申请
          </Button>
        </div>
      </div>
    </section>
  )
}
