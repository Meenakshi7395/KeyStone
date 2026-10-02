import { FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { dashboardPathFor } from "./dashboards/DashboardRouter";
import AuthShowcase from "../components/AuthShowcase";
import { IconLock, KeystoneLogo } from "../components/Icons";

export default function LoginPage() {
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [userEmail, setUserEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated && user) {
    const from = (location.state as { from?: string } | null)?.from;
    return <Navigate to={from ?? dashboardPathFor(user.role)} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const loggedInUser = await login({ userEmail, password });
      navigate(dashboardPathFor(loggedInUser.role), { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, "Invalid email or password."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-split">
      <AuthShowcase />

      <div className="auth-split__form">
        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="auth-mobile-brand">
            <KeystoneLogo size={30} /> KEYSTONE
          </div>
          <span className="auth-eyebrow">Operator sign-in</span>
          <h1 className="auth-card__title">Welcome back</h1>
          <p className="auth-card__subtitle">
            Sign in to your KEYSTONE workspace. You'll land on the dashboard for your role.
          </p>

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              required
              autoFocus
              autoComplete="email"
              value={userEmail}
              onChange={(e) => setUserEmail(e.target.value)}
              placeholder="you@meridian.com"
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </label>

          {error && <div className="form-error">{error}</div>}

          <button className="btn btn--primary btn--block" type="submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in →"}
          </button>

          <p className="auth-card__footer">
            Accounts are created by your Meridian manager. Need access? Contact Meridian.
          </p>

          <div className="auth-secure">
            <IconLock width={13} height={13} /> Secured with signed JWT · role-based access
          </div>
        </form>
      </div>
    </div>
  );
}
