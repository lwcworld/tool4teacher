#!/bin/bash
# insert_png_to_hwpx.java 테스트 스크립트

set -e

echo "=========================================="
echo "insert_png_to_hwpx 테스트"
echo "=========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

# 출력 디렉토리 생성
mkdir -p output

# 테스트용 PNG 파일 확인
TEST_PNG="../../dataset/downloads/suneung/수학영역_문제지/page_0005_0013_picture_0001.png"
if [ ! -f "$TEST_PNG" ]; then
    echo "⚠️  테스트용 PNG 파일이 없습니다: $TEST_PNG"
    echo "   먼저 extract_pictures를 실행하여 PNG를 추출하세요."
    echo ""
    echo "=========================================="
    echo "⚠️  insert_png_to_hwpx 테스트 스킵"
    echo "=========================================="
    exit 0
fi

# 이전 출력 파일 삭제
echo "기존 출력 파일 삭제 중..."
rm -f output/inserted_image.hwpx

# Maven 컴파일
echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

# Java 실행
echo "----------------------------------------"
echo "insert_png_to_hwpx 실행 중..."
echo "----------------------------------------"
echo ""

mvn exec:java -Dexec.mainClass="insert_png_to_hwpx" -q

# 출력 파일 확인
echo ""
echo "----------------------------------------"
echo "출력 파일 검증 중..."
echo "----------------------------------------"

OUTPUT_FILE="output/inserted_image.hwpx"
if [ ! -f "$OUTPUT_FILE" ]; then
    echo "❌ 테스트 실패: HWPX 파일이 생성되지 않았습니다."
    exit 1
fi

FILE_SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE" 2>/dev/null)
echo "생성된 파일: $OUTPUT_FILE"
echo "파일 크기: ${FILE_SIZE} bytes"

if [ "$FILE_SIZE" -eq 0 ]; then
    echo "❌ 테스트 실패: 생성된 파일이 비어있습니다."
    exit 1
fi

echo ""
echo "파일 구조 확인:"
unzip -l "$OUTPUT_FILE" | head -20

# 성공 메시지
echo ""
echo "=========================================="
echo "✅ insert_png_to_hwpx 테스트 성공!"
echo "=========================================="
echo "출력 파일: $(pwd)/$OUTPUT_FILE"
echo "파일 크기: ${FILE_SIZE} bytes"
echo ""
