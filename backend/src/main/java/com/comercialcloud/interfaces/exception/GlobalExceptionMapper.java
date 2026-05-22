package com.comercialcloud.interfaces.exception;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.infrastructure.observability.CorrelationId;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    CorrelationId correlationId;

    @Override
    public Response toResponse(Exception exception) {
        Instant ts = Instant.now();
        String corr = correlationId.getValue();

        if (exception instanceof NotFoundException nfe) {
            return json(Response.Status.NOT_FOUND.getStatusCode(), nfe.getCode(), nfe.getMessage(), corr, ts);
        }
        if (exception instanceof ConflictException ce) {
            return json(Response.Status.CONFLICT.getStatusCode(), ce.getCode(), ce.getMessage(), corr, ts);
        }
        if (exception instanceof BusinessException bre) {
            return json(400, bre.getCode(), bre.getMessage(), corr, ts);
        }
        if (exception instanceof ConstraintViolationException cve) {
            String first = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .findFirst()
                    .orElse("Erro de validação");
            return json(400, "VALIDATION_ERROR", first, corr, ts);
        }
        LOG.error("Erro não tratado", exception);
        return json(500, "INTERNAL_ERROR", "Erro interno do servidor", corr, ts);
    }

    private static Response json(int httpStatus, String code, String message, String correlationId, Instant ts) {
        return Response.status(httpStatus)
                .entity(new ErrorResponse(code, message, correlationId, ts))
                .build();
    }
}
