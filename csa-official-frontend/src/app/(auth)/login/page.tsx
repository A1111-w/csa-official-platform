import Link from "next/link";
import { LoginForm } from "@/components/business/auth/LoginForm";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <Card className="w-full max-w-md shadow-lg border-0 sm:border sm:shadow-sm">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight text-blue-900">
            CSA 登录
          </CardTitle>
          <CardDescription>
            欢迎回到计算机协会官方平台
          </CardDescription>
        </CardHeader>
        <CardContent>
          {/* 放入表单组件 */}
          <LoginForm />
        </CardContent>
        <CardFooter className="flex justify-center text-sm text-slate-500">
          还没有账号？
          <Link href="/register" className="ml-1 text-blue-600 hover:underline font-medium">
            立即注册
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}