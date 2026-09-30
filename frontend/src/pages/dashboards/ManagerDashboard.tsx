import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import * as usersApi from "../../api/users";
import * as customersApi from "../../api/customers";
import * as workOrdersApi from "../../api/workOrders";

import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import { useAuth } from "../../context/AuthContext";

import type {
  Role,
  User,
  WorkOrder,
} from "../../types";

const ROLE_LABELS: Record<Role, string> = {
  DISPATCHER: "Dispatchers",
  TECHNICIAN: "Technicians",
  MANAGER: "Managers",
  CUSTOMER: "Customer users",
};

export default function ManagerDashboard() {
  const { user } = useAuth();

  const [users, setUsers] = useState<User[] | null>(null);
  const [customerCount, setCustomerCount] =
    useState<number | null>(null);

  const [workOrders, setWorkOrders] =
    useState<WorkOrder[] | null>(null);

  const [error, setError] =
    useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [
          allUsers,
          customerPage,
          workOrderPage,
        ] = await Promise.all([
          usersApi.listUsers(),

          customersApi.listCustomers({
            page: 0,
            size: 1,
          }),

          workOrdersApi.listWorkOrders(
            0,
            100
          ),
        ]);

        if (cancelled) {
          return;
        }

        setUsers(allUsers);

        setCustomerCount(
          customerPage.totalElements
        );

        setWorkOrders(
          workOrderPage.content
        );
      } catch (err) {
        if (!cancelled) {
          setError(
            apiErrorMessage(
              err,
              "Could not load dashboard data."
            )
          );
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, []);

  // =========================================
  // USER COUNTS
  // =========================================

  const counts: Record<Role, number> = {
    DISPATCHER: 0,
    TECHNICIAN: 0,
    MANAGER: 0,
    CUSTOMER: 0,
  };

  users?.forEach((u) => {
    counts[u.role] += 1;
  });

  // =========================================
  // WORK ORDER COUNTS
  // =========================================

  const totalWorkOrders =
    workOrders?.length ?? 0;

  const openWorkOrders =
    workOrders?.filter(
      (w) => w.status === "OPEN"
    ).length ?? 0;

  const assignedWorkOrders =
    workOrders?.filter(
      (w) => w.status === "ASSIGNED"
    ).length ?? 0;

  const inProgressWorkOrders =
    workOrders?.filter(
      (w) => w.status === "IN_PROGRESS"
    ).length ?? 0;

  const completedWorkOrders =
    workOrders?.filter(
      (w) => w.status === "COMPLETED"
    ).length ?? 0;

  const onHoldWorkOrders =
    workOrders?.filter(
      (w) => w.status === "ON_HOLD"
    ).length ?? 0;

  // =========================================
  // SLA
  // =========================================

  const overdueWorkOrders =
    workOrders?.filter((workOrder) => {
      if (!workOrder.slaDueDate) {
        return false;
      }

      if (
        workOrder.status === "COMPLETED" ||
        workOrder.status === "CLOSED"
      ) {
        return false;
      }

      return (
        new Date(workOrder.slaDueDate).getTime() <
        Date.now()
      );
    }).length ?? 0;

  const slaTrackedWorkOrders =
    workOrders?.filter(
      (w) => !!w.slaDueDate
    ).length ?? 0;

  return (
    <div className="page-stack">

      {/* ===================================== */}
      {/* HEADER */}
      {/* ===================================== */}

      <div className="page-header">

        <h1>
          Welcome back, {user?.name}
        </h1>

        <p className="page-header__subtitle">
          Manager view — everything a dispatcher
          can do, plus user and account management.
        </p>

      </div>

      {error && (
        <div className="form-error">
          {error}
        </div>
      )}

      {/* ===================================== */}
      {/* USER / CUSTOMER STATS */}
      {/* ===================================== */}

      <div className="stat-grid">

        <StatCard
          label="Total users"
          value={
            users
              ? users.length
              : "—"
          }
        />

        <StatCard
          label="Customers"
          value={
            customerCount ?? "—"
          }
        />

        {(Object.keys(
          ROLE_LABELS
        ) as Role[]).map((role) => (
          <StatCard
            key={role}
            label={ROLE_LABELS[role]}
            value={
              users
                ? counts[role]
                : "—"
            }
          />
        ))}

      </div>

      {/* ===================================== */}
      {/* WORK ORDER STATS */}
      {/* ===================================== */}

      <div className="stat-grid">

        <StatCard
          label="Total work orders"
          value={
            workOrders
              ? totalWorkOrders
              : "—"
          }
        />

        <StatCard
          label="Open"
          value={
            workOrders
              ? openWorkOrders
              : "—"
          }
        />

        <StatCard
          label="Assigned"
          value={
            workOrders
              ? assignedWorkOrders
              : "—"
          }
        />

        <StatCard
          label="In progress"
          value={
            workOrders
              ? inProgressWorkOrders
              : "—"
          }
        />

        <StatCard
          label="Completed"
          value={
            workOrders
              ? completedWorkOrders
              : "—"
          }
        />

        <StatCard
          label="On hold"
          value={
            workOrders
              ? onHoldWorkOrders
              : "—"
          }
        />

        <StatCard
          label="SLA tracked"
          value={
            workOrders
              ? slaTrackedWorkOrders
              : "—"
          }
        />

        <StatCard
          label="SLA overdue"
          value={
            workOrders
              ? overdueWorkOrders
              : "—"
          }
        />

      </div>

      {/* ===================================== */}
      {/* MANAGEMENT PANELS */}
      {/* ===================================== */}

      <div className="panel-grid">

        {/* USERS */}

        <section className="panel">

          <h2>
            Manage users
          </h2>

          <p>
            Create accounts, review roles,
            and remove access.
          </p>

          <Link
            className="btn btn--primary"
            to="/users"
          >
            Open Users
          </Link>

        </section>

        {/* CUSTOMERS */}

        <section className="panel">

          <h2>
            Manage customers
          </h2>

          <p>
            Create and manage the organisations
            serviced by KEYSTONE.
          </p>

          <Link
            className="btn btn--primary"
            to="/customers"
          >
            Open Customers
          </Link>

        </section>

        {/* SITES */}

        <section className="panel">

          <h2>
            Customer sites
          </h2>

          <p>
            Manage customer locations and
            service addresses.
          </p>

          <Link
            className="btn btn--primary"
            to="/sites"
          >
            Open Sites
          </Link>

        </section>

        {/* WORK ORDERS */}

        <section className="panel">

          <h2>
            Work orders
          </h2>

          <p>
            Create, assign, monitor and update
            service work orders.
          </p>

          <Link
            className="btn btn--primary"
            to="/work-orders"
          >
            Open Work Orders
          </Link>

        </section>

        {/* SLA */}

        <section className="panel">

          <h2>
            SLA monitoring
          </h2>

          <p>
            {workOrders
              ? `${overdueWorkOrders} work order${
                  overdueWorkOrders === 1
                    ? ""
                    : "s"
                } currently overdue.`
              : "Loading SLA information..."}

          </p>

          <Link
            className="btn btn--primary"
            to="/work-orders"
          >
            Review SLA
          </Link>

        </section>

        {/* REPORTING */}

        <section className="panel">

          <h2>
            Work order reporting
          </h2>

          <p>
            Review current workload, status,
            assignments, SLA deadlines and
            completed service work.
          </p>

          <Link
            className="btn btn--primary"
            to="/work-orders"
          >
            View Work Orders
          </Link>

        </section>

      </div>

    </div>
  );
}