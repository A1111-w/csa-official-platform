"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { publicService, ContributorVo, CarouselItem } from "@/services/public";
import { ArrowRight, Code, Database, Trophy, Users } from "lucide-react";

export default function Home() {
  const [about, setAbout] = useState<string>("");
  const [contributors, setContributors] = useState<ContributorVo[]>([]);
  // const [carousel, setCarousel] = useState<CarouselItem[]>([]); // 暂时先不渲染轮播图，用 Hero 替代
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const [aboutData, contributorsData] = await Promise.all([
          publicService.getAbout(),
          publicService.getContributors()
        ]);
        setAbout(aboutData || "暂无介绍");
        setContributors(contributorsData || []);
      } catch (error) {
        console.error("Fetch home data failed", error);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  return (
    <div className="min-h-screen flex flex-col font-sans">
      <Navbar />

      <main className="flex-1">
        {/* 1. Hero Section - 视觉冲击区 */}
        <section className="relative py-20 md:py-32 bg-gradient-to-b from-blue-50 to-white dark:from-slate-900 dark:to-background overflow-hidden">
          <div className="container px-4 md:px-8 mx-auto max-w-7xl text-center relative z-10">
            <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100 mb-6">
              连接每一位 <span className="text-blue-600">开发者</span>
            </h1>
            <p className="text-lg md:text-xl text-slate-600 dark:text-slate-400 mb-8 max-w-2xl mx-auto leading-relaxed">
              广州华立学院计算机协会官方平台。在这里，你可以分享资源、参与竞技、提交代码，与志同道合的伙伴共同成长。
            </p>
            <div className="flex justify-center gap-4">
              <Link href="/register">
                <Button size="lg" className="rounded-full px-8 text-base">
                  立即加入 <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
              <Link href="/about">
                <Button variant="outline" size="lg" className="rounded-full px-8 text-base">
                  了解更多
                </Button>
              </Link>
            </div>
          </div>
          {/* 背景装饰 */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full opacity-10 pointer-events-none">
             <div className="absolute inset-0 bg-[url('/grid.svg')] bg-center [mask-image:linear-gradient(180deg,white,rgba(255,255,255,0))]"></div>
          </div>
        </section>

        {/* 2. Features - 功能特区 */}
        <section className="py-20 bg-white dark:bg-background">
          <div className="container px-4 md:px-8 mx-auto max-w-7xl">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <FeatureCard 
                icon={<Database className="h-10 w-10 text-blue-500" />}
                title="海量资源"
                desc="汇聚历届学长学姐的学习资料、软件工具与实战项目，等级越高，权限越大。"
              />
              <FeatureCard 
                icon={<Trophy className="h-10 w-10 text-yellow-500" />}
                title="技术竞赛"
                desc="定期举办编程比赛、黑客马拉松，提供丰厚奖品与核心成员晋升通道。"
              />
              <FeatureCard 
                icon={<Code className="h-10 w-10 text-green-500" />}
                title="简历托管"
                desc="支持 Markdown 在线编辑与 Git 仓库绑定，提供专业的简历审核与指导。"
              />
            </div>
          </div>
        </section>

        {/* 3. Introduction - 协会介绍 (从后端加载) */}
        <section className="py-20 bg-slate-50 dark:bg-slate-900/50">
          <div className="container px-4 md:px-8 mx-auto max-w-5xl">
            <div className="text-center mb-12">
              <h2 className="text-3xl font-bold mb-4">关于 CSA</h2>
              <div className="h-1 w-20 bg-blue-600 mx-auto rounded-full"></div>
            </div>
            <div className="prose prose-lg dark:prose-invert mx-auto bg-white dark:bg-slate-800 p-8 md:p-12 rounded-2xl shadow-sm">
              {loading ? (
                <div className="space-y-4">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-5/6" />
                  <Skeleton className="h-4 w-full" />
                </div>
              ) : (
                <div dangerouslySetInnerHTML={{ __html: about }} className="whitespace-pre-wrap leading-relaxed" />
              )}
            </div>
          </div>
        </section>

        {/* 4. Contributors - 核心成员/贡献墙 */}
        <section className="py-20 bg-white dark:bg-background">
          <div className="container px-4 md:px-8 mx-auto max-w-7xl">
            <div className="flex flex-col md:flex-row justify-between items-end mb-10 gap-4">
               <div>
                  <h2 className="text-3xl font-bold mb-2">核心成员</h2>
                  <p className="text-muted-foreground">这些大佬正在为社团提供技术与运营支持</p>
               </div>
               <Link href="/contributors">
                  <Button variant="ghost" className="group">
                    查看完整榜单 <ArrowRight className="ml-1 h-4 w-4 transition-transform group-hover:translate-x-1" />
                  </Button>
               </Link>
            </div>
            
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="flex flex-col items-center space-y-3">
                    <Skeleton className="h-24 w-24 rounded-full" />
                    <Skeleton className="h-4 w-20" />
                    <Skeleton className="h-3 w-16" />
                  </div>
                ))
              ) : (
                contributors.slice(0, 10).map((member) => (
                  <Card key={member.id} className="border-0 shadow-none hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
                    <CardContent className="flex flex-col items-center p-6 text-center">
                      <Avatar className="h-20 w-20 mb-4 border-2 border-slate-100">
                        <AvatarImage src={member.avatar} />
                        <AvatarFallback className="text-lg bg-blue-100 text-blue-600">
                          {member.realName ? member.realName.substring(0, 1) : "U"}
                        </AvatarFallback>
                      </Avatar>
                      <h3 className="font-semibold text-gray-900 dark:text-gray-100">{member.realName || "神秘成员"}</h3>
                      <p className="text-xs text-blue-600 font-medium mt-1 mb-1">{member.title}</p>
                      <p className="text-xs text-muted-foreground">{member.deptName || "核心组"}</p>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

// 简单的功能卡片组件
function FeatureCard({ icon, title, desc }: { icon: React.ReactNode, title: string, desc: string }) {
  return (
    <div className="group p-8 rounded-2xl border bg-card text-card-foreground shadow-sm hover:shadow-md transition-all hover:-translate-y-1">
      <div className="mb-4 inline-flex p-3 rounded-xl bg-slate-50 dark:bg-slate-800 group-hover:bg-blue-50 dark:group-hover:bg-blue-900/20 transition-colors">
        {icon}
      </div>
      <h3 className="text-xl font-bold mb-3">{title}</h3>
      <p className="text-muted-foreground leading-relaxed">
        {desc}
      </p>
    </div>
  );
}