#!/bin/bash
# 모든 HWP 예제 테스트 스크립트

set -e

echo "=========================================="
echo "HWP 예제 전체 테스트"
echo "=========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 테스트 결과 추적
PASSED=0
FAILED=0
TOTAL=0

# 각 테스트 실행
run_test() {
    local test_script=$1
    local test_name=$(basename "$test_script" .sh)

    TOTAL=$((TOTAL + 1))
    echo ""
    echo "[$TOTAL] 테스트 실행: $test_name"
    echo "===================="

    if bash "$test_script"; then
        PASSED=$((PASSED + 1))
        echo "✅ $test_name 통과"
    else
        FAILED=$((FAILED + 1))
        echo "❌ $test_name 실패"
    fi
}

# 모든 테스트 실행
run_test "$SCRIPT_DIR/test_hello_world.sh"
run_test "$SCRIPT_DIR/test_table_4x4.sh"

# 최종 결과 출력
echo ""
echo "=========================================="
echo "테스트 결과 요약"
echo "=========================================="
echo "총 테스트: $TOTAL"
echo "통과: $PASSED"
echo "실패: $FAILED"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo "✅ 모든 테스트 통과!"
    exit 0
else
    echo "❌ $FAILED 개의 테스트 실패"
    exit 1
fi
