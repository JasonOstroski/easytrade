package com.dynatrace.easytrade.bitcoinservice.models;

import lombok.Getter;

@Getter
public enum StatusType {
    ORDER_CREATED(10, "order_created", "A bitcoin payment order has been accepted."),
    PAYMENT_PENDING(20, "payment_pending", "Awaiting blockchain confirmation."),
    PAYMENT_CONFIRMED(30, "payment_confirmed", "Bitcoin payment has been confirmed on the blockchain."),
    PAYMENT_FAILED(40, "payment_failed", "Bitcoin payment failed or timed out."),
    SEQUENCE_ERROR(99, "sequence_error", "Wrong sequence of status occurred. Please verify the whole process!");

    private int sequence;
    private String type;
    private String description;

    StatusType(int sequence, String type, String description) {
        this.sequence = sequence;
        this.type = type;
        this.description = description;
    }
}
