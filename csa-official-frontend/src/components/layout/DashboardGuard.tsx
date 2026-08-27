"use client"

import { useCallback, useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"

import { useIsClient } from "@/hooks/use-is-client"
import { getSafeRedirect } from "@/lib/navigation"
import { userService } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"

export function DashboardGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const isClient = useIsClient()
  const { logout, setUser, user } = useAuthStore()

  const redirectToLogin = useCallback(() => {
    const query = window.location.search.replace(/^\?/, "")
    const redirect = getSafeRedirect(`${pathname}${query ? `?${query}` : ""}`)
    router.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
  }, [pathname, router])

  useEffect(() => {
    if (!isClient) {
      return
    }

    if (!user) {
      redirectToLogin()
      return
    }

    let cancelled = false

    async function verifySession() {
      try {
        const profile = await userService.getInfo({ skipAuthRedirect: true })
        if (!cancelled) {
          setUser({
            username: profile.username,
            roleLevel: profile.roleLevel,
          })
        }
      } catch {
        if (!cancelled) {
          logout()
          redirectToLogin()
        }
      }
    }

    verifySession()

    return () => {
      cancelled = true
    }
  }, [isClient, logout, redirectToLogin, setUser, user])

  if (!isClient || !user) {
    return (
      <div className="space-y-4">
        <div className="h-28 rounded-lg border bg-background" />
        <div className="grid gap-4 md:grid-cols-3">
          <div className="h-32 rounded-lg border bg-background" />
          <div className="h-32 rounded-lg border bg-background" />
          <div className="h-32 rounded-lg border bg-background" />
        </div>
      </div>
    )
  }

  return children
}
