package com.dynatrace.easytrade.bitcoinservice;

import com.dynatrace.easytrade.bitcoinservice.models.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * Simulates detection of incoming bitcoin payments on the blockchain.
 * Picks up orders with status ORDER_CREATED and moves them to PAYMENT_PENDING
 * to simulate a customer having sent their BTC to the deposit address.
 */
@Service
public class PaymentDetectionScheduler extends BaseScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDetectionScheduler.class);
    private final DatabaseHelper dbHelper;

    public PaymentDetectionScheduler(DatabaseHelper dbHelper) {
        super("payment-detection",
                Integer.parseInt(System.getenv().getOrDefault("DETECTION_DELAY", "60")),
                Integer.parseInt(System.getenv().getOrDefault("DETECTION_RATE", "180")));
        this.dbHelper = dbHelper;
    }

    @Override
    protected void run() {
        logger.info("Running PaymentDetectionScheduler task!");

        try (Connection conn = dbHelper.getConnection()) {
            List<String> newOrders = dbHelper.getOrderIdsWithStatus(conn, StatusType.ORDER_CREATED);

            for (String orderId : newOrders) {
                logger.info("Detected incoming payment for order: {}", orderId);
                dbHelper.insertNewStatus(orderId, StatusType.PAYMENT_PENDING,
                        "Payment detected on blockchain. Waiting for confirmations (0/6).");
            }
        } catch (Exception e) {
            logger.error("Error in PaymentDetectionScheduler: " + e.getMessage(), e);
        }

        logger.info("Finished PaymentDetectionScheduler task!");
        randomFixedRatePlusSleep();
    }
}
