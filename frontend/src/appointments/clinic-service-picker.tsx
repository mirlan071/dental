import { useState } from "react";
import { Search } from "lucide-react";
import { ResponsivePicker } from "../components/responsive-picker";
import { Input } from "../components/ui/input";
import { formatMoney } from "../lib/utils";
import type { ClinicService } from "../types/api";

export function ClinicServicePicker({
  services,
  onSelect,
}: {
  services: ClinicService[];
  onSelect: (service: ClinicService) => void;
}) {
  const [search, setSearch] = useState("");
  const visible = services.filter((service) =>
    service.name
      .toLocaleLowerCase("ru")
      .includes(search.trim().toLocaleLowerCase("ru")),
  );
  return (
    <ResponsivePicker
      title="Выберите услугу"
      label={<span className="text-slate-500">Найти и добавить услугу</span>}
    >
      {(close) => (
        <>
          <div className="relative mb-2">
            <Search
              size={17}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            />
            <Input
              autoFocus
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Поиск услуги"
              className="pl-9"
            />
          </div>
          <div className="space-y-1">
            {visible.map((service) => (
              <button
                type="button"
                key={service.id}
                onClick={() => {
                  onSelect(service);
                  setSearch("");
                  close();
                }}
                className="min-h-14 w-full rounded-lg px-3 py-2 text-left hover:bg-brand-50"
              >
                <span className="block text-sm font-medium text-slate-900">
                  {service.name}
                </span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  {formatMoney(service.price)} · {service.durationMinutes} мин
                </span>
              </button>
            ))}
            {!visible.length && (
              <p className="px-3 py-6 text-center text-sm text-slate-500">
                Услуга не найдена
              </p>
            )}
          </div>
        </>
      )}
    </ResponsivePicker>
  );
}
