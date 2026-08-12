import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "../lib/api"; import type { AuthUser } from "../types/api";
type AuthContextValue={user:AuthUser|null;loading:boolean;login:(username:string,password:string)=>Promise<AuthUser>;logout:()=>Promise<void>};
const AuthContext=createContext<AuthContextValue|null>(null);
export function AuthProvider({children}:{children:ReactNode}){const[user,setUser]=useState<AuthUser|null>(null);const[loading,setLoading]=useState(true);
 useEffect(()=>{api<AuthUser>("/api/auth/me").then(setUser).catch(()=>setUser(null)).finally(()=>setLoading(false));const clear=()=>setUser(null);window.addEventListener("auth:unauthorized",clear);return()=>window.removeEventListener("auth:unauthorized",clear)},[]);
 async function login(username:string,password:string){const current=await api<AuthUser>("/api/auth/login",{method:"POST",body:JSON.stringify({username,password})});setUser(current);return current}
 async function logout(){await api<void>("/api/auth/logout",{method:"POST"});setUser(null)}
 return <AuthContext.Provider value={{user,loading,login,logout}}>{children}</AuthContext.Provider>}
export function useAuth(){const value=useContext(AuthContext);if(!value)throw new Error("useAuth must be used inside AuthProvider");return value}
