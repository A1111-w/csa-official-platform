import api from "@/lib/axios"
import type { PageResult } from "@/types/api"

/**
 * 列表项。注意这里只有 `summary`（后端截断好的纯文本摘要），没有完整正文。
 * 列表页只渲染摘要，正文要走 `detail` 接口单独取，避免列表响应里塞满富文本 HTML。
 */
export interface CompetitionItem {
  id: number
  title: string
  summary: string | null
  coverImg: string | null
  startTime: string | null
  endTime: string | null
  publisherId: number | null
  status: string | number | null
  createTime: string | null
  updateTime: string | null
  canEdit?: boolean
  canGrant?: boolean
}

/** 详情项，比列表项多一个完整的 `content` 富文本正文。 */
export interface CompetitionDetailItem extends Omit<CompetitionItem, "summary"> {
  content: string
}

export interface CompetitionListParams {
  page?: number
  size?: number
}

export interface SaveCompetitionPayload {
  id?: number
  title: string
  content: string
  coverImg?: string | null
  startTime?: string | null
  endTime?: string | null
  status?: number | null
}

export interface GrantCompetitionEditorPayload {
  compId: number
  targetUserId: number
}

export const competitionService = {
  list: (params: CompetitionListParams = {}) =>
    api.get<PageResult<CompetitionItem>, PageResult<CompetitionItem>>("/api/biz/comp/list", {
      params,
    }),

  listPublic: (params: CompetitionListParams = {}) =>
    api.get<PageResult<CompetitionItem>, PageResult<CompetitionItem>>(
      "/api/public/competitions",
      {
        params,
        skipAuthRedirect: true,
      }
    ),

  detail: (id: number) =>
    api.get<CompetitionDetailItem, CompetitionDetailItem>(`/api/biz/comp/${id}`),

  detailPublic: (id: number) =>
    api.get<CompetitionDetailItem, CompetitionDetailItem>(`/api/public/competitions/${id}`, {
      skipAuthRedirect: true,
    }),

  save: (data: SaveCompetitionPayload) =>
    api.post<string, string>("/api/biz/comp/save", data),

  grantEditor: (data: GrantCompetitionEditorPayload) =>
    api.post<string, string>("/api/biz/comp/grant", data),
}
