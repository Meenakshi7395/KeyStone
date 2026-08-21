import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { ROLES, type Role } from "../types";
import { dashboardPathFor } from "./dashboards/DashboardRouter";

export default function RegisterPage() {
  const { register, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("CUSTOMER");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  if (isAuthenticated && user) {
    return <Navigate to={dashboardPathFor(user.role)} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register({ name, email, password, role });
      setDone(true);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not create the account."));
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <h1 className="auth-card__title">Account created</h1>
          <p className="auth-card__subtitle">You can now sign in as {email}.</p>
          <button className="btn btn--primary btn--block" onClick={() => navigate("/login")}>
            Go to sign in
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1 className="auth-card__title">Create an account</h1>
        <p className="auth-card__subtitle">Register for KEYSTONE</p>

        <label className="field">
          <span>Full name</span>
          <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="Meenakshi Sharma" />
        </label>

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@keystone.com"
          />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="At least 8 characters"
          />
        </label>

        <label className="field">
          <span>Role</span>
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>

        {error && <div className="form-error">{error}</div>}

        <button className="btn btn--primary btn--block" type="submit" disabled={submitting}>
          {submitting ? "Creating…" : "Create account"}
        </button>

        <p className="auth-card__footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
