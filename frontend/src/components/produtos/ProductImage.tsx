"use client";

import { Package } from "lucide-react";
import { API_BASE_URL } from "@/lib/constants";
import { cn } from "@/lib/utils";

const SIZE_CLASSES = {
  sm: "h-10 w-10",
  md: "h-14 w-14",
  lg: "h-20 w-20",
} as const;

function resolveImageSrc(src: string): string {
  if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("data:")) {
    return src;
  }
  if (src.startsWith("/")) {
    return `${API_BASE_URL}${src}`;
  }
  return src;
}

interface ProductImageProps {
  src?: string | null;
  alt: string;
  size?: keyof typeof SIZE_CLASSES;
  className?: string;
}

export function ProductImage({ src, alt, size = "md", className }: ProductImageProps) {
  const sizeClass = SIZE_CLASSES[size];

  if (!src?.trim()) {
    return (
      <div
        className={cn(
          "flex shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 text-slate-400",
          sizeClass,
          className,
        )}
        aria-hidden
      >
        <Package className={size === "sm" ? "h-4 w-4" : size === "lg" ? "h-8 w-8" : "h-6 w-6"} />
      </div>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={resolveImageSrc(src)}
      alt={alt}
      loading="lazy"
      className={cn(
        "shrink-0 rounded-lg border border-slate-200 bg-white object-cover",
        sizeClass,
        className,
      )}
    />
  );
}
