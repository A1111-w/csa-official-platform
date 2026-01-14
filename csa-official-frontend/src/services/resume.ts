// src/services/resume.ts
import api from '@/lib/axios';

// 对应后端的 Resume 实体
export interface ResumeData {
  id?: number;
  content: string;    // Markdown 文本
  gitRepoUrl: string; // Git 仓库链接
  status: number;     // 0:草稿, 1:待审核, 2:已通过, 3:已驳回
  rejectReason?: string;
  auditBy?: number;
  auditTime?: string;
}

// 对应后端的 ResumeStatusEnum
export const RESUME_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
};

export const resumeService = {
  // 获取我的简历
  getMyResume: () => {
    return api.get<any, ResumeData>('/api/resume/my');
  },

  // 保存简历 (只传 content 和 gitUrl)
  save: (data: { content: string; gitRepoUrl: string }) => {
    return api.post<any, string>('/api/resume/save', data);
  },

  // 提交审核
  submit: () => {
    return api.post<any, string>('/api/resume/submit');
  }
};