import React, { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import {
    Button,
    CardActions,
    TextField,
    Typography,
    Alert,
    CircularProgress,
    Box,
} from "@mui/material"
import { useForm } from "react-hook-form"
import { FormContainer, TextFieldElement } from "react-hook-form-mui"
import { z } from "zod"
import { Stack } from "@mui/system"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { NumberFormField } from "../NumberFormField"
import { useAuthUser } from "../../contexts/UserContext/context"
import { useAuthUserData } from "../../contexts/UserContext/hooks"
import { useFormatter } from "../../contexts/FormatterContext/context"
import { balanceInvalidateQuery } from "../../contexts/QueryContext/user/queries"
import useStatusDisplay from "../../hooks/useStatusDisplay"
import StatusDisplay from "../StatusDisplay"
import { backends } from "../../api/backend"

const MOCK_WALLET = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"

const formSchema = z.object({
    amount: z
        .number({ required_error: "Amount is required" })
        .positive("Amount must be greater than 0"),
    walletAddress: z
        .string()
        .min(26, "Invalid Bitcoin wallet address")
        .max(62, "Invalid Bitcoin wallet address"),
    agreementCheck: z.boolean().refine((v) => v, "Must agree to terms"),
})

type FormData = z.infer<typeof formSchema>

const defaultValues: FormData = {
    amount: 0,
    walletAddress: "",
    agreementCheck: false,
}

export default function BitcoinDepositForm() {
    const { userId } = useAuthUser()
    const { balance } = useAuthUserData()
    const { formatCurrency } = useFormatter()
    const queryClient = useQueryClient()
    const { setError, setSuccess, resetStatus, statusContext } =
        useStatusDisplay()

    const accountId = Number(userId)

    const formContext = useForm<FormData>({
        defaultValues,
        resolver: zodResolver(formSchema),
        resetOptions: { keepDefaultValues: true },
    })
    const { watch, reset, setValue } = formContext

    const { data: paymentStatus, refetch: refetchStatus } = useQuery({
        queryKey: ["bitcoin-payment-status", accountId],
        queryFn: async () => {
            const res = await backends.bitcoin.getPaymentStatus(accountId)
            return res.data
        },
        enabled: false,
        retry: false,
    })

    const { mutate, isPending } = useMutation({
        mutationFn: async ({ amount, walletAddress }: FormData) => {
            const res = await backends.bitcoin.createPayment({
                accountId,
                amount,
                walletAddress,
            })
            return res.data
        },
        onMutate: resetStatus,
        onSuccess: async () => {
            setSuccess(
                "Bitcoin payment initiated. Awaiting blockchain confirmation..."
            )
            reset()
            refetchStatus()
            const poll = setInterval(async () => {
                const res = await backends.bitcoin.getPaymentStatus(accountId)
                if (res.data.status === "confirmed") {
                    clearInterval(poll)
                    await balanceInvalidateQuery(queryClient)
                    setSuccess("Bitcoin deposit confirmed and credited to your account!")
                } else if (res.data.status === "failed") {
                    clearInterval(poll)
                    setError(new Error("Bitcoin payment failed. Please try again."))
                }
                refetchStatus()
            }, 3000)
        },
        onError: (err: Error) => {
            setError(err)
        },
    })

    useEffect(() => {
        const { unsubscribe } = watch(() => resetStatus())
        return unsubscribe
    }, [watch])

    function autofillForm() {
        if (formContext.getValues().amount === defaultValues.amount) {
            setValue("amount", 500, { shouldValidate: true })
        }
        setValue("walletAddress", MOCK_WALLET, { shouldValidate: true })
        setValue("agreementCheck", true, { shouldValidate: true })
    }

    return (
        <FormContainer
            onSuccess={(data: FormData) => mutate(data)}
            formContext={formContext}
        >
            <Stack direction="column" spacing={2}>
                <TextField
                    name="balance"
                    label="Current balance"
                    value={
                        balance?.value === undefined
                            ? "Loading..."
                            : formatCurrency(balance.value)
                    }
                    disabled
                    fullWidth
                    slotProps={{ htmlInput: { "data-dt-content": true } }}
                />
                <NumberFormField
                    id="amount"
                    name="amount"
                    label="Amount (USD equivalent)"
                    type="number"
                    required
                    autoFocus
                    fullWidth
                    slotProps={{ htmlInput: { "data-dt-content": true } }}
                />
                <TextFieldElement
                    id="walletAddress"
                    name="walletAddress"
                    label="Your Bitcoin wallet address"
                    required
                    fullWidth
                    slotProps={{ htmlInput: { "data-dt-content": true } }}
                />
                <Alert severity="info" sx={{ fontSize: "0.8rem" }}>
                    Funds will be credited after blockchain confirmation (~15 seconds in demo mode).
                </Alert>
                <Typography variant="body2">* Required field</Typography>
                <CardActions sx={{ justifyContent: "center" }}>
                    <Button
                        id="submitBitcoinButton"
                        loading={isPending}
                        type="submit"
                        variant="outlined"
                    >
                        Deposit with Bitcoin
                    </Button>
                    <Button type="button" variant="outlined" onClick={autofillForm}>
                        Autofill
                    </Button>
                </CardActions>

                {paymentStatus && (
                    <Box sx={{ textAlign: "center" }}>
                        <Typography variant="body2" color="text.secondary">
                            Payment status:{" "}
                            <strong>{paymentStatus.status}</strong>
                        </Typography>
                        {(paymentStatus.status === "pending" ||
                            paymentStatus.status === "confirming") && (
                            <CircularProgress size={16} sx={{ ml: 1 }} />
                        )}
                    </Box>
                )}

                <StatusDisplay {...statusContext} />
            </Stack>
        </FormContainer>
    )
}
