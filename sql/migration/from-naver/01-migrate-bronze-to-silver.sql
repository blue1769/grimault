-- =============================================================================
-- outgo: raw -> stage
-- =============================================================================
with casted_raw_outgo as (
    select id as raw_id,
           to_date(entry_date, 'YYYY"년"MM"월"DD"일"') as entry_date,
           nullif(trim(merchant), '') as merchant,
           nullif(trim(description), '') as description,
           coalesce(nullif(replace(cash_amount, ',', ''), '')::bigint, 0) as cash_amount,
           coalesce(nullif(replace(card_amount, ',', ''), '')::bigint, 0) as card_amount,
           nullif(trim(bank_account), '') as bank_account,
           nullif(trim(card_name), '') as card_name,
           nullif(trim(category), '') as category,
           nullif(trim(tags), '') as tags,
           case when trim(is_waste) = 'V' then true else false end as is_waste
    from raw.outgo
), filtered_raw_outgo as (
    select *
    from casted_raw_outgo
    where (cash_amount <> 0 or card_amount <> 0)
    and (bank_account is not null or card_name is not null)
)

insert into stage.outgo (raw_id, entry_date, merchant, description, cash_amount, card_amount, bank_account, card_name, category, tags, is_waste)
select raw_id, entry_date, merchant, description, cash_amount, card_amount, bank_account, card_name, category, tags, is_waste
from filtered_raw_outgo
;

-- =============================================================================
-- income: raw -> stage
-- =============================================================================
with casted_raw_income as (
    select id as raw_id,
           to_date(entry_date, 'YYYY"년"MM"월"DD"일"') as entry_date,
           nullif(trim(description), '') as description,
           coalesce(nullif(replace(amount, ',', ''), '')::bigint, 0) as amount,
           nullif(trim(deposit_account), '') as deposit_account,
           nullif(trim(category), '') as category,
           nullif(trim(tags), '') as tags
    from raw.income
), filtered_raw_income as (
    select *
    from casted_raw_income
    where amount <> 0
)

insert into stage.income (raw_id, entry_date, description, amount, deposit_account, category, tags)
select raw_id, entry_date, description, amount, deposit_account, category, tags
from filtered_raw_income
;