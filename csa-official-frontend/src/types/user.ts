export interface AuthUser {
  username: string
  roleLevel: number
}

export interface UserInfo extends AuthUser {
  id: number
  realName: string | null
  avatar: string | null
  email: string | null
  phone: string | null
  contact: string | null
  positionType: number | null
  balance: number | string | null
  college: string | null
  className: string | null
  studentId: string | null
}
