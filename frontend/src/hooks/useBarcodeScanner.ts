"use client";

import { useEffect, useRef } from "react";

const SCAN_GAP_MS = 80;
const MIN_BARCODE_LENGTH = 4;

export function useBarcodeScanner(onScan: (code: string) => void, enabled: boolean) {
  const bufferRef = useRef("");
  const lastKeyRef = useRef(0);
  const onScanRef = useRef(onScan);

  useEffect(() => {
    onScanRef.current = onScan;
  }, [onScan]);

  useEffect(() => {
    if (!enabled) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const tag = target?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") {
        return;
      }

      const now = Date.now();
      if (now - lastKeyRef.current > SCAN_GAP_MS) {
        bufferRef.current = "";
      }
      lastKeyRef.current = now;

      if (e.key === "Enter") {
        const code = bufferRef.current.trim();
        bufferRef.current = "";
        if (code.length >= MIN_BARCODE_LENGTH) {
          e.preventDefault();
          onScanRef.current(code);
        }
        return;
      }

      if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
        bufferRef.current += e.key;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [enabled]);
}

export function looksLikeBarcode(termo: string): boolean {
  const trimmed = termo.trim();
  return /^\d{4,}$/.test(trimmed);
}
