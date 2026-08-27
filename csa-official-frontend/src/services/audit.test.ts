import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import { auditService } from "@/services/audit"

describe("audit service", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("loads a filtered audit page", async () => {
    const response = { records: [], total: 0, size: 20, current: 1, pages: 0 }
    const get = vi.spyOn(api, "get").mockResolvedValue(response)

    await expect(
      auditService.list({
        page: 2,
        size: 20,
        action: "ROLE_CHANGE",
        result: "SUCCESS",
        requestId: "request-1",
      })
    ).resolves.toEqual(response)

    expect(get).toHaveBeenCalledWith("/api/sys/audit/list", {
      params: {
        page: 2,
        size: 20,
        action: "ROLE_CHANGE",
        result: "SUCCESS",
        requestId: "request-1",
      },
    })
  })
})
