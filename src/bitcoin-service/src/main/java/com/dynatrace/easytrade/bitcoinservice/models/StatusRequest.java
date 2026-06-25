package com.dynatrace.easytrade.bitcoinservice.models;

import java.util.Optional;

public record StatusRequest(String orderId, String type, String timestamp, Optional<Object> details) {
}
