"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { 
  Loader2, 
  Save, 
  Send, 
  GitBranch, 
  FileText, 
  AlertCircle, 
  CheckCircle2,
  Terminal
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
// 👇 引入正版 Alert 组件
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { resumeService, ResumeData, RESUME_STATUS } from "@/services/resume";

export default function ResumePage() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [resume, setResume] = useState<ResumeData | null>(null);

  // 表单状态
  const [gitUrl, setGitUrl] = useState("");
  const [content, setContent] = useState("");

  // 初始化加载
  useEffect(() => {
    loadData();
  }, []);

  // 校验 Git URL 是否合法
  const validateGitUrl = (url: string) => {
    if (!url) return true; // 允许为空（如果允许不填的话）
    // 简单正则：必须是 http 或 https 开头，且包含 .
    const regex = /^(http|https):\/\/[^ "]+$/;
    return regex.test(url);
  };

  const loadData = async () => {
    try {
      const data = await resumeService.getMyResume();
      if (data) {
        setResume(data);
        setGitUrl(data.gitRepoUrl || "");
        setContent(data.content || "");
      }
    } catch (error: any) {
      // 这里的 error 可能是 403 (权限不足) 或 404 (没创建)
      // 如果是权限不足，axios 拦截器可能已经跳登录或弹窗了，这里主要处理数据加载逻辑
      console.error("加载简历失败", error);
    } finally {
      setLoading(false);
    }
  };

  // 保存草稿
  const handleSave = async () => {
    // 1. 校验 URL 格式
    if (gitUrl && !validateGitUrl(gitUrl)) {
      toast.error("Git 链接格式不正确，请以 http:// 或 https:// 开头");
      return;
    }

    if (!gitUrl && !content) {
      toast.error("请至少填写一项内容");
      return;
    }

    setSubmitting(true);
    try {
      await resumeService.save({
        content: content,
        gitRepoUrl: gitUrl
      });
      toast.success("保存成功");
      loadData(); 
    } catch (error: any) {
      toast.error(error.message || "保存失败");
    } finally {
      setSubmitting(false);
    }
  };

  // 提交审核
  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      await resumeService.save({ content, gitRepoUrl: gitUrl });
      await resumeService.submit();
      
      toast.success("已提交审核，请耐心等待部长批阅");
      loadData();
    } catch (error: any) {
      toast.error(error.message || "提交失败");
    } finally {
      setSubmitting(false);
    }
  };

  // 渲染状态标签 (小徽章)
  const renderStatus = (status: number) => {
    switch (status) {
      case RESUME_STATUS.DRAFT:
        return <span className="px-2 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-bold border">草稿</span>;
      case RESUME_STATUS.PENDING:
        return <span className="px-2 py-1 rounded-full bg-yellow-100 text-yellow-700 text-xs font-bold flex items-center gap-1 border border-yellow-200"><Loader2 className="w-3 h-3 animate-spin"/> 审核中</span>;
      case RESUME_STATUS.APPROVED:
        return <span className="px-2 py-1 rounded-full bg-green-100 text-green-700 text-xs font-bold flex items-center gap-1 border border-green-200"><CheckCircle2 className="w-3 h-3"/> 已通过</span>;
      case RESUME_STATUS.REJECTED:
        return <span className="px-2 py-1 rounded-full bg-red-100 text-red-700 text-xs font-bold flex items-center gap-1 border border-red-200"><AlertCircle className="w-3 h-3"/> 已驳回</span>;
      default:
        return null;
    }
  };

  if (loading) {
    return <div className="p-10 flex justify-center"><Loader2 className="h-8 w-8 animate-spin text-muted-foreground" /></div>;
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto animate-in fade-in duration-500">
      
      {/* 顶部标题区 */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">我的简历</h2>
          <p className="text-muted-foreground mt-1">展示你的技术实力与项目经验</p>
        </div>
        <div className="flex items-center gap-3">
            {resume && renderStatus(resume.status)}
        </div>
      </div>

      {/* === 状态提示区 (使用 Alert 组件) === */}

      {/* 1. 驳回提示 */}
      {resume?.status === RESUME_STATUS.REJECTED && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>审核未通过</AlertTitle>
          <AlertDescription>
            驳回原因：{resume.rejectReason || "未填写原因，请联系管理员"}
            <br/>请修改后重新提交。
          </AlertDescription>
        </Alert>
      )}

      {/* 2. 通过提示 */}
      {resume?.status === RESUME_STATUS.APPROVED && (
        <Alert className="border-green-500/50 text-green-600 bg-green-50 dark:bg-green-900/10 dark:text-green-400">
          <CheckCircle2 className="h-4 w-4" />
          <AlertTitle>审核已通过</AlertTitle>
          <AlertDescription>
            恭喜！你的简历已归档。如果现在【保存】修改，状态将重置为【待审核】，需重新排队。
          </AlertDescription>
        </Alert>
      )}

      {/* 3. 待审核提示 (可选，为了更加明显) */}
      {resume?.status === RESUME_STATUS.PENDING && (
        <Alert className="bg-yellow-50 dark:bg-yellow-900/10 border-yellow-200 dark:border-yellow-800">
          <Terminal className="h-4 w-4 text-yellow-600" />
          <AlertTitle className="text-yellow-700 dark:text-yellow-500">正在审核中</AlertTitle>
          <AlertDescription className="text-yellow-600 dark:text-yellow-400">
            部长正在仔细阅读你的代码，请耐心等待。此时提交会被锁定。
          </AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader>
          <CardTitle>简历详情</CardTitle>
          <CardDescription>
            支持 Markdown 格式。建议包含：个人简介、技术栈 (Java/Next.js/Docker...)、项目经历等。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          
         {/* Git 链接模块 */}
          <div className="space-y-3">
            <Label htmlFor="gitUrl" className="flex items-center gap-2">
              <GitBranch className="h-4 w-4" /> 
              代码仓库链接
            </Label>
            
            <div className="flex gap-2">
              <Input 
                id="gitUrl" 
                placeholder="例如: https://github.com/yourname/project" 
                value={gitUrl}
                onChange={(e) => setGitUrl(e.target.value)}
                disabled={resume?.status === RESUME_STATUS.PENDING}
                // 如果格式不对，显示红色边框
                className={gitUrl && !validateGitUrl(gitUrl) ? "border-red-500 focus-visible:ring-red-500" : ""}
              />
              {/* 这里的按钮用于让用户自己点一下，看看能不能打开 */}
              {gitUrl && validateGitUrl(gitUrl) && (
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  title="测试链接能否访问"
                  onClick={() => window.open(gitUrl, '_blank')}
                >
                  <Send className="h-4 w-4 -rotate-45" /> {/* 模拟跳转图标 */}
                </Button>
              )}
            </div>

            {/* 错误提示 */}
            {gitUrl && !validateGitUrl(gitUrl) && (
              <p className="text-xs text-red-500">
                链接格式错误，请输入完整的 HTTP/HTTPS 网址。
              </p>
            )}

            {/* 友情提示：防坑关键点 */}
            <div className="bg-slate-100 dark:bg-slate-800 p-3 rounded-md text-xs text-muted-foreground space-y-1">
               <p className="font-bold flex items-center gap-1">
                 ⚠️ 重要提示：
               </p>
               <ul className="list-disc list-inside space-y-1 ml-1">
                 <li>请确保你的仓库是 <strong>Public (公开)</strong> 的，否则面试官无法访问。</li>
                 <li>推荐提交完整的项目代码，而不仅仅是 README。</li>
                 <li>如果是 SSH 地址 (git@...) 请改为 HTTPS 地址。</li>
               </ul>
            </div>
          </div>

          {/* Markdown 内容 */}
          <div className="space-y-2">
            <Label htmlFor="content" className="flex items-center gap-2">
              <FileText className="h-4 w-4" /> 详细介绍 (Markdown)
            </Label>
            <textarea
              id="content"
              // 手写样式模仿 Shadcn Input
              className="flex min-h-[300px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 font-mono leading-relaxed"
              placeholder="# 个人简介&#10;- 熟悉 Java Spring Boot&#10;- 熟悉 Next.js&#10;&#10;# 项目经验&#10;..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              // 审核中禁止修改
              disabled={resume?.status === RESUME_STATUS.PENDING}
            />
          </div>

        </CardContent>
        <CardFooter className="flex justify-between border-t p-6 bg-slate-50/50 dark:bg-slate-900/20">
            <div className="text-xs text-muted-foreground">
               {resume?.updateTime ? `上次保存: ${resume.updateTime.replace("T", " ")}` : "暂未保存"}
            </div>
            <div className="flex gap-3">
               <Button 
                variant="outline" 
                onClick={handleSave} 
                disabled={submitting || resume?.status === RESUME_STATUS.PENDING}
               >
                 <Save className="mr-2 h-4 w-4" />
                 仅保存
               </Button>
               <Button 
                onClick={handleSubmit} 
                disabled={submitting || resume?.status === RESUME_STATUS.PENDING}
               >
                 {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                 <Send className="mr-2 h-4 w-4" />
                 提交审核
               </Button>
            </div>
        </CardFooter>
      </Card>
    </div>
  );
}