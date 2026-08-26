"use client"

import { useState } from "react"
import Link from "next/link"
import { KeyRound, Loader2 } from "lucide-react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import * as z from "zod"
import { zodResolver } from "@hookform/resolvers/zod"

import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { authService } from "@/services/auth"

const schema = z
  .object({
    email: z.string().email("请输入有效邮箱"),
    code: z.string().regex(/^\d{6}$/, "验证码必须是 6 位数字"),
    newPassword: z
      .string()
      .regex(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,64}$/, "密码需包含字母和数字，长度 8-64 位"),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ["confirmPassword"],
    message: "两次输入的密码不一致",
  })

type FormValues = z.infer<typeof schema>

export function ResetPasswordForm() {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", code: "", newPassword: "", confirmPassword: "" },
  })

  async function onSubmit(values: FormValues) {
    setLoading(true)
    try {
      await authService.resetPassword({
        email: values.email,
        code: values.code,
        newPassword: values.newPassword,
      })
      toast.success("密码已重置，请重新登录")
      router.push("/login")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "重置失败，请稍后重试")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>绑定邮箱</FormLabel>
              <FormControl>
                <Input autoComplete="email" placeholder="name@example.com" {...field} />
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
              <FormLabel>重置验证码</FormLabel>
              <FormControl>
                <Input inputMode="numeric" maxLength={6} placeholder="6 位验证码" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {(["newPassword", "confirmPassword"] as const).map((name) => (
          <FormField
            key={name}
            control={form.control}
            name={name}
            render={({ field }) => (
              <FormItem>
                <FormLabel>{name === "newPassword" ? "新密码" : "确认新密码"}</FormLabel>
                <FormControl>
                  <div className="relative">
                    <KeyRound className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      className="pl-9"
                      type="password"
                      autoComplete="new-password"
                      {...field}
                    />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        ))}
        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? <Loader2 className="animate-spin" /> : null}
          确认重置密码
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          想重新发送验证码？
          <Link href="/forgot-password" className="ml-1 text-primary hover:underline">
            找回密码
          </Link>
        </p>
      </form>
    </Form>
  )
}
