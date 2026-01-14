export function Footer() {
  return (
    <footer className="border-t bg-slate-50 dark:bg-slate-900 py-10 text-slate-600 dark:text-slate-400">
      <div className="container mx-auto max-w-7xl px-4 md:px-8 grid grid-cols-1 md:grid-cols-3 gap-8 text-sm">
        
        {/* About */}
        <div className="space-y-3">
          <h3 className="font-bold text-foreground text-lg">CSA 计算机协会</h3>
          <p className="leading-relaxed">
            致力于打造校园内最硬核的技术交流平台。
            <br />
            资源共享 · 技术竞技 · 极客文化
          </p>
        </div>

        {/* Links */}
        <div className="space-y-3">
          <h3 className="font-bold text-foreground text-lg">传送门</h3>
          <ul className="space-y-2">
            <li><a href="#" className="hover:text-primary hover:underline">开源仓库 (Gitea)</a></li>
            <li><a href="#" className="hover:text-primary hover:underline">加入 微信 群</a></li>
            <li><a href="#" className="hover:text-primary hover:underline">常见问题</a></li>
          </ul>
        </div>

        {/* Contact */}
        <div className="space-y-3">
          <h3 className="font-bold text-foreground text-lg">联系我们</h3>
          <p>地点：综合实验楼 B304 (示例)</p>
          <p>邮箱：contact@csa-official.com</p>
        </div>
      </div>
      <div className="container mx-auto max-w-7xl px-4 mt-10 pt-6 border-t text-center text-xs text-muted-foreground">
        © 2025 CSA Official. All rights reserved.
      </div>
    </footer>
  );
}