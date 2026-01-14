import axios from 'axios';

// 读取环境变量，默认连本地 8080
const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL,
  timeout: 30000, // 10秒超时
  headers: {
    'Content-Type': 'application/json',
  },
});

// 1. 请求拦截器：带上 Token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 2. 响应拦截器：处理数据剥离和 401 跳转
api.interceptors.response.use(
  (response) => {
    const res = response.data;
    
    // === 修改开始：自动剥离 R 结构 ===
    // 判断是否为后端标准返回结构 (包含 code 字段)
    if (res && typeof res.code === 'number') {
        // 200 表示业务成功，直接返回 data
        if (res.code === 200) {
            return res.data; 
        }
        // 非 200 (如 500 密码错误)，视为错误，抛出异常进入 catch
        return Promise.reject(new Error(res.message || 'System Error'));
    }
    // === 修改结束 ===

    // 如果不是 R 结构 (比如文件流下载)，直接返回
    return res;
  },
  (error) => {
    if (error.response) {
      // 401 未登录 -> 踢回登录页
      if (error.response.status === 401) {
        if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
          localStorage.removeItem('token');
          // 记录当前页面，登录后跳回来 (可选)
          const currentPath = window.location.pathname;
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
        }
      }
      // 尝试返回后端传回的具体错误信息
      const backendMsg = error.response.data?.message;
      if (backendMsg) {
          return Promise.reject(new Error(backendMsg));
      }
      return Promise.reject(error.response.data);
    }
    return Promise.reject(error);
  }
);

export default api;