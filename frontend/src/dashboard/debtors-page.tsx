import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateTime, formatMoney } from "../lib/utils";
import type { DebtorSummary } from "../types/api";
import { PageHeader } from "../components/page-header";
import { Card } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { EmptyState, ErrorState, LoadingState } from "../components/feedback";

export function DebtorsPage() {
  const [search, setSearch] = useState("");
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ["dashboard", "debtors", search],
    queryFn: () =>
      api<DebtorSummary[]>(
        `/api/dashboard/debtors${search.trim() ? `?search=${encodeURIComponent(search.trim())}` : ""}`,
      ),
  });
  return (
    <>
      <PageHeader
        title="Должники"
        description="Пациенты с неоплаченными завершёнными приёмами"
        actions={
          <Input
            className="w-72"
            placeholder="Имя или телефон"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        }
      />
      {query.isLoading ? (
        <LoadingState />
      ) : query.error ? (
        <ErrorState error={query.error} />
      ) : query.data?.length ? (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                {[
                  "Пациент",
                  "Телефон",
                  "Врач / Врачи",
                  "Стоимость лечения",
                  "Оплачено",
                  "Долг",
                  "Последнее лечение",
                ].map((h) => (
                  <th className="px-5 py-3" key={h}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {query.data.map((d) => (
                <tr
                  key={d.patientId}
                  onClick={() => navigate(`/patients/${d.patientId}`)}
                  className="cursor-pointer border-t border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-5 py-4 font-medium">{d.patientFullName}</td>
                  <td className="px-5 py-4">{d.phone}</td>
                  <td className="px-5 py-4">
                    {d.doctors.map((x) => x.fullName).join(", ")}
                  </td>
                  <td className="px-5 py-4">
                    {formatMoney(d.totalTreatmentAmount)}
                  </td>
                  <td className="px-5 py-4">{formatMoney(d.totalPaid)}</td>
                  <td className="px-5 py-4 font-semibold text-amber-700">
                    {formatMoney(d.totalDebt)}
                  </td>
                  <td className="px-5 py-4">
                    {formatDateTime(d.lastTreatmentDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      ) : (
        <EmptyState
          title="Должников нет"
          text="Нет завершённых приёмов с остатком долга"
        />
      )}
    </>
  );
}
