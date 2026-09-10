-- =============================================================================
-- master seeds for `account`
-- =============================================================================
with unioned_account_sources as (
    select coalesce(bank_account, card_name) as name,
           case when bank_account is not null and card_name is not null then card_name end as payment_detail -- 추후 거래 내역 분개 작성 시 참조
    from stage.outgo
    union all
    select deposit_account as name, null as payment_detail
    from stage.income
    union all
    select distinct trim(split_part(category, '>', 2)) as name,
           null as payment_detail
    from stage.outgo
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
    from stage.outgo
    union all
    select distinct 'INCOME' as type,
                    trim(split_part(category, '>', 1)) as parent_name,
                    trim(split_part(category, '>', 2)) as sub_name
    from stage.income
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