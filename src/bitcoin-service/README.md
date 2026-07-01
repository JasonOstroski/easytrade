# Bitcoin Service

A Java service for handling bitcoin payment orders in EasyTrade.

## Technology

- Java 21
- Spring Boot
- Gradle
- MSSQL

## Endpoints

Swagger UI: `/swagger-ui/index.html`

### Create Bitcoin Payment Order

`POST /v1/orders`

Creates a new bitcoin payment order and returns a deposit address for the customer to send BTC to.

Request body:

```json
{
  "accountId": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "walletAddress": "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
  "amount": 0.05,
  "currency": "BTC"
}
```

Response (201):

```json
{
  "statusCode": 201,
  "message": "Bitcoin payment order has been created. Send payment to the provided deposit address.",
  "results": {
    "orderId": "d3bfb8ac-9ba5-433c-a431-06b64eac2162",
    "depositAddress": "bc1q...",
    "amount": 0.05
  }
}
```

### Get Status History

`GET /v1/orders/{accountId}/status`

Returns the full status history for the most recent bitcoin order of an account.

### Get Latest Status

`GET /v1/orders/{accountId}/status/latest`

Returns the latest status for the given account's bitcoin order.

### Update Order Status

`POST /v1/orders/{id}/status`

Updates the bitcoin order status (used internally by the payment confirmation scheduler).

```json
{
  "orderId": "d3bfb8ac-9ba5-433c-a431-06b64eac2162",
  "type": "payment_confirmed",
  "timestamp": "2026-06-25T14:12:12.830Z"
}
```

### Delete Orders

`DELETE /v1/orders/{accountId}`

Deletes all bitcoin orders and status history for the given account.

## Order Lifecycle

```
ORDER_CREATED → PAYMENT_PENDING → PAYMENT_CONFIRMED
                                 → PAYMENT_FAILED
```

1. **ORDER_CREATED** - Customer submits a payment request, receives a deposit address
2. **PAYMENT_PENDING** - Payment detected on the blockchain, awaiting confirmations
3. **PAYMENT_CONFIRMED** - 6+ confirmations received, payment is complete
4. **PAYMENT_FAILED** - Payment not confirmed within timeout (30 min)

## Configuration

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `MSSQL_CONNECTIONSTRING` | JDBC connection string to the database | *required* |
| `CONFIRMATION_DELAY` | Initial delay before payment confirmation scheduler starts (seconds) | 120 |
| `CONFIRMATION_RATE` | Interval between payment confirmation checks (seconds) | 300 |

## Docker

```bash
docker build -t bitcoin-service .
docker run -e MSSQL_CONNECTIONSTRING="jdbc:sqlserver://..." bitcoin-service
```
