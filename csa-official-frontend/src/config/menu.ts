import { 
  LayoutDashboard, 
  FileText, 
  Trophy, 
  Vote, 
  Users, 
  Settings, 
  HardDrive,
  UserCircle
} from "lucide-react";

export interface MenuItem {
  title: string;
  href: string;
  icon: any;
  minLevel: number; // 最低可见等级
}

export const dashboardMenu: MenuItem[] = [
  {
    title: "概览",
    href: "/dashboard",
    icon: LayoutDashboard,
    minLevel: 0, // 所有人可见
  },
  {
    title: "个人资料",
    href: "/dashboard/profile", // 原定 /profile，建议统一归到 dashboard 下管理
    icon: UserCircle,
    minLevel: 0,
  },
  {
    title: "资源下载",
    href: "/dashboard/resources", 
    icon: HardDrive,
    minLevel: 1, // 会员可见
  },
  {
    title: "我的简历",
    href: "/dashboard/resume",
    icon: FileText,
    minLevel: 2, // 核心成员可见
  },
  {
    title: "比赛管理",
    href: "/dashboard/competitions",
    icon: Trophy,
    minLevel: 3, // 部长可见
  },
  {
    title: "提案投票",
    href: "/dashboard/vote",
    icon: Vote,
    minLevel: 3, // 部长可见
  },
  {
    title: "部门人事",
    href: "/dashboard/dept",
    icon: Users,
    minLevel: 3, // 部长可见
  },
  {
    title: "系统设置",
    href: "/dashboard/settings",
    icon: Settings,
    minLevel: 4, // 会长可见
  },
];