import type { Role } from "../types";

const LABELS: Record<Role, string> = {
  DISPATCHER: "Dispatcher",
  TECHNICIAN: "Technician",
  MANAGER: "Manager",
  CUSTOMER: "Customer",
};

export default function RoleBadge({ role }: { role: Role }) {
  return <span className={`role-badge role-badge--${role.toLowerCase()}`}>{LABELS[role]}</span>;
}
