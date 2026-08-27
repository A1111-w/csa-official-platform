import api from "@/lib/axios"

export interface CarouselItem {
  id: number
  imgUrl: string
  targetUrl: string | null
  title: string
}

export interface ContributionRankItem {
  userId: number
  username: string | null
  realName: string | null
  avatar: string | null
  deptName: string | null
  score: number | string
  contributionCount: number
}

export interface ContributorVo {
  id: number
  realName: string
  avatar: string
  deptName: string
  title: string
  roleLevel: number
}

export interface PrivacyNotice {
  policyVersion: string
  collectedFields: string[]
  purposes: Record<string, string>
  retention: Record<string, string>
  userRights: string[]
  contactEmail: string
}

export const publicService = {
  getCarousel: () => {
    return api.get<CarouselItem[], CarouselItem[]>("/api/public/carousel/list")
  },

  getAbout: () => {
    return api.get<string, string>("/api/public/about")
  },

  getContributors: () => {
    return api.get<ContributorVo[], ContributorVo[]>("/api/public/contributors")
  },

  getContributionRank: (limit = 5) => {
    return api.get<ContributionRankItem[], ContributionRankItem[]>(
      "/api/public/contribution/rank",
      { params: { limit } }
    )
  },

  getPrivacyNotice: () => {
    return api.get<PrivacyNotice, PrivacyNotice>("/api/public/privacy", { skipAuthRedirect: true })
  },

  updateAbout: (content: string) => {
    return api.post<string, string>("/api/sys/config/update-about", { content })
  },
}
