import api from "@/lib/axios"

export interface CarouselAdminItem {
  id: number
  imgUrl: string
  targetUrl: string | null
  title: string
  sortOrder: number
  status: number
  createTime: string | null
  updateTime: string | null
}

export interface SaveCarouselPayload {
  id?: number
  imgUrl: string
  targetUrl?: string | null
  title: string
  sortOrder: number
  status: number
}

export const carouselService = {
  list: () =>
    api.get<CarouselAdminItem[], CarouselAdminItem[]>("/api/sys/carousel/list"),

  save: (data: SaveCarouselPayload) =>
    api.post<string, string>("/api/sys/carousel/save", data),

  remove: (id: number) =>
    api.post<string, string>("/api/sys/carousel/delete", null, {
      params: { id },
    }),
}
