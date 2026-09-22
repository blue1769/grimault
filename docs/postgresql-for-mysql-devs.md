# 1. 스토리지 구조: "Clustered Index"가 아닌 "Heap Table"

> [!IMPORTANT]
> PostgreSQL의 보조 인덱스는 명시하지 않은 PK를 쥐고 있지 않으므로 필요 시 반드시 직접 인덱스에 명시 필수

> [!WARNING]
> 테이블 본체(Heap) 접근 자체를 생략하여 디스크 I/O를 생략하는 커버링 인덱스(Index-only Scan) 효과가 필요한 경우

## MySQL (InnoDB)
- 테이블 자체가 PK 순서대로 물리 정렬된 B+Tree (= Clustered Index)
- 모든 보조 인덱스(Secondary Index)의 리프 노드에는 행의 데이터 대신 PK 값이 암묵적으로 포함

## PostgreSQL
- 테이블 본체는 정렬 없이 무작위로 데이터 블록에 담긴 Heap 파일 그 자체
- PK, Index 모두 물리적으로는 완전히 같은 별도 B-Tree 파일
- 리프 노드가 가리키는 건 Heap Block의 물리 좌표인 `ctid` (Block#, Offset#)

# 2. Partial & Covering: B-Tree를 극단적으로 가볍게 활용

> [!NOTE]
> MySQL(InnoDB) 등에서 미지원하거나 제약으로 인해 실사용 불가능했던 기능의 적극적 채택 검토

## Partial Index
```sql
CREATE INDEX idx_children
    ON public.family_relationship (resident_id, related_resident_id)
    WHERE type = 'CHILD';
```
- 전체 테이블이 아닌 특정 조건식을 만족하는 행만 B-Tree에 등록 가능
- 인덱스 크기를 절반 이하로 압축하고 쓰기 오버헤드를 줄이며 캐시 히트율은 극대화

## Covering Index
```sql
CREATE INDEX IF NOT EXISTS idx_ledger_entry_category
    ON public.ledger_entry (category_id)
    INCLUDE (amount)
    WHERE category_id IS NOT NULL;
```
- 인덱스 탐색 Key로 사용하지 않지만 리프 노드에 칼럼 값을 함께 적재
- 대시보드 등의 통계 목적의 집계 시 Table Heap Full Scan을 회피하고 Index-Only Scan을 유도할 때 유용

# 3. MVCC와 쓰기 Lifecycle: "Undo 로그"가 아닌 "Append-only + VACUUM"

> [!WARNING]
> PostgreSQL의 잦은 업데이트는 테이블과 인덱스를 비대화(Bloat)하는 주요 원인

> [!NOTE]
> (역설) 그래서 오히려 수정/삭제를 지양하는 모델링의 경우 PostgreSQL의 철학과 궁합이 좋음 (예: 이중 원장)

## MySQL (Undo Log)
- 행을 UPDATE하면 원본 블록을 직접 고치고 이전 스냅샷은 Undo 영역에 체인으로 남김
- 백그라운드에서 Purge Thread가 Undo를 세그먼트를 연속 감시하며 참조 트랜잭션이 모두 종료된 시점에 지연 없이 비동기 회수

## PostgreSQL (Append-only + VACUUM)
- **디스크 블록을 직접 제자리에서 덮어쓰지 않음**
- UPDATE = 이전 행 논리 삭제(xmax, Soft Delete) + 새 행 힙 블록에 신규 INSERT(xmin) 하는 구조
- 쌓이는 Dead Tuple(쓰레기 행) → 백그라운드에서 VACUUM 데몬이 디스크 공간을 재활용 가능한 빈 영역으로 지속 회수 필요

# 4. 타입과 네임스페이스: 엄격한 정합성과 2-Part 네임스페이스

## `VARCHAR(255)`의 무의미함
- MySQL에서는 메모리 임시 테이블 크기로 인해 칼럼 사이즈에 예민했으나
- PostgreSQL은 내부적으로 `VARCHAR(n)`과 `TEXT`가 완전히 동일한 가변 바이트 구조체(`varlena`)로 존재
- 길이에 명확한 비즈니스 룰이 없다면 고민 없이 `TEXT`를 써도 무방

## 엄격한 boolean과 타입 검증
- MySQL의 `TINYINT(1)`을 boolean의 대안으로 활용할 때 불가피한 암묵적 형 변환(`1 = true`)이 불필요

## 2-Part 네임스페이스
- 단일 데이터베이스 내 논리적 네임스페이스인 스키마를 자유롭게 나눌 수 있음
- 단일 커넥션 풀(HikariCP)과 단일 트랜잭션(ACID) 안에서 안전하게 교차 쿼리 가능