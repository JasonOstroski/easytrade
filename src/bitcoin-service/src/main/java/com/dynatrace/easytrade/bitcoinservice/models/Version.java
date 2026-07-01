package com.dynatrace.easytrade.bitcoinservice.models;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Version(String buildVersion, String buildDate, String buildCommit) {
    @Override
    public String toString() {
        return String.format("%s (%s) [%s]", buildVersion, buildDate, buildCommit);
    }

    public Optional<String> toJson() {
        try {
            return Optional.of(new ObjectMapper().writeValueAsString(this));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
