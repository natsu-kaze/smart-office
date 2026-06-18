export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

export type ApiId = string | number

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthUser {
  id: ApiId
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  roles: string[]
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface UserItem {
  id: ApiId
  username: string
  realName: string
  phone?: string
  email?: string
  status: number
  roleNames?: string[]
}

export interface DepartmentNode {
  id: ApiId
  parentId: ApiId
  departmentCode: string
  departmentName: string
  leaderName?: string
  status: number
  children?: DepartmentNode[]
}

export interface ApprovalItem {
  id: ApiId
  approvalType: string
  title: string
  applicantUserId: ApiId
  applicantName: string
  applicantDeptName?: string
  content?: string
  amount?: number
  status: string
  currentApproverId?: ApiId
  currentApproverName?: string
  submittedAt?: string
  completedAt?: string
}

export interface AttendanceToday {
  checkInTime?: string
  checkOutTime?: string
  checkInStatus?: string
  checkOutStatus?: string
}

export interface MessageNotice {
  id: ApiId
  title: string
  content: string
  businessType?: string
  businessId?: ApiId
  readStatus: number
  readTime?: string
  createTime?: string
}

export interface MessageTodo {
  id: ApiId
  title: string
  businessType: string
  businessId: ApiId
  status: string
  dueTime?: string
  completedTime?: string
  createTime?: string
}

export interface FileRecord {
  id: ApiId
  originalName: string
  storageName: string
  bucket: string
  objectKey: string
  contentType?: string
  size: number
  url?: string
  uploaderId?: ApiId
  businessType?: string
  businessId?: ApiId
}
