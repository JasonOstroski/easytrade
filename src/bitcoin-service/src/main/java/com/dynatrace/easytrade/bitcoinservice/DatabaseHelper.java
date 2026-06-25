package com.dynatrace.easytrade.bitcoinservice;

import com.dynatrace.easytrade.bitcoinservice.models.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseHelper {
    private final String INSERT_ORDER_QUERY = "INSERT INTO [dbo].[BitcoinOrders] ([Id], [AccountId], [Email], [Name], [WalletAddress], [Amount], [Currency], [DepositAddress]) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private final String INSERT_STATUS_QUERY = "INSERT INTO [dbo].[BitcoinOrderStatus] ([BitcoinOrderId], [Timestamp], [Status], [Details]) VALUES (?, ?, ?, ?)";
    private final String COUNT_PENDING_ORDERS_BY_ACCOUNT_ID = "SELECT COUNT(*) FROM BitcoinOrders bo WHERE bo.AccountId = ? AND bo.Id IN (SELECT bos.BitcoinOrderId FROM BitcoinOrderStatus bos WHERE bos.Status IN ('order_created', 'payment_pending'))";
    private final String GET_LAST_STATUS_QUERY = "SELECT TOP 1 * FROM BitcoinOrderStatus WHERE BitcoinOrderId = ? ORDER BY Timestamp DESC";
    private final String GET_STATUS_LIST_QUERY = "SELECT * FROM BitcoinOrderStatus WHERE BitcoinOrderId = ? ORDER BY Timestamp DESC";
    private final String GET_ORDER_IDS_BY_ACCOUNT_ID = "SELECT Id FROM BitcoinOrders WHERE AccountId = ?";
    private final String GET_LAST_STATUS_BY_ACCOUNT_ID_QUERY = "SELECT TOP 1 bos.* FROM BitcoinOrderStatus bos INNER JOIN BitcoinOrders bo ON bos.BitcoinOrderId = bo.Id WHERE bo.AccountId = ? ORDER BY bos.Timestamp DESC";
    private final String DELETE_ORDER_STATUS_BY_ACCOUNT_ID_QUERY = "DELETE FROM BitcoinOrderStatus WHERE BitcoinOrderId IN (SELECT Id FROM BitcoinOrders WHERE AccountId = ?)";
    private final String DELETE_ORDER_BY_ACCOUNT_ID_QUERY = "DELETE FROM BitcoinOrders WHERE AccountId = ?";
    private final String GET_PENDING_ORDERS_QUERY = "SELECT x.BitcoinOrderId, x.Status FROM BitcoinOrderStatus x INNER JOIN (SELECT MAX(Id) Id, BitcoinOrderId FROM BitcoinOrderStatus GROUP BY BitcoinOrderId) y ON x.BitcoinOrderId = y.BitcoinOrderId AND x.Id = y.Id";
    private final String GET_ORDER_BY_ID_QUERY = "SELECT * FROM BitcoinOrders WHERE Id = ?";

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);
    private final HikariDataSource dataSource;

    public DatabaseHelper() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("MSSQL_CONNECTIONSTRING"));
        // Pool sizing: sized for 100x traffic per replica
        // Formula: connections = (core_count * 2) + effective_spindle_count
        // For a service pod with 1 CPU core: (1*2)+1 = 3 minimum, but we allow headroom
        config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "20")));
        config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")));
        config.setConnectionTimeout(Integer.parseInt(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT_MS", "10000")));
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(600000); // 10 minutes
        config.setPoolName("bitcoin-service-pool");
        // Leak detection: flag connections held longer than 30s
        config.setLeakDetectionThreshold(30000);
        // Keep connections validated
        config.setKeepaliveTime(60000);
        this.dataSource = new HikariDataSource(config);
        logger.info("HikariCP pool initialized: max={}, minIdle={}, timeout={}ms",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout());
    }

    public String insertNewOrder(Connection conn, BitcoinOrderRequest request, String depositAddress) throws SQLException {
        PreparedStatement query = conn.prepareStatement(INSERT_ORDER_QUERY);
        String guid = UUID.randomUUID().toString();
        query.setString(1, guid);
        query.setInt(2, request.accountId());
        query.setString(3, request.email());
        query.setString(4, request.name());
        query.setString(5, request.walletAddress());
        query.setDouble(6, request.amount());
        query.setString(7, request.currency());
        query.setString(8, depositAddress);
        query.executeUpdate();
        query.close();
        return guid;
    }

    public Integer getPendingOrderCountForAccountId(Connection conn, Integer accountId) throws SQLException {
        PreparedStatement query = conn.prepareStatement(COUNT_PENDING_ORDERS_BY_ACCOUNT_ID);
        query.setInt(1, accountId);
        ResultSet rs = query.executeQuery();
        rs.next();
        var result = rs.getInt(1);
        query.close();
        return result;
    }

    public BitcoinOrderStatus getLastOrderStatus(Connection conn, String orderId) throws SQLException {
        PreparedStatement query = conn.prepareStatement(GET_LAST_STATUS_QUERY);
        query.setString(1, orderId);
        ResultSet rs = query.executeQuery();

        BitcoinOrderStatus status = null;
        if (rs.next()) {
            status = BitcoinOrderStatus.fromResultSet(rs);
        }
        query.close();
        return status;
    }

    public List<BitcoinOrderStatus> getOrderStatusList(Connection conn, String orderId) throws SQLException {
        PreparedStatement query = conn.prepareStatement(GET_STATUS_LIST_QUERY);
        query.setString(1, orderId);
        ResultSet rs = query.executeQuery();

        List<BitcoinOrderStatus> result = new ArrayList<>();
        while (rs.next()) {
            result.add(BitcoinOrderStatus.fromResultSet(rs));
        }
        query.close();
        return result;
    }

    public Optional<BitcoinOrderStatus> getLastOrderStatusForAccountId(Connection conn, Integer accountId) throws SQLException {
        PreparedStatement query = conn.prepareStatement(GET_LAST_STATUS_BY_ACCOUNT_ID_QUERY);
        query.setInt(1, accountId);
        ResultSet rs = query.executeQuery();

        Optional<BitcoinOrderStatus> result;
        if (rs.next()) {
            result = Optional.of(BitcoinOrderStatus.fromResultSet(rs));
        } else {
            result = Optional.empty();
        }
        query.close();
        return result;
    }

    public List<String> getOrderIdsWithStatus(Connection conn, StatusType targetStatus) throws SQLException {
        PreparedStatement query = conn.prepareStatement(GET_PENDING_ORDERS_QUERY);
        ResultSet rs = query.executeQuery();

        ArrayList<String> orders = new ArrayList<>();
        while (rs.next()) {
            String id = rs.getString(1);
            String status = rs.getString(2);
            if (StatusType.valueOf(status.toUpperCase()) == targetStatus) {
                orders.add(id);
            }
        }
        query.close();
        return orders;
    }

    public Optional<String> getOrderIdForAccount(Connection conn, Integer accountId) throws SQLException {
        PreparedStatement query = conn.prepareStatement(GET_ORDER_IDS_BY_ACCOUNT_ID);
        query.setInt(1, accountId);
        ResultSet rs = query.executeQuery();

        Optional<String> result;
        if (rs.next()) {
            result = Optional.of(rs.getString("Id"));
        } else {
            result = Optional.empty();
        }
        query.close();
        return result;
    }

    public void insertNewStatus(Connection conn, String orderId, StatusType statusType) throws SQLException {
        insertNewStatus(conn, orderId, statusType, statusType.getDescription());
    }

    public void insertNewStatus(Connection conn, String orderId, StatusType statusType, String details) throws SQLException {
        Timestamp timestamp = Timestamp.valueOf(OffsetDateTime.now().toLocalDateTime());
        logger.debug("Inserting new bitcoin status [orderID::{}] [date::{}] [status::{}] [details::{}]",
                orderId, timestamp, statusType.getType(), details);
        PreparedStatement query = conn.prepareStatement(INSERT_STATUS_QUERY);
        query.setString(1, orderId);
        query.setTimestamp(2, timestamp);
        query.setString(3, statusType.getType());
        query.setString(4, details);
        query.executeUpdate();
        query.close();
    }

    public void insertNewStatus(String orderId, StatusType statusType) throws SQLException {
        try (Connection conn = getConnection()) {
            insertNewStatus(conn, orderId, statusType, statusType.getDescription());
        }
    }

    public void insertNewStatus(String orderId, StatusType statusType, String details) throws SQLException {
        try (Connection conn = getConnection()) {
            insertNewStatus(conn, orderId, statusType, details);
        }
    }

    public void deleteOrdersForAccountId(Connection conn, Integer accountId) throws SQLException {
        deleteByAccountId(conn, accountId, DELETE_ORDER_STATUS_BY_ACCOUNT_ID_QUERY);
        deleteByAccountId(conn, accountId, DELETE_ORDER_BY_ACCOUNT_ID_QUERY);
    }

    private void deleteByAccountId(Connection conn, Integer accountId, String deleteQuery) throws SQLException {
        PreparedStatement query = conn.prepareStatement(deleteQuery);
        query.setInt(1, accountId);
        query.executeUpdate();
        query.close();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
