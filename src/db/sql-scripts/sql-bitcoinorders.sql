USE [TradeManagement]
GO
        CREATE TABLE [dbo].[BitcoinOrders] (
                [Id] nvarchar(36) NOT NULL,
                [AccountId] INT NOT NULL,
                [Email] nvarchar(255) NOT NULL,
                [Name] nvarchar(101) NOT NULL,
                [WalletAddress] nvarchar(255) NOT NULL,
                [Amount] FLOAT NOT NULL,
                [Currency] VARCHAR(10) NOT NULL DEFAULT 'BTC',
                [DepositAddress] nvarchar(255) NOT NULL,
                [CreatedAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
                CONSTRAINT [PK_BitcoinOrders] PRIMARY KEY ([Id]),
                CONSTRAINT [FK_BitcoinOrders_Accounts] FOREIGN KEY ([AccountId]) REFERENCES Accounts([Id])
        )
GO
