package com.comercialcloud.infrastructure.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * E-mails transacionais (boas-vindas, fatura, suspensão). Desligado por padrão;
 * ative com {@code comercial-cloud.mail.enabled=true} e configure SMTP Quarkus.
 */
@ApplicationScoped
public class TransacionalEmailService {

    private static final Logger LOG = Logger.getLogger(TransacionalEmailService.class);
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject ReactiveMailer mailer;

    @Inject
    @ConfigProperty(name = "comercial-cloud.mail.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    @ConfigProperty(name = "comercial-cloud.mail.from", defaultValue = "noreply@comercialcloud.local")
    String from;

    @Inject
    @ConfigProperty(name = "comercial-cloud.mail.app-url", defaultValue = "http://localhost:3000")
    String appUrl;

    public void enviarBoasVindas(
            String destinatario, String nome, String planoCodigo, LocalDate trialFim) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        String trial =
                trialFim != null
                        ? "Seu trial gratuito vai até " + trialFim.format(DATA_BR) + ".\n"
                        : "";
        enviar(
                destinatario,
                "Bem-vindo ao ComercialCloud",
                "Olá, "
                        + nome
                        + "!\n\n"
                        + "Sua conta foi criada com sucesso no plano "
                        + planoCodigo
                        + ".\n"
                        + trial
                        + "\nAcesse: "
                        + appUrl
                        + "/login\n"
                        + "PDV: "
                        + appUrl
                        + "/pdv\n\n"
                        + "— Equipe ComercialCloud");
    }

    public void enviarNovaFatura(
            String destinatario,
            String competencia,
            BigDecimal valor,
            LocalDate vencimento,
            String checkoutUrl) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        String pagamento =
                checkoutUrl != null && !checkoutUrl.isBlank()
                        ? "Pagar agora: " + checkoutUrl + "\n"
                        : "Regularize em: " + appUrl + "/assinatura\n";
        enviar(
                destinatario,
                "Fatura ComercialCloud — competência " + competencia,
                "Sua fatura de "
                        + competencia
                        + " no valor de R$ "
                        + valor.toPlainString()
                        + " vence em "
                        + vencimento.format(DATA_BR)
                        + ".\n\n"
                        + pagamento
                        + "\n— Equipe ComercialCloud");
    }

    public void enviarSuspensao(String destinatario, String competencia) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        enviar(
                destinatario,
                "Conta ComercialCloud suspensa — regularize o pagamento",
                "Não identificamos o pagamento da fatura "
                        + competencia
                        + ". Seu acesso foi suspenso.\n\n"
                        + "Regularize em: "
                        + appUrl
                        + "/assinatura\n\n"
                        + "— Equipe ComercialCloud");
    }

    public void enviarPagamentoConfirmado(String destinatario, String competencia) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        enviar(
                destinatario,
                "Pagamento confirmado — ComercialCloud",
                "Recebemos o pagamento da fatura "
                        + competencia
                        + ". Sua conta está ativa.\n\n"
                        + "Acesse: "
                        + appUrl
                        + "/dashboard\n\n"
                        + "— Equipe ComercialCloud");
    }

    public void enviarConviteUsuario(String destinatario, String nome, String token) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        String link = appUrl + "/convite/" + token;
        enviar(
                destinatario,
                "Convite para acessar o ComercialCloud",
                "Olá, "
                        + nome
                        + "!\n\n"
                        + "Você foi convidado para acessar o ComercialCloud.\n"
                        + "Defina sua senha e ative sua conta:\n\n"
                        + link
                        + "\n\n"
                        + "O link expira em alguns dias.\n\n"
                        + "— Equipe ComercialCloud");
    }

    public String appLoginUrl() {
        return appUrl + "/login";
    }

    private void enviar(String destinatario, String assunto, String corpo) {
        if (!enabled) {
            LOG.debugf("Mail desabilitado — não enviado para %s: %s", destinatario, assunto);
            return;
        }
        mailer.send(Mail.withText(destinatario, assunto, corpo).setFrom(from))
                .subscribe()
                .with(
                        unused -> LOG.debugf("E-mail enviado para %s: %s", destinatario, assunto),
                        err -> LOG.warnf(err, "Falha ao enviar e-mail para %s: %s", destinatario, assunto));
    }
}
