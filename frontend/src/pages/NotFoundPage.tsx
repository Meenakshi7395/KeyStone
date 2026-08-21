import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="auth-screen">
      <div className="auth-card">
        <h1 className="auth-card__title">404</h1>
        <p className="auth-card__subtitle">That page doesn't exist.</p>
        <Link className="btn btn--primary btn--block" to="/">
          Back home
        </Link>
      </div>
    </div>
  );
}
