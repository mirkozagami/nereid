# Mermaid Diagram Samples for Marketplace Screenshots

## 1. Flowchart - Software Development Process

```mermaid
flowchart TD
    subgraph Planning["Planning Phase"]
        A[Requirements] --> B[Design]
        B --> C{Approved?}
    end

    subgraph Development["Development Phase"]
        D[Coding] --> E[Code Review]
        E --> F[Testing]
    end

    subgraph Deployment["Deployment Phase"]
        G[Staging] --> H[Production]
        H --> I[Monitoring]
    end

    C -->|Yes| D
    C -->|No| B
    F -->|Pass| G
    F -->|Fail| D
    I -->|Issues| D

    style A fill:#ff6b6b,stroke:#c92a2a,color:#fff
    style B fill:#ffa94d,stroke:#e67700,color:#fff
    style C fill:#ffd43b,stroke:#fab005,color:#333
    style D fill:#69db7c,stroke:#2f9e44,color:#fff
    style E fill:#38d9a9,stroke:#0ca678,color:#fff
    style F fill:#4dabf7,stroke:#1c7ed6,color:#fff
    style G fill:#748ffc,stroke:#4c6ef5,color:#fff
    style H fill:#9775fa,stroke:#7950f2,color:#fff
    style I fill:#da77f2,stroke:#be4bdb,color:#fff
```

---

## 2. Sequence Diagram - Authentication Flow

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant C as Client App
    participant A as Auth Server
    participant R as Resource Server

    rect rgb(255, 235, 235)
        Note over U,C: Login Request
        U->>+C: Enter credentials
        C->>+A: POST /oauth/token
    end

    rect rgb(235, 255, 235)
        Note over A: Validate & Generate Token
        A-->>A: Verify credentials
        A-->>A: Generate JWT
        A-->>-C: Access Token + Refresh Token
    end

    rect rgb(235, 235, 255)
        Note over C,R: API Request
        C->>+R: GET /api/data (Bearer token)
        R-->>R: Validate JWT
        R-->>-C: Protected Resource
        C-->>-U: Display Data
    end
```

---

## 3. Class Diagram - E-Commerce System

```mermaid
classDiagram
    direction TB

    class Customer {
        +String id
        +String name
        +String email
        +List~Order~ orders
        +placeOrder()
        +viewHistory()
    }

    class Order {
        +String orderId
        +Date createdAt
        +OrderStatus status
        +List~LineItem~ items
        +calculateTotal()
        +cancel()
    }

    class Product {
        +String sku
        +String name
        +Money price
        +int stockLevel
        +Category category
        +updateStock()
    }

    class LineItem {
        +Product product
        +int quantity
        +Money subtotal
    }

    class Payment {
        +String transactionId
        +Money amount
        +PaymentMethod method
        +process()
        +refund()
    }

    Customer "1" --> "*" Order : places
    Order "1" --> "*" LineItem : contains
    LineItem "*" --> "1" Product : references
    Order "1" --> "1" Payment : has

    style Customer fill:#74c0fc,stroke:#1c7ed6
    style Order fill:#63e6be,stroke:#0ca678
    style Product fill:#ffd43b,stroke:#fab005
    style LineItem fill:#da77f2,stroke:#be4bdb
    style Payment fill:#ff8787,stroke:#fa5252
```

---

## 4. State Diagram - Order Lifecycle

```mermaid
stateDiagram-v2
    direction LR

    [*] --> Pending: Order Created

    Pending --> PaymentProcessing: Payment Initiated
    PaymentProcessing --> Confirmed: Payment Success
    PaymentProcessing --> Pending: Payment Failed

    Confirmed --> Preparing: Start Fulfillment
    Preparing --> Shipped: Dispatch
    Shipped --> Delivered: Arrival Confirmed

    Delivered --> [*]

    Pending --> Cancelled: User Cancels
    Confirmed --> Cancelled: Admin Cancels
    Cancelled --> [*]

    Delivered --> Refunded: Refund Requested
    Refunded --> [*]

    classDef pending fill:#ffd43b,stroke:#fab005,color:#333
    classDef processing fill:#74c0fc,stroke:#1c7ed6,color:#fff
    classDef success fill:#69db7c,stroke:#2f9e44,color:#fff
    classDef error fill:#ff8787,stroke:#fa5252,color:#fff

    class Pending pending
    class PaymentProcessing processing
    class Confirmed,Preparing,Shipped success
    class Delivered success
    class Cancelled,Refunded error
```

---

## 5. Entity Relationship Diagram - Blog Platform

```mermaid
erDiagram
    USER ||--o{ POST : writes
    USER ||--o{ COMMENT : makes
    USER {
        uuid id PK
        string username UK
        string email UK
        string password_hash
        timestamp created_at
    }

    POST ||--o{ COMMENT : has
    POST ||--|{ POST_TAG : tagged
    POST {
        uuid id PK
        uuid author_id FK
        string title
        text content
        enum status
        timestamp published_at
    }

    COMMENT {
        uuid id PK
        uuid post_id FK
        uuid user_id FK
        text body
        timestamp created_at
    }

    TAG ||--|{ POST_TAG : applied
    TAG {
        uuid id PK
        string name UK
        string color
    }

    POST_TAG {
        uuid post_id FK
        uuid tag_id FK
    }
```

---

## 6. Pie Chart - Technology Stack Usage

```mermaid
pie showData
    title Technology Distribution in Project
    "Kotlin" : 42
    "TypeScript" : 28
    "Python" : 15
    "Go" : 10
    "Other" : 5
```

---

## 7. Gantt Chart - Sprint Planning

```mermaid
gantt
    title Q1 2026 Development Roadmap
    dateFormat YYYY-MM-DD

    section Backend
        API Design           :done, api, 2026-01-01, 7d
        Database Schema      :done, db, after api, 5d
        Core Services        :active, core, after db, 14d
        Integration Tests    :int, after core, 7d

    section Frontend
        UI/UX Design         :done, design, 2026-01-01, 10d
        Component Library    :active, comp, after design, 12d
        Page Implementation  :pages, after comp, 14d
        E2E Testing          :e2e, after pages, 5d

    section DevOps
        CI/CD Pipeline       :crit, cicd, 2026-01-08, 7d
        Staging Environment  :stage, after cicd, 5d
        Production Setup     :prod, after stage, 5d
        Monitoring           :mon, after prod, 7d

    section Milestones
        Alpha Release        :milestone, m1, 2026-02-01, 0d
        Beta Release         :milestone, m2, 2026-02-20, 0d
        GA Release           :milestone, m3, 2026-03-15, 0d
```

---

## 8. Mindmap - Project Architecture

```mermaid
mindmap
    root((Microservices Platform))
        API Gateway
            Authentication
            Rate Limiting
            Load Balancing
        Services
            User Service
                Registration
                Profiles
                Preferences
            Product Service
                Catalog
                Inventory
                Pricing
            Order Service
                Cart
                Checkout
                Fulfillment
        Infrastructure
            Kubernetes
            Docker
            Terraform
        Observability
            Logging
                ELK Stack
            Metrics
                Prometheus
                Grafana
            Tracing
                Jaeger
```

---

## 9. Git Graph - Feature Branch Workflow

```mermaid
gitGraph
    commit id: "Initial" tag: "v1.0.0"
    branch develop
    checkout develop
    commit id: "Setup CI"

    branch feature/auth
    checkout feature/auth
    commit id: "Add login"
    commit id: "Add JWT"

    checkout develop
    branch feature/api
    checkout feature/api
    commit id: "REST endpoints"
    commit id: "Validation"

    checkout develop
    merge feature/auth id: "Merge auth" tag: "v1.1.0"

    checkout feature/api
    commit id: "Tests"

    checkout develop
    merge feature/api id: "Merge API"

    checkout main
    merge develop id: "Release" tag: "v1.2.0"

    checkout develop
    branch hotfix/security
    commit id: "Patch CVE"

    checkout main
    merge hotfix/security id: "Hotfix" tag: "v1.2.1"
```

---

## 10. Timeline - Product Evolution

```mermaid
timeline
    title Product Evolution 2024-2026

    section 2024
        Q1 : Initial Concept : Market Research : Prototype
        Q2 : Seed Funding : Team Formation
        Q3 : MVP Development : Alpha Testing
        Q4 : Private Beta : First 100 Users

    section 2025
        Q1 : Public Beta : 1,000 Users
        Q2 : Series A : Mobile App Launch
        Q3 : Enterprise Features : 10,000 Users
        Q4 : International Expansion : API Platform

    section 2026
        Q1 : Series B : AI Integration : 100,000 Users
```

---

## 11. Quadrant Chart - Technology Evaluation

```mermaid
quadrantChart
    title Technology Assessment Matrix
    x-axis Low Complexity --> High Complexity
    y-axis Low Value --> High Value

    quadrant-1 Strategic Investment
    quadrant-2 Quick Wins
    quadrant-3 Avoid
    quadrant-4 Careful Consideration

    Kubernetes: [0.75, 0.85]
    GraphQL: [0.55, 0.70]
    Microservices: [0.80, 0.75]
    Docker: [0.35, 0.80]
    REST API: [0.25, 0.65]
    Monolith: [0.20, 0.40]
    Custom ORM: [0.70, 0.30]
    Serverless: [0.45, 0.72]
```

---

## 12. Requirement Diagram - User Stories

```mermaid
requirementDiagram
    requirement user_authentication {
        id: REQ-001
        text: Users must authenticate before accessing protected resources
        risk: high
        verifymethod: test
    }

    requirement password_policy {
        id: REQ-002
        text: Passwords must be at least 12 characters with mixed case and symbols
        risk: medium
        verifymethod: inspection
    }

    requirement session_management {
        id: REQ-003
        text: Sessions expire after 30 minutes of inactivity
        risk: medium
        verifymethod: test
    }

    requirement mfa_support {
        id: REQ-004
        text: System shall support multi-factor authentication
        risk: high
        verifymethod: demonstration
    }

    element auth_module {
        type: module
        docref: AUTH-SPEC-001
    }

    auth_module - satisfies -> user_authentication
    auth_module - satisfies -> password_policy
    auth_module - satisfies -> session_management
    auth_module - satisfies -> mfa_support
    password_policy - refines -> user_authentication
    mfa_support - derives -> user_authentication
```

---

## 13. C4 Context Diagram - System Overview

```mermaid
C4Context
    title System Context Diagram - E-Commerce Platform

    Person(customer, "Customer", "A user who browses and purchases products")
    Person(admin, "Administrator", "Manages products, orders, and users")

    System(ecommerce, "E-Commerce Platform", "Allows customers to browse, purchase products and track orders")

    System_Ext(payment, "Payment Gateway", "Handles payment processing (Stripe)")
    System_Ext(shipping, "Shipping Provider", "Manages delivery logistics (FedEx)")
    System_Ext(email, "Email Service", "Sends transactional emails (SendGrid)")
    System_Ext(analytics, "Analytics", "Tracks user behavior (Mixpanel)")

    Rel(customer, ecommerce, "Browses, purchases", "HTTPS")
    Rel(admin, ecommerce, "Manages", "HTTPS")
    Rel(ecommerce, payment, "Processes payments", "API")
    Rel(ecommerce, shipping, "Creates shipments", "API")
    Rel(ecommerce, email, "Sends notifications", "SMTP")
    Rel(ecommerce, analytics, "Sends events", "API")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

---

## 14. Architecture Diagram - Kubernetes Deployment

```mermaid
architecture-beta
    group k8s(cloud)[Kubernetes Cluster]

    service ingress(internet)[Ingress Controller] in k8s
    service api(server)[API Gateway] in k8s
    service auth(server)[Auth Service] in k8s
    service users(server)[User Service] in k8s
    service orders(server)[Order Service] in k8s
    service cache(database)[Redis Cache] in k8s
    service db(database)[PostgreSQL] in k8s
    service queue(disk)[Message Queue] in k8s

    ingress:R --> L:api
    api:R --> L:auth
    api:B --> T:users
    api:B --> T:orders
    auth:R --> L:cache
    users:B --> T:db
    orders:B --> T:db
    orders:R --> L:queue
```

---

## Tips for Best Screenshots

1. **Use a light IDE theme** for better contrast
2. **Zoom to 100-150%** for crisp text
3. **Capture one diagram at a time** for clarity
4. **Include some surrounding context** (file tabs, gutter) to show IDE integration
5. **Use diagrams that showcase color** (flowcharts, class diagrams with styles)
