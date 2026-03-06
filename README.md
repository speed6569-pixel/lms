# LMS 프로젝트 수행계획서 (3주 집중형 / Spring Boot + Thymeleaf)

## 1) 프로젝트 개요

- **프로젝트명**: LMS (Learning Management System)
- **개발 기간**: 3주 (핵심 기능 MVP)
- **개발 목표**: 학습 운영에 필요한 최소 핵심 기능을 빠르게 구현하고, 이후 확장 가능한 구조를 확보한다.
- **개발 경로**: `~/project/lms`
- **개발 방식**: 서버 렌더링(Thymeleaf) 중심 구현 → 화면/기능 검증 → 문서화

---

## 2) 목표와 성공 기준 (Definition of Done)

### 2.1 핵심 목표
1. 역할 기반 인증/인가 체계를 갖춘다. (ADMIN / INSTRUCTOR / STUDENT)
2. 강의 생성~조회~수정~삭제(CRUD)가 동작한다.
3. 수강 신청/취소 기능이 동작한다.
4. 강의별 공지 등록/조회가 동작한다.
5. 화면(Thymeleaf) + 핵심 기능 + 기본 테스트가 포함된 상태로 마무리한다.

### 2.2 완료 기준 (DoD)
- [ ] 주요 화면/요청 흐름이 브라우저에서 정상 동작
- [ ] 인증 없는 접근 차단, 권한별 접근 제어 확인
- [ ] 핵심 도메인(User/Course/Enrollment/Notice) CRUD 또는 필수 기능 동작
- [ ] 공통 예외 응답 포맷 통일
- [ ] 최소 단위/통합 테스트 작성 및 통과
- [ ] README + 실행 방법 + ERD + API 명세 정리 완료

---

## 3) 범위 정의

## 3.1 MVP 포함 범위 (3주)

### A. 인증/인가
- 회원가입
- 로그인 (세션 기반)
- 사용자 역할(Role) 기반 접근 제어

### B. 강의 관리
- 강의 생성 (INSTRUCTOR/ADMIN)
- 강의 목록/상세 조회
- 강의 수정/삭제 (권한 체크)

### C. 수강 관리
- 수강 신청 (STUDENT)
- 수강 취소
- 내 수강 목록 조회

### D. 공지 관리
- 강의 공지 등록 (INSTRUCTOR/ADMIN)
- 강의 공지 목록/상세 조회

### E. 품질/운영
- 공통 예외 처리
- 입력값 검증(Validation)
- 화면/기능 명세 문서화
- 기본 테스트 코드

## 3.2 MVP 제외 범위 (후속)
- 과제/제출, 퀴즈/시험
- 파일 업로드(S3)
- 실시간 알림
- 통계 대시보드 고도화
- 프론트엔드 완성 UI

---

## 4) 기술 스택 및 환경

- **Language**: Java 17 (처음 학습에 안정적)
- **Framework**: Spring Boot 3.x
- **Template Engine**: Thymeleaf
- **Build**: Gradle
- **DB**: MySQL 8.x
- **Data Access**: JdbcTemplate (Repository에서 SQL 직접 작성/확인)
- **JPA 사용 범위**: 테이블 매핑 검증/스키마 검토 용도(핵심 조회/저장은 JdbcTemplate 기준)
- **Security**: Spring Security (폼 로그인 + 세션 기반, 학습 난이도 완화)
- **Test**: JUnit5, MockMvc
- **Dev Tools(선택)**: Lombok (가독성 우선이라 최소 사용 권장)

---

## 5) 아키텍처 및 설계 원칙

## 5.1 아키텍처
- 단일 Spring Boot 애플리케이션 (모듈형 패키지 구조)
- 계층 구조: `WebController → Service → JdbcRepository → DB`
- 화면(Thymeleaf) + 서버 로직을 명확히 분리
- 도메인 중심으로 패키지 분리

### 패키지 구조 예시

```text
com.example.lms
 ├─ common
 │   ├─ config
 │   ├─ exception
 │   └─ util
 ├─ auth
 │   ├─ web
 │   ├─ service
 │   └─ repository
 ├─ user
 ├─ course
 ├─ enrollment
 ├─ notice
 └─ view
     ├─ layout
     ├─ course
     ├─ enrollment
     └─ notice
```

## 5.2 설계 원칙 (초심자 친화)
- 비즈니스 로직은 Service 계층에만 작성
- Controller는 화면 이동/요청 파라미터 처리에 집중
- Repository는 JdbcTemplate로 SQL을 명시적으로 작성 (쿼리 가시성 확보)
- 메서드 이름은 길어도 의미가 드러나게 작성
- 한 메서드는 한 가지 일만 하도록 작게 유지
- DTO/폼 객체를 분리해 입력 흐름을 읽기 쉽게 유지
- 예외는 도메인별 Custom Exception + Global Handler 처리
- JPA는 엔티티-테이블 매핑 검증 용도로 제한적 사용

## 5.3 코드 가독성 규칙 (입문자 기준)
- 컨트롤러 메서드는 30줄 내외로 유지
- 서비스 메서드는 "입력 검증 → 조회/계산 → 저장" 순서 고정
- SQL은 멀티라인 문자열로 작성하고, WHERE/JOIN을 줄바꿈해 읽기 쉽게 작성
- 복잡한 쿼리는 Repository에 private 상수로 분리
- 변수명은 축약보다 의미 우선 (`courseId`, `currentUserId`)
- 한글 주석은 "왜 필요한지" 중심으로 짧게 작성

JdbcTemplate 예시:

```java
private static final String FIND_BY_ID = """
    SELECT id, title, description, instructor_id
    FROM course
    WHERE id = ?
""";

public Optional<Course> findById(Long courseId) {
    List<Course> result = jdbcTemplate.query(FIND_BY_ID, courseRowMapper, courseId);
    return result.stream().findFirst();
}
```

---

## 6) 도메인 모델 (MVP)

## 6.1 User
- `id` (PK)
- `email` (unique)
- `password`
- `name`
- `role` (ADMIN, INSTRUCTOR, STUDENT)
- `createdAt`, `updatedAt`

## 6.2 Course
- `id` (PK)
- `title`
- `description`
- `instructorId` (FK: User)
- `status` (OPEN, CLOSED)
- `createdAt`, `updatedAt`

## 6.3 Enrollment
- `id` (PK)
- `studentId` (FK: User)
- `courseId` (FK: Course)
- `enrolledAt`
- 제약: `(studentId, courseId)` unique

## 6.4 Notice
- `id` (PK)
- `courseId` (FK: Course)
- `title`
- `content`
- `authorId` (FK: User)
- `createdAt`, `updatedAt`

---

## 7) 화면/요청 경로 설계 초안 (Thymeleaf)

## 7.1 Auth
- `GET /login` 로그인 페이지
- `POST /login` 로그인 처리
- `GET /signup` 회원가입 페이지
- `POST /signup` 회원가입 처리

## 7.2 User
- `GET /me` 내 정보 페이지

## 7.3 Course
- `GET /courses` 강의 목록
- `GET /courses/{courseId}` 강의 상세
- `GET /courses/new` 강의 생성 폼
- `POST /courses` 강의 생성 처리
- `GET /courses/{courseId}/edit` 강의 수정 폼
- `POST /courses/{courseId}/edit` 강의 수정 처리
- `POST /courses/{courseId}/delete` 강의 삭제 처리

## 7.4 Enrollment
- `POST /courses/{courseId}/enroll` 수강 신청
- `POST /courses/{courseId}/unenroll` 수강 취소
- `GET /enrollments/me` 내 수강 목록

## 7.5 Notice
- `GET /courses/{courseId}/notices` 공지 목록
- `GET /courses/{courseId}/notices/{noticeId}` 공지 상세
- `GET /courses/{courseId}/notices/new` 공지 등록 폼
- `POST /courses/{courseId}/notices` 공지 등록 처리

---

## 8) 로그인/인증 전략 (요구사항 반영)

### 8.1 기본 정책: SSO 우선
- 고객사 사용자는 기본적으로 **SSO만 허용**
- 우선 구현 표준: **OIDC (Authorization Code + PKCE)**
- SAML은 엔터프라이즈 요구 시 추가
- 대상 IdP: Microsoft Entra ID / Google Workspace / Okta

### 8.2 보조 정책: 로컬 로그인 최소화
- 로컬 로그인은 **Super Admin(내부 운영자)** 전용
- 방식: 이메일 + 비밀번호
- 고객사 사용자 로컬 로그인은 기본 차단
- 필요 시 테넌트 설정으로 예외 허용

### 8.3 온보딩 정책: 초대 링크 + 도메인 제한
- 관리자가 사용자 초대
- 초대 수락 시 해당 조직(tenant) 자동 소속
- 조직별 허용 도메인(예: `@company.com`) 검증
- 파일럿/초기 계약 시 계정 세팅 속도 개선

### 8.4 구현 메모
- 상세 설계 문서: `docs/auth-strategy.md`
- 추후 테이블 확장: tenants, tenant_domains, identity_providers, invitations, user_identities

---

## 9) 권한 정책 (RBAC)

- **ADMIN**: 전체 관리 가능
- **INSTRUCTOR**: 본인 강의의 생성/수정/삭제, 공지 등록
- **STUDENT**: 강의 조회, 수강 신청/취소, 공지 조회

### 권한 검증 예시
- 강의 수정/삭제 시: `course.instructorId == currentUser.id` 또는 ADMIN
- 공지 등록 시: 해당 강의 담당 강사 또는 ADMIN

---

## 9) 예외 처리 및 사용자 메시지 표준

## 9.1 처리 원칙
- 사용자에게는 이해하기 쉬운 메시지 표시
- 로그에는 원인(예외 클래스/쿼리 정보) 기록
- 권한 오류(403), 인증 오류(401), 데이터 없음(404) 분리

## 9.2 주요 예외 코드
- `INVALID_INPUT`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `USER_NOT_FOUND`
- `COURSE_NOT_FOUND`
- `DUPLICATE_ENROLLMENT`
- `NOTICE_NOT_FOUND`

---

## 10) 테스트 전략

## 10.1 테스트 범위
- Service 단위 테스트: 핵심 비즈니스 로직
- Controller 테스트(MockMvc): 인증/권한/요청 검증
- Repository 테스트: 기본 쿼리/제약 확인

## 10.2 우선 테스트 케이스
1. 로그인 성공/실패
2. 권한 없는 강의 수정 차단
3. 중복 수강 신청 차단
4. 존재하지 않는 강의/공지 조회 예외
5. 공지 등록 권한 검증

---

## 11) 주차별 상세 일정 (3주)

## Week 1 — 기반 구축 + 인증/인가

### 목표
- 실행 가능한 기본 프로젝트 골격 완성
- 인증/인가 체계 동작

### 작업
- [ ] 프로젝트 생성 및 의존성 구성
- [ ] 패키지 구조/공통 모듈 세팅
- [ ] MySQL 연결 및 JPA 매핑 검증 설정
- [ ] User/Role 엔티티 및 리포지토리 구현
- [ ] 회원가입/로그인 API 구현
- [ ] 세션 로그인/로그아웃 흐름 구현
- [ ] Security 설정(인가 룰)
- [ ] Auth 화면/요청 흐름 테스트 + 문서화

### 산출물
- 인증 가능한 서버, 기본 사용자 관리, 보안 설정

---

## Week 2 — 강의 관리 + 수강 관리

### 목표
- 강의 CRUD 및 수강 핵심 플로우 완성

### 작업
- [ ] Course 엔티티/DTO/리포지토리/서비스/컨트롤러 구현
- [ ] 강의 CRUD API 구현
- [ ] Enrollment 도메인 구현
- [ ] 수강 신청/취소/내역 조회 API 구현
- [ ] 중복 신청 방지 로직 및 예외 처리
- [ ] 권한 검증(강의 수정/삭제)
- [ ] 단위 테스트(강의/수강 핵심 로직)

### 산출물
- 강의/수강 핵심 기능 동작 + 주요 테스트

---

## Week 3 — 공지 + 안정화 + 문서화

### 목표
- 공지 기능 추가 후 MVP 안정화

### 작업
- [ ] Notice 도메인 및 API 구현
- [ ] 공지 등록 권한 검증
- [ ] GlobalExceptionHandler 정리
- [ ] 입력 검증 메시지/응답 포맷 통일
- [ ] 통합 테스트 및 버그 수정
- [ ] 화면 흐름 문서 정리 + README 보강
- [ ] ERD/시퀀스 흐름 간단 문서화

### 산출물
- 3주 MVP 완료본 (시연 가능한 수준)

---

## 12) 작업 우선순위 (백로그)

### P0 (필수)
- 인증/인가
- 강의 CRUD
- 수강 신청/취소
- 공지 등록/조회

### P1 (권장)
- 테스트 보강
- 예외/응답 표준화 강화
- 문서화 완성도 향상

### P2 (후속)
- 과제/퀴즈/파일/알림/통계

---

## 13) 개발 운영 규칙

- 브랜치: `main`, `develop`, `feature/*`
- 커밋 컨벤션: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- PR 규칙:
  - 목적/변경점/테스트 결과 필수
  - API 변경 시 문서 동시 업데이트
- 코드 스타일: 일관성 우선(포맷터/린터 적용)

---

## 14) 리스크와 대응 방안

1. **일정 부족**
   - 대응: P0 기능 외 신규 요구는 차주 이월
2. **권한 처리 복잡도 증가**
   - 대응: 도메인별 권한 체크 공통화
3. **예외 케이스 누락**
   - 대응: 실패 케이스 테스트 우선 작성
4. **요구사항 변경**
   - 대응: API 계약(스펙) 먼저 고정 후 구현

---

## 15) 최종 산출물 체크리스트

- [ ] 실행 가능한 Spring Boot 프로젝트
- [ ] 핵심 기능(Auth/Course/Enrollment/Notice)
- [ ] 기능/화면 명세 문서
- [ ] ERD
- [ ] 테스트 코드 및 결과
- [ ] 프로젝트 README(실행/구성/기능 설명)

---

## 16) 즉시 실행 액션 (오늘 시작용)

1. Spring Boot 프로젝트 생성 (`~/project/lms`)
2. 의존성 확정: Web/Thymeleaf/JdbcTemplate/JPA(검증용)/Security/Validation
3. DB 스키마 및 기본 엔티티(User, Role) 작성
4. Auth 화면/처리(signup/login)부터 구현
5. Security + 세션 기반 인증 적용
6. 브라우저 화면 흐름으로 1차 검증

---

## 17) 15일(3주) 일일 작업표 + 예상 소요시간

> 기준: 평일 15일, 1일 순수 개발시간 6~8시간 가정

### Week 1 — 기반 구축 + 인증/인가

#### Day 1 (환경/골격 세팅) — 총 7h
- 프로젝트 생성 및 의존성 구성 (Web/Thymeleaf/JdbcTemplate/JPA/Security/Validation) — **1.5h**
- 패키지 구조/공통 모듈(common) 생성 — **1h**
- application 설정(dev/prod 분리 초안 포함) — **1h**
- DB 연결(MySQL) 및 실행 확인 — **1.5h**
- 헬스체크 API + 기본 실행 검증 — **1h**
- README 초기 실행 방법 정리 — **1h**

#### Day 2 (사용자/권한 모델링) — 총 7h
- User/Role 엔티티 설계 및 구현 — **2h**
- UserRepository/기본 서비스 구현 — **1.5h**
- 비밀번호 인코더 및 기본 보안 설정 — **1h**
- 회원가입 API 구현 — **1.5h**
- 회원가입 유효성 검증(중복 이메일 등) — **1h**

#### Day 3 (로그인/세션 인증) — 총 8h
- 로그인 API 구현 — **2h**
- 세션 인증 헬퍼/유틸 정리 — **2h**
- 폼 로그인 설정 및 SecurityChain 연동 — **2h**
- 인증 실패/만료 예외 처리 — **1h**
- 브라우저 시나리오 점검 — **1h**

#### Day 4 (인가/RBAC 정리) — 총 7h
- 역할별 접근 정책 설계 및 반영 — **2h**
- 메서드/URL 단위 권한 제어 구현 — **2h**
- `/users/me` API 구현 — **1h**
- 공통 응답 포맷/에러 포맷 1차 정리 — **1h**
- 인증/인가 테스트 코드(핵심 케이스) — **1h**

#### Day 5 (1주차 안정화) — 총 6h
- Auth/User 코드 리팩터링 — **1.5h**
- 예외 코드 정리 및 메시지 표준화 — **1.5h**
- Auth 화면/요청 흐름 문서화 — **1h**
- 1주차 기능 통합 점검 및 버그 수정 — **2h**

---

### Week 2 — 강의/수강 핵심 기능

#### Day 6 (Course 기본 구현) — 총 7h
- Course 엔티티/DTO/Repository 구현 — **2.5h**
- 강의 생성 API 구현 — **1.5h**
- 강의 목록/상세 조회 API 구현 — **2h**
- 기본 검증(제목 길이/필수값) — **1h**

#### Day 7 (Course 수정/삭제 + 권한) — 총 7h
- 강의 수정 API 구현 — **2h**
- 강의 삭제 API 구현 — **1.5h**
- 강사 소유권 검증 로직 구현 — **2h**
- 예외 처리/응답 정리 — **1h**
- API 테스트 — **0.5h**

#### Day 8 (Enrollment 모델링) — 총 7h
- Enrollment 엔티티/Repository 구현 — **2h**
- `(studentId, courseId)` unique 제약 반영 — **1h**
- 수강 신청 API 구현 — **2h**
- 중복 신청/강의 상태 체크 로직 — **2h**

#### Day 9 (수강 취소/내역 조회) — 총 7h
- 수강 취소 API 구현 — **2h**
- 내 수강 목록 조회 API 구현 — **2h**
- 권한/예외 처리 정교화 — **1.5h**
- 통합 테스트 시나리오 작성 — **1.5h**

#### Day 10 (2주차 안정화) — 총 6h
- Course/Enrollment 리팩터링 — **1.5h**
- 서비스 단위 테스트 보강 — **2h**
- Course/Enrollment 화면/요청 문서화 — **1h**
- 회귀 테스트 + 버그 수정 — **1.5h**

---

### Week 3 — 공지 + 마감 품질

#### Day 11 (Notice 기본 구현) — 총 7h
- Notice 엔티티/DTO/Repository 구현 — **2h**
- 공지 등록 API 구현 — **2h**
- 강의별 공지 목록/상세 API 구현 — **2h**
- 입력값 검증 — **1h**

#### Day 12 (공지 권한/정책) — 총 7h
- 공지 등록 권한 검증(담당 강사/관리자) — **2h**
- Notice 예외 처리 코드 정리 — **1.5h**
- 공지 수정/삭제 필요 여부 검토 및 최소 반영(선택) — **2h**
- API 테스트 — **1.5h**

#### Day 13 (공통 품질 강화) — 총 7h
- GlobalExceptionHandler 정리 — **2h**
- 공통 응답 래퍼 최종 통일 — **1.5h**
- Validation 메시지 일관화 — **1h**
- 보안 룰 점검(CORS/권한 누락) — **1.5h**
- 로깅 포인트 정리 — **1h**

#### Day 14 (테스트/문서 마감) — 총 8h
- 핵심 통합 테스트 작성/보강 — **3h**
- 실패 시나리오 테스트(권한/중복/NotFound) — **2h**
- 기능/화면 문서 최종 정리 — **1h**
- ERD 및 화면/요청 흐름 문서화 — **1h**
- README 실행/검증 절차 보강 — **1h**

#### Day 15 (최종 점검/릴리스 준비) — 총 6h
- 전체 회귀 테스트 — **2h**
- 버그 수정 및 코드 정리 — **2h**
- 배포/실행 체크리스트 작성 — **1h**
- 최종 시연 시나리오 정리 — **1h**

---

## 18) 버퍼/리스크 대응 시간 계획

- 총 개발시간(예상): **약 105h**
- 권장 버퍼: **10~15h** (예상치의 약 10~15%)
- 버퍼 사용 우선순위:
  1. 인증/인가 버그
  2. 권한 누락 보완
  3. 테스트 실패/회귀 이슈
  4. 문서 미비 보완

---

## 19) 일일 완료 점검 템플릿 (Daily DoD)

매일 작업 종료 전 아래 항목 체크:

- [ ] 오늘 목표 기능 동작 확인
- [ ] 예외/실패 케이스 최소 1개 이상 점검
- [ ] 테스트 실행 및 결과 확인
- [ ] 문서/README 변경사항 반영
- [ ] 다음 날 작업 우선순위 3개 작성


---

필요하면 다음 단계로, 위 15일 작업표를 기준으로 **Jira/Trello 티켓 형식(담당자/우선순위/완료조건 포함)**으로 변환해줄 수 있다.

---

## 추가 문서

- 수행계획서(초안): `docs/수행계획서.md`
- 수행계획서(최종 제출용): `docs/최종_수행계획서.md`
myapp1
