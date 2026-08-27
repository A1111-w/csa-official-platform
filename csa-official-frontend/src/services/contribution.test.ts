import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import { contributionService } from "@/services/contribution"

describe("contributionService", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("creates a manual contribution record", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue("Award granted")
    const payload = {
      userId: 7,
      type: "DEV" as const,
      score: 12.5,
      reason: "完成官网无障碍改造",
    }

    await contributionService.award(payload)

    expect(post).toHaveBeenCalledWith("/api/sys/contribution/award", payload)
  })

  it("loads filtered contribution history", async () => {
    const get = vi.spyOn(api, "get").mockResolvedValue({
      records: [],
      current: 1,
      size: 20,
      total: 0,
      pages: 0,
    })

    await contributionService.listAwards({
      page: 2,
      size: 20,
      keyword: "张三",
      type: "DEV",
      source: "MANUAL",
    })

    expect(get).toHaveBeenCalledWith("/api/sys/contribution/awards", {
      params: {
        page: 2,
        size: 20,
        keyword: "张三",
        type: "DEV",
        source: "MANUAL",
      },
    })
  })
})
