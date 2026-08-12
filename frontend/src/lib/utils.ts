import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const currency = new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 2 });
export const formatMoney = (value: number) => `${currency.format(value)} сом`;
export const formatDate = (value: string) =>
  new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Asia/Bishkek",
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(new Date(value));
export const formatTime = (value: string) =>
  new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Asia/Bishkek",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
export const formatDateTime = (value: string) =>
  `${formatDate(value)}, ${formatTime(value)}`;
export const formatCompactDateTime = (value: string) =>
  new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Asia/Bishkek",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));

export function dateRange(period: "today" | "week" | "month") {
  const now = new Date();
  const from = new Date(now);
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  if (period === "today") to.setDate(to.getDate() + 1);
  if (period === "week") {
    const day = (from.getDay() + 6) % 7;
    from.setDate(from.getDate() - day);
    to.setTime(from.getTime());
    to.setDate(to.getDate() + 7);
  }
  if (period === "month") {
    from.setDate(1);
    to.setTime(from.getTime());
    to.setMonth(to.getMonth() + 1);
  }
  return { from: from.toISOString(), to: to.toISOString() };
}
