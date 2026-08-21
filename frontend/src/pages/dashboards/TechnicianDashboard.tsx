import EmptyState from "../../components/EmptyState";
import RoleBadge from "../../components/RoleBadge";
import { useAuth } from "../../context/AuthContext";

export default function TechnicianDashboard() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Welcome back, {user.name}</h1>
        <p className="page-header__subtitle">Technician view — your assigned jobs, from a phone or a desk.</p>
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
          <dt>Joined</dt>
          <dd>{new Date(user.createdAt).toLocaleDateString()}</dd>
        </dl>
      </section>

      <section className="panel panel--muted">
        <EmptyState
          title="No jobs to show"
          description="This view is meant to list work orders assigned to you — start, hold/resume, complete, and log parts and time. The backend doesn't have a WorkOrder API yet, so there's nothing real to fetch here. Build /api/work-orders (see the spec's Section 10) and this page is ready to wire up."
        />
      </section>
    </div>
  );
}
