"use client";

import { useAuthStore } from "@/store/useAuthStore";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useEffect, useState } from "react";

export default function DashboardPage() {
  const { user } = useAuthStore();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold tracking-tight">概览</h2>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">当前身份</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">
               {user?.roleLevel === 99 ? "超级管理员" : `Level ${user?.roleLevel}`}
            </div>
            <p className="text-xs text-muted-foreground">
               {user?.username}
            </p>
          </CardContent>
        </Card>

        {/* 这里以后可以放待办事项、未读消息等 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">系统状态</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">运行正常</div>
            <p className="text-xs text-muted-foreground">
              所有服务已连接
            </p>
          </CardContent>
        </Card>
      </div>
      
      <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
        这里将来会展示你的近期活动、贡献图表或待办任务...
      </div>
    </div>
  );
}