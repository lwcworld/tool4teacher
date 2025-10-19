#!/bin/bash

# DotsOCR 처리를 위한 Conda 환경 설정 스크립트
# 이 스크립트는 dotsocr이라는 이름의 conda 환경을 생성하고 필요한 패키지를 설치합니다.

set -e  # 에러 발생 시 스크립트 중단

echo "========================================"
echo "DotsOCR 환경 설정 시작"
echo "========================================"

# Conda 환경 이름
ENV_NAME="dotsocr"

# 0. 시스템 패키지 설치 (poppler-utils for PDF processing)
echo "----------------------------------------"
echo "시스템 패키지 확인 중..."
echo "----------------------------------------"

# sudo 권한이 있는 경우에만 apt 패키지 설치 시도
if command -v apt-get &> /dev/null && [ "$EUID" -eq 0 ]; then
    echo "시스템 레벨 패키지 설치 중..."
    apt-get update -qq
    apt-get install -y -qq poppler-utils
    echo "✓ poppler-utils 설치 완료"
elif command -v apt-get &> /dev/null && sudo -n true 2>/dev/null; then
    echo "시스템 레벨 패키지 설치 중 (sudo 사용)..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq poppler-utils
    echo "✓ poppler-utils 설치 완료"
else
    echo "⚠ 시스템 패키지 설치를 건너뜁니다. (sudo 권한 없음 또는 apt-get 없음)"
    echo "PDF 처리를 위해 poppler-utils가 필요합니다."
    echo "수동으로 설치하세요: sudo apt-get install poppler-utils"
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

# 2. Conda 환경 생성 (Python 3.11)
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

# 4. 필요한 Python 패키지 설치
echo "----------------------------------------"
echo "Python 패키지 설치 중..."
echo "----------------------------------------"

# requirements.txt를 사용하여 패키지 설치
if [ -f "scripts/dotsocr/requirements.txt" ]; then
    pip install -r scripts/dotsocr/requirements.txt
    echo "✓ requirements.txt에서 패키지 설치 완료"
else
    echo "⚠ scripts/dotsocr/requirements.txt를 찾을 수 없습니다."
    echo "기본 패키지 설치 중..."
    pip install replicate python-dotenv requests pillow aiohttp
fi

# 5. 설치 확인
echo "----------------------------------------"
echo "설치 확인"
echo "----------------------------------------"
python --version
pip list | grep -E "replicate|python-dotenv|requests|pillow|aiohttp"

# 6. 환경 변수 파일 생성 안내
echo "----------------------------------------"
echo "환경 변수 설정 안내"
echo "----------------------------------------"
if [ ! -f "scripts/dotsocr/.env" ]; then
    echo ".env 파일이 없습니다."
    echo "scripts/dotsocr/.env 파일을 생성하고 다음 내용을 추가하세요:"
    echo ""
    echo "REPLICATE_API_TOKEN=your_replicate_api_token_here"
    echo ""
    echo "Replicate API 토큰은 https://replicate.com/account/api-tokens 에서 발급받을 수 있습니다."
else
    echo ".env 파일이 이미 존재합니다."
fi

echo "========================================"
echo "DotsOCR 환경 설정 완료!"
echo "========================================"
echo ""
echo "설치된 패키지:"
echo "  - Python 3.11"
echo "  - replicate (Replicate API 클라이언트)"
echo "  - python-dotenv (환경 변수 관리)"
echo "  - requests (HTTP 클라이언트)"
echo "  - pillow (이미지 처리)"
echo "  - aiohttp (비동기 HTTP 클라이언트)"
echo ""
echo "환경 활성화 방법:"
echo "  conda activate ${ENV_NAME}"
echo ""
echo "다음 단계:"
echo "  1. Replicate 계정 생성 (https://replicate.com)"
echo "  2. API 토큰 발급 (https://replicate.com/account/api-tokens)"
echo "  3. scripts/dotsocr/.env 파일에 토큰 추가"
echo "     REPLICATE_API_TOKEN=your_token_here"
echo "  4. DotsOCR 애플리케이션 실행"
echo ""
echo "환경 비활성화 방법:"
echo "  conda deactivate"
echo ""
echo "참고:"
echo "  - DotsOCR API: https://replicate.com/sljeff/dots.ocr/api"
echo "  - Replicate Python 클라이언트: https://github.com/replicate/replicate-python"
echo "========================================"
