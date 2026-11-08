export interface MenuItem { title: string; href: string; minLevel: number; description: string }
export const dashboardMenu: MenuItem[] = [
  { title: '概览', href: '/dashboard', minLevel: 0, description: '查看账号身份与工作区入口' },
  { title: '个人资料', href: '/dashboard/profile', minLevel: 0, description: '查看个人资料与成员信息' },
  { title: '资源库', href: '/dashboard/resources', minLevel: 1, description: '查找、分享与维护学习资源' },
  { title: 'Agent 实验室', href: '/dashboard/ai', minLevel: 1, description: '知识问答、调用轨迹与规则评测' },
  { title: '我的简历', href: '/dashboard/resume', minLevel: 2, description: '编辑简历并提交部门审核' },
  { title: '简历审核', href: '/dashboard/resume-reviews', minLevel: 3, description: '处理成员简历审核队列' },
  { title: '比赛看板', href: '/dashboard/competitions', minLevel: 3, description: '管理比赛、活动与协作成员' },
  { title: '提案中心', href: '/dashboard/vote', minLevel: 3, description: '发起议题并参与组织投票' },
  { title: '部门人事', href: '/dashboard/departments', minLevel: 4, description: '维护部门与负责人任命' },
  { title: '审计日志', href: '/dashboard/audit', minLevel: 4, description: '查看账号安全与管理操作记录' },
  { title: '贡献记录', href: '/dashboard/contributions', minLevel: 4, description: '查询和补录成员贡献' },
  { title: '成员导出', href: '/dashboard/member-export', minLevel: 4, description: '按字段和条件导出成员资料' },
  { title: '轮播管理', href: '/dashboard/carousels', minLevel: 4, description: '维护首页展示图、顺序与链接' },
  { title: '公开设置', href: '/dashboard/settings', minLevel: 0, description: '账号安全与协会介绍维护' },
]
