#!/bin/bash
# extract_pictures.java 테스트 스크립트

set -e

echo "=========================================="
echo "extract_pictures 테스트"
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
    echo "✅ extract_pictures 테스트 스킵 (데이터셋 없음)"
    echo "=========================================="
    exit 0
fi

# 이전 출력 파일 삭제
echo "기존 Picture PNG 파일 삭제 중..."
find "$DEFAULT_PATH" -name "*_picture_*.png" -type f -delete 2>/dev/null || true

# Maven 컴파일
echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

# Java 실행 (단일 파일 테스트)
echo "----------------------------------------"
echo "extract_pictures 실행 중 (단일 파일)..."
echo "----------------------------------------"

java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" \
    extract_pictures "$DEFAULT_PATH/page_0005_0013.json"

# 출력 파일 확인
echo "----------------------------------------"
echo "출력 파일 검증 중..."
echo "----------------------------------------"

# 생성된 파일 개수 확인
PICTURE_COUNT=$(find "$DEFAULT_PATH" -name "page_0005_0013_picture_*.png" -type f | wc -l)
if [ "$PICTURE_COUNT" -eq 0 ]; then
    echo "❌ 테스트 실패: Picture PNG 파일이 생성되지 않았습니다."
    exit 1
fi

echo "생성된 Picture 파일: ${PICTURE_COUNT}개"
echo ""

# 생성된 파일 목록
echo "생성된 파일 목록:"
find "$DEFAULT_PATH" -name "page_0005_0013_picture_*.png" -type f | while read file; do
    FILE_SIZE=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
    echo "  - $(basename "$file"): ${FILE_SIZE} bytes"
done

# 성공 메시지
echo ""
echo "=========================================="
echo "✅ extract_pictures 테스트 성공!"
echo "=========================================="
echo "출력 디렉토리: $DEFAULT_PATH"
echo "생성된 Picture 파일: ${PICTURE_COUNT}개"
echo ""
