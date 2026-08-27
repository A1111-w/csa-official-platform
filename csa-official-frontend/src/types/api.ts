export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  errorCode?: string | null
  traceId?: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export class ApiError extends Error {
  status?: number
  code?: number
  errorCode?: string
  traceId?: string
  details?: unknown

  constructor(options: {
    message: string
    status?: number
    code?: number
    errorCode?: string
    traceId?: string
    details?: unknown
  }) {
    super(options.message)
    this.name = "ApiError"
    this.status = options.status
    this.code = options.code
    this.errorCode = options.errorCode
    this.traceId = options.traceId
    this.details = options.details
  }
}
