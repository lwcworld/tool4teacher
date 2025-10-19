#!/bin/bash
# LaTeX to HWP 번역기 종합 테스트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 기본 JSON 파일 경로
JSON_FILE="$PROJECT_DIR/../../dataset/equation_examples/equations.json"

# 명령줄 인자로 JSON 파일 경로 전달 가능
if [ $# -gt 0 ]; then
    JSON_FILE="$1"
fi

echo "=========================================="
echo "LaTeX → HWP 번역기 테스트"
echo "프로젝트 경로: $PROJECT_DIR"
echo "테스트 파일: $JSON_FILE"
echo "=========================================="
echo ""

# 프로젝트 빌드
(cd "$PROJECT_DIR" && mvn -q compile)

# 번역기 실행
(cd "$PROJECT_DIR" && \
    java -cp "target/classes:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" \
    TestLatexTranslator "$JSON_FILE")
