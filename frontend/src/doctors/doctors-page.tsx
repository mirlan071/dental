import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, KeyRound, UserPlus } from "lucide-react";
import { api, errorMessage } from "../lib/api";
import type {
  Doctor,
  DoctorCreateInput,
  DoctorUpdateInput,
} from "../types/api";
import { PageHeader } from "../components/page-header";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Dialog } from "../components/ui/dialog";
import { Input } from "../components/ui/input";
import { EmptyState, ErrorState, LoadingState } from "../components/feedback";

export function DoctorsPage() {
  const query = useQuery({
    queryKey: ["doctors"],
    queryFn: () => api<Doctor[]>("/api/doctors"),
  });
  const [form, setForm] = useState<{
    mode: "create" | "edit";
    doctor?: Doctor;
  } | null>(null);
  const [reset, setReset] = useState<Doctor | null>(null);
  return (
    <>
      <PageHeader
        title="Врачи"
        description="Учётные записи и профили врачей"
        actions={
          <Button onClick={() => setForm({ mode: "create" })}>
            <UserPlus size={17} />
            Новый врач
          </Button>
        }
      />
      {query.isLoading ? (
        <LoadingState />
      ) : query.error ? (
        <ErrorState error={query.error} />
      ) : !query.data?.length ? (
        <EmptyState
          title="Врачей нет"
          text="Добавьте первую учётную запись врача"
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {query.data.map((doctor) => (
            <DoctorCard
              key={doctor.id}
              doctor={doctor}
              edit={() => setForm({ mode: "edit", doctor })}
              reset={() => setReset(doctor)}
            />
          ))}
        </div>
      )}
      <DoctorDialog
        key={`${form?.mode ?? "closed"}-${form?.doctor?.id ?? 0}`}
        state={form}
        close={() => setForm(null)}
      />
      <PasswordDialog
        key={reset?.id ?? "closed"}
        doctor={reset}
        close={() => setReset(null)}
      />
    </>
  );
}

function DoctorCard({
  doctor,
  edit,
  reset,
}: {
  doctor: Doctor;
  edit: () => void;
  reset: () => void;
}) {
  const client = useQueryClient();
  const [confirmDeactivate, setConfirmDeactivate] = useState(false);
  const mutation = useMutation({
    mutationFn: () =>
      api<Doctor>(`/api/doctors/${doctor.id}/active`, {
        method: "PATCH",
        body: JSON.stringify({ active: !doctor.active }),
      }),
    onSuccess: () => client.invalidateQueries({ queryKey: ["doctors"] }),
  });
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="font-semibold text-slate-900">{doctor.fullName}</h2>
          <p className="mt-1 text-sm text-slate-500">
            {doctor.specialization || "Специализация не указана"}
          </p>
        </div>
        <Status active={doctor.active} />
      </div>
      <div className="mt-4 space-y-1 text-sm text-slate-600">
        <p>{doctor.phone || "Телефон не указан"}</p>
        <p className="text-slate-400">@{doctor.username}</p>
      </div>
      <div className="mt-5 flex flex-wrap gap-2">
        <Button size="sm" variant="secondary" onClick={edit}>
          <Pencil size={15} />
          Редактировать
        </Button>
        <Button size="sm" variant="secondary" onClick={reset}>
          <KeyRound size={15} />
          Сбросить пароль
        </Button>
        <Button
          size="sm"
          variant="secondary"
          disabled={mutation.isPending}
          onClick={() =>
            doctor.active ? setConfirmDeactivate(true) : mutation.mutate()
          }
        >
          {doctor.active ? "Деактивировать" : "Активировать"}
        </Button>
      </div>
      {mutation.error && (
        <p className="mt-3 text-sm text-red-700">
          {errorMessage(mutation.error)}
        </p>
      )}
      <Dialog
        open={confirmDeactivate}
        onOpenChange={(open) => {
          if (!open && mutation.isPending) return;
          setConfirmDeactivate(open);
        }}
        title="Деактивировать врача?"
      >
        <p className="text-sm text-slate-600">
          <span className="font-medium text-slate-900">{doctor.fullName}</span>{" "}
          больше не сможет войти в систему и не будет доступен для новых
          записей.
        </p>
        <p className="mt-3 text-sm text-slate-600">
          История приёмов и финансовые данные сохранятся.
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button
            variant="secondary"
            disabled={mutation.isPending}
            onClick={() => setConfirmDeactivate(false)}
          >
            Отмена
          </Button>
          <Button
            variant="danger"
            disabled={mutation.isPending}
            onClick={() =>
              mutation.mutate(undefined, {
                onSuccess: () => setConfirmDeactivate(false),
              })
            }
          >
            Деактивировать
          </Button>
        </div>
      </Dialog>
    </Card>
  );
}
function Status({ active }: { active: boolean }) {
  return (
    <span
      className={
        active
          ? "rounded-full bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700"
          : "rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-600"
      }
    >
      {active ? "Активен" : "Неактивен"}
    </span>
  );
}

function DoctorDialog({
  state,
  close,
}: {
  state: { mode: "create" | "edit"; doctor?: Doctor } | null;
  close: () => void;
}) {
  const client = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  if (!state) return null;
  return (
    <Dialog
      open
      onOpenChange={(open) => !open && !pending && close()}
      title={state.mode === "create" ? "Новый врач" : "Редактировать врача"}
    >
      <DoctorForm
        key={`${state.mode}-${state.doctor?.id ?? 0}`}
        state={state}
        error={error}
        pending={pending}
        setPending={setPending}
        submit={async (input) => {
          setError(null);
          try {
            if (state.mode === "create")
              await api<Doctor>("/api/doctors", {
                method: "POST",
                body: JSON.stringify(input),
              });
            else
              await api<Doctor>(`/api/doctors/${state.doctor!.id}`, {
                method: "PUT",
                body: JSON.stringify(input),
              });
            await client.invalidateQueries({ queryKey: ["doctors"] });
            close();
          } catch (e) {
            setError(errorMessage(e));
          }
        }}
        close={close}
      />
    </Dialog>
  );
}
function DoctorForm({
  state,
  submit,
  close,
  error,
  pending,
  setPending,
}: {
  state: { mode: "create" | "edit"; doctor?: Doctor };
  submit: (input: DoctorCreateInput | DoctorUpdateInput) => Promise<void>;
  close: () => void;
  error: string | null;
  pending: boolean;
  setPending: (pending: boolean) => void;
}) {
  const d = state.doctor;
  const [fullName, setFullName] = useState(d?.fullName ?? "");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [specialization, setSpecialization] = useState(d?.specialization ?? "");
  const [phone, setPhone] = useState(d?.phone ?? "");
  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setPending(true);
    const common = {
      fullName,
      specialization: specialization || null,
      phone: phone || null,
    };
    try {
      await submit(
        state.mode === "create" ? { ...common, username, password } : common,
      );
    } finally {
      setPending(false);
    }
  }
  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <Field label="ФИО" value={fullName} set={setFullName} required />
      {state.mode === "create" && (
        <>
          <Field
            label="Имя пользователя"
            value={username}
            set={setUsername}
            required
          />
          <Field
            label="Начальный пароль"
            value={password}
            set={setPassword}
            type="password"
            minLength={8}
            required
          />
        </>
      )}
      <Field
        label="Специализация"
        value={specialization}
        set={setSpecialization}
      />
      <Field label="Телефон" value={phone} set={setPhone} />
      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>
      )}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="secondary"
          disabled={pending}
          onClick={close}
        >
          Отмена
        </Button>
        <Button disabled={pending}>
          {pending ? "Сохраняем…" : "Сохранить"}
        </Button>
      </div>
    </form>
  );
}
function Field({
  label,
  value,
  set,
  type = "text",
  required = false,
  minLength,
}: {
  label: string;
  value: string;
  set: (v: string) => void;
  type?: string;
  required?: boolean;
  minLength?: number;
}) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <Input
        className="mt-2"
        type={type}
        value={value}
        onChange={(e) => set(e.target.value)}
        required={required}
        minLength={minLength}
      />
    </label>
  );
}
function PasswordDialog({
  doctor,
  close,
}: {
  doctor: Doctor | null;
  close: () => void;
}) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const mutation = useMutation({
    mutationFn: () =>
      api(`/api/doctors/${doctor!.id}/password`, {
        method: "PATCH",
        body: JSON.stringify({ password }),
      }),
    onSuccess: close,
    onError: (e) => setError(errorMessage(e)),
  });
  return (
    <Dialog
      open={Boolean(doctor)}
      onOpenChange={(open) => !open && !mutation.isPending && close()}
      title="Сбросить пароль"
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate();
        }}
      >
        <p className="mb-4 text-sm text-slate-500">
          Новый пароль для {doctor?.fullName}. Минимум 8 символов.
        </p>
        <Field
          label="Новый пароль"
          value={password}
          set={setPassword}
          type="password"
          minLength={8}
          required
        />
        {error && <p className="mt-3 text-sm text-red-700">{error}</p>}
        <div className="mt-6 flex justify-end gap-2">
          <Button type="button" variant="secondary" disabled={mutation.isPending} onClick={close}>
            Отмена
          </Button>
          <Button disabled={mutation.isPending || password.length < 8}>
            Сохранить пароль
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
