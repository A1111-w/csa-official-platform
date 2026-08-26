import type { LucideIcon } from "lucide-react"
import {
  ClipboardCheck,
  FileText,
  HardDrive,
  LayoutDashboard,
  ScrollText,
  Settings,
  Trophy,
  UserCircle,
  Users2,
  Vote,
} from "lucide-react"

export interface MenuItem {
  title: string
  href: string
  icon: LucideIcon
  minLevel: number
}

export const dashboardMenu: MenuItem[] = [
  {
    title: "概览",
    href: "/dashboard",
    icon: LayoutDashboard,
    minLevel: 0,
  },
  {
    title: "个人资料",
    href: "/dashboard/profile",
    icon: UserCircle,
    minLevel: 0,
  },
  {
    title: "资源库",
    href: "/dashboard/resources",
    icon: HardDrive,
    minLevel: 1,
  },
  {
    title: "我的简历",
    href: "/dashboard/resume",
    icon: FileText,
    minLevel: 2,
  },
  {
    title: "简历审核",
    href: "/dashboard/resume-reviews",
    icon: ClipboardCheck,
    minLevel: 3,
  },
  {
    title: "比赛看板",
    href: "/dashboard/competitions",
    icon: Trophy,
    minLevel: 3,
  },
  {
    title: "提案中心",
    href: "/dashboard/vote",
    icon: Vote,
    minLevel: 3,
  },
  {
    title: "部门人事",
    href: "/dashboard/departments",
    icon: Users2,
    minLevel: 4,
  },
  {
    title: "审计日志",
    href: "/dashboard/audit",
    icon: ScrollText,
    minLevel: 4,
  },
  {
    title: "公开设置",
    href: "/dashboard/settings",
    icon: Settings,
    minLevel: 0,
  },
]
