import { Navigate, createBrowserRouter } from "react-router-dom";
import { AppShell } from "./app-shell";
import { ProtectedRoute } from "../auth/protected-route";
import { LoginPage } from "../auth/login-page";
import { DashboardPage } from "../dashboard/dashboard-page";
import { DebtorsPage } from "../dashboard/debtors-page";
import { DoctorDashboardPage } from "../dashboard/doctor-dashboard-page";
import { AppointmentsPage } from "../appointments/appointments-page";
import { AppointmentDetailsPage } from "../appointments/appointment-details-page";
import { PatientsPage } from "../patients/patients-page";
import { PatientDetailsPage } from "../patients/patient-details-page";
import { DoctorsPage } from "../doctors/doctors-page";
import { ServicesPage } from "../services/services-page";
import { useAuth } from "../auth/auth-context";

function HomeRedirect() {
  const { user } = useAuth();
  return (
    <Navigate to={user?.role === "ADMIN" ? "/dashboard" : "/my-day"} replace />
  );
}
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <HomeRedirect /> },
          { path: "appointments", element: <AppointmentsPage /> },
          { path: "appointments/:id", element: <AppointmentDetailsPage /> },
          { path: "patients", element: <PatientsPage /> },
          { path: "patients/:id", element: <PatientDetailsPage /> },
          {
            element: <ProtectedRoute roles={["ADMIN"]} />,
            children: [
              { path: "dashboard", element: <DashboardPage /> },
              { path: "debtors", element: <DebtorsPage /> },
              { path: "doctors", element: <DoctorsPage /> },
              { path: "services", element: <ServicesPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={["DOCTOR"]} />,
            children: [{ path: "my-day", element: <DoctorDashboardPage /> }],
          },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/" replace /> },
]);
