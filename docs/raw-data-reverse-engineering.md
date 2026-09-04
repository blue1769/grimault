# Raw-data Reverse Engineering

# 접근 전략

## 데이터 기반 스코프 필터링

`raw.outgo` 와 `raw.income` 에 단 한 번도 쓰이지 않았거나 극소수 활용 기능은 탈락

## 행동 패턴 중심 도출

지난 기간 직접적으로 활용한 기능을 쿼리로 파악, 행동을 쾌적하게 지원하는 인터페이스만 여과

# 데이터 분석 핵심 아젠다

## 계정(Account) 및 결제 수단

- 현금 및 카드 복합 결제 행 없음
- 이력 내 고유 통장 및 카드 개수 (outgo, income 통합)
    - 현재 유효 계정: 6건 (현금성 4건, 신용카드 2건)
    - 전체 이력 내 계정 총 합계: 31건 (오등록 1건은 제외, 둘 모두 지정 안됨)
    - 쿼리 참조
        
        ```sql
        select Z.bank_account, Z.card_name, count(*) as cnt
        from (
            select bank_account, card_name
            from raw.outgo
            union all
            select deposit_account as bank_account, null as card_name
            from raw.income
        ) Z
        group by Z.bank_account, Z.card_name
        order by cnt desc
        ;
        ```


### 💡 스키마 확정 과정에서 ‘체크카드’를 독자 계정으로 취급할지에 관한 여부

- 실질적 거래는 실시간 은행 계좌에서 발생함
- 즉, 체크카드가 독자 계정일 경우 잔액 관리 난도 증가 우려
- **‘태그’ 필드가 아닌 ‘결제수단 태그’로 명시할 수 있다면 실제 은행 계좌로 매핑하는 편이 현실에 근접**

## 계정 간 이체 및 카드 대금 상환 패턴

- 통장 간 계좌 이체(혹은, 현금 인출)이나 카드 대금 납부는 이중 집계하지 않음
    - 네이버 작성 시 카테고리 분류에 ‘이체/대체’ 항목이 존재 (카드 대금 역시 ’카드대금’)
    - 이체/대체 항목은 관리 중인 모든 현금성 계정 목록을 노출하여 선택 가능
    - 카드 대금 납부는 연결 계좌에서 현금성 자산을 마이너스 (즉, 실제 차감 시점엔 카드 자체와는 무관)
    - 지출 내역으로 ‘출금 통장’ 항목을 선택한 뒤 ‘이체/대체’ 대상 고르면 자동 반영

        <aside>
        👉

      예시) 신한은행에서 현금 10만원 인출 시,

        - 지출 내역 작성 시 ‘출금 통장’을 신한은행으로 선택
        - 해당 지출 내역의 ‘분류’를 ‘이체/대체 > 지갑’으로 선택
        - ‘저장’ 시 신한은행 -10만원 & 지갑 +10만원 반영
        </aside>


### 💡 네이버의 ‘카드 대금’ 납부 데이터 마이그레이션 전략

- 네이버 가계부의 지출에 신용카드 사용 이력과 카드 대금을 모두 기록
- 이중 집계를 우회하는 꼼수로 네이버는 카드 대금을 현금 지출로 산정하지 않는 예외를 둠
- 현재 개발 단계에서 고려하는 이중원장으로의 스키마 전환 시 이대로 유지는 불가
- **즉, 카드 대금 결제내역은 신용카드 부채 상환하는 이체 분개로 일괄 변환이 불가피**

## 카테고리 깊이(Depth) 및 파편화 수준

- 모든 카테고리(outgo, income)는 항시 2단계로 고정 (네이버 가계부 특성)
- 적어도 한 번 이상 내역에 작성한 카테고리 총 수량은 72건 (이체/대체, 카드대금 카테고리 제외)
- 일회성 활용 등 유령 카테고리 존재 여부
    - 단 1건씩 기록된 내역으로는 총 3건 존재 (outgo 2건, income 1건)
    - 각 상세 내역 검출 결과 타 일반적인 카테고리로 전환 가능
    - 2단계 상세 분류에 ‘기타’로 등록한 내역은 일반적 타 분류로 조정 필요? (내역 총 752건)
- 쿼리 참조

    ```sql
    with category_raw as (
        select category
        from raw.outgo
        union all
        select category as category
        from raw.income
    ), categories as (
        select category, count(*) as cnt, (1 + length(category) - length(replace(category, '>', ''))) as depth
        from category_raw
        group by category
    )
    
    select sum(cnt)
    from categories
    where (category not like '이체/대체>%' and category not like '카드대금>%')
    -- and category like '%>기타%'
    order by cnt desc
    ;
    ```


### 💡 2차 카테고리 ‘기타’와 사용 빈도 낮은 카테고리 대응 방향 (정규화)

- **1회성 카테고리 3건은 수기로 수정하여 대표 카테고리로 전환 가능**
- **2차 카테고리 ‘기타’의 경우 왜곡 없이 그대로 ‘기타’로 흡수**
- **단, 개발된 신규 서비스에서는 가능하다면 ‘기타’로의 기록을 지양(노출 안함) 가용성 검토**

## 부가 필드의 실효성 검증

### ‘낭비’

- 전체 지출 내역 총 18,978건 중 795회 사용됨 (약 4.19%)
- 가장 최근 사용일: 2026년 1분기
- 네이버 가계부 내 보고서 항목에서 지정된 기간 내 낭비 내역을 리스트해서 보여주는 수준
- 단순 노출로 전월 가계부 내역에서 낭비했던 항목을 그냥 아는 수준

### ‘태그’

- 전체 지출 내역 18,978건 중 2,431회 사용됨 (약 12.81%)
- 전체 수입 내역 1,013건 중 60회 사용됨 (약 5.92%)
- 모임 회식, 데이트 등에서 함께했던 사람들에 대한 기록 (가계부에선 무의미, 일기장 느낌)
- 세뱃돈 등 거래 대상자에 대한 상세 정보 태깅 이력
- 혹은, 카드 결제/승인일시가 달라 불포함한 정보 내 실 결제일 정보 기록 등
- 네이버 가계부에 존재하는 해당 필드를 이용한 검색을 기대하였으나 미지원

### ‘사용처’와 ‘상세 내역’

- 둘 중 하나만 사용한 내역은 존재하지 않음.
- 사용처: 지역명 및 가게 상호, 혹은 온라인 몰 명칭 등
- 사용 내역: 해당 사용처에서 발생된 금액의 상세 항목 (구매 물품, 음식 메뉴 등)

    <aside>
    👉

  예시) 다양한 사용처와 상세 내역 조합

    - 사용처: 이동통신사 상호 / 상세 내역: 2026년 9월 이동통신요금
    - 사용처: YouTube Premium / 상세 내역: 2026년 9월 정기결제
    - 사용처: 이마트24 (ㅇㅇ동) / 상세 내역: 도시락
    - 사용처: 쿠팡 / 상세 내역: 쿠페이 머니 충전 (이체/대체 > 쿠페이머니)
    </aside>


# 주요 기능 후보 (Plan)

## Must-Have `v1 core`

- 당일 거래 기록 (지출/수입)
- 계정 간 이체 (복식부기 자산 이동)
- 기간별/키워드별 고속 검색
- 카테고리/결제수단 관리

## Nice-to-Have `v1.5 backlog`

- 고정지출(반복 거래) 템플릿
- 월별 카테고리 지출 합계/추이 요약
- 자주 쓰는 거래 빠른 입력

## Won’t-Have `non-goal`

- 월별 예산 대비 지출 알림 시각화
- 통계 차트 대시보드
- 영수증 OCR/SMS 자동 수신
- 낭비 지출 집중 분석 보고서

# 참고

## 이중원장 코어 스키마 구조

<aside>
💬

금융 원장 및 핀테크 도메인에서 사실상의 표준(De-facto Standard)로 통용되는 마틴 파울러의 Accounting Transaction Pattern(거래-분개 3단 구조)를 가볍게 축약한 스키마 설계와 예시

</aside>

### 이중원장 코어 스키마 표준 구조

전통 회계처럼 차변(Debit)/대변(Credit) 컬럼을 두지 않고 ‘부호형 단일 금액(Signed Amount: 증가+/감소-)’를 채택하여 쿼리와 집계에 직관성을 부여함

```sql
-- 1. 계정 마스터 (자산, 부채뿐 아니라 수입, 지출 카테고리도 '계정'으로 취급)
CREATE TABLE public.account (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,               -- 예: 국민은행통장, 신한카드, 식비, 급여
    type VARCHAR(20) NOT NULL,               -- ASSET, LIABILITY, EXPENSE, REVENUE
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 2. 거래 헤더 (언제, 어디서, 무슨 일이 일어났는가)
CREATE TABLE public.transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_date DATE NOT NULL,          -- 앞서 논의한 비즈니스 일자
    description VARCHAR(255) NOT NULL,       -- 거래 내역 (적요)
    created_at TIMESTAMPTZ DEFAULT now()     -- 시스템 등록 감사 일시
);

-- 3. 분개 원장 라인 (하나의 거래를 쪼갠 세부 자산 이동)
CREATE TABLE public.ledger_entry (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES public.transaction(id) ON DELETE CASCADE,
    account_id INT NOT NULL REFERENCES public.account(id),
    amount NUMERIC(15, 2) NOT NULL           -- 증가(+), 감소(-)
);
```

**핵심 불변식(Zero-Sum Constraint):**
하나의 `transaction_id` 에 속한 모든 `ledger_entry.amount` 의 합은 반드시 0이어야 함.
(즉, 누군가의 잔액이 차감된 만큼 다른 누군가의 잔액은 정확히 증가함.)

### 실제 일상 거래가 적재되는 형태 (예시)

1. 국민은행에서 카카오뱅크로 50,000원 계좌 이체
    - `transaction`: `id=1, date='2026-09-04', description='생활비 이체'`
    - `ledger_entry`:
        - 국민은행 (ASSET): -50,000원
        - 카카오뱅크 (ASSET): +50,000원
        - 합계: 0원 / 지출이나 수입으로 집계하지 않고 완벽히 자산 이동으로만 기록
2. 신용카드로 식당에서 저녁 식사 30,000원 결제
    - `transaction`: `id=2, date='2026-09-04', description='저녁 삼겹살'`
    - `ledger_entry`:
        - 신용카드 부채 (LIABILITY): -30,000원 (부채 증가 = 순자산 관점 마이너스)
        - 식비 (EXPENSE): +30,000원 (비용 증가)
        - 합계: 0원 / 통장에선 돈이 안 나갔지만 이번 달 지출 3만원과 카드 빚 3만원이 즉시 인식됨
3. 통장에서 카드 대금 30,000원 출금 (상환)
    - `transaction`: `id=3, date='2026-09-25', description='신한카드 결제대금 출금'`
    - `ledger_entry`:
        - 국민은행 (ASSET): -30,000원 (자산 감소)
        - 신용카드 부채 (LIABILITY): +30,000원 (부채 청산)
        - 합계: 0원 / 이미 삼겹살 먹을 때 식비로 잡힘, 카드 대금이 빠져나갈 때엔 지출로 잡지 않음

### 해당 구조가 주는 쿼리의 압도적 단순성

단식부기에서는 이체와 카드 결제 때문에 온갖 `CASE WHEN`과 셀프 조인이 난무하나, 이중원장에서는 모든 쿼리가 단순 `SUM` 하나로 끝낼 수 있음

- 특정 통장(국민은행)의 현재 실시간 잔액

    ```sql
    SELECT SUM(amount) 
    FROM public.ledger_entry 
    WHERE account_id = (SELECT id FROM public.account WHERE name = '국민은행');
    ```

- 이번 달 순수 총 지출 합계 (이체 내역 자동 필터링)

    ```sql
    SELECT SUM(e.amount) 
    FROM public.ledger_entry e
    JOIN public.account a ON e.account_id = a.id
    JOIN public.transaction t ON e.transaction_id = t.id
    WHERE a.type = 'EXPENSE' 
      AND t.transaction_date BETWEEN '2026-09-01' AND '2026-09-30';
    ```