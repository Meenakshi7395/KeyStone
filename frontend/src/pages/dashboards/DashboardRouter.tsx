import type { Role } from "../../types";
import { useAuth } from "../../context/AuthContext";
import ManagerDashboard from "./ManagerDashboard";
import DispatcherDashboard from "./DispatcherDashboard";
import TechnicianDashboard from "./TechnicianDashboard";
import CustomerDashboard from "./CustomerDashboard";

// All four roles currently land on the same /dashboard route; this just
// picks which view to render. Kept as one path (rather than
// /dashboard/manager, /dashboard/technician, …) since nothing outside this
// component needs to link to a role-specific dashboard URL yet.
export function dashboardPathFor(_role: Role): string {
  return "/dashboard";
}

export default function DashboardRouter() {
  const { user } = useAuth();
  if (!user) return null;

  switch (user.role) {
    case "MANAGER":
      return <ManagerDashboard />;
    case "DISPATCHER":
      return <DispatcherDashboard />;
    case "TECHNICIAN":
      return <TechnicianDashboard />;
    case "CUSTOMER":
      return <CustomerDashboard />;
  }
}
