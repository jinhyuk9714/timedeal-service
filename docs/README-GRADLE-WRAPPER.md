# Gradle Wrapper 설정

`gradle-wrapper.jar` 파일이 없어서 빌드가 실행되지 않습니다.

## 해결 방법

### 방법 1: 브라우저에서 직접 다운로드 (권장)

다음 링크에서 `gradle-wrapper.jar` 파일을 다운로드하세요:

**다운로드 링크:**
- https://raw.githubusercontent.com/gradle/gradle/v8.14.0/gradle/wrapper/gradle-wrapper.jar
- 또는 https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.14/gradle-wrapper-8.14.jar

**설치 방법:**
1. 위 링크에서 파일을 다운로드
2. 파일명을 `gradle-wrapper.jar`로 변경
3. `gradle/wrapper/` 디렉토리에 저장

```bash
# 다운로드 후
mv ~/Downloads/gradle-wrapper.jar gradle/wrapper/
```

### 방법 2: 다른 Gradle 프로젝트에서 복사

다른 Gradle 프로젝트가 있다면:
```bash
cp /path/to/other-project/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
```

### 방법 3: Gradle 설치 후 wrapper 생성

Gradle이 설치되어 있다면:
```bash
gradle wrapper --gradle-version 8.14
```

## 확인

다운로드 후 다음 명령으로 확인:
```bash
./gradlew --version
```
