import api from "@/lib/axios"

export interface ProposalItem {
  id: number
  type: string
  title: string
  reason: string
  proposerId: number
  status: number
  expireTime: string | null
  finalResultJson: string | null
  createTime: string | null
  updateTime: string | null
}

export interface CreateProposalParams {
  type: string
  title: string
  reason: string
}

export interface SubmitVoteParams {
  proposalId: number
  agree: boolean
  comment?: string
}

export const voteService = {
  list: () => api.get<ProposalItem[], ProposalItem[]>("/api/sys/vote/list"),
  create: (data: CreateProposalParams) =>
    api.post<ProposalItem, ProposalItem>("/api/sys/vote/create", data),
  submit: (data: SubmitVoteParams) =>
    api.post<string, string>("/api/sys/vote/submit", data),
}
