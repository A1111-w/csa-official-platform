import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import type { AuthUser } from "@/types/user"

interface AuthState {
  user: AuthUser | null
  setLogin: (user: AuthUser) => void
  setUser: (user: AuthUser | null) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,

      // 认证凭据只存在于 httpOnly cookie 中，前端不再持有 token
      setLogin: (user) => {
        set({ user })
        if (typeof window !== "undefined") {
          localStorage.removeItem("token")
        }
      },

      setUser: (user) => {
        set({ user })
      },

      logout: () => {
        set({ user: null })
        if (typeof window !== "undefined") {
          localStorage.removeItem("token")
        }
      },
    }),
    {
      name: "csa-auth-storage",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user }),
      version: 3,
      migrate: (persistedState) => {
        const state = persistedState as Partial<AuthState> | undefined
        return {
          user: state?.user ?? null,
        }
      },
    }
  )
)
