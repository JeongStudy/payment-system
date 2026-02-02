
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
- Spring Security + JWT 기반 인증/인가
- 향후 간편 결제 등 여러 결제 수단 확장

---

## 2. 기술 스택

| 영역           | 내용                                                                    |
| -------------- | ---------------------------------------------------------------------- |
| Language       | Java 21                                                                |
| Framework      | Spring Boot 3.5.3, Spring MVC, Spring Data JPA, Spring Security        |
| DB             | PostgreSQL                                                             |
| Message Broker | Apache Kafka                                                           |
| Cache          | Redis                                                                  |
| Auth           | JWT (Json Web Token)                                                   |
| Test           | JUnit 5, Testcontainers                                                |
| Infra/Dev      | Docker, Docker Compose, Kubernetes(GKE 샘플 설정), GitHub Actions(.github) |
| Utils          | Lombok                                                                 |

---

## 3. 아키텍처 개요

### 3-1. 시스템 구성

- **API 서버 (Spring MVC + Security)**
    - 사용자 인증/인가 (JWT)
    - 결제 요청 API 제공
    - 결제 비즈니스 유효성 검사, 검증, 데이터 가공
    - 결제 엔티티 + Outbox 메시지를 동일 트랜잭션으로 DB에 저장

- **Outbox Scheduler**
  - Outbox 테이블에서 “미전송” 상태의 레코드를 주기적으로 조회
  - Kafka로 결제 이벤트 발행
  - 발행 성공 시 Outbox 상태 업데이트(전송 완료 플래그)

- **결제 처리 워커 (Kafka Consumer)**
  - Kafka Topic에서 결제 이벤트 메시지 소비
  - 실제 결제 처리(PG 승인/실패 시뮬레이션)
  - 결제 상태 업데이트, 트랜잭션 로그 기록
  - 재시도/장애 상황 처리 전략

- **Kafka**
  - 결제 요청/처리 이벤트를 전달하는 중앙 메시지 브로커

- **PostgreSQL**
  - 회원 정보, 결제 정보, Outbox 메시지 등 영속 데이터 저장소

- **Redis**
  - 결제 관련 캐시 및 세션 관리

### 3-2. 아키텍처 다이어그램 (개념)

``` mermaid
flowchart LR
    C[Client] -->|JWT Auth| API[Payment API Server]

    subgraph DB[PostgreSQL]
        U[Users]
        P[Payments]
        O[Outbox]
    end

    API -->|"회원 가입/로그인"| U
    API -->|"결제 요청 트랜잭션\nPayment + Outbox INSERT"| P
    API -->|"결제 요청 트랜잭션\nPayment + Outbox INSERT"| O

    subgraph Scheduler[Outbox Scheduler]
        OB[Outbox Publisher]
    end

    OB -->|"미전송 Outbox 조회"| O
    OB -->|"결제 이벤트 발행"| K[Kafka Topic]

    K --> W[Payment Worker]
    W -->|"결제 처리/상태 업데이트"| P

```

## 4. 핵심 기능

### 4-1. 인증 및 사용자 관리
- **회원가입/로그인**: BCrypt 비밀번호 암호화, JWT 발급
- **인증 필터**: 요청 시 JWT 검증 및 SecurityContext 설정
- **권한 관리**: Spring Security 기반 접근 제어

### 4-2. 결제 요청 API
- **기능**: 카드 결제 요청, 검증
- **트랜잭션**: 결제 정보와 Outbox 이벤트를 하나의 트랜잭션으로 저장 (At-least-once 보장 초석)

### 4-3. Outbox 패턴 및 스케줄러
- **역할**: DB에 저장된 이벤트를 Kafka로 발행
- **보장**: API 서버와 Kafka 간 연결 장애 시에도 데이터 유실 방지

### 4-4. 결제 처리 워커 (Consumer)
- **역할**: Kafka 메시지 소비 후 실제 결제 로직 수행
- **처리**: 결제 상태 업데이트 (PENDING -> APPROVED/FAILED)

---

## 5. 실행 방법 (Runbook)

### 5-1. 사전 준비
- Java 21
- Docker, Docker Compose

### 5-2. 프로젝트 클론
```bash
git clone https://github.com/JeongStudy/payment-system.git
cd payment-system
```

### 5-3. 인프라 실행
```bash
docker compose up -d
```

### 5-4. Kafka 토픽 생성
```bash
./create-topics.sh
```

### 5-5. 애플리케이션 실행
```bash
./gradlew clean build
java -jar build/libs/*.jar
```

---

## 6. 프로젝트 구조

```
src/main/java/com/system/payment
├── config       # Security, Kafka, Scheduler 등 설정
├── common       # 공통 유틸리티, 예외 처리, 필터
├── user         # 사용자 도메인 (Controller, Service, Repository)
├── payment      # 결제 도메인 (Controller, Service, Producer, Consumer)
├── outbox       # Outbox 패턴 (Scheduler, Service)
├── card         # 카드 결제 관련
└── pg           # PG사 연동 관련
```

---

## 7. API 사용 방법

1. **회원가입**: `/api/auth/signup`
2. **로그인**: `/api/auth/login` (응답받은 Token을 Header에 포함)
3. **결제 요청**: `/api/payment/...` (Authorization: Bearer {Token})

---

## 8. 테스트

- **Unit Test**: JUnit 5
- **Integration Test**: Testcontainers (PostgreSQL, Kafka 컨테이너 활용)
