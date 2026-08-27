export const roleLabels: Record<number, string> = {
  0: "游客",
  1: "会员",
  2: "核心成员",
  3: "部长",
  4: "会长",
  99: "Root",
}

export function getRoleLabel(roleLevel?: number | null) {
  if (roleLevel == null) {
    return "访客"
  }

  return roleLabels[roleLevel] ?? `Level ${roleLevel}`
}

export function getRoleDescription(roleLevel?: number | null) {
  if (roleLevel == null) {
    return "登录后即可查看你在协会中的当前身份。"
  }

  if (roleLevel >= 99) {
    return "拥有全站管理与运维权限。"
  }

  if (roleLevel >= 4) {
    return "负责协会治理、成员任命与公开信息维护。"
  }

  if (roleLevel >= 3) {
    return "可以管理比赛、发起提案，并参与组织层协作。"
  }

  if (roleLevel >= 2) {
    return "可以提交简历、展示项目经历并参与核心协作。"
  }

  if (roleLevel >= 1) {
    return "可以访问资源库并参与成员活动。"
  }

  return "游客账号可浏览公开内容，完成升级后可进入更多功能区。"
}

export function hasRoleLevel(roleLevel: number | null | undefined, minimum: number) {
  return (roleLevel ?? -1) >= minimum
}
