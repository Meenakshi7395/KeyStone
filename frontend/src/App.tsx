import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import NotFoundPage from "./pages/NotFoundPage";
import UsersPage from "./pages/UsersPage";
import CustomersPage from "./pages/CustomersPage";
import SitesPage from "./pages/SitesPage";
import WorkOrdersPage from "./pages/WorkOrdersPage";
import DashboardRouter from "./pages/dashboards/DashboardRouter";
import WorkOrderDetailsPage from "./pages/WorkOrderDetailsPage";
import BoardPage from "./pages/BoardPage";
import PartsPage from "./pages/PartsPage";

export default function App(){
 const {isAuthenticated}=useAuth();
 return <Routes>
  <Route path="/login" element={<LoginPage/>}/><Route path="/register" element={<Navigate to="/login" replace/>}/>
  <Route element={<ProtectedRoute/>}><Route path="/dashboard" element={<DashboardRouter/>}/></Route>
  <Route element={<ProtectedRoute allowedRoles={["DISPATCHER","MANAGER"]}/>}>
   <Route path="/customers" element={<CustomersPage/>}/><Route path="/sites" element={<SitesPage/>}/><Route path="/work-orders" element={<WorkOrdersPage/>}/>
   <Route path="/board" element={<BoardPage/>}/><Route path="/parts" element={<PartsPage/>}/>
  </Route>
  <Route element={<ProtectedRoute allowedRoles={["MANAGER"]}/>}><Route path="/users" element={<UsersPage/>}/></Route>
  <Route element={<ProtectedRoute/>}><Route path="/work-orders/:id" element={<WorkOrderDetailsPage/>}/></Route>
  <Route path="/" element={<Navigate to={isAuthenticated?"/dashboard":"/login"} replace/>}/><Route path="*" element={<NotFoundPage/>}/>

 </Routes>;
}
