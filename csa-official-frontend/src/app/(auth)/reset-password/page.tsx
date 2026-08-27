import Link from "next/link"

import { ResetPasswordForm } from "@/components/business/auth/ResetPasswordForm"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"

export default function ResetPasswordPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/20 px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>重置密码</CardTitle>
          <CardDescription>验证码只使用一次，重置完成后所有旧会话都会失效。</CardDescription>
        </CardHeader>
        <CardContent>
          <ResetPasswordForm />
        </CardContent>
        <CardFooter className="justify-center text-sm text-muted-foreground">
          <Link href="/login" className="text-primary hover:underline">返回登录</Link>
        </CardFooter>
      </Card>
    </main>
  )
}
