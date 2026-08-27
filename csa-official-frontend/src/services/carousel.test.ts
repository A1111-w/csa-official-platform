import { afterEach, describe, expect, it, vi } from "vitest"

import api from "@/lib/axios"
import { carouselService } from "@/services/carousel"

describe("carousel service", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("uses the protected management endpoints", async () => {
    const get = vi.spyOn(api, "get").mockResolvedValue([])
    const post = vi.spyOn(api, "post").mockResolvedValue("ok")

    await carouselService.list()
    await carouselService.save({
      title: "招新开放日",
      imgUrl: "/files/7/banner.png",
      targetUrl: "/register",
      sortOrder: 2,
      status: 1,
    })
    await carouselService.remove(9)

    expect(get).toHaveBeenCalledWith("/api/sys/carousel/list")
    expect(post).toHaveBeenNthCalledWith(1, "/api/sys/carousel/save", {
      title: "招新开放日",
      imgUrl: "/files/7/banner.png",
      targetUrl: "/register",
      sortOrder: 2,
      status: 1,
    })
    expect(post).toHaveBeenNthCalledWith(2, "/api/sys/carousel/delete", null, {
      params: { id: 9 },
    })
  })
})
