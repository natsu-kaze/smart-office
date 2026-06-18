export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

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
  id: number
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
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  status: number
  roleNames?: string[]
}

export interface DepartmentNode {
  id: number
  parentId: number
  departmentCode: string
  departmentName: string
  leaderName?: string
  status: number
  children?: DepartmentNode[]
}

export interface ApprovalItem {
  id: number
  approvalType: string
  title: string
  applicantUserId: number
  applicantName: string
  applicantDeptName?: string
  content?: string
  amount?: number
  status: string
  currentApproverId?: number
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
  id: number
  title: string
  content: string
  businessType?: string
  businessId?: number
  readStatus: number
  readTime?: string
  createTime?: string
}

export interface MessageTodo {
  id: number
  title: string
  businessType: string
  businessId: number
  status: string
  dueTime?: string
  completedTime?: string
  createTime?: string
}

export interface FileRecord {
  id: number
  originalName: string
  storageName: string
  bucket: string
  objectKey: string
  contentType?: string
  size: number
  url?: string
  uploaderId?: number
  businessType?: string
  businessId?: number
}
