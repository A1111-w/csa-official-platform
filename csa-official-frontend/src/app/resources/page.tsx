import { HardDrive } from "lucide-react"

import { ResourceLibrary } from "@/components/business/resources/ResourceLibrary"
import { Footer } from "@/components/layout/Footer"
import { Navbar } from "@/components/layout/Navbar"

export default function ResourcesPage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-12 md:px-8">
        <section className="mb-10 max-w-3xl">
          <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <HardDrive className="h-5 w-5" />
          </div>
          <h1 className="text-4xl font-semibold tracking-tight">资源库</h1>
          <p className="mt-3 text-sm leading-8 text-muted-foreground">
            课程资料、工具包、项目模板和比赛素材集中在这里，登录并完成会员身份后即可查看。
          </p>
        </section>
        <ResourceLibrary />
      </main>
      <Footer />
    </div>
  )
}
