import api from "@/lib/axios"

export interface DeptItem {
  id: number
  name: string
  intro: string | null
  leaderId: number | null
  createTime: string | null
  updateTime: string | null
}

export const deptService = {
  list: () => api.get<DeptItem[], DeptItem[]>("/api/sys/dept/list"),

  appoint: (deptId: number, userId: number) =>
    api.post<string, string>("/api/sys/dept/appoint", {
      deptId,
      userId,
    }),
}
