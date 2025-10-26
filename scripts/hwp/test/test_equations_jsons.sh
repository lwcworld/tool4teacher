#!/bin/bash
# equations_from_jsons.java 테스트 스크립트

set -e

echo "=========================================="
echo "equations_from_jsons 테스트"
echo "=========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HWP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$HWP_DIR"

# 이전 출력 파일 삭제
if [ -f "output/equations_from_jsons.hwpx" ]; then
    echo "기존 출력 파일 삭제 중..."
    rm output/equations_from_jsons.hwpx
fi

# Maven 컴파일
echo "----------------------------------------"
echo "Maven 컴파일 중..."
echo "----------------------------------------"
mvn compile -q

# Java 실행 (기본 경로 사용: ../../dataset/downloads/suneung/수학영역_문제지)
echo "----------------------------------------"
echo "equations_from_jsons 실행 중..."
echo "----------------------------------------"

# 기본 경로가 존재하지 않으면 스킵
DEFAULT_PATH="../../dataset/downloads/suneung/수학영역_문제지"
if [ ! -d "$DEFAULT_PATH" ]; then
    echo "⚠️  기본 데이터셋 경로가 존재하지 않습니다: $DEFAULT_PATH"
    echo "   테스트를 스킵합니다. (정상)"
    echo ""
    echo "=========================================="
    echo "✅ equations_from_jsons 테스트 스킵 (데이터셋 없음)"
    echo "=========================================="
    exit 0
fi

# equations_from_jsons 실행 (기본 경로 사용)
mvn -q exec:java -Dexec.mainClass="equations_from_jsons" -Dexec.cleanupDaemonThreads=false

# 출력 파일 확인
echo "----------------------------------------"
echo "출력 파일 검증 중..."
echo "----------------------------------------"

if [ ! -f "output/equations_from_jsons.hwpx" ]; then
    echo "❌ 테스트 실패: output/equations_from_jsons.hwpx 파일이 생성되지 않았습니다."
    exit 1
fi

# 파일 크기 확인
FILE_SIZE=$(stat -f%z "output/equations_from_jsons.hwpx" 2>/dev/null || stat -c%s "output/equations_from_jsons.hwpx" 2>/dev/null)
if [ "$FILE_SIZE" -eq 0 ]; then
    echo "❌ 테스트 실패: 생성된 파일이 비어있습니다."
    exit 1
fi

# ZIP 파일 구조 확인
echo "파일 구조 확인:"
unzip -l "output/equations_from_jsons.hwpx" | head -15

# 성공 메시지
echo ""
echo "=========================================="
echo "✅ equations_from_jsons 테스트 성공!"
echo "=========================================="
echo "출력 파일: output/equations_from_jsons.hwpx"
echo "파일 크기: ${FILE_SIZE} bytes"
echo ""
