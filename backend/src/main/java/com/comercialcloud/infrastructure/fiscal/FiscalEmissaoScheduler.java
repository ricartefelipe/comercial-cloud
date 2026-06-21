package com.comercialcloud.infrastructure.fiscal;

import com.comercialcloud.application.fiscal.FiscalService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FiscalEmissaoScheduler {

    private static final Logger LOG = Logger.getLogger(FiscalEmissaoScheduler.class);

    @Inject
    FiscalService fiscalService;

    @Scheduled(
            every = "${comercial-cloud.fiscal.scheduler.every}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void processarPendentes() {
        List<UUID> ids = fiscalService.idsPendentes();
        if (ids.isEmpty()) {
            return;
        }
        LOG.debugf("Processando %d documento(s) fiscal(is) pendente(s)", ids.size());
        for (UUID id : ids) {
            try {
                fiscalService.processarDocumento(id);
            } catch (RuntimeException e) {
                LOG.warnf(e, "Falha ao processar documento fiscal %s", id);
            }
        }
    }
}
