import { DashboardSidebar } from "@/components/layout/DashboardSidebar";
import { Navbar } from "@/components/layout/Navbar"; 

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col">
      {/* 这里我复用了 Navbar，你也可以去掉它，只留 Sidebar。
         但为了方便用户随时回首页或退出登录，保留 Navbar 是个不错的选择。
      */}
      <Navbar /> 
      
      <div className="flex flex-1">
        {/* 左侧侧边栏 */}
        <DashboardSidebar />
        
        {/* 右侧内容区域 */}
        <main className="flex-1 p-6 md:p-8 overflow-y-auto bg-slate-50/50 dark:bg-black">
          {children}
        </main>
      </div>
    </div>
  );
}