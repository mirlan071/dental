export type DashboardPeriod = "today" | "week" | "month";
export const CLINIC_TIME_ZONE = "Asia/Bishkek";
const CLINIC_OFFSET = "+06:00";

function clinicDateParts(date = new Date()) {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: CLINIC_TIME_ZONE, year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(date);
  const value = (type: string) => Number(parts.find(part => part.type === type)?.value);
  return { year: value("year"), month: value("month"), day: value("day") };
}

function dateString(year: number, month: number, day: number) {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function shiftDate(value: string, days: number) {
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days));
  return dateString(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
}

export function clinicToday() {
  const { year, month, day } = clinicDateParts();
  return dateString(year, month, day);
}

export function clinicDayRange(date = clinicToday()) {
  return { from: `${date}T00:00:00${CLINIC_OFFSET}`, to: `${shiftDate(date, 1)}T00:00:00${CLINIC_OFFSET}` };
}

export function clinicPeriodRange(period: DashboardPeriod) {
  const today = clinicToday();
  if (period === "today") return clinicDayRange(today);
  const [year, month] = today.split("-").map(Number);
  if (period === "month") {
    const nextMonth = month === 12 ? dateString(year + 1, 1, 1) : dateString(year, month + 1, 1);
    return { from: `${dateString(year, month, 1)}T00:00:00${CLINIC_OFFSET}`, to: `${nextMonth}T00:00:00${CLINIC_OFFSET}` };
  }
  const localMidday = new Date(`${today}T12:00:00${CLINIC_OFFSET}`);
  const weekday = Number(new Intl.DateTimeFormat("en-US", { timeZone: CLINIC_TIME_ZONE, weekday: "short" }).formatToParts(localMidday).find(part => part.type === "weekday")?.value === "Sun" ? 7 : localMidday.getUTCDay() || 7);
  const monday = shiftDate(today, -(weekday - 1));
  return { from: `${monday}T00:00:00${CLINIC_OFFSET}`, to: `${shiftDate(monday, 7)}T00:00:00${CLINIC_OFFSET}` };
}
