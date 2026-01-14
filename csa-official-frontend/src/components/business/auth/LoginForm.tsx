"use client"; 

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner"; 

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { authService } from "@/services/auth";
import { useAuthStore } from "@/store/useAuthStore";

const formSchema = z.object({
  username: z.string().min(1, "请输入用户名"),
  password: z.string().min(1, "请输入密码"),
});

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setLogin = useAuthStore((state) => state.setLogin);
  const [loading, setLoading] = useState(false);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  async function onSubmit(values: z.infer<typeof formSchema>) {
    setLoading(true);
    try {
      const data = await authService.login(values);
      
      setLogin(data.token, { 
        username: data.username, 
        roleLevel: data.roleLevel 
      });

      toast.success(`欢迎回来, ${data.username}!`);
      
      const redirect = searchParams.get("redirect");
      if (redirect) {
        router.push(redirect);
      } else {
        router.push("/"); 
      }
      
    } catch (error: any) {
      // === 核心修改点：精细化错误处理 ===
      const msg = error.message || "登录失败";

      if (msg.includes("账号或密码错误")) {
        // 1. 如果是密码错，直接在表单密码框下报红
        form.setError("password", { 
          type: "manual", 
          message: "账号或密码错误，请检查" 
        });
        // 2. 清空密码框
        form.setValue("password", "");
      } else {
        // 3. 其他错误（如网络断了），还是弹窗提示
        toast.error(msg);
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>账号</FormLabel>
              <FormControl>
                <Input placeholder="请输入用户名" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>密码</FormLabel>
              <FormControl>
                <Input type="password" placeholder="请输入密码" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={loading}>
          {loading ? "登录中..." : "立即登录"}
        </Button>
      </form>
    </Form>
  );
}