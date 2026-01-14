import api from '@/lib/axios';

export interface CarouselItem {
  id: number;
  imgUrl: string;
  targetUrl: string;
  title: string;
}

export interface ContributorVo {
  id: number;
  realName: string;
  avatar: string;
  deptName: string;
  title: string;
  roleLevel: number;
}

export const publicService = {
  // 获取轮播图
  getCarousel: () => {
    return api.get<any, CarouselItem[]>('/api/public/carousel/list');
  },

  // 获取协会介绍
  getAbout: () => {
    return api.get<any, string>('/api/public/about');
  },

  // 获取贡献者/核心成员名单
  getContributors: () => {
    return api.get<any, ContributorVo[]>('/api/public/contributors');
  }
};