package com.comercialcloud.interfaces.exception;

import java.time.Instant;

public record ErrorResponse(String code, String message, String correlationId, Instant timestamp) {}
