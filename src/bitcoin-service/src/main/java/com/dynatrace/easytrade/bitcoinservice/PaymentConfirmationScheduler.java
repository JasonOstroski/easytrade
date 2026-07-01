package com.dynatrace.easytrade.bitcoinservice;

import com.dynatrace.easytrade.bitcoinservice.models.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * Simulates blockchain confirmation of bitcoin payments.
 * Picks up orders with status PAYMENT_PENDING and randomly confirms or fails them
 * to simulate real blockchain behavior.
 */
@Service
public class PaymentConfirmationScheduler extends BaseScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PaymentConfirmationScheduler.class);
    private final DatabaseHelper dbHelper;

    // 90% confirmation success rate to simulate real-world blockchain behavior
    private static final double CONFIRMATION_SUCCESS_RATE = 0.9;

    public PaymentConfirmationScheduler(DatabaseHelper dbHelper) {
        super("payment-confirmation",
                Integer.parseInt(System.getenv().getOrDefault("CONFIRMATION_DELAY", "120")),
                Integer.parseInt(System.getenv().getOrDefault("CONFIRMATION_RATE", "300")));
        this.dbHelper = dbHelper;
    }

    @Override
    protected void run() {
        logger.info("Running PaymentConfirmationScheduler task!");

        try (Connection conn = dbHelper.getConnection()) {
            List<String> pendingOrders = dbHelper.getOrderIdsWithStatus(conn, StatusType.PAYMENT_PENDING);

            for (String orderId : pendingOrders) {
                logger.info("Processing blockchain confirmation for order: {}", orderId);

                if (random.nextDouble() < CONFIRMATION_SUCCESS_RATE) {
                    String txHash = generateTransactionHash();
                    dbHelper.insertNewStatus(orderId, StatusType.PAYMENT_CONFIRMED,
                            String.format("Payment confirmed. Transaction hash: %s. Confirmations: 6/6.", txHash));
                    logger.info("Payment confirmed for order: {} with tx: {}", orderId, txHash);
                } else {
                    dbHelper.insertNewStatus(orderId, StatusType.PAYMENT_FAILED,
                            "Payment failed: transaction not confirmed within timeout period (30 minutes). Funds returned to sender.");
                    logger.warn("Payment failed for order: {}", orderId);
                }
            }
        } catch (Exception e) {
            logger.error("Error in PaymentConfirmationScheduler: " + e.getMessage(), e);
        }

        logger.info("Finished PaymentConfirmationScheduler task!");
        randomFixedRatePlusSleep();
    }

    private String generateTransactionHash() {
        StringBuilder hash = new StringBuilder();
        String hex = "0123456789abcdef";
        for (int i = 0; i < 64; i++) {
            hash.append(hex.charAt(random.nextInt(hex.length())));
        }
        return hash.toString();
    }
}
