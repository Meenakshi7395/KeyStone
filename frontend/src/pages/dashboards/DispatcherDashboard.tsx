import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as customersApi from "../../api/customers";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import EmptyState from "../../components/EmptyState";
import { useAuth } from "../../context/AuthContext";

export default function DispatcherDashboard() {
  const { user } = useAuth();
  const [customerCount, setCustomerCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    customersApi
      .listCustomers({ page: 0, size: 1 })
      .then((res) => {
        if (!cancelled) setCustomerCount(res.totalElements);
      })
      .catch((err) => {
        if (!cancelled) setError(apiErrorMessage(err, "Could not load dashboard data."));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Welcome back, {user?.name}</h1>
        <p className="page-header__subtitle">Dispatcher view — create customers and sites, then assign work.</p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="Customers" value={customerCount ?? "—"} />
      </div>

      <div className="panel-grid">
        <section className="panel">
          <h2>Customers</h2>
          <p>Add customers and keep their contact details current.</p>
          <Link className="btn btn--primary" to="/customers">
            Open Customers
          </Link>
        </section>
        <section className="panel panel--muted">
          <EmptyState
            title="Work order board"
            description="Work orders are available now. Technician assignment and status updates can be added next."
          />
        </section>
        <section className="panel"><h2>Work orders</h2><p>Create and review current service work.</p><Link className="btn btn--primary" to="/work-orders">Open Work Orders</Link></section>
      </div>
    </div>
  );
}
