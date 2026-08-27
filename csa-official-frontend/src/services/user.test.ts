import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import { userService } from "@/services/user"

describe("user service", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("exports selected member columns and filters", async () => {
    const file = new Blob(["xlsx"])
    const post = vi.spyOn(api, "post").mockResolvedValue(file)
    const payload = {
      columns: ["realName", "studentId"],
      college: "计算机学院",
      roleLevel: 2,
    }

    await expect(userService.exportMembers(payload)).resolves.toBe(file)

    expect(post).toHaveBeenCalledWith("/api/sys/export/members", payload, {
      responseType: "blob",
    })
  })
})
