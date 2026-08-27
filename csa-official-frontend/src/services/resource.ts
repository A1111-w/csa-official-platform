import api from "@/lib/axios"
import type { PageResult } from "@/types/api"

export interface ResourceItem {
  id: number
  title: string
  summary: string
  fileUrl: string
  category: string
  uploaderId: number
  downloadCount: number
  createTime: string
}

export interface ResourceListParams {
  page?: number
  size?: number
  category?: string
}

export interface SaveResourcePayload {
  id?: number
  title: string
  summary?: string
  fileUrl: string
  category?: string
}

export const resourceService = {
  list: (params: ResourceListParams = {}) =>
    api.get<PageResult<ResourceItem>, PageResult<ResourceItem>>("/api/sys/resource/list", {
      params,
    }),

  listCategories: () =>
    api.get<string[], string[]>("/api/sys/resource/categories"),

  save: (data: SaveResourcePayload) =>
    api.post<string, string>("/api/sys/resource/save", data),

  remove: (id: number) =>
    api.post<string, string>("/api/sys/resource/delete", null, {
      params: { id },
    }),

  trackDownload: (id: number) =>
    api.post<string, string>("/api/sys/resource/download", null, {
      params: { id },
    }),
}
