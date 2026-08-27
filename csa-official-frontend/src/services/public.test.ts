import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import { publicService } from "@/services/public"

describe("public service", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("loads carousel and contribution ranking data", async () => {
    const get = vi.spyOn(api, "get")
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([])

    await publicService.getCarousel()
    await publicService.getContributionRank(6)

    expect(get).toHaveBeenNthCalledWith(1, "/api/public/carousel/list")
    expect(get).toHaveBeenNthCalledWith(2, "/api/public/contribution/rank", {
      params: { limit: 6 },
    })
  })
})
