# Postman JWT 토큰 자동 설정 가이드

Postman에서 JWT 토큰을 자동으로 저장하고 모든 API 요청에 자동으로 포함시키는 방법을 설명합니다.

---

## 🎯 목표

1. 로그인 API 호출 시 JWT 토큰을 자동으로 저장
2. 인증이 필요한 모든 API 요청에 토큰을 자동으로 포함
3. 수동으로 토큰을 복사/붙여넣기할 필요 없음

---

## 📝 단계별 설정

### 1단계: Environment 생성

1. Postman 우측 상단의 **Environment** 드롭다운 클릭
2. **+** 버튼 클릭하여 새 Environment 생성
3. Environment 이름 입력 (예: `TimeDeal Local`)
4. 다음 변수 추가:

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `base_url` | `http://localhost:8080` | `http://localhost:8080` |
| `jwt_token` | (비워둠) | (비워둠) |

5. **Save** 클릭
6. 생성한 Environment를 선택 (우측 상단 드롭다운)

---

### 2단계: 로그인 API에 Tests 스크립트 추가

1. **로그인 API** (`POST /api/auth/login`) 선택
2. **Tests** 탭 클릭
3. 다음 스크립트 입력:

```javascript
// 응답이 성공(200)인 경우
if (pm.response.code === 200) {
    // JSON 응답 파싱
    var jsonData = pm.response.json();
    
    // JWT 토큰 추출
    var token = jsonData.token;
    
    // Environment 변수에 토큰 저장
    pm.environment.set("jwt_token", token);
    
    // 콘솔에 확인 메시지 출력 (선택사항)
    console.log("JWT 토큰이 저장되었습니다:", token);
}
```

4. **Save** 클릭

**동작 방식:**
- 로그인 API 호출 성공 시
- 응답에서 `token` 값을 추출
- `jwt_token` Environment 변수에 자동 저장

---

### 3단계: 인증이 필요한 API에 Authorization 설정

각 인증이 필요한 API 요청에 대해:

1. API 요청 선택 (예: `POST /api/orders/users/{userId}`)
2. **Authorization** 탭 클릭
3. **Type** 드롭다운에서 **Bearer Token** 선택
4. **Token** 필드에 `{{jwt_token}}` 입력

**또는**

1. **Headers** 탭 클릭
2. **Key**: `Authorization`
3. **Value**: `Bearer {{jwt_token}}`

**동작 방식:**
- `{{jwt_token}}`은 Environment 변수에서 자동으로 값을 가져옴
- 로그인 후 저장된 토큰이 자동으로 사용됨

---

## 🚀 사용 방법

### 전체 플로우

1. **Environment 선택**
   - 우측 상단에서 `TimeDeal Local` 선택

2. **로그인**
   - `POST /api/auth/login` 요청 실행
   - Tests 스크립트가 자동으로 토큰 저장
   - 콘솔에서 "JWT 토큰이 저장되었습니다" 메시지 확인

3. **인증이 필요한 API 호출**
   - 다른 API 요청 실행
   - Authorization 헤더에 토큰이 자동으로 포함됨
   - 수동 입력 불필요!

---

## 📋 Collection 설정 (선택사항)

여러 API를 Collection으로 관리하는 경우:

### Collection Variables 설정

1. Collection 선택 → **Variables** 탭
2. 다음 변수 추가:

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `base_url` | `http://localhost:8080` | `http://localhost:8080` |
| `jwt_token` | (비워둠) | (비워둠) |

### Collection Pre-request Script

Collection 레벨에서 모든 요청에 공통 설정을 적용:

1. Collection 선택 → **Pre-request Script** 탭
2. 다음 스크립트 입력 (선택사항):

```javascript
// 모든 요청에 공통 헤더 설정
pm.request.headers.add({
    key: 'Content-Type',
    value: 'application/json'
});
```

---

## 🔧 고급 설정

### 1. 토큰 만료 체크 및 자동 재로그인

로그인 API의 **Tests** 탭에 다음 스크립트 추가:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    var token = jsonData.token;
    
    // 토큰 저장
    pm.environment.set("jwt_token", token);
    
    // 토큰 만료 시간 저장 (24시간 후)
    var expirationTime = new Date();
    expirationTime.setHours(expirationTime.getHours() + 24);
    pm.environment.set("token_expires_at", expirationTime.toISOString());
    
    console.log("JWT 토큰이 저장되었습니다. 만료 시간:", expirationTime);
}
```

### 2. 자동 토큰 갱신 (Pre-request Script)

인증이 필요한 API의 **Pre-request Script** 탭에 추가:

```javascript
// 토큰 만료 시간 체크
var expiresAt = pm.environment.get("token_expires_at");
if (expiresAt) {
    var now = new Date();
    var expiration = new Date(expiresAt);
    
    // 만료 5분 전이면 경고
    if (expiration - now < 5 * 60 * 1000) {
        console.warn("⚠️ 토큰이 곧 만료됩니다. 재로그인이 필요할 수 있습니다.");
    }
}
```

### 3. 로그아웃 시 토큰 삭제

로그아웃 API의 **Tests** 탭에 추가:

```javascript
if (pm.response.code === 200) {
    // 토큰 삭제
    pm.environment.unset("jwt_token");
    pm.environment.unset("token_expires_at");
    
    console.log("✅ 로그아웃 완료. 토큰이 삭제되었습니다.");
}
```

---

## 📝 실제 사용 예시

### 요청 URL에 변수 사용

**URL 설정:**
```
{{base_url}}/api/orders/users/{{user_id}}
```

**Environment Variables:**
- `base_url`: `http://localhost:8080`
- `user_id`: `1`

**실제 요청 URL:**
```
http://localhost:8080/api/orders/users/1
```

---

## 🐛 문제 해결

### 토큰이 저장되지 않는 경우

1. **Tests 탭 확인**
   - 로그인 API의 Tests 탭에 스크립트가 있는지 확인
   - 스크립트에 오타가 없는지 확인

2. **응답 형식 확인**
   - 로그인 API 응답이 `{"token": "...", "tokenType": "Bearer"}` 형식인지 확인
   - `jsonData.token`이 올바른 경로인지 확인

3. **Environment 선택 확인**
   - 우측 상단에서 올바른 Environment가 선택되어 있는지 확인

### 토큰이 자동으로 포함되지 않는 경우

1. **Authorization 설정 확인**
   - Type이 `Bearer Token`인지 확인
   - Token 필드에 `{{jwt_token}}`이 입력되어 있는지 확인
   - 중괄호 `{{}}`가 빠지지 않았는지 확인

2. **변수 이름 확인**
   - Environment 변수 이름이 `jwt_token`인지 확인
   - 대소문자 구분 주의

### 콘솔 확인 방법

1. Postman 하단의 **Console** 탭 클릭
2. 요청 실행 후 로그 확인
3. `console.log()` 메시지 확인

---

## 💡 팁

### 1. 여러 Environment 관리

- `TimeDeal Local`: 로컬 개발 환경
- `TimeDeal Dev`: 개발 서버
- `TimeDeal Prod`: 프로덕션 서버

각 Environment마다 `base_url`만 다르게 설정하면 됩니다.

### 2. Collection Export/Import

팀원들과 설정을 공유하려면:
1. Collection → **...** → **Export**
2. JSON 파일 저장
3. 다른 사람이 **Import**하여 사용

### 3. Environment Export/Import

Environment 설정도 공유 가능:
1. Environment → **...** → **Export**
2. JSON 파일 저장
3. 다른 사람이 **Import**하여 사용

**⚠️ 주의:** `jwt_token`은 개인별로 다르므로 공유하지 마세요!

---

## 📚 참고

- [Postman 공식 문서 - Variables](https://learning.postman.com/docs/sending-requests/variables/)
- [Postman 공식 문서 - Scripts](https://learning.postman.com/docs/writing-scripts/intro-to-scripts/)

---

**작성일**: 2026-01-26
