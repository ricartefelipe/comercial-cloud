import { test, expect } from "@playwright/test";

test.describe("ComercialCloud smoke", () => {
  test("página de login carrega", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByText("ComercialCloud")).toBeVisible();
    await expect(page.getByRole("button", { name: /entrar em modo demo/i })).toBeVisible();
  });

  test("modo demo acessa dashboard", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: /entrar em modo demo/i }).click();
    await expect(page).toHaveURL(/dashboard/);
  });

  test("PDV carrega", async ({ page }) => {
    await page.goto("/pdv");
    await expect(page.getByText(/ComercialCloud|Caixa fechado|PDV/i).first()).toBeVisible();
  });
});
