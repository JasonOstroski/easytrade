USE [TradeManagement]
GO
        CREATE TABLE [dbo].[BitcoinOrderStatus] (
                [Id] INT IDENTITY(1,1) NOT NULL,
                [BitcoinOrderId] nvarchar(36) NOT NULL,
                [Timestamp] DATETIME2 NOT NULL,
                [Status] VARCHAR(30) NOT NULL CHECK(
                        Status IN('order_created', 'payment_pending', 'payment_confirmed', 'payment_failed')
                ),
                [Details] nvarchar(500),
                CONSTRAINT [PK_BitcoinOrderStatus] PRIMARY KEY ([Id]),
                CONSTRAINT [FK_BitcoinOrderStatus_BitcoinOrders] FOREIGN KEY ([BitcoinOrderId]) REFERENCES BitcoinOrders([Id])
        )
GO
