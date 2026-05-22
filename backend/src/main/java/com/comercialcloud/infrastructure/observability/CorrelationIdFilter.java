package com.comercialcloud.infrastructure.observability;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    CorrelationId correlationId;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String header = requestContext.getHeaderString(CorrelationId.HEADER);
        if (header == null || header.isBlank()) {
            header = CorrelationId.generate();
        }
        correlationId.setValue(header);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (correlationId.getValue() != null) {
            responseContext.getHeaders().putSingle(CorrelationId.HEADER, correlationId.getValue());
        }
    }
}
