import { AxiosHeaders, type AxiosAdapter } from "axios"
import { describe, expect, it } from "vitest"

import api from "@/lib/axios"
import { ApiError } from "@/types/api"

function adapter(status: number, data: unknown): AxiosAdapter {
  return async (config) => ({
    config,
    data,
    headers: new AxiosHeaders(),
    status,
    statusText: String(status),
  })
}

describe("API response handling", () => {
  it("unwraps successful API envelopes", async () => {
    await expect(
      api.get("/test/success", {
        adapter: adapter(200, { code: 200, message: "Success", data: { ok: true } }),
      })
    ).resolves.toEqual({ ok: true })
  })

  it("rejects a business error even when the HTTP response is 200", async () => {
    const request = api.get("/test/business-error", {
      adapter: adapter(200, {
        code: 409,
        message: "duplicate",
        data: null,
        errorCode: "CONFLICT",
        traceId: "trace-business",
      }),
    })

    await expect(request).rejects.toMatchObject({
      message: "duplicate",
      status: 200,
      code: 409,
      errorCode: "CONFLICT",
      traceId: "trace-business",
    } satisfies Partial<ApiError>)
  })

  it("rejects a non-2xx HTTP response even if the body claims success", async () => {
    const request = api.get("/test/http-error", {
      adapter: adapter(503, {
        code: 200,
        message: "temporarily unavailable",
        data: null,
        errorCode: "SERVICE_UNAVAILABLE",
        traceId: "trace-http",
      }),
      validateStatus: () => true,
    })

    await expect(request).rejects.toMatchObject({
      message: "temporarily unavailable",
      status: 503,
      code: 200,
      errorCode: "SERVICE_UNAVAILABLE",
      traceId: "trace-http",
    } satisfies Partial<ApiError>)
  })
})
