package com.dynatrace.easytrade.bitcoinservice.models;

import java.util.Objects;

public record BitcoinOrderRequest(
        Integer accountId,
        String email,
        String name,
        String walletAddress,
        Double amount,
        String currency) {
    public BitcoinOrderRequest {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(email);
        Objects.requireNonNull(name);
        Objects.requireNonNull(walletAddress);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
    }
}
