import Link from "next/link"

import { ForgotPasswordForm } from "@/components/business/auth/ForgotPasswordForm"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"

export default function ForgotPasswordPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/20 px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>找回密码</CardTitle>
          <CardDescription>输入绑定邮箱，我们会发送一次性重置验证码。</CardDescription>
        </CardHeader>
        <CardContent>
          <ForgotPasswordForm />
        </CardContent>
        <CardFooter className="justify-center text-sm text-muted-foreground">
          <Link href="/login" className="text-primary hover:underline">返回登录</Link>
        </CardFooter>
      </Card>
    </main>
  )
}
