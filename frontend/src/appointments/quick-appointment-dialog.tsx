import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Search, Trash2, UserPlus } from "lucide-react";
import { Dialog } from "../components/ui/dialog";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { ClinicDatePicker } from "../components/clinic-date-picker";
import { api, ApiError, errorMessage } from "../lib/api";
import { clinicDayRange, formatClinicDay } from "../lib/clinic-date";
import { formatMoney } from "../lib/utils";
import type {
  Appointment,
  AppointmentCreateInput,
  ClinicService,
  Doctor,
  Patient,
  PatientInput,
} from "../types/api";
import { useClinicSettings } from "../settings/clinic-settings";
import { ClinicDoctorPicker } from "./clinic-doctor-picker";
import { ClinicServicePicker } from "./clinic-service-picker";
import { ClinicTimePicker } from "./clinic-time-picker";
import {
  CALENDAR_CONFIG,
  minutesToTime,
  timeToMinutes,
} from "./calendar-config";

type InitialValues = { doctorId?: number; date: string; time?: string };
type SelectedService = { service: ClinicService; quantity: number };

export function QuickAppointmentDialog({
  open,
  onOpenChange,
  initial,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initial: InitialValues;
  onCreated: (appointment: Appointment) => void;
}) {
  const client = useQueryClient();
  const [patientSearch, setPatientSearch] = useState("");
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(initial.date);
  const [time, setTime] = useState(initial.time ?? "");
  const [selectedServices, setSelectedServices] = useState<SelectedService[]>(
    [],
  );
  const [creatingPatient, setCreatingPatient] = useState(false);
  const clinicSettings = useClinicSettings();
  const workdayStart = clinicSettings.data?.workdayStart.slice(0, 5) ?? "";
  const workdayEnd = clinicSettings.data?.workdayEnd.slice(0, 5) ?? "";

  useEffect(() => {
    if (open) {
      setDoctorId(initial.doctorId ? String(initial.doctorId) : "");
      setDate(initial.date);
      setTime(initial.time ?? workdayStart);
    }
  }, [open, initial, workdayStart]);

  const doctors = useQuery({
    queryKey: ["doctors", "active"],
    queryFn: () => api<Doctor[]>("/api/doctors?activeOnly=true"),
    enabled: open,
  });
  const services = useQuery({
    queryKey: ["services", "active"],
    queryFn: () => api<ClinicService[]>("/api/services?activeOnly=true"),
    enabled: open,
  });
  const patients = useQuery({
    queryKey: ["patients", "booking", patientSearch],
    queryFn: () =>
      api<Patient[]>(
        `/api/patients?search=${encodeURIComponent(patientSearch.trim())}`,
      ),
    enabled: open && patientSearch.trim().length > 0,
  });
  const dayRange = clinicDayRange(date);
  const dayAppointments = useQuery({
    queryKey: ["appointments", date],
    queryFn: () =>
      api<Appointment[]>(
        `/api/appointments?from=${encodeURIComponent(dayRange.from)}&to=${encodeURIComponent(dayRange.to)}`,
      ),
    enabled: open && Boolean(doctorId),
  });

  const duration = selectedServices.length
    ? selectedServices.reduce(
        (sum, item) => sum + item.service.durationMinutes * item.quantity,
        0,
      )
    : CALENDAR_CONFIG.fallbackDurationMinutes;
  const effectiveTime = time || workdayStart || "00:00";
  const start = useMemo(
    () => new Date(`${date}T${effectiveTime}:00+06:00`),
    [date, effectiveTime],
  );
  const end = useMemo(
    () => new Date(start.getTime() + duration * 60_000),
    [start, duration],
  );
  const estimatedTotal = selectedServices.reduce(
    (sum, item) => sum + item.service.price * item.quantity,
    0,
  );
  const suggestedTimes = useMemo(() => {
    if (!doctorId) return [];
    if (!workdayStart || !workdayEnd) return [];
    const workdayStartMinutes = timeToMinutes(workdayStart);
    const workdayEndMinutes = timeToMinutes(workdayEnd);
    const blocking = (dayAppointments.data ?? [])
      .filter(
        (item) =>
          item.doctorId === Number(doctorId) &&
          item.status !== "CANCELLED" &&
          item.status !== "NO_SHOW",
      )
      .map((item) => ({
        start: clinicMinutes(item.startTime),
        end: clinicMinutes(item.endTime),
      }));
    return Array.from(
      {
        length:
          (workdayEndMinutes - workdayStartMinutes) /
          CALENDAR_CONFIG.slotMinutes,
      },
      (_, index) => workdayStartMinutes + index * CALENDAR_CONFIG.slotMinutes,
    )
      .filter(
        (candidate) =>
          candidate + duration <= workdayEndMinutes &&
          !blocking.some(
            (item) => candidate < item.end && candidate + duration > item.start,
          ),
      )
      .map(minutesToTime);
  }, [dayAppointments.data, doctorId, duration, workdayEnd, workdayStart]);

  const createAppointment = useMutation({
    mutationFn: (input: AppointmentCreateInput) =>
      api<Appointment>("/api/appointments", {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: async (appointment) => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["appointments"] }),
        client.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
      onCreated(appointment);
      reset();
      onOpenChange(false);
    },
  });

  function reset() {
    setPatientSearch("");
    setSelectedPatient(null);
    setSelectedServices([]);
    setCreatingPatient(false);
  }
  function close(next: boolean) {
    if (!next && !createAppointment.isPending) reset();
    onOpenChange(next);
  }
  function selectService(service: ClinicService) {
    setSelectedServices((current) =>
      current.some((item) => item.service.id === service.id)
        ? current.map((item) =>
            item.service.id === service.id
              ? { ...item, quantity: item.quantity + 1 }
              : item,
          )
        : current.concat({ service, quantity: 1 }),
    );
  }
  function quantity(id: number, delta: number) {
    setSelectedServices((current) =>
      current.map((item) =>
        item.service.id === id
          ? { ...item, quantity: Math.max(1, item.quantity + delta) }
          : item,
      ),
    );
  }
  function submit(event: FormEvent) {
    event.preventDefault();
    if (!selectedPatient || !doctorId) return;
    createAppointment.mutate({
      patientId: selectedPatient.id,
      doctorId: Number(doctorId),
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      notes: null,
      services: selectedServices.map((item) => ({
        serviceId: item.service.id,
        quantity: item.quantity,
      })),
    });
  }
  const conflict =
    createAppointment.error instanceof ApiError &&
    createAppointment.error.status === 409;

  return (
    <Dialog
      open={open}
      onOpenChange={close}
      title="Новая запись"
      className="max-w-3xl max-md:bottom-0 max-md:left-0 max-md:top-auto max-md:max-h-[96dvh] max-md:w-full max-md:max-w-none max-md:translate-x-0 max-md:translate-y-0 max-md:overflow-y-auto max-md:rounded-b-none max-md:p-4"
    >
      <form onSubmit={submit} className="grid gap-6 md:grid-cols-[1fr_1fr]">
        <div className="space-y-5">
          <section>
            <label className="text-sm font-medium text-slate-700">
              Пациент
            </label>
            {selectedPatient ? (
              <div className="mt-2 flex items-center justify-between rounded-lg border border-brand-200 bg-brand-50 px-3 py-2.5">
                <div>
                  <p className="text-sm font-medium text-slate-900">
                    {selectedPatient.fullName}
                  </p>
                  <p className="text-xs text-slate-500">
                    {selectedPatient.phone}
                  </p>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setSelectedPatient(null)}
                >
                  Изменить
                </Button>
              </div>
            ) : creatingPatient ? (
              <InlinePatientForm
                onCreated={(patient) => {
                  setSelectedPatient(patient);
                  setCreatingPatient(false);
                  setPatientSearch("");
                }}
                onCancel={() => setCreatingPatient(false)}
              />
            ) : (
              <div className="relative mt-2">
                <Search
                  className="absolute left-3 top-3 text-slate-400"
                  size={17}
                />
                <Input
                  className="pl-9"
                  placeholder="Имя или телефон"
                  value={patientSearch}
                  onChange={(e) => setPatientSearch(e.target.value)}
                />
                {patientSearch && (
                  <div className="absolute z-20 mt-1 max-h-52 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white p-1 shadow-lg">
                    {patients.isLoading && (
                      <p className="px-3 py-2 text-sm text-slate-500">Ищем…</p>
                    )}
                    {patients.data?.map((patient) => (
                      <button
                        type="button"
                        key={patient.id}
                        onClick={() => {
                          setSelectedPatient(patient);
                          setPatientSearch("");
                        }}
                        className="block w-full rounded-md px-3 py-2 text-left hover:bg-slate-50"
                      >
                        <span className="block text-sm font-medium text-slate-800">
                          {patient.fullName}
                        </span>
                        <span className="text-xs text-slate-500">
                          {patient.phone}
                        </span>
                      </button>
                    ))}
                    {patients.data?.length === 0 && (
                      <div className="p-2">
                        <p className="px-1 pb-2 text-sm text-slate-500">
                          Пациент не найден
                        </p>
                        <Button
                          type="button"
                          variant="secondary"
                          size="sm"
                          className="w-full"
                          onClick={() => setCreatingPatient(true)}
                        >
                          <UserPlus size={16} />
                          Создать пациента
                        </Button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </section>
          <section className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="text-sm font-medium text-slate-700">
              Врач
              <div className="mt-2">
                <ClinicDoctorPicker
                  doctors={(doctors.data ?? []).filter(
                    (doctor) => doctor.active,
                  )}
                  value={doctorId ? Number(doctorId) : null}
                  onChange={(id) => setDoctorId(String(id))}
                />
              </div>
            </div>
            <div className="text-sm font-medium text-slate-700">
              Дата
              <ClinicDatePicker
                className="mt-2 w-full justify-start"
                value={date}
                onChange={setDate}
                label={formatClinicDay(date, true)}
              />
            </div>
            <div className="text-sm font-medium text-slate-700">
              Время
              <div className="mt-2">
                {workdayStart && workdayEnd ? (
                  <ClinicTimePicker
                    value={effectiveTime}
                    start={workdayStart}
                    end={workdayEnd}
                    slotMinutes={CALENDAR_CONFIG.slotMinutes}
                    onChange={setTime}
                  />
                ) : (
                  <div className="h-12 animate-pulse rounded-lg bg-slate-100" />
                )}
              </div>
            </div>
            <div className="rounded-lg bg-slate-100 px-3 py-2">
              <p className="text-xs text-slate-500">Продолжительность</p>
              <p className="mt-1 text-sm font-semibold text-slate-800">
                {effectiveTime} →{" "}
                {new Intl.DateTimeFormat("ru-RU", {
                  hour: "2-digit",
                  minute: "2-digit",
                  timeZone: "Asia/Bishkek",
                }).format(end)}
              </p>
              <p className="text-xs text-slate-500">{duration} мин</p>
            </div>
            {doctorId && (
              <div className="sm:col-span-2">
                <p className="text-sm font-medium text-slate-700">
                  Свободное время
                </p>
                <div className="mt-2 flex max-h-32 flex-wrap gap-2 overflow-y-auto">
                  {suggestedTimes.length ? (
                    suggestedTimes.map((candidate) => (
                      <button
                        type="button"
                        key={candidate}
                        onClick={() => setTime(candidate)}
                        className={`min-h-11 rounded-lg border px-3 text-sm font-medium ${
                          candidate === time
                            ? "border-brand-700 bg-brand-700 text-white"
                            : "border-brand-200 bg-brand-50 text-brand-800"
                        }`}
                      >
                        {candidate}
                      </button>
                    ))
                  ) : (
                    <p className="text-sm text-slate-500">
                      Подходящих интервалов нет. Можно указать время вручную.
                    </p>
                  )}
                </div>
              </div>
            )}
          </section>
        </div>
        <div className="space-y-5">
          <section>
            <label className="text-sm font-medium text-slate-700">Услуги</label>
            <div className="mt-2">
              <ClinicServicePicker
                services={services.data ?? []}
                onSelect={selectService}
              />
            </div>
            <div className="mt-3 space-y-2">
              {selectedServices.map((item) => (
                <div
                  key={item.service.id}
                  className="rounded-lg border border-slate-200 p-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium text-slate-800">
                        {item.service.name}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {formatMoney(item.service.price)} × {item.quantity}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        setSelectedServices((current) =>
                          current.filter(
                            (entry) => entry.service.id !== item.service.id,
                          ),
                        )
                      }
                      className="flex size-11 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-600"
                      aria-label="Удалить услугу"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <div className="flex items-center rounded-md border border-slate-200">
                      <button
                        type="button"
                        onClick={() => quantity(item.service.id, -1)}
                        className="flex size-11 items-center justify-center text-slate-500"
                      >
                        <Minus size={14} />
                      </button>
                      <span className="min-w-8 text-center text-sm font-medium">
                        {item.quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => quantity(item.service.id, 1)}
                        className="flex size-11 items-center justify-center text-slate-500"
                      >
                        <Plus size={14} />
                      </button>
                    </div>
                    <p className="text-sm font-semibold text-slate-900">
                      {formatMoney(item.service.price * item.quantity)}
                    </p>
                  </div>
                </div>
              ))}
              {!selectedServices.length && (
                <div className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center">
                  <p className="text-sm font-medium text-slate-700">
                    Услуги пока не добавлены
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Можно создать запись сейчас и указать лечение позже.
                  </p>
                </div>
              )}
            </div>
          </section>
          <div className="hidden rounded-xl bg-slate-900 p-4 text-white md:block">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-300">Итого</span>
              <span className="text-xl font-semibold">
                {formatMoney(estimatedTotal)}
              </span>
            </div>
          </div>
        </div>
        {createAppointment.error && (
          <div className="md:col-span-2 rounded-lg bg-red-50 p-3 text-sm text-red-700">
            {conflict
              ? "Это время уже занято у выбранного врача."
              : errorMessage(createAppointment.error)}
          </div>
        )}
        <div className="sticky -bottom-4 z-20 -mx-4 flex flex-col gap-3 border-t border-slate-200 bg-white px-4 pb-[calc(1rem+env(safe-area-inset-bottom))] pt-3 md:static md:mx-0 md:flex-row md:items-center md:justify-end md:bg-transparent md:p-0 md:pt-4 md:col-span-2">
          <div className="flex items-center justify-between md:mr-auto md:hidden">
            <div>
              <p className="text-xs text-slate-500">Итого</p>
              <p className="text-lg font-semibold text-slate-950">
                {formatMoney(estimatedTotal)}
              </p>
            </div>
            <p className="text-sm font-medium text-slate-700">
              {effectiveTime} →{" "}
              {new Intl.DateTimeFormat("ru-RU", {
                hour: "2-digit",
                minute: "2-digit",
                timeZone: "Asia/Bishkek",
              }).format(end)}
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1 md:flex-none"
              onClick={() => close(false)}
            >
              Отмена
            </Button>
            <Button
              className="flex-[2] md:flex-none"
              disabled={
                !selectedPatient ||
                !doctorId ||
                !workdayStart ||
                !workdayEnd ||
                createAppointment.isPending
              }
            >
              {createAppointment.isPending ? "Создаём…" : "Создать запись"}
            </Button>
          </div>
        </div>
      </form>
    </Dialog>
  );
}

function clinicMinutes(value: string) {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Bishkek",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(value));
  return (
    Number(parts.find((part) => part.type === "hour")?.value) * 60 +
    Number(parts.find((part) => part.type === "minute")?.value)
  );
}

function InlinePatientForm({
  onCreated,
  onCancel,
}: {
  onCreated: (patient: Patient) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const mutation = useMutation({
    mutationFn: (input: PatientInput) =>
      api<Patient>("/api/patients", {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: onCreated,
  });
  return (
    <div className="mt-2 rounded-lg border border-slate-200 bg-slate-50 p-3">
      <p className="mb-3 text-sm font-medium text-slate-800">Новый пациент</p>
      <div className="space-y-2">
        <Input
          placeholder="ФИО"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
        />
        <Input
          type="tel"
          placeholder="Телефон"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
      </div>
      {mutation.error && (
        <p className="mt-2 text-xs text-red-600">
          {errorMessage(mutation.error)}
        </p>
      )}
      <div className="mt-3 flex justify-end gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          Назад
        </Button>
        <Button
          type="button"
          size="sm"
          disabled={!name.trim() || !phone.trim() || mutation.isPending}
          onClick={() =>
            mutation.mutate({
              fullName: name.trim(),
              phone: phone.trim(),
              birthDate: null,
              notes: null,
            })
          }
        >
          {mutation.isPending ? "Создаём…" : "Создать и выбрать"}
        </Button>
      </div>
    </div>
  );
}
