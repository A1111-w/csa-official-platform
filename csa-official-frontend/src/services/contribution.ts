import api from "@/lib/axios"
import type { PageResult } from "@/types/api"

export type ContributionType = "DEV" | "RES" | "COMP" | "OPS"
export type ContributionSource = "AUTO" | "MANUAL" | "LEGACY"

export interface ContributionAwardPayload {
  userId: number
  type: ContributionType
  score: number
  reason: string
}

export interface ContributionAwardItem {
  id: number
  userId: number
  username: string | null
  realName: string | null
  departmentName: string | null
  type: ContributionType | string
  typeLabel: string
  score: number | string
  reason: string | null
  source: ContributionSource | string
  sourceLabel: string
  awardedBy: number | null
  awardedByUsername: string | null
  createTime: string | null
}

export interface ContributionAwardListParams {
  page?: number
  size?: number
  keyword?: string
  type?: ContributionType
  source?: ContributionSource
}

export const contributionService = {
  award: (payload: ContributionAwardPayload) =>
    api.post<string, string>("/api/sys/contribution/award", payload),

  listAwards: (params: ContributionAwardListParams = {}) =>
    api.get<PageResult<ContributionAwardItem>, PageResult<ContributionAwardItem>>(
      "/api/sys/contribution/awards",
      { params }
    ),
}
