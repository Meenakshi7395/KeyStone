import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import RoleBadge from "../../components/RoleBadge";
import StatCard from "../../components/StatCard";
import { useAuth } from "../../context/AuthContext";

import * as customersApi from "../../api/customers";
import * as sitesApi from "../../api/sites";
import * as workOrdersApi from "../../api/workOrders";
import { apiErrorMessage } from "../../api/client";

import type {
  Customer,
  Site,
  WorkOrder,
} from "../../types";

export default function CustomerDashboard() {
  const { user } = useAuth();

  const [customer, setCustomer] = useState<Customer | null>(null);
  const [sites, setSites] = useState<Site[]>([]);
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /*
   * Important:
   * Capture the logged-in user's email after the null check.
   * This prevents TypeScript from treating user as possibly null
   * inside the async function.
   */
  if (!user) {
    return null;
  }

  const userEmail = user.email;

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      try {
        setLoading(true);
        setError(null);

        /*
         * Get customers and find the organisation linked
         * to the logged-in customer's email.
         */
        const customerPage =
          await customersApi.listCustomers({
            page: 0,
            size: 100,
          });

        if (cancelled) {
          return;
        }

        const matchedCustomer =
          customerPage.content.find(
            (customer) =>
              customer.contactEmail.toLowerCase() ===
              userEmail.toLowerCase()
          );

        if (!matchedCustomer) {
          throw new Error(
            "No customer organisation is linked to this account."
          );
        }

        setCustomer(matchedCustomer);

        /*
         * Load sites and work orders for this customer.
         */
        const [
          customerSites,
          customerWorkOrders,
        ] = await Promise.all([
          sitesApi.listSites(matchedCustomer.id),

          workOrdersApi.listWorkOrdersByCustomer(
            matchedCustomer.id,
            0,
            100
          ),
        ]);

        if (cancelled) {
          return;
        }

        setSites(customerSites);
        setWorkOrders(customerWorkOrders.content);
      } catch (err) {
        if (!cancelled) {
          setError(
            apiErrorMessage(
              err,
              "Could not load customer dashboard."
            )
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      cancelled = true;
    };
  }, [userEmail]);

  const openCount = workOrders.filter(
    (workOrder) => workOrder.status === "OPEN"
  ).length;

  const assignedCount = workOrders.filter(
    (workOrder) => workOrder.status === "ASSIGNED"
  ).length;

  const inProgressCount = workOrders.filter(
    (workOrder) => workOrder.status === "IN_PROGRESS"
  ).length;

  const completedCount = workOrders.filter(
    (workOrder) => workOrder.status === "COMPLETED"
  ).length;

  const overdueCount = workOrders.filter((workOrder) => {
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
  }).length;

  return (
    <div className="page-stack">

      {/* =========================================
          HEADER
      ========================================= */}

      <div className="page-header">
        <h1>Welcome back, {user.name}</h1>

        <p className="page-header__subtitle">
          Customer portal — track your service requests
          and work orders.
        </p>
      </div>

      {/* =========================================
          ERROR
      ========================================= */}

      {error && (
        <div className="form-error">
          {error}
        </div>
      )}

      {/* =========================================
          PROFILE
      ========================================= */}

      <section className="panel">
        <h2>Your profile</h2>

        <dl className="profile-grid">

          <dt>Name</dt>
          <dd>{user.name}</dd>

          <dt>Email</dt>
          <dd>{user.email}</dd>

          <dt>Role</dt>
          <dd>
            <RoleBadge role={user.role} />
          </dd>

          {customer && (
            <>
              <dt>Organisation</dt>
              <dd>{customer.name}</dd>
            </>
          )}

        </dl>
      </section>

      {/* =========================================
          WORK ORDER STATISTICS
      ========================================= */}

      <div className="stat-grid">

        <StatCard
          label="Total work orders"
          value={
            loading
              ? "—"
              : workOrders.length
          }
        />

        <StatCard
          label="Open"
          value={
            loading
              ? "—"
              : openCount
          }
        />

        <StatCard
          label="Assigned"
          value={
            loading
              ? "—"
              : assignedCount
          }
        />

        <StatCard
          label="In progress"
          value={
            loading
              ? "—"
              : inProgressCount
          }
        />

        <StatCard
          label="Completed"
          value={
            loading
              ? "—"
              : completedCount
          }
        />

        <StatCard
          label="SLA overdue"
          value={
            loading
              ? "—"
              : overdueCount
          }
        />

      </div>

      {/* =========================================
          CUSTOMER SITES
      ========================================= */}

      <section className="panel">

        <h2>Your sites</h2>

        <p>
          Locations belonging to your organisation.
        </p>

        {loading ? (
          <p>Loading sites...</p>
        ) : sites.length === 0 ? (
          <p>No sites found.</p>
        ) : (
          <div className="panel-grid">

            {sites.map((site) => (
              <div
                className="panel"
                key={site.id}
              >
                <h3>{site.name}</h3>

                <p>{site.address}</p>
              </div>
            ))}

          </div>
        )}

      </section>

      {/* =========================================
          CUSTOMER WORK ORDERS
      ========================================= */}

      <section className="panel">

        <h2>Your work orders</h2>

        <p>
          Track the status and SLA of your service
          requests.
        </p>

        {loading ? (
          <p>Loading work orders...</p>
        ) : workOrders.length === 0 ? (
          <p>No work orders found.</p>
        ) : (
          <div className="table-wrap">

            <table className="data-table">

              <thead>
                <tr>
                  <th>Title</th>
                  <th>Site</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>SLA Due</th>
                  <th>Technician</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>

                {workOrders.map((workOrder) => {

                  const isOverdue =
                    !!workOrder.slaDueDate &&
                    workOrder.status !== "COMPLETED" &&
                    workOrder.status !== "CLOSED" &&
                    new Date(
                      workOrder.slaDueDate
                    ).getTime() < Date.now();

                  return (
                    <tr key={workOrder.id}>

                      <td>
                        <strong>
                          {workOrder.title}
                        </strong>
                      </td>

                      <td>
                        {workOrder.siteName}
                      </td>

                      <td>
                        {workOrder.priority}
                      </td>

                      <td>
                        {workOrder.status}
                      </td>

                      <td>
                        {workOrder.slaDueDate
                          ? new Date(
                              workOrder.slaDueDate
                            ).toLocaleString()
                          : "—"}

                        {isOverdue && (
                          <div className="form-error">
                            Overdue
                          </div>
                        )}
                      </td>

                      <td>
                        {workOrder.technicianName ||
                          "Not assigned"}
                      </td>

                      <td>
                        <Link
                          className="btn btn--ghost"
                          to={`/work-orders/${workOrder.id}`}
                        >
                          View
                        </Link>
                      </td>

                    </tr>
                  );
                })}

              </tbody>

            </table>

          </div>
        )}

      </section>

    </div>
  );
}