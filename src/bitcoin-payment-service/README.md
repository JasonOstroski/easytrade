# Bitcoin Payment Service

A FastAPI microservice for the EasyTrade demo application that simulates Bitcoin payment processing.

## Overview

Accepts Bitcoin payment requests, simulates blockchain confirmation with configurable delays, and deposits funds to user accounts via the broker-service upon confirmation.

## Status Flow

`pending` → `confirming` (~5s) → `confirmed` (~10s more) → broker-service deposit called

## API

- `POST /v1/payments` — create a new payment
- `GET /v1/payments/{accountId}/status` — get latest payment status for an account
- `DELETE /v1/payments/{accountId}` — cancel a pending payment
- `GET /v1/version` — service version info

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `BROKER_SERVICE_HOST` | `broker-service` | Broker service hostname |
| `BROKER_SERVICE_PORT` | `8080` | Broker service port |
| `CONFIRMATION_DELAY_SECONDS` | `10` | Delay between confirming → confirmed |
| `PROXY_PREFIX` | `bitcoin-payment-service` | URL path prefix |
| `PORT` | `8080` | Port this service listens on |

## Running Locally

```bash
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8080
```
