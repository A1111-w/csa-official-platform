"use client"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import { resolveAssetUrl } from "@/lib/format"
import type { ContributorVo } from "@/services/public"

interface ContributorWallProps {
  members: ContributorVo[]
  loading?: boolean
  limit?: number
}

export function ContributorWall({
  members,
  loading = false,
  limit,
}: ContributorWallProps) {
  const visibleMembers = limit ? members.slice(0, limit) : members

  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: limit ?? 4 }).map((_, index) => (
          <div key={index} className="rounded-lg border bg-card p-5">
            <div className="flex items-center gap-4">
              <Skeleton className="h-14 w-14 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-3 w-16" />
              </div>
            </div>
            <div className="mt-4 space-y-2">
              <Skeleton className="h-3 w-20" />
              <Skeleton className="h-3 w-28" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (!visibleMembers.length) {
    return (
      <div className="rounded-lg border border-dashed px-6 py-10 text-sm text-muted-foreground">
        暂时还没有公开的核心成员信息。
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {visibleMembers.map((member) => (
        <article
          key={member.id}
          className="rounded-lg border bg-card p-5 transition-colors hover:border-primary/30"
        >
          <div className="flex items-center gap-4">
            <Avatar className="h-14 w-14 border">
              <AvatarImage src={resolveAssetUrl(member.avatar)} alt={member.realName} />
              <AvatarFallback className="bg-primary/10 text-primary">
                {(member.realName || "U").slice(0, 1)}
              </AvatarFallback>
            </Avatar>
            <div className="min-w-0">
              <h3 className="truncate font-semibold text-foreground">
                {member.realName || "未公开姓名"}
              </h3>
              <p className="text-sm text-primary">{member.title || "核心成员"}</p>
            </div>
          </div>

          <div className="mt-4 space-y-1 text-sm text-muted-foreground">
            <p>{member.deptName || "协会协作组"}</p>
            <p>Level {member.roleLevel}</p>
          </div>
        </article>
      ))}
    </div>
  )
}
