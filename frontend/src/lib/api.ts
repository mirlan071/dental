import type { ApiErrorBody } from "../types/api";

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: ApiErrorBody | null,
    message: string,
  ) {
    super(message);
  }
}

let csrf: { token: string; headerName: string } | null = null;
const mutationMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);
const apiBaseUrl = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`;
}

async function csrfToken() {
  if (!csrf) {
    const response = await fetch(apiUrl("/api/auth/csrf"), {
      credentials: "include",
    });
    if (!response.ok) throw await toError(response);
    csrf = (await response.json()) as { token: string; headerName: string };
  }
  return csrf;
}

export function clearCsrfToken() {
  csrf = null;
}

async function toError(response: Response) {
  let body: ApiErrorBody | null = null;
  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    /* empty Spring Security response */
  }
  if (response.status === 401) {
    clearCsrfToken();
    window.dispatchEvent(new Event("auth:unauthorized"));
  }
  return new ApiError(
    response.status,
    body,
    body?.message ??
      (response.status === 401
        ? "Требуется вход в систему"
        : "Не удалось выполнить запрос"),
  );
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (mutationMethods.has(method)) {
    const token = await csrfToken();
    headers.set(token.headerName, token.token);
  }
  const response = await fetch(apiUrl(path), {
    ...options,
    method,
    headers,
    credentials: "include",
  });
  if (!response.ok) throw await toError(response);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    const fields = Object.values(error.body?.validationErrors ?? {});
    return fields.length ? fields.join(". ") : error.message;
  }
  return "Произошла непредвиденная ошибка";
}
