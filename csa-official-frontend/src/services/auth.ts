import api from '@/lib/axios';

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  roleLevel: number;
}

// 新增：注册参数类型 (对应后端的 RegisterDto)
export interface RegisterParams {
  username: string;
  password: string;
  email: string;
  code: string; // 验证码
  realName?: string;
  studentId?: string;
  college?: string;
  className?: string;
  inviteCode?: string; // 选填
  merchantNo?: string; // 选填
}

export const authService = {
  // 登录
  login: (data: LoginParams) => {
    return api.post<any, LoginResponse>('/api/auth/login', data);
  },

  // 发送验证码 (注意：后端是 @RequestParam，所以用 params 传参)
  sendCode: (email: string) => {
    return api.post<any, string>('/api/auth/send-code', null, { params: { email } });
  },

  // 注册
  register: (data: RegisterParams) => {
    return api.post<any, string>('/api/auth/register', data);
  }
};