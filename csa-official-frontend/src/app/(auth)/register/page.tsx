import Link from "next/link";
import { RegisterForm } from "@/components/business/auth/RegisterForm";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

export default function RegisterPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <Card className="w-full max-w-lg shadow-lg border-0 sm:border sm:shadow-sm">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight text-blue-900">
            加入 CSA
          </CardTitle>
          <CardDescription>
            创建您的账户以访问协会资源、比赛和投票系统
          </CardDescription>
        </CardHeader>
        <CardContent>
          <RegisterForm />
        </CardContent>
        <CardFooter className="flex justify-center text-sm text-slate-500">
          已有账号？
          <Link href="/login" className="ml-1 text-blue-600 hover:underline font-medium">
            直接登录
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}