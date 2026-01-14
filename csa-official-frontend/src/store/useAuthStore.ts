import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  user: {
    username: string;
    roleLevel: number;
  } | null;
  // 动作
  setLogin: (token: string, user: { username: string; roleLevel: number }) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,

      setLogin: (token, user) => {
        set({ token, user });
        // 手动存一下 localStorage，双重保险 (虽然 persist 会存，但为了 axios 拦截器读取方便)
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', token);
        }
      },

      logout: () => {
        set({ token: null, user: null });
        if (typeof window !== 'undefined') {
          localStorage.removeItem('token');
        }
      },
    }),
    {
      name: 'csa-auth-storage', // 存到 localStorage 的 key 名字
      storage: createJSONStorage(() => localStorage),
    }
  )
);