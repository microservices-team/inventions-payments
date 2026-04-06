# anyosa-payments — Arquitectura CQRS

## Diagrama 1 — Flujo completo CQRS

```mermaid
flowchart TB
    subgraph CLIENT["🌐 Cliente (React)"]
        UI["diegoanyosa.com\n/login → obtiene JWT"]
    end

    subgraph GATEWAY["🔀 API Gateway :8080"]
        GW["Spring Cloud Gateway\nValida JWT → inyecta\nX-User-Id, X-User-Roles"]
    end

    subgraph PAYMENTS["💳 anyosa-payments :8083"]
        direction TB

        subgraph REST["REST Layer"]
            TC["TransactionController\nPOST /api/payments/transactions\nGET  /api/payments/transactions\nPOST /api/payments/transactions/{id}/reverse"]
            AC["AccountController\nGET  /api/payments/accounts/me\nPOST /api/payments/accounts\nDELETE /api/payments/accounts/{userId}"]
        end

        subgraph CQRS_WRITE["✏️ WRITE SIDE — Command"]
            CMD["TransactionCommand\n(immutable value object)\ncommandId · senderId\nrecipientId · amount\ncurrency · requestedBy"]
            TCH["TransactionCommandHandler\n1. Idempotencia (commandId)\n2. Validar sender = requestedBy\n3. Cargar cuentas c/ lock\n4. Validar moneda y fondos\n5. debit() + credit()\n6. Guardar Transaction\n7. publishEvent()"]
            ACH["AccountCommandHandler\ncreateAccount()\ndeactivateAccount()"]
        end

        subgraph CQRS_READ["📖 READ SIDE — Query"]
            TQH["TransactionQueryHandler\n@EventListener → actualiza\nTransactionView\n\nsearch(userId, direction, status)\ngetAccount(userId)"]
        end

        subgraph EVENTS["📡 Domain Events"]
            EVT["TransactionEvent\neventId · transactionId\nstatus · amount · currency\noccurredAt"]
        end
    end

    subgraph DB["🐘 PostgreSQL — schema: payments"]
        direction LR
        WM[("transactions\nWrite Model\ncommand_id UNIQUE\nstatus · amount\nfailure_reason")]
        RM[("transaction_views\nRead Model\nProjection\ndesnormalizada")]
        UA[("user_accounts\nbalance · currency\nversion (optimistic lock)")]
    end

    UI -->|"POST /api/payments/transactions\nAuthorization: Bearer JWT"| GATEWAY
    GATEWAY -->|"X-User-Id injected"| TC
    TC --> CMD
    CMD --> TCH
    TCH -->|"UserAccount.debit()\nUserAccount.credit()"| UA
    TCH -->|"Transaction.COMPLETED\nTransaction.FAILED"| WM
    TCH -->|"ApplicationEventPublisher\n.publishEvent()"| EVT
    EVT -->|"@EventListener\nactualiza proyección"| TQH
    TQH --> RM
    TC -->|"GET ?direction=&status="| TQH
    TQH -->|"Page<TransactionResponse>"| TC
    AC --> ACH
    ACH --> UA
    AC --> TQH
```

---

## Diagrama 2 — API unificada de búsqueda

```mermaid
flowchart LR
    subgraph CLIENT["Cliente"]
        R1["GET /api/payments/transactions\n(todo el historial)"]
        R2["GET /api/payments/transactions\n?direction=SENT"]
        R3["GET /api/payments/transactions\n?direction=RECEIVED"]
        R4["GET /api/payments/transactions\n?status=FAILED"]
        R5["GET /api/payments/transactions\n?direction=SENT&status=COMPLETED"]
        R6["GET /api/payments/transactions\n?direction=RECEIVED&status=PENDING\n&page=0&size=10"]
    end

    subgraph CONTROLLER["TransactionController\nGET /api/payments/transactions"]
        PARAM["Query Params\ndirection: SENT | RECEIVED | ALL\nstatus: COMPLETED | FAILED\n        PENDING | REVERSED\npage: 0-based\nsize: items/page"]
    end

    subgraph QUERY["TransactionQueryHandler\n.search(userId, direction, status, page, size)"]
        NORM["Normalización\ndirection null → ALL\nstatus null → todos"]
        JPQL["JPQL Query\nWHERE (\n  direction=SENT     → senderId=userId\n  direction=RECEIVED → recipientId=userId\n  direction=ALL      → senderId OR recipientId\n)\nAND status = :status (si no es null)\nORDER BY createdAt DESC"]
    end

    subgraph DB["transaction_views"]
        TV[("Read Model\nProjection")]
    end

    R1 & R2 & R3 & R4 & R5 & R6 --> PARAM
    PARAM --> NORM --> JPQL --> TV
    TV -->|"Page<TransactionResponse>"| CLIENT
```

---

## Diagrama 3 — Flujo de una transferencia exitosa

```mermaid
sequenceDiagram
    actor U as Usuario A (sender)
    participant GW as API Gateway
    participant TC as TransactionController
    participant TCH as CommandHandler
    participant UA as UserAccount (DB)
    participant WM as transactions (DB)
    participant EVT as EventPublisher
    participant TQH as QueryHandler
    participant RM as transaction_views (DB)

    U->>GW: POST /api/payments/transactions\n{commandId, recipientId, amount: 100, currency: PEN}
    GW->>GW: Valida JWT → extrae X-User-Id
    GW->>TC: Request + X-User-Id header

    TC->>TCH: handle(TransactionCommand)

    TCH->>WM: existsByCommandId(commandId)?
    WM-->>TCH: false (primera vez)

    TCH->>UA: findActiveByUserIdForUpdate(senderId) [LOCK]
    UA-->>TCH: sender {balance: 500, currency: PEN}

    TCH->>UA: findActiveByUserIdForUpdate(recipientId) [LOCK]
    UA-->>TCH: recipient {balance: 100, currency: PEN}

    TCH->>TCH: sender.hasSufficientFunds(100) → true
    TCH->>UA: sender.debit(100) → balance: 400
    TCH->>UA: recipient.credit(100) → balance: 200
    TCH->>UA: save(sender) + save(recipient)

    TCH->>WM: save(Transaction{status: COMPLETED})
    WM-->>TCH: Transaction{id: uuid}

    TCH->>EVT: publishEvent(TransactionEvent{status: COMPLETED})
    EVT->>TQH: @EventListener onTransactionEvent()
    TQH->>RM: save(TransactionView{status: COMPLETED})

    TCH-->>TC: Transaction
    TC-->>U: 201 Created\n{id, status: COMPLETED, amount: 100}
```

---

## Diagrama 4 — Flujo de fondos insuficientes

```mermaid
sequenceDiagram
    actor U as Usuario A (sender)
    participant TCH as CommandHandler
    participant UA as UserAccount
    participant WM as transactions (DB)
    participant EVT as EventPublisher

    U->>TCH: handle(command{amount: 1000})

    TCH->>UA: findActiveByUserIdForUpdate(senderId)
    UA-->>TCH: {balance: 50, currency: PEN}

    TCH->>TCH: hasSufficientFunds(1000) → false

    TCH->>WM: save(Transaction{status: FAILED\nfailureReason: "Insufficient funds. Balance: 50"})
    TCH->>EVT: publishEvent(status: FAILED)

    TCH-->>U: 422 Unprocessable Entity\n{message: "Insufficient funds"}

    Note over UA: ⚠️ Balance NO modificado\nLa cuenta queda intacta
```

---

## Diagrama 5 — Idempotencia (reintento del cliente)

```mermaid
sequenceDiagram
    actor U as Cliente
    participant TCH as CommandHandler
    participant WM as transactions (DB)

    Note over U: Primera llamada
    U->>TCH: POST {commandId: "abc-123", amount: 100}
    TCH->>WM: existsByCommandId("abc-123") → false
    TCH->>WM: Procesa y guarda COMPLETED
    TCH-->>U: 201 {status: COMPLETED}

    Note over U: Red falla, cliente reintenta...

    Note over U: Segunda llamada (mismo commandId)
    U->>TCH: POST {commandId: "abc-123", amount: 100}
    TCH->>WM: existsByCommandId("abc-123") → true ✅
    WM-->>TCH: Transaction existente
    TCH-->>U: 201 {status: COMPLETED}

    Note over WM: ✅ No se duplica el débito\nSe retorna la misma transacción
```

---

## Diagrama 6 — Estructura de datos CQRS

```mermaid
erDiagram

    user_accounts {
        varchar user_id     PK "UUID del auth-service"
        numeric balance        "Saldo actual (19,4)"
        char    currency       "PEN / USD / EUR"
        boolean active
        bigint  version        "Optimistic locking"
        timestamp created_at
        timestamp updated_at
    }

    transactions {
        uuid    id          PK
        varchar command_id  UK "Idempotency key"
        varchar sender_id
        varchar recipient_id
        numeric amount
        char    currency
        varchar description
        varchar status         "PENDING|COMPLETED|FAILED|REVERSED"
        varchar failure_reason
        timestamp created_at
        timestamp updated_at
    }

    transaction_views {
        uuid    id          PK "Mismo UUID que transactions"
        varchar sender_id
        varchar sender_name    "Desnormalizado — sin JOIN"
        varchar sender_email
        varchar recipient_id
        varchar recipient_name
        varchar recipient_email
        numeric amount
        char    currency
        varchar status
        varchar failure_reason
        timestamp created_at
        timestamp updated_at
    }

    user_accounts ||--o{ transactions : "sender"
    user_accounts ||--o{ transactions : "recipient"
    transactions  ||--|| transaction_views : "proyectado por evento"
```
