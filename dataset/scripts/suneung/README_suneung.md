# 수능 시험지 자동 다운로드 스크립트

수능 홈페이지(https://www.suneung.re.kr)에서 시험지를 자동으로 다운로드하는 스크립트입니다.

## 설치

### 1. Python 패키지 설치

```bash
pip install -r scripts/requirements_suneung.txt
```

### 2. Chrome 및 ChromeDriver 설치

#### Ubuntu/Debian
```bash
# Chrome 설치
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt install ./google-chrome-stable_current_amd64.deb

# ChromeDriver는 Selenium이 자동으로 관리합니다
```

#### macOS
```bash
brew install --cask google-chrome
```

#### Windows
Chrome을 공식 사이트에서 다운로드하여 설치하세요.

## 사용법

### 기본 사용

```bash
# 최근 10페이지의 모든 시험지 다운로드
python scripts/download_suneung.py

# 특정 연도 시험지 다운로드
python scripts/suneung/download_suneung.py --year 2024

# 특정 과목 시험지 다운로드
python scripts/suneung/download_suneung.py --subject 국어

# 연도와 과목을 함께 지정
python scripts/suneung/download_suneung.py --year 2024 --subject 수학

# 최대 페이지 수 지정
python scripts/suneung/download_suneung.py --year 2024 --max-pages 5
```

### 고급 옵션

```bash
# 다운로드 디렉토리 지정
python scripts/download_suneung.py --output /path/to/download

# 브라우저 화면 표시 (디버깅용)
python scripts/download_suneung.py --no-headless

# 상세 로그 출력
python scripts/download_suneung.py --verbose
```

## 옵션 설명

- `--year`: 다운로드할 연도 (예: 2024, 2023)
- `--subject`: 다운로드할 과목 (예: 국어, 수학, 영어, 사회, 과학)
- `--output`: 다운로드 디렉토리 (기본값: downloads/suneung)
- `--max-pages`: 최대 페이지 수 (기본값: 10)
- `--no-headless`: 브라우저 화면 표시
- `--verbose`: 상세 로그 출력

## 예제

### 2024년 국어 시험지만 다운로드
```bash
python scripts/download_suneung.py --year 2024 --subject 국어 --output downloads/korean_2024
```

### 최근 3페이지만 빠르게 확인
```bash
python scripts/download_suneung.py --max-pages 3 --no-headless
```

### 모든 수학 시험지 다운로드
```bash
python scripts/download_suneung.py --subject 수학 --max-pages 20
```

## 주의사항

1. **웹사이트 부하**: 다운로드 사이에 자동으로 대기 시간을 두어 서버에 부담을 주지 않도록 합니다.

2. **ChromeDriver**: Selenium 4.x 이상은 ChromeDriver를 자동으로 관리하므로 별도 설치가 필요 없습니다.

3. **네트워크**: 대용량 파일을 다운로드하므로 안정적인 네트워크 연결이 필요합니다.

4. **저장 공간**: 다운로드할 파일 개수에 따라 충분한 디스크 공간이 필요합니다.

## 문제 해결

### ChromeDriver 오류
```bash
# Chrome과 ChromeDriver 버전이 맞지 않는 경우
pip install --upgrade selenium
```

### 다운로드가 시작되지 않는 경우
```bash
# 브라우저를 표시하여 확인
python scripts/download_suneung.py --no-headless --verbose
```

### 권한 오류 (Linux)
```bash
# 실행 권한 추가
chmod +x scripts/download_suneung.py
```

## 라이선스

이 스크립트는 교육 목적으로만 사용하세요. 다운로드한 시험지의 저작권은 한국교육과정평가원에 있습니다.
