import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import * as authApi from "../api/auth";
import { TOKEN_STORAGE_KEY } from "../api/client";

import type {
  CreateUserRequest,
  LoginRequest,
  User,
} from "../types";

const USER_STORAGE_KEY = "user";

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;

  login: (payload: LoginRequest) => Promise<User>;

  register: (
    payload: CreateUserRequest
  ) => Promise<User>;

  logout: () => void;
}

const AuthContext =
  createContext<AuthContextValue | undefined>(
    undefined
  );

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function AuthProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [token, setToken] =
    useState<string | null>(() =>
      localStorage.getItem(TOKEN_STORAGE_KEY)
    );

  const [user, setUser] =
    useState<User | null>(() =>
      readStoredUser()
    );

  const login = useCallback(
    async (payload: LoginRequest) => {
      const response =
        await authApi.login(payload);

      // Store ONLY the JWT string.
      localStorage.setItem(
        TOKEN_STORAGE_KEY,
        response.token
      );

      // Store the logged-in user separately.
      localStorage.setItem(
        USER_STORAGE_KEY,
        JSON.stringify(response.user)
      );

      setToken(response.token);
      setUser(response.user);

      return response.user;
    },
    []
  );

  const register = useCallback(
    async (payload: CreateUserRequest) => {
      return authApi.register(payload);
    },
    []
  );

  const logout = useCallback(() => {
    localStorage.removeItem(
      TOKEN_STORAGE_KEY
    );

    localStorage.removeItem(
      USER_STORAGE_KEY
    );

    setToken(null);
    setUser(null);
  }, []);

  const value =
    useMemo<AuthContextValue>(
      () => ({
        user,
        token,
        isAuthenticated: Boolean(token),
        login,
        register,
        logout,
      }),
      [
        user,
        token,
        login,
        register,
        logout,
      ]
    );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside <AuthProvider>"
    );
  }

  return context;
}

