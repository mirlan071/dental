import { useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";
import type { ClinicSettings } from "../types/api";

export const clinicSettingsQueryKey = ["settings", "clinic"] as const;

export function useClinicSettings() {
  return useQuery({
    queryKey: clinicSettingsQueryKey,
    queryFn: () => api<ClinicSettings>("/api/settings/clinic"),
    staleTime: 5 * 60_000,
  });
}
