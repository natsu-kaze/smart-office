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
  permissions?: string[]
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
  avatar?: string
  status: number
  roleIds?: ApiId[]
  roleCodes?: string[]
  roleNames?: string[]
}

export interface RoleItem {
  id: ApiId
  roleCode: string
  roleName: string
  sort?: number
  status: number
  remark?: string
  menuIds?: ApiId[]
}

export interface RoleSavePayload {
  roleCode: string
  roleName: string
  sort?: number
  status: number
  remark?: string
}

export interface MenuNode {
  id: ApiId
  parentId: ApiId
  menuName: string
  menuType: string
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort?: number
  visible: number
  status: number
  children?: MenuNode[]
}

export interface ProfileUpdatePayload {
  realName: string
  phone?: string
  email?: string
  avatar?: string
}

export interface PasswordChangePayload {
  oldPassword: string
  newPassword: string
}

export interface DepartmentNode {
  id: ApiId
  parentId: ApiId
  departmentCode: string
  departmentName: string
  leaderUserId?: ApiId
  leaderName?: string
  sort?: number
  status: number
  children?: DepartmentNode[]
}

export interface DepartmentSavePayload {
  parentId: ApiId
  departmentCode: string
  departmentName: string
  leaderUserId?: ApiId | null
  sort?: number
  status: number
}

export interface CompanyInfo {
  id: ApiId
  companyName: string
  contactName?: string
  contactPhone?: string
  address?: string
  status: number
}

export interface PositionItem {
  id: ApiId
  departmentId: ApiId
  departmentName?: string
  positionCode: string
  positionName: string
  sort?: number
  status: number
}

export interface PositionSavePayload {
  departmentId: ApiId
  positionCode: string
  positionName: string
  sort?: number
  status: number
}

export interface EmployeeItem {
  id: ApiId
  userId: ApiId
  realName?: string
  employeeNo: string
  departmentId: ApiId
  departmentName?: string
  positionId?: ApiId
  positionName?: string
  employmentStatus: string
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
  records?: ApprovalRecord[]
}

export interface ApprovalRecord {
  id: ApiId
  formId: ApiId
  action: string
  operatorUserId: ApiId
  operatorName?: string
  fromStatus?: string
  toStatus?: string
  comment?: string
  createTime?: string
}

export interface AttendanceToday {
  id?: ApiId
  attendanceDate?: string
  checkInTime?: string
  checkOutTime?: string
  checkInStatus?: string
  checkOutStatus?: string
  remark?: string
}

export interface AttendanceRecord extends AttendanceToday {
  id: ApiId
  userId: ApiId
  realName?: string
  departmentId?: ApiId
  departmentName?: string
}

export interface AttendanceSummary {
  userId: ApiId
  realName?: string
  summaryMonth: string
  normalDays: number
  lateCount: number
  earlyLeaveCount: number
  missingCount: number
  leaveDays: number
  overtimeHours: number
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

export interface AnnouncementPayload {
  title: string
  content: string
  targetType: string
  departmentId?: ApiId | null
}

export interface AnnouncementSendResult {
  recipientCount: number
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

export interface PolicyDocument {
  id: ApiId
  title: string
  content?: string
  summary?: string
  documentVersion?: string
  status: string
  publisherId?: ApiId
  publishedAt?: string
}
