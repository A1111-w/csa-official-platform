const FALLBACK_REDIRECT = "/dashboard"

export function getSafeRedirect(
  redirect: string | null | undefined,
  fallback = FALLBACK_REDIRECT
) {
  const value = redirect?.trim()

  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return fallback
  }

  try {
    const parsed = new URL(value, "http://csa.local")

    if (parsed.origin !== "http://csa.local") {
      return fallback
    }

    if (parsed.pathname.startsWith("/login") || parsed.pathname.startsWith("/register")) {
      return fallback
    }

    return `${parsed.pathname}${parsed.search}${parsed.hash}`
  } catch {
    return fallback
  }
}
