import { Check } from "lucide-react";
import { ResponsivePicker } from "../components/responsive-picker";
import type { Doctor } from "../types/api";
import { cn } from "../lib/utils";

export function ClinicDoctorPicker({
  doctors,
  value,
  onChange,
}: {
  doctors: Doctor[];
  value: number | null;
  onChange: (id: number) => void;
}) {
  const selected = doctors.find((doctor) => doctor.id === value);
  return (
    <ResponsivePicker
      title="Выберите врача"
      label={
        selected ? (
          <>
            <span className="block truncate font-medium">
              {selected.fullName}
            </span>
            <span className="block truncate text-xs text-slate-500">
              {selected.specialization || "Стоматолог"}
            </span>
          </>
        ) : (
          <span className="text-slate-500">Выберите врача</span>
        )
      }
    >
      {(close) => (
        <div className="space-y-1">
          {doctors.map((doctor) => (
            <button
              type="button"
              key={doctor.id}
              onClick={() => {
                onChange(doctor.id);
                close();
              }}
              className={cn(
                "flex min-h-14 w-full items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-slate-100",
                doctor.id === value && "bg-brand-50",
              )}
            >
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium text-slate-900">
                  {doctor.fullName}
                </span>
                <span className="block truncate text-xs text-slate-500">
                  {doctor.specialization || "Стоматолог"}
                </span>
              </span>
              {doctor.id === value && (
                <Check size={18} className="text-brand-700" />
              )}
            </button>
          ))}
        </div>
      )}
    </ResponsivePicker>
  );
}
