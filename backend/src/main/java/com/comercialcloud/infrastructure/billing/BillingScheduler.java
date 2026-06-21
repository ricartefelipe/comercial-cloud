package com.comercialcloud.infrastructure.billing;

import com.comercialcloud.application.billing.BillingService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class BillingScheduler {

    private static final Logger LOG = Logger.getLogger(BillingScheduler.class);

    @Inject
    BillingService billingService;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void gerarFaturas() {
        for (UUID id : billingService.idsAssinaturasParaFaturar()) {
            try {
                billingService.gerarFaturaAssinatura(id);
            } catch (RuntimeException e) {
                LOG.warnf(e, "Falha ao gerar fatura da assinatura %s", id);
            }
        }
    }

    @Scheduled(every = "30m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void processarCobrancas() {
        for (UUID id : billingService.idsFaturasParaCobrar()) {
            try {
                billingService.cobrarFatura(id);
            } catch (RuntimeException e) {
                LOG.warnf(e, "Falha ao cobrar fatura %s", id);
            }
        }
    }
}
