-- =============================================================================
-- master seeds for `account`
-- =============================================================================
with unioned_account_sources as (
    select coalesce(bank_account, card_name) as name,
           case when bank_account is not null and card_name is not null then card_name end as payment_detail -- 추후 거래 내역 분개 작성 시 참조
    from stage.outgo_curated
    union all
    select deposit_account as name, null as payment_detail
    from stage.income_curated
    union all
    select distinct trim(split_part(category, '>', 2)) as name,
           null as payment_detail
    from stage.outgo_curated
    where category ilike '이체/대체>%'
), deduped_accounts as (
    select distinct name
    from unioned_account_sources
), eligible_accounts as (
    select name,
           case
            when name in ('전세대출(가양)', '전세대출(성남)', '휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream', '휘닉스-M') then 'LIABILITY'
            else 'ASSET'
           end as type,
           case
            when name in ('지갑') then 'CASH'
            when name in ('우리은행', '국민은행', '하나주택청약', '카카오뱅크', '신한은행', '기업은행', '언제든적금', '전세금') then 'BANK'
            when name in ('식권', '복지카드', '쿠페이머니', '파스쿠치카드', '긴급재난지원금', '보그헤어(선납)', '스타벅스(App)') then 'PREPAID'
            when name in ('전세대출(가양)', '전세대출(성남)') then 'LOAN'
            when name in ('휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream', '휘닉스-M') then 'CREDIT_CARD'
           end as sub_type,
           case
            when name in ('1Q Daily+') then 5
            when name in ('휘닉스', '생활의지혜', 'Deep Dream', '휘닉스-M') then 20
           end as settlement_day,
           case
            when name in ('신한은행', '지갑', '쿠페이머니', '스타벅스(App)', 'Deep Dream', '1Q Daily+') then true
            else false
           end as is_active
    from deduped_accounts
    where name is not null and name not in ('-', '어머니')
), final as (
    select *
    from eligible_accounts
    order by type, sub_type, name
)

insert into public.account (name, type, sub_type, settlement_day, is_active)
select name, type, sub_type, settlement_day, is_active
from final
;

-- =============================================================================
-- master seeds for `category`
-- =============================================================================
with unioned_category_sources as (
    select distinct 'EXPENSE' as type,
                    trim(split_part(category, '>', 1)) as parent_name,
                    trim(split_part(category, '>', 2)) as sub_name
    from stage.outgo_curated
    union all
    select distinct 'INCOME' as type,
                    trim(split_part(category, '>', 1)) as parent_name,
                    trim(split_part(category, '>', 2)) as sub_name
    from stage.income_curated
), marked_eligible_categories as (
    select type,
           parent_name,
           sub_name,
           case
               when (type = 'EXPENSE' and parent_name in ('이체/대체', '카드대금', '미분류')) then false
               when (type = 'INCOME' and parent_name in ('전월이월', '미분류')) then false
               else true
           end as is_eligible
    from unioned_category_sources
), final as (
    select type,
           parent_name,
           sub_name,
           true as is_active
    from marked_eligible_categories
    where is_eligible = true
    order by type, parent_name, sub_name
)

insert into public.category (type, parent_name, sub_name, is_active)
select type, parent_name, sub_name, is_active
from final;

-- ==========================================================================
-- `transaction` + `ledger_entry`
-- ==========================================================================
-- add income and outgo raw_id bridge column for migration tracking
alter table public.transaction add column if not exists outgo_raw_id int;
alter table public.transaction add column if not exists income_raw_id int;

-- [!WARNING] drop migration key after service release and delta migration
-- alter table public.transaction drop column if exists outgo_raw_id;
-- alter table public.transaction drop column if exists income_raw_id;

-- ==========================================================================
-- #1. journalize general expenses
-- ==========================================================================
begin;

create temp table tmp_eligible_expenses on commit drop as
with cleansing_source as (
    select raw_id,
           entry_date,
           coalesce(nullif(merchant, ''), description) as merchant,
           description,
           coalesce(nullif(cash_amount, 0), nullif(card_amount, 0)) as amount,
           coalesce(bank_account, card_name) as account_name,
           split_part(category, '>', 1) as parent_category,
           split_part(category, '>', 2) as sub_category,
           tags,
           is_waste,
           case when bank_account is not null and card_name is not null then card_name end as payment_method
    from stage.outgo_curated
), eligible_expenses as (
    select s.raw_id,
           s.entry_date,
           a.id as account_id,
           a.name as account_name,
           a.type as account_type,
           a.sub_type as account_sub_type,
           s.payment_method,
           s.amount,
           s.merchant,
           s.description,
           c.id as category_id,
           c.type as category_type,
           c.parent_name as category_parent_name,
           c.sub_name as category_sub_name,
           s.tags,
           s.is_waste
    from cleansing_source s
    left outer join public.account a on a.name = s.account_name
    left outer join public.category c on c.type = 'EXPENSE' and c.parent_name = s.parent_category and c.sub_name = s.sub_category
    where s.parent_category not in ('이체/대체', '카드대금')
)

select *
from eligible_expenses
;

-- (1) insert into transaction
insert into public.transaction (transaction_date, merchant, description, payment_method, tags, is_waste, outgo_raw_id)
select entry_date as transaction_date,
       merchant,
       description,
       payment_method,
       tags,
       is_waste,
       raw_id as outgo_raw_id
from tmp_eligible_expenses
;

-- (2) insert into ledger_entry
with journal_entry_bases as (
    select tx.id as transaction_id,
           ee.account_id,
           ee.category_id,
           ee.amount,
           ee.account_type as entry_type
    from public.transaction tx
    inner join tmp_eligible_expenses ee on ee.raw_id = tx.outgo_raw_id
)

insert into public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type)
select transaction_id,
       account_id,
       null as category_id,
       -amount as amount,
       entry_type
from journal_entry_bases
union all
select transaction_id,
       null,
       category_id,
       amount,
       'EXPENSE'
from journal_entry_bases
;

commit;

-- ==========================================================================
-- #2. journalize general incomes
-- ==========================================================================
begin;

create temp table tmp_eligible_incomes on commit drop as
with cleansing_sources as (
    select raw_id,
           entry_date,
           coalesce(nullif(trim(description), ''), '수입') as description,
           amount,
           deposit_account as account_name,
           split_part(category, '>', 1) as parent_category,
           split_part(category, '>', 2) as sub_category,
           tags
    from stage.income_curated
), eligible_incomes as (
    select s.raw_id,
           s.entry_date,
           a.id as account_id,
           a.name as account_name,
           a.type as account_type,
           a.sub_type as account_sub_type,
           s.amount,
           s.description,
           c.id as category_id,
           c.type as category_type,
           c.parent_name as category_parent_name,
           c.sub_name as category_sub_name,
           s.tags
    from cleansing_sources s
    left outer join public.account a on a.name = s.account_name
    left outer join public.category c on c.type = 'INCOME' and c.parent_name = s.parent_category and c.sub_name = s.sub_category
    where s.parent_category != '전월이월'
    and s.sub_category != '환불'
    and s.raw_id not in (767, 816, 832, 840, 852, 853, 865, 873, 884, 889, 891, 903, 913, 924, 928, 942, 958) -- 선불 자산 자금 이동 대상 17건
)

select *
from eligible_incomes
;

-- (1) insert into transaction
insert into public.transaction (transaction_date, merchant, description, payment_method, tags, is_waste, income_raw_id)
select entry_date as transaction_date,
       description as merchant,
       description,
       null as payment_method,
       tags,
       false as is_waste,
       raw_id as income_raw_id
from tmp_eligible_incomes
;

-- (2) insert into ledger_entry
with journal_entry_bases as (
    select tx.id as transaction_id,
           ei.account_id,
           ei.category_id,
           ei.amount
    from public.transaction tx
    inner join tmp_eligible_incomes ei on ei.raw_id = tx.income_raw_id
)

insert into public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type)
select transaction_id,
       account_id,
       null as category_id,
       amount as amount,
       'ASSET' as entry_type
from journal_entry_bases
union all
select transaction_id,
       null,
       category_id,
       -amount,
       'REVENUE'
from journal_entry_bases
;

commit;

-- ==========================================================================
-- #2-1. journalize refunded transaction
-- ==========================================================================
-- add target merchant and category columns refunded transaction
alter table stage.income_curated
    add column if not exists target_merchant varchar(100),
    add column if not exists target_category varchar(100)
;

begin;

create temp table tmp_eligible_refunds on commit drop as
with cleansing_sources as (
    select raw_id,
           entry_date,
           target_merchant as merchant,
           description,
           amount,
           deposit_account as account_name,
           split_part(target_category, '>', 1) as parent_category,
           split_part(target_category, '>', 2) as sub_category,
           tags
    from stage.income_curated
    where category = '부수입>환불'
    and raw_id != 622 -- 주수입>급여 오등록 내역 1건 (forced)
), eligible_refunds as (
    select s.raw_id,
           s.entry_date,
           a.id as account_id,
           a.name as account_name,
           a.type as account_type,
           a.sub_type as account_sub_type,
           s.amount,
           s.merchant,
           s.description,
           c.id as category_id,
           c.type as category_type,
           c.parent_name as category_parent_name,
           c.sub_name as category_sub_name,
           s.tags
    from cleansing_sources s
    left outer join public.account a on a.name = s.account_name
    left outer join public.category c on c.type = 'EXPENSE' and c.parent_name = s.parent_category and c.sub_name = s.sub_category
)

select *
from eligible_refunds
;

-- (1) insert into transaction
insert into public.transaction (transaction_date, merchant, description, payment_method, tags, is_waste, income_raw_id)
select entry_date as transaction_date,
       merchant,
       description,
       null as payment_method,
       tags,
       false as is_waste,
       raw_id as income_raw_id
from tmp_eligible_refunds
;

-- (2) insert into ledger_entry
with journal_entry_bases as (
    select tx.id as transaction_id,
           er.account_id,
           er.category_id,
           er.amount
    from public.transaction tx
    inner join tmp_eligible_refunds er on er.raw_id = tx.income_raw_id
)

insert into public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type)
select transaction_id,
       account_id,
       null as category_id,
       amount,
       'ASSET' as entry_type
from journal_entry_bases
union all
select transaction_id,
       null,
       category_id,
       -amount,
       'EXPENSE'
from journal_entry_bases
;

commit;

-- ==========================================================================
-- #3. journalize accounts-transfer transaction
-- ==========================================================================
begin;

create temp table tmp_eligible_transfers on commit drop as
with cleansing_source as (
    select raw_id,
           entry_date,
           description,
           coalesce(nullif(cash_amount, 0), nullif(card_amount, 0)) as amount,
           coalesce(bank_account, card_name) as account_name,
           split_part(category, '>', 1) as parent_category,
           split_part(category, '>', 2) as sub_category,
           tags,
           is_waste
    from stage.outgo_curated
), eligible_transfers as (
    select s.raw_id,
           s.entry_date,
           sa.id as source_account_id,
           sa.name as source_account_name,
           sa.type as source_account_type,
           sa.sub_type as source_account_sub_type,
           s.amount,
           s.sub_category as merchant, -- 거래 대상 (Counterparty)
           s.description,
           ta.id as target_account_id,
           ta.name as target_account_name,
           ta.type as target_account_type,
           ta.sub_type as target_account_sub_type,
           s.tags,
           s.is_waste
    from cleansing_source s
    left outer join public.account sa on sa.name = s.account_name
    left outer join public.account ta on ta.name = s.sub_category
    where s.parent_category in ('이체/대체', '카드대금')
)

select *
from eligible_transfers
;

-- (1) insert into transaction
insert into public.transaction (transaction_date, merchant, description, payment_method, tags, is_waste, outgo_raw_id)
select entry_date as transaction_date,
       merchant,
       description,
       null as payment_method,
       tags,
       is_waste,
       raw_id as outgo_raw_id
from tmp_eligible_transfers
;

-- (2) insert into ledger_entry
with journal_entry_bases as (
    select tx.id as transaction_id,
           et.source_account_id,
           et.source_account_type,
           et.target_account_id,
           et.target_account_type,
           et.amount
    from public.transaction tx
    inner join tmp_eligible_transfers et on et.raw_id = tx.outgo_raw_id
)

insert into public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type)
select transaction_id,
       source_account_id as account_id,
       null::integer as category_id,
       -amount as amount,
       source_account_type as entry_type
from journal_entry_bases
union all
select transaction_id,
       target_account_id,
       null,
       amount,
       target_account_type
from journal_entry_bases
;

commit;
