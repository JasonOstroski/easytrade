package com.dynatrace.easytrade.bitcoinservice.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record BitcoinOrderStatus(Integer id, String bitcoinOrderId, OffsetDateTime timestamp, String status,
        String details) {

    private static final Logger logger = LoggerFactory.getLogger(BitcoinOrderStatus.class);

    public static BitcoinOrderStatus fromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("Id");
        String orderId = rs.getString("BitcoinOrderId");
        Timestamp timestamp = rs.getTimestamp("Timestamp");
        String status = rs.getString("Status");
        String details = rs.getString("Details");
        OffsetDateTime datetime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
        logger.debug("Creating BitcoinOrderStatus with [id::{}] [orderId::{}] [date::{}] [status::{}]",
                id, orderId, datetime, status);
        return new BitcoinOrderStatus(id, orderId, datetime, status, details);
    }
}
