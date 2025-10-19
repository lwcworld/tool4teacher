#!/bin/bash

# HWP 처리를 위한 Conda 환경 설정 스크립트
# 이 스크립트는 hwp라는 이름의 conda 환경을 생성하고 필요한 패키지를 설치합니다.

set -e  # 에러 발생 시 스크립트 중단

echo "========================================"
echo "HWP 환경 설정 시작"
echo "========================================"

# Conda 환경 이름
ENV_NAME="hwp"

# 0. 시스템 패키지 업데이트 및 설치 (선택적)
echo "----------------------------------------"
echo "시스템 패키지 확인 중..."
echo "----------------------------------------"

# sudo 권한이 있는 경우에만 apt 패키지 설치 시도
if command -v apt-get &> /dev/null && [ "$EUID" -eq 0 ]; then
    echo "시스템 레벨 패키지 설치 중..."
    apt-get update -qq
    apt-get install -y -qq openjdk-11-jdk maven git
    echo "시스템 패키지 설치 완료"
elif command -v apt-get &> /dev/null && sudo -n true 2>/dev/null; then
    echo "시스템 레벨 패키지 설치 중 (sudo 사용)..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq openjdk-11-jdk maven git
    echo "시스템 패키지 설치 완료"
else
    echo "시스템 패키지 설치를 건너뜁니다. (sudo 권한 없음 또는 apt-get 없음)"
    echo "Conda를 통해 필요한 패키지를 설치합니다."
fi

# 1. 기존 환경이 있는지 확인
if conda env list | grep -q "^${ENV_NAME} "; then
    echo "기존 ${ENV_NAME} 환경이 발견되었습니다."
    read -p "기존 환경을 삭제하고 새로 생성하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "기존 환경 삭제 중..."
        conda env remove -n ${ENV_NAME} -y
    else
        echo "환경 설정을 중단합니다."
        exit 0
    fi
fi

# 2. Conda 환경 생성 (Python 포함)
echo "----------------------------------------"
echo "Conda 환경 생성 중: ${ENV_NAME}"
echo "----------------------------------------"
conda create -n ${ENV_NAME} python=3.11 -y

# 3. 환경 활성화
echo "----------------------------------------"
echo "환경 활성화 중..."
echo "----------------------------------------"
eval "$(conda shell.bash hook)"
conda activate ${ENV_NAME}

# 4. Java 설치 (conda)
echo "----------------------------------------"
echo "Java 설치 중..."
echo "----------------------------------------"
conda install -c conda-forge openjdk=11 -y

# 5. 설치 확인
echo "----------------------------------------"
echo "설치 확인"
echo "----------------------------------------"
java -version
mvn -version

# 6. Git 서브모듈 초기화 (hwpxlib)
echo "----------------------------------------"
echo "Git 서브모듈 초기화 중..."
echo "----------------------------------------"
if [ -d "third_party/hwpxlib" ] && [ -z "$(ls -A third_party/hwpxlib)" ]; then
    echo "hwpxlib 서브모듈 초기화..."
    git submodule update --init --recursive third_party/hwpxlib
else
    echo "hwpxlib 서브모듈이 이미 초기화되어 있습니다."
fi

# 7. scripts/hwp 디렉토리로 이동
echo "----------------------------------------"
echo "scripts/hwp 디렉토리로 이동..."
echo "----------------------------------------"
cd scripts/hwp || { echo "scripts/hwp 디렉토리를 찾을 수 없습니다."; exit 1; }

# 8. Maven 의존성 다운로드
echo "----------------------------------------"
echo "Maven 의존성 다운로드 중..."
echo "----------------------------------------"
mvn dependency:resolve

echo "========================================"
echo "HWP 환경 설정 완료!"
echo "========================================"
echo ""
echo "설치된 패키지:"
echo "  - Python 3.11 (conda)"
echo "  - OpenJDK 11 (conda)"
echo "  - Apache Maven (시스템)"
echo "  - Git (시스템)"
echo ""
echo "환경 활성화 방법:"
echo "  conda activate ${ENV_NAME}"
echo ""
echo "Java 코드 실행 방법:"
echo "  # scripts/hwp 디렉토리로 이동"
echo "  cd scripts/hwp"
echo ""
echo "  # CreateHangulFile 실행 (hello world 문서 생성)"
echo "  mvn clean compile exec:java -Dexec.mainClass=\"CreateHangulFile\""
echo ""
echo "  # Create4x4Table 실행 (4x4 테이블 문서 생성)"
echo "  mvn clean compile exec:java -Dexec.mainClass=\"Create4x4Table\""
echo ""
echo "출력 파일 위치:"
echo "  scripts/hwp/output/hello_world.hwpx"
echo "  scripts/hwp/output/table_4x4.hwpx"
echo ""
echo "환경 비활성화 방법:"
echo "  conda deactivate"
echo ""
echo "참고:"
echo "  - scripts/hwp/docs/hwpxlib_functions.md 에서 hwpxlib API 문서 확인"
echo "  - third_party/hwpxlib/ 에서 예제 파일 참고"
echo "========================================"
