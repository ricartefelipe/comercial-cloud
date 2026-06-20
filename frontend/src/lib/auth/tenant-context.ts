import {
  LOJA_ID as DEMO_LOJA_ID,
  TENANT_ID as DEMO_TENANT_ID,
  USER_ID as DEMO_USER_ID,
} from "@/lib/constants";

const TENANT_KEY = "cc_tenant_id";
const USER_KEY = "cc_user_id";
const LOJA_KEY = "cc_loja_id";

export interface WorkspaceContext {
  tenantId: string;
  userId: string;
  lojaId: string;
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

export function setWorkspaceContext(ctx: WorkspaceContext): void {
  writeStorage(TENANT_KEY, ctx.tenantId);
  writeStorage(USER_KEY, ctx.userId);
  writeStorage(LOJA_KEY, ctx.lojaId);
}

export function resetWorkspaceToDemo(): void {
  setWorkspaceContext({
    tenantId: DEMO_TENANT_ID,
    userId: DEMO_USER_ID,
    lojaId: DEMO_LOJA_ID,
  });
}

export function clearWorkspaceContext(): void {
  removeStorage(TENANT_KEY);
  removeStorage(USER_KEY);
  removeStorage(LOJA_KEY);
}

interface JwtPayload {
  tenant_id?: string;
  app_user_id?: string;
  loja_id?: string;
  sub?: string;
}

export function applyJwtWorkspaceClaims(token: string): void {
  const claims = parseJwtPayload(token);
  if (!claims) return;

  const tenantId = claims.tenant_id;
  const userId = claims.app_user_id ?? claims.sub;
  const lojaId = claims.loja_id;
  if (!tenantId || !userId) return;

  setWorkspaceContext({
    tenantId,
    userId,
    lojaId: lojaId ?? getLojaId(),
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
