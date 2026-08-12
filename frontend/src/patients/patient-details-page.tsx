import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Calendar, FileText, Pencil, Phone } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { Dialog } from "../components/ui/dialog";
import { Input } from "../components/ui/input";
import { EmptyState, ErrorState, LoadingState } from "../components/feedback";
import { StatusBadge } from "../components/status-badge";
import { api, errorMessage } from "../lib/api";
import { useAuth } from "../auth/auth-context";
import { formatDateTime, formatMoney } from "../lib/utils";
import type { Patient, PatientDebtDetails, PatientInput } from "../types/api";

export function PatientDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [editing, setEditing] = useState(false);
  const patient = useQuery({
    queryKey: ["patient", id],
    queryFn: () => api<Patient>(`/api/patients/${id}`),
    enabled: Boolean(id),
  });
  const debt = useQuery({
    queryKey: ["dashboard", "patient-debt", id],
    queryFn: () => api<PatientDebtDetails>(`/api/dashboard/debtors/${id}`),
    enabled: Boolean(id) && user?.role === "ADMIN",
    retry: false,
  });
  if (patient.isLoading || (user?.role === "ADMIN" && debt.isLoading)) return <LoadingState />;
  if (patient.error || !patient.data)
    return <ErrorState error={patient.error} />;
  const p = patient.data;
  return (
    <>
      <Button variant="ghost" onClick={() => navigate(-1)}>
        <ArrowLeft size={17} />
        Назад
      </Button>
      <div className="mt-5">
        <div className="flex items-start justify-between gap-4"><h1 className="text-2xl font-semibold text-slate-950">{p.fullName}</h1>{user?.role==="ADMIN"&&<Button variant="secondary" onClick={()=>setEditing(true)}><Pencil size={16}/>Редактировать пациента</Button>}</div>
        <p className="mt-1 text-sm text-slate-500">Карточка пациента №{p.id}</p>
        <div className="mt-6 grid gap-6 xl:grid-cols-[360px_1fr]">
          <Card className="h-fit divide-y divide-slate-100">
            {[
              { icon: Phone, label: "Телефон", value: p.phone },
              {
                icon: Calendar,
                label: "Дата рождения",
                value: p.birthDate
                  ? new Intl.DateTimeFormat("ru-RU").format(
                      new Date(`${p.birthDate}T00:00:00`),
                    )
                  : "Не указана",
              },
              {
                icon: FileText,
                label: "Примечания",
                value: p.notes || "Нет примечаний",
              },
            ].map(({ icon: Icon, label, value }) => (
              <div className="flex gap-4 p-5" key={label}>
                <Icon className="text-slate-500" size={18} />
                <div>
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className="mt-1 whitespace-pre-wrap text-sm font-medium">
                    {value}
                  </p>
                </div>
              </div>
            ))}
          </Card>
          {user?.role === "ADMIN" && debt.data ? (
            <div className="space-y-6">
              <Card>
                <div className="border-b border-slate-200 px-5 py-4">
                  <h2 className="font-semibold">Финансы пациента</h2>
                </div>
                <div className="grid divide-y divide-slate-100 sm:grid-cols-3 sm:divide-x sm:divide-y-0">
                  {[
                    ["Общая стоимость лечения", debt.data.totalTreatmentAmount],
                    ["Оплачено", debt.data.totalPaid],
                    ["Остаток долга", debt.data.totalDebt],
                  ].map(([label, value]) => (
                    <div className="p-5" key={String(label)}>
                      <p className="text-sm text-slate-500">{label}</p>
                      <p
                        className={
                          String(label) === "Остаток долга" && Number(value) > 0
                            ? "mt-2 text-xl font-semibold text-amber-700"
                            : "mt-2 text-xl font-semibold"
                        }
                      >
                        {formatMoney(Number(value))}
                      </p>
                    </div>
                  ))}
                </div>
              </Card>
              <Card>
                <div className="border-b border-slate-200 px-5 py-4">
                  <h2 className="font-semibold">Приёмы с задолженностью</h2>
                </div>
                {debt.data.appointments.length ? (
                  <div className="divide-y divide-slate-100">
                    {debt.data.appointments.map((a) => (
                      <button
                        key={a.appointmentId}
                        onClick={() =>
                          navigate(`/appointments/${a.appointmentId}`)
                        }
                        className="flex w-full flex-col gap-3 px-5 py-4 text-left hover:bg-slate-50"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <span className="font-medium">
                            {formatDateTime(a.date)} · {a.doctorName}
                          </span>
                          <StatusBadge status={a.status} />
                        </div>
                        <p className="text-sm text-slate-500">
                          {a.services.join(", ")}
                        </p>
                        <div className="flex flex-wrap gap-5 text-sm">
                          <span>
                            Стоимость: <b>{formatMoney(a.appointmentTotal)}</b>
                          </span>
                          <span>
                            Оплачено: <b>{formatMoney(a.paid)}</b>
                          </span>
                          <span className="text-amber-700">
                            Долг: <b>{formatMoney(a.remainingBalance)}</b>
                          </span>
                        </div>
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="p-5">
                    <EmptyState
                      title="Оплачено полностью"
                      text="У пациента нет текущей задолженности"
                    />
                  </div>
                )}
              </Card>
            </div>
          ) : user?.role === "ADMIN" ? (
            <ErrorState error={debt.error} />
          ) : null}
        </div>
      </div>
      <PatientEditDialog patient={p} open={editing} close={()=>setEditing(false)}/>
    </>
  );
}

function PatientEditDialog({patient,open,close}:{patient:Patient;open:boolean;close:()=>void}){const client=useQueryClient();const[fullName,setFullName]=useState(patient.fullName);const[phone,setPhone]=useState(patient.phone);const[birthDate,setBirthDate]=useState(patient.birthDate??"");const[notes,setNotes]=useState(patient.notes??"");const mutation=useMutation({mutationFn:(input:PatientInput)=>api<Patient>(`/api/patients/${patient.id}`,{method:"PUT",body:JSON.stringify(input)}),onSuccess:async()=>{await Promise.all([client.invalidateQueries({queryKey:["patient",String(patient.id)]}),client.invalidateQueries({queryKey:["patients"]}),client.invalidateQueries({queryKey:["dashboard"]})]);close()}});function submit(e:FormEvent){e.preventDefault();mutation.mutate({fullName,phone,birthDate:birthDate||null,notes:notes||null})}return <Dialog open={open} onOpenChange={value=>!value&&close()} title="Редактировать пациента"><form onSubmit={submit} className="space-y-4"><label className="block text-sm font-medium text-slate-700">ФИО<Input className="mt-2" value={fullName} onChange={e=>setFullName(e.target.value)} required/></label><label className="block text-sm font-medium text-slate-700">Телефон<Input className="mt-2" value={phone} onChange={e=>setPhone(e.target.value)} required/></label><label className="block text-sm font-medium text-slate-700">Дата рождения<Input className="mt-2" type="date" value={birthDate} onChange={e=>setBirthDate(e.target.value)}/></label><label className="block text-sm font-medium text-slate-700">Примечания<textarea className="mt-2 min-h-28 w-full rounded-lg border border-slate-300 p-3 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-100" value={notes} onChange={e=>setNotes(e.target.value)}/></label>{mutation.error&&<p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errorMessage(mutation.error)}</p>}<div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={close}>Отмена</Button><Button disabled={mutation.isPending}>{mutation.isPending?"Сохраняем…":"Сохранить"}</Button></div></form></Dialog>}
