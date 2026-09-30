
// =========================================
// USER / AUTH
// =========================================

export type Role =
  | "DISPATCHER"
  | "TECHNICIAN"
  | "MANAGER"
  | "CUSTOMER";

export const ROLES: Role[] = [
  "DISPATCHER",
  "TECHNICIAN",
  "MANAGER",
  "CUSTOMER",
];

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  userEmail: string;
  password: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}


// =========================================
// CUSTOMER
// =========================================

export interface Customer {
  id: number;
  name: string;
  contactEmail: string;
  createdAt: string;
}

export interface CustomerRequest {
  name: string;
  contactEmail: string;
}


// =========================================
// SITE
// =========================================

export interface Site {
  id: number;
  name: string;
  address: string;
  createdAt: string;
  customerId: number;
}

export interface SiteRequest {
  name: string;
  address: string;
}


// =========================================
// PART
// =========================================

export interface Part {
  id: number;
  name: string;
  description?: string | null;
  stockQuantity: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PartRequest {
  name: string;
  description?: string;
  stockQuantity: number;
}


// =========================================
// WORK ORDER STATUS
// MUST MATCH BACKEND ENUM
// =========================================

export type WorkOrderStatus =
  | "OPEN"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "ON_HOLD"
  | "COMPLETED"
  | "CLOSED";


// =========================================
// WORK ORDER PRIORITY
// MUST MATCH BACKEND ENUM
// =========================================

export type WorkOrderPriority =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";


// =========================================
// WORK ORDER
// Matches WorkOrderResponseDTO
// =========================================

export interface WorkOrder {
  id: number;

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

  createdAt: string;

  updatedAt: string | null;
}


// =========================================
// CREATE WORK ORDER
// Matches WorkOrderRequestDTO
// =========================================

export interface WorkOrderRequest {
  title: string;

  description: string;

  customerId: number;

  siteId: number;

  priority: WorkOrderPriority;

  slaDueDate: string;
}


// =========================================
// ASSIGN TECHNICIAN
// Matches AssignTechnicianRequestDTO
// =========================================

export interface AssignTechnicianRequest {
  technicianId: number;
}


// =========================================
// UPDATE STATUS
// Matches UpdateWorkOrderStatusRequestDTO
// =========================================

export interface UpdateWorkOrderStatusRequest {
  status: WorkOrderStatus;
}


// =========================================
// WORK ORDER HISTORY
// Matches WorkOrderHistoryResponseDTO
// =========================================

export interface WorkOrderHistory {
  id: number;

  action: string;

  oldValue: string | null;

  newValue: string | null;

  createdAt: string;
}


// =========================================
// ADD PART TO WORK ORDER
// Matches AddPartToWorkOrderRequestDTO
// =========================================

export interface AddPartToWorkOrderRequest {
  partId: number;

  quantity: number;
}


// =========================================
// LOG WORK ORDER TIME
// Matches LogWorkOrderTimeRequestDTO
// =========================================

export interface LogTimeRequest {
  hours: number;

  description: string;
}


// =========================================
// WORK ORDER TIME
// Matches WorkOrderTimeResponseDTO
// =========================================

export interface WorkOrderTime {
  id: number;

  workOrderId: number;

  hours: number;

  description: string;

  createdAt: string;
}


// =========================================
// PAGINATION
// =========================================

export interface Page<T> {
  content: T[];

  totalElements: number;

  totalPages: number;

  number: number;

  size: number;

  first: boolean;

  last: boolean;
}


// =========================================
// API ERROR
// =========================================

export interface ApiError {
  timestamp?: string;

  status?: number;

  error?: string;

  message?: string;

  path?: string;

  errors?: Record<string, string>;
}

