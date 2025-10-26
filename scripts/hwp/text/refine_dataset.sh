#!/bin/bash
# dotsocr JSON -> text/math refinement 실행 스크립트 (dataset 전체 대상)

set -e

echo "=========================================="
echo "dataset JSON refine 실행"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

DATASET_DIR="../../dataset/downloads/suneung/수학영역_문제지"

if [ ! -d "$DATASET_DIR" ]; then
    echo "❌ 데이터셋 디렉토리를 찾을 수 없습니다: $DATASET_DIR"
    exit 1
fi

echo "대상 디렉토리: $(cd "$DATASET_DIR" && pwd)"

echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

CLASSPATH="target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)"

echo "----------------------------------------"
echo "refine_json 실행..."
echo "----------------------------------------"
java -cp "$CLASSPATH" refine_json "$DATASET_DIR"

echo ""
echo "완료된 파일 예시:"
find "$DATASET_DIR" -maxdepth 1 -name 'page_*_refine.json' | head -n 5 | sed 's|.*/||'

echo ""
echo "=========================================="
echo "✅ dataset JSON refine 완료"
echo "=========================================="
