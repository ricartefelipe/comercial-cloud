import { apiClient } from "@/lib/api/client";
import type { BrandingConfig } from "@/types";

export async function getBranding(): Promise<BrandingConfig> {
  return apiClient<BrandingConfig>("/api/v1/branding", { includeUserId: false });
}

export async function saveBranding(data: BrandingConfig): Promise<BrandingConfig> {
  return apiClient<BrandingConfig>("/api/v1/branding", {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}
