# JWT 인증 가이드

## 📋 개요

이 프로젝트는 JWT (JSON Web Token) 기반 인증 시스템을 사용합니다.

## 🔐 인증 흐름

```
1. 사용자 회원가입 (POST /api/users)
   ↓
2. 로그인 (POST /api/auth/login)
   → JWT 토큰 발급
   ↓
3. API 요청 시 헤더에 토큰 포함
   Authorization: Bearer {token}
   ↓
4. JwtAuthenticationFilter가 토큰 검증
   ↓
5. Controller에서 @AuthenticationPrincipal로 사용자 ID 접근
```

## 🚀 사용 방법

### 1. 회원가입

```bash
POST /api/users
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**응답:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "createdAt": "2026-01-26T10:00:00"
}
```

### 2. 로그인

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

### 3. 인증이 필요한 API 호출

모든 주문 관련 API는 인증이 필요합니다.

```bash
POST /api/orders/users/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "itemId": 1,
  "quantity": 2
}
```

## 📁 주요 파일 구조

```
src/main/java/com/timedeal/api/
├── infrastructure/security/
│   ├── JwtTokenProvider.java      # JWT 토큰 생성/검증
│   ├── JwtAuthenticationFilter.java # JWT 필터 (요청 검증)
│   └── SecurityConfig.java         # Spring Security 설정
├── service/
│   └── AuthService.java            # 로그인 비즈니스 로직
└── controller/
    └── AuthController.java         # 로그인 API
```

## ⚙️ 설정

### application.yml

```yaml
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
  expiration: 86400000 # 24시간 (밀리초)
```

**⚠️ 주의:** 프로덕션 환경에서는 반드시 `jwt.secret`을 안전한 값으로 변경하세요!

## 🔒 보안 설정

### 공개 엔드포인트 (인증 불필요)
- `POST /api/auth/login` - 로그인
- `POST /api/users` - 회원가입
- `GET /api/users/{id}` - 사용자 조회 (선택사항)

### 인증 필요 엔드포인트
- `POST /api/orders/users/{userId}` - 주문 생성
- `GET /api/orders/users/{userId}` - 사용자별 주문 목록
- `GET /api/items` - 상품 목록 (향후 인증 필요로 변경 가능)

## 💡 주요 개념

### 1. JWT 토큰 구조
```
Header.Payload.Signature
```

- **Header**: 토큰 타입과 알고리즘
- **Payload**: 사용자 ID, 발급 시간, 만료 시간
- **Signature**: 비밀키로 서명하여 위조 방지

### 2. PasswordEncoder
- BCrypt 알고리즘 사용
- 같은 비밀번호라도 매번 다른 해시값 생성
- `passwordEncoder.encode()`: 암호화
- `passwordEncoder.matches()`: 검증

### 3. @AuthenticationPrincipal
```java
@PostMapping("/api/orders/users/{userId}")
public ResponseEntity<OrderResponse> createOrder(
        @PathVariable Long userId,
        @AuthenticationPrincipal Long authenticatedUserId) {
    // authenticatedUserId: JWT에서 추출한 사용자 ID
}
```

## 🧪 테스트

### cURL 예시

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","name":"테스트"}'

# 2. 로그인
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}' \
  | jq -r '.token')

# 3. 인증이 필요한 API 호출
curl -X POST http://localhost:8080/api/orders/users/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"quantity":2}'
```

## 📚 학습 포인트

1. **Spring Security**: 보안 프레임워크
2. **JWT**: 토큰 기반 인증
3. **Filter**: 요청 전처리
4. **PasswordEncoder**: 비밀번호 암호화
5. **@AuthenticationPrincipal**: 인증 정보 주입

---

**작성일**: 2026-01-26
