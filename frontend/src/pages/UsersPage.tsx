import { FormEvent, useEffect, useMemo, useState } from "react";
import * as usersApi from "../api/users";
import { apiErrorMessage } from "../api/client";
import DataTable, { type Column } from "../components/DataTable";
import RoleBadge from "../components/RoleBadge";
import { ROLES, type CreateUserRequest, type Role, type User } from "../types";
import { useAuth } from "../context/AuthContext";

const emptyForm: CreateUserRequest = { name: "", email: "", password: "", role: "TECHNICIAN" };

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");

  const [form, setForm] = useState<CreateUserRequest>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      setUsers(await usersApi.listUsers());
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load users."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) => u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
  }, [users, search]);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await usersApi.createUser(form);
      setForm(emptyForm);
      await loadUsers();
    } catch (err) {
      setFormError(apiErrorMessage(err, "Could not create the user."));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("Delete this user? This cannot be undone.")) return;
    setDeletingId(id);
    try {
      await usersApi.deleteUser(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch (err) {
      alert(apiErrorMessage(err, "Could not delete the user."));
    } finally {
      setDeletingId(null);
    }
  }

  const columns: Column<User>[] = [
    { header: "Name", render: (u) => u.name },
    { header: "Email", render: (u) => u.email },
    { header: "Role", render: (u) => <RoleBadge role={u.role} /> },
    { header: "Joined", render: (u) => new Date(u.createdAt).toLocaleDateString() },
    {
      header: "",
      align: "right",
      render: (u) => (
        <button
          className="btn btn--danger btn--small"
          disabled={deletingId === u.id || u.id === currentUser?.id}
          title={u.id === currentUser?.id ? "You can't delete your own account" : undefined}
          onClick={() => handleDelete(u.id)}
        >
          {deletingId === u.id ? "Deleting…" : "Delete"}
        </button>
      ),
    },
  ];

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Users</h1>
        <p className="page-header__subtitle">
          All accounts across all four roles. GET/PUT/DELETE /api/users currently only requires being logged in
          (any role) — the backend doesn't yet restrict user management to Manager server-side, so treat this page
          as UI-level convenience only.
        </p>
      </div>

      <section className="panel">
        <h2>Add a user</h2>
        <form className="inline-form" onSubmit={handleCreate}>
          <input
            required
            placeholder="Full name"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <input
            required
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          />
          <input
            required
            type="password"
            minLength={8}
            placeholder="Password"
            value={form.password}
            onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
          />
          <select value={form.role} onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as Role }))}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          <button className="btn btn--primary" type="submit" disabled={submitting}>
            {submitting ? "Adding…" : "Add user"}
          </button>
        </form>
        {formError && <div className="form-error">{formError}</div>}
      </section>

      <section className="panel">
        <div className="panel__toolbar">
          <h2>All users ({filtered.length})</h2>
          <input
            className="search-input"
            placeholder="Search by name or email…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <DataTable
          columns={columns}
          rows={filtered}
          rowKey={(u) => u.id}
          loading={loading}
          error={error}
          emptyMessage="No users match your search."
        />
      </section>
    </div>
  );
}
