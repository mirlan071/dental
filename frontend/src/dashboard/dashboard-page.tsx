import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Banknote,
  CalendarDays,
  CheckCircle2,
  CircleDollarSign,
  CreditCard,
  QrCode,
  Stethoscope,
  Users,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { clinicPeriodRange, type DashboardPeriod } from "../lib/clinic-date";
import { cn, formatDateTime, formatMoney } from "../lib/utils";
import type { DashboardSummary, DebtorSummary } from "../types/api";
import { Card } from "../components/ui/card";
import { PageHeader } from "../components/page-header";
import { EmptyState, ErrorState, LoadingState } from "../components/feedback";

const periods: { value: DashboardPeriod; label: string }[] = [
  { value: "today", label: "Сегодня" },
  { value: "week", label: "Неделя" },
  { value: "month", label: "Месяц" },
];
export function DashboardPage() {
  const [period, setPeriod] = useState<DashboardPeriod>("today");
  const range = clinicPeriodRange(period);
  const summary = useQuery({
    queryKey: ["dashboard", "admin", period, range.from],
    queryFn: () =>
      api<DashboardSummary>(
        `/api/dashboard/summary?from=${encodeURIComponent(range.from)}&to=${encodeURIComponent(range.to)}`,
      ),
  });
  const debtors = useQuery({
    queryKey: ["dashboard", "debtors"],
    queryFn: () => api<DebtorSummary[]>("/api/dashboard/debtors"),
  });
  return (
    <>
      <PageHeader
        title="Обзор клиники"
        description="Работа клиники и текущее финансовое состояние"
        actions={
          <div className="flex rounded-lg border border-slate-200 bg-white p-1">
            {periods.map((item) => (
              <button
                key={item.value}
                onClick={() => setPeriod(item.value)}
                className={cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium",
                  period === item.value
                    ? "bg-slate-900 text-white"
                    : "text-slate-600 hover:bg-slate-50",
                )}
              >
                {item.label}
              </button>
            ))}
          </div>
        }
      />
      {summary.isLoading || debtors.isLoading ? (
        <LoadingState />
      ) : summary.error ? (
        <ErrorState error={summary.error} />
      ) : debtors.error ? (
        <ErrorState error={debtors.error} />
      ) : (
        summary.data && (
          <DashboardContent data={summary.data} debtors={debtors.data ?? []} />
        )
      )}
    </>
  );
}

function DashboardContent({
  data,
  debtors,
}: {
  data: DashboardSummary;
  debtors: DebtorSummary[];
}) {
  const navigate = useNavigate();
  const metrics = [
    { label: "Пациенты", value: String(data.totalPatients), icon: Users },
    { label: "Приёмы", value: String(data.appointments), icon: CalendarDays },
    {
      label: "Завершено",
      value: String(data.completedAppointments),
      icon: CheckCircle2,
    },
    {
      label: "Оказано услуг",
      value: formatMoney(data.servicesPerformed),
      icon: Stethoscope,
    },
    {
      label: "Получено оплат",
      value: formatMoney(data.paymentsReceived),
      icon: Banknote,
    },
  ];
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {metrics.map(({ label, value, icon: Icon }) => (
          <Metric key={label} label={label} value={value} icon={Icon} />
        ))}
      </div>
      <div>
        <p className="mb-3 text-sm font-medium text-slate-500">
          Текущее финансовое состояние
        </p>
        <div className="grid gap-4 sm:grid-cols-2">
          <Metric
            label="Должники"
            value={String(data.debtorCount)}
            icon={Users}
          />
          <Metric
            label="Общая задолженность"
            value={formatMoney(data.totalDebt)}
            icon={CircleDollarSign}
          />
        </div>
      </div>
      <Card>
        <Title
          title="Оплаты по способам"
          subtitle="Платежи по завершённым приёмам выбранного периода"
        />
        <div className="grid divide-y divide-slate-200 sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          {[
            { label: "Наличные", value: data.cashRevenue, icon: Banknote },
            { label: "Карта", value: data.cardRevenue, icon: CreditCard },
            { label: "QR", value: data.qrRevenue, icon: QrCode },
          ].map(({ label, value, icon: Icon }) => (
            <div className="flex items-center gap-4 p-5" key={label}>
              <Icon className="text-slate-500" size={20} />
              <div>
                <p className="text-sm text-slate-500">{label}</p>
                <p className="font-semibold">{formatMoney(value)}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>
      <Card>
        <Title
          title="Эффективность врачей"
          subtitle="По завершённым приёмам выбранного периода"
        />
        <Table
          headers={[
            "Врач",
            "Пациенты",
            "Завершено",
            "Оказано услуг",
            "Получено оплат",
            "Не оплачено",
          ]}
        >
          {data.doctors.map((d) => (
            <tr key={d.doctorId} className="border-t border-slate-100">
              <Cell strong>{d.doctorFullName}</Cell>
              <Cell>{d.patients}</Cell>
              <Cell>{d.completedAppointments}</Cell>
              <Cell>{formatMoney(d.servicesPerformed)}</Cell>
              <Cell>{formatMoney(d.paymentsReceived)}</Cell>
              <Cell debt={d.outstandingAmount > 0}>
                {formatMoney(d.outstandingAmount)}
              </Cell>
            </tr>
          ))}
        </Table>
      </Card>
      <Card>
        <div className="flex items-center justify-between">
          <Title title="Должники" subtitle="Наибольшая текущая задолженность" />
          <Link
            to="/debtors"
            className="mr-5 text-sm font-medium text-brand-700"
          >
            Все должники
          </Link>
        </div>
        {debtors.length ? (
          <Table
            headers={[
              "Пациент",
              "Телефон",
              "Врач / Врачи",
              "Стоимость лечения",
              "Оплачено",
              "Долг",
              "Последнее лечение",
            ]}
          >
            {debtors.slice(0, 5).map((d) => (
              <tr
                key={d.patientId}
                onClick={() => navigate(`/patients/${d.patientId}`)}
                className="cursor-pointer border-t border-slate-100 hover:bg-slate-50"
              >
                <Cell strong>{d.patientFullName}</Cell>
                <Cell>{d.phone}</Cell>
                <Cell>{d.doctors.map((x) => x.fullName).join(", ")}</Cell>
                <Cell>{formatMoney(d.totalTreatmentAmount)}</Cell>
                <Cell>{formatMoney(d.totalPaid)}</Cell>
                <Cell debt>{formatMoney(d.totalDebt)}</Cell>
                <Cell>{formatDateTime(d.lastTreatmentDate)}</Cell>
              </tr>
            ))}
          </Table>
        ) : (
          <div className="p-5">
            <EmptyState
              title="Задолженности нет"
              text="Все завершённые приёмы оплачены"
            />
          </div>
        )}
      </Card>
    </div>
  );
}
function Metric({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: string;
  icon: typeof Users;
}) {
  return (
    <Card className="p-5">
      <div className="flex justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-semibold">{value}</p>
        </div>
        <span className="grid size-10 place-items-center rounded-lg bg-brand-50 text-brand-700">
          <Icon size={20} />
        </span>
      </div>
    </Card>
  );
}
function Title({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="px-5 py-4">
      <h2 className="font-semibold text-slate-900">{title}</h2>
      <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
    </div>
  );
}
function Table({
  headers,
  children,
}: {
  headers: string[];
  children: React.ReactNode;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] text-sm">
        <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
          <tr>
            {headers.map((h) => (
              <th className="px-5 py-3" key={h}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}
function Cell({
  children,
  strong = false,
  debt = false,
}: {
  children: React.ReactNode;
  strong?: boolean;
  debt?: boolean;
}) {
  return (
    <td
      className={cn(
        "px-5 py-3.5",
        strong && "font-medium text-slate-900",
        debt && "font-semibold text-amber-700",
      )}
    >
      {children}
    </td>
  );
}
