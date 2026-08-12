import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "../components/page-header";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { api, errorMessage } from "../lib/api";
import type { ClinicSettings } from "../types/api";
import { clinicSettingsQueryKey, useClinicSettings } from "./clinic-settings";
import { LoadingState, ErrorState } from "../components/feedback";
import { SuccessToast } from "../components/toast";

export function SettingsPage() {
  const query = useClinicSettings();
  const client = useQueryClient();
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [saved, setSaved] = useState(false);
  useEffect(() => {
    if (query.data) {
      setStart(query.data.workdayStart.slice(0, 5));
      setEnd(query.data.workdayEnd.slice(0, 5));
    }
  }, [query.data]);
  const mutation = useMutation({
    mutationFn: () =>
      api<ClinicSettings>("/api/settings/clinic", {
        method: "PUT",
        body: JSON.stringify({ workdayStart: start, workdayEnd: end }),
      }),
    onSuccess: async (settings) => {
      client.setQueryData(clinicSettingsQueryKey, settings);
      await client.invalidateQueries({ queryKey: ["appointments"] });
      setSaved(true);
    },
  });
  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }
  return (
    <>
      <PageHeader
        title="Настройки клиники"
        description="Общие параметры работы клиники"
      />
      {query.isLoading ? (
        <LoadingState />
      ) : query.error ? (
        <ErrorState error={query.error} />
      ) : (
        <Card className="max-w-2xl p-5 sm:p-6">
          <h2 className="text-lg font-semibold text-slate-950">
            Рабочее время
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Используется календарём и проверкой новых записей.
          </p>
          <form onSubmit={submit} className="mt-6 space-y-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <TimeField
                label="Начало рабочего дня"
                value={start}
                onChange={setStart}
              />
              <TimeField
                label="Конец рабочего дня"
                value={end}
                onChange={setEnd}
              />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-700">Часовой пояс</p>
              <p className="mt-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-700">
                {query.data?.timezone}
              </p>
            </div>
            {mutation.error && (
              <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
                {errorMessage(mutation.error)}
              </p>
            )}
            <Button
              disabled={!start || !end || start >= end || mutation.isPending}
            >
              {mutation.isPending ? "Сохраняем…" : "Сохранить"}
            </Button>
          </form>
        </Card>
      )}
      {saved && (
        <SuccessToast
          message="Настройки сохранены"
          onClose={() => setSaved(false)}
        />
      )}
    </>
  );
}

function TimeField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-medium text-slate-700">
      {label}
      <input
        type="time"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 h-12 w-full rounded-lg border border-slate-300 bg-white px-3 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-100"
        required
      />
    </label>
  );
}
