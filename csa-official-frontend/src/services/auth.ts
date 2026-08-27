import api, { clearCsrfToken, rememberCsrfToken } from "@/lib/axios"

import type { AuthUser } from "@/types/user"

export interface LoginParams {
  username: string
  password: string
}

interface LoginResponseRaw {
  csrfToken?: string
  username: string
  roleLevel: string
}

export type LoginResponse = AuthUser

export interface RegisterParams {
  username: string
  password: string
  email: string
  code: string
  realName?: string
  studentId?: string
  college?: string
  className?: string
  inviteCode?: string
  merchantNo?: string
  privacyConsentVersion: string
}

export interface ForgotPasswordParams {
  email: string
}

export interface ResetPasswordParams {
  email: string
  code: string
  newPassword: string
}

export const authService = {
  login: async (data: LoginParams) => {
    const response = await api.post<LoginResponseRaw, LoginResponseRaw>("/api/auth/login", data)
    rememberCsrfToken(response.csrfToken)
    return {
      username: response.username,
      roleLevel: Number(response.roleLevel),
    } satisfies LoginResponse
  },

  sendCode: (email: string) => {
    return api.post<string, string>("/api/auth/send-code", null, { params: { email } })
  },

  register: (data: RegisterParams) => {
    return api.post<string, string>("/api/auth/register", data)
  },

  forgotPassword: (data: ForgotPasswordParams) => {
    return api.post<string, string>("/api/auth/forgot-password", data, { skipAuthRedirect: true })
  },

  resetPassword: (data: ResetPasswordParams) => {
    return api.post<string, string>("/api/auth/reset-password", data, { skipAuthRedirect: true })
  },

  logout: async () => {
    try {
      return await api.post<string, string>("/api/auth/logout")
    } finally {
      clearCsrfToken()
    }
  },
}
