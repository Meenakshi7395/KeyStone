import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../context/AuthContext";
import RoleBadge from "./RoleBadge";
import {
  IconBuilding,
  IconClipboard,
  IconDashboard,
  IconPin,
  IconUsers,
  KeystoneLogo,
} from "./Icons";

function initials(name: string) {
  return (
    name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "?"
  );
}

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

      <NavLink to="/dashboard" className="navbar__brand">
        <KeystoneLogo size={34} />
        <span className="navbar__brand-text">
          <span className="navbar__logo">KEYSTONE</span>
          <span className="navbar__subtitle">Field Service</span>
        </span>
      </NavLink>


      {/* =====================================
          NAVIGATION
      ====================================== */}

      <nav className="navbar__links">

        <div className="navbar__section-label">Workspace</div>

        <NavLink
          to="/dashboard"
          className={({ isActive }) =>
            isActive
              ? "navlink navlink--active"
              : "navlink"
          }
        >
          <IconDashboard /><span>Dashboard</span>
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
              <IconBuilding /><span>Customers</span>
            </NavLink>

            <NavLink
              to="/sites"
              className={({ isActive }) =>
                isActive
                  ? "navlink navlink--active"
                  : "navlink"
              }
            >
              <IconPin /><span>Sites</span>
            </NavLink>

            <NavLink
              to="/work-orders"
              className={({ isActive }) =>
                isActive
                  ? "navlink navlink--active"
                  : "navlink"
              }
            >
              <IconClipboard /><span>Work Orders</span>
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
            <IconUsers /><span>Users</span>
          </NavLink>
        )}

      </nav>


      {/* =====================================
          PROFILE AREA
      ====================================== */}

      <div className="navbar__status">
        <span className="pulse-dot" />
        System online
      </div>

      <div className="navbar__user">

        {/* PROFILE BUTTON */}

        <button
          type="button"
          className="navbar__profile-button"
          onClick={() =>
            setProfileOpen((value) => !value)
          }
        >

          <span className="navbar__avatar">
            {initials(user.name)}
          </span>

          <div className="navbar__user-info">

            <span className="navbar__user-name">
              {user.name}
            </span>

            <RoleBadge role={user.role} />

          </div>

          <span className="navbar__profile-arrow">
            {profileOpen ? "▼" : "▲"}
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