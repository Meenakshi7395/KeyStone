export type Role = "DISPATCHER" | "TECHNICIAN" | "MANAGER" | "CUSTOMER";
export const ROLES: Role[] = ["DISPATCHER","TECHNICIAN","MANAGER","CUSTOMER"];

export interface User { id:number; name:string; email:string; role:Role; createdAt:string; }
export interface AuthResponse { token:string; user:User; }
export interface LoginRequest { userEmail:string; password:string; }
export interface CreateUserRequest { name:string; email:string; password:string; role:Role; }

export interface Customer { id:number; name:string; contactEmail:string; createdAt:string; }
export interface CustomerRequest { name:string; contactEmail:string; }

export interface Site { id:number; name:string; address:string; createdAt:string; customerId:number; }
export interface SiteRequest { name:string; address:string; }

export type WorkOrderStatus = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export interface WorkOrder {
  id:number; title:string; description:string | null;
  customerId:number; customerName:string; siteId:number; siteName:string;
  technicianId:number | null; technicianName:string | null;
  status:WorkOrderStatus; createdAt:string; updatedAt:string;
}
export interface WorkOrderRequest { title:string; description:string; customerId:number; siteId:number; }

export interface Page<T> {
 content:T[]; totalElements:number; totalPages:number; number:number; size:number; first:boolean; last:boolean;
}
export interface ApiError { timestamp?:string; status?:number; message?:string; errors?:Record<string,string>; }
