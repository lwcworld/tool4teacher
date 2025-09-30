# Tool4Teacher

교사를 위한 한글 문서 처리 도구 모음

## 목차
- [소개](#소개)
- [주요 기능](#주요-기능)
- [사전 요구사항](#사전-요구사항)
- [설치 및 실행](#설치-및-실행)
  - [1. Docker 컨테이너 생성](#1-docker-컨테이너-생성)
  - [2. 환경 설정](#2-환경-설정)
  - [3. 프로그램 실행](#3-프로그램-실행)
- [프로젝트 구조](#프로젝트-구조)
- [사용 예제](#사용-예제)
- [개발 환경](#개발-환경)
- [참고 문서](#참고-문서)
- [라이센스](#라이센스)

## 소개

Tool4Teacher는 교사들이 한글 문서(HWPX)를 프로그래밍 방식으로 생성, 편집, 처리할 수 있도록 돕는 도구 모음입니다. [hwpxlib](https://github.com/neolord0/hwpxlib) 라이브러리를 기반으로 구축되었습니다.

## 주요 기능

- 한글 문서(HWPX) 생성
- 한글 문서 텍스트 추출 및 편집
- 문서 템플릿 기반 자동화
- 표, 이미지 등 다양한 문서 요소 처리

## 사전 요구사항

- Docker (컨테이너 기반 실행을 위해)
- 또는 직접 설치 시:
  - Java 11 이상
  - Maven 3.6 이상

## 설치 및 실행

### 1. Docker 컨테이너 생성

#### 방법 A: 자동 스크립트 사용 (권장)

프로젝트에 포함된 스크립트를 사용하여 컨테이너를 생성합니다:

```bash
# 스크립트 실행 권한 부여
chmod +x create_ubuntu_container.sh

# 컨테이너 생성
./create_ubuntu_container.sh
```

실행 시 다음 정보를 입력해야 합니다:
- **로컬 디렉토리 경로**: 컨테이너와 공유할 디렉토리 (예: `/Users/username/workspace/tool4teacher`)
- **컨테이너 이름**: 생성할 컨테이너의 이름 (기본값: `ubuntu-container`)

#### 방법 B: 수동으로 컨테이너 생성

```bash
# Ubuntu 24.04 이미지 다운로드
docker pull ubuntu:24.04

# 컨테이너 생성 및 실행
# [LOCAL_DIR]을 실제 프로젝트 경로로 변경하세요
docker run -it \
    --name tool4teacher-container \
    -v /path/to/tool4teacher:/home/tool4teacher \
    ubuntu:24.04 \
    /bin/bash
```

#### 컨테이너 재시작 및 접속

```bash
# 컨테이너 시작
docker start tool4teacher-container

# 컨테이너 접속
docker exec -it tool4teacher-container /bin/bash
```

### 2. 환경 설정

컨테이너 내부에서 필요한 개발 환경을 설치합니다:

```bash
# 프로젝트 디렉토리로 이동
cd /home/tool4teacher

# 자동 설치 스크립트 실행
chmod +x install.sh
./install.sh
```

`install.sh` 스크립트는 다음을 자동으로 설치합니다:
- OpenJDK 11
- Maven
- 프로젝트 의존성 (hwpxlib 등)

### 3. 프로그램 실행

#### 예제 1: Hello World 문서 생성

"hello world" 텍스트가 포함된 한글 문서를 생성하는 예제:

```bash
# Maven을 통한 실행 (기본 예제)
mvn exec:java
```

실행 결과:
- `output/hello_world.hwpx` 파일이 생성됩니다.

#### 예제 2: 4x4 테이블 문서 생성

테두리가 있는 4x4 빈 테이블을 생성하는 예제:

```bash
# 특정 클래스 직접 실행
mvn compile
java -cp "target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" Create4x4Table
```

실행 결과:
- `output/table_4x4.hwpx` 파일이 생성됩니다.

#### 빌드 및 테스트

```bash
# 프로젝트 컴파일
mvn compile

# 전체 빌드 (clean + compile)
mvn clean compile

# 패키지 생성 (JAR 파일)
mvn package
```

## 프로젝트 구조

```
tool4teacher/
├── README.md                      # 프로젝트 문서
├── pom.xml                        # Maven 설정 파일
├── install.sh                     # 자동 환경 설치 스크립트
├── create_ubuntu_container.sh     # Docker 컨테이너 생성 스크립트
├── src/
│   └── main/
│       └── java/
│           ├── CreateHangulFile.java  # 예제 1: Hello World 문서 생성
│           └── Create4x4Table.java    # 예제 2: 4x4 테이블 생성
├── output/
│   ├── hello_world.hwpx           # 예제 1 결과 파일
│   └── table_4x4.hwpx             # 예제 2 결과 파일
├── docs/
│   └── hwpxlib_functions.md       # hwpxlib 라이브러리 전체 문서 및 가이드
└── third_party/
    ├── hwpxlib/                   # 한글 문서 처리 라이브러리
    └── dots.ocr/                  # OCR 라이브러리
```

## 사용 예제

### 예제 1: Hello World 문서 생성
`src/main/java/CreateHangulFile.java` - 기본적인 텍스트 문서 생성 방법

### 예제 2: 4x4 테이블 생성
`src/main/java/Create4x4Table.java` - 테두리가 있는 테이블 생성 방법

### 추가 문서
더 많은 기능, API 사용법, 그리고 **테이블 생성 시 주의사항**은 [`docs/hwpxlib_functions.md`](docs/hwpxlib_functions.md)를 참고하세요.
- 전체 API 문서
- 실전 예제 모음
- 테이블 생성 시행착오 및 해결 방법
- 베스트 프랙티스

## 개발 환경

- **언어**: Java 11
- **빌드 도구**: Maven 3.6+
- **주요 라이브러리**:
  - [hwpxlib 1.0.6](https://github.com/neolord0/hwpxlib) - 한글 문서(HWPX) 처리
- **개발 환경**: Ubuntu 24.04 (Docker)

## 참고 문서

- [hwpxlib 기능 구현 메뉴얼](docs/hwpxlib_functions.md) - hwpxlib 라이브러리의 전체 API 및 기능 구현 가이드
- [hwpxlib GitHub](https://github.com/neolord0/hwpxlib) - 원본 라이브러리 저장소

## 라이센스

이 프로젝트는 다음 오픈소스 라이브러리를 사용합니다:
- [hwpxlib](https://github.com/neolord0/hwpxlib) - Apache-2.0 License

## 기여

버그 리포트 및 기능 제안은 Issues를 통해 제출해주세요.