import asyncio
import os
import uuid
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Optional

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

BROKER_SERVICE_HOST = os.getenv("BROKER_SERVICE_HOST", "broker-service")
BROKER_SERVICE_PORT = os.getenv("BROKER_SERVICE_PORT", "8080")
CONFIRMATION_DELAY_SECONDS = int(os.getenv("CONFIRMATION_DELAY_SECONDS", "10"))
PROXY_PREFIX = os.getenv("PROXY_PREFIX", "bitcoin-payment-service")
PORT = int(os.getenv("PORT", "8080"))

BROKER_BASE_URL = f"http://{BROKER_SERVICE_HOST}:{BROKER_SERVICE_PORT}"

PENDING_DELAY = 5
CONFIRMING_DELAY = CONFIRMATION_DELAY_SECONDS

payments: dict[int, dict] = {}


class PaymentRequest(BaseModel):
    accountId: int
    amount: float
    walletAddress: str


class DepositRequest(BaseModel):
    amount: float


async def process_payment(account_id: int, payment_id: str) -> None:
    await asyncio.sleep(PENDING_DELAY)

    payment = payments.get(account_id)
    if payment is None or payment["paymentId"] != payment_id or payment["status"] != "pending":
        return

    payment["status"] = "confirming"
    payment["timestamp"] = datetime.now(timezone.utc).isoformat()

    await asyncio.sleep(CONFIRMING_DELAY)

    payment = payments.get(account_id)
    if payment is None or payment["paymentId"] != payment_id or payment["status"] != "confirming":
        return

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{BROKER_BASE_URL}/v1/balance/{account_id}/deposit",
                json={"amount": payment["amount"]},
            )
            response.raise_for_status()
        payment["status"] = "confirmed"
    except Exception:
        payment["status"] = "failed"

    payment["timestamp"] = datetime.now(timezone.utc).isoformat()


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="Bitcoin Payment Service", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/v1/version")
async def version():
    return {"version": "1.0.0"}


@app.post("/v1/payments", status_code=201)
async def create_payment(request: PaymentRequest):
    existing = payments.get(request.accountId)
    if existing and existing["status"] in ("pending", "confirming"):
        raise HTTPException(
            status_code=409,
            detail=f"Account {request.accountId} already has an active payment",
        )

    payment_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    payments[request.accountId] = {
        "paymentId": payment_id,
        "accountId": request.accountId,
        "status": "pending",
        "walletAddress": request.walletAddress,
        "amount": request.amount,
        "timestamp": now,
    }

    asyncio.create_task(process_payment(request.accountId, payment_id))

    return {
        "paymentId": payment_id,
        "status": "pending",
        "walletAddress": request.walletAddress,
        "amount": request.amount,
    }


@app.get("/v1/payments/{account_id}/status")
async def get_payment_status(account_id: int):
    payment = payments.get(account_id)
    if payment is None:
        raise HTTPException(status_code=404, detail=f"No payment found for account {account_id}")

    return {
        "paymentId": payment["paymentId"],
        "status": payment["status"],
        "amount": payment["amount"],
        "walletAddress": payment["walletAddress"],
        "timestamp": payment["timestamp"],
    }


@app.delete("/v1/payments/{account_id}", status_code=204)
async def cancel_payment(account_id: int):
    payment = payments.get(account_id)
    if payment is None:
        raise HTTPException(status_code=404, detail=f"No payment found for account {account_id}")

    if payment["status"] not in ("pending",):
        raise HTTPException(
            status_code=409,
            detail=f"Payment in status '{payment['status']}' cannot be cancelled",
        )

    del payments[account_id]
