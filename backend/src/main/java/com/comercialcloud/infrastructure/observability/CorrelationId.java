package com.comercialcloud.infrastructure.observability;

import jakarta.enterprise.context.RequestScoped;

import java.util.UUID;

@RequestScoped
public class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
