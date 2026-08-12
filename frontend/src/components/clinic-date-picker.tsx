import { useState } from "react";
import * as Popover from "@radix-ui/react-popover";
import * as Dialog from "@radix-ui/react-dialog";
import { DayPicker } from "react-day-picker";
import { ru } from "react-day-picker/locale";
import { CalendarDays, X } from "lucide-react";
import { Button } from "./ui/button";
import { useMediaQuery } from "../lib/use-media-query";
import { clinicToday } from "../lib/clinic-date";
import { cn } from "../lib/utils";

function fromDateString(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day, 12);
}

function toDateString(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

export function ClinicDatePicker({
  value,
  onChange,
  label,
  className,
}: {
  value: string;
  onChange: (value: string) => void;
  label: string;
  className?: string;
}) {
  const mobile = useMediaQuery("(max-width: 767px)");
  const [open, setOpen] = useState(false);
  const calendar = (
    <DayPicker
      mode="single"
      locale={ru}
      weekStartsOn={1}
      selected={fromDateString(value)}
      defaultMonth={fromDateString(value)}
      onSelect={(day) => {
        if (day) {
          onChange(toDateString(day));
          setOpen(false);
        }
      }}
      showOutsideDays
      className="clinic-day-picker"
    />
  );
  const trigger = (
    <button
      type="button"
      className={cn(
        "inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-800 hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600",
        className,
      )}
      aria-label="Выбрать дату"
    >
      <CalendarDays size={17} />
      {label}
    </button>
  );
  if (mobile)
    return (
      <Dialog.Root open={open} onOpenChange={setOpen}>
        <Dialog.Trigger asChild>{trigger}</Dialog.Trigger>
        <Dialog.Portal>
          <Dialog.Overlay className="fixed inset-0 z-50 bg-slate-950/35" />
          <Dialog.Content className="fixed inset-x-0 bottom-0 z-50 rounded-t-2xl bg-white px-4 pb-[calc(1rem+env(safe-area-inset-bottom))] pt-4 shadow-2xl">
            <div className="mb-2 flex items-center justify-between">
              <Dialog.Title className="text-lg font-semibold text-slate-950">
                Выберите дату
              </Dialog.Title>
              <Dialog.Close
                className="grid size-11 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
                aria-label="Закрыть"
              >
                <X size={21} />
              </Dialog.Close>
            </div>
            <div className="flex justify-center">{calendar}</div>
            <Button
              variant="secondary"
              className="mt-2 w-full"
              onClick={() => {
                onChange(clinicToday());
                setOpen(false);
              }}
            >
              Сегодня
            </Button>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    );
  return (
    <Popover.Root open={open} onOpenChange={setOpen}>
      <Popover.Trigger asChild>{trigger}</Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          sideOffset={8}
          align="center"
          className="z-50 rounded-xl border border-slate-200 bg-white p-3 shadow-xl"
        >
          {calendar}
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}
