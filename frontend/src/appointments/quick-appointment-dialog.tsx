import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Search, Trash2, UserPlus } from "lucide-react";
import { Dialog } from "../components/ui/dialog";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { api, ApiError, errorMessage } from "../lib/api";
import { formatMoney } from "../lib/utils";
import type { Appointment, AppointmentCreateInput, ClinicService, Doctor, Patient, PatientInput } from "../types/api";
import { CALENDAR_CONFIG } from "./calendar-config";

type InitialValues = { doctorId?: number; date: string; time?: string };
type SelectedService = { service: ClinicService; quantity: number };

export function QuickAppointmentDialog({open,onOpenChange,initial,onCreated}:{open:boolean;onOpenChange:(open:boolean)=>void;initial:InitialValues;onCreated:(appointment:Appointment)=>void}) {
  const client=useQueryClient();
  const[patientSearch,setPatientSearch]=useState("");
  const[selectedPatient,setSelectedPatient]=useState<Patient|null>(null);
  const[doctorId,setDoctorId]=useState("");
  const[date,setDate]=useState(initial.date);
  const[time,setTime]=useState(initial.time??CALENDAR_CONFIG.workdayStart);
  const[selectedServices,setSelectedServices]=useState<SelectedService[]>([]);
  const[serviceId,setServiceId]=useState("");
  const[creatingPatient,setCreatingPatient]=useState(false);

  useEffect(()=>{if(open){setDoctorId(initial.doctorId?String(initial.doctorId):"");setDate(initial.date);setTime(initial.time??CALENDAR_CONFIG.workdayStart)}},[open,initial]);

  const doctors=useQuery({queryKey:["doctors"],queryFn:()=>api<Doctor[]>("/api/doctors"),enabled:open});
  const services=useQuery({queryKey:["services","active"],queryFn:()=>api<ClinicService[]>("/api/services?activeOnly=true"),enabled:open});
  const patients=useQuery({queryKey:["patients","booking",patientSearch],queryFn:()=>api<Patient[]>(`/api/patients?search=${encodeURIComponent(patientSearch.trim())}`),enabled:open&&patientSearch.trim().length>0});

  const duration=selectedServices.length?selectedServices.reduce((sum,item)=>sum+item.service.durationMinutes*item.quantity,0):CALENDAR_CONFIG.fallbackDurationMinutes;
  const start=useMemo(()=>new Date(`${date}T${time}:00+06:00`),[date,time]);
  const end=useMemo(()=>new Date(start.getTime()+duration*60_000),[start,duration]);
  const estimatedTotal=selectedServices.reduce((sum,item)=>sum+item.service.price*item.quantity,0);

  const createAppointment=useMutation({mutationFn:(input:AppointmentCreateInput)=>api<Appointment>("/api/appointments",{method:"POST",body:JSON.stringify(input)}),onSuccess:async appointment=>{await Promise.all([client.invalidateQueries({queryKey:["appointments"]}),client.invalidateQueries({queryKey:["dashboard"]})]);onCreated(appointment);reset();onOpenChange(false)}});

  function reset(){setPatientSearch("");setSelectedPatient(null);setSelectedServices([]);setServiceId("");setCreatingPatient(false)}
  function close(next:boolean){if(!next&&!createAppointment.isPending)reset();onOpenChange(next)}
  function addService(){const service=services.data?.find(item=>item.id===Number(serviceId));if(!service)return;setSelectedServices(current=>current.some(item=>item.service.id===service.id)?current:current.concat({service,quantity:1}));setServiceId("")}
  function quantity(id:number,delta:number){setSelectedServices(current=>current.map(item=>item.service.id===id?{...item,quantity:Math.max(1,item.quantity+delta)}:item))}
  function submit(event:FormEvent){event.preventDefault();if(!selectedPatient||!doctorId)return;createAppointment.mutate({patientId:selectedPatient.id,doctorId:Number(doctorId),startTime:start.toISOString(),endTime:end.toISOString(),notes:null,services:selectedServices.map(item=>({serviceId:item.service.id,quantity:item.quantity}))})}
  const conflict=createAppointment.error instanceof ApiError&&createAppointment.error.status===409;

  return <Dialog open={open} onOpenChange={close} title="Новая запись" className="max-w-3xl">
    <form onSubmit={submit} className="grid gap-6 md:grid-cols-[1fr_1fr]">
      <div className="space-y-5">
        <section><label className="text-sm font-medium text-slate-700">Пациент</label>{selectedPatient?<div className="mt-2 flex items-center justify-between rounded-lg border border-brand-200 bg-brand-50 px-3 py-2.5"><div><p className="text-sm font-medium text-slate-900">{selectedPatient.fullName}</p><p className="text-xs text-slate-500">{selectedPatient.phone}</p></div><Button type="button" variant="ghost" size="sm" onClick={()=>setSelectedPatient(null)}>Изменить</Button></div>:creatingPatient?<InlinePatientForm onCreated={patient=>{setSelectedPatient(patient);setCreatingPatient(false);setPatientSearch("")}} onCancel={()=>setCreatingPatient(false)}/>:<div className="relative mt-2"><Search className="absolute left-3 top-3 text-slate-400" size={17}/><Input className="pl-9" placeholder="Имя или телефон" value={patientSearch} onChange={e=>setPatientSearch(e.target.value)}/>{patientSearch&&<div className="absolute z-20 mt-1 max-h-52 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white p-1 shadow-lg">{patients.isLoading&&<p className="px-3 py-2 text-sm text-slate-500">Ищем…</p>}{patients.data?.map(patient=><button type="button" key={patient.id} onClick={()=>{setSelectedPatient(patient);setPatientSearch("")}} className="block w-full rounded-md px-3 py-2 text-left hover:bg-slate-50"><span className="block text-sm font-medium text-slate-800">{patient.fullName}</span><span className="text-xs text-slate-500">{patient.phone}</span></button>)}{patients.data?.length===0&&<div className="p-2"><p className="px-1 pb-2 text-sm text-slate-500">Пациент не найден</p><Button type="button" variant="secondary" size="sm" className="w-full" onClick={()=>setCreatingPatient(true)}><UserPlus size={16}/>Создать пациента</Button></div>}</div>}</div>}</section>
        <section className="grid grid-cols-2 gap-3"><label className="text-sm font-medium text-slate-700">Врач<select className="mt-2 h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-100" value={doctorId} onChange={e=>setDoctorId(e.target.value)} required><option value="">Выберите</option>{doctors.data?.filter(d=>d.active).map(doctor=><option key={doctor.id} value={doctor.id}>{doctor.fullName}</option>)}</select></label><label className="text-sm font-medium text-slate-700">Дата<Input className="mt-2" type="date" value={date} onChange={e=>setDate(e.target.value)} required/></label><label className="text-sm font-medium text-slate-700">Время<Input className="mt-2" type="time" step={CALENDAR_CONFIG.slotMinutes*60} value={time} onChange={e=>setTime(e.target.value)} required/></label><div className="rounded-lg bg-slate-100 px-3 py-2"><p className="text-xs text-slate-500">Продолжительность</p><p className="mt-1 text-sm font-semibold text-slate-800">{time} → {new Intl.DateTimeFormat("ru-RU",{hour:"2-digit",minute:"2-digit",timeZone:"Asia/Bishkek"}).format(end)}</p><p className="text-xs text-slate-500">{duration} мин</p></div></section>
      </div>
      <div className="space-y-5"><section><label className="text-sm font-medium text-slate-700">Услуги</label><div className="mt-2 flex gap-2"><select className="h-10 min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 text-sm outline-none focus:border-brand-600" value={serviceId} onChange={e=>setServiceId(e.target.value)}><option value="">Выберите услугу</option>{services.data?.filter(service=>!selectedServices.some(item=>item.service.id===service.id)).map(service=><option key={service.id} value={service.id}>{service.name} · {formatMoney(service.price)}</option>)}</select><Button type="button" variant="secondary" onClick={addService} disabled={!serviceId}><Plus size={17}/></Button></div><div className="mt-3 space-y-2">{selectedServices.map(item=><div key={item.service.id} className="rounded-lg border border-slate-200 p-3"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-medium text-slate-800">{item.service.name}</p><p className="mt-0.5 text-xs text-slate-500">{formatMoney(item.service.price)} × {item.quantity}</p></div><button type="button" onClick={()=>setSelectedServices(current=>current.filter(entry=>entry.service.id!==item.service.id))} className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label="Удалить услугу"><Trash2 size={16}/></button></div><div className="mt-2 flex items-center justify-between"><div className="flex items-center rounded-md border border-slate-200"><button type="button" onClick={()=>quantity(item.service.id,-1)} className="p-1.5 text-slate-500"><Minus size={14}/></button><span className="min-w-8 text-center text-sm font-medium">{item.quantity}</span><button type="button" onClick={()=>quantity(item.service.id,1)} className="p-1.5 text-slate-500"><Plus size={14}/></button></div><p className="text-sm font-semibold text-slate-900">{formatMoney(item.service.price*item.quantity)}</p></div></div>)}{!selectedServices.length&&<p className="rounded-lg border border-dashed border-slate-300 px-3 py-5 text-center text-sm text-slate-500">Можно создать запись без услуги и добавить её позже</p>}</div></section><div className="rounded-xl bg-slate-900 p-4 text-white"><div className="flex items-center justify-between"><span className="text-sm text-slate-300">Предварительная сумма</span><span className="text-xl font-semibold">{formatMoney(estimatedTotal)}</span></div></div></div>
      {createAppointment.error&&<div className="md:col-span-2 rounded-lg bg-red-50 p-3 text-sm text-red-700">{conflict?"Это время уже занято у выбранного врача.":errorMessage(createAppointment.error)}</div>}
      <div className="flex justify-end gap-2 border-t border-slate-200 pt-4 md:col-span-2"><Button type="button" variant="secondary" onClick={()=>close(false)}>Отмена</Button><Button disabled={!selectedPatient||!doctorId||createAppointment.isPending}>{createAppointment.isPending?"Создаём…":"Создать запись"}</Button></div>
    </form>
  </Dialog>
}

function InlinePatientForm({onCreated,onCancel}:{onCreated:(patient:Patient)=>void;onCancel:()=>void}){const[name,setName]=useState("");const[phone,setPhone]=useState("");const mutation=useMutation({mutationFn:(input:PatientInput)=>api<Patient>("/api/patients",{method:"POST",body:JSON.stringify(input)}),onSuccess:onCreated});return <div className="mt-2 rounded-lg border border-slate-200 bg-slate-50 p-3"><p className="mb-3 text-sm font-medium text-slate-800">Новый пациент</p><div className="space-y-2"><Input placeholder="ФИО" value={name} onChange={e=>setName(e.target.value)} autoFocus/><Input type="tel" placeholder="Телефон" value={phone} onChange={e=>setPhone(e.target.value)}/></div>{mutation.error&&<p className="mt-2 text-xs text-red-600">{errorMessage(mutation.error)}</p>}<div className="mt-3 flex justify-end gap-2"><Button type="button" variant="ghost" size="sm" onClick={onCancel}>Назад</Button><Button type="button" size="sm" disabled={!name.trim()||!phone.trim()||mutation.isPending} onClick={()=>mutation.mutate({fullName:name.trim(),phone:phone.trim(),birthDate:null,notes:null})}>{mutation.isPending?"Создаём…":"Создать и выбрать"}</Button></div></div>}
