"use client"

import { useEffect } from "react"
import type { ReactNode } from "react"
import { ThemeProvider } from "next-themes"

import { Toaster } from "@/components/ui/sonner"
import { userService } from "@/services/user"
import { useAuthStore } from "@/store/useAuthStore"

export function AppProviders({
  children,
  nonce,
}: {
  children: ReactNode
  nonce?: string
}) {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="light"
      enableSystem={false}
      forcedTheme="light"
      disableTransitionOnChange
      nonce={nonce}
    >
      <AuthBootstrap />
      {children}
      <Toaster position="top-right" richColors />
    </ThemeProvider>
  )
}

function AuthBootstrap() {
  const { user, setLogin, logout } = useAuthStore()
  const username = user?.username
  const roleLevel = user?.roleLevel

  useEffect(() => {
    if (!username || roleLevel == null) {
      return
    }

    let cancelled = false
    userService
      .getInfo({ skipAuthRedirect: true })
      .then((info) => {
        if (!cancelled) {
          setLogin({
            username: info.username,
            roleLevel: info.roleLevel,
          })
        }
      })
      .catch(() => {
        if (!cancelled) {
          logout()
        }
      })

    return () => {
      cancelled = true
    }
  }, [logout, roleLevel, setLogin, username])

  return null
}
