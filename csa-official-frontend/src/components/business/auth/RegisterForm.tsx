"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { authService } from "@/services/auth";

// === 1. 定义校验规则 (与后端 RegisterDto 保持一致) ===
const registerSchema = z.object({
  username: z.string().min(4, "用户名至少4位").max(20, "用户名最多20位"),
  password: z
    .string()
    .regex(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/, "密码需包含字母和数字，长度6-20位"),
  confirmPassword: z.string(),
  email: z.string().email("邮箱格式不正确"),
  code: z.string().length(6, "验证码必须是6位"),
  realName: z.string().optional(),
  studentId: z.string().optional(),
  college: z.string().optional(),
  className: z.string().optional(),
  inviteCode: z.string().optional(),
  merchantNo: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "两次输入的密码不一致",
  path: ["confirmPassword"],
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  
  // 验证码倒计时状态
  const [countdown, setCountdown] = useState(0);
  const [sendingCode, setSendingCode] = useState(false);

  // 初始化表单
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      password: "",
      confirmPassword: "",
      email: "",
      code: "",
      inviteCode: "",
      merchantNo: "",
    },
  });

  // === 倒计时逻辑 ===
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (countdown > 0) {
      timer = setTimeout(() => setCountdown(countdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [countdown]);

  // === 发送验证码逻辑 ===
  const onSendCode = async () => {
    // 1. 获取邮箱值
    const email = form.getValues("email");
    
    // 2. 单独触发邮箱字段的校验
    const isEmailValid = await form.trigger("email");
    
    // 如果校验不通过，或者邮箱为空，直接拦截
    if (!isEmailValid || !email) {
      toast.error("请先输入有效的邮箱地址");
      return;
    }

    setSendingCode(true); // 按钮转圈圈
    try {
      // 3. 调用后端接口
      // 对应后端 AuthController.sendCode(@RequestParam String email)
      await authService.sendCode(email);
      
      // 4. 成功后：提示 + 开启倒计时
      toast.success("验证码已发送，请查收邮件");
      setCountdown(60); 

    } catch (error: any) {
      console.error("发送验证码失败:", error);
      
      // 5. 错误处理
      // 后端 MailService 会抛出 "请勿频繁发送验证码"
      // 后端 RateLimitAspect 也会抛出 "操作过于频繁..."
      const errorMsg = error.message || "发送失败，请稍后再试";
      toast.error(errorMsg);
      
      // 这里不设置倒计时，允许用户在解决问题（如输错邮箱）后立即重试
    } finally {
      setSendingCode(false); // 停止转圈圈
    }
  };

  // === 提交注册逻辑 ===
  async function onSubmit(values: RegisterFormValues) {
    setLoading(true);
    try {
      // 移除 confirmPassword，因为后端不需要
      const { confirmPassword, ...submitData } = values;
      
      await authService.register(submitData);
      
      toast.success("注册成功！即将跳转登录页...");
      
      // 延迟跳转，让用户看清提示
      setTimeout(() => {
        router.push("/login");
      }, 1500);

    } catch (error: any) {
      console.error("注册错误:", error);
      // 🌟 重点：处理业务报错
      // 如果后端返回 "用户名已存在" 或 "验证码错误"，会在这里捕获
      if (error.message && error.message.includes("用户名")) {
         form.setError("username", { message: error.message });
      } else if (error.message && error.message.includes("验证码")) {
         form.setError("code", { message: error.message });
      } else {
         toast.error(error.message || "注册失败，请稍后重试");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        
        {/* === 基础账号信息 === */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="username"
            render={({ field }) => (
              <FormItem>
                <FormLabel>用户名 <span className="text-red-500">*</span></FormLabel>
                <FormControl>
                  <Input placeholder="英文数字组合" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
           <FormField
            control={form.control}
            name="realName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>真实姓名</FormLabel>
                <FormControl>
                  <Input placeholder="方便联系（选填）" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>密码 <span className="text-red-500">*</span></FormLabel>
                <FormControl>
                  <Input type="password" placeholder="6-20位，含字母数字" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="confirmPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>确认密码 <span className="text-red-500">*</span></FormLabel>
                <FormControl>
                  <Input type="password" placeholder="再次输入密码" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* === 邮箱与验证码 (带按钮) === */}
        <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>邮箱 <span className="text-red-500">*</span></FormLabel>
                <FormControl>
                  <Input placeholder="example@qq.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <FormLabel>验证码 <span className="text-red-500">*</span></FormLabel>
              <div className="flex gap-2">
                <FormControl>
                  <Input placeholder="6位数字" maxLength={6} {...field} />
                </FormControl>
                
                {/* === 优化后的按钮组件 === */}
                <Button 
                  type="button" 
                  variant="outline" 
                  // 倒计时中 或 正在发送中 都要禁用
                  disabled={countdown > 0 || sendingCode}
                  onClick={onSendCode}
                  // w-[140px] 固定宽度，防止倒计时数字变化导致按钮忽长忽短
                  // transition-all 增加平滑感
                  className="w-[140px] transition-all"
                >
                  {/* 1. 发送中：显示转圈图标 */}
                  {sendingCode ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      发送中
                    </>
                  ) : countdown > 0 ? (
                    // 2. 倒计时中：显示剩余秒数
                    `${countdown}s 后重发`
                  ) : (
                    // 3. 正常状态
                    "发送验证码"
                  )}
                </Button>
              </div>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* === 注册类型切换 === */}
        <Tabs defaultValue="invite" className="w-full mt-4">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="invite">使用邀请码</TabsTrigger>
            <TabsTrigger value="guest">普通/付费注册</TabsTrigger>
          </TabsList>
          
          <TabsContent value="invite">
            <FormField
              control={form.control}
              name="inviteCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>邀请码</FormLabel>
                  <FormControl>
                    <Input placeholder="请输入社团分发的邀请码" {...field} />
                  </FormControl>
                  <FormDescription>拥有邀请码可直接升级为会员</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </TabsContent>
          
          <TabsContent value="guest">
             <div className="space-y-4">
                <FormField
                  control={form.control}
                  name="merchantNo"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>支付单号 (选填)</FormLabel>
                      <FormControl>
                        <Input placeholder="微信/支付宝支付单号" {...field} />
                      </FormControl>
                      <FormDescription>若已缴费，填写单号可快速审核，否则为路人身份</FormDescription>
                    </FormItem>
                  )}
                />
                <div className="grid grid-cols-3 gap-2">
                   <FormField control={form.control} name="college" render={({field}) => (
                      <FormItem><FormControl><Input placeholder="学院" {...field} /></FormControl></FormItem>
                   )} />
                   <FormField control={form.control} name="className" render={({field}) => (
                      <FormItem><FormControl><Input placeholder="班级" {...field} /></FormControl></FormItem>
                   )} />
                   <FormField control={form.control} name="studentId" render={({field}) => (
                      <FormItem><FormControl><Input placeholder="学号" {...field} /></FormControl></FormItem>
                   )} />
                </div>
             </div>
          </TabsContent>
        </Tabs>

        <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 mt-6" disabled={loading}>
          {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          立即注册
        </Button>
      </form>
    </Form>
  );
}