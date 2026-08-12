export type Role = "ADMIN" | "DOCTOR";
export type AppointmentStatus = "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type PaymentMethod = "CASH" | "CARD" | "QR";

export interface AuthUser { id: number; username: string; fullName: string; role: Role }
export interface ApiErrorBody { timestamp: string; status: number; error: string; message: string; path: string; validationErrors?: Record<string, string> }
export interface DashboardSummary { from: string; to: string; totalPatients: number; appointments: number; completedAppointments: number; cancelledAppointments: number; clinicRevenue: number; cashRevenue: number; cardRevenue: number; qrRevenue: number }
export interface AppointmentServiceItem { id: number; serviceId: number; serviceName: string; quantity: number; unitPrice: number; lineTotal: number }
export interface AppointmentPayment { id: number; amount: number; paymentMethod: PaymentMethod; paidAt: string }
export interface Appointment { id: number; patientId: number; patientName: string; doctorId: number; doctorName: string; startTime: string; endTime: string; status: AppointmentStatus; notes: string | null; createdAt: string; createdBy: string; services: AppointmentServiceItem[]; servicesTotal: number; payments: AppointmentPayment[]; paidTotal: number; remainingBalance: number }
export interface Patient { id: number; fullName: string; phone: string; birthDate: string | null; notes: string | null; createdAt: string }
export interface PatientInput { fullName: string; phone: string; birthDate: string | null; notes: string | null }
export interface Doctor { id: number; userId: number; username: string; fullName: string; specialization: string | null; phone: string | null; active: boolean }
export interface ClinicService { id: number; name: string; price: number; durationMinutes: number; active: boolean }
export interface PaymentInput { appointmentId: number; amount: number; paymentMethod: PaymentMethod; paidAt: string }
