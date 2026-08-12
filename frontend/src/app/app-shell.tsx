import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import {
  CalendarDays,
  CircleDollarSign,
  LayoutDashboard,
  LogOut,
  Menu,
  Stethoscope,
  Users,
  UserRoundCog,
  Wrench,
  Settings,
  X,
} from "lucide-react";
import { useAuth } from "../auth/auth-context";
import { Button } from "../components/ui/button";
import { cn } from "../lib/utils";
const adminNav = [
  { to: "/dashboard", label: "Главная", icon: LayoutDashboard },
  { to: "/appointments", label: "Календарь", icon: CalendarDays },
  { to: "/patients", label: "Пациенты", icon: Users },
  { to: "/debtors", label: "Должники", icon: CircleDollarSign },
  { to: "/doctors", label: "Врачи", icon: UserRoundCog },
  { to: "/services", label: "Услуги", icon: Wrench },
  { to: "/settings", label: "Настройки", icon: Settings },
];
const doctorNav = [
  { to: "/my-day", label: "Мой день", icon: LayoutDashboard },
  { to: "/appointments", label: "Календарь", icon: CalendarDays },
  { to: "/patients", label: "Пациенты", icon: Users },
];
export function AppShell() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const nav = user?.role === "ADMIN" ? adminNav : doctorNav;
  return (
    <div className="min-h-screen bg-slate-50">
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 w-64 border-r border-slate-200 bg-white transition-transform lg:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-16 items-center justify-between border-b border-slate-200 px-5">
          <div className="flex items-center gap-2.5">
            <span className="grid size-8 place-items-center rounded-lg bg-brand-700 text-white">
              <Stethoscope size={18} />
            </span>
            <span className="font-semibold text-slate-950">Dental CRM</span>
          </div>
          <button
            className="rounded-md p-1 text-slate-500 lg:hidden"
            onClick={() => setOpen(false)}
            aria-label="Закрыть меню"
          >
            <X size={21} />
          </button>
        </div>
        <nav className="space-y-1 p-3">
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium",
                  isActive
                    ? "bg-brand-50 text-brand-800"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
                )
              }
            >
              <Icon size={19} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="absolute inset-x-0 bottom-0 border-t border-slate-200 p-3">
          <div className="mb-2 px-3 py-2">
            <p className="truncate text-sm font-medium text-slate-900">
              {user?.fullName}
            </p>
            <p className="text-xs text-slate-500">
              {user?.role === "ADMIN" ? "Администратор" : "Врач"}
            </p>
          </div>
          <Button
            variant="ghost"
            className="w-full justify-start"
            onClick={() => void logout()}
          >
            <LogOut size={17} />
            Выйти
          </Button>
        </div>
      </aside>
      {open && (
        <button
          className="fixed inset-0 z-30 bg-slate-950/30 lg:hidden"
          onClick={() => setOpen(false)}
          aria-label="Закрыть меню"
        />
      )}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center border-b border-slate-200 bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8">
          <button
            className="rounded-lg border border-slate-200 p-2 text-slate-600 lg:hidden"
            onClick={() => setOpen(true)}
            aria-label="Открыть меню"
          >
            <Menu size={20} />
          </button>
          <p className="hidden text-sm text-slate-500 sm:ml-4 sm:block lg:ml-0">
            Стоматологическая клиника
          </p>
        </header>
        <main className="mx-auto max-w-[1440px] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
