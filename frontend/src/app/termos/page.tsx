import { LegalShell } from "@/components/legal/LegalShell";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Termos de uso — ComercialCloud",
};

export default function TermosPage() {
  return (
    <LegalShell title="Termos de uso" updated="Última atualização: junho de 2026">
      <p>
        Ao utilizar o ComercialCloud (aplicação SaaS, trial ou licença white-label), você
        concorda com estes termos.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">1. Objeto</h2>
      <p>
        O ComercialCloud é uma plataforma de PDV e retaguarda multitenant. O uso inclui
        cadastro, operação comercial, emissão fiscal (quando configurada) e cobrança
        recorrente conforme plano contratado.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">2. Conta e responsabilidade</h2>
      <p>
        Você é responsável pelas credenciais de acesso, pelos dados inseridos no sistema e
        pelo cumprimento das obrigações fiscais perante a SEFAZ e consumidores.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">3. Trial e planos pagos</h2>
      <p>
        O período de teste gratuito tem duração limitada (14 dias, salvo acordo em contrário).
        Após o trial, o acesso pode ser suspenso até a regularização da assinatura.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">4. White-label</h2>
      <p>
        Parceiros licenciados podem operar com marca própria nos limites do contrato de
        licença. Redistribuição do código-fonte como produto concorrente não é permitida.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">5. Disponibilidade</h2>
      <p>
        Empregamos esforços razoáveis para manter o serviço disponível. SLAs específicos
        aplicam-se conforme plano ou contrato white-label.
      </p>
    </LegalShell>
  );
}
