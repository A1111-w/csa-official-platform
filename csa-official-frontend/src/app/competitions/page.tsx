import { Trophy } from "lucide-react"

import { CompetitionBoard } from "@/components/business/competitions/CompetitionBoard"
import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"

export default function CompetitionsPage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-12 md:px-8">
        <section className="mb-10 max-w-3xl">
          <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Trophy className="h-5 w-5" />
          </div>
          <h1 className="text-4xl font-semibold tracking-tight">比赛活动</h1>
          <p className="mt-3 text-sm leading-8 text-muted-foreground">
            汇总协会近期比赛、训练营和项目型活动，成员可在控制台继续维护发布内容。
          </p>
        </section>
        <CompetitionBoard />
      </main>
      <Footer />
    </div>
  )
}
