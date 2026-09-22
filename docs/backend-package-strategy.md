# Backend Architecture & Package Strategy

## 1. 패키징 원칙 (Package by Feature / Domain)
- 기술 계층(Layer) 대신 **비즈니스 도메인(Feature)**을 최우선 경계로 설정하여 응집도를 극대화한다.
- 각 도메인은 독립된 모듈처럼 동작하며, 도메인 내부 구현 세부사항은 패키지 외부로 불필요하게 노출하지 않는다.

---

## 2. 패키지 간 의존성 규칙 (Dependency Rules)

```text
[ global ] ◄── (공통 인프라/설정/예외)
   ▲
   │ (참조 허용, 역참조 금지)
   │
[ account ]     [ category ]
   ▲                 ▲
   └─── [ ledger ] ──┘  (코어 원장은 마스터 도메인을 단방향 참조)
```

### (1) 단방향 의존성 원칙:
- `domain` ➔ `global` 참조는 허용하지만, `global`이 특정 `domain`의 클래스를 역참조하는 것은 엄격히 금지한다.
- `ledger`(원장)는 분개 매핑을 위해 `account`와 `category`를 참조할 수 있다.
- 반대로 `account`와 `category`는 `ledger`의 존재를 전혀 알지 못해야 한다. (단방향 유지)

### (2) 도메인 캡슐화:
- 외부 도메인에 노출할 필요가 없는 내부 클래스(내부 계산 로직, 전용 Helper 등)는 Kotlin의 `internal` 또는 `private`을 적극 활용한다.

### (3) Entity vs DTO 분리:
- `entity`는 도메인의 상태와 불변식을 보호하며, Controller 레이어 바깥(HTTP Request/Response)으로 절대 직접 노출하지 않는다.
- Controller와의 통신은 각 도메인 내 `dto` 패키지(추후 생성)의 Request/Response 객체로만 수행한다.

## 3. 디렉토리 구조 명세

```text
com.blustar.grimault
├── domain
│   ├── account
│   │   ├── entity
│   │   │   ├── Account.kt
│   │   │   ├── AccountType.kt          # Enum (ASSET, LIABILITY, EQUITY)
│   │   │   └── AccountSubType.kt       # Enum (BANK, CASH, PREPAID, CREDIT_CARD)
│   │   ├── repository
│   │   │   └── AccountRepository.kt
│   │   └── service                     # 비즈니스 로직
│   │
│   ├── category
│   │   ├── entity
│   │   │   ├── Category.kt
│   │   │   └── CategoryType.kt         # Enum (EXPENSE, INCOME)
│   │   └── repository
│   │       └── CategoryRepository.kt
│   │
│   └── ledger                          # 코어 원장 (Transaction + LedgerEntry)
│       ├── entity
│       │   ├── Transaction.kt          # 거래 헤더
│       │   ├── LedgerEntry.kt          # 분개 엔트리 (FK: Account, Category)
│       │   └── EntryType.kt            # Enum (DEBIT, CREDIT / ASSET, EXPENSE 등)
│       └── repository
│           ├── TransactionRepository.kt
│           └── LedgerEntryRepository.kt
│
└── global                              # 공통 인프라/유틸/예외 처리
    ├── common                          # BaseTimeEntity 등 공통 엔티티 추상화
    ├── config                          # JPA Auditing, DB 설정 등
    └── error                           # GlobalExceptionHandler, ErrorCode, CustomException
```
