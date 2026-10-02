// =========================================
// Types mirror the backend DTOs (com.KeyStone.DeliveryService.DTO)
// =========================================

// ---------- USER / AUTH ----------

export type Role = "DISPATCHER" | "TECHNICIAN" | "MANAGER" | "CUSTOMER";

export const ROLES: Role[] = ["DISPATCHER", "TECHNICIAN", "MANAGER", "CUSTOMER"];

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
  /** Set for CUSTOMER logins once linked to an organisation. */
  customerId?: number | null;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  userEmail: string;
  password: string;
}

/** POST/PUT /api/users (password optional on update). Also reused by self-registration. */
export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
  customerId?: number | null;
}

// ---------- CUSTOMER / SITE ----------

export interface Customer {
  id: number;
  name: string;
  /** null when a not-yet-linked customer searches the directory. */
  contactEmail: string | null;
  createdAt: string;
}

export interface CustomerRequest {
  name: string;
  contactEmail: string;
}

export interface Site {
  id: number;
  name: string;
  address: string;
  createdAt: string;
  customerId: number;
  customerName?: string;
}

export interface SiteRequest {
  name: string;
  address: string;
}

// ---------- PARTS ----------

export interface Part {
  id: number;
  name: string;
  stockQuantity: number;
  sku?: string | null;
  unitCost?: number | null;
}

export interface PartRequest {
  name: string;
  stockQuantity: number;
  sku?: string;
  unitCost?: number | null;
}

// ---------- WORK ORDERS ----------

export type WorkOrderStatus =
  | "OPEN"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "ON_HOLD"
  | "COMPLETED"
  | "CLOSED"
  | "CANCELLED";

export type WorkOrderPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type SlaState = "NONE" | "ON_TRACK" | "AT_RISK" | "BREACHED" | "MET";

export interface WorkOrder {
  id: number;
  /** Human-readable code, e.g. WO-0042. */
  code: string;
  title: string;
  description: string | null;
  customerId: number;
  customerName: string;
  siteId: number;
  siteName: string;
  technicianId: number | null;
  technicianName: string | null;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  slaDueDate: string | null;
  slaState: SlaState;
  completedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface WorkOrderRequest {
  title: string;
  description: string;
  customerId: number;
  siteId: number;
  priority: WorkOrderPriority;
  /** Optional — the server derives it from priority when omitted. */
  slaDueDate?: string;
}

export interface WorkOrderUpdateRequest {
  title: string;
  description: string;
  siteId: number;
  priority: WorkOrderPriority;
  slaDueDate?: string;
}

export interface WorkOrderFilters {
  status?: WorkOrderStatus[];
  priority?: WorkOrderPriority;
  technicianId?: number;
  unassigned?: boolean;
  siteId?: number;
  customerId?: number;
  overdue?: boolean;
  q?: string;
  from?: string; // yyyy-mm-dd
  to?: string;
  page?: number;
  size?: number;
  sort?: string; // e.g. "slaDueDate,asc"
}

export interface AssignTechnicianRequest {
  technicianId: number;
}

export interface UpdateWorkOrderStatusRequest {
  status: WorkOrderStatus;
  note?: string;
}

export interface WorkOrderHistory {
  id: number;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  note: string | null;
  changedById: number | null;
  changedByName: string | null;
  createdAt: string;
}

export interface AddPartToWorkOrderRequest {
  partId: number;
  quantity: number;
}

/** One line of parts used on a work order. */
export interface WorkOrderPartLine {
  id: number;
  partId: number;
  name: string;
  sku: string | null;
  quantity: number;
  unitCost: number | null;
  lineCost: number;
  loggedByName: string | null;
  createdAt: string;
}

export interface LogTimeRequest {
  minutes: number;
  description?: string;
}

export interface WorkOrderTime {
  id: number;
  workOrderId: number;
  minutes: number;
  hours: number;
  description: string | null;
  loggedById: number | null;
  loggedByName: string | null;
  createdAt: string;
}

export interface WorkOrderTotals {
  workOrderId: number;
  partLines: number;
  partUnits: number;
  partsCost: number;
  labourMinutes: number;
  timeEntries: number;
}

// ---------- NOTIFICATIONS ----------

export type NotificationType = "ASSIGNED" | "SLA_AT_RISK" | "SLA_BREACHED" | "STATUS_CHANGED" | "COMPLETED";

export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string | null;
  workOrderId: number | null;
  workOrderCode: string | null;
  createdAt: string;
  readAt: string | null;
}

// ---------- REPORTS ----------

export interface BreakdownRow {
  id: number | null;
  name: string;
  total: number;
  active: number;
  completed: number;
  overdue: number;
  slaMet: number;
  slaBreached: number;
}

export interface ReportSummary {
  totalWorkOrders: number;
  openWorkOrders: number;
  assignedWorkOrders: number;
  inProgressWorkOrders: number;
  completedWorkOrders: number;
  totalCustomers: number;
  totalTechnicians: number;
  onHoldWorkOrders: number;
  closedWorkOrders: number;
  cancelledWorkOrders: number;
  activeWorkOrders: number;
  overdueWorkOrders: number;
  atRiskWorkOrders: number;
  slaMet: number;
  slaBreached: number;
  slaCompliancePercent: number | null;
  averageResolutionHours: number | null;
  countsByStatus: Record<WorkOrderStatus, number>;
  byTechnician: BreakdownRow[];
  bySite: BreakdownRow[];
  generatedAt: string;
}

// ---------- PAGINATION / ERRORS ----------

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  errors?: Record<string, string>;
}
