import { DashboardGuard } from "@/components/layout/DashboardGuard"
import { DashboardSidebar } from "@/components/layout/DashboardSidebar"
import { Navbar } from "@/components/layout/Navbar"

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />

      <div className="flex min-h-[calc(100vh-4rem)] flex-1 flex-col lg:flex-row">
        <DashboardSidebar />

        <main className="min-w-0 flex-1 overflow-y-auto bg-muted/20 p-4 md:p-8">
          <DashboardGuard>{children}</DashboardGuard>
        </main>
      </div>
    </div>
  )
}
