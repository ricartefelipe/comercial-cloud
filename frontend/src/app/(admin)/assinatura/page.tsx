import { LoadingSpinner } from "@/components/ui/Common";
import { Suspense } from "react";
import AssinaturaContent from "./AssinaturaContent";

export default function AssinaturaPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <AssinaturaContent />
    </Suspense>
  );
}
