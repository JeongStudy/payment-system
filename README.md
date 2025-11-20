
# payment-system : 결제 시스템
- 대규모 결제 트랜잭션을 위한 결제 백엔드 시스템
- Kafka를 이용해 결제 요청(API)과 결제 처리(Worker)를 분리하고, 예외/장애 실패를 재처리할 수 있는 구조를 실험·학습하기 위한 프로젝트

---
## 1. 개요

이 프로젝트는 다음과 같은 요구를 가진 결제 시스템을 학습·실험하기 위한 백엔드 애플리케이션이다.

- 대규모 결제 트래픽 처리
- 결제 요청, 대기, 완료 등 복잡한 결제 플로우 분리
- Outbox 패턴 기반의 안전한 이벤트 발행
- Kafka 기반 비동기 메시지 처리
- 결제 실패 및 재처리 플로우 지원
- 향후 간편 결제 등 여러 결제 수단 확장

---

## 2. 기술 스택

| 영역           | 내용                                                                    |
| -------------- | ---------------------------------------------------------------------- |
| Language       | Java 21                                                                |
| Framework      | Spring Boot 3.5.3, Spring MVC, Spring Data JPA                         |
| DB             | PostgreSQL                                                             |
| Message Broker | Apache Kafka                                                           |
| Cache          | Redis                                                                  |
| Test           | JUnit 5                                                                |
| Infra/Dev      | Docker, Docker Compose, Kubernetes(GKE 샘플 설정), GitHub Actions(.github) |

---

## 3. 아키텍처 개요

### 3-1. 시스템 구성

- 결제 요청 API 서버 (Spring MVC)
    - 결제 요청 API 제공
    - 결제 비즈니스 유효성 검사, 검증, 데이터 가공, 생성 및 업데이트 수행
    - 결제 엔티티 + Outbox 메시지를 동일 트랜잭션으로 DB에 저장

- Outbox 배치
  - Outbox 테이블에서 “미전송” 상태의 레코드를 조회
  - Kafka로 결제 이벤트 발행
  - 발행 성공 시 Outbox 상태 업데이트(전송 완료 플래그)

- 결제 처리 워커 (Kafka Consumer, 동일 코드베이스 내 별 모듈/역할)
  - Kafka Topic에서 결제 이벤트 메시지 소비
  - 실제 결제 처리(PG 승인/실패 시뮬레이션)
  - 결제 상태 업데이트, 트랜잭션 로그 기록
  - 재시도/장애 상황 처리 전략 설계 기반

- Kafka
  - 결제 요청/처리 이벤트를 전달하는 중앙 메시지 브로커
  - 추후 장애/실패 메시지 별도 Topic, 재처리 전략으로 확장 가능

- PostgreSQL
  - 결제 정보, 결제 내역, Outbox 메시지, 트랜잭션 로그 등 영속 데이터 저장소

- Redis
  - 결제 관련 캐시(예: idempotency key, 세션, 토큰 등)

### 3-2. 아키텍처 다이어그램 (개념)

``` mermaid
flowchart LR
    C[Client] --> API[Payment API Server]

    subgraph DB[PostgreSQL]
        P[Payments]
        O[Outbox]
    end

    API -->|"결제 요청 트랜잭션\nPayment + Outbox INSERT"| P
    API -->|"결제 요청 트랜잭션\nPayment + Outbox INSERT"| O

    subgraph Batch[Outbox Batch Job]
        OB[Outbox Reader/Publisher]
    end

    OB -->|"미전송 Outbox 조회"| O
    OB -->|"결제 이벤트 발행"| K[Kafka Topic]

    K --> W[Payment Worker]
    W -->|"결제 처리/상태 업데이트"| P

```

## 4. 핵심 기능

### 4-1. 결제 요청 API

- 기능
  - 카드 결제 / 간편 결제 등 요청 API 설계
  - 금액, 주문, 사용자, 결제 수단 등의 기본 검증

- 트랜잭션 구조
  - 결제 엔티티 저장
  - 결제 히스토리 저장
  - Outbox 테이블에 결제 이벤트 레코드 저장 둘을 하나의 트랜잭션으로 커밋해 “결제는 성공했는데 이벤트는 안 나간다” 상황 방지

- 공통 응답
  - { code, message, data } 형태의 공통 Response DTO
  - 실패 시 { code, message, errors[] } 형태로 전역 예외 처리

### 4-2. Outbox 배치

- 역할
  - Outbox 테이블에서 status = PENDING 등 미전송 상태 메시지 조회
  - Kafka Topic으로 이벤트 발행
  - 발행 성공 시 해당 Outbox 레코드 상태를 SENT로 업데이트

- 목표
  - API 서버와 Kafka 간의 일시적 장애/네트워크 문제에도 DB에 안전하게 쌓인 Outbox를 기준으로 “최소 한 번(at least once)” 이벤트 발행 보장

### 4-3. 결제 처리 워커

- Kafka Consumer
  - 특정 Topic(예: payment.requested.v1)을 구독
  - 메시지 단위로 결제 처리 로직 수행

- 비즈니스 로직
  - 결제 승인/실패 처리
  - 결제 상태 업데이트 및 트랜잭션 로그 기록
  - 멱등성/중복 처리 키 적용

### 4-4. 예외 처리 및 공통 응답

- GlobalExceptionHandler 기반 전역 예외 처리
- CustomException 도입으로 도메인별 에러 코드 관리
- 클라이언트 입장에서 일관된 응답 포맷 제공

---

## 5. 실행 방법 (Runbook)

### 5-1. 사전 준비

- Java 21
- Docker, Docker Compose
- Git

### 5-2. 프로젝트 클론

```
git clone https://github.com/JeongStudy/payment-system.git
cd payment-system
```


### 5-3. 의존 서비스 실행 (PostgreSQL, Kafka, Redis 등)

```
# Docker 백그라운드 실행
docker compose up -d

# 컨테이너 확인
docker ps
```
포트 정보는 docker-compose.yml을 참고한다.

### 5-4. Kafka 토픽 생성

```
./create-topics.sh
```
스크립트 내에서 결제 관련 Topic 설정(이름, 파티션 수 등)을 확인할 수 있다.

### 5-5. 애플리케이션 실행

```
./gradlew clean build
java -jar build/libs/*.jar
```
IDE에서 실행 시 메인 클래스(PaymentSystemApplication 등)를 선택해서 실행하면 된다.

---

## 6. 프로젝트 구조

---
7. API 사용 방법

- 레포 루트의 requests/ 디렉터리에 결제 관련 HTTP 샘플이 포함되어 있다면, 다음과 같은 흐름으로 API를 테스트할 수 있다.
  1. 결제 요청 생성 API 호출
  2. 결제 상태 조회 API 호출
  3. Outbox 배치 실행
  4. Kafka → Worker → 결제 상태 업데이트 확인
- Postman, Insomnia 또는 IDE의 HTTP Client(IntelliJ .http) 등을 사용하면 된다.

---

## 8. 테스트

- JUnit 5 기반 테스트 코드
  - 결제 요청 서비스 단위 테스트
  - Outbox 저장 및 조회 서비스 단위 테스트
  - Outbox 배치 카프카 메시지 전송 통합 및 인수 테스트


---

## 9. 목표 및 기대 효과

### 목표

- 결제 트랜잭션 처리 및 동시성 문제 해결
- Outbox 패턴을 이용한 결제 트랜잭션 정합성 확보
- DB 트랜잭션과 메시지 발행 간의 불일치 문제 방지
- 대규모 트래픽에 대한 분산 처리
- Kafka 기반 비동기 이벤트 처리 파이프라인 설계
- 메시지 기반 순서 보장
- 장애/실패 상황에서의 재처리·복구 전략 실험

### 기대 효과

- 안정적이고 신뢰성 있는 결제 처리 플로우 설계 경험
- Outbox + Kafka 조합을 다른 도메인(포인트, 정산 등)에 재사용 가능
- 대규모 트래픽/데이터 처리 시스템 설계 역량 강화
- 빠르고 효율적인 오류/실패 복구
