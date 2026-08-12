import { Clock3 } from "lucide-react";
import { ResponsivePicker } from "../components/responsive-picker";
import { cn } from "../lib/utils";
import { minutesToTime, timeToMinutes } from "./calendar-config";

export function ClinicTimePicker({
  value,
  start,
  end,
  slotMinutes,
  onChange,
}: {
  value: string;
  start: string;
  end: string;
  slotMinutes: number;
  onChange: (value: string) => void;
}) {
  const startMinutes = timeToMinutes(start);
  const endMinutes = timeToMinutes(end);
  const times = Array.from(
    { length: Math.ceil((endMinutes - startMinutes) / slotMinutes) },
    (_, index) => minutesToTime(startMinutes + index * slotMinutes),
  );
  return (
    <ResponsivePicker
      title="Другое время"
      label={
        <span className="flex items-center gap-2">
          <Clock3 size={17} className="text-slate-400" />
          <span className="font-medium">{value}</span>
        </span>
      }
    >
      {(close) => (
        <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
          {times.map((time) => (
            <button
              type="button"
              key={time}
              onClick={() => {
                onChange(time);
                close();
              }}
              className={cn(
                "min-h-11 rounded-lg border px-2 text-sm font-medium",
                time === value
                  ? "border-brand-700 bg-brand-700 text-white"
                  : "border-slate-200 bg-white text-slate-700 hover:bg-brand-50",
              )}
            >
              {time}
            </button>
          ))}
        </div>
      )}
    </ResponsivePicker>
  );
}
