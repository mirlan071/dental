import type { PaymentMethod } from "../types/api";
export const paymentMethodLabels: Record<PaymentMethod, string> = { CASH: "Наличные", CARD: "Карта", QR: "QR" };
