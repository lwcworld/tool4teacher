#!/bin/bash

# Tool4Teacher 프로젝트 설치 스크립트
# Java와 Maven을 설치하고 프로젝트를 빌드합니다.

set -e  # 오류 발생 시 스크립트 중단

echo "========================================="
echo "Tool4Teacher 설치 시작"
echo "========================================="

# 1. 패키지 목록 업데이트
echo ""
echo "[1/4] 패키지 목록 업데이트 중..."
apt-get update -qq

# 2. Java 11 JDK 설치
echo ""
echo "[2/4] OpenJDK 11 설치 중..."
apt-get install -y openjdk-11-jdk

# Java 버전 확인
echo ""
echo "설치된 Java 버전:"
java -version

# 3. Maven 설치
echo ""
echo "[3/4] Maven 설치 중..."
apt-get install -y maven

# Maven 버전 확인
echo ""
echo "설치된 Maven 버전:"
mvn -version

# 4. Maven 의존성 다운로드 및 컴파일
echo ""
echo "[4/4] 프로젝트 의존성 다운로드 및 컴파일 중..."
mvn clean compile

echo ""
echo "========================================="
echo "설치 완료!"
echo "========================================="
echo ""
echo "실행 방법:"
echo "  mvn exec:java"
echo ""