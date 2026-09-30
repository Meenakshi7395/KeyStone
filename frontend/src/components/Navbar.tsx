// import { NavLink, useNavigate } from "react-router-dom";
// import { useAuth } from "../context/AuthContext";
// import RoleBadge from "./RoleBadge";

// export default function Navbar(){
//  const {user,logout}=useAuth(); const navigate=useNavigate(); if(!user)return null;
//  const operational=user.role==="DISPATCHER"||user.role==="MANAGER";
//  return <header className="navbar"><div className="navbar__brand"><span className="navbar__logo">KEYSTONE</span><span className="navbar__subtitle">Field Service</span></div>
//  <nav className="navbar__links"><NavLink to="/dashboard" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Dashboard</NavLink>
//  {operational&&<><NavLink to="/customers" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Customers</NavLink><NavLink to="/sites" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Sites</NavLink><NavLink to="/work-orders" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Work Orders</NavLink></>}
//  {user.role==="MANAGER"&&<NavLink to="/users" className={({isActive})=>isActive?"navlink navlink--active":"navlink"}>Users</NavLink>}</nav>
//  <div className="navbar__user"><div className="navbar__user-info"><span className="navbar__user-name">{user.name}</span><RoleBadge role={user.role}/></div><button className="btn btn--ghost" onClick={()=>{logout();navigate("/login",{replace:true});}}>Log out</button></div></header>
// }

import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../context/AuthContext";
import RoleBadge from "./RoleBadge";

export default function Navbar() {
  const {
    user,
    logout,
    savedAccounts,
    removeSavedAccount,
  } = useAuth();

  const navigate = useNavigate();

  const [profileOpen, setProfileOpen] =
    useState(false);

  if (!user) {
    return null;
  }

  const operational =
    user.role === "DISPATCHER" ||
    user.role === "MANAGER";

  // =========================================
  // LOGOUT
  // =========================================

  function handleLogout() {
    setProfileOpen(false);

    logout();

    navigate("/login", {
      replace: true,
    });
  }

  // =========================================
  // SWITCH ACCOUNT
  // =========================================
  // Switch account means:
  // 1. Logout current account
  // 2. Keep saved accounts
  // 3. Return to login page
  // 4. User can login with another account
  // =========================================

  function handleSwitchAccount() {
    setProfileOpen(false);

    logout();

    navigate("/login", {
      replace: true,
    });
  }

  // =========================================
  // REMOVE SAVED ACCOUNT
  // =========================================

  function handleRemoveAccount(
    userId: number
  ) {
    removeSavedAccount(userId);
  }

  return (
    <header className="navbar">

      {/* =====================================
          BRAND
      ====================================== */}

      <div className="navbar__brand">

        <span className="navbar__logo">
          KEYSTONE
        </span>

        <span className="navbar__subtitle">
          Field Service
        </span>

      </div>


      {/* =====================================
          NAVIGATION
      ====================================== */}

      <nav className="navbar__links">

        <NavLink
          to="/dashboard"
          className={({ isActive }) =>
            isActive
              ? "navlink navlink--active"
              : "navlink"
          }
        >
          Dashboard
        </NavLink>


        {operational && (
          <>
            <NavLink
              to="/customers"
              className={({ isActive }) =>
                isActive
                  ? "navlink navlink--active"
                  : "navlink"
              }
            >
              Customers
            </NavLink>

            <NavLink
              to="/sites"
              className={({ isActive }) =>
                isActive
                  ? "navlink navlink--active"
                  : "navlink"
              }
            >
              Sites
            </NavLink>

            <NavLink
              to="/work-orders"
              className={({ isActive }) =>
                isActive
                  ? "navlink navlink--active"
                  : "navlink"
              }
            >
              Work Orders
            </NavLink>
          </>
        )}


        {user.role === "MANAGER" && (
          <NavLink
            to="/users"
            className={({ isActive }) =>
              isActive
                ? "navlink navlink--active"
                : "navlink"
            }
          >
            Users
          </NavLink>
        )}

      </nav>


      {/* =====================================
          PROFILE AREA
      ====================================== */}

      <div className="navbar__user">

        {/* PROFILE BUTTON */}

        <button
          type="button"
          className="navbar__profile-button"
          onClick={() =>
            setProfileOpen((value) => !value)
          }
        >

          <div className="navbar__user-info">

            <span className="navbar__user-name">
              {user.name}
            </span>

            <RoleBadge role={user.role} />

          </div>

          <span className="navbar__profile-arrow">
            {profileOpen ? "▲" : "▼"}
          </span>

        </button>


        {/* ===================================
            PROFILE DROPDOWN
        ==================================== */}

        {profileOpen && (
          <div className="navbar__profile-menu">

            {/* PROFILE INFORMATION */}

            <div className="navbar__profile-header">

              <strong>
                {user.name}
              </strong>

              <span>
                {user.email}
              </span>

              <RoleBadge role={user.role} />

            </div>


            {/* =================================
                VIEW PROFILE
            ================================== */}

            <button
              type="button"
              className="navbar__profile-item"
              onClick={() => {
                setProfileOpen(false);
                navigate("/profile");
              }}
            >
              👤 View Profile
            </button>


            {/* =================================
                SWITCH ACCOUNT
            ================================== */}

            <div className="navbar__profile-section">

              <div className="navbar__profile-section-title">
                Account
              </div>

              <button
                type="button"
                className="navbar__profile-item"
                onClick={handleSwitchAccount}
              >
                🔄 Switch Account
              </button>

            </div>


            {/* =================================
                SAVED ACCOUNTS
            ================================== */}

            {savedAccounts.length > 0 && (
              <div className="navbar__profile-section">

                <div className="navbar__profile-section-title">
                  Saved Accounts
                </div>


                {savedAccounts.map((account) => {

                  const isCurrent =
                    account.user.id === user.id;

                  return (
                    <div
                      key={account.user.id}
                      className="navbar__account-row"
                    >

                      <div
                        className={
                          isCurrent
                            ? "navbar__account navbar__account--active"
                            : "navbar__account"
                        }
                      >

                        <div className="navbar__account-info">

                          <span className="navbar__account-name">
                            {account.user.name}
                          </span>

                          <span className="navbar__account-email">
                            {account.user.email}
                          </span>

                        </div>

                        <RoleBadge
                          role={account.user.role}
                        />

                        {isCurrent && (
                          <span className="navbar__account-current">
                            Current
                          </span>
                        )}

                      </div>


                      {!isCurrent && (
                        <button
                          type="button"
                          className="navbar__account-remove"
                          title="Remove saved account"
                          onClick={() =>
                            handleRemoveAccount(
                              account.user.id
                            )
                          }
                        >
                          ×
                        </button>
                      )}

                    </div>
                  );
                })}

              </div>
            )}


            {/* =================================
                LOGOUT
            ================================== */}

            <div className="navbar__profile-footer">

              <button
                type="button"
                className="btn btn--ghost navbar__logout"
                onClick={handleLogout}
              >
                Log out
              </button>

            </div>

          </div>
        )}

      </div>

    </header>
  );
}