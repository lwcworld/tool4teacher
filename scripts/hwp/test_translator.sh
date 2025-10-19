#!/bin/bash
# LaTeX to HWP 번역기 테스트 스크립트

cd "$(dirname "$0")"

# JSON 파일 경로 (기본값)
JSON_FILE="../../dataset/equation_examples/equations.json"

# 명령줄 인수로 JSON 파일 경로 지정 가능
if [ $# -gt 0 ]; then
    JSON_FILE="$1"
fi

echo "Testing LaTeX to HWP translator..."
echo "Test file: $JSON_FILE"
echo ""

# 먼저 컴파일
mvn -q compile

# Java 직접 실행
java -cp "target/classes:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" \
    TestLatexTranslator "$JSON_FILE"
