import { test, expect, type Page } from "@playwright/test";

/**
 * Jornada da demonstração contra o backend real (COMERCIAL_CLOUD_TENANT_RESOLUTION=demo) e o painel construído com
 * NEXT_PUBLIC_AUTH_MODE=demo. A API não responde a nenhuma rota de dados sem a sessão da conta de demonstração.
 */
const API = process.env.E2E_API_URL ?? "http://localhost:8080";
const SENHA = process.env.E2E_DEMO_SENHA ?? "";

const ROTAS_DE_DADOS = [
  "/api/v1/clientes",
  "/api/v1/produtos",
  "/api/v1/usuarios",
  "/api/v1/auditoria",
  "/api/v1/configuracoes",
  "/api/v1/vendas",
  "/api/v1/dashboard/resumo",
  "/api/v1/sessao",
];

async function entrarComAContaDeDemonstracao(page: Page) {
  await page.goto("/login");
  await page.getByRole("button", { name: /entrar com a conta de demonstração/i }).click();
  await expect(page).toHaveURL(/\/dashboard/);
}

test.describe("ComercialCloud: demonstração com sessão", () => {
  test.beforeAll(() => {
    expect(SENHA, "defina E2E_DEMO_SENHA com a COMERCIAL_CLOUD_DEMO_SENHA do backend").not.toBe("");
  });

  test("tela de login mostra a conta de demonstração e não tem atalho anônimo", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByText("ComercialCloud", { exact: true })).toBeVisible();
    await expect(page.getByText("Conta de demonstração", { exact: true })).toBeVisible();
    await expect(page.getByText("E-mail: admin@demo.com")).toBeVisible();
    await expect(page.getByRole("button", { name: /entrar com a conta de demonstração/i })).toBeVisible();
    await expect(page.getByRole("button", { name: /entrar em modo demo/i })).toHaveCount(0);
  });

  test("a API não responde sem sessão", async ({ request }) => {
    for (const rota of ROTAS_DE_DADOS) {
      const semSessao = await request.get(`${API}${rota}`);
      expect(semSessao.status(), rota).toBe(401);
      const comCabecalhos = await request.get(`${API}${rota}`, {
        headers: {
          "X-Tenant-Id": "11111111-1111-1111-1111-111111111111",
          "X-User-Id": "33333333-3333-3333-3333-333333333333",
        },
      });
      expect(comCabecalhos.status(), `${rota} com cabeçalhos de locatário`).toBe(401);
    }
  });

  test("senha errada mostra o erro e não entra", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill("admin@demo.com");
    await page.getByLabel("Senha").fill("senha-errada");
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await expect(page.getByText("E-mail ou senha inválidos.")).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test("a conta de demonstração entra e vê os dados da loja fictícia", async ({ page }) => {
    await entrarComAContaDeDemonstracao(page);
    await page.goto("/produtos");
    await expect(page.getByText("Produto Demo 1").first()).toBeVisible();
    await page.goto("/relatorios");
    await expect(page.getByRole("heading", { name: "Relatórios" })).toBeVisible();
    await page.goto("/auditoria");
    await expect(page.getByRole("heading", { name: "Auditoria" })).toBeVisible();
  });

  test("entrar digitando a conta publicada também funciona", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill("admin@demo.com");
    await page.getByLabel("Senha").fill(SENHA);
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test("sair encerra a sessão", async ({ page }) => {
    await entrarComAContaDeDemonstracao(page);
    await page.getByRole("button", { name: "Sair" }).click();
    await expect(page).toHaveURL(/\/login/);
    await page.goto("/produtos");
    await expect(page).toHaveURL(/\/login/);
  });

  test("sessão adulterada volta ao login", async ({ page }) => {
    await entrarComAContaDeDemonstracao(page);
    await page.evaluate(() => sessionStorage.setItem("cc_access_token", "adulterado.invalido.token"));
    await page.goto("/produtos");
    await expect(page).toHaveURL(/\/login\?redirect=/);
  });
});
