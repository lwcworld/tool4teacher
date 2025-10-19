#!/bin/bash
# hello_world.java 테스트 스크립트

set -e

echo "=========================================="
echo "hello_world.java 테스트"
echo "=========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

# 이전 출력 파일 삭제
if [ -f "output/hello_world.hwpx" ]; then
    echo "기존 출력 파일 삭제 중..."
    rm output/hello_world.hwpx
fi

# Maven 컴파일
echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

# Java 직접 실행
echo "----------------------------------------"
echo "hello_world 실행 중..."
echo "----------------------------------------"
java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" hello_world

# 출력 파일 확인
echo "----------------------------------------"
echo "출력 파일 검증 중..."
echo "----------------------------------------"

if [ ! -f "output/hello_world.hwpx" ]; then
    echo "❌ 테스트 실패: output/hello_world.hwpx 파일이 생성되지 않았습니다."
    exit 1
fi

# 파일 크기 확인
FILE_SIZE=$(stat -f%z "output/hello_world.hwpx" 2>/dev/null || stat -c%s "output/hello_world.hwpx" 2>/dev/null)
if [ "$FILE_SIZE" -eq 0 ]; then
    echo "❌ 테스트 실패: 생성된 파일이 비어있습니다."
    exit 1
fi

# ZIP 파일 구조 확인
echo "파일 구조 확인:"
unzip -l "output/hello_world.hwpx" | head -15

# 성공 메시지
echo ""
echo "=========================================="
echo "✅ hello_world 테스트 성공!"
echo "=========================================="
echo "출력 파일: output/hello_world.hwpx"
echo "파일 크기: ${FILE_SIZE} bytes"
echo ""
