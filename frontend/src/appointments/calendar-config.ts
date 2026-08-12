export const CALENDAR_CONFIG = {
  workdayStart: "09:00",
  workdayEnd: "18:00",
  slotMinutes: 30,
  hourHeight: 88,
  doctorColumnWidth: 240,
  fallbackDurationMinutes: 30,
} as const;

export function timeToMinutes(time: string) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

export function minutesToTime(minutes: number) {
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}
