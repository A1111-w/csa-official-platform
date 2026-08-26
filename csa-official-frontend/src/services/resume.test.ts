import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import {
  RESUME_STATUS,
  isPendingResumeReview,
  resumeService,
} from "@/services/resume"

describe("resume review service", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("requests a paginated review queue with the selected status", async () => {
    const response = { records: [], total: 0, size: 20, current: 1, pages: 0 }
    const get = vi.spyOn(api, "get").mockResolvedValue(response)

    await expect(
      resumeService.listReviews({
        page: 1,
        size: 20,
        status: RESUME_STATUS.REJECTED,
      })
    ).resolves.toEqual(response)

    expect(get).toHaveBeenCalledWith("/api/resume/reviews", {
      params: { page: 1, size: 20, status: RESUME_STATUS.REJECTED },
    })
  })

  it("loads review detail and submits the review decision", async () => {
    const get = vi.spyOn(api, "get").mockResolvedValue({ id: 7 })
    const post = vi.spyOn(api, "post").mockResolvedValue("审核完成")

    await resumeService.reviewDetail(7)
    await resumeService.audit({ resumeId: 7, pass: false, reason: "项目说明不完整" })

    expect(get).toHaveBeenCalledWith("/api/resume/reviews/7")
    expect(post).toHaveBeenCalledWith("/api/resume/audit", {
      resumeId: 7,
      pass: false,
      reason: "项目说明不完整",
    })
  })

  it("loads and starts Git repository synchronization", async () => {
    const status = {
      configured: true,
      status: "SYNCING" as const,
      startedAt: null,
      completedAt: null,
      errorCode: null,
      branch: null,
      commit: null,
      sizeBytes: null,
    }
    const get = vi.spyOn(api, "get").mockResolvedValue(status)
    const post = vi.spyOn(api, "post").mockResolvedValue(status)

    await expect(resumeService.getGitSyncStatus()).resolves.toEqual(status)
    await expect(resumeService.syncGitRepository()).resolves.toEqual(status)

    expect(get).toHaveBeenCalledWith("/api/resume/git-sync")
    expect(post).toHaveBeenCalledWith("/api/resume/git-sync")
  })

  it("only treats pending resumes as reviewable", () => {
    expect(isPendingResumeReview(RESUME_STATUS.PENDING)).toBe(true)
    expect(isPendingResumeReview(RESUME_STATUS.APPROVED)).toBe(false)
    expect(isPendingResumeReview(RESUME_STATUS.REJECTED)).toBe(false)
    expect(isPendingResumeReview(RESUME_STATUS.DRAFT)).toBe(false)
  })
})
