package com.dynatrace.easytrade.bitcoinservice.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard response wrapper for bitcoin service endpoints.")
public record StandardResponse(
        Integer statusCode,
        String message,
        @Schema(nullable = true, description = "Result data if the action succeeded.")
        Object results,
        @Schema(nullable = true, description = "Additional context if the action failed non-critically.")
        Object data,
        @Schema(nullable = true, description = "Exception details if an error occurred.")
        Object error) {
}
