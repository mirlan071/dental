export type Role = "ADMIN" | "DOCTOR";
export type AppointmentStatus = "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type PaymentMethod = "CASH" | "CARD" | "QR";

export interface AuthUser { id: number; username: string; fullName: string; role: Role }
export interface ApiErrorBody { timestamp: string; status: number; error: string; message: string; path: string; validationErrors?: Record<string, string> }
export interface DoctorPerformance { doctorId:number; doctorFullName:string; patients:number; completedAppointments:number; servicesPerformed:number; paymentsReceived:number; outstandingAmount:number }
export interface DashboardSummary { from:string; to:string; totalPatients:number; appointments:number; completedAppointments:number; cancelledAppointments:number; clinicRevenue:number; cashRevenue:number; cardRevenue:number; qrRevenue:number; servicesPerformed:number; paymentsReceived:number; outstandingAmount:number; debtorCount:number; totalDebt:number; doctors:DoctorPerformance[] }
export interface DebtorDoctor { id:number; fullName:string }
export interface DebtorSummary { patientId:number; patientFullName:string; phone:string; totalTreatmentAmount:number; totalPaid:number; totalDebt:number; lastTreatmentDate:string; unpaidAppointments:number; doctors:DebtorDoctor[] }
export interface DebtAppointment { appointmentId:number; date:string; doctorId:number; doctorName:string; services:string[]; appointmentTotal:number; paid:number; remainingBalance:number; status:AppointmentStatus }
export interface PatientDebtDetails { patientId:number; patientFullName:string; phone:string; totalTreatmentAmount:number; totalPaid:number; totalDebt:number; appointments:DebtAppointment[] }
export interface DoctorDashboardSummary { from: string; to: string; patients: number; completedAppointments: number; revenue: number; averageCheck: number; payments: { cash: number; card: number; qr: number } }
export interface AppointmentServiceItem { id: number; serviceId: number; serviceName: string; quantity: number; unitPrice: number; lineTotal: number }
export interface AppointmentPayment { id: number; amount: number; paymentMethod: PaymentMethod; paidAt: string }
export interface Appointment { id: number; patientId: number; patientName: string; doctorId: number; doctorName: string; startTime: string; endTime: string; status: AppointmentStatus; notes: string | null; createdAt: string; createdBy: string; services: AppointmentServiceItem[]; servicesTotal: number; payments: AppointmentPayment[]; paidTotal: number; remainingBalance: number }
export interface Patient { id: number; fullName: string; phone: string; birthDate: string | null; notes: string | null; createdAt: string }
export interface PatientInput { fullName: string; phone: string; birthDate: string | null; notes: string | null }
export interface Doctor { id: number; userId: number; username: string; fullName: string; specialization: string | null; phone: string | null; active: boolean }
export interface DoctorCreateInput { fullName:string; username:string; password:string; specialization:string|null; phone:string|null }
export interface DoctorUpdateInput { fullName:string; specialization:string|null; phone:string|null }
export interface ClinicService { id: number; name: string; price: number; durationMinutes: number; active: boolean }
export interface ClinicServiceInput { name:string; price:number; durationMinutes:number; active:boolean }
export interface PaymentInput { appointmentId: number; amount: number; paymentMethod: PaymentMethod; paidAt: string }
export interface AppointmentCreateInput { patientId: number; doctorId: number; startTime: string; endTime: string; notes: string | null; services: { serviceId: number; quantity: number }[] }
