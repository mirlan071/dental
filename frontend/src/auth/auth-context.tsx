import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, clearCsrfToken } from "../lib/api";
import type { AuthUser } from "../types/api";
type AuthContextValue={user:AuthUser|null;loading:boolean;login:(username:string,password:string)=>Promise<AuthUser>;logout:()=>Promise<void>};
const AuthContext=createContext<AuthContextValue|null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();

  useEffect(() => {
    const controller = new AbortController();
    const clear = () => {
      queryClient.clear();
      setUser(null);
    };
    window.addEventListener("auth:unauthorized", clear);
    api<AuthUser>("/api/auth/me", { signal: controller.signal })
      .then(setUser)
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setUser(null);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => {
      controller.abort();
      window.removeEventListener("auth:unauthorized", clear);
    };
  }, [queryClient]);

  async function login(username: string, password: string) {
    queryClient.clear();
    const current = await api<AuthUser>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    clearCsrfToken();
    queryClient.clear();
    setUser(current);
    return current;
  }

  async function logout() {
    await api<void>("/api/auth/logout", { method: "POST" });
    clearCsrfToken();
    queryClient.clear();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
