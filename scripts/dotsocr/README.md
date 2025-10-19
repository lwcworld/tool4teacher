# DotsOCR Python Application

Python 애플리케이션으로 Replicate의 DotsOCR API를 사용하여 이미지와 PDF 파일에서 텍스트를 추출합니다.

## 기능

- 로컬 이미지 파일에서 OCR
- 이미지 URL에서 OCR
- 로컬 PDF 파일에서 OCR (멀티페이지 지원)
- PDF URL에서 OCR
- 동기/비동기 처리 지원
- 배치 처리 (여러 파일 동시 처리)

## 설치

### 1. Conda 환경 설정

프로젝트 루트 디렉토리에서 setup 스크립트를 실행합니다:

```bash
cd /path/to/tool4teacher
./setup/conda_dotsocr_setup.sh
```

환경 활성화:

```bash
conda activate dotsocr
```

### 2. 수동 설치 (선택사항)

Conda를 사용하지 않는 경우:

```bash
cd scripts/dotsocr
pip install -r requirements.txt
```

### 3. API 토큰 설정

1. [Replicate](https://replicate.com) 계정 생성
2. [API 토큰 발급](https://replicate.com/account/api-tokens)
3. `.env` 파일 생성:

```bash
cd scripts/dotsocr
cp .env.example .env
```

4. `.env` 파일에 토큰 추가:

```env
REPLICATE_API_TOKEN=your_replicate_api_token_here
```

## 사용 방법

### 기본 사용

```python
from dotsocr_client import DotsOCRClient

# 클라이언트 초기화
client = DotsOCRClient()

# 이미지에서 텍스트 추출
text = client.ocr_image("image.jpg")
print(text)

# PDF에서 텍스트 추출
text = client.ocr_pdf("document.pdf")
print(text)

# URL에서 텍스트 추출
text = client.ocr_image_url("https://example.com/image.jpg")
print(text)
```

### 동기 예제 실행

```bash
cd scripts/dotsocr
python example_sync.py
```

`example_sync.py` 파일을 열어 사용하고 싶은 예제의 주석을 해제하세요:

```python
# example_image_file()      # 로컬 이미지 파일 OCR
# example_image_url()       # 이미지 URL OCR
# example_pdf_file()        # 로컬 PDF 파일 OCR
# example_pdf_url()         # PDF URL OCR
# example_general_ocr()     # 범용 OCR 메서드
# example_save_to_file()    # OCR 결과를 파일로 저장
```

### 비동기 예제 실행

여러 파일을 동시에 처리하려면:

```bash
cd scripts/dotsocr
python example_async.py
```

`example_async.py` 파일을 열어 사용하고 싶은 예제의 주석을 해제하세요:

```python
# 모든 파일을 동시에 처리
# asyncio.run(process_multiple_files(example_files))

# 동시 처리 제한 (API 요청 수 제어)
# asyncio.run(process_batch_with_limit(example_files, max_concurrent=2))

# 결과를 파일로 저장
# asyncio.run(example_save_results(example_files, output_dir="ocr_output"))
```

## API 참조

### DotsOCRClient

#### 초기화

```python
client = DotsOCRClient(api_token=None)
```

- `api_token`: Replicate API 토큰 (선택사항, 환경변수에서 자동 로드)

#### 메서드

##### ocr_image(image_path, **kwargs)

로컬 이미지 파일에서 텍스트 추출

```python
text = client.ocr_image("image.jpg")
```

##### ocr_image_url(image_url, **kwargs)

이미지 URL에서 텍스트 추출

```python
text = client.ocr_image_url("https://example.com/image.jpg")
```

##### ocr_pdf(pdf_path, **kwargs)

로컬 PDF 파일에서 텍스트 추출

```python
text = client.ocr_pdf("document.pdf")
```

##### ocr_pdf_url(pdf_url, **kwargs)

PDF URL에서 텍스트 추출

```python
text = client.ocr_pdf_url("https://example.com/document.pdf")
```

##### ocr(source, is_url=False, **kwargs)

범용 OCR 메서드 (파일 형식 자동 감지)

```python
# 로컬 파일
text = client.ocr("file.jpg")

# URL
text = client.ocr("https://example.com/file.pdf", is_url=True)
```

## 예제 시나리오

### 1. 단일 이미지 처리

```python
from dotsocr_client import DotsOCRClient

client = DotsOCRClient()
text = client.ocr_image("receipt.jpg")
print(text)
```

### 2. 여러 파일 배치 처리

```python
import asyncio
from dotsocr_client import DotsOCRClient

async def process_files(file_list):
    client = DotsOCRClient()
    tasks = []

    for file_path in file_list:
        loop = asyncio.get_event_loop()
        task = loop.run_in_executor(None, client.ocr, file_path)
        tasks.append(task)

    results = await asyncio.gather(*tasks)
    return results

files = ["doc1.pdf", "doc2.pdf", "image1.jpg"]
results = asyncio.run(process_files(files))
```

### 3. OCR 결과 저장

```python
from dotsocr_client import DotsOCRClient
from pathlib import Path

client = DotsOCRClient()

input_file = "document.pdf"
output_file = "document_text.txt"

text = client.ocr_pdf(input_file)

with open(output_file, 'w', encoding='utf-8') as f:
    f.write(text)

print(f"Extracted {len(text)} characters")
print(f"Saved to: {output_file}")
```

## 디렉토리 구조

```
scripts/dotsocr/
├── .env.example           # 환경변수 템플릿
├── README.md              # 이 파일
├── requirements.txt       # Python 패키지 의존성
├── dotsocr_client.py      # DotsOCR 클라이언트 모듈
├── example_sync.py        # 동기 처리 예제
└── example_async.py       # 비동기 처리 예제
```

## 문제 해결

### API 토큰 오류

```
ValueError: Replicate API token not found
```

**해결 방법:**
1. `.env` 파일이 존재하는지 확인
2. `REPLICATE_API_TOKEN`이 올바르게 설정되었는지 확인
3. `.env` 파일이 스크립트와 같은 디렉토리에 있는지 확인

### 파일을 찾을 수 없음

```
FileNotFoundError: Image file not found
```

**해결 방법:**
1. 파일 경로가 올바른지 확인
2. 절대 경로를 사용하거나 현재 디렉토리 확인
3. 파일 권한 확인

### 패키지 임포트 오류

```
ModuleNotFoundError: No module named 'replicate'
```

**해결 방법:**
1. Conda 환경이 활성화되었는지 확인: `conda activate dotsocr`
2. 패키지 재설치: `pip install -r requirements.txt`

## 참고 자료

- [DotsOCR on Replicate](https://replicate.com/sljeff/dots.ocr)
- [DotsOCR API Documentation](https://replicate.com/sljeff/dots.ocr/api)
- [Replicate Python Client](https://github.com/replicate/replicate-python)
- [Replicate API Documentation](https://replicate.com/docs)

## 라이선스

이 프로젝트는 Tool4Teacher 프로젝트의 일부입니다.

## 기여

버그 리포트나 기능 제안은 이슈로 등록해주세요.
