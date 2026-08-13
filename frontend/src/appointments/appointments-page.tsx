import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Clock3, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { api } from "../lib/api";
import {
  clinicDayRange,
  clinicToday,
  formatClinicDay,
  shiftClinicDate,
} from "../lib/clinic-date";
import { cn, formatMoney, formatTime } from "../lib/utils";
import type { Appointment, Doctor } from "../types/api";
import { PageHeader } from "../components/page-header";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { ClinicDatePicker } from "../components/clinic-date-picker";
import { StatusBadge } from "../components/status-badge";
import { EmptyState, ErrorState, LoadingState } from "../components/feedback";
import { SuccessToast } from "../components/toast";
import {
  CALENDAR_CONFIG,
  minutesToTime,
  timeToMinutes,
} from "./calendar-config";
import { QuickAppointmentDialog } from "./quick-appointment-dialog";
import { useClinicSettings } from "../settings/clinic-settings";

export function AppointmentsPage({ myDay = false }: { myDay?: boolean }) {
  const { user } = useAuth();
  return user?.role === "ADMIN" && !myDay ? (
    <AdminDailyCalendar />
  ) : (
    <AppointmentList myDay={myDay} />
  );
}

function AdminDailyCalendar() {
  const [date, setDate] = useState(clinicToday);
  const [clock, setClock] = useState(Date.now);
  const [doctorFilter, setDoctorFilter] = useState<number | null>(null);
  const [booking, setBooking] = useState<{
    open: boolean;
    doctorId?: number;
    time?: string;
  }>({ open: false });
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();
  const clinicSettings = useClinicSettings();
  const range = clinicDayRange(date);
  const doctors = useQuery({
    queryKey: ["doctors"],
    queryFn: () => api<Doctor[]>("/api/doctors"),
  });
  const appointments = useQuery({
    queryKey: ["appointments", date],
    queryFn: () =>
      api<Appointment[]>(
        `/api/appointments?from=${encodeURIComponent(range.from)}&to=${encodeURIComponent(range.to)}`,
      ),
  });
  const dayAppointments = appointments.data ?? [];
  const calendarDoctors = (doctors.data ?? []).filter(
    (doctor) =>
      doctor.active ||
      dayAppointments.some((appointment) => appointment.doctorId === doctor.id),
  );
  const visibleDoctorFilter = calendarDoctors.some(
    (doctor) => doctor.id === doctorFilter,
  )
    ? doctorFilter
    : null;
  const workdayStart = clinicSettings.data?.workdayStart.slice(0, 5);
  const workdayEnd = clinicSettings.data?.workdayEnd.slice(0, 5);
  const canBookSlot = (minutes: number) =>
    new Date(`${date}T${minutesToTime(minutes)}:00+06:00`).getTime() > clock;
  function move(days: number) {
    setDate((current) => shiftClinicDate(current, days));
  }
  function openSlot(doctorId: number, minutes: number) {
    const now = Date.now();
    if (
      new Date(`${date}T${minutesToTime(minutes)}:00+06:00`).getTime() <= now
    ) {
      setClock(now);
      return;
    }
    setBooking({ open: true, doctorId, time: minutesToTime(minutes) });
  }
  useEffect(() => {
    const timer = window.setInterval(() => setClock(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  useEffect(() => {
    if (!success) return;
    const timer = window.setTimeout(() => setSuccess(""), 3500);
    return () => window.clearTimeout(timer);
  }, [success]);
  const controls = <DateControls date={date} setDate={setDate} move={move} />;
  return (
    <>
      <div className="hidden md:block">
        <PageHeader
          title="Календарь"
          description="Дневное расписание врачей"
          actions={
            <Button onClick={() => setBooking({ open: true })}>
              <Plus size={18} />
              Новая запись
            </Button>
          }
        />
        <div className="mb-4">{controls}</div>
      </div>
      <div className="md:hidden">
        <div className="mb-4">
          <h1 className="text-2xl font-semibold text-slate-950">Календарь</h1>
          <div className="mt-4">{controls}</div>
        </div>
      </div>
      {doctors.isLoading ||
      appointments.isLoading ||
      clinicSettings.isLoading ? (
        <LoadingState />
      ) : doctors.error ? (
        <ErrorState error={doctors.error} />
      ) : appointments.error ? (
        <ErrorState error={appointments.error} />
      ) : clinicSettings.error ? (
        <ErrorState error={clinicSettings.error} />
      ) : !calendarDoctors.length ? (
        <EmptyState
          title="Нет активных врачей"
          text="Активируйте врача, чтобы показать расписание"
        />
      ) : (
        <>
          <div className="hidden md:block">
            <AdminDailyCalendarDesktop
              date={date}
              doctors={calendarDoctors}
              appointments={dayAppointments}
              workdayStart={workdayStart!}
              workdayEnd={workdayEnd!}
              openSlot={openSlot}
              canBookSlot={canBookSlot}
              openAppointment={(id) => navigate(`/appointments/${id}`)}
            />
          </div>
          <div className="md:hidden">
            <AdminDailyAgendaMobile
              date={date}
              doctors={calendarDoctors}
              appointments={dayAppointments}
              workdayStart={workdayStart!}
              workdayEnd={workdayEnd!}
              selectedDoctor={visibleDoctorFilter}
              setSelectedDoctor={setDoctorFilter}
              openSlot={openSlot}
              canBookSlot={canBookSlot}
              openAppointment={(id) => navigate(`/appointments/${id}`)}
            />
          </div>
        </>
      )}
      <div className="fixed inset-x-4 bottom-[calc(1rem+env(safe-area-inset-bottom))] z-20 md:hidden">
        <Button
          className="w-full shadow-lg"
          onClick={() => setBooking({ open: true })}
        >
          <Plus size={18} />
          Новая запись
        </Button>
      </div>
      <QuickAppointmentDialog
        open={booking.open}
        onOpenChange={(open) => setBooking((current) => ({ ...current, open }))}
        initial={{ doctorId: booking.doctorId, date, time: booking.time }}
        onCreated={(appointment) =>
          setSuccess(`Запись для ${appointment.patientName} создана`)
        }
      />
      {success && (
        <SuccessToast message={success} onClose={() => setSuccess("")} />
      )}
    </>
  );
}

function DateControls({
  date,
  setDate,
  move,
}: {
  date: string;
  setDate: (date: string) => void;
  move: (days: number) => void;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 max-md:justify-between">
      <Button
        variant="secondary"
        className="size-11 px-0"
        onClick={() => move(-1)}
        aria-label="Предыдущий день"
      >
        <ChevronLeft size={20} />
      </Button>
      <ClinicDatePicker
        value={date}
        onChange={setDate}
        label={formatClinicDay(
          date,
          typeof window !== "undefined" && window.innerWidth < 768,
        )}
        className="min-w-48 flex-1 md:flex-none"
      />
      <Button
        variant="secondary"
        className="size-11 px-0"
        onClick={() => move(1)}
        aria-label="Следующий день"
      >
        <ChevronRight size={20} />
      </Button>
      <Button
        variant="ghost"
        className="max-md:w-full"
        onClick={() => setDate(clinicToday())}
      >
        Сегодня
      </Button>
    </div>
  );
}

function AdminDailyCalendarDesktop({
  date,
  doctors,
  appointments,
  workdayStart,
  workdayEnd,
  openSlot,
  canBookSlot,
  openAppointment,
}: {
  date: string;
  doctors: Doctor[];
  appointments: Appointment[];
  workdayStart: string;
  workdayEnd: string;
  openSlot: (doctorId: number, minutes: number) => void;
  canBookSlot: (minutes: number) => boolean;
  openAppointment: (id: number) => void;
}) {
  const configuredStartMinutes = timeToMinutes(workdayStart),
    configuredEndMinutes = timeToMinutes(workdayEnd);
  const appointmentStarts = appointments.map((item) =>
    minutesInBishkek(item.startTime),
  );
  const appointmentEnds = appointments.map((item) =>
    minutesInBishkek(item.endTime),
  );
  const startMinutes = Math.floor(
    Math.min(configuredStartMinutes, ...appointmentStarts) /
      CALENDAR_CONFIG.slotMinutes,
  ) * CALENDAR_CONFIG.slotMinutes;
  const endMinutes = Math.ceil(
    Math.max(configuredEndMinutes, ...appointmentEnds) /
      CALENDAR_CONFIG.slotMinutes,
  ) * CALENDAR_CONFIG.slotMinutes;
  const totalHeight =
    ((endMinutes - startMinutes) / 60) * CALENDAR_CONFIG.hourHeight;
  const slots = Array.from(
    {
      length: Math.ceil(
        (configuredEndMinutes - configuredStartMinutes) /
          CALENDAR_CONFIG.slotMinutes,
      ),
    },
    (_, index) =>
      configuredStartMinutes + index * CALENDAR_CONFIG.slotMinutes,
  );
  const timeTicks = Array.from(
    { length: Math.floor((endMinutes - startMinutes) / 60) + 1 },
    (_, index) => startMinutes + index * 60,
  );
  if (timeTicks.at(-1) !== endMinutes) timeTicks.push(endMinutes);
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <div
          style={{
            minWidth: 80 + doctors.length * CALENDAR_CONFIG.doctorColumnWidth,
          }}
        >
          <div
            className="sticky top-0 z-20 grid border-b border-slate-200 bg-white"
            style={{
              gridTemplateColumns: `80px repeat(${doctors.length}, minmax(${CALENDAR_CONFIG.doctorColumnWidth}px, 1fr))`,
            }}
          >
            <div className="flex h-16 items-center border-r border-slate-200 px-3 text-xs font-medium text-slate-400">
              Время
            </div>
            {doctors.map((doctor) => (
              <div
                key={doctor.id}
                className="flex h-16 flex-col justify-center border-r border-slate-200 px-4 last:border-r-0"
              >
                <p className="truncate text-sm font-semibold text-slate-900">
                  {doctor.fullName}
                </p>
                <p className="truncate text-xs text-slate-500">
                  {doctor.specialization || "Врач"}
                  {!doctor.active && " · неактивен"}
                </p>
              </div>
            ))}
          </div>
          <div
            className="grid"
            style={{
              gridTemplateColumns: `80px repeat(${doctors.length}, minmax(${CALENDAR_CONFIG.doctorColumnWidth}px, 1fr))`,
            }}
          >
            <div
              className="relative border-r border-slate-200 bg-slate-50"
              style={{ height: totalHeight }}
            >
              {timeTicks.map((minutes, index) => (
                <span
                  key={minutes}
                  className={cn(
                    "absolute right-3 text-xs text-slate-500",
                    index !== 0 &&
                      (index === timeTicks.length - 1
                        ? "-translate-y-full"
                        : "-translate-y-1/2"),
                  )}
                  style={{
                    top:
                      index === 0
                        ? 4
                        : ((minutes - startMinutes) / 60) *
                          CALENDAR_CONFIG.hourHeight,
                  }}
                >
                  {minutesToTime(minutes)}
                </span>
              ))}
            </div>
            {doctors.map((doctor) => (
              <DoctorDayColumn
                key={doctor.id}
                doctor={doctor}
                date={date}
                appointments={appointments.filter(
                  (item) => item.doctorId === doctor.id,
                )}
                slots={slots}
                startMinutes={startMinutes}
                totalHeight={totalHeight}
                calendarEndMinutes={endMinutes}
                onSlot={doctor.active ? openSlot : undefined}
                canBookSlot={canBookSlot}
                onAppointment={openAppointment}
              />
            ))}
          </div>
        </div>
      </div>
    </Card>
  );
}

function AdminDailyAgendaMobile({
  doctors,
  appointments,
  workdayStart,
  workdayEnd,
  selectedDoctor,
  setSelectedDoctor,
  openSlot,
  canBookSlot,
  openAppointment,
}: {
  date: string;
  doctors: Doctor[];
  appointments: Appointment[];
  workdayStart: string;
  workdayEnd: string;
  selectedDoctor: number | null;
  setSelectedDoctor: (id: number | null) => void;
  openSlot: (doctorId: number, minutes: number) => void;
  canBookSlot: (minutes: number) => boolean;
  openAppointment: (id: number) => void;
}) {
  const visible = appointments
    .filter(
      (item) => selectedDoctor === null || item.doctorId === selectedDoctor,
    )
    .sort((a, b) => a.startTime.localeCompare(b.startTime));
  const doctor = doctors.find((item) => item.id === selectedDoctor);
  const free = doctor?.active
    ? availableStarts(
        appointments.filter((item) => item.doctorId === doctor.id),
        CALENDAR_CONFIG.fallbackDurationMinutes,
        workdayStart,
        workdayEnd,
      ).filter(canBookSlot).slice(0, 10)
    : [];
  return (
    <div className="space-y-4 pb-32">
      <div className="-mx-4 overflow-x-auto px-4">
        <div className="flex min-w-max gap-2 pb-1">
          <Filter
            active={selectedDoctor === null}
            onClick={() => setSelectedDoctor(null)}
          >
            Все
          </Filter>
          {doctors.map((item) => (
            <Filter
              key={item.id}
              active={selectedDoctor === item.id}
              onClick={() => setSelectedDoctor(item.id)}
            >
              {firstName(item.fullName)}
            </Filter>
          ))}
        </div>
      </div>
      {doctor?.active && (
        <Card className="p-4">
          <div className="flex items-center gap-2">
            <Clock3 size={18} className="text-brand-700" />
            <h2 className="font-semibold text-slate-900">Свободное время</h2>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Свободные интервалы от 30 минут
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            {free.length ? (
              free.map((minutes) => (
                <button
                  key={minutes}
                  className="min-h-11 rounded-lg border border-brand-200 bg-brand-50 px-4 text-sm font-medium text-brand-800"
                  onClick={() => openSlot(doctor.id, minutes)}
                >
                  {minutesToTime(minutes)}
                </button>
              ))
            ) : (
              <p className="text-sm text-slate-500">
                На этот день свободных интервалов нет
              </p>
            )}
          </div>
        </Card>
      )}
      {visible.length ? (
        <div className="space-y-3">
          {visible.map((item) => (
            <button
              key={item.id}
              onClick={() => openAppointment(item.id)}
              className="w-full rounded-xl border border-slate-200 bg-white p-4 text-left shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-lg font-semibold text-slate-950">
                    {formatTime(item.startTime)}–{formatTime(item.endTime)}
                  </p>
                  <p className="mt-1 font-medium text-slate-900">
                    {item.patientName}
                  </p>
                  {selectedDoctor === null && (
                    <p className="mt-0.5 text-sm text-slate-500">
                      {item.doctorName}
                    </p>
                  )}
                </div>
                <StatusBadge status={item.status} />
              </div>
              {item.services.length > 0 && (
                <p className="mt-3 truncate text-sm text-slate-500">
                  {item.services
                    .map((service) => service.serviceName)
                    .join(", ")}
                </p>
              )}
            </button>
          ))}
        </div>
      ) : (
        <EmptyState
          title="Приёмов нет"
          text="На выбранную дату ничего не запланировано"
        />
      )}
    </div>
  );
}

function Filter({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "min-h-11 rounded-full border px-4 text-sm font-medium",
        active
          ? "border-brand-700 bg-brand-700 text-white"
          : "border-slate-200 bg-white text-slate-700",
      )}
    >
      {children}
    </button>
  );
}
function firstName(name: string) {
  return name.split(" ")[0];
}
function availableStarts(
  source: Appointment[],
  duration: number,
  workdayStart: string,
  workdayEnd: string,
) {
  const start = timeToMinutes(workdayStart),
    end = timeToMinutes(workdayEnd);
  const blocking = source
    .filter((item) => item.status !== "CANCELLED" && item.status !== "NO_SHOW")
    .map((item) => ({
      start: minutesInBishkek(item.startTime),
      end: minutesInBishkek(item.endTime),
    }));
  return Array.from(
    { length: (end - start) / CALENDAR_CONFIG.slotMinutes },
    (_, index) => start + index * CALENDAR_CONFIG.slotMinutes,
  ).filter(
    (candidate) =>
      candidate + duration <= end &&
      !blocking.some(
        (item) => candidate < item.end && candidate + duration > item.start,
      ),
  );
}

function DoctorDayColumn({
  doctor,
  date,
  appointments,
  slots,
  startMinutes,
  totalHeight,
  calendarEndMinutes,
  onSlot,
  canBookSlot,
  onAppointment,
}: {
  doctor: Doctor;
  date: string;
  appointments: Appointment[];
  slots: number[];
  startMinutes: number;
  totalHeight: number;
  calendarEndMinutes: number;
  onSlot?: (doctorId: number, minutes: number) => void;
  canBookSlot: (minutes: number) => boolean;
  onAppointment: (id: number) => void;
}) {
  return (
    <div
      className="relative border-r border-slate-200 last:border-r-0"
      style={{ height: totalHeight }}
    >
      {onSlot && slots.filter(canBookSlot).map((minutes) => (
        <button
          key={minutes}
          onClick={() => onSlot(doctor.id, minutes)}
          aria-label={`Новая запись: ${doctor.fullName}, ${date}, ${minutesToTime(minutes)}`}
          className="absolute inset-x-0 border-b border-slate-100 hover:bg-brand-50/60 focus:z-10 focus:bg-brand-50 focus:outline-none"
          style={{
            top: ((minutes - startMinutes) / 60) * CALENDAR_CONFIG.hourHeight,
            height:
              (CALENDAR_CONFIG.slotMinutes / 60) * CALENDAR_CONFIG.hourHeight,
          }}
        />
      ))}
      {appointments.map((item) => {
        const start = minutesInBishkek(item.startTime),
          end = minutesInBishkek(item.endTime);
        const top = Math.max(
          0,
          ((start - startMinutes) / 60) * CALENDAR_CONFIG.hourHeight,
        );
        const height = Math.max(
          30,
          ((Math.min(end, calendarEndMinutes) -
            Math.max(start, startMinutes)) /
            60) *
            CALENDAR_CONFIG.hourHeight,
        );
        const overlapping = connectedOverlaps(item, appointments);
        const lane = overlapping.findIndex((other) => other.id === item.id);
        const laneCount = overlapping.length;
        const muted = item.status === "CANCELLED" || item.status === "NO_SHOW";
        return (
          <button
            key={item.id}
            onClick={() => onAppointment(item.id)}
            className={cn(
              "absolute z-10 overflow-hidden rounded-md border px-2 py-1.5 text-left shadow-sm hover:brightness-95",
              muted
                ? "border-slate-300 bg-slate-100 text-slate-500 opacity-80"
                : item.status === "IN_PROGRESS"
                  ? "border-amber-300 bg-amber-100 text-amber-950"
                  : item.status === "COMPLETED"
                    ? "border-emerald-300 bg-emerald-100 text-emerald-950"
                    : "border-blue-300 bg-blue-100 text-blue-950",
            )}
            style={{
              top,
              height,
              left: `calc(${(lane * 100) / laneCount}% + 4px)`,
              width: `calc(${100 / laneCount}% - 8px)`,
            }}
          >
            <p
              className={cn(
                "truncate text-xs font-semibold",
                muted && "line-through",
              )}
            >
              {item.patientName}
            </p>
            <p className="truncate text-[11px] opacity-75">
              {formatTime(item.startTime)}–{formatTime(item.endTime)}
            </p>
            <p className="mt-0.5 truncate text-[10px] opacity-70">
              {statusLabel(item.status)}
            </p>
          </button>
        );
      })}
    </div>
  );
}

function connectedOverlaps(seed: Appointment, source: Appointment[]) {
  const ids = new Set([seed.id]);
  let changed = true;
  while (changed) {
    changed = false;
    for (const candidate of source) {
      if (ids.has(candidate.id)) continue;
      const candidateStart = minutesInBishkek(candidate.startTime);
      const candidateEnd = minutesInBishkek(candidate.endTime);
      const touchesGroup = source.some((member) => {
        if (!ids.has(member.id)) return false;
        const memberStart = minutesInBishkek(member.startTime);
        const memberEnd = minutesInBishkek(member.endTime);
        return candidateStart < memberEnd && candidateEnd > memberStart;
      });
      if (touchesGroup) {
        ids.add(candidate.id);
        changed = true;
      }
    }
  }
  return source
    .filter((item) => ids.has(item.id))
    .sort(
      (a, b) => a.startTime.localeCompare(b.startTime) || a.id - b.id,
    );
}

function minutesInBishkek(value: string) {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Bishkek",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(value));
  return (
    Number(parts.find((p) => p.type === "hour")?.value) * 60 +
    Number(parts.find((p) => p.type === "minute")?.value)
  );
}
function statusLabel(status: Appointment["status"]) {
  return {
    SCHEDULED: "Запланирован",
    IN_PROGRESS: "На приёме",
    COMPLETED: "Завершён",
    CANCELLED: "Отменён",
    NO_SHOW: "Не пришёл",
  }[status];
}

function AppointmentList({ myDay }: { myDay: boolean }) {
  const [date, setDate] = useState(clinicToday);
  const navigate = useNavigate();
  const range = clinicDayRange(date);
  const query = useQuery({
    queryKey: ["appointments", date],
    queryFn: () =>
      api<Appointment[]>(
        `/api/appointments?from=${encodeURIComponent(range.from)}&to=${encodeURIComponent(range.to)}`,
      ),
  });
  return (
    <>
      <PageHeader
        title={myDay ? "Мой день" : "Календарь приёмов"}
        description={
          myDay
            ? "Ваши приёмы на выбранный день"
            : "Расписание на выбранный день"
        }
        actions={
          <ClinicDatePicker
            value={date}
            onChange={setDate}
            label={formatClinicDay(date, true)}
          />
        }
      />
      {query.isLoading ? (
        <LoadingState />
      ) : query.error ? (
        <ErrorState error={query.error} />
      ) : !query.data?.length ? (
        <EmptyState
          title="Приёмов нет"
          text="На выбранную дату ничего не запланировано"
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="hidden grid-cols-[90px_1.3fr_1fr_140px_120px_120px_130px] gap-4 border-b border-slate-200 bg-slate-50 px-5 py-3 text-xs font-semibold uppercase text-slate-500 md:grid">
            <span>Время</span>
            <span>Пациент</span>
            <span>Врач</span>
            <span>Статус</span>
            <span className="text-right">Сумма</span>
            <span className="text-right">Оплачено</span>
            <span className="text-right">Остаток</span>
          </div>
          {query.data.map((item) => (
            <button
              key={item.id}
              onClick={() => navigate(`/appointments/${item.id}`)}
              className="grid w-full gap-3 border-b border-slate-100 px-4 py-4 text-left last:border-0 hover:bg-slate-50 md:grid-cols-[90px_1.3fr_1fr_140px_120px_120px_130px] md:items-center md:gap-4 md:px-5"
            >
              <span className="font-semibold text-slate-950">
                {formatTime(item.startTime)}
              </span>
              <span className="font-medium text-slate-900">
                {item.patientName}
              </span>
              <span className="text-sm text-slate-600">{item.doctorName}</span>
              <StatusBadge status={item.status} />
              <span className="text-right text-sm">
                {formatMoney(item.servicesTotal)}
              </span>
              <span className="text-right text-sm">
                {formatMoney(item.paidTotal)}
              </span>
              <span className="text-right text-sm font-semibold">
                {formatMoney(item.remainingBalance)}
              </span>
            </button>
          ))}
        </Card>
      )}
    </>
  );
}
