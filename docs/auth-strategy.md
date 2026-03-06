# LMS 로그인/인증 전략 (멀티 테넌트 B2B 기준)

## 1. 목표
- 고객사 사용자는 고객사 IdP(Entra ID/Google Workspace/Okta 등)로 로그인한다.
- 내부 운영자(Super Admin)만 로컬 로그인(이메일+비밀번호)을 사용한다.
- 고객사 온보딩 속도를 위해 "초대 링크 + 도메인 제한"을 기본 제공한다.

---

## 2. 인증 우선순위

### 2.1 기본: SSO 우선 (OIDC/SAML)
- **기본 표준**: OIDC (Authorization Code + PKCE)
- **추가 표준**: SAML (엔터프라이즈 고객 요구 시)

지원 대상 예시:
- Microsoft Entra ID
- Google Workspace
- Okta

선택 이유:
- 계정 생성/비활성화(퇴사자 처리) 고객사 IT 시스템 연동
- 비밀번호 정책/MFA 정책을 고객사가 직접 관리
- LMS 운영팀의 인증 운영 부담 감소

권장 정책:
- 신규 테넌트 생성 시 `sso_required=true`
- 고객사 사용자의 로컬 비밀번호 로그인은 기본 차단

---

### 2.2 보조: 로컬 로그인 최소 허용
- **허용 대상**: Super Admin (내부 운영자)
- **방식**: 이메일 + 비밀번호
- **고객사 사용자**: 원칙적으로 SSO만 허용
- **예외**: 테넌트 설정으로 임시 허용 가능 (`allow_local_login=true`)

보안 권장:
- 로컬 로그인 계정에 MFA 강제
- 관리자 계정 IP 제한/접속 로그 모니터링

---

### 2.3 온보딩: 초대 링크 + 도메인 제한

흐름:
1. 관리자가 사용자 초대 (이메일 입력)
2. 시스템이 만료 시간 포함 초대 링크 발급
3. 사용자 링크 접속 후 SSO 로그인
4. 로그인된 이메일 도메인 검증
5. 유효 시 해당 조직(tenant)으로 자동 소속

조직 정책:
- 테넌트별 허용 도메인 설정 (예: `company.com`)
- 도메인 불일치 시 가입 차단 + 관리자 승인 대기 처리(옵션)

효과:
- 파일럿/초기 계약 시 계정 세팅 속도 향상
- 잘못된 조직 소속/권한 부여 리스크 감소

---

## 3. 권한/데이터 모델 제안

## 3.1 핵심 테이블(추가)
- `tenants` : 조직 정보
- `tenant_domains` : 허용 도메인 목록
- `identity_providers` : 테넌트별 IdP 설정(OIDC/SAML)
- `tenant_users` : 사용자-조직 매핑
- `invitations` : 초대 토큰/만료/상태
- `user_identities` : 사용자 외부 식별자(sub, issuer, provider)

## 3.2 users 테이블 권장 컬럼
- `login_type` (`LOCAL`, `SSO`)
- `is_super_admin` (boolean)
- `status` (`ACTIVE`, `SUSPENDED`)

---

## 4. 로그인 플로우

### A. 고객사 사용자 (SSO)
1. `/login` → "조직으로 로그인" 선택
2. 이메일/도메인 기반 테넌트 식별 또는 조직 코드 입력
3. IdP로 리다이렉트(OIDC 우선)
4. 콜백에서 토큰 검증 후 사용자 동기화(provisioning)
5. `tenant_users` 매핑 후 세션 발급

### B. Super Admin (로컬)
1. `/admin/login` 접속
2. 이메일+비밀번호 인증
3. MFA(추가 예정) 검증 후 관리자 세션 발급

---

## 5. Spring Boot 구현 가이드

## 5.1 1차 구현 범위 (추천)
- OIDC 로그인 (Spring Security OAuth2 Client)
- 로컬 로그인 (admin 경로 전용)
- 초대 링크 발급/검증 API
- 테넌트 도메인 검증

## 5.2 설정 분리
- `security.admin-local-login.enabled=true`
- `security.tenant.default-sso-required=true`
- `security.tenant.allow-local-login-exception=false`

## 5.3 라우팅 제안
- `/login` : 일반 사용자 로그인 진입(SSO 안내)
- `/sso/{tenantKey}` : 테넌트별 SSO 시작
- `/login/oauth2/code/{registrationId}` : OIDC callback
- `/admin/login` : Super Admin 로컬 로그인
- `/invitations/accept?token=...` : 초대 수락

---

## 6. 운영 정책
- 고객사 사용자 비밀번호 저장 금지(원칙)
- SSO 실패 시 장애 원인(도메인/권한/IdP 설정) 로그 분리
- 초대 링크 만료 기본 72시간
- 감사 로그: 로그인 성공/실패, 초대 수락, 도메인 불일치 이벤트 저장

---

## 7. 단계별 로드맵

### Phase 1 (즉시)
- 현재 로그인 화면을 분기: "조직 로그인" / "운영자 로그인"
- Super Admin 로컬 로그인만 유지

### Phase 2
- OIDC 1개 공급자(Entra ID 또는 Google) 연동
- tenant/domain/invitation 테이블 추가

### Phase 3
- 다중 IdP(Okta 포함), JIT 프로비저닝 고도화
- SAML 지원(요구 고객사 대상)
