import {
  LOJA_ID as DEMO_LOJA_ID,
  TENANT_ID as DEMO_TENANT_ID,
  USER_ID as DEMO_USER_ID,
} from "@/lib/constants";

const TENANT_KEY = "cc_tenant_id";
const USER_KEY = "cc_user_id";
const LOJA_KEY = "cc_loja_id";
const DISPLAY_NAME_KEY = "cc_display_name";

export interface WorkspaceContext {
  tenantId: string;
  userId: string;
  lojaId: string;
  displayName?: string;
}

function readStorage(key: string): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(key);
}

function writeStorage(key: string, value: string): void {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(key, value);
}

function removeStorage(key: string): void {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(key);
}

export function getTenantId(): string {
  return readStorage(TENANT_KEY) ?? DEMO_TENANT_ID;
}

export function getUserId(): string {
  return readStorage(USER_KEY) ?? DEMO_USER_ID;
}

export function getLojaId(): string {
  return readStorage(LOJA_KEY) ?? DEMO_LOJA_ID;
}

export function setLojaId(lojaId: string): void {
  writeStorage(LOJA_KEY, lojaId);
}

export function getDisplayName(): string | null {
  return readStorage(DISPLAY_NAME_KEY);
}

export function setDisplayName(name: string): void {
  writeStorage(DISPLAY_NAME_KEY, name);
}

export function getOperatorLabel(): string {
  return getDisplayName() ?? "Operador";
}

export function setWorkspaceContext(ctx: WorkspaceContext): void {
  writeStorage(TENANT_KEY, ctx.tenantId);
  writeStorage(USER_KEY, ctx.userId);
  writeStorage(LOJA_KEY, ctx.lojaId);
  if (ctx.displayName) {
    writeStorage(DISPLAY_NAME_KEY, ctx.displayName);
  }
}

export function resetWorkspaceToDemo(): void {
  setWorkspaceContext({
    tenantId: DEMO_TENANT_ID,
    userId: DEMO_USER_ID,
    lojaId: DEMO_LOJA_ID,
    displayName: "Admin Demo",
  });
}

export function clearWorkspaceContext(): void {
  removeStorage(TENANT_KEY);
  removeStorage(USER_KEY);
  removeStorage(LOJA_KEY);
  removeStorage(DISPLAY_NAME_KEY);
}

interface JwtPayload {
  tenant_id?: string;
  app_user_id?: string;
  loja_id?: string;
  sub?: string;
  preferred_username?: string;
  name?: string;
  email?: string;
}

export function applyJwtWorkspaceClaims(token: string): void {
  const claims = parseJwtPayload(token);
  if (!claims) return;

  const tenantId = claims.tenant_id;
  const userId = claims.app_user_id ?? claims.sub;
  const lojaId = claims.loja_id;
  if (!tenantId || !userId) return;

  const displayName =
    claims.preferred_username ?? claims.name ?? claims.email ?? undefined;

  setWorkspaceContext({
    tenantId,
    userId,
    lojaId: lojaId ?? getLojaId(),
    displayName,
  });
}

export function getWorkspaceContext(): WorkspaceContext {
  return {
    tenantId: getTenantId(),
    userId: getUserId(),
    lojaId: getLojaId(),
  };
}

function parseJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
