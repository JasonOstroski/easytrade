import axios, { AxiosInstance } from "axios"

export type BitcoinPaymentRequest = {
    accountId: number
    amount: number
    walletAddress: string
}

export type PaymentStatus = "pending" | "confirming" | "confirmed" | "failed"

export type BitcoinPaymentResponse = {
    paymentId: string
    status: PaymentStatus
    walletAddress: string
    amount: number
}

export type BitcoinPaymentStatusResponse = {
    paymentId: string
    status: PaymentStatus
    amount: number
    walletAddress: string
    timestamp: string
}

export class BitcoinBackend {
    private readonly agent: AxiosInstance

    constructor(bitcoinServiceUrl: string) {
        this.agent = axios.create({
            baseURL: bitcoinServiceUrl,
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
            },
        })
    }

    createPayment(request: BitcoinPaymentRequest) {
        return this.agent.post<BitcoinPaymentResponse>("/payments", request)
    }

    getPaymentStatus(accountId: number) {
        return this.agent.get<BitcoinPaymentStatusResponse>(
            `/payments/${accountId}/status`
        )
    }

    cancelPayment(accountId: number) {
        return this.agent.delete(`/payments/${accountId}`)
    }
}
