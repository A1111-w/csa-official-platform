import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios"

import { getSafeRedirect } from "@/lib/navigation"
import { useAuthStore } from "@/store/useAuthStore"
import { ApiError, type ApiEnvelope } from "@/types/api"

const baseURL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
const CSRF_ENDPOINT = "/api/auth/csrf"
const CSRF_HEADER_NAME = "X-CSRF-Token"
const CSRF_EXEMPT_PATHS = new Set([
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/send-code",
  CSRF_ENDPOINT,
])
const SAFE_METHODS = new Set(["get", "head", "options", "trace"])

let csrfToken: string | null = null
let csrfTokenPromise: Promise<string | null> | null = null

declare module "axios" {
  interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
  }

  interface InternalAxiosRequestConfig {
    skipAuthRedirect?: boolean
  }
}

const api = axios.create({
  baseURL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
})

function isApiEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  if (!value || typeof value !== "object") {
    return false
  }

  return typeof (value as { code?: unknown }).code === "number"
}

function extractCsrfToken(payload: unknown) {
  const data = isApiEnvelope(payload) ? payload.data : payload

  if (!data || typeof data !== "object") {
    return null
  }

  const token = (data as { csrfToken?: unknown }).csrfToken
  return typeof token === "string" && token.trim() ? token : null
}

function buildApiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`

  if (/^https?:\/\//i.test(baseURL)) {
    return new URL(normalizedPath, baseURL).toString()
  }

  return `${baseURL.replace(/\/$/, "")}${normalizedPath}`
}

function getRequestPath(url?: string) {
  if (!url) {
    return ""
  }

  try {
    const fallbackBase = /^https?:\/\//i.test(baseURL) ? baseURL : "http://csa.local"
    return new URL(url, fallbackBase).pathname
  } catch {
    return url.split("?")[0] ?? url
  }
}

function needsCsrf(config: InternalAxiosRequestConfig) {
  const method = config.method?.toLowerCase() ?? "get"
  return !SAFE_METHODS.has(method) && !CSRF_EXEMPT_PATHS.has(getRequestPath(config.url))
}

async function ensureCsrfToken() {
  if (typeof window === "undefined") {
    return null
  }

  if (csrfToken) {
    return csrfToken
  }

  csrfTokenPromise ??= fetch(buildApiUrl(CSRF_ENDPOINT), {
    credentials: "include",
  })
    .then(async (response) => {
      if (!response.ok) {
        return null
      }

      const payload = (await response.json()) as unknown
      csrfToken = extractCsrfToken(payload)
      return csrfToken
    })
    .catch(() => null)
    .finally(() => {
      csrfTokenPromise = null
    })

  return csrfTokenPromise
}

export function rememberCsrfToken(token: string | null | undefined) {
  csrfToken = token && token.trim() ? token : null
}

export function clearCsrfToken() {
  csrfToken = null
  csrfTokenPromise = null
}

function getErrorMessage(payload: unknown) {
  if (!payload || typeof payload !== "object") {
    return null
  }

  const message = (payload as { message?: unknown }).message
  return typeof message === "string" && message.trim() ? message : null
}

function getOptionalString(payload: unknown, key: "errorCode" | "traceId") {
  if (!payload || typeof payload !== "object") {
    return undefined
  }

  const value = (payload as Record<string, unknown>)[key]
  return typeof value === "string" && value.trim() ? value : undefined
}

function handleUnauthorized() {
  if (typeof window === "undefined") {
    return
  }

  const pathname = window.location.pathname
  if (pathname.startsWith("/login") || pathname.startsWith("/register")) {
    return
  }

  clearCsrfToken()
  useAuthStore.getState().logout()
  const redirect = getSafeRedirect(`${pathname}${window.location.search}`, "/dashboard")
  window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
}

function shouldHandleUnauthorized(config?: AxiosRequestConfig | InternalAxiosRequestConfig) {
  return !config?.skipAuthRedirect
}

api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 认证走 httpOnly cookie（withCredentials），前端不再附带 Bearer token。
    // 对非安全方法附加 CSRF token 完成双提交校验。
    if (typeof window !== "undefined" && needsCsrf(config)) {
      const token = await ensureCsrfToken()
      if (token) {
        config.headers.set(CSRF_HEADER_NAME, token)
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  ((response: AxiosResponse) => {
    const payload = response.data as unknown

    if (response.status < 200 || response.status >= 300) {
      if (
        (response.status === 401 || (isApiEnvelope(payload) && payload.code === 401)) &&
        shouldHandleUnauthorized(response.config)
      ) {
        handleUnauthorized()
      }

      return Promise.reject(
        new ApiError({
          message: getErrorMessage(payload) ?? `HTTP ${response.status}`,
          status: response.status,
          code: isApiEnvelope(payload) ? payload.code : undefined,
          errorCode: getOptionalString(payload, "errorCode"),
          traceId: getOptionalString(payload, "traceId"),
          details: isApiEnvelope(payload) ? payload.data : payload,
        })
      )
    }

    if (isApiEnvelope(payload)) {
      if (payload.code === 200) {
        return payload.data
      }

      if (payload.code === 401) {
        if (shouldHandleUnauthorized(response.config)) {
          handleUnauthorized()
        }
      }

      return Promise.reject(
        new ApiError({
          message: payload.message || "请求失败",
          status: response.status,
          code: payload.code,
          errorCode: getOptionalString(payload, "errorCode"),
          traceId: getOptionalString(payload, "traceId"),
          details: payload.data,
        })
      )
    }

    return payload
  }) as unknown as (value: AxiosResponse) => AxiosResponse | Promise<AxiosResponse>,
  (error: AxiosError<unknown>) => {
    const status = error.response?.status
    const payload = error.response?.data

    if (
      (status === 401 || (isApiEnvelope(payload) && payload.code === 401)) &&
      shouldHandleUnauthorized(error.config)
    ) {
      handleUnauthorized()
    }

    const message =
      getErrorMessage(payload) ??
      error.message ??
      "网络请求失败，请稍后再试"

    return Promise.reject(
      new ApiError({
        message,
        status,
        code: isApiEnvelope(payload) ? payload.code : undefined,
        errorCode: getOptionalString(payload, "errorCode"),
        traceId: getOptionalString(payload, "traceId"),
        details: isApiEnvelope(payload) ? payload.data : payload,
      })
    )
  }
)

export default api
