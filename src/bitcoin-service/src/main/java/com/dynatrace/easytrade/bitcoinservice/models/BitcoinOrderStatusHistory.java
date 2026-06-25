package com.dynatrace.easytrade.bitcoinservice.models;

import java.util.List;

public record BitcoinOrderStatusHistory(String orderId, List<BitcoinOrderStatus> statusHistory) {
}
