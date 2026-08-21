import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import RoleBadge from "./RoleBadge";

export default function Navbar(){
 const {user,logout}=useAuth(); const navigate=useNavigate(); if(!user)return null;
 const operational=user.role==="DISPATCHER"||user.role==="MANAGER";
 return <header className="navbar"><div className="navbar__brand"><span className="navbar__logo">KEYSTONE</span><span className="navbar__subtitle">Field Service</span></div>
 <nav className="navbar__links"><NavLink to="/dashboard" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Dashboard</NavLink>
 {operational&&<><NavLink to="/customers" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Customers</NavLink><NavLink to="/sites" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Sites</NavLink><NavLink to="/work-orders" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Work Orders</NavLink></>}
 {user.role==="MANAGER"&&<NavLink to="/users" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Users</NavLink>}</nav>
 <div className="navbar__user"><div className="navbar__user-info"><span className="navbar__user-name">{user.name}</span><RoleBadge role={user.role}/></div><button className="btn btn--ghost" onClick={()=>{logout();navigate("/login",{replace:true});}}>Log out</button></div></header>
}
