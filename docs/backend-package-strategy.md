# Backend Architecture & Package Strategy

## 1. 패키징 원칙 (Package by Feature / Domain)

- 기술 계층(Layer) 대신 **비즈니스 도메인(Feature)**을 최우선 경계로 설정하여 응집도를 높인다.
- 각 도메인은 독립된 모듈처럼 동작하며, 내부 구현 세부사항을 외부에 불필요하게 노출하지 않는다.

## 2. 패키지 간 의존성 규칙 (Dependency Rules)

### 단방향 의존성 원칙

- `domain` → `global` 참조는 허용하되, `global`에서 특정 `domain`을 역참조하지 않는다.
- 도메인 간 참조는 비즈니스 책임에 따라 명시적으로 정의하며, 순환 의존성을 허용하지 않는다.
- 공통 기능은 `global`에 배치하되, 도메인 고유의 비즈니스 로직은 해당 도메인 내부에서 관리한다.

### 도메인 캡슐화

- 외부 도메인에 노출할 필요가 없는 내부 구현은 Kotlin의 `internal` 또는 `private`을 활용하여 접근 범위를 제한한다.

### Entity와 DTO 분리

- `entity`는 도메인의 상태와 불변식을 관리하며, HTTP Request/Response 모델로 직접 노출하지 않는다.
- API 입출력은 도메인별 DTO를 통해 처리한다.

## 3. 패키지 구조 예시

아래 구조는 패키지 구성 원칙을 설명하기 위한 예시이며, 실제 도메인 및 클래스 구성과 일치할 필요는 없다.

```text
com.example.project
├── domain
│   ├── feature-a
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   │
│   └── feature-b
│       ├── controller
│       ├── dto
│       ├── entity
│       ├── repository
│       └── service
│
└── global
    ├── config
    ├── error
    └── common
```

- `domain`: 기능 및 도메인별 비즈니스 책임을 관리한다.
- `global`: 특정 도메인에 종속되지 않는 공통 인프라와 설정을 관리한다.
- 각 도메인의 하위 패키지는 실제 구현에 필요한 구성만 선택적으로 사용한다.