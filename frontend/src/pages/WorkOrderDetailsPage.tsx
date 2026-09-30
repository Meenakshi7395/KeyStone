import { PriorityTag, StatusPill } from "../components/StatusPill";
import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { useAuth } from "../context/AuthContext";
import * as workOrdersApi from "../api/workOrders";
import { apiErrorMessage } from "../api/client";

import type {
  AddPartToWorkOrderRequest,
  LogTimeRequest,
  Part,
  WorkOrder,
  WorkOrderHistory,
  WorkOrderStatus,
  WorkOrderTime,
} from "../types";

const STATUSES: WorkOrderStatus[] = [
  "OPEN",
  "ASSIGNED",
  "IN_PROGRESS",
  "ON_HOLD",
  "COMPLETED",
  "CLOSED",
];

export default function WorkOrderDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const workOrderId = Number(id);

  const [workOrder, setWorkOrder] =
    useState<WorkOrder | null>(null);

  const [history, setHistory] =
    useState<WorkOrderHistory[]>([]);

  const [parts, setParts] =
    useState<Part[]>([]);

  const [timeLogs, setTimeLogs] =
    useState<WorkOrderTime[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState<string | null>(null);

  const [actionError, setActionError] =
    useState<string | null>(null);

  const [status, setStatus] =
    useState<WorkOrderStatus>("OPEN");

  const [technicianId, setTechnicianId] =
    useState("");

  const [partId, setPartId] =
    useState("");

  const [quantity, setQuantity] =
    useState("1");

  const [hours, setHours] =
    useState("1");

  const [timeDescription, setTimeDescription] =
    useState("");

  const [savingStatus, setSavingStatus] =
    useState(false);

  const [assigning, setAssigning] =
    useState(false);

  const [addingPart, setAddingPart] =
    useState(false);

  const [loggingTime, setLoggingTime] =
    useState(false);

  const canManageWorkOrder =
    user?.role === "DISPATCHER" ||
    user?.role === "MANAGER";

  const canUpdateStatus =
    user?.role === "DISPATCHER" ||
    user?.role === "MANAGER" ||
    user?.role === "TECHNICIAN";

  const canUseParts =
    user?.role === "DISPATCHER" ||
    user?.role === "MANAGER" ||
    user?.role === "TECHNICIAN";

  async function loadWorkOrder() {
    if (!Number.isInteger(workOrderId)) {
      setError("Invalid work order ID.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const [
        workOrderData,
        historyData,
        partsData,
        timeData,
      ] = await Promise.all([
        workOrdersApi.getWorkOrder(workOrderId),
        workOrdersApi.getWorkOrderHistory(workOrderId),
        workOrdersApi.getWorkOrderParts(workOrderId),
        workOrdersApi.getWorkOrderTimeLogs(workOrderId),
      ]);

      setWorkOrder(workOrderData);
      setStatus(workOrderData.status);
      setHistory(historyData);
      setParts(partsData);
      setTimeLogs(timeData);
    } catch (e) {
      setError(
        apiErrorMessage(
          e,
          "Could not load work order."
        )
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadWorkOrder();
  }, [workOrderId]);

  async function updateStatus(
    e: FormEvent
  ) {
    e.preventDefault();

    if (!workOrder) {
      return;
    }

    setActionError(null);
    setSavingStatus(true);

    try {
      const updated =
        await workOrdersApi.updateWorkOrderStatus(
          workOrder.id,
          { status }
        );

      setWorkOrder(updated);

      const historyData =
        await workOrdersApi.getWorkOrderHistory(
          workOrder.id
        );

      setHistory(historyData);
    } catch (e) {
      setActionError(
        apiErrorMessage(
          e,
          "Could not update status."
        )
      );
    } finally {
      setSavingStatus(false);
    }
  }

  async function assignTechnician(
    e: FormEvent
  ) {
    e.preventDefault();

    if (!workOrder) {
      return;
    }

    const id = Number(technicianId);

    if (!id) {
      setActionError(
        "Please enter a valid technician ID."
      );
      return;
    }

    setActionError(null);
    setAssigning(true);

    try {
      const updated =
        await workOrdersApi.assignTechnician(
          workOrder.id,
          {
            technicianId: id,
          }
        );

      setWorkOrder(updated);
      setTechnicianId("");

      const historyData =
        await workOrdersApi.getWorkOrderHistory(
          workOrder.id
        );

      setHistory(historyData);
    } catch (e) {
      setActionError(
        apiErrorMessage(
          e,
          "Could not assign technician."
        )
      );
    } finally {
      setAssigning(false);
    }
  }

  async function addPart(
    e: FormEvent
  ) {
    e.preventDefault();

    if (!workOrder) {
      return;
    }

    const selectedPartId = Number(partId);
    const selectedQuantity = Number(quantity);

    if (
      !selectedPartId ||
      !selectedQuantity ||
      selectedQuantity < 1
    ) {
      setActionError(
        "Please enter a valid part ID and quantity."
      );
      return;
    }

    const payload: AddPartToWorkOrderRequest = {
      partId: selectedPartId,
      quantity: selectedQuantity,
    };

    setActionError(null);
    setAddingPart(true);

    try {
      await workOrdersApi.addPartToWorkOrder(
        workOrder.id,
        payload
      );

      const partsData =
        await workOrdersApi.getWorkOrderParts(
          workOrder.id
        );

      setParts(partsData);

      setPartId("");
      setQuantity("1");
    } catch (e) {
      setActionError(
        apiErrorMessage(
          e,
          "Could not add part."
        )
      );
    } finally {
      setAddingPart(false);
    }
  }

  async function logTime(
    e: FormEvent
  ) {
    e.preventDefault();

    if (!workOrder) {
      return;
    }

    const selectedHours = Number(hours);

    if (
      !selectedHours ||
      selectedHours < 1
    ) {
      setActionError(
        "Hours must be at least 1."
      );
      return;
    }

    if (!timeDescription.trim()) {
      setActionError(
        "Please enter a time description."
      );
      return;
    }

    const payload: LogTimeRequest = {
      hours: selectedHours,
      description: timeDescription.trim(),
    };

    setActionError(null);
    setLoggingTime(true);

    try {
      await workOrdersApi.logWorkOrderTime(
        workOrder.id,
        payload
      );

      const timeData =
        await workOrdersApi.getWorkOrderTimeLogs(
          workOrder.id
        );

      setTimeLogs(timeData);

      setHours("1");
      setTimeDescription("");
    } catch (e) {
      setActionError(
        apiErrorMessage(
          e,
          "Could not log time."
        )
      );
    } finally {
      setLoggingTime(false);
    }
  }

  function slaText() {
    if (!workOrder?.slaDueDate) {
      return "No SLA due date";
    }

    const due = new Date(
      workOrder.slaDueDate
    );

    if (Number.isNaN(due.getTime())) {
      return "Invalid SLA date";
    }

    const now = new Date();

    if (due.getTime() < now.getTime()) {
      return `OVERDUE — ${due.toLocaleString()}`;
    }

    return due.toLocaleString();
  }

  if (loading) {
    return (
      <div className="page-stack">
        <section className="panel">
          Loading work order...
        </section>
      </div>
    );
  }

  if (error || !workOrder) {
    return (
      <div className="page-stack">
        <section className="panel">
          <div className="form-error">
            {error ?? "Work order not found."}
          </div>

          <button
            className="btn btn--secondary"
            onClick={() => navigate("/work-orders")}
          >
            Back to Work Orders
          </button>
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>
          Work Order <span className="mono">#{workOrder.id}</span>
        </h1>

        <p className="page-header__subtitle">
          {workOrder.title}
        </p>

        <div>
          <Link
            to="/work-orders"
            className="btn btn--secondary"
          >
            ← Back to Work Orders
          </Link>
        </div>
      </div>

      {actionError && (
        <div className="form-error">
          {actionError}
        </div>
      )}

      {/* BASIC INFORMATION */}

      <section className="panel">
        <h2>Work Order Information</h2>

        <div className="profile-grid">
          <div>
            <strong>ID</strong>
            <div>{workOrder.id}</div>
          </div>

          <div>
            <strong>Title</strong>
            <div>{workOrder.title}</div>
          </div>

          <div>
            <strong>Customer</strong>
            <div>
              {workOrder.customerName}
            </div>
          </div>

          <div>
            <strong>Site</strong>
            <div>
              {workOrder.siteName}
            </div>
          </div>

          <div>
            <strong>Priority</strong>
            <div>
              <PriorityTag priority={workOrder.priority} />
            </div>
          </div>

          <div>
            <strong>Status</strong>
            <div>
              <StatusPill status={workOrder.status} />
            </div>
          </div>

          <div>
            <strong>Technician</strong>
            <div>
              {workOrder.technicianName ??
                "Not assigned"}
            </div>
          </div>

          <div>
            <strong>SLA Due</strong>
            <div>
              {slaText()}
            </div>
          </div>

          <div>
            <strong>Created</strong>
            <div>
              {new Date(
                workOrder.createdAt
              ).toLocaleString()}
            </div>
          </div>

          <div>
            <strong>Updated</strong>
            <div>
              {workOrder.updatedAt
                ? new Date(
                    workOrder.updatedAt
                  ).toLocaleString()
                : "—"}
            </div>
          </div>
        </div>

        <div style={{ marginTop: "1rem" }}>
          <strong>Description</strong>

          <p>
            {workOrder.description ||
              "No description provided."}
          </p>
        </div>
      </section>

      {/* STATUS */}

      {canUpdateStatus && (
        <section className="panel">
          <h2>Update Status</h2>

          <form
            className="inline-form"
            onSubmit={updateStatus}
          >
            <select
              value={status}
              onChange={(e) =>
                setStatus(
                  e.target
                    .value as WorkOrderStatus
                )
              }
            >
              {STATUSES.map((item) => (
                <option
                  key={item}
                  value={item}
                >
                  {item}
                </option>
              ))}
            </select>

            <button
              className="btn btn--primary"
              disabled={savingStatus}
            >
              {savingStatus
                ? "Saving..."
                : "Update Status"}
            </button>
          </form>
        </section>
      )}

      {/* ASSIGN TECHNICIAN */}

      {canManageWorkOrder && (
        <section className="panel">
          <h2>Assign Technician</h2>

          <p>
            Current technician:{" "}
            <strong>
              {workOrder.technicianName ??
                "Not assigned"}
            </strong>
          </p>

          <form
            className="inline-form"
            onSubmit={assignTechnician}
          >
            <input
              type="number"
              min="1"
              placeholder="Technician ID"
              value={technicianId}
              onChange={(e) =>
                setTechnicianId(
                  e.target.value
                )
              }
            />

            <button
              className="btn btn--primary"
              disabled={assigning}
            >
              {assigning
                ? "Assigning..."
                : "Assign Technician"}
            </button>
          </form>
        </section>
      )}

      {/* HISTORY */}

      <section className="panel">
        <h2>Work Order History</h2>

        {history.length === 0 ? (
          <p>No history available.</p>
        ) : (
          <div className="data-table">
            <table>
              <thead>
                <tr>
                  <th>Action</th>
                  <th>Old Value</th>
                  <th>New Value</th>
                  <th>Date</th>
                </tr>
              </thead>

              <tbody>
                {history.map((item) => (
                  <tr key={item.id}>
                    <td>{item.action}</td>
                    <td>
                      {item.oldValue ?? "—"}
                    </td>
                    <td>
                      {item.newValue ?? "—"}
                    </td>
                    <td>
                      {new Date(
                        item.createdAt
                      ).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* PARTS */}

      <section className="panel">
        <h2>Parts Used</h2>

        {canUseParts && (
          <form
            className="inline-form"
            onSubmit={addPart}
          >
            <input
              type="number"
              min="1"
              placeholder="Part ID"
              value={partId}
              onChange={(e) =>
                setPartId(e.target.value)
              }
            />

            <input
              type="number"
              min="1"
              placeholder="Quantity"
              value={quantity}
              onChange={(e) =>
                setQuantity(e.target.value)
              }
            />

            <button
              className="btn btn--primary"
              disabled={addingPart}
            >
              {addingPart
                ? "Adding..."
                : "Add Part"}
            </button>
          </form>
        )}

        <div
          className="data-table"
          style={{ marginTop: "1rem" }}
        >
          {parts.length === 0 ? (
            <p>No parts used.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Stock</th>
                </tr>
              </thead>

              <tbody>
                {parts.map((part) => (
                  <tr key={part.id}>
                    <td>{part.id}</td>
                    <td>{part.name}</td>
                    <td>
                      {part.stockQuantity}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      {/* TIME */}

      <section className="panel">
        <h2>Time Logs</h2>

        {canUseParts && (
          <form
            className="inline-form"
            onSubmit={logTime}
          >
            <input
              type="number"
              min="1"
              placeholder="Hours"
              value={hours}
              onChange={(e) =>
                setHours(e.target.value)
              }
            />

            <input
              placeholder="Description"
              value={timeDescription}
              onChange={(e) =>
                setTimeDescription(
                  e.target.value
                )
              }
            />

            <button
              className="btn btn--primary"
              disabled={loggingTime}
            >
              {loggingTime
                ? "Logging..."
                : "Log Time"}
            </button>
          </form>
        )}

        <div
          className="data-table"
          style={{ marginTop: "1rem" }}
        >
          {timeLogs.length === 0 ? (
            <p>No time logs.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Hours</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>

              <tbody>
                {timeLogs.map((log) => (
                  <tr key={log.id}>
                    <td>{log.hours}</td>
                    <td>
                      {log.description}
                    </td>
                    <td>
                      {new Date(
                        log.createdAt
                      ).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </div>
  );
}