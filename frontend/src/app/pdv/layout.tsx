"use client";

import { BrandingProvider } from "@/components/branding/BrandingProvider";
import type { ReactNode } from "react";

export default function PdvLayout({ children }: { children: ReactNode }) {
  return <BrandingProvider>{children}</BrandingProvider>;
}
