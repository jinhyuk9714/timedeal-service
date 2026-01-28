# 문서 목차

이 디렉터리는 프로젝트 가이드·성능 테스트·운영 문서를 모아 둔 곳입니다.  
루트에는 **README.md**만 두고, 나머지 문서는 여기로 모았습니다.

---

## docs 내 문서

### 가이드 (docs/guides/)

| 문서 | 설명 |
|------|------|
| [guides/JWT_GUIDE.md](./guides/JWT_GUIDE.md) | JWT 인증 흐름, 로그인/로그아웃 사용법 |
| [guides/PAGING_GUIDE.md](./guides/PAGING_GUIDE.md) | 페이징 개념, Pageable/Page 사용법, API 예시 |
| [guides/PESSIMISTIC_LOCK_GUIDE.md](./guides/PESSIMISTIC_LOCK_GUIDE.md) | 비관적 락 개념·구현·동작 원리·SQL |
| [guides/POSTMAN_SETUP_GUIDE.md](./guides/POSTMAN_SETUP_GUIDE.md) | Postman JWT 자동 저장 등 설정 |
| [guides/POSTMAN_ENDPOINTS_GUIDE.md](./guides/POSTMAN_ENDPOINTS_GUIDE.md) | 엔드포인트별 요청/응답 예시 |

### 구조·배포·락

| 문서 | 설명 |
|------|------|
| [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) | 프로젝트 구조, 레이어, API, 테스트 가이드 |
| [deployment-monitoring.md](./deployment-monitoring.md) | 배포·모니터링 가이드 (실행 방법, 헬스·메트릭·로그) |
| [lock-strategy-comparison.md](./lock-strategy-comparison.md) | 비관적 vs 낙관적 락 비교 (한 페이지 요약) |

### 성능 테스트 (docs/perf/)

| 문서 | 설명 |
|------|------|
| [perf/PERF_TEST_PLAN.md](./perf/PERF_TEST_PLAN.md) | 성능 테스트 계획·SLO·시나리오 |
| [perf/PERF_RESULT.md](./perf/PERF_RESULT.md) | 성능 테스트 실측 결과 리포트 |

### 기타

| 문서 | 설명 |
|------|------|
| [README-GRADLE-WRAPPER.md](./README-GRADLE-WRAPPER.md) | Gradle Wrapper 다운로드·설명 |

---

## 레포 루트

| 문서 | 설명 |
|------|------|
| [README.md](../README.md) | 프로젝트 요약, 기술 스택, 실행 방법, 성능 테스트 요약, 관련 문서 링크 |
