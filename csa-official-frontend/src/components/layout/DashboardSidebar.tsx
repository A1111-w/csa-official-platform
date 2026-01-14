"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { dashboardMenu } from "@/config/menu";
import { useAuthStore } from "@/store/useAuthStore";
import { useEffect, useState } from "react";

export function DashboardSidebar() {
  const pathname = usePathname();
  const { user } = useAuthStore();
  const [mounted, setMounted] = useState(false);

  // 避免服务端渲染不一致
  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  // 如果没登录，默认等级为 0
  const currentLevel = user?.roleLevel || 0;

  return (
    <div className="hidden border-r bg-slate-50/40 dark:bg-slate-900/40 lg:block w-64 min-h-screen">
      <div className="flex h-full max-h-screen flex-col gap-2">
        <div className="flex h-[60px] items-center border-b px-6">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl text-primary">
            <span>CSA 控制台</span>
          </Link>
        </div>
        <div className="flex-1 overflow-auto py-2">
          <nav className="grid items-start px-4 text-sm font-medium">
            {dashboardMenu.map((item, index) => {
              // 权限过滤
              if (currentLevel < item.minLevel) return null;

              const Icon = item.icon;
              const isActive = pathname === item.href;

              return (
                <Link
                  key={index}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2 transition-all hover:text-primary",
                    isActive 
                      ? "bg-slate-100 text-primary dark:bg-slate-800" 
                      : "text-muted-foreground hover:bg-slate-100 dark:hover:bg-slate-800"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {item.title}
                </Link>
              );
            })}
          </nav>
        </div>
      </div>
    </div>
  );
}