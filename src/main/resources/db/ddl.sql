-- 1. 스키마가 없으면 생성
CREATE SCHEMA IF NOT EXISTS payment;

SET search_path TO payment;

-- 2. manager 계정에 payment 스키마에 대한 모든 권한 부여 (이미 있으면 아무 일도 없음)
GRANT ALL ON SCHEMA payment TO manager;

-- 3. manager 계정에 payment 스키마 내 모든 테이블에 권한 부여 (이미 있으면 아무 일도 없음)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA payment TO manager;

-- 4. manager 계정에 payment 스키마 내 모든 시퀀스에 권한 부여 (시퀀스가 생성되는 경우에 대비)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA payment TO manager;

create table if not exists payment.payment_user
(
    id                integer generated always as identity
        constraint payment_user_pk
            primary key,
    email             varchar(200)                              not null
        constraint payment_user_email_unique_key
            unique,
    password          varchar(300)                              not null,
    last_name         varchar(50)                               not null,
    first_name        varchar(50)                               not null,
    is_deleted        varchar(1) default 'F'::character varying not null,
    created_timestamp timestamp  default now()                  not null,
    updated_timestamp timestamp  default now()                  not null
);

comment on table payment.payment_user is '결제 유저';
comment on column payment.payment_user.id is '결제 사용자 고유번호';
comment on constraint payment_user_pk on payment.payment_user is '결제 사용자 기본키';
comment on column payment.payment_user.last_name is '사용자 성';
comment on column payment.payment_user.first_name is '사용자 이름';
comment on column payment.payment_user.is_deleted is '사용자 삭제 여부';
comment on column payment.payment_user.email is '결제 사용자 이메일(아이디)';
comment on column payment.payment_user.password is '결제 사용자 비밀번호';
comment on column payment.payment_user.created_timestamp is '사용자 생성시간';
comment on column payment.payment_user.updated_timestamp is '사용자 수정시간';
alter table payment.payment_user owner to manager;

create table if not exists payment.payment_user_card
(
    id                 integer generated always as identity
        constraint payment_user_card_pk
            primary key,
    user_id            integer                                   not null
        constraint payment_user_card_payment_user_id_fk
            references payment.payment_user,
    card_number_masked varchar(4)                                not null,
    card_company       varchar(50)                               not null,
    card_type          varchar(20)                               not null,
    expiration_year    varchar(4)                                not null,
    expiration_month   varchar(2)                                not null,
    pg_company         varchar(30)                               not null,
    pg_company_code    varchar(20)                               not null,
    billing_key        varchar(200)                              not null,
    billing_key_status varchar(20)                               not null,
    is_deleted         varchar(1) default 'F'::character varying not null,
    created_timestamp  timestamp  default now()                  not null,
    updated_timestamp  timestamp  default now()                  not null
);

comment on table payment.payment_user_card is '결제 유저 결제수단(카드)';
comment on column payment.payment_user_card.id is '사용자 결제 수단 고유번호';
comment on column payment.payment_user_card.user_id is '사용자 고유 번호';
comment on constraint payment_user_card_payment_user_id_fk on payment.payment_user_card is '사용자 고유번호 외래키';
comment on column payment.payment_user_card.card_number_masked is '결제 수단 카드 번호 뒷 4자리 (표시용, ****-****-****-1234)';
comment on column payment.payment_user_card.card_company is '결제 수단 카드 회사명(신한, 국민 등)';
comment on column payment.payment_user_card.card_type is '결제 수단 카드 종류(신용, 체크 등)';
comment on column payment.payment_user_card.expiration_year is '결제 수단 카드 만료(연도)';
comment on column payment.payment_user_card.expiration_month is '결제 수단 카드 만료(월)';
comment on column payment.payment_user_card.pg_company is '결제 수단 PG 회사명(이니시스, KCP, 토스 등) ';
comment on column payment.payment_user_card.pg_company_code is '결제 수단 PG 회사 코드(INICIS, KCP, TOSS 등)';
comment on column payment.payment_user_card.billing_key is '결제 수단 PG사가 발급한 빌링키/토큰(customer_uid 등)';
comment on column payment.payment_user_card.billing_key_status is '결제 수단 PG사가 발급한 빌링키 상태(정상/만료/취소)(ACTIVE,EXPIRED,CANCELED)';
comment on column payment.payment_user_card.is_deleted is '결제 수단 카드 삭제 여부';
comment on column payment.payment_user_card.created_timestamp is '결제 수단 카드 생성 시간';
comment on column payment.payment_user_card.updated_timestamp is '결제 수단 카드 수정 시간';
alter table payment.payment_user_card owner to manager;

create table if not exists payment.payment_user_point
(
    id                integer generated always as identity
        constraint payment_user_point_pk
            primary key,
    user_id           integer                  not null
        constraint payment_user_point_payment_user_id_fk
            references payment.payment_user,
    paid_point        integer    default 0     not null
        constraint paid_point_more_than_zero
            check (paid_point >= 0),
    free_point        integer    default 0     not null
        constraint free_point_more_than_zero
            check (free_point >= 0),
    is_deleted        varchar(1) default 'F'::character varying,
    created_timestamp timestamp  default now() not null,
    updated_timestamp timestamp  default now() not null
);

comment on table payment.payment_user_point is '결제 사용자 포인트';
comment on column payment.payment_user_point.id is '결제 사용자 포인트 고유번호';
comment on column payment.payment_user_point.user_id is '사용자 고유번호';
comment on constraint payment_user_point_payment_user_id_fk on payment.payment_user_point is '결제 사용자 포인트 외래키';
comment on column payment.payment_user_point.paid_point is '결제 사용자 유상 포인트';
comment on constraint paid_point_more_than_zero on payment.payment_user_point is '유상포인트 0 이상 체크';
comment on column payment.payment_user_point.free_point is '결제 사용자 무상 포인트';
comment on constraint free_point_more_than_zero on payment.payment_user_point is '무상포인트 0 이상 체크';
comment on column payment.payment_user_point.is_deleted is '사용자 포인트 지갑 삭제 여부';
comment on column payment.payment_user_point.created_timestamp is '포인트 지갑 생성시간';
comment on column payment.payment_user_point.updated_timestamp is '포인트 지갑 수정시간';
alter table payment.payment_user_point owner to manager;

create table if not exists payment.payment
(
    id                  integer generated always as identity
        constraint payment_pk
            primary key,
    user_id             integer                                   not null
        constraint payment_payment_user_id_fk
            references payment.payment_user,
    reference_id        integer,
    reference_type      varchar(20),
    payment_method_type varchar(30)                               not null,
    payment_method_id   integer                                   not null,
    payment_type        varchar(30)                               not null,
    total_amount              integer                             not null,
    payment_result_code varchar(2)                                not null,
    requested_timestamp timestamp  default now()                  not null,
    approved_timestamp  timestamp,
    canceled_timestamp  timestamp,
    failed_timestamp    timestamp,
    external_payment_id varchar(200),
    error_code          varchar(20),
    error_message       varchar(300),
    idempotency_key     varchar(100)                              not null,
    transaction_id      varchar(100)                              not null,
    is_deleted          varchar(1) default 'F'::character varying not null,
    created_timestamp   timestamp  default now()                  not null,
    updated_timestamp   timestamp  default now()                  not null
);

comment on table payment.payment is '결제 테이블';
comment on column payment.payment.id is '결제 고유 번호';
comment on column payment.payment.user_id is '결제 유저 고유 번호';
comment on constraint payment_payment_user_id_fk on payment.payment is '결제 사용자 고유 번호';
comment on column payment.payment.reference_id is '결제 대상 고유번호(주문, 라이선스, 멤버십, 청구, 강의 등) ';
comment on column payment.payment.reference_type is '결제 대상 유형(ORDER/LICENSE/MEMBERSHIP/BILL/LECTURE)';
comment on column payment.payment.payment_method_type is '결제 수단(CARD, ACCOUNT, EASYPAY)';
comment on column payment.payment.payment_method_id is '결제 수단 고유 번호';
comment on column payment.payment.payment_type is '결제 유형(NORMAL, SUBCRIPTION, SPLIT)';
comment on column payment.payment.total_amount is '결제 금액';
comment on column payment.payment.payment_result_code is '결제 상태 코드(00: 결제 대기, 11: 결제 요청, 22: 결제 완료, 33: 결제 실패, 44: 결제취소';
comment on column payment.payment.requested_timestamp is '결제 요청 시간';
comment on column payment.payment.approved_timestamp is '결제 승인 시간';
comment on column payment.payment.canceled_timestamp is '결제 취소 시간';
comment on column payment.payment.failed_timestamp is '결제 실패 시간';
comment on column payment.payment.external_payment_id is '외부 결제사(PG사 등)에서 관리하는 결제의 고유 식별자';
comment on column payment.payment.error_code is '결제 실패 PG 에러 코드';
comment on column payment.payment.error_message is '결제 실패 PG 에러 메시지';
comment on column payment.payment.idempotency_key is '결제 멱등성 키(클라이언트 전달)';
comment on column payment.payment.transaction_id is '결제 트랜잭션 고유번호';
comment on column payment.payment.is_deleted is '결제 삭제 여부';
comment on column payment.payment.created_timestamp is '결제 생성시간';
comment on column payment.payment.updated_timestamp is '결제 수정시간';

alter table payment.payment owner to manager;

create table if not exists payment.payment_detail
(
    id                         integer generated always as identity
        constraint payment_detail_pk
            primary key,
    payment_id                 integer                                   not null
        constraint payment_detail_payment_id_fk
            references payment.payment,
    item_id                    integer,
    item_type                  integer,
    amount                     integer                                   not null,
    payment_detail_result_code varchar(2)                                not null,
    is_deleted                 varchar(1) default 'F'::character varying not null,
    created_timestamp          timestamp  default now()                  not null,
    updated_timestamp          timestamp  default now()                  not null
);

comment on table payment.payment_detail is '결제 상세 행위(여러 결제 수단 부분 취소/복합 결제)';
comment on column payment.payment_detail.id is '결제 상세 행위 고유 번호';
comment on column payment.payment_detail.payment_id is '결제 고유 번호';
comment on constraint payment_detail_payment_id_fk on payment.payment_detail is '결제 테이블 외래키';
comment on column payment.payment_detail.item_id is '결제 상세 행위의 아이템 고유 번호(상품/쿠폰/포인트 등 상세 항목ID)';
comment on column payment.payment_detail.item_type is '결제 상세 행위의 아이템 타입(PRODUCT/COUPON/POINT)';
comment on column payment.payment_detail.amount is '결제 상세 해당 내역 금액(포인트 차감/쿠폰 할인 등)';
comment on column payment.payment_detail.payment_detail_result_code is '각 결제 상세의 상태 코드(00: 대기, 22: 완료, 33: 실패, 44: 취소)';
comment on column payment.payment_detail.created_timestamp is '결제 상세 생성시간';
comment on column payment.payment_detail.updated_timestamp is '결제 상세 수정시간';
comment on column payment.payment_detail.is_deleted is '결제 상세 삭제 여부';
alter table payment.payment_detail owner to manager;

create table if not exists payment.payment_history
(
    id                integer generated always as identity
        constraint payment_history_pk
            primary key,
    payment_id        integer                 not null
        constraint payment_history_payment_id_fk
            references payment.payment,
    prev_result_code  varchar(2),
    new_result_code   varchar(2)              not null,
    changed_at        timestamp               not null,
    changed_by        varchar(30)             not null,
    changed_reason    varchar(200),
    prev_data         jsonb,
    new_data          jsonb                   not null,
    external_response jsonb,
    transaction_id    varchar(100)            not null,
    created_timestamp timestamp default now() not null,
    updated_timestamp timestamp default now() not null
);

comment on table payment.payment_history is '결제 이력 테이블';
comment on column payment.payment_history.id is '결제 이력 고유 번호';
comment on column payment.payment_history.payment_id is '결제 고유 번호';
comment on constraint payment_history_payment_id_fk on payment.payment_history is '결제 외래키';
comment on column payment.payment_history.prev_result_code is '결제 변경 전 상태 코드';
comment on column payment.payment_history.new_result_code is '결제 변경 후 상태 코드';
comment on column payment.payment_history.changed_at is '결제 상태 변경 업무 이벤트 시간(PG 승인, 카프카 컨슈머 메시지, 관리자 수동 변경)';
comment on column payment.payment_history.changed_by is '결제 상태 변경자(SYSTEM, PG_API, KAFKA_CONSUMER, ADMIN, SCHEDULER)';
comment on column payment.payment_history.changed_reason is '결제 상태 변경 이유';
comment on column payment.payment_history.prev_data is '상태 변경 전의 결제 데이터(스냅샷)';
comment on column payment.payment_history.new_data is '상태 변경 후의 결제 데이터(스냅샷)';
comment on column payment.payment_history.external_response is '결제 상태 변경 시 외부 시스템 원본 응답값( PG사, 포인트/쿠폰 API)';
comment on column payment.payment_history.transaction_id is '결제 트랜잭션 고유번호';
comment on column payment.payment_history.created_timestamp is '결제 이력 생성 시간';
comment on column payment.payment_history.updated_timestamp is '결제 이력 수정 시간';
alter table payment.payment_history owner to manager;

create table if not exists payment.payment_user_point_history
(
    id                 integer generated always as identity
        constraint payment_user_point_history_pk
            primary key,
    user_id            integer                                         not null
        constraint payment_user_point_history_payment_user_point_id_fk
            references payment.payment_user_point,
    related_payment_id integer
        constraint payment_user_point_history_payment_id_fk
            references payment.payment,
    point_type         varchar(4)                                      not null,
    point_action_type  varchar(1)                                      not null,
    point              integer                                         not null,
    before_paid_point  integer                                         not null,
    after_paid_point   integer                                         not null,
    before_free_point  integer                                         not null,
    after_free_point   integer                                         not null,
    point_group_key    varchar(200)                                    not null,
    changed_by         varchar(30) default 'SYSTEM'::character varying not null,
    created_timestamp  timestamp   default now()                       not null,
    updated_timestamp  timestamp   default now()                       not null
);

comment on table payment.payment_user_point_history is '사용자 포인트 이력 테이블';
comment on column payment.payment_user_point_history.id is '사용자 포인트 이력 고유 번호';
comment on column payment.payment_user_point_history.user_id is '사용자 고유 번호';
comment on constraint payment_user_point_history_payment_user_point_id_fk on payment.payment_user_point_history is '포인트 액션 이력 사용자 외래키';
comment on column payment.payment_user_point_history.related_payment_id is '포인트 액션 이력 관련 결제 고유 번호(충전/환불)';
comment on constraint payment_user_point_history_payment_id_fk on payment.payment_user_point_history is '포인트 액션 이력 결제 외래키';
comment on column payment.payment_user_point_history.point_type is '포인트 유형(PAID: 유상, FREE: 무상)';
comment on column payment.payment_user_point_history.point_action_type is '포인트 유형(A: 충전, B: 전환, C : 취소, E: 적립, R: 환불, U: 사용)';
comment on column payment.payment_user_point_history.point is '포인트 액션 이력 변동 포인트(+, -)';
comment on column payment.payment_user_point_history.before_paid_point is '포인트 액션 이력 변경 전 유상 잔액';
comment on column payment.payment_user_point_history.after_paid_point is '포인트 액션 이력 변경 후 유상 잔액';
comment on column payment.payment_user_point_history.before_free_point is '포인트 액션 이력 변경 전 무상 잔액';
comment on column payment.payment_user_point_history.after_free_point is '포인트 액션 이력 변경 후 무상 잔액';
comment on column payment.payment_user_point_history.point_group_key is '포인트 액션 그룹키(예시 : 유상 + 무상 차감)';
comment on column payment.payment_user_point_history.changed_by is '포인트 액션 이행자(SYSTEM, ADMIN)';
comment on column payment.payment_user_point_history.created_timestamp is '포인트 액션 이력 생성 시간';
comment on column payment.payment_user_point_history.updated_timestamp is '포인트 액션 이력 수정 시간';
alter table payment.payment_user_point_history owner to manager;
