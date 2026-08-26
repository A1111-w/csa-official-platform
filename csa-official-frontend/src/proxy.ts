import { NextRequest, NextResponse } from "next/server"

const AUTH_COOKIE_NAME = process.env.AUTH_COOKIE_NAME || "CSA_AUTH_TOKEN"

function getApiOrigin() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL

  if (!apiUrl) {
    return null
  }

  try {
    return new URL(apiUrl).origin
  } catch {
    return null
  }
}

function buildContentSecurityPolicy(nonce: string) {
  const isDevelopment = process.env.NODE_ENV === "development"
  const connectSrc = [
    "'self'",
    getApiOrigin(),
    ...(isDevelopment
      ? [
          "http://localhost:8080",
          "http://127.0.0.1:8080",
          "ws://localhost:*",
          "ws://127.0.0.1:*",
        ]
      : []),
  ].filter(Boolean)

  return [
    "default-src 'self'",
    "base-uri 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "form-action 'self'",
    "img-src 'self' data: blob: https:",
    "font-src 'self' data:",
    "style-src 'self' 'unsafe-inline'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDevelopment ? " 'unsafe-eval'" : ""}`,
    `connect-src ${connectSrc.join(" ")}`,
    ...(isDevelopment ? [] : ["upgrade-insecure-requests"]),
  ].join("; ")
}

function applySecurityHeaders(response: NextResponse, contentSecurityPolicy: string) {
  response.headers.set("Content-Security-Policy", contentSecurityPolicy)

  if (process.env.NODE_ENV === "production") {
    response.headers.set(
      "Strict-Transport-Security",
      "max-age=63072000; includeSubDomains; preload"
    )
  }

  return response
}

export function proxy(request: NextRequest) {
  const nonce = crypto.randomUUID().replaceAll("-", "")
  const contentSecurityPolicy = buildContentSecurityPolicy(nonce)

  if (
    request.nextUrl.pathname.startsWith("/dashboard") &&
    !request.cookies.get(AUTH_COOKIE_NAME)?.value
  ) {
    const loginUrl = new URL("/login", request.url)
    loginUrl.searchParams.set(
      "redirect",
      `${request.nextUrl.pathname}${request.nextUrl.search}`
    )
    return applySecurityHeaders(NextResponse.redirect(loginUrl), contentSecurityPolicy)
  }

  const requestHeaders = new Headers(request.headers)
  requestHeaders.set("x-nonce", nonce)
  requestHeaders.set("Content-Security-Policy", contentSecurityPolicy)

  return applySecurityHeaders(
    NextResponse.next({
      request: {
        headers: requestHeaders,
      },
    }),
    contentSecurityPolicy
  )
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt|.*\\..*).*)",
  ],
}
