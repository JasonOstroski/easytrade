package com.dynatrace.easytrade.bitcoinservice.models;

public record BitcoinOrderResponse(String orderId, String depositAddress, Double amount) {
}
