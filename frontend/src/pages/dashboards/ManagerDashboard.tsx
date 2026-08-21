import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as usersApi from "../../api/users";
import * as customersApi from "../../api/customers";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import { useAuth } from "../../context/AuthContext";
import type { Role, User } from "../../types";

const ROLE_LABELS: Record<Role, string> = {
  DISPATCHER: "Dispatchers",
  TECHNICIAN: "Technicians",
  MANAGER: "Managers",
  CUSTOMER: "Customer users",
};

export default function ManagerDashboard() {
  const { user } = useAuth();
  const [users, setUsers] = useState<User[] | null>(null);
  const [customerCount, setCustomerCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [allUsers, customerPage] = await Promise.all([
          usersApi.listUsers(),
          customersApi.listCustomers({ page: 0, size: 1 }),
        ]);
        if (cancelled) return;
        setUsers(allUsers);
        setCustomerCount(customerPage.totalElements);
      } catch (err) {
        if (!cancelled) setError(apiErrorMessage(err, "Could not load dashboard data."));
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const counts: Record<Role, number> = { DISPATCHER: 0, TECHNICIAN: 0, MANAGER: 0, CUSTOMER: 0 };
  users?.forEach((u) => {
    counts[u.role] += 1;
  });

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Welcome back, {user?.name}</h1>
        <p className="page-header__subtitle">
          Manager view — everything a dispatcher can do, plus user and account management.
        </p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="Total users" value={users ? users.length : "—"} />
        <StatCard label="Customers" value={customerCount ?? "—"} />
        {(Object.keys(ROLE_LABELS) as Role[]).map((r) => (
          <StatCard key={r} label={ROLE_LABELS[r]} value={users ? counts[r] : "—"} />
        ))}
      </div>

      <div className="panel-grid">
        <section className="panel">
          <h2>Manage users</h2>
          <p>Create accounts, review roles, and remove access.</p>
          <Link className="btn btn--primary" to="/users">
            Open Users
          </Link>
        </section>
        <section className="panel">
          <h2>Manage customers</h2>
          <p>Create and edit the organisations Meridian services.</p>
          <Link className="btn btn--primary" to="/customers">
            Open Customers
          </Link>
        </section>
        <section className="panel panel--muted">
          <h2>Work orders, SLA &amp; reporting</h2>
          <p>
            Not shown here yet — the WorkOrder, Site, Part, and TimeLog entities from the KEYSTONE spec haven't been
            built on the backend. Once those endpoints exist, this is where the status/SLA dashboard belongs.
          </p>
        </section>
        <section className="panel"><h2>Work orders</h2><p>Create and review current service work.</p><Link className="btn btn--primary" to="/work-orders">Open Work Orders</Link></section>
      </div>
    </div>
  );
}
