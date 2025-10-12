#!/bin/bash

# tool4teacher Docker 컨테이너 설정 스크립트

# 컨테이너 설정
CONTAINER_NAME="tool4teacher"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== ${CONTAINER_NAME} Docker 컨테이너 설정 ===${NC}"

# 사용자로부터 로컬 디렉토리 경로 입력받기
read -p "컨테이너와 연결할 로컬 디렉토리 경로를 입력하세요: " LOCAL_DIR

# 디렉토리 존재 확인
if [ ! -d "$LOCAL_DIR" ]; then
    echo -e "${YELLOW}경고: 디렉토리가 존재하지 않습니다. 생성하시겠습니까? (y/n)${NC}"
    read -p "" CREATE_DIR
    if [ "$CREATE_DIR" = "y" ]; then
        mkdir -p "$LOCAL_DIR"
        echo -e "${GREEN}디렉토리 생성 완료: $LOCAL_DIR${NC}"
    else
        echo -e "${RED}디렉토리가 필요합니다. 스크립트를 종료합니다.${NC}"
        exit 1
    fi
fi

# 절대 경로로 변환
LOCAL_DIR=$(cd "$LOCAL_DIR" && pwd)

echo -e "${GREEN}Step 1: NVIDIA PyTorch 이미지 다운로드${NC}"
docker pull nvcr.io/nvidia/pytorch:24.04-py3

if [ $? -ne 0 ]; then
    echo -e "${RED}이미지 다운로드 실패${NC}"
    exit 1
fi

echo -e "${GREEN}Step 2: 기존 컨테이너 확인 및 제거${NC}"
if docker ps -a | grep -q $CONTAINER_NAME; then
    echo -e "${YELLOW}기존 ${CONTAINER_NAME} 컨테이너를 발견했습니다. 제거하시겠습니까? (y/n)${NC}"
    read -p "" REMOVE_CONTAINER
    if [ "$REMOVE_CONTAINER" = "y" ]; then
        docker stop $CONTAINER_NAME 2>/dev/null
        docker rm $CONTAINER_NAME 2>/dev/null
        echo -e "${GREEN}기존 컨테이너 제거 완료${NC}"
    else
        echo -e "${RED}기존 컨테이너가 존재합니다. 스크립트를 종료합니다.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}Step 3: GPU 지원 확인${NC}"
# NVIDIA GPU 확인
if command -v nvidia-smi &> /dev/null; then
    echo -e "${GREEN}NVIDIA GPU 감지됨${NC}"
    GPU_FLAGS="--gpus all"
    # NVIDIA Docker Runtime 확인
    if ! docker info 2>/dev/null | grep -q nvidia; then
        echo -e "${YELLOW}경고: NVIDIA Docker Runtime이 설치되지 않았을 수 있습니다.${NC}"
        echo -e "${YELLOW}nvidia-docker2 설치를 권장합니다: sudo apt-get install nvidia-docker2${NC}"
    fi
else
    echo -e "${YELLOW}NVIDIA GPU를 감지하지 못했습니다. GPU 없이 진행합니다.${NC}"
    GPU_FLAGS=""
fi

echo -e "${GREEN}Step 4: X11 디스플레이 권한 설정${NC}"
# X11 포워딩을 위한 xhost 권한 부여
xhost +local:docker

echo -e "${GREEN}Step 5: ${CONTAINER_NAME} 컨테이너 생성 및 실행${NC}"
docker run -itd \
    --name $CONTAINER_NAME \
    $GPU_FLAGS \
    --privileged \
    --network host \
    -e DISPLAY=$DISPLAY \
    -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
    -v "$LOCAL_DIR":/home/workspace \
    -v /dev:/dev \
    nvcr.io/nvidia/pytorch:24.04-py3

if [ $? -eq 0 ]; then
    echo -e "${GREEN}=== 컨테이너 생성 완료 ===${NC}"

    echo -e "${GREEN}Step 6: 기타 필수 라이브러리 설치${NC}"
    # docker exec $CONTAINER_NAME bash -c "apt-get update && apt-get install -y \
    #     libvulkan1 \
    #     vulkan-tools \
    #     mesa-vulkan-drivers \
    #     speech-dispatcher \
    #     libxkbcommon-x11-0"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Vulkan 라이브러리 설치 완료${NC}"
    else
        echo -e "${YELLOW}경고: Vulkan 라이브러리 설치 실패${NC}"
    fi

    echo -e ""
    echo -e "${GREEN}컨테이너 이름: ${CONTAINER_NAME}${NC}"
    echo -e "${GREEN}마운트된 디렉토리: $LOCAL_DIR -> /home${NC}"
    echo -e ""
    echo -e "${GREEN}컨테이너 접속: docker exec -it ${CONTAINER_NAME} /bin/bash${NC}"
    echo -e "${GREEN}컨테이너 중지: docker stop ${CONTAINER_NAME}${NC}"
    echo -e "${GREEN}컨테이너 시작: docker start ${CONTAINER_NAME}${NC}"
    echo -e "${GREEN}컨테이너 제거: docker rm -f ${CONTAINER_NAME}${NC}"
else
    echo -e "${RED}컨테이너 생성 실패${NC}"
    exit 1
fi
