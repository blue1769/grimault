/*
 * Validation: Zero-Balancing Reconciliation for Credit Card Bills
 * Target Cards: '휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream'
 *
 * [상세 정황]
 * 2013년 2월/3월에 걸친 해당 카드 해지 완납 및 후청구(잔여) 발생으로
 * 월별 단식원장 상에는 일시적인 시차 오차(+/-301,469원)가 대칭으로 노출.
 * 단, 해당 기간 전체 청구액 합계(1,400,738원)와 실결제액 합계(1,400,738원)가
 * 1원 단위까지 완벽하게 일치하므로 정합성에 이상은 없음을 확인함. (Net Variance = 0원)
 *
 * [특이 사항]
 * 향후 릴리즈 직전 델타 마이그레이션 상황에 카드대금 대사 무결성 체크에서 재사용성 가치 검토됨.
 */

with transaction_sum as (
    select card_name,
           case
               when card_name in ('생활의지혜', 'Deep Dream') then date_trunc('month', entry_date - interval '6 days')::date
               when card_name in ('1Q Daily+') then date_trunc('month', entry_date - interval '22 days')::date
               when card_name in ('휘닉스') then date_trunc('month', entry_date - interval '5 days')::date
           end as settlement_month,
           sum(card_amount) as sum_amount
    from stage.outgo_curated
    where card_name in ('휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream')
    group by card_name, settlement_month
    order by card_name, settlement_month
), recorded_bills as (
    select replace(category, '카드대금>', '') as clean_name,
           case
               when category = '카드대금>1Q Daily+' then (date_trunc('month', entry_date) - interval '2 months')::date
               else (date_trunc('month', entry_date) - interval '1 months')::date
           end as billing_date,
           min(description) as description,
           sum(cash_amount) as bill_amount
    from stage.outgo_curated
    where category in ('카드대금>휘닉스', '카드대금>생활의지혜', '카드대금>1Q Daily+', '카드대금>Deep Dream')
    group by clean_name, billing_date
    order by clean_name, billing_date
), final as (
    select t.card_name,
           t.settlement_month,
           b.billing_date,
           b.description,
           t.sum_amount as transaction_sum,
           b.bill_amount as bill_amount,
           t.sum_amount - b.bill_amount as distance_amounts
    from transaction_sum t
    full outer join recorded_bills b on t.card_name = b.clean_name and t.settlement_month = b.billing_date
    order by t.settlement_month
)

select *
from final
where distance_amounts <> 0
;
