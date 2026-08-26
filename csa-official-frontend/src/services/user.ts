import api from "@/lib/axios"
import type { AxiosRequestConfig } from "axios"

import type { UserInfo } from "@/types/user"

export interface UserDirectoryItem {
  id: number
  username: string
  realName: string | null
  avatar: string | null
  email: string | null
  roleLevel: number | null
  positionType: number | null
  departmentId: number | null
  departmentName: string | null
}

export interface UserListParams {
  keyword?: string
  departmentId?: number
  minRoleLevel?: number
  size?: number
}

export const userService = {
  getInfo: (config?: AxiosRequestConfig) =>
    api.get<UserInfo, UserInfo>("/api/sys/user/info", config),

  list: (params: UserListParams = {}) =>
    api.get<UserDirectoryItem[], UserDirectoryItem[]>("/api/sys/user/list", {
      params,
    }),
}
