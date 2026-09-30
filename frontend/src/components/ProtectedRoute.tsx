// import { Navigate, Outlet } from "react-router-dom";
// import { useAuth } from "../context/AuthContext";
// import type { Role } from "../types";
// import Navbar from "./Navbar";

// interface ProtectedRouteProps {
//   allowedRoles?: Role[];
// }

// export default function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
//   const { isAuthenticated, user } = useAuth();

//   if (!isAuthenticated || !user) {
//     return <Navigate to="/login" replace />;
//   }

//   if (allowedRoles && !allowedRoles.includes(user.role)) {
//     return <Navigate to="/dashboard" replace />;
//   }

//   return (
//     <>
//       <Navbar />
//       <main className="page">
//         <Outlet />
//       </main>
//     </>
//   );
// }

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { Role } from "../types";
import Navbar from "./Navbar";

interface ProtectedRouteProps {
  allowedRoles?: Role[];
}

/**
 * Gates a route on being logged in, and optionally on role. This is a
 * usability nicety, not the security boundary — the real enforcement is
 * server-side in SecurityConfig/@PreAuthorize. A role check here just
 * keeps a user from landing on a screen whose API calls will all 403.
 */
export default function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="app-shell">
      <Navbar />
      <main className="page">
        <Outlet />
      </main>
    </div>
  );
}
