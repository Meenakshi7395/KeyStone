import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from "react";

import type {
  AuthResponse,
  CreateUserRequest,
  LoginRequest,
  User,
} from "../types";

import {
  login as loginApi,
  register as registerApi,
} from "../api/auth";

export interface SavedAccount {
  token: string;
  user: User;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;

  login: (request: LoginRequest) => Promise<User>;
  register: (request: CreateUserRequest) => Promise<User>;
  logout: () => void;

  savedAccounts: SavedAccount[];
  switchAccount: (account: SavedAccount) => void;
  removeSavedAccount: (userId: number) => void;
}

const AuthContext = createContext<
  AuthContextType | undefined
>(undefined);

const TOKEN_KEY = "keystone_token";
const USER_KEY = "keystone_user";
const ACCOUNTS_KEY = "keystone_saved_accounts";

function readUser(): User | null {
  try {
    const value = localStorage.getItem(USER_KEY);

    if (!value) {
      return null;
    }

    return JSON.parse(value) as User;
  } catch {
    return null;
  }
}

function readAccounts(): SavedAccount[] {
  try {
    const value =
      localStorage.getItem(ACCOUNTS_KEY);

    if (!value) {
      return [];
    }

    const accounts = JSON.parse(value);

    if (!Array.isArray(accounts)) {
      return [];
    }

    return accounts as SavedAccount[];
  } catch {
    return [];
  }
}

export function AuthProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_KEY)
  );

  const [user, setUser] = useState<User | null>(
    () => readUser()
  );

  const [savedAccounts, setSavedAccounts] =
    useState<SavedAccount[]>(
      () => readAccounts()
    );

  // =========================================
  // SAVE ACCOUNTS
  // =========================================

  function saveAccounts(
    accounts: SavedAccount[]
  ) {
    setSavedAccounts(accounts);

    localStorage.setItem(
      ACCOUNTS_KEY,
      JSON.stringify(accounts)
    );
  }

  // =========================================
  // LOGIN
  // =========================================

  async function login(
    request: LoginRequest
  ): Promise<User> {
    const response: AuthResponse =
      await loginApi(request);

    if (!response.token || !response.user) {
      throw new Error(
        "Invalid login response from server."
      );
    }

    localStorage.setItem(
      TOKEN_KEY,
      response.token
    );

    localStorage.setItem(
      USER_KEY,
      JSON.stringify(response.user)
    );

    setToken(response.token);
    setUser(response.user);

    const existingAccounts = readAccounts();

    const updatedAccounts: SavedAccount[] = [
      {
        token: response.token,
        user: response.user,
      },
      ...existingAccounts.filter(
        (account) =>
          account.user.id !== response.user.id
      ),
    ];

    saveAccounts(updatedAccounts);

    return response.user;
  }

  // =========================================
  // REGISTER
  // =========================================

  async function register(
    request: CreateUserRequest
  ): Promise<User> {
    const response =
      await registerApi(request);

    return response;
  }

  // =========================================
  // SWITCH ACCOUNT
  // =========================================

  function switchAccount(
    account: SavedAccount
  ) {
    localStorage.setItem(
      TOKEN_KEY,
      account.token
    );

    localStorage.setItem(
      USER_KEY,
      JSON.stringify(account.user)
    );

    setToken(account.token);
    setUser(account.user);
  }

  // =========================================
  // REMOVE SAVED ACCOUNT
  // =========================================

  function removeSavedAccount(
    userId: number
  ) {
    const updatedAccounts =
      savedAccounts.filter(
        (account) =>
          account.user.id !== userId
      );

    saveAccounts(updatedAccounts);
  }

  // =========================================
  // LOGOUT
  // =========================================

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);

    setToken(null);
    setUser(null);
  }

  // =========================================
  // CONTEXT VALUE
  // =========================================

  const value: AuthContextType = {
    user,
    token,

    isAuthenticated:
      !!token && !!user,

    login,
    register,
    logout,

    savedAccounts,
    switchAccount,
    removeSavedAccount,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    );
  }

  return context;
}

