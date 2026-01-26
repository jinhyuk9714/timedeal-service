#!/bin/bash

# Gradle Wrapper JAR 다운로드 스크립트

echo "Gradle Wrapper JAR 다운로드 중..."

# 여러 소스에서 시도
URLS=(
    "https://raw.githubusercontent.com/gradle/gradle/v8.14.0/gradle/wrapper/gradle-wrapper.jar"
    "https://github.com/gradle/gradle/raw/v8.14.0/gradle/wrapper/gradle-wrapper.jar"
    "https://services.gradle.org/distributions/gradle-8.14-bin.zip"
)

mkdir -p gradle/wrapper

for url in "${URLS[@]}"; do
    echo "시도: $url"
    if curl -L -f -o gradle/wrapper/gradle-wrapper.jar "$url" 2>/dev/null; then
        echo "✓ 다운로드 성공!"
        exit 0
    fi
done

echo "✗ 자동 다운로드 실패"
echo ""
echo "수동 다운로드 방법:"
echo "1. 브라우저에서 다음 링크 열기:"
echo "   https://raw.githubusercontent.com/gradle/gradle/v8.14.0/gradle/wrapper/gradle-wrapper.jar"
echo ""
echo "2. 파일을 다운로드하여 다음 위치에 저장:"
echo "   gradle/wrapper/gradle-wrapper.jar"
echo ""
echo "3. 또는 다른 Gradle 프로젝트에서 복사:"
echo "   cp /path/to/other-project/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/"

exit 1
