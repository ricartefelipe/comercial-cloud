const TOKEN_KEY = "cc_access_token";
const DEMO_MODE_KEY = "cc_demo_mode";

function setCookie(name: string, value: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; SameSite=Lax`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; path=/; max-age=0`;
}

export function saveToken(token: string): void {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(TOKEN_KEY, token);
  setCookie(TOKEN_KEY, token);
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(TOKEN_KEY);
}

export function clearToken(): void {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(TOKEN_KEY);
  deleteCookie(TOKEN_KEY);
}

export function enableDemoMode(): void {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(DEMO_MODE_KEY, "true");
  setCookie(DEMO_MODE_KEY, "true");
}

export function isDemoMode(): boolean {
  if (typeof window === "undefined") return false;
  return sessionStorage.getItem(DEMO_MODE_KEY) === "true";
}

export function clearDemoMode(): void {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(DEMO_MODE_KEY);
  deleteCookie(DEMO_MODE_KEY);
}

export function isAuthenticated(): boolean {
  return !!getToken() || isDemoMode();
}

export function clearSession(): void {
  clearToken();
  clearDemoMode();
}
