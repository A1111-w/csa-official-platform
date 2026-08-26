"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { Menu } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { dashboardMenu } from "@/config/menu"
import { useIsClient } from "@/hooks/use-is-client"
import { getRoleLabel } from "@/lib/access"
import { cn } from "@/lib/utils"
import { useAuthStore } from "@/store/useAuthStore"

export function DashboardSidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const { user } = useAuthStore()
  const isClient = useIsClient()

  if (!isClient) return null

  const currentLevel = user?.roleLevel || 0
  const visibleItems = dashboardMenu.filter((item) => currentLevel >= item.minLevel)

  return (
    <>
      <div className="border-b bg-background px-4 py-3 lg:hidden">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" className="w-full justify-between">
              控制台导航
              <Menu className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-[calc(100vw-2rem)]">
            <DropdownMenuLabel>当前身份：{getRoleLabel(currentLevel)}</DropdownMenuLabel>
            {visibleItems.map((item) => {
              const Icon = item.icon

              return (
                <DropdownMenuItem key={item.href} onClick={() => router.push(item.href)}>
                  <Icon className="mr-2 h-4 w-4" />
                  {item.title}
                </DropdownMenuItem>
              )
            })}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <aside className="hidden w-64 shrink-0 border-r bg-muted/30 lg:block">
        <div className="sticky top-16 flex h-[calc(100vh-4rem)] flex-col gap-2">
          <div className="border-b px-6 py-5">
            <Link href="/dashboard" className="text-xl font-semibold tracking-tight text-primary">
              CSA 控制台
            </Link>
            <p className="mt-2 text-xs text-muted-foreground">
              当前身份：{getRoleLabel(currentLevel)}
            </p>
          </div>
          <div className="flex-1 overflow-auto py-2">
            <nav className="grid items-start gap-1 px-4 text-sm font-medium">
              {visibleItems.map((item) => {
                const Icon = item.icon
                const isActive = pathname === item.href

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2 transition-all hover:text-primary",
                      isActive
                        ? "bg-background text-primary shadow-sm"
                        : "text-muted-foreground hover:bg-background"
                    )}
                  >
                    <Icon className="h-4 w-4" />
                    {item.title}
                  </Link>
                )
              })}
            </nav>
          </div>
        </div>
      </aside>
    </>
  )
}
