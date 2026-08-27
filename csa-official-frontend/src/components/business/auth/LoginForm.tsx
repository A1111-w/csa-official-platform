"use client"

import { useEffect, useRef, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { Eye, EyeOff } from "lucide-react"
import { useRouter, useSearchParams } from "next/navigation"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import * as z from "zod"

import type { AuthSceneMood } from "@/components/business/auth/AnimatedAuthScene"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { getSafeRedirect } from "@/lib/navigation"
import { authService } from "@/services/auth"
import { useAuthStore } from "@/store/useAuthStore"

const formSchema = z.object({
  username: z.string().min(1, "请输入用户名"),
  password: z.string().min(1, "请输入密码"),
})

type LoginFormProps = {
  onSceneChange?: (mood: AuthSceneMood) => void
}

export function LoginForm({ onSceneChange }: LoginFormProps = {}) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setLogin = useAuthStore((state) => state.setLogin)
  const [loading, setLoading] = useState(false)
  const [activeField, setActiveField] = useState<"username" | "password" | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const errorTimerRef = useRef<number | null>(null)

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  })

  useEffect(() => {
    return () => {
      if (errorTimerRef.current) {
        window.clearTimeout(errorTimerRef.current)
      }
    }
  }, [])

  const setScene = (mood: AuthSceneMood) => {
    onSceneChange?.(mood)
  }

  const showErrorScene = () => {
    setScene("error")
    if (errorTimerRef.current) {
      window.clearTimeout(errorTimerRef.current)
    }
    errorTimerRef.current = window.setTimeout(() => {
      setScene(activeField === "password" ? (showPassword ? "peek" : "password") : "idle")
    }, 900)
  }

  async function onSubmit(values: z.infer<typeof formSchema>) {
    setLoading(true)
    try {
      const data = await authService.login(values)

      setLogin({
        username: data.username,
        roleLevel: data.roleLevel,
      })

      toast.success(`欢迎回来，${data.username}`)
      setScene("success")
      router.push(getSafeRedirect(searchParams.get("redirect")))
    } catch (error) {
      const msg = error instanceof Error ? error.message : "登录失败"
      showErrorScene()

      if (msg.includes("账号") || msg.includes("密码")) {
        form.setError("password", {
          type: "manual",
          message: "账号或密码错误，请检查后重试",
        })
        form.setValue("password", "")
      } else {
        toast.error(msg)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit, () => showErrorScene())} className="space-y-5">
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>账号</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入你的用户名"
                  autoComplete="username"
                  {...field}
                  onFocus={() => {
                    setActiveField("username")
                    setScene("username")
                  }}
                  onBlur={() => {
                    field.onBlur()
                    setActiveField(null)
                    setScene("idle")
                  }}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>密码</FormLabel>
              <FormControl>
                <div className="relative">
                  <Input
                    type={showPassword ? "text" : "password"}
                    placeholder="输入密码"
                    autoComplete="current-password"
                    className="pr-10"
                    {...field}
                    onFocus={() => {
                      setActiveField("password")
                      setScene(showPassword ? "peek" : "password")
                    }}
                    onBlur={() => {
                      field.onBlur()
                      setActiveField(null)
                      setScene("idle")
                    }}
                  />
                  <button
                    type="button"
                    aria-label={showPassword ? "隐藏密码" : "显示密码"}
                    className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                    onClick={() => {
                      const next = !showPassword
                      setShowPassword(next)
                      setScene(next ? "peek" : "password")
                    }}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? "登录中..." : "立即登录"}
        </Button>
      </form>
    </Form>
  )
}
