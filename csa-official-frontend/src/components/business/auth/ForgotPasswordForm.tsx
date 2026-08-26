"use client"

import { useState } from "react"
import Link from "next/link"
import { Loader2, Mail } from "lucide-react"
import { toast } from "sonner"
import * as z from "zod"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"

import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { authService } from "@/services/auth"

const schema = z.object({
  email: z.string().email("请输入有效邮箱"),
})

type FormValues = z.infer<typeof schema>

export function ForgotPasswordForm() {
  const [loading, setLoading] = useState(false)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
  })

  async function onSubmit(values: FormValues) {
    setLoading(true)
    try {
      await authService.forgotPassword(values)
      toast.success("如果账号存在，重置验证码已发送")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "请求失败，请稍后重试")
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
                <div className="relative">
                  <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input className="pl-9" autoComplete="email" placeholder="name@example.com" {...field} />
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? <Loader2 className="animate-spin" /> : null}
          发送重置验证码
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          已收到验证码？
          <Link href="/reset-password" className="ml-1 text-primary hover:underline">
            重置密码
          </Link>
        </p>
      </form>
    </Form>
  )
}
