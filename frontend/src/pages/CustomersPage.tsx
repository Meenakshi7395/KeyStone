import { FormEvent, useEffect, useState } from "react";
import * as customersApi from "../api/customers";
import { apiErrorMessage } from "../api/client";
import DataTable, { type Column } from "../components/DataTable";
import type { Customer, CustomerRequest } from "../types";

const emptyForm: CustomerRequest = {
  name: "",
  contactEmail: "",
};

const PAGE_SIZE = 10;

export default function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editingId, setEditingId] =
    useState<number | null>(null);

  const [form, setForm] =
    useState<CustomerRequest>(emptyForm);

  const [formError, setFormError] =
    useState<string | null>(null);

  const [submitting, setSubmitting] =
    useState(false);

  async function loadCustomers() {
    setLoading(true);
    setError(null);

    try {
      const result =
        await customersApi.listCustomers({
          page,
          size: PAGE_SIZE,
        });

      setCustomers(result.content);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(
        apiErrorMessage(
          err,
          "Could not load customers."
        )
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  function startCreate() {
    setEditingId(null);
    setForm(emptyForm);
    setFormError(null);
  }

  function startEdit(customer: Customer) {
    setEditingId(customer.id);

    setForm({
      name: customer.name,
      contactEmail: customer.contactEmail ?? "",
    });

    setFormError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();

    setFormError(null);
    setSubmitting(true);

    try {
      if (editingId != null) {
        await customersApi.updateCustomer(
          editingId,
          form
        );
      } else {
        await customersApi.createCustomer(form);
      }

      startCreate();

      await loadCustomers();
    } catch (err) {
      setFormError(
        apiErrorMessage(
          err,
          "Could not save the customer."
        )
      );
    } finally {
      setSubmitting(false);
    }
  }

  const columns: Column<Customer>[] = [
    {
      header: "Name",
      render: (customer) => customer.name,
    },
    {
      header: "Contact email",
      render: (customer) =>
        customer.contactEmail,
    },
    {
      header: "Added",
      render: (customer) =>
        new Date(
          customer.createdAt
        ).toLocaleDateString(),
    },
    {
      header: "",
      align: "right",
      render: (customer) => (
        <button
          className="btn btn--secondary btn--small"
          onClick={() => startEdit(customer)}
        >
          Edit
        </button>
      ),
    },
  ];

  return (
    <div className="page-stack">

      <div className="page-header">
        <h1>Customers</h1>

        <p className="page-header__subtitle">
          Manage customer records.
        </p>
      </div>

      <section className="panel">

        <h2>
          {editingId != null
            ? "Edit customer"
            : "Add a customer"}
        </h2>

        <form
          className="inline-form"
          onSubmit={handleSubmit}
        >

          <input
            required
            placeholder="Customer name"
            value={form.name}
            onChange={(e) =>
              setForm((current) => ({
                ...current,
                name: e.target.value,
              }))
            }
          />

          <input
            required
            type="email"
            placeholder="Contact email"
            value={form.contactEmail}
            onChange={(e) =>
              setForm((current) => ({
                ...current,
                contactEmail: e.target.value,
              }))
            }
          />

          <button
            className="btn btn--primary"
            type="submit"
            disabled={submitting}
          >
            {submitting
              ? "Saving..."
              : editingId != null
                ? "Save changes"
                : "Add customer"}
          </button>

          {editingId != null && (
            <button
              type="button"
              className="btn btn--ghost"
              onClick={startCreate}
            >
              Cancel
            </button>
          )}

        </form>

        {formError && (
          <div className="form-error">
            {formError}
          </div>
        )}

      </section>

      <section className="panel">

        <div className="panel__toolbar">
          <h2>
            All customers ({totalElements})
          </h2>
        </div>

        <DataTable
          columns={columns}
          rows={customers}
          rowKey={(customer) => customer.id}
          loading={loading}
          error={error}
        />

        {totalPages > 1 && (
          <div className="pagination">

            <button
              className="btn btn--ghost btn--small"
              disabled={page === 0}
              onClick={() =>
                setPage((current) =>
                  current - 1
                )
              }
            >
              Previous
            </button>

            <span>
              Page {page + 1} of {totalPages}
            </span>

            <button
              className="btn btn--ghost btn--small"
              disabled={
                page + 1 >= totalPages
              }
              onClick={() =>
                setPage((current) =>
                  current + 1
                )
              }
            >
              Next
            </button>

          </div>
        )}

      </section>

    </div>
  );
}
