import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "../../lib/utils";
type Props = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "danger" | "ghost"; size?: "sm" | "md" };
export const Button = forwardRef<HTMLButtonElement, Props>(({ className, variant = "primary", size = "md", ...props }, ref) => (
  <button ref={ref} className={cn("inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2", size === "sm" ? "h-8 px-3 text-sm" : "h-10 px-4 text-sm", { "bg-brand-700 text-white hover:bg-brand-800": variant === "primary", "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50": variant === "secondary", "bg-red-600 text-white hover:bg-red-700": variant === "danger", "text-slate-600 hover:bg-slate-100": variant === "ghost" }, className)} {...props} />
));
Button.displayName = "Button";
