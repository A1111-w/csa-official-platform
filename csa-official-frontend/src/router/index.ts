import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { dashboardMenu } from '@/config/menu'
import { sessionGuard } from './guard'

const dashboardViews = {
  '/dashboard': () => import('@/views/DashboardView.vue'),
  '/dashboard/profile': () => import('@/views/admin/ProfileView.vue'),
  '/dashboard/resources': () => import('@/views/business/ResourcesView.vue'),
  '/dashboard/ai': () => import('@/views/admin/AgentView.vue'),
  '/dashboard/resume': () => import('@/views/business/ResumeView.vue'),
  '/dashboard/resume-reviews': () => import('@/views/business/ResumeReviewsView.vue'),
  '/dashboard/competitions': () => import('@/views/business/CompetitionsView.vue'),
  '/dashboard/vote': () => import('@/views/business/VoteView.vue'),
  '/dashboard/departments': () => import('@/views/business/DepartmentsView.vue'),
  '/dashboard/audit': () => import('@/views/admin/AuditView.vue'),
  '/dashboard/contributions': () => import('@/views/admin/ContributionsView.vue'),
  '/dashboard/member-export': () => import('@/views/admin/MemberExportView.vue'),
  '/dashboard/carousels': () => import('@/views/admin/CarouselsView.vue'),
  '/dashboard/settings': () => import('@/views/admin/SettingsView.vue'),
}

export const routes: RouteRecordRaw[] = [
  { path: '/', component: () => import('@/views/public/HomeView.vue'), meta: { title: '首页' } },
  { path: '/about', component: () => import('@/views/public/AboutView.vue'), meta: { title: '关于协会' } },
  { path: '/contributors', component: () => import('@/views/public/ContributorsView.vue'), meta: { title: '贡献者' } },
  { path: '/privacy', component: () => import('@/views/public/PrivacyView.vue'), meta: { title: '隐私政策' } },
  { path: '/resources', component: () => import('@/views/business/ResourcesView.vue'), meta: { title: '资源库' } },
  { path: '/competitions', component: () => import('@/views/business/CompetitionsView.vue'), meta: { title: '竞赛活动' } },
  ...['login', 'register', 'forgot-password', 'reset-password'].map(path => ({
    path: '/' + path, component: () => import('@/views/auth/AuthView.vue'),
    meta: { title: ({ login: '登录', register: '注册', 'forgot-password': '找回密码', 'reset-password': '重置密码' } as Record<string,string>)[path] },
  })),
  ...dashboardMenu.map(item => ({
    path: item.href, component: dashboardViews[item.href as keyof typeof dashboardViews],
    meta: { title: item.title, requiresAuth: true, minLevel: item.minLevel },
  })),
  { path: '/forbidden', component: () => import('@/views/StatusView.vue'), props: { kind: 'forbidden' }, meta: { title: '权限不足' } },
  { path: '/session-error', component: () => import('@/views/StatusView.vue'), props: { kind: 'session' }, meta: { title: '会话检查失败' } },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/StatusView.vue'), props: { kind: 'missing' }, meta: { title: '页面未找到' } },
]
const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
router.beforeEach(sessionGuard)
router.afterEach(to => { document.title = String(to.meta.title || '官网') + ' · CSA 计算机协会' })
export default router
