import { LegalShell } from "@/components/legal/LegalShell";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Política de privacidade — ComercialCloud",
};

export default function PrivacidadePage() {
  return (
    <LegalShell
      title="Política de privacidade"
      updated="Última atualização: junho de 2026 · LGPD (Lei 13.709/2018)"
    >
      <p>Esta política descreve como tratamos dados pessoais no ComercialCloud.</p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">1. Dados coletados</h2>
      <ul className="mt-2 list-disc space-y-1 pl-5">
        <li>Cadastro: nome, e-mail, CNPJ/CPF da loja</li>
        <li>Operação: vendas, clientes, produtos, caixa e financeiro</li>
        <li>Técnicos: logs de acesso, IP e identificadores de sessão</li>
        <li>Pagamentos: processados pelo gateway Asaas (sem armazenar cartão completo)</li>
      </ul>
      <h2 className="mt-6 text-base font-semibold text-slate-900">2. Finalidades</h2>
      <p>
        Prestação do SaaS, cobrança, suporte, segurança, cumprimento legal e emissão fiscal
        quando habilitada.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">3. Compartilhamento</h2>
      <p>
        Provedores necessários à operação: hospedagem, Asaas, Focus NFe, Keycloak e
        autoridades quando exigido por lei.
      </p>
      <h2 className="mt-6 text-base font-semibold text-slate-900">4. Direitos do titular</h2>
      <p>
        Você pode solicitar acesso, correção, exclusão ou portabilidade dos seus dados pelo
        canal de suporte do operador da plataforma.
      </p>
    </LegalShell>
  );
}
