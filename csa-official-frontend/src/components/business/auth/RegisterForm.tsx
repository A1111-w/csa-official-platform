"use client"

import { type FocusEvent, useEffect, useRef, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { Eye, EyeOff, Loader2 } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import * as z from "zod"

import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import type { AuthSceneMood } from "@/components/business/auth/AnimatedAuthScene"
import { authService } from "@/services/auth"
import { publicService } from "@/services/public"

const registerSchema = z
  .object({
    username: z
      .string()
      .min(4, "用户名至少 4 位")
      .max(20, "用户名最多 20 位")
      .regex(/^[A-Za-z0-9_]+$/, "用户名只能包含字母、数字和下划线"),
    password: z
      .string()
      .regex(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/, "密码需包含字母和数字，长度 6-20 位"),
    confirmPassword: z.string(),
    email: z.string().email("邮箱格式不正确"),
    code: z.string().regex(/^\d{6}$/, "验证码必须是 6 位数字"),
    realName: z.string().optional(),
    studentId: z.string().optional(),
    college: z.string().optional(),
    className: z.string().optional(),
    inviteCode: z.string().optional(),
    merchantNo: z.string().optional(),
    privacyConsent: z.boolean().refine((value) => value, "请先同意隐私政策"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "两次输入的密码不一致",
    path: ["confirmPassword"],
  })

type RegisterFormValues = z.infer<typeof registerSchema>

type RegisterFormProps = {
  onSceneChange?: (mood: AuthSceneMood) => void
}

export function RegisterForm({ onSceneChange }: RegisterFormProps = {}) {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [sendingCode, setSendingCode] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [activeField, setActiveField] = useState<"username" | "password" | null>(null)
  const [privacyVersion, setPrivacyVersion] = useState<string | null>(null)
  const [privacyLoading, setPrivacyLoading] = useState(true)
  const errorTimerRef = useRef<number | null>(null)

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      password: "",
      confirmPassword: "",
      email: "",
      code: "",
      realName: "",
      studentId: "",
      college: "",
      className: "",
      inviteCode: "",
      merchantNo: "",
      privacyConsent: false,
    },
  })

  useEffect(() => {
    let cancelled = false
    publicService
      .getPrivacyNotice()
      .then((notice) => {
        if (!cancelled) {
          setPrivacyVersion(notice.policyVersion)
        }
      })
      .catch(() => {
        if (!cancelled) {
          toast.error("隐私政策暂时无法加载，请稍后重试")
        }
      })
      .finally(() => {
        if (!cancelled) {
          setPrivacyLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (countdown <= 0) {
      return
    }

    const timer = window.setTimeout(() => setCountdown((value) => value - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [countdown])

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

  const setPasswordScene = (visible = showPassword || showConfirmPassword) => {
    setScene(visible ? "peek" : "password")
  }

  const showErrorScene = () => {
    setScene("error")
    if (errorTimerRef.current) {
      window.clearTimeout(errorTimerRef.current)
    }
    errorTimerRef.current = window.setTimeout(() => {
      if (activeField === "password") {
        setPasswordScene()
      } else if (activeField === "username") {
        setScene("username")
      } else {
        setScene("idle")
      }
    }, 900)
  }

  const sceneFieldProps = (field: "username" | "password") => ({
    onFocus: () => {
      setActiveField(field)
      if (field === "password") {
        setPasswordScene()
      } else {
        setScene("username")
      }
    },
    onBlur: (event: FocusEvent<HTMLElement>) => {
      if (event.currentTarget.contains(event.relatedTarget as Node | null)) {
        return
      }
      setActiveField(null)
      setScene("idle")
    },
  })

  const onSendCode = async () => {
    const email = form.getValues("email")
    const isEmailValid = await form.trigger("email")

    if (!isEmailValid || !email) {
      showErrorScene()
      toast.error("请先输入有效的邮箱地址")
      return
    }

    setSendingCode(true)
    try {
      await authService.sendCode(email)
      toast.success("验证码已发送，请查收邮件")
      setCountdown(60)
    } catch (error) {
      showErrorScene()
      const errorMsg = error instanceof Error ? error.message : "发送失败，请稍后再试"
      toast.error(errorMsg)
    } finally {
      setSendingCode(false)
    }
  }

  async function onSubmit(values: RegisterFormValues) {
    if (!privacyVersion) {
      toast.error("隐私政策版本尚未加载")
      return
    }

    setLoading(true)
    try {
      await authService.register({
        username: values.username,
        password: values.password,
        email: values.email,
        code: values.code,
        realName: values.realName,
        studentId: values.studentId,
        college: values.college,
        className: values.className,
        inviteCode: values.inviteCode,
        merchantNo: values.merchantNo,
        privacyConsentVersion: privacyVersion,
      })

      toast.success("注册成功，即将跳转登录页")
      window.setTimeout(() => {
        router.push("/login")
      }, 1200)
    } catch (error) {
      showErrorScene()
      const message = error instanceof Error ? error.message : "注册失败，请稍后重试"
      if (message.includes("用户名")) {
        form.setError("username", { message })
      } else if (message.includes("验证码")) {
        form.setError("code", { message })
      } else {
        toast.error(message)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit, () => showErrorScene())} className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2">
          <FormField
            control={form.control}
            name="username"
            render={({ field }) => (
              <FormItem {...sceneFieldProps("username")}>
                <FormLabel>
                  用户名 <span className="text-red-500">*</span>
                </FormLabel>
                <FormControl>
                  <Input placeholder="英文、数字或下划线" autoComplete="username" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="realName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>真实姓名</FormLabel>
                <FormControl>
                  <Input placeholder="便于协会联系，可选填" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem {...sceneFieldProps("password")}>
                <FormLabel>
                  密码 <span className="text-red-500">*</span>
                </FormLabel>
                <FormControl>
                  <div className="relative">
                    <Input
                      type={showPassword ? "text" : "password"}
                    placeholder="6-20 位，需包含字母和数字"
                      autoComplete="new-password"
                      className="pr-10"
                      {...field}
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
          <FormField
            control={form.control}
            name="confirmPassword"
            render={({ field }) => (
              <FormItem {...sceneFieldProps("password")}>
                <FormLabel>
                  确认密码 <span className="text-red-500">*</span>
                </FormLabel>
                <FormControl>
                  <div className="relative">
                    <Input
                      type={showConfirmPassword ? "text" : "password"}
                    placeholder="再次输入密码"
                      autoComplete="new-password"
                      className="pr-10"
                      {...field}
                    />
                    <button
                      type="button"
                      aria-label={showConfirmPassword ? "隐藏确认密码" : "显示确认密码"}
                      className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                      onClick={() => {
                        const next = !showConfirmPassword
                        setShowConfirmPassword(next)
                        setScene(next ? "peek" : "password")
                      }}
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                邮箱 <span className="text-red-500">*</span>
              </FormLabel>
              <FormControl>
                <Input placeholder="example@qq.com" autoComplete="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <FormLabel>
                验证码 <span className="text-red-500">*</span>
              </FormLabel>
              <div className="flex gap-2">
                <FormControl>
                  <Input placeholder="6 位数字" maxLength={6} {...field} />
                </FormControl>
                <Button
                  type="button"
                  variant="outline"
                  disabled={countdown > 0 || sendingCode}
                  onClick={onSendCode}
                  className="w-36 shrink-0"
                >
                  {sendingCode ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      发送中
                    </>
                  ) : countdown > 0 ? (
                    `${countdown}s 后重发`
                  ) : (
                    "发送验证码"
                  )}
                </Button>
              </div>
              <FormMessage />
            </FormItem>
          )}
        />

        <Tabs defaultValue="invite" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="invite">邀请码注册</TabsTrigger>
            <TabsTrigger value="guest">普通注册</TabsTrigger>
          </TabsList>

          <TabsContent value="invite" className="mt-4">
            <FormField
              control={form.control}
              name="inviteCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>邀请码</FormLabel>
                  <FormControl>
                    <Input placeholder="输入协会发放的邀请码" {...field} />
                  </FormControl>
                  <FormDescription>拥有邀请码可直接解锁会员身份。</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </TabsContent>

          <TabsContent value="guest" className="mt-4">
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="merchantNo"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>支付单号（选填）</FormLabel>
                    <FormControl>
                      <Input placeholder="微信或支付宝支付单号" {...field} />
                    </FormControl>
                    <FormDescription>
                      若已缴费可辅助人工核验；未填写则默认创建游客账号。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <div className="grid gap-3 md:grid-cols-3">
                <FormField
                  control={form.control}
                  name="college"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Input placeholder="学院" {...field} />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="className"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Input placeholder="班级" {...field} />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="studentId"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Input placeholder="学号" {...field} />
                      </FormControl>
                    </FormItem>
                  )}
                />
              </div>
            </div>
          </TabsContent>
        </Tabs>

        <FormField
          control={form.control}
          name="privacyConsent"
          render={({ field }) => (
            <FormItem className="flex items-start gap-3 rounded-md border bg-muted/20 p-3">
              <FormControl>
                <input
                  type="checkbox"
                  className="mt-1 size-4 accent-primary"
                  checked={field.value}
                  onChange={(event) => field.onChange(event.target.checked)}
                  onBlur={field.onBlur}
                  ref={field.ref}
                  disabled={privacyLoading}
                />
              </FormControl>
              <div className="space-y-1">
                <FormLabel className="leading-6">
                  我已阅读并同意
                  <Link href="/privacy" className="ml-1 text-primary underline underline-offset-4">
                    隐私政策
                  </Link>
                </FormLabel>
                <FormDescription>
                  当前版本：{privacyVersion ?? "加载中"}
                </FormDescription>
                <FormMessage />
              </div>
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full" disabled={loading || privacyLoading || !privacyVersion}>
          {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          立即注册
        </Button>
      </form>
    </Form>
  )
}
