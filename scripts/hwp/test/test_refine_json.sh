#!/bin/bash
# dotsocr JSON -> text/math refinement 테스트 스크립트

set -e

echo "=========================================="
echo "refine_json 테스트"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

DATASET_DIR="../../dataset/downloads/suneung/수학영역_문제지"
if [ ! -d "$DATASET_DIR" ]; then
    echo "⚠️  기본 데이터셋 경로가 존재하지 않습니다: $DATASET_DIR"
    echo "   테스트를 스킵합니다. (정상)"
    echo ""
    echo "=========================================="
    echo "✅ refine_json 테스트 스킵 (데이터셋 없음)"
    echo "=========================================="
    exit 0
fi

SAMPLE_FILE="$(find "$DATASET_DIR" -maxdepth 1 -name 'page_*.json' | sort | head -n 1)"
if [ -z "$SAMPLE_FILE" ]; then
    echo "❌ 테스트 실패: 샘플 JSON 파일을 찾을 수 없습니다."
    exit 1
fi

echo "샘플 파일: $(basename "$SAMPLE_FILE")"

echo "이전에 생성된 _refine 파일 정리 중..."
find "$DATASET_DIR" -maxdepth 1 -name 'page_*_refine.json' -delete 2>/dev/null || true

echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

CLASSPATH="target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)"

echo "----------------------------------------"
echo "단일 파일 리파인 실행..."
echo "----------------------------------------"
java -cp "$CLASSPATH" refine_json "$SAMPLE_FILE"

SAMPLE_BASENAME="$(basename "$SAMPLE_FILE" .json)"
SAMPLE_OUTPUT="$(dirname "$SAMPLE_FILE")/${SAMPLE_BASENAME}_refine.json"

if [ ! -f "$SAMPLE_OUTPUT" ]; then
    echo "❌ 테스트 실패: 출력 파일이 생성되지 않았습니다."
    exit 1
fi

echo "생성된 파일: $(basename "$SAMPLE_OUTPUT")"
echo ""
echo "미리보기 (상위 20줄):"
head -n 20 "$SAMPLE_OUTPUT"

echo ""
echo "----------------------------------------"
echo "디렉토리 단위 리파인 실행..."
echo "----------------------------------------"
java -cp "$CLASSPATH" refine_json "$DATASET_DIR"

BATCH_COUNT=$(find "$DATASET_DIR" -maxdepth 1 -name 'page_*_refine.json' | wc -l | tr -d ' ')
if [ "$BATCH_COUNT" -eq 0 ]; then
    echo "❌ 테스트 실패: 디렉토리 리파인 결과가 없습니다."
    exit 1
fi

echo "생성된 배치 파일: ${BATCH_COUNT}개"
find "$DATASET_DIR" -maxdepth 1 -name 'page_*_refine.json' | head -n 5 | sed 's|.*/||'

echo ""
echo "=========================================="
echo "✅ refine_json 테스트 성공!"
echo "=========================================="
(cd "$DATASET_DIR" && echo "출력 디렉토리: $(pwd)")
echo ""
