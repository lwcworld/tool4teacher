#!/bin/bash
# json_to_hwpx.java 테스트 스크립트

set -e

echo "=========================================="
echo "json_to_hwpx 테스트"
echo "=========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

# 기본 경로 확인
DEFAULT_PATH="../../dataset/downloads/suneung/수학영역_문제지"
if [ ! -d "$DEFAULT_PATH" ]; then
    echo "⚠️  기본 데이터셋 경로가 존재하지 않습니다: $DEFAULT_PATH"
    echo "   테스트를 스킵합니다. (정상)"
    echo ""
    echo "=========================================="
    echo "✅ json_to_hwpx 테스트 스킵 (데이터셋 없음)"
    echo "=========================================="
    exit 0
fi

# 이전 출력 파일 삭제
echo "기존 HWPX 파일 삭제 중..."
find "$DEFAULT_PATH" -name "page_*.hwpx" -type f -delete 2>/dev/null || true

# Maven 컴파일
echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

# Java 실행 (기본 경로 사용: ../../dataset/downloads/suneung/수학영역_문제지)
echo "----------------------------------------"
echo "json_to_hwpx 실행 중..."
echo "----------------------------------------"

# Java 직접 실행 (출력은 입력 디렉토리와 동일)
java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" json_to_hwpx "$DEFAULT_PATH"

# 출력 파일 확인
echo "----------------------------------------"
echo "출력 파일 검증 중..."
echo "----------------------------------------"

# 생성된 파일 개수 확인
HWPX_COUNT=$(find "$DEFAULT_PATH" -name "page_*.hwpx" -type f | wc -l)
if [ "$HWPX_COUNT" -eq 0 ]; then
    echo "❌ 테스트 실패: HWPX 파일이 생성되지 않았습니다."
    exit 1
fi

echo "생성된 HWPX 파일: ${HWPX_COUNT}개"

# 첫 번째 파일 구조 확인
FIRST_FILE=$(find "$DEFAULT_PATH" -name "page_*.hwpx" -type f | head -1)
if [ -n "$FIRST_FILE" ]; then
    FILE_SIZE=$(stat -f%z "$FIRST_FILE" 2>/dev/null || stat -c%s "$FIRST_FILE" 2>/dev/null)
    echo ""
    echo "첫 번째 파일: $(basename "$FIRST_FILE")"
    echo "파일 크기: ${FILE_SIZE} bytes"

    if [ "$FILE_SIZE" -eq 0 ]; then
        echo "❌ 테스트 실패: 생성된 파일이 비어있습니다."
        exit 1
    fi

    echo ""
    echo "파일 구조 확인:"
    unzip -l "$FIRST_FILE" | head -15
fi

# 성공 메시지
echo ""
echo "=========================================="
echo "✅ json_to_hwpx 테스트 성공!"
echo "=========================================="
echo "출력 디렉토리: $DEFAULT_PATH"
echo "생성된 파일: ${HWPX_COUNT}개"
echo ""
