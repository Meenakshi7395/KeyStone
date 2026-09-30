import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NotFoundPage from "./pages/NotFoundPage";
import UsersPage from "./pages/UsersPage";
import CustomersPage from "./pages/CustomersPage";
import SitesPage from "./pages/SitesPage";
import WorkOrdersPage from "./pages/WorkOrdersPage";
import DashboardRouter from "./pages/dashboards/DashboardRouter";
import WorkOrderDetailsPage from "./pages/WorkOrderDetailsPage";

export default function App(){
 const {isAuthenticated}=useAuth();
 return <Routes>
  <Route path="/login" element={<LoginPage/>}/><Route path="/register" element={<RegisterPage/>}/>
  <Route element={<ProtectedRoute/>}><Route path="/dashboard" element={<DashboardRouter/>}/></Route>
  <Route element={<ProtectedRoute allowedRoles={["DISPATCHER","MANAGER"]}/>}>
   <Route path="/customers" element={<CustomersPage/>}/><Route path="/sites" element={<SitesPage/>}/><Route path="/work-orders" element={<WorkOrdersPage/>}/>
  </Route>
  <Route element={<ProtectedRoute allowedRoles={["MANAGER"]}/>}><Route path="/users" element={<UsersPage/>}/></Route>
  <Route path="/" element={<Navigate to={isAuthenticated?"/dashboard":"/login"} replace/>}/><Route path="*" element={<NotFoundPage/>}/>
  
    <Route
          path="/work-orders/:id"
          element={<WorkOrderDetailsPage />}
        />


 </Routes>;
}
