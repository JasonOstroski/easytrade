package com.dynatrace.easytrade.bitcoinservice;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dynatrace.easytrade.bitcoinservice.models.BitcoinOrderRequest;
import com.dynatrace.easytrade.bitcoinservice.models.BitcoinOrderResponse;
import com.dynatrace.easytrade.bitcoinservice.models.BitcoinOrderStatus;
import com.dynatrace.easytrade.bitcoinservice.models.BitcoinOrderStatusHistory;
import com.dynatrace.easytrade.bitcoinservice.models.StandardResponse;
import com.dynatrace.easytrade.bitcoinservice.models.StatusRequest;
import com.dynatrace.easytrade.bitcoinservice.models.StatusType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping(value = "/v1/orders",
        produces = {"application/json", "application/xml"})
@CrossOrigin
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success", content =
        @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request", content =
        @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error", content =
        @Content(schema = @Schema(implementation = StandardResponse.class))),
})
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    public static final String ORDER_CREATED = "Bitcoin payment order has been created. Send payment to the provided deposit address.";
    public static final String PENDING_ORDER_EXISTS = "A pending bitcoin payment order already exists for this account.";
    public static final String STATUS_UPDATED = "Bitcoin order status updated successfully.";
    private final DatabaseHelper dbHelper;

    public OrderController(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @PostMapping(value = "", consumes = {"application/json", "application/xml"})
    @Operation(summary = "Create a bitcoin payment order")
    public ResponseEntity<StandardResponse> createBitcoinOrder(@RequestBody BitcoinOrderRequest request) {
        logger.info("Creating bitcoin payment order for account: {}", request.accountId());

        try (Connection conn = dbHelper.getConnection()) {
            Integer pendingCount = dbHelper.getPendingOrderCountForAccountId(conn, request.accountId());

            if (pendingCount == 0) {
                // Generate a simulated deposit address for the customer to send BTC to
                String depositAddress = generateDepositAddress();
                String guid = dbHelper.insertNewOrder(conn, request, depositAddress);
                dbHelper.insertNewStatus(conn, guid, StatusType.ORDER_CREATED);

                BitcoinOrderResponse response = new BitcoinOrderResponse(guid, depositAddress, request.amount());
                return buildResponseEntity(HttpStatus.CREATED, ORDER_CREATED, response, null, null);
            } else {
                return buildResponseEntity(HttpStatus.BAD_REQUEST, PENDING_ORDER_EXISTS, null, request, null);
            }
        } catch (SQLException e) {
            return handleSQLException(e);
        }
    }

    @GetMapping("/{accountId}/status")
    @Operation(summary = "Get bitcoin order status history for an account")
    public ResponseEntity<StandardResponse> getStatusHistory(@PathVariable Integer accountId) {
        logger.info("Getting bitcoin order status history for accountId: {}", accountId);
        try (Connection conn = dbHelper.getConnection()) {
            Optional<String> orderId = dbHelper.getOrderIdForAccount(conn, accountId);
            if (orderId.isEmpty()) {
                return buildResponseEntity(HttpStatus.NOT_FOUND,
                        "No bitcoin order found for account [" + accountId + "]");
            }
            List<BitcoinOrderStatus> statusList = dbHelper.getOrderStatusList(conn, orderId.get());
            return buildResponseEntity(HttpStatus.OK, "Status history found",
                    new BitcoinOrderStatusHistory(orderId.get(), statusList));
        } catch (SQLException e) {
            return handleSQLException(e);
        }
    }

    @GetMapping("/{accountId}/status/latest")
    @Operation(summary = "Get latest bitcoin order status for an account")
    public ResponseEntity<StandardResponse> getLatestStatus(@PathVariable Integer accountId) {
        logger.info("Getting latest bitcoin status for accountId: {}", accountId);

        try (Connection conn = dbHelper.getConnection()) {
            Optional<BitcoinOrderStatus> status = dbHelper.getLastOrderStatusForAccountId(conn, accountId);
            return status
                    .map(s -> buildResponseEntity(HttpStatus.OK, "Status found successfully.", s))
                    .orElse(buildResponseEntity(HttpStatus.NOT_FOUND,
                            "No bitcoin order status found for the given account."));
        } catch (SQLException e) {
            return handleSQLException(e);
        }
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Delete bitcoin orders for an account")
    public ResponseEntity<StandardResponse> deleteOrders(@PathVariable Integer accountId) {
        logger.info("Deleting bitcoin orders for accountId: {}", accountId);
        try (Connection conn = dbHelper.getConnection()) {
            dbHelper.deleteOrdersForAccountId(conn, accountId);
            return buildResponseEntity(HttpStatus.OK, "Bitcoin orders successfully deleted.", null, null, null);
        } catch (SQLException e) {
            return handleSQLException(e);
        }
    }

    @PostMapping(value = "/{id}/status", consumes = {"application/json", "application/xml"})
    @Operation(summary = "Update the bitcoin order status")
    public ResponseEntity<StandardResponse> updateStatus(@PathVariable String id, @RequestBody StatusRequest request) {
        logger.info("Updating status for bitcoin order: {}", id);
        try (Connection conn = dbHelper.getConnection()) {
            if (!id.equals(request.orderId())) {
                return buildResponseEntity(HttpStatus.BAD_REQUEST,
                        "Order ID in path and request body don't match!", null, request, null);
            }

            BitcoinOrderStatus currentStatus = dbHelper.getLastOrderStatus(conn, id);
            if (currentStatus == null) {
                return buildResponseEntity(HttpStatus.BAD_REQUEST,
                        "No status found for bitcoin order: " + id, null, null, null);
            }

            StatusType newStatusType = StatusType.valueOf(request.type().toUpperCase());
            StatusType oldStatusType = StatusType.valueOf(currentStatus.status().toUpperCase());

            if (newStatusType.getSequence() <= oldStatusType.getSequence()) {
                String message = String.format("Invalid status transition from %s to %s",
                        oldStatusType.getType(), newStatusType.getType());
                return buildResponseEntity(HttpStatus.BAD_REQUEST, message, null, null, null);
            }

            String details = request.details().map(Object::toString).orElse(newStatusType.getDescription());
            dbHelper.insertNewStatus(id, newStatusType, details);

            return buildResponseEntity(HttpStatus.OK, STATUS_UPDATED, null, null, null);
        } catch (SQLException e) {
            return handleSQLException(e);
        }
    }

    private String generateDepositAddress() {
        // Simulate a BTC deposit address (bc1 bech32 format)
        return "bc1q" + UUID.randomUUID().toString().replace("-", "").substring(0, 38);
    }

    private ResponseEntity<StandardResponse> buildResponseEntity(HttpStatus status, String message) {
        return buildResponseEntity(status, message, null, null, null);
    }

    private ResponseEntity<StandardResponse> buildResponseEntity(HttpStatus status, String message, Object results) {
        return buildResponseEntity(status, message, results, null, null);
    }

    private ResponseEntity<StandardResponse> buildResponseEntity(HttpStatus status, String message, Object results,
            Object data, Object error) {
        logger.info(message);
        return ResponseEntity
                .status(status)
                .body(new StandardResponse(
                        status.value(),
                        message,
                        results,
                        data,
                        error));
    }

    private ResponseEntity<StandardResponse> handleSQLException(SQLException e) {
        logger.error("SQL Exception: {}", e.getMessage());
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "An exception occurred!",
                null, null, e.getMessage());
    }
}
