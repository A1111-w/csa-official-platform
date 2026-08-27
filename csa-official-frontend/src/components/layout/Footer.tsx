import Link from "next/link"

export function Footer() {
  return (
    <footer className="border-t bg-muted/30 py-10 text-sm text-muted-foreground">
      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 px-4 md:grid-cols-3 md:px-8">
        <div className="space-y-3">
          <h3 className="text-base font-semibold text-foreground">CSA 计算机协会</h3>
          <p className="max-w-sm leading-7">
            连接资源、比赛、简历与组织协作，让协会沉淀真正被看见、被复用、被延续。
          </p>
        </div>

        <div className="space-y-3">
          <h3 className="text-base font-semibold text-foreground">站内入口</h3>
          <ul className="space-y-2">
            <li>
              <Link href="/resources" className="hover:text-primary">
                资源库
              </Link>
            </li>
            <li>
              <Link href="/competitions" className="hover:text-primary">
                比赛活动
              </Link>
            </li>
            <li>
              <Link href="/contributors" className="hover:text-primary">
                核心成员
              </Link>
            </li>
            <li>
              <Link href="/dashboard" className="hover:text-primary">
                控制台
              </Link>
            </li>
            <li>
              <Link href="/privacy" className="hover:text-primary">
                隐私说明
              </Link>
            </li>
          </ul>
        </div>

        <div className="space-y-3">
          <h3 className="text-base font-semibold text-foreground">联系</h3>
          <p>地点：综合实验楼 B304</p>
          <p>邮箱：contact@csa-official.com</p>
          <p className="leading-7">欢迎在协会活动、资源沉淀和项目协作里留下你的名字。</p>
        </div>
      </div>
      <div className="mx-auto mt-10 max-w-7xl border-t px-4 pt-6 text-center text-xs md:px-8">
        © 2026 CSA Official. All rights reserved.
      </div>
    </footer>
  )
}
