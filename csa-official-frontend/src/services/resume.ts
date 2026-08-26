import api from "@/lib/axios"
import type { PageResult } from "@/types/api"

export interface ResumeData {
  id?: number
  content: string
  gitRepoUrl: string
  gitSyncStatus?: ResumeGitSyncStatus
  gitSyncStartedAt?: string | null
  gitSyncCompletedAt?: string | null
  gitSyncErrorCode?: string | null
  gitSyncBranch?: string | null
  gitSyncCommit?: string | null
  gitSyncSizeBytes?: number | null
  status: number
  rejectReason?: string
  auditBy?: number
  auditTime?: string
  updateTime?: string
}

export type ResumeGitSyncStatus =
  | "NOT_SYNCED"
  | "SYNCING"
  | "SUCCEEDED"
  | "FAILED"

export interface ResumeGitSyncData {
  configured: boolean
  status: ResumeGitSyncStatus
  startedAt: string | null
  completedAt: string | null
  errorCode: string | null
  branch: string | null
  commit: string | null
  sizeBytes: number | null
}

export const RESUME_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
} as const

export type ResumeReviewStatus =
  | typeof RESUME_STATUS.PENDING
  | typeof RESUME_STATUS.APPROVED
  | typeof RESUME_STATUS.REJECTED

export interface ResumeReviewListItem {
  id: number
  applicantId: number
  username: string | null
  realName: string | null
  avatar: string | null
  departmentId: number | null
  departmentName: string | null
  status: ResumeReviewStatus
  contentSummary: string
  gitRepoUrl: string | null
  gitSyncStatus: ResumeGitSyncStatus | null
  createTime: string | null
  updateTime: string | null
  auditTime: string | null
}

export interface ResumeReviewDetail extends ResumeReviewListItem {
  email: string | null
  studentId: string | null
  college: string | null
  className: string | null
  content: string | null
  gitSyncCompletedAt: string | null
  gitSyncErrorCode: string | null
  gitSyncBranch: string | null
  gitSyncCommit: string | null
  gitSyncSizeBytes: number | null
  rejectReason: string | null
  auditBy: number | null
  auditorName: string | null
}

export interface ResumeReviewListParams {
  page?: number
  size?: number
  status?: ResumeReviewStatus
}

export interface ResumeAuditPayload {
  resumeId: number
  pass: boolean
  reason?: string
}

export function isPendingResumeReview(status: number | null | undefined) {
  return status === RESUME_STATUS.PENDING
}

export const resumeService = {
  getMyResume: () => {
    return api.get<ResumeData, ResumeData>("/api/resume/my")
  },

  save: (data: { content: string; gitRepoUrl: string }) => {
    return api.post<string, string>("/api/resume/save", data)
  },

  submit: () => {
    return api.post<string, string>("/api/resume/submit")
  },

  getGitSyncStatus: () =>
    api.get<ResumeGitSyncData, ResumeGitSyncData>("/api/resume/git-sync"),

  syncGitRepository: () =>
    api.post<ResumeGitSyncData, ResumeGitSyncData>("/api/resume/git-sync"),

  listReviews: (params: ResumeReviewListParams = {}) =>
    api.get<PageResult<ResumeReviewListItem>, PageResult<ResumeReviewListItem>>(
      "/api/resume/reviews",
      { params }
    ),

  reviewDetail: (id: number) =>
    api.get<ResumeReviewDetail, ResumeReviewDetail>(`/api/resume/reviews/${id}`),

  audit: (data: ResumeAuditPayload) =>
    api.post<string, string>("/api/resume/audit", data),
}
