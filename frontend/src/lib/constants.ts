export const TENANT_ID = "11111111-1111-1111-1111-111111111111";
export const USER_ID = "33333333-3333-3333-3333-333333333333";
export const CAIXA_USER_ID = "44444444-4444-4444-4444-444444444444";
export const LOJA_ID = "22222222-2222-2222-2222-222222222222";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const KEYCLOAK_URL =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost:8180";
export const KEYCLOAK_REALM =
  process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? "comercialcloud";
export const KEYCLOAK_CLIENT_ID =
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "comercial-cloud-web";

/**
 * Como o painel entra: `keycloak` (produção, padrão), `demo` (demonstração pública: conta de demonstração trocada por
 * uma sessão do servidor) ou `header` (só desenvolvimento local, sem Keycloak).
 */
export type AuthMode = "keycloak" | "demo" | "header";
const authModeEnv = process.env.NEXT_PUBLIC_AUTH_MODE;
export const AUTH_MODE: AuthMode =
  authModeEnv === "demo" || authModeEnv === "header" ? authModeEnv : "keycloak";

/** Na demonstração a sessão vai neste cabeçalho: `Authorization` é do Keycloak. */
export const SESSAO_DEMO_HEADER = "X-Sessao-Demo";

/** Conta pública da demonstração: os valores são publicados de propósito, como nas demais demonstrações. */
export const DEMO_EMAIL = process.env.NEXT_PUBLIC_DEMO_EMAIL ?? "admin@demo.com";
export const DEMO_SENHA = process.env.NEXT_PUBLIC_DEMO_SENHA ?? "";
