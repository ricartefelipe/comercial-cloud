"use client";

import { getBranding } from "@/lib/api/branding";
import type { BrandingConfig } from "@/types";
import { useQuery } from "@tanstack/react-query";
import { createContext, useContext, useEffect, type ReactNode } from "react";

const DEFAULT_BRANDING: BrandingConfig = {
  nomeExibicao: "ComercialCloud",
  corPrimaria: "#4f7cff",
  logoUrl: "",
};

const BrandingContext = createContext<BrandingConfig>(DEFAULT_BRANDING);

export function BrandingProvider({ children }: { children: ReactNode }) {
  const { data } = useQuery({
    queryKey: ["branding"],
    queryFn: getBranding,
    staleTime: 60_000,
  });

  const branding = data ?? DEFAULT_BRANDING;

  useEffect(() => {
    document.documentElement.style.setProperty("--brand-primary", branding.corPrimaria);
  }, [branding.corPrimaria]);

  return (
    <BrandingContext.Provider value={branding}>{children}</BrandingContext.Provider>
  );
}

export function useBranding(): BrandingConfig {
  return useContext(BrandingContext);
}
