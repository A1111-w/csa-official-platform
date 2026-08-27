"use client"

import { useState } from "react"
import Link from "next/link"

import {
  AnimatedAuthScene,
  type AuthSceneMood,
} from "@/components/business/auth/AnimatedAuthScene"
import { RegisterForm } from "@/components/business/auth/RegisterForm"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

export default function RegisterPage() {
  const [sceneMood, setSceneMood] = useState<AuthSceneMood>("idle")

  return (
    <main className="min-h-screen bg-[linear-gradient(135deg,#f7fbff_0%,#eef4ff_48%,#edfff8_100%)] px-4 py-8">
      <div className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-6xl overflow-hidden rounded-lg border bg-white shadow-[0_24px_80px_rgba(37,56,112,0.14)] lg:grid-cols-[0.9fr_1.1fr]">
        <AnimatedAuthScene
          mood={sceneMood}
          label="注册"
          title="加入 CSA 平台"
          description="从游客到成员，你的权限会跟着协作深度逐步打开，资源、简历和提案都会进入同一套工作流。"
          compact
        />

        <section className="flex items-center justify-center px-4 py-10 sm:px-10">
          <Card className="w-full max-w-2xl border-0 shadow-none">
            <CardHeader className="space-y-2 px-0 text-left">
              <CardTitle className="text-3xl font-semibold tracking-tight">
                创建账号
              </CardTitle>
              <CardDescription>
                注册后即可进入协会平台，后续再按身份与权限继续解锁功能。
              </CardDescription>
            </CardHeader>
            <CardContent className="px-0">
              <RegisterForm onSceneChange={setSceneMood} />
            </CardContent>
            <CardFooter className="justify-center px-0 text-sm text-muted-foreground">
              已有账号？
              <Link href="/login" className="ml-1 font-medium text-primary hover:underline">
                直接登录
              </Link>
            </CardFooter>
          </Card>
        </section>
      </div>
    </main>
  )
}
