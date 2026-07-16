import React, { useState } from "react"
import {
    Card,
    CardContent,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
} from "@mui/material"
import DepositForm from "../../components/forms/DepositForm"
import BitcoinDepositForm from "../../components/forms/BitcoinDepositForm"
import DemoAppWarning from "../../components/DemoAppWarning"
import { deposit } from "../../api/creditCard/deposit/deposit"
import { useFeatureFlag } from "../../contexts/QueryContext/featureFlag/hooks"

type PaymentMethod = "creditCard" | "bitcoin"

export default function Deposit() {
    const [paymentMethod, setPaymentMethod] =
        useState<PaymentMethod>("creditCard")
    const bitcoinEnabled = useFeatureFlag("bitcoin_payments")

    return (
        <Card
            sx={{
                margin: "auto",
                maxWidth: "450px",
            }}
        >
            <CardContent>
                <Stack
                    spacing={2}
                    alignItems="center"
                    justifyContent="center"
                    direction="column"
                >
                    <DemoAppWarning />
                    {bitcoinEnabled && (
                        <>
                            <Typography variant="subtitle2" color="text.secondary">
                                Payment method
                            </Typography>
                            <ToggleButtonGroup
                                value={paymentMethod}
                                exclusive
                                onChange={(_, v) => v && setPaymentMethod(v)}
                                size="small"
                            >
                                <ToggleButton value="creditCard">
                                    Credit Card
                                </ToggleButton>
                                <ToggleButton value="bitcoin">
                                    ₿ Bitcoin
                                </ToggleButton>
                            </ToggleButtonGroup>
                        </>
                    )}
                    {paymentMethod === "creditCard" || !bitcoinEnabled ? (
                        <DepositForm submitHandler={deposit} />
                    ) : (
                        <BitcoinDepositForm />
                    )}
                </Stack>
            </CardContent>
        </Card>
    )
}
