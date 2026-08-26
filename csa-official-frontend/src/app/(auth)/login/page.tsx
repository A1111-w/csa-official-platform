"use client"

import { Suspense, useState } from "react"
import Link from "next/link"

import {
  AnimatedAuthScene,
  type AuthSceneMood,
} from "@/components/business/auth/AnimatedAuthScene"
import { LoginForm } from "@/components/business/auth/LoginForm"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

export default function LoginPage() {
  const [sceneMood, setSceneMood] = useState<AuthSceneMood>("idle")

  return (
    <main className="min-h-screen bg-[linear-gradient(135deg,#f7fbff_0%,#eef4ff_48%,#edfff8_100%)] px-4 py-8">
      <div className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-6xl overflow-hidden rounded-lg border bg-white shadow-[0_24px_80px_rgba(37,56,112,0.14)] lg:grid-cols-[1fr_0.95fr]">
        <AnimatedAuthScene
          mood={sceneMood}
          title="CSA 工作台登录"
          description="继续处理协会资源、比赛活动、简历审核和内部协作，所有入口都按你的身份自动收拢。"
        />

        <section className="flex items-center justify-center px-4 py-10 sm:px-10">
          <Card className="w-full max-w-md border-0 shadow-none">
            <CardHeader className="space-y-2 px-0 text-left">
              <CardTitle className="text-3xl font-semibold tracking-tight">
                欢迎回来
              </CardTitle>
              <CardDescription>
                使用你的协会账号登录，进入当前已解锁的功能区。
              </CardDescription>
            </CardHeader>
            <CardContent className="px-0">
              <Suspense fallback={<div className="h-52 rounded-lg border bg-muted/40" />}>
                <LoginForm onSceneChange={setSceneMood} />
              </Suspense>
            </CardContent>
            <CardFooter className="flex-col justify-center gap-2 px-0 text-sm text-muted-foreground">
              还没有账号？
              <Link href="/register" className="ml-1 font-medium text-primary hover:underline">
                立即注册
              </Link>
              <Link href="/forgot-password" className="font-medium text-primary hover:underline">
                忘记密码
              </Link>
            </CardFooter>
          </Card>
        </section>
      </div>
    </main>
  )
}
