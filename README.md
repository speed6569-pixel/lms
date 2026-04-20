# LMS

## 프로젝트 소개 (한 줄 요약)
Spring Boot + Thymeleaf 기반으로 학습자, 강사, 관리자의 학습 운영(수강/결제/학습진도/게시판/관리자 운영)을 처리하는 LMS 웹 서비스입니다.

## 기술 스택
- Language: Java 17
- Framework: Spring Boot 4.0.3
- View: Thymeleaf
- Security: Spring Security (세션 기반 인증)
- Data Access: Spring JDBC, Spring Data JPA
- DB: MySQL 8.x
- Build: Gradle
- Validation: spring-boot-starter-validation
- Mail: spring-boot-starter-mail (비밀번호 재설정/인증 코드 메일)
- Test: Spring Boot Test (JUnit Platform)

## 실행 방법
1) 프로젝트 이동
- WSL/Linux 기준: `cd /home/hkit/.openclaw/workspace/lms-master`

2) 환경값 준비
- 민감정보(DB/메일 계정)는 코드에 하드코딩하지 않고 외부 설정으로 주입합니다.
- 권장 방법(로컬): 프로젝트 루트에 `config/local-secrets.yaml` 생성 후 값 입력
  - 예시 키: `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`, `APP_MAIL_HOST`, `APP_MAIL_PORT`, `APP_MAIL_USERNAME`, `APP_MAIL_PASSWORD`, `APP_MAIL_FROM`
- 대안: 환경변수 사용
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- 기본 설정 파일: `src/main/resources/application.yaml`
- 로컬/운영 프로필 파일: `application-local.yaml`, `application-prod.yaml`

3) DB 준비
- MySQL에 `lms_db` 생성
- `db/schema.sql`은 현재 코드 기준 최신 스키마가 아니므로 사용하지 않는 것을 권장
- 아래 마이그레이션을 파일명 순서대로 실행
  - `db/migrations/V20260227_01_core_lms_schema.sql`
  - `db/migrations/V20260227_02_seed_sample_data.sql`
  - `db/migrations/V20260227_03_settings_profile_and_login_history.sql`
  - 이후 `db/migrations` 내 `V2026...` 파일 전체를 번호 순서대로 실행
- 개발 중 seed 데이터 기준 계정
  - 관리자: `seed_admin`
  - 사용자: `seed_user`

4) WSL + Windows MySQL 사용 시 주의사항
- WSL에서 앱을 실행하고 Windows MySQL에 접속하는 경우, `localhost` 대신 Windows host IP를 사용해야 할 수 있음
- 예시: `jdbc:mysql://172.27.192.1:3306/lms_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- MySQL 사용자에 WSL 대역 또는 `%` host 권한이 필요할 수 있음
  - 예: `CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '비밀번호';`
  - 예: `GRANT ALL PRIVILEGES ON lms_db.* TO 'root'@'%';`
  - 예: `FLUSH PRIVILEGES;`

5) 애플리케이션 실행 전 점검 (중요)
- 8080 포트 사용 중인지 확인
  - macOS/Linux: `lsof -i :8080 -P -n`
- 기존 LMS 프로세스가 이미 떠 있으면 종료 후 실행
  - 예: `pkill -f com.example.lms.LmsApplication`

6) 애플리케이션 실행
- WSL/Linux 예시:
  - `DB_USERNAME=root DB_PASSWORD='Mysql1234!' ./gradlew bootRun`
- Windows 예시:
  - `gradlew.bat bootRun`

7) 접속
- 기본 URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 주요 기능
- 인증/계정
  - 로그인, 회원가입, 아이디 찾기, 비밀번호 재설정
- 학습자 기능
  - 강의 목록/상세 확인
  - 수강 신청/취소, 내 수강 내역 조회
  - 수강신청 필터(과목코드/직군/직급/키워드/요일 범위 선택)
  - 결제 내역 조회 및 환불 요청
  - 내 정보/설정(프로필, 비밀번호, 계정 탈퇴)
  - 학습 화면 진입 및 진도 저장
- 학습 콘텐츠
  - 강의별 레슨 관리, 학습 진도율 조회
  - 동영상 파일 제공(`/media/videos/{filename}`)
  - 학습 질의 API(`/api/learn/{courseId}/chat/query`)
- 게시판/고객센터
  - 공지/FAQ 조회
  - 고객센터 문의 등록/조회
- 관리자 기능
  - 대시보드
  - 사용자 관리(권한/활성 여부/포인트 지급)
  - 강의/세션/수강 승인 관리
  - 강의 통합 수정(상태/코드/과목명/교수/직군/직급/가격/정원/요일/시간)
  - 요일 선택(개별 + 범위 선택: 예: 월~금)
  - 결제 및 환불 승인/반려
  - 게시글/고객센터 문의 관리(사용자 문의와 단일 테이블 연동)

## 디렉토리 구조
```text
/home/ubuntu/project/lms
├── src/main/java/com/example/lms
│   ├── auth/        # 인증/회원
│   ├── admin/       # 관리자 기능
│   ├── enrollment/  # 수강/포인트/환불
│   ├── learn/       # 학습/레슨/진도/미디어
│   ├── posts/       # 공지/FAQ
│   ├── support/     # 고객센터
│   ├── settings/    # 사용자 설정
│   └── controller/, web/, config/
├── src/main/resources
│   ├── templates/   # Thymeleaf 화면
│   ├── static/      # CSS/JS 정적 파일
│   └── application.yaml
├── db
│   ├── migrations/  # 버전별 SQL 마이그레이션
│   └── schema.sql
├── docs             # 인증전략/수행계획 문서
├── uploads/videos   # 업로드 영상 파일 저장 경로
└── build.gradle
```

## API 또는 화면 사용 방법
### 1) 주요 화면 경로
- 홈: `GET /`, `GET /homepage`
- 로그인: `GET /login`
- 회원가입: `GET /signup`
- 강의 목록: `GET /courses`
- 내 페이지: `GET /mypage` (또는 `GET /enrollments/me`)
- 학습 화면: `GET /learn/{courseId}`
- 고객센터: `GET /customer-center`
- 관리자 대시보드: `GET /admin/dashboard`

### 2) 주요 사용자 API
- 강의 카드 조회: `GET /api/courses/{courseId}/card`
- 강의 결제: `POST /api/courses/{courseId}/pay`
- 강의 신청: `POST /api/courses/{courseId}/enroll`
- 내 대시보드: `GET /api/me/dashboard`
- 내 시간표: `GET /api/me/timetable`
- 내 수강목록: `GET /api/me/enrollments`
- 수강 취소: `POST /api/me/enrollments/{id}/cancel`
- 학습 진도 저장: `POST /api/learn/{lessonId}/progress`
- 강의 진도 조회: `GET /api/courses/{courseId}/progress`
- 설정 조회: `GET /api/settings/me`
- 프로필 수정: `PUT /api/settings/profile`

### 3) 주요 관리자 API
- 강의 목록/등록: `GET/POST /admin/api/courses`
- 강의 수정/삭제: `PUT/DELETE /admin/api/courses/{id}`
- 강의 상태 변경: `PATCH /admin/api/courses/{id}/status`
- 직군/직급 메타 조회: `GET /admin/api/job-meta`
- 수강 승인/반려: `POST /admin/api/enrollments/{id}/approve`, `POST /admin/api/enrollments/{id}/reject`
- 사용자 권한 변경: `PATCH /admin/api/users/{id}/role`
- 사용자 활성 변경: `PATCH /admin/api/users/{id}/enabled`
- 관리자 통계: `GET /admin/api/stats`

### 4) 사용자 메타 API
- 직군별 직급 조회: `GET /api/meta/job-levels`

### 5) 고객센터 연동 흐름
- 사용자 문의 작성: `POST /customer-center` → `support_posts` 저장
- 관리자 문의 목록: `GET /admin/support` (최신순)
- 관리자 답변 등록: `POST /admin/support/{id}/answer`
- 사용자 상세 반영: `GET /customer-center/{id}`에서 답변/상태 즉시 확인

### 6) 테스트 실행
- `./gradlew test`

## 참고 문서
- 인증 전략: `docs/auth-strategy.md`
- 수행 계획: `docs/수행계획서.md`, `docs/최종_수행계획서.md`
