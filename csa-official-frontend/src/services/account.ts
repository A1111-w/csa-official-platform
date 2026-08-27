import api from "@/lib/axios"

export interface PersonalDataExport {
  exportVersion: string
  generatedAt: string
  account: Record<string, unknown>
  resume: Record<string, unknown> | null
  uploadedFiles: Array<Record<string, unknown>>
  securityEvents: Array<Record<string, unknown>>
}

export interface ChangePasswordParams {
  currentPassword: string
  newPassword: string
}

export const accountService = {
  changePassword: (data: ChangePasswordParams) => {
    return api.post<string, string>("/api/account/change-password", data)
  },

  revokeSessions: () => {
    return api.post<string, string>("/api/account/revoke-sessions")
  },

  deactivate: () => {
    return api.post<string, string>("/api/account/deactivate")
  },

  requestDeletion: () => {
    return api.post<string, string>("/api/account/deletion-request")
  },

  exportData: () => {
    return api.get<PersonalDataExport, PersonalDataExport>("/api/account/export")
  },
}
