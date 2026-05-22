package com.comercialcloud.interfaces.exception;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.infrastructure.observability.CorrelationId;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    CorrelationId correlationId;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        Instant ts = Instant.now();
        String corr = correlationId.getValue();
        String path = uriInfo != null ? uriInfo.getPath() : null;

        if (exception instanceof NotFoundException nfe) {
            return json(Response.Status.NOT_FOUND, nfe.getCode(), nfe.getMessage(), corr, ts, path);
        }
        if (exception instanceof ConflictException ce) {
            return json(Response.Status.CONFLICT, ce.getCode(), ce.getMessage(), corr, ts, path);
        }
        if (exception instanceof BusinessException bre) {
            return json(Response.Status.BAD_REQUEST, bre.getCode(), bre.getMessage(), corr, ts, path);
        }
        if (exception instanceof ConstraintViolationException cve) {
            List<String> details =
                    cve.getConstraintViolations().stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .toList();
            String first = details.isEmpty() ? "Erro de validação" : details.getFirst();
            return json(Response.Status.BAD_REQUEST, "VALIDATION_ERROR", first, corr, ts, path, details);
        }
        if (exception instanceof jakarta.ws.rs.ForbiddenException fe) {
            return json(Response.Status.FORBIDDEN, "FORBIDDEN", fe.getMessage(), corr, ts, path);
        }
        LOG.error("Erro não tratado", exception);
        return json(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno do servidor", corr, ts, path);
    }

    private static Response json(
            Response.Status status, String code, String message, String correlationId, Instant ts, String path) {
        return json(status, code, message, correlationId, ts, path, null);
    }

    private static Response json(
            Response.Status status,
            String code,
            String message,
            String correlationId,
            Instant ts,
            String path,
            List<String> details) {
        return Response.status(status)
                .entity(new ErrorResponse(
                        ts,
                        status.getStatusCode(),
                        status.getReasonPhrase(),
                        message,
                        path,
                        correlationId,
                        details,
                        code))
                .build();
    }
}
