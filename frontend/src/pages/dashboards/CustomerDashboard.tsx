import EmptyState from "../../components/EmptyState";
import RoleBadge from "../../components/RoleBadge";
import { useAuth } from "../../context/AuthContext";

export default function CustomerDashboard() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Welcome back, {user.name}</h1>
        <p className="page-header__subtitle">Customer portal — raise a request and track it without phoning in.</p>
      </div>

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
        </dl>
      </section>

      <section className="panel panel--muted">
        <EmptyState
          title="No requests yet"
          description="This is meant to show the status and history of your own work orders, and let you raise a new one for one of your sites. The backend doesn't have Site or WorkOrder endpoints yet, so there's nothing real to show — once those exist, this page can call them directly."
        />
      </section>
    </div>
  );
}
