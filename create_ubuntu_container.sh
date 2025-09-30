#!/bin/bash

# Ubuntu 24.04 Docker 컨테이너 생성 스크립트

# 사용자로부터 디렉토리 경로 입력받기
read -p "포워딩할 로컬 디렉토리 경로를 입력하세요: " LOCAL_DIR

# 디렉토리 존재 여부 확인
if [ ! -d "$LOCAL_DIR" ]; then
    echo "오류: 디렉토리 '$LOCAL_DIR'가 존재하지 않습니다."
    exit 1
fi

# 절대 경로로 변환
LOCAL_DIR=$(cd "$LOCAL_DIR" && pwd)

# 컨테이너 이름 입력받기 (선택사항)
read -p "컨테이너 이름을 입력하세요 (기본값: ubuntu-container): " CONTAINER_NAME
CONTAINER_NAME=${CONTAINER_NAME:-ubuntu-container}

# Ubuntu 24.04 이미지 다운로드
echo "Ubuntu 24.04 이미지를 다운로드합니다..."
docker pull ubuntu:24.04

# 컨테이너 생성 및 실행
echo "컨테이너를 생성합니다..."
docker run -it \
    --name "$CONTAINER_NAME" \
    -v "$LOCAL_DIR:/home/$(basename "$LOCAL_DIR")" \
    ubuntu:24.04 \
    /bin/bash

echo "컨테이너가 종료되었습니다."