import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { dashboardPathFor } from "./dashboards/DashboardRouter";
import AuthShowcase from "../components/AuthShowcase";
import { IconLock, IconUser, KeystoneLogo } from "../components/Icons";

export default function RegisterPage() {
  const { register, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  if (isAuthenticated && user) {
    return <Navigate to={dashboardPathFor(user.role)} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      // Role is fixed server-side; CUSTOMER is sent only to satisfy the shared request type.
      await register({ name: name.trim(), email: email.trim(), password, role: "CUSTOMER" });
      setDone(true);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not create the account."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-split">
      <AuthShowcase />

      <div className="auth-split__form">
        {done ? (
          <div className="auth-card">
            <div className="auth-mobile-brand">
              <KeystoneLogo size={30} /> KEYSTONE
            </div>
            <span className="auth-eyebrow">Account created</span>
            <h1 className="auth-card__title">You're all set</h1>
            <p className="auth-card__subtitle">
              Your customer account for <strong>{email}</strong> is ready. Sign in to raise service
              requests and track their progress.
            </p>

            <div className="auth-note">
              <strong>Next step:</strong> after signing in, link your account to your organisation
              so you can see your sites and work orders.
            </div>

            <button className="btn btn--primary btn--block" onClick={() => navigate("/login")}>
              Go to sign in →
            </button>
          </div>
        ) : (
          <form className="auth-card" onSubmit={handleSubmit}>
            <div className="auth-mobile-brand">
              <KeystoneLogo size={30} /> KEYSTONE
            </div>
            <span className="auth-eyebrow">Customer portal</span>
            <h1 className="auth-card__title">Create your account</h1>
            <p className="auth-card__subtitle">
              Raise maintenance requests for your buildings and follow them from dispatch to
              close-out, without a phone call.
            </p>

            <div className="auth-role-chip">
              <span className="auth-role-chip__icon">
                <IconUser />
              </span>
              <div>
                <div className="auth-role-chip__title">Signing up as a Customer</div>
                <div className="auth-role-chip__desc">
                  Staff accounts (dispatcher, technician, manager) are created by your manager.
                </div>
              </div>
            </div>

            <label className="field">
              <span>Full name</span>
              <input
                required
                autoFocus
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Meenakshi Sharma"
              />
            </label>

            <label className="field">
              <span>Work email</span>
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
              />
            </label>

            <div className="field-row">
              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Min. 6 characters"
                />
              </label>

              <label className="field">
                <span>Confirm</span>
                <input
                  type="password"
                  required
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="Repeat password"
                />
              </label>
            </div>

            {error && <div className="form-error">{error}</div>}

            <button className="btn btn--primary btn--block" type="submit" disabled={submitting}>
              {submitting ? "Creating account…" : "Create account →"}
            </button>

            <div className="auth-divider">or</div>

            <p className="auth-card__footer">
              Already have an account? <Link to="/login">Sign in</Link>
            </p>

            <div className="auth-secure">
              <IconLock width={13} height={13} /> Passwords are stored as BCrypt hashes
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
