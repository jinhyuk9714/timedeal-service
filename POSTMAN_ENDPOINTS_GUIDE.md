# Postman API 엔드포인트 가이드

타임딜 서비스의 모든 API 엔드포인트를 Postman에서 테스트하기 위한 상세 가이드입니다.

---

## 📋 목차

1. [기본 설정](#기본-설정)
2. [인증 API](#인증-api)
3. [사용자 API](#사용자-api)
4. [상품 API](#상품-api)
5. [주문 API](#주문-api)
6. [전체 테스트 시나리오](#전체-테스트-시나리오)

---

## 기본 설정

### Base URL
```
http://localhost:8080
```

### 공통 헤더
모든 요청에 다음 헤더를 설정:
- **Content-Type**: `application/json`
- **Authorization**: `Bearer {{jwt_token}}` (인증이 필요한 API만)

### Environment Variables
Postman Environment에 다음 변수 설정:
- `base_url`: `http://localhost:8080`
- `jwt_token`: (로그인 후 자동 저장)

---

## 인증 API

### 1. 로그인

**요청**
- **Method**: `POST`
- **URL**: `{{base_url}}/api/auth/login`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```

**응답** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

**Postman Tests 스크립트** (자동 토큰 저장):
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.token);
    console.log("JWT 토큰이 저장되었습니다:", jsonData.token);
}
```

**에러 응답**:
- `400 Bad Request`: 이메일 형식 오류, 필수 필드 누락
- `401 Unauthorized`: 잘못된 이메일 또는 비밀번호

---

### 2. 로그아웃

**요청**
- **Method**: `POST`
- **URL**: `{{base_url}}/api/auth/logout`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```

**응답** (200 OK):
```
(응답 본문 없음)
```

**Postman Tests 스크립트** (토큰 삭제):
```javascript
if (pm.response.code === 200) {
    pm.environment.unset("jwt_token");
    console.log("✅ 로그아웃 완료. 토큰이 삭제되었습니다.");
}
```

**에러 응답**:
- `400 Bad Request`: 토큰 없음
- `401 Unauthorized`: 잘못된 토큰

---

## 사용자 API

### 1. 회원가입

**요청**
- **Method**: `POST`
- **URL**: `{{base_url}}/api/users`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "email": "user@example.com",
    "password": "password123",
    "name": "홍길동"
  }
  ```

**응답** (201 Created):
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**에러 응답**:
- `400 Bad Request`: 
  - 이메일 형식 오류
  - 필수 필드 누락
  - 중복 이메일

---

### 2. 사용자 조회

**요청**
- **Method**: `GET`
- **URL**: `{{base_url}}/api/users/{id}`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Path Variables**: 
  - `id`: 사용자 ID (예: `1`)

**응답** (200 OK):
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**에러 응답**:
- `404 Not Found`: 존재하지 않는 사용자

---

## 상품 API

### 1. 상품 등록

**요청**
- **Method**: `POST`
- **URL**: `{{base_url}}/api/items`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Body** (raw JSON):
  ```json
  {
    "name": "타임딜 상품 1",
    "price": 99000,
    "openTime": "2026-01-27T10:00:00",
    "stockQuantity": 100
  }
  ```

**응답** (201 Created):
```json
{
  "id": 1,
  "name": "타임딜 상품 1",
  "price": 99000,
  "openTime": "2026-01-27T10:00:00",
  "stockQuantity": 100,
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**에러 응답**:
- `400 Bad Request`: 
  - 필수 필드 누락
  - 가격이 0 이하
  - 재고 수량이 0 이하
- `401 Unauthorized`: 인증 토큰 없음

---

### 2. 전체 상품 목록 조회

**요청**
- **Method**: `GET`
- **URL**: `{{base_url}}/api/items`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```

**응답** (200 OK):
```json
[
  {
    "id": 1,
    "name": "타임딜 상품 1",
    "price": 99000,
    "openTime": "2026-01-27T10:00:00",
    "stockQuantity": 100,
    "createdAt": "2026-01-26T18:00:00",
    "updatedAt": "2026-01-26T18:00:00"
  },
  {
    "id": 2,
    "name": "타임딜 상품 2",
    "price": 199000,
    "openTime": "2026-01-27T11:00:00",
    "stockQuantity": 50,
    "createdAt": "2026-01-26T18:00:00",
    "updatedAt": "2026-01-26T18:00:00"
  }
]
```

**에러 응답**:
- `401 Unauthorized`: 인증 토큰 없음

---

### 3. 상품 조회 (단건)

**요청**
- **Method**: `GET`
- **URL**: `{{base_url}}/api/items/{id}`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Path Variables**: 
  - `id`: 상품 ID (예: `1`)

**응답** (200 OK):
```json
{
  "id": 1,
  "name": "타임딜 상품 1",
  "price": 99000,
  "openTime": "2026-01-27T10:00:00",
  "stockQuantity": 100,
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**에러 응답**:
- `404 Not Found`: 존재하지 않는 상품
- `401 Unauthorized`: 인증 토큰 없음

---

## 주문 API

### 1. 주문 생성

**요청**
- **Method**: `POST`
- **URL**: `{{base_url}}/api/orders/users/{userId}`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Path Variables**: 
  - `userId`: 주문하는 사용자 ID (예: `1`)
- **Body** (raw JSON):
  ```json
  {
    "itemId": 1,
    "quantity": 2
  }
  ```

**응답** (201 Created):
```json
{
  "id": 1,
  "userId": 1,
  "itemId": 1,
  "itemName": "타임딜 상품 1",
  "status": "ORDERED",
  "quantity": 2,
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**⚠️ 중요 사항**:
- 주문은 타임딜 오픈 시간(`openTime`) 이후에만 가능합니다
- 현재 시간이 `openTime`보다 이전이면 주문 불가
- 재고가 부족하면 주문 불가
- 요청한 `userId`와 인증된 사용자 ID가 일치해야 합니다

**에러 응답**:
- `400 Bad Request`: 
  - 타임딜 오픈 시간 이전
  - 재고 부족
  - 필수 필드 누락
  - 주문 수량이 0 이하
- `401 Unauthorized`: 인증 토큰 없음
- `403 Forbidden`: 다른 사용자 ID로 주문 시도
- `404 Not Found`: 존재하지 않는 상품 또는 사용자

---

### 2. 주문 조회 (단건)

**요청**
- **Method**: `GET`
- **URL**: `{{base_url}}/api/orders/{id}`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Path Variables**: 
  - `id`: 주문 ID (예: `1`)

**응답** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "itemId": 1,
  "itemName": "타임딜 상품 1",
  "status": "ORDERED",
  "quantity": 2,
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:00:00"
}
```

**에러 응답**:
- `404 Not Found`: 존재하지 않는 주문
- `401 Unauthorized`: 인증 토큰 없음

---

### 3. 사용자별 주문 목록 조회

**요청**
- **Method**: `GET`
- **URL**: `{{base_url}}/api/orders/users/{userId}`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Path Variables**: 
  - `userId`: 사용자 ID (예: `1`)

**응답** (200 OK):
```json
[
  {
    "id": 1,
    "userId": 1,
    "itemId": 1,
    "itemName": "타임딜 상품 1",
    "status": "ORDERED",
    "quantity": 2,
    "createdAt": "2026-01-26T18:00:00",
    "updatedAt": "2026-01-26T18:00:00"
  },
  {
    "id": 2,
    "userId": 1,
    "itemId": 2,
    "itemName": "타임딜 상품 2",
    "status": "ORDERED",
    "quantity": 1,
    "createdAt": "2026-01-26T18:05:00",
    "updatedAt": "2026-01-26T18:05:00"
  }
]
```

**⚠️ 중요**: 요청한 `userId`와 인증된 사용자 ID가 일치해야 합니다.

**에러 응답**:
- `403 Forbidden`: 다른 사용자 주문 목록 조회 시도
- `401 Unauthorized`: 인증 토큰 없음

---

### 4. 주문 취소

**요청**
- **Method**: `PATCH`
- **URL**: `{{base_url}}/api/orders/{id}/cancel`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```
- **Path Variables**: 
  - `id`: 취소할 주문 ID (예: `1`)

**응답** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "itemId": 1,
  "itemName": "타임딜 상품 1",
  "status": "CANCELED",
  "quantity": 2,
  "createdAt": "2026-01-26T18:00:00",
  "updatedAt": "2026-01-26T18:10:00"
}
```

**⚠️ 중요**: 
- 주문 취소 시 재고가 자동으로 복구됩니다
- 이미 취소된 주문은 다시 취소할 수 없습니다

**에러 응답**:
- `400 Bad Request`: 이미 취소된 주문
- `404 Not Found`: 존재하지 않는 주문
- `401 Unauthorized`: 인증 토큰 없음

---

## 전체 테스트 시나리오

### 시나리오 1: 정상 플로우

1. **회원가입**
   ```
   POST {{base_url}}/api/users
   Body: {
     "email": "user1@test.com",
     "password": "pass123",
     "name": "테스트유저"
   }
   → 사용자 ID: 1 저장
   ```

2. **로그인**
   ```
   POST {{base_url}}/api/auth/login
   Body: {
     "email": "user1@test.com",
     "password": "pass123"
   }
   → JWT 토큰 자동 저장 (Tests 스크립트)
   ```

3. **상품 등록** (오픈 시간을 현재 시간 이후로 설정)
   ```
   POST {{base_url}}/api/items
   Headers: Authorization: Bearer {{jwt_token}}
   Body: {
     "name": "타임딜 상품",
     "price": 99000,
     "openTime": "2026-01-27T10:00:00",
     "stockQuantity": 100
   }
   → 상품 ID: 1 저장
   ```

4. **상품 조회** (재고 확인)
   ```
   GET {{base_url}}/api/items/1
   Headers: Authorization: Bearer {{jwt_token}}
   → stockQuantity: 100 확인
   ```

5. **주문 생성** (오픈 시간이 지난 후)
   ```
   POST {{base_url}}/api/orders/users/1
   Headers: Authorization: Bearer {{jwt_token}}
   Body: {
     "itemId": 1,
     "quantity": 2
   }
   → 주문 ID: 1 저장
   ```

6. **상품 재고 확인** (주문 후 재고 차감 확인)
   ```
   GET {{base_url}}/api/items/1
   Headers: Authorization: Bearer {{jwt_token}}
   → stockQuantity: 98 확인 (100 - 2)
   ```

7. **주문 조회**
   ```
   GET {{base_url}}/api/orders/1
   Headers: Authorization: Bearer {{jwt_token}}
   ```

8. **주문 목록 조회**
   ```
   GET {{base_url}}/api/orders/users/1
   Headers: Authorization: Bearer {{jwt_token}}
   ```

9. **주문 취소**
   ```
   PATCH {{base_url}}/api/orders/1/cancel
   Headers: Authorization: Bearer {{jwt_token}}
   ```

10. **상품 재고 확인** (취소 후 재고 복구 확인)
    ```
    GET {{base_url}}/api/items/1
    Headers: Authorization: Bearer {{jwt_token}}
    → stockQuantity: 100 확인 (98 + 2)
    ```

11. **로그아웃**
    ```
    POST {{base_url}}/api/auth/logout
    Headers: Authorization: Bearer {{jwt_token}}
    → 토큰 삭제 (Tests 스크립트)
    ```

---

## 📝 Postman Collection 설정

### Collection Variables

Collection 레벨에서 변수 설정:
- `base_url`: `http://localhost:8080`

### Collection Pre-request Script

모든 요청에 공통 헤더 자동 설정:
```javascript
// Content-Type 헤더 자동 설정
pm.request.headers.add({
    key: 'Content-Type',
    value: 'application/json'
});
```

### Authorization 설정

인증이 필요한 API의 **Authorization** 탭:
- **Type**: `Bearer Token`
- **Token**: `{{jwt_token}}`

---

## 🔍 에러 코드 참고

| 상태 코드 | 의미 | 예시 |
|----------|------|------|
| 200 OK | 요청 성공 | 조회 성공 |
| 201 Created | 리소스 생성 성공 | 회원가입, 상품 등록, 주문 생성 |
| 400 Bad Request | 잘못된 요청 | 유효성 검증 실패, 비즈니스 규칙 위반 |
| 401 Unauthorized | 인증 실패 | 토큰 없음, 잘못된 토큰, 로그인 실패 |
| 403 Forbidden | 권한 없음 | 다른 사용자 리소스 접근 |
| 404 Not Found | 리소스를 찾을 수 없음 | 존재하지 않는 ID |

---

## 💡 유용한 팁

### 1. 타임딜 오픈 시간 설정

상품 등록 시 `openTime`을 현재 시간 이후로 설정:
```json
{
  "openTime": "2026-01-27T10:00:00"  // 현재 시간 이후
}
```

### 2. 재고 확인

주문 전후로 상품 조회하여 재고 변화 확인:
- 주문 전: `stockQuantity: 100`
- 주문 후: `stockQuantity: 98` (2개 주문 시)
- 취소 후: `stockQuantity: 100` (재고 복구)

### 3. 동시성 테스트

여러 사용자가 동시에 주문하는 시나리오 테스트:
- 같은 상품에 대해 여러 주문 동시 생성
- 재고가 정확하게 차감되는지 확인

---

## 📚 관련 문서

- [JWT 인증 가이드](./JWT_GUIDE.md)
- [Postman 설정 가이드](./POSTMAN_SETUP_GUIDE.md)
- [프로젝트 구조](./PROJECT_STRUCTURE.md)

---

**작성일**: 2026-01-26
