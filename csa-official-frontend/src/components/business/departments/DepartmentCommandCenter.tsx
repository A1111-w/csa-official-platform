"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Building2, Loader2, Search, ShieldCheck, UserRoundCog } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleLabel, hasRoleLevel } from "@/lib/access"
import { deptService, type DeptItem } from "@/services/dept"
import { userService, type UserDirectoryItem } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"

function getDisplayName(user?: UserDirectoryItem | null) {
  if (!user) {
    return "暂未任命"
  }

  return user.realName || user.username
}

function getAppointmentRestriction(
  member: UserDirectoryItem,
  selectedDeptId: number | null
) {
  if ((member.roleLevel ?? 0) >= 4) {
    return "会长或 Root 账号不参与部长任命"
  }

  if (
    selectedDeptId != null &&
    member.roleLevel === 3 &&
    member.departmentId != null &&
    member.departmentId !== selectedDeptId
  ) {
    return "已担任其他部门部长，需要先完成卸任"
  }

  return null
}

export function DepartmentCommandCenter() {
  const isClient = useIsClient()
  const { user } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [appointingId, setAppointingId] = useState<number | null>(null)
  const [departments, setDepartments] = useState<DeptItem[]>([])
  const [members, setMembers] = useState<UserDirectoryItem[]>([])
  const [keyword, setKeyword] = useState("")
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null)

  const canManageDepartments = Boolean(user) && hasRoleLevel(user?.roleLevel, 4)

  const leaderMap = useMemo(
    () => new Map(members.map((member) => [member.id, member])),
    [members]
  )

  const selectedDept = useMemo(
    () => departments.find((dept) => dept.id === selectedDeptId) ?? null,
    [departments, selectedDeptId]
  )

  const filteredMembers = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase()
    const sortedMembers = [...members].sort((left, right) => {
      const roleCompare = (right.roleLevel ?? -1) - (left.roleLevel ?? -1)
      if (roleCompare !== 0) {
        return roleCompare
      }

      return String(left.username).localeCompare(String(right.username))
    })

    if (!normalizedKeyword) {
      return sortedMembers
    }

    return sortedMembers.filter((member) =>
      [member.realName, member.username, member.email, member.departmentName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalizedKeyword))
    )
  }, [keyword, members])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [deptList, memberList] = await Promise.all([
        deptService.list(),
        userService.list({ size: 150 }),
      ])

      setDepartments(deptList)
      setMembers(memberList)
      setSelectedDeptId((current) => current ?? deptList[0]?.id ?? null)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "部门数据加载失败")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!isClient || !canManageDepartments) {
      return
    }

    void loadData()
  }, [canManageDepartments, isClient, loadData])

  async function handleAppoint(member: UserDirectoryItem) {
    if (!selectedDept) {
      toast.error("请先选择一个部门")
      return
    }

    const confirmed = window.confirm(
      `确认任命 ${getDisplayName(member)} 为 ${selectedDept.name} 部长吗？`
    )

    if (!confirmed) {
      return
    }

    setAppointingId(member.id)
    try {
      await deptService.appoint(selectedDept.id, member.id)
      toast.success("人事任命已生效")
      await loadData()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "任命失败")
    } finally {
      setAppointingId(null)
    }
  }

  if (!isClient) {
    return <Skeleton className="h-[560px] w-full rounded-lg" />
  }

  if (!canManageDepartments) {
    return (
      <section className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <UserRoundCog className="h-5 w-5" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">部门人事页仅向会长及以上开放</h2>
            <p className="text-sm leading-7 text-muted-foreground">
              这里用于查看当前部门负责人并执行部长任命。当前账号暂无访问权限。
            </p>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-2xl font-semibold">部门与人事工作台</h2>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-muted-foreground">
              在同一页查看部门概况、当前负责人和候选成员。任命新部长时，
              后端会处理旧部长降级与部门归属变更。
            </p>
          </div>
          <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
            <span className="rounded-full bg-primary/10 px-3 py-1 text-primary">
              {departments.length} 个部门
            </span>
            <span className="rounded-full bg-secondary px-3 py-1 text-secondary-foreground">
              {members.length} 名成员
            </span>
          </div>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
        <div className="space-y-4">
          {loading ? (
            Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-32 w-full rounded-lg" />
            ))
          ) : departments.length ? (
            departments.map((dept) => {
              const leader = dept.leaderId ? leaderMap.get(dept.leaderId) : null
              const selected = selectedDeptId === dept.id

              return (
                <button
                  key={dept.id}
                  type="button"
                  className={`w-full rounded-lg border p-5 text-left transition-colors ${
                    selected
                      ? "border-primary bg-primary/5"
                      : "border-border bg-card hover:border-primary/30"
                  }`}
                  onClick={() => setSelectedDeptId(dept.id)}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="flex items-center gap-2">
                        <Building2 className="h-4 w-4 text-primary" />
                        <h3 className="font-semibold">{dept.name}</h3>
                      </div>
                      <p className="mt-3 text-sm leading-6 text-muted-foreground">
                        {dept.intro || "暂无部门简介。"}
                      </p>
                    </div>
                    <span className="rounded-full bg-secondary px-2.5 py-1 text-xs text-secondary-foreground">
                      #{dept.id}
                    </span>
                  </div>

                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <div className="rounded-lg border p-3">
                      <p className="text-xs text-muted-foreground">当前负责人</p>
                      <p className="mt-2 font-medium">{getDisplayName(leader)}</p>
                    </div>
                    <div className="rounded-lg border p-3">
                      <p className="text-xs text-muted-foreground">负责人等级</p>
                      <p className="mt-2 font-medium">
                        {leader ? getRoleLabel(leader.roleLevel ?? 0) : "--"}
                      </p>
                    </div>
                  </div>
                </button>
              )
            })
          ) : (
            <div className="rounded-lg border border-dashed px-6 py-12 text-sm text-muted-foreground">
              当前还没有可用的部门数据。
            </div>
          )}
        </div>

        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <h3 className="font-semibold">
                  {selectedDept ? `${selectedDept.name} 部长候选池` : "候选成员"}
                </h3>
              </div>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                搜索成员后直接任命。若对方已在其他部门担任部长，接口会阻止重复兼任。
              </p>
            </div>
          </div>

          <div className="mt-5 rounded-lg border p-4">
            <p className="text-xs text-muted-foreground">当前目标部门</p>
            <p className="mt-2 text-lg font-semibold">
              {selectedDept?.name || "先在左侧选择一个部门"}
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              当前负责人：
              {selectedDept?.leaderId
                ? ` ${getDisplayName(leaderMap.get(selectedDept.leaderId))}`
                : " 暂未任命"}
            </p>
          </div>

          <div className="mt-4 flex items-center gap-2 rounded-lg border px-3">
            <Search className="h-4 w-4 text-muted-foreground" />
            <Input
              className="border-0 shadow-none focus-visible:ring-0"
              placeholder="搜索姓名、账号、邮箱或部门"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>

          <div className="mt-5 max-h-[560px] space-y-3 overflow-auto pr-1">
            {loading ? (
              Array.from({ length: 5 }).map((_, index) => (
                <Skeleton key={index} className="h-24 w-full rounded-lg" />
              ))
            ) : filteredMembers.length ? (
              filteredMembers.map((member) => {
                const isCurrentLeader = selectedDept?.leaderId === member.id
                const restriction = getAppointmentRestriction(member, selectedDept?.id ?? null)

                return (
                  <article key={member.id} className="rounded-lg border p-4">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h4 className="font-semibold">
                            {member.realName || member.username}
                          </h4>
                          <span className="rounded-full bg-secondary px-2.5 py-1 text-xs text-secondary-foreground">
                            {getRoleLabel(member.roleLevel ?? 0)}
                          </span>
                          {isCurrentLeader ? (
                            <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs text-primary">
                              当前负责人
                            </span>
                          ) : null}
                        </div>

                        <div className="mt-2 flex flex-wrap gap-4 text-sm text-muted-foreground">
                          <span>@{member.username}</span>
                          <span>{member.departmentName || "未分配部门"}</span>
                          <span>{member.email || "未公开邮箱"}</span>
                        </div>

                        {restriction ? (
                          <p className="mt-2 text-sm text-amber-600">{restriction}</p>
                        ) : null}
                      </div>

                      <Button
                        disabled={
                          !selectedDept ||
                          isCurrentLeader ||
                          appointingId === member.id ||
                          Boolean(restriction)
                        }
                        onClick={() => handleAppoint(member)}
                      >
                        {appointingId === member.id ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : null}
                        任命为部长
                      </Button>
                    </div>
                  </article>
                )
              })
            ) : (
              <div className="rounded-lg border border-dashed px-6 py-12 text-sm text-muted-foreground">
                没找到符合条件的成员。
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
