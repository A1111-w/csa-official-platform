import api from "@/lib/axios"
import type { PageResult } from "@/types/api"

export interface AuditLogItem {
  id: number
  actorUserId: number | null
  actorUsername: string | null
  action: string
  targetType: string | null
  targetId: string | null
  result: string
  ipAddress: string | null
  userAgent: string | null
  requestId: string | null
  detailsJson: string | null
  createTime: string | null
}

export interface AuditLogListParams {
  page?: number
  size?: number
  action?: string
  result?: string
  requestId?: string
}

export const auditService = {
  list: (params: AuditLogListParams = {}) =>
    api.get<PageResult<AuditLogItem>, PageResult<AuditLogItem>>(
      "/api/sys/audit/list",
      { params }
    ),
}
