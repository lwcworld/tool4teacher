# HWP Scripts

한글(HWP/HWPX) 파일 생성 및 LaTeX 수식 변환 도구 모음

## 디렉토리 구조

```
scripts/hwp/
├── src/
│   ├── main/java/          # 메인 프로그램
│   │   ├── equations_from_json.java    # JSON에서 LaTeX 수식 추출 → HWPX 변환
│   │   ├── hello_world.java            # 기본 한글 파일 생성 예제
│   │   ├── table_4x4.java              # 4x4 표 생성 예제
│   │   └── test_simple.java            # 간단한 테스트 파일 생성
│   │
│   └── utility/            # 유틸리티 모듈
│       ├── LatexToHwpTranslator.java   # LaTeX → HWP 수식 번역기
│       └── TestLatexTranslator.java    # 번역기 테스트 프로그램
│
├── test/                   # 테스트 스크립트
│   ├── test_all.sh
│   ├── test_hello_world.sh
│   ├── test_table_4x4.sh
│   └── test_theta.java     # OCR 오류 패턴 테스트
│
├── output/                 # 생성된 HWPX 파일 저장 위치
├── docs/                   # 문서
├── pom.xml                 # Maven 설정
├── run_equations.sh        # equations_from_json 실행 스크립트
└── test_translator.sh      # 번역기 테스트 실행 스크립트
```

## 주요 기능

### 1. LaTeX 수식 변환 (equations_from_json)

OCR로 생성된 JSON 파일에서 LaTeX 수식을 추출하여 HWPX 파일로 변환합니다.

**실행 방법:**
```bash
./run_equations.sh <JSON_FILE_PATH>

# 예제
./run_equations.sh ../../dataset/downloads/suneung/수학영역_문제지/page_0001.json
```

**특징:**
- JSON 파일에서 `$...$` (인라인) 및 `$$...$$` (블록) 수식 자동 추출
- LaTeX 문법을 HWP 수식 문법으로 자동 번역
- 각 수식을 텍스트와 Equation 객체 두 가지 형식으로 출력
- OCR 오류 자동 복구 (예: 탭 문자 → 백슬래시)

### 2. LaTeX to HWP 번역기 (LatexToHwpTranslator)

LaTeX 수식 문법을 HWP 수식 문법으로 변환하는 유틸리티입니다.

**지원하는 변환:**
- 분수: `\frac{a}{b}` → `{a} over {b}`
- 제곱근: `\sqrt{x}` → `sqrt {x}`
- n제곱근: `\sqrt[n]{x}` → `nroot {n} {x}`
- 그리스 문자: `\theta` → `theta`, `\pi` → `pi`
- 삼각함수: `\sin`, `\cos`, `\tan` → `sin`, `cos`, `tan`
- 극한: `\lim_{x \to a}` → `lim from {x -> a}`
- 합/적분: `\sum`, `\int`, `\prod` → `sum`, `int`, `prod`
- 비교 연산자: `\leq` → `<=`, `\geq` → `>=`
- 특수 기호: `\times` → `*`, `\infty` → `infinity`
- 중첩 구조 지원 (예: `\frac{\sqrt{2}}{2}`)

### 3. 번역기 테스트 (TestLatexTranslator)

번역기의 정확도를 검증하는 자동화된 테스트 프로그램입니다.

**실행 방법:**
```bash
./test_translator.sh [JSON_FILE_PATH]

# 기본 테스트 케이스 사용
./test_translator.sh

# 커스텀 테스트 파일 사용
./test_translator.sh /path/to/equations.json
```

**테스트 케이스:**
- 테스트 케이스는 `dataset/equation_examples/equations.json`에 저장
- 47개의 테스트 케이스, 13개 카테고리
- 각 테스트는 LaTeX 입력, 예상 HWP 출력, 설명 포함
- 상태: passing (통과), pending (미구현), failing (실패)

## 빌드 및 실행

### 요구사항
- Java 11 이상
- Maven 3.6 이상
- hwpxlib 1.0.6
- Gson 2.10.1

### 컴파일
```bash
mvn clean compile
```

### 개별 프로그램 실행
```bash
# equations_from_json 실행
mvn exec:java -Dexec.mainClass="equations_from_json" \
  -Dexec.args="<JSON_FILE_PATH>"

# 또는 스크립트 사용
./run_equations.sh <JSON_FILE_PATH>

# 번역기 테스트
./test_translator.sh
```

## 테스트 케이스 관리

테스트 케이스는 `dataset/equation_examples/equations.json`에서 관리합니다.

**JSON 포맷:**
```json
{
  "description": "LaTeX to HWP equation translation test cases",
  "version": "1.0",
  "categories": [
    {
      "category": "category_name",
      "description": "카테고리 설명",
      "equations": [
        {
          "latex": "\\frac{1}{2}",
          "expected_hwp": "{1} over {2}",
          "description": "기본 분수",
          "status": "passing"
        }
      ]
    }
  ]
}
```

**테스트 케이스 추가:**
1. `equations.json` 파일을 열기
2. 적절한 카테고리에 새 수식 추가
3. `./test_translator.sh` 실행하여 결과 확인

## 예제

### 지원되는 LaTeX 수식 예제

```latex
# 기본
x^{2} + y^{2}          # 지수
x_{i}                  # 아래 첨자

# 분수
\frac{1}{2}            # 기본 분수
\frac{\sqrt{2}}{2}     # 중첩 분수

# 제곱근
\sqrt{24}              # 제곱근
\sqrt[3]{8}            # 세제곱근

# 삼각함수
\sin(x)
\tan\theta

# 극한
\lim_{h \to 0} \frac{f(2+h) - f(2)}{h}

# 합/적분
\sum_{i=1}^{n} i
\int_{0}^{1} x^2 dx

# 조건부 함수
f(x) = \begin{cases}
  3x - a & (x < 2) \\
  x^2 + a & (x \ge 2)
\end{cases}
```

## 개발 가이드

### 번역 규칙 추가

`src/utility/LatexToHwpTranslator.java`에서:

1. 새 변환 함수 작성:
```java
private static String translateNewPattern(String latex) {
    return latex.replaceAll("\\\\pattern", "hwp_replacement");
}
```

2. `translate()` 메소드에 추가:
```java
public static String translate(String latex) {
    // ... 기존 변환들
    hwp = translateNewPattern(hwp);
    return hwp;
}
```

3. 테스트 케이스 추가 (`equations.json`)

4. 테스트 실행:
```bash
./test_translator.sh
```

## 알려진 이슈 및 제한사항

### 현재 실패하는 테스트 케이스
1. **공백 문제**: `\tan\theta` → `tantheta` (공백 누락)
2. **OCR 오류**: 탭 문자 복구가 번역기에 미적용

### Pending 기능
- 일부 그리스 문자 (alpha, beta 등은 구현됨)
- 복잡한 행렬
- 대괄호/중괄호 크기 조절 (`\left`, `\right`)

## 참고 자료

- hwpxlib 문서: `docs/hwpxlib_functions.md`
- LaTeX 수식 문법: [LaTeX Math Wiki](https://en.wikibooks.org/wiki/LaTeX/Mathematics)
- HWP 수식 문법: hwpxlib 예제 파일 참고

## 라이선스

Tool4Teacher 프로젝트의 일부입니다.
