import type { Role } from "../../types";
import { useAuth } from "../../context/AuthContext";
import ManagerDashboard from "./ManagerDashboard";
import DispatcherDashboard from "./DispatcherDashboard";
import TechnicianDashboard from "./TechnicianDashboard";
import CustomerDashboard from "./CustomerDashboard";

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
