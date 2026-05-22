export const TENANT_ID = "11111111-1111-1111-1111-111111111111";
export const USER_ID = "44444444-4444-4444-4444-444444444444";
export const LOJA_ID = "22222222-2222-2222-2222-222222222222";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const KEYCLOAK_URL =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost:8180";
export const KEYCLOAK_REALM =
  process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? "comercialcloud";
export const KEYCLOAK_CLIENT_ID =
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "comercial-cloud-web";
