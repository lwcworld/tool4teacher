# hwpxlib 한글 문서 처리 기능 목록

> **hwpxlib**는 한글과컴퓨터의 워드프로세서 "한글"의 HWPX 파일을 읽고 쓸 수 있는 Java 라이브러리입니다.
>
> **라이브러리 위치:** `third_party/hwpxlib`
> **원본 저장소:** https://github.com/neolord0/hwpxlib

## 목차
- [시작하기](#시작하기)
- [1. 문서 입출력](#1-문서-입출력)
- [2. 문서 구조 및 주요 객체](#2-문서-구조-및-주요-객체)
- [3. 텍스트 추출](#3-텍스트-추출)
- [4. 객체 검색](#4-객체-검색)
- [5. 문서 생성](#5-문서-생성)
- [6. 문서 내용 조작](#6-문서-내용-조작)
- [7. 문서 헤더 및 스타일](#7-문서-헤더-및-스타일)
- [8. 문서 설정](#8-문서-설정)
- [9. 유틸리티 클래스](#9-유틸리티-클래스)
- [10. 열거형 및 상수](#10-열거형-및-상수)
- [실전 예제 모음](#실전-예제-모음)
- [일반적인 패턴 및 베스트 프랙티스](#일반적인-패턴-및-베스트-프랙티스)

---

## 시작하기

### Maven 의존성 추가

```xml
<dependency>
    <groupId>kr.dogfoot</groupId>
    <artifactId>hwpxlib</artifactId>
    <version>1.0.6</version>
</dependency>
```

### 주요 Import 문

```java
// 문서 읽기/쓰기
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;

// 문서 구조
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;

// 헤더 및 스타일
import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.RefList;

// 도구
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import kr.dogfoot.hwpxlib.tool.finder.ObjectFinder;
import kr.dogfoot.hwpxlib.tool.finder.FieldFinder;
import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;

// 유틸리티
import kr.dogfoot.hwpxlib.object.common.ObjectList;

// 예외 처리
import java.io.File;
import java.io.OutputStream;
```

### 기본 코드 템플릿

```java
public class Hwpxoutput {
    public static void main(String[] args) {
        try {
            // HWPX 파일 읽기
            HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

            // 작업 수행
            // ...

            // HWPX 파일 저장
            HWPXWriter.toFilepath(hwpxFile, "output.hwpx");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 1. 문서 입출력

### HWPXReader
**패키지:** `kr.dogfoot.hwpxlib.reader.HWPXReader`

HWPX 파일을 읽는 메인 클래스입니다.

#### 주요 메서드

```java
static HWPXFile fromFilepath(String filepath) throws Exception
```
- 파일 경로에서 HWPX 파일을 읽습니다
- **반환:** 전체 문서 구조를 담은 `HWPXFile` 객체

```java
static HWPXFile fromFile(File file) throws Exception
```
- File 객체에서 HWPX 파일을 읽습니다
- **반환:** `HWPXFile` 객체

```java
static HWPXFile fromFilepath(String filepath, boolean xmlNamespaceAware) throws Exception
```
- XML 네임스페이스 인식 제어 옵션을 포함하여 읽습니다
- **매개변수:**
  - `filepath`: 파일 경로
  - `xmlNamespaceAware`: 네임스페이스 인식 여부 (호환성을 위해 기본값은 true)

```java
static HWPXFile fromFile(File file, boolean xmlNamespaceAware) throws Exception
```
- File 객체 버전으로 네임스페이스 인식 제어 가능

---

### HWPXWriter
**패키지:** `kr.dogfoot.hwpxlib.writer.HWPXWriter`

HWPX 파일을 쓰는 메인 클래스입니다.

#### 주요 메서드

```java
static void toFilepath(HWPXFile hwpxFile, String filepath) throws Exception
```
- HWPXFile 객체를 파일 경로에 저장합니다
- **매개변수:**
  - `hwpxFile`: 저장할 문서 객체
  - `filepath`: 출력 파일 경로

```java
static void toStream(HWPXFile hwpxFile, OutputStream os) throws Exception
```
- HWPXFile을 출력 스트림에 저장합니다
- 네트워크 전송이나 메모리 내 작업에 유용합니다

```java
static byte[] toBytes(HWPXFile hwpxFile) throws Exception
```
- HWPXFile을 바이트 배열로 변환합니다
- **반환:** 전체 HWPX 파일의 바이트 배열

---

## 2. 문서 구조 및 주요 객체

### HWPXFile
**패키지:** `kr.dogfoot.hwpxlib.object.HWPXFile`

전체 HWPX 문서를 나타내는 루트 객체입니다.

#### Getter 메서드

```java
VersionXMLFile versionXMLFile()
```
- 문서 버전 정보

```java
ManifestXMLFile manifestXMLFile()
```
- 파일 매니페스트

```java
ContainerXMLFile containerXMLFile()
```
- 컨테이너 구조

```java
ContentHPFFile contentHPFFile()
```
- 메인 콘텐츠 파일

```java
HeaderXMLFile headerXMLFile()
```
- 문서 헤더 (스타일, 폰트, 속성)

```java
ObjectList<MasterPageXMLFile> masterPageXMLFileList()
```
- 마스터 페이지 (머리글/바닥글)

```java
ObjectList<SectionXMLFile> sectionXMLFileList()
```
- 문서 섹션 (주요 콘텐츠)

```java
SettingsXMLFile settingsXMLFile()
```
- 문서 설정

```java
ObjectList<HistoryXMLFile> historyXMLFileList()
```
- 버전 이력

```java
ObjectList<ChartXMLFile> chartXMLFileList()
```
- 임베디드 차트

```java
UnparsedXMLFile[] unparsedXMLFiles()
```
- 파싱되지 않은 XML 파일들

#### 조작 메서드

```java
void addUnparsedXMLFile(String href, String xml)
```
- 파싱되지 않은 XML 추가

```java
void removeUnparsedXMLFile(UnparsedXMLFile unparsedXMLFile)
```
- 특정 파일 제거

```java
void removeAllUnparsedXMLFiles()
```
- 모든 파싱되지 않은 파일 제거

```java
HWPXFile clone()
```
- 전체 문서를 깊은 복사

```java
void copyFrom(HWPXFile from)
```
- 다른 문서에서 복사

---

## 3. 텍스트 추출

### TextExtractor
**패키지:** `kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor`

HWPX 문서에서 순수 텍스트를 추출하는 도구입니다.

#### 주요 메서드

```java
static String extract(HWPXFile hwpxFile, TextExtractMethod objectExtractMethod,
                     boolean insertParaHead, TextMarks textMarks) throws Exception
```
- 전체 문서에서 모든 텍스트를 추출합니다
- **매개변수:**
  - `hwpxFile`: 원본 문서
  - `objectExtractMethod`: 컨트롤 객체 처리 방법 (enum: `InsertControlTextBetweenParagraphText` 또는 `AppendControlTextAfterParagraphText`)
  - `insertParaHead`: 문단 헤더/번호 포함 여부
  - `textMarks`: 서식용 사용자 정의 마커 (TextMarks 객체)
- **반환:** 추출된 텍스트 문자열

```java
static String extractFrom(HWPXObject from, TextExtractMethod objectExtractMethod,
                         TextMarks textMarks) throws Exception
```
- 특정 문서 부분에서 텍스트를 추출합니다
- Section, Paragraph, Table 등에서 추출 가능

```java
static String extractFrom(ParaListCore from, TextExtractMethod objectExtractMethod,
                         TextMarks textMarks, ObjectPosition startPosition,
                         ObjectPosition endPosition) throws Exception
```
- 특정 범위에서 텍스트를 추출합니다
- **매개변수:** 문단 리스트, 메서드, 마커, 시작/종료 위치

---

### TextMarks
**패키지:** `kr.dogfoot.hwpxlib.tool.textextractor.TextMarks`

텍스트 추출 마커를 구성합니다.

#### 구성 가능한 마커 (setter/getter 메서드)

```java
TextMarks paraSeparator(String value)
TextMarks paraSeparatorAnd(String value)
```
- 문단 구분자

```java
TextMarks lineBreak(String value)
TextMarks lineBreakAnd(String value)
```
- 줄바꿈 마커

```java
TextMarks tab(String value)
TextMarks tabAnd(String value)
```
- 탭 문자 대체

```java
TextMarks fieldStart(String value)
TextMarks fieldStartAnd(String value)
```
- 필드 시작 마커

```java
TextMarks fieldEnd(String value)
TextMarks fieldEndAnd(String value)
```
- 필드 종료 마커

```java
TextMarks tableStart(String value)
TextMarks tableStartAnd(String value)
```
- 표 시작 마커

```java
TextMarks tableEnd(String value)
TextMarks tableEndAnd(String value)
```
- 표 종료 마커

```java
TextMarks tableRowSeparator(String value)
TextMarks tableRowSeparatorAnd(String value)
```
- 표 행 구분자

```java
TextMarks tableCellSeparator(String value)
TextMarks tableCellSeparatorAnd(String value)
```
- 표 셀 구분자

#### 도형 객체 마커

```java
// 컨테이너
TextMarks containerStart(String value)
TextMarks containerEnd(String value)

// 선
TextMarks lineStart(String value)
TextMarks lineEnd(String value)

// 사각형
TextMarks rectangleStart(String value)
TextMarks rectangleEnd(String value)

// 타원
TextMarks ellipseStart(String value)
TextMarks ellipseEnd(String value)

// 호
TextMarks arcStart(String value)
TextMarks arcEnd(String value)

// 다각형
TextMarks polygonStart(String value)
TextMarks polygonEnd(String value)

// 곡선
TextMarks curveStart(String value)
TextMarks curveEnd(String value)

// 연결선
TextMarks connectLineStart(String value)
TextMarks connectLineEnd(String value)

// 텍스트 아트
TextMarks textArtStart(String value)
TextMarks textArtEnd(String value)
```

---

## 4. 객체 검색

### ObjectFinder
**패키지:** `kr.dogfoot.hwpxlib.tool.finder.ObjectFinder`

사용자 정의 필터를 사용하여 HWPX 문서 내의 객체를 찾습니다.

#### 주요 메서드

```java
static Result[] find(HWPXObject from, ObjectFilter objectFilter,
                    boolean findFirstOnly) throws Exception
```
- 필터 조건과 일치하는 객체를 찾습니다
- **매개변수:**
  - `from`: 시작 객체 (HWPXFile, Section 등)
  - `objectFilter`: `ObjectFilter` 인터페이스를 구현한 사용자 정의 필터
  - `findFirstOnly`: 첫 번째 일치 항목 발견 시 중지
- **반환:** 찾은 객체와 부모 경로를 포함하는 `Result` 객체 배열

#### ObjectFinder.Result 클래스

```java
HWPXObject thisObject()
```
- 찾은 객체

```java
ArrayList<HWPXObject> parentsPath()
```
- 루트에서 이 객체까지의 경로

#### ObjectFilter 인터페이스

```java
boolean isMatched(HWPXObject thisObject, ArrayList<HWPXObject> parentsPath)
```
- 사용자 정의 매칭 로직 구현

---

### FieldFinder
**패키지:** `kr.dogfoot.hwpxlib.tool.finder.FieldFinder`

문서 필드 전용 검색 도구입니다.

#### 주요 메서드

```java
static Result[] find(HWPXObject from, String fieldName, boolean findFirstOnly) throws Exception
```
- 이름으로 필드를 찾습니다
- **반환:** 시작/종료 마커가 있는 필드 결과 배열

#### FieldFinder.Result 클래스

```java
ParaListCore paraList()      // 컨테이너 문단 리스트
Para beginPara()             // 필드 시작을 포함하는 문단
Run beginRun()               // 필드 시작을 포함하는 Run
Ctrl beginCtrl()             // 필드 시작을 포함하는 Control
FieldBegin beginField()      // 필드 시작 객체
Para endPara()               // 필드 종료를 포함하는 문단
Run endRun()                 // 필드 종료를 포함하는 Run
Ctrl endCtrl()               // 필드 종료를 포함하는 Control
FieldEnd endField()          // 필드 종료 객체
```

---

## 5. 문서 생성

### BlankFileMaker
**패키지:** `kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker`

빈/템플릿 HWPX 문서를 생성합니다.

#### 주요 메서드

```java
static HWPXFile make()
```
- 최소한의 유효한 HWPX 문서를 생성합니다
- **반환:** 필수 구조를 갖춘 빈 HWPXFile
- 사전 구성: 설정, 버전 정보, 컨테이너, 콘텐츠 구조, 헤더, 1개의 섹션

---

## 6. 문서 내용 조작

### SectionXMLFile
**패키지:** `kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile`

문서 섹션을 나타냅니다 (`ParaListCore`를 상속).

#### ParaListCore에서 상속된 메서드

```java
int countOfPara()                           // 문단 수 가져오기
Para getPara(int index)                     // 인덱스로 문단 가져오기
int getParaIndex(Para para)                 // 문단의 인덱스 가져오기
void addPara(Para para)                     // 문단 추가
Para addNewPara()                           // 새로운 빈 문단 추가
void insertPara(Para para, int position)    // 위치에 삽입
void removePara(int position)               // 인덱스로 제거
void removePara(Para para)                  // 특정 문단 제거
void removeAllParas()                       // 모든 문단 제거
Iterable<Para> paras()                      // 문단 반복
SectionXMLFile clone()                      // 섹션 복제
void copyFrom(SectionXMLFile from)          // 다른 섹션에서 복사
```

---

### Para
**패키지:** `kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para`

문단을 나타냅니다.

#### 속성 메서드

```java
String id() / void id(String) / Para idAnd(String)
```
- 문단 ID

```java
String paraPrIDRef() / void paraPrIDRef(String) / Para paraPrIDRefAnd(String)
```
- 문단 스타일 참조

```java
String styleIDRef() / void styleIDRef(String) / Para styleIDRefAnd(String)
```
- 스타일 참조

```java
Boolean pageBreak() / void pageBreak(Boolean) / Para pageBreakAnd(Boolean)
```
- 페이지 나누기 플래그

```java
Boolean columnBreak() / void columnBreak(Boolean) / Para columnBreakAnd(Boolean)
```
- 단 나누기 플래그

```java
Boolean merged() / void merged(Boolean) / Para mergedAnd(Boolean)
```
- 병합된 문단 플래그

```java
String paraTcId() / void paraTcId(String) / Para paraTcIdAnd(String)
```
- 변경 내용 추적 ID

#### Run 관리 메서드

```java
int countOfRun()                        // Run 수 가져오기
Run getRun(int index)                   // 인덱스로 Run 가져오기
int getRunIndex(Run run)                // Run의 인덱스 가져오기
void addRun(Run run)                    // Run 추가
Run addNewRun()                         // 새로운 빈 Run 추가
void insertRun(Run run, int position)   // 위치에 삽입
void removeRun(int position)            // 인덱스로 제거
void removeRun(Run run)                 // 특정 Run 제거
void removeAllRuns()                    // 모든 Run 제거
Iterable<Run> runs()                    // Run 반복
```

#### 선 세그먼트 메서드

```java
ObjectList<LineSeg> lineSegArray()      // 선 세그먼트 가져오기
void createLineSegArray()               // 선 세그먼트 배열 생성
void removeLineSegArray()               // 선 세그먼트 제거
```

#### 유틸리티 메서드

```java
Para clone()                            // 문단 복제
void copyFrom(Para from)                // 다른 문단에서 복사
```

---

### Run
**패키지:** `kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run`

Run (동일한 서식의 텍스트)을 나타냅니다.

#### 속성 메서드

```java
String charPrIDRef() / void charPrIDRef(String) / Run charPrIDRefAnd(String)
```
- 문자 스타일 참조

```java
String charTcId() / void charTcId(String) / Run charTcIdAnd(String)
```
- 변경 내용 추적 ID

```java
SecPr secPr() / void createSecPr() / void removeSecPr()
```
- 섹션 속성

#### RunItem 관리

```java
int countOfRunItem()                            // 항목 수 가져오기
RunItem getRunItem(int index)                   // 인덱스로 항목 가져오기
int getRunItemIndex(RunItem runItem)            // 항목의 인덱스 가져오기
void addRunItem(RunItem runItem)                // 항목 추가
void insertRunItem(RunItem runItem, int pos)    // 위치에 삽입
void removeRunItem(int position)                // 인덱스로 제거
void removeRunItem(RunItem runItem)             // 특정 항목 제거
void removeAllRunItems()                        // 모든 항목 제거
Iterable<RunItem> runItems()                    // 항목 반복
```

#### 특정 RunItem 유형 추가

```java
Ctrl addNewCtrl()               // 컨트롤 문자 추가
T addNewT()                     // 텍스트 추가
Table addNewTable()             // 표 추가
Picture addNewPicture()         // 그림 추가
Container addNewContainer()     // 컨테이너 추가
OLE addNewOLE()                 // OLE 객체 추가
Equation addNewEquation()       // 수식 추가
Line addNewLine()               // 선 도형 추가
Rectangle addNewRectangle()     // 사각형 도형 추가
Ellipse addNewEllipse()         // 타원 도형 추가
Arc addNewArc()                 // 호 도형 추가
Polygon addNewPolygon()         // 다각형 도형 추가
Curve addNewCurve()             // 곡선 도형 추가
ConnectLine addNewConnectLine() // 연결선 추가
TextArt addNewTextArt()         // 텍스트 아트 추가
Compose addNewCompose()         // 겹친 문자 추가
Dutmal addNewDutmal()           // 한글 두음절 추가
Button addNewButton()           // 폼 버튼 추가
RadioButton addNewRadioButton() // 라디오 버튼 추가
CheckButton addNewCheckButton() // 체크박스 추가
ComboBox addNewComboBox()       // 콤보 박스 추가
ListBox addNewListBox()         // 리스트 박스 추가
Edit addNewEdit()               // 편집 컨트롤 추가
ScrollBar addNewScrollBar()     // 스크롤바 추가
Video addNewVideo()             // 비디오 추가
Chart addNewChart()             // 차트 추가
```

#### 유틸리티 메서드

```java
Run clone()                     // Run 복제
void copyFrom(Run from)         // 다른 Run에서 복사
```

---

### T (Text)
**패키지:** `kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T`

텍스트 콘텐츠를 나타냅니다.

#### 속성 메서드

```java
String charPrIDRef() / void charPrIDRef(String) / T charPrIDRefAnd(String)
```
- 문자 스타일 참조

#### 텍스트 관리

```java
void addText(String text)       // 텍스트 추가
void clear()                    // 모든 텍스트 제거
boolean isEmpty()               // 비어 있는지 확인
boolean isOnlyText()            // 단순 텍스트만 포함하는지 확인 (마크업 없음)
String onlyText()               // 단순 텍스트 가져오기 (마크업이 없을 때)
```

#### TItem 관리 (마크업이 있는 복잡한 텍스트용)

```java
int countOfItems()                          // 항목 수 가져오기
TItem getItem(int index)                    // 인덱스로 항목 가져오기
int getItemIndex(TItem textItem)            // 항목의 인덱스 가져오기
void addItem(TItem textItem)                // 텍스트 항목 추가
void insertItem(TItem textItem, int pos)    // 위치에 삽입
void removeItem(int position)               // 인덱스로 제거
void removeItem(TItem textItem)             // 특정 항목 제거
void removeAllItems()                       // 모든 항목 제거
Iterable<TItem> items()                     // 항목 반복
```

#### 특정 텍스트 항목 추가

```java
NormalText addNewText()             // 일반 텍스트 추가
MarkpenBegin addNewMarkpenBegin()   // 하이라이트 시작 추가
MarkpenEnd addNewMarkpenEnd()       // 하이라이트 종료 추가
TitleMark addNewTitleMark()         // 제목 표시 추가
Tab addNewTab()                     // 탭 추가
LineBreak addNewLineBreak()         // 줄바꿈 추가
Hyphen addNewHyphen()               // 하이픈 추가
NBSpace addNewNBSpace()             // 줄바꿈 없는 공백 추가
FWSpace addNewFWSpace()             // 전각 공백 추가
InsertBegin addNewInsertBegin()     // 변경 내용 추적 삽입 시작 추가
InsertEnd addNewInsertEnd()         // 변경 내용 추적 삽입 종료 추가
DeleteBegin addNewDeleteBegin()     // 변경 내용 추적 삭제 시작 추가
DeleteEnd addNewDeleteEnd()         // 변경 내용 추적 삭제 종료 추가
```

#### 유틸리티 메서드

```java
T clone()                           // 텍스트 객체 복제
void copyFrom(T from)               // 다른 텍스트 객체에서 복사
```

---

## 7. 문서 헤더 및 스타일

### HeaderXMLFile
**패키지:** `kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile`

스타일, 폰트 및 속성을 포함하는 문서 헤더입니다.

#### 속성 메서드

```java
String version() / void version(String) / HeaderXMLFile versionAnd(String)
```
- 헤더 버전

```java
Short secCnt() / void secCnt(Short) / HeaderXMLFile secCntAnd(Short)
```
- 섹션 수

#### 구성 요소 접근/생성

```java
BeginNum beginNum() / void createBeginNum() / void removeBeginNum()
```
- 시작 번호 매기기

```java
RefList refList() / void createRefList() / void removeRefList()
```
- 참조 목록 (스타일, 폰트 등)

```java
ObjectList<ForbiddenWord> forbiddenWordList()
  / void createForbiddenWordList() / void removeForbiddenWordList()
```
- 금칙어

```java
CompatibleDocument compatibleDocument()
  / void createCompatibleDocument() / void removeCompatibleDocument()
```
- 호환성 설정

```java
DocOption docOption() / void createDocOption() / void removeDocOption()
```
- 문서 옵션

```java
HasOnlyText metaTag() / void createMetaTag() / void removeMetaTag()
```
- 메타데이터 태그

```java
TrackChangeConfig trackChangeConfig()
  / void createTrackChangeConfig() / void removeTrackChangeConfig()
```
- 변경 내용 추적 구성

#### 유틸리티 메서드

```java
HeaderXMLFile clone()               // 헤더 복제
void copyFrom(HeaderXMLFile from)   // 다른 헤더에서 복사
```

---

### RefList
**패키지:** `kr.dogfoot.hwpxlib.object.content.header_xml.RefList`

문서 리소스를 포함하는 참조 목록입니다.

#### 리소스 접근/생성

```java
Fontfaces fontfaces() / void createFontfaces() / void removeFontfaces()
```
- 폰트 정의

```java
ObjectList<BorderFill> borderFills()
  / void createBorderFills() / void removeBorderFills()
```
- 테두리/채우기 스타일

```java
ObjectList<CharPr> charProperties()
  / void createCharProperties() / void removeCharProperties()
```
- 문자 속성/스타일

```java
ObjectList<TabPr> tabProperties()
  / void createTabProperties() / void removeTabProperties()
```
- 탭 속성

```java
ObjectList<Numbering> numberings()
  / void createNumberings() / void removeNumberings()
```
- 번호 매기기 정의

```java
ObjectList<Bullet> bullets()
  / void createBullets() / void removeBullets()
```
- 글머리 기호 스타일

```java
ObjectList<ParaPr> paraProperties()
  / void createParaProperties() / void removeParaProperties()
```
- 문단 속성/스타일

```java
ObjectList<Style> styles()
  / void createStyles() / void removeStyles()
```
- 명명된 스타일

```java
ObjectList<MemoPr> memoProperties()
  / void createMemoProperties() / void removeMemoProperties()
```
- 메모/주석 속성

```java
ObjectList<TrackChange> trackChanges()
  / void createTrackChanges() / void removeTrackChanges()
```
- 변경 내용 추적 레코드

```java
ObjectList<TrackChangeAuthor> trackChangeAuthors()
  / void createTrackChangeAuthors() / void removeTrackChangeAuthors()
```
- 변경 내용 추적 작성자

#### 유틸리티 메서드

```java
RefList clone()                 // 참조 목록 복제
void copyFrom(RefList from)     // 다른 참조 목록에서 복사
```

---

## 8. 문서 설정

### SettingsXMLFile
**패키지:** `kr.dogfoot.hwpxlib.object.root.SettingsXMLFile`

문서 설정 및 구성입니다.

#### 구성 요소 접근/생성

```java
CaretPosition caretPosition()
  / void createCaretPosition() / void removeCaretPosition()
```
- 캐럿/커서 위치

```java
ConfigItemSet configItemSet()
  / void createConfigItemSet() / void removeConfigItemSet()
```
- 구성 항목

#### 유틸리티 메서드

```java
SettingsXMLFile clone()             // 설정 복제
void copyFrom(SettingsXMLFile from) // 다른 설정에서 복사
```

---

## 9. 유틸리티 클래스

### ObjectList&lt;T&gt;
**패키지:** `kr.dogfoot.hwpxlib.object.common.ObjectList`

HWPX 객체용 제네릭 리스트입니다.

#### 메서드

```java
int size()                      // 개수 가져오기
T get(int index)                // 인덱스로 가져오기
T addNew()                      // 새 항목 추가
void add(T item)                // 기존 항목 추가
void remove(int index)          // 인덱스로 제거
void remove(T item)             // 특정 항목 제거
void clear()                    // 모두 제거
Iterable<T> items()             // 항목 반복
```

---

## 10. 열거형 및 상수

### TextExtractMethod
**패키지:** `kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod`

```java
InsertControlTextBetweenParagraphText   // 컨트롤 텍스트를 인라인으로 삽입
AppendControlTextAfterParagraphText     // 컨트롤 텍스트를 문단 뒤에 추가
```

### ObjectType
객체 유형 식별에 사용됩니다.

주요 유형:
- `HWPXFile` - 루트 문서
- `hs_sec` - 섹션
- `hp_p` - 문단
- `hp_run` - Run
- `hp_t` - 텍스트
- `hp_ctrl` - 컨트롤
- `hp_fieldBegin` - 필드 시작
- `hp_fieldEnd` - 필드 종료
- `hp_tbl` - 표
- `hh_head` - 헤더
- 그 외 다양한 문서 요소들

---

## 기능 요약

### 읽기/쓰기
- 파일/스트림에서 HWPX 읽기 (네임스페이스 제어 옵션 포함)
- 파일/스트림/바이트로 HWPX 쓰기
- 전체 문서 깊은 복사

### 텍스트 추출
- 문서에서 모든 텍스트 추출
- 특정 섹션/부분에서 텍스트 추출
- 특정 범위에서 텍스트 추출
- 서식 보존을 위한 구성 가능한 텍스트 마커

### 객체 검색
- 사용자 정의 필터를 사용한 제네릭 객체 검색기
- 이름으로 검색하는 전용 필드 검색기
- 전체 부모 경로와 함께 객체 반환

### 문서 생성
- 유효한 구조를 가진 빈 문서 생성
- 사전 구성된 템플릿

### 내용 조작
- 섹션 추가/제거/수정
- 문단 추가/제거/수정
- Run 추가/제거/수정
- 텍스트 추가/제거/수정
- 모든 한글 문서 객체 지원 (표, 그림, 도형, 폼 등)

### 스타일 및 서식
- 문자 속성 접근/수정
- 문단 속성 접근/수정
- 폰트 접근/수정
- 테두리 및 채우기 접근/수정
- 스타일 접근/수정
- 번호 매기기 및 글머리 기호 접근/수정

### 고급 기능
- 변경 내용 추적 지원
- 필드 지원 (병합 필드, 책갈피)
- 마스터 페이지 (머리글/바닥글)
- 차트 임베딩
- OLE 객체 임베딩
- 폼 컨트롤 (버튼, 체크박스, 콤보 박스 등)
- 비디오 임베딩
- 그리기 객체 (선, 사각형, 타원, 다각형, 곡선 등)

---

## 실전 예제 모음

### 예제 1: 빈 문서 생성하고 텍스트 추가

```java
import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;

public class CreateNewDocument {
    public static void main(String[] args) throws Exception {
        // 1. 빈 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();

        // 2. 첫 번째 섹션 가져오기
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 3. 새 문단 추가 (제목)
        Para titlePara = section.addNewPara();
        Run titleRun = titlePara.addNewRun();
        T titleText = titleRun.addNewT();
        titleText.addText("한글 문서 제목");

        // 4. 새 문단 추가 (본문)
        Para bodyPara = section.addNewPara();
        Run bodyRun = bodyPara.addNewRun();
        T bodyText = bodyRun.addNewT();
        bodyText.addText("이것은 본문 내용입니다.");

        // 5. 여러 줄 추가
        for (int i = 1; i <= 3; i++) {
            Para para = section.addNewPara();
            Run run = para.addNewRun();
            T text = run.addNewT();
            text.addText("문단 " + i + "의 내용입니다.");
        }

        // 6. 문서 저장
        HWPXWriter.toFilepath(hwpxFile, "new_document.hwpx");
        System.out.println("문서가 생성되었습니다: new_document.hwpx");
    }
}
```

### 예제 2: 기존 문서 읽고 텍스트 추출

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

public class ExtractText {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

        // 2. 텍스트 마커 설정
        TextMarks marks = new TextMarks();
        marks.paraSeparator("\n\n")      // 문단 사이에 빈 줄
             .lineBreak("\n")             // 줄바꿈
             .tab("\t")                   // 탭
             .tableStart("[표 시작]\n")   // 표 시작 마커
             .tableEnd("[표 끝]\n")       // 표 종료 마커
             .tableRowSeparator("\n")     // 표 행 구분
             .tableCellSeparator(" | ");  // 표 셀 구분

        // 3. 전체 텍스트 추출
        String extractedText = TextExtractor.extract(
            hwpxFile,
            TextExtractMethod.InsertControlTextBetweenParagraphText,
            true,  // 문단 헤더 포함
            marks
        );

        // 4. 결과 출력
        System.out.println("=== 추출된 텍스트 ===");
        System.out.println(extractedText);
    }
}
```

### 예제 3: 특정 텍스트 찾아서 바꾸기

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;

public class FindAndReplace {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

        String searchText = "old_text";
        String replaceText = "new_text";
        int replaceCount = 0;

        // 2. 모든 섹션 순회
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            // 3. 모든 문단 순회
            for (Para para : section.paras()) {
                // 4. 모든 Run 순회
                for (Run run : para.runs()) {
                    // 5. 모든 RunItem 순회
                    for (int i = 0; i < run.countOfRunItem(); i++) {
                        if (run.getRunItem(i) instanceof T) {
                            T textItem = (T) run.getRunItem(i);

                            // 6. 텍스트 확인 및 바꾸기
                            if (textItem.isOnlyText()) {
                                String text = textItem.onlyText();
                                if (text.contains(searchText)) {
                                    String newText = text.replace(searchText, replaceText);
                                    textItem.clear();
                                    textItem.addText(newText);
                                    replaceCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. 문서 저장
        HWPXWriter.toFilepath(hwpxFile, "output.hwpx");
        System.out.println(replaceCount + "개의 텍스트를 바꿨습니다.");
    }
}
```

### 예제 4: 필드(Field) 찾아서 값 바꾸기

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.tool.finder.FieldFinder;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;

public class ReplaceFieldValue {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("template.hwpx");

        // 2. "이름" 필드 찾기
        FieldFinder.Result[] results = FieldFinder.find(hwpxFile, "이름", false);

        // 3. 찾은 필드의 값 바꾸기
        for (FieldFinder.Result result : results) {
            Para beginPara = result.beginPara();
            Run beginRun = result.beginRun();

            // 필드 시작 다음의 텍스트를 변경
            // (실제로는 필드 begin과 end 사이의 Run에서 텍스트를 찾아 변경)
            boolean foundField = false;
            for (Run run : beginPara.runs()) {
                if (foundField) {
                    // 필드 내용 찾음
                    for (int i = 0; i < run.countOfRunItem(); i++) {
                        if (run.getRunItem(i) instanceof T) {
                            T textItem = (T) run.getRunItem(i);
                            textItem.clear();
                            textItem.addText("홍길동");  // 새 값으로 변경
                            break;
                        }
                    }
                    break;
                }
                if (run == beginRun) {
                    foundField = true;
                }
            }
        }

        // 4. 문서 저장
        HWPXWriter.toFilepath(hwpxFile, "filled_template.hwpx");
        System.out.println(results.length + "개의 필드를 처리했습니다.");
    }
}
```

### 예제 5: 표(Table) 추가하기

```java
import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.object.table.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.section_xml.object.table.Tc;

public class CreateTable {
    public static void main(String[] args) throws Exception {
        // 1. 빈 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 2. 제목 추가
        Para titlePara = section.addNewPara();
        Run titleRun = titlePara.addNewRun();
        titleRun.addNewT().addText("학생 명단");

        // 3. 표 추가를 위한 문단과 Run 생성
        Para tablePara = section.addNewPara();
        Run tableRun = tablePara.addNewRun();

        // 4. 표 생성 (3행 3열)
        Table table = tableRun.addNewTable();

        // 헤더 행
        Tr headerRow = table.addNewTr();
        for (String header : new String[]{"번호", "이름", "학년"}) {
            Tc cell = headerRow.addNewTc();
            Para cellPara = cell.addNewPara();
            Run cellRun = cellPara.addNewRun();
            cellRun.addNewT().addText(header);
        }

        // 데이터 행 추가
        String[][] data = {
            {"1", "홍길동", "3학년"},
            {"2", "김철수", "2학년"}
        };

        for (String[] row : data) {
            Tr dataRow = table.addNewTr();
            for (String cellData : row) {
                Tc cell = dataRow.addNewTc();
                Para cellPara = cell.addNewPara();
                Run cellRun = cellPara.addNewRun();
                cellRun.addNewT().addText(cellData);
            }
        }

        // 5. 문서 저장
        HWPXWriter.toFilepath(hwpxFile, "table_document.hwpx");
        System.out.println("표가 포함된 문서가 생성되었습니다.");
    }
}
```

### 예제 6: ObjectFinder를 사용한 커스텀 객체 검색

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.tool.finder.ObjectFinder;
import kr.dogfoot.hwpxlib.tool.finder.ObjectFilter;
import kr.dogfoot.hwpxlib.object.common.HWPXObject;
import kr.dogfoot.hwpxlib.object.content.section_xml.object.table.Table;

import java.util.ArrayList;

public class FindTables {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

        // 2. 표 찾기 필터 정의
        ObjectFilter tableFilter = new ObjectFilter() {
            @Override
            public boolean isMatched(HWPXObject thisObject,
                                   ArrayList<HWPXObject> parentsPath) {
                return thisObject instanceof Table;
            }
        };

        // 3. 모든 표 찾기
        ObjectFinder.Result[] results = ObjectFinder.find(
            hwpxFile,
            tableFilter,
            false  // 모든 표 찾기
        );

        // 4. 결과 출력
        System.out.println("찾은 표의 개수: " + results.length);
        for (int i = 0; i < results.length; i++) {
            Table table = (Table) results[i].thisObject();
            System.out.println("표 " + (i+1) + ": " +
                             table.countOfTr() + "행");
        }
    }
}
```

### 예제 7: 문서 복제 및 병합

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;

public class MergeDocuments {
    public static void main(String[] args) throws Exception {
        // 1. 첫 번째 문서 읽기
        HWPXFile doc1 = HWPXReader.fromFilepath("document1.hwpx");

        // 2. 두 번째 문서 읽기
        HWPXFile doc2 = HWPXReader.fromFilepath("document2.hwpx");

        // 3. doc1의 첫 번째 섹션 가져오기
        SectionXMLFile section1 = doc1.sectionXMLFileList().get(0);

        // 4. doc2의 모든 문단을 doc1에 추가
        SectionXMLFile section2 = doc2.sectionXMLFileList().get(0);
        for (Para para : section2.paras()) {
            // 문단 복제 후 추가
            Para clonedPara = para.clone();
            section1.addPara(clonedPara);
        }

        // 5. 병합된 문서 저장
        HWPXWriter.toFilepath(doc1, "merged_document.hwpx");
        System.out.println("문서가 병합되었습니다.");
    }
}
```

### 예제 8: 문단 스타일 및 서식 적용

```java
import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;

public class ApplyFormatting {
    public static void main(String[] args) throws Exception {
        // 1. 빈 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 2. 문단 추가 및 스타일 참조 설정
        Para para1 = section.addNewPara();
        para1.styleIDRef("0");  // 기본 스타일 참조
        Run run1 = para1.addNewRun();
        run1.charPrIDRef("0");  // 기본 문자 속성 참조
        run1.addNewT().addText("기본 스타일 텍스트");

        // 3. 페이지 나누기가 있는 문단
        Para para2 = section.addNewPara();
        para2.pageBreakAnd(true);  // Fluent API 사용
        Run run2 = para2.addNewRun();
        run2.addNewT().addText("이 문단은 새 페이지에서 시작합니다.");

        // 4. 문서 저장
        HWPXWriter.toFilepath(hwpxFile, "formatted_document.hwpx");
        System.out.println("서식이 적용된 문서가 생성되었습니다.");
    }
}
```

### 예제 9: 섹션별 텍스트 추출

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

public class ExtractTextBySection {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

        // 2. 텍스트 마커 설정
        TextMarks marks = new TextMarks();
        marks.paraSeparator("\n");

        // 3. 각 섹션별로 텍스트 추출
        int sectionNum = 1;
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            String sectionText = TextExtractor.extractFrom(
                section,
                TextExtractMethod.InsertControlTextBetweenParagraphText,
                marks
            );

            System.out.println("=== 섹션 " + sectionNum + " ===");
            System.out.println(sectionText);
            System.out.println();
            sectionNum++;
        }
    }
}
```

### 예제 10: 바이트 배열로 변환 (메모리 처리)

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ByteArrayConversion {
    public static void main(String[] args) throws Exception {
        // 1. 문서 읽기
        HWPXFile hwpxFile = HWPXReader.fromFilepath("input.hwpx");

        // 2. 바이트 배열로 변환
        byte[] hwpxBytes = HWPXWriter.toBytes(hwpxFile);
        System.out.println("문서 크기: " + hwpxBytes.length + " bytes");

        // 3. 바이트 배열을 스트림으로 출력 (예: 네트워크 전송)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HWPXWriter.toStream(hwpxFile, outputStream);
        byte[] streamBytes = outputStream.toByteArray();

        System.out.println("스트림 크기: " + streamBytes.length + " bytes");
    }
}
```

---

## 일반적인 패턴 및 베스트 프랙티스

### 패턴 1: Fluent API 활용

hwpxlib는 fluent API 패턴을 지원합니다. `xxxAnd()` 메서드를 사용하여 메서드 체이닝이 가능합니다.

```java
// 일반 방식
Para para = section.addNewPara();
para.id("para1");
para.pageBreak(true);
para.styleIDRef("0");

// Fluent API 방식
Para para = section.addNewPara()
    .idAnd("para1")
    .pageBreakAnd(true)
    .styleIDRefAnd("0");
```

### 패턴 2: 안전한 null 체크

일부 객체는 생성되지 않을 수 있으므로 null 체크가 필요합니다.

```java
HeaderXMLFile header = hwpxFile.headerXMLFile();
if (header != null) {
    RefList refList = header.refList();
    if (refList != null) {
        // RefList 사용
    } else {
        header.createRefList();
        refList = header.refList();
    }
}
```

### 패턴 3: Iterator 활용

ObjectList와 Para, Run 등은 Iterable을 구현하므로 for-each 루프를 사용할 수 있습니다.

```java
// ObjectList 순회
for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
    // 섹션 처리
}

// Para 순회
for (Para para : section.paras()) {
    // 문단 처리
}

// Run 순회
for (Run run : para.runs()) {
    // Run 처리
}
```

### 패턴 4: instanceof를 사용한 타입 확인

RunItem은 다양한 타입(T, Table, Picture 등)이 될 수 있으므로 타입 확인이 필요합니다.

```java
for (int i = 0; i < run.countOfRunItem(); i++) {
    RunItem item = run.getRunItem(i);

    if (item instanceof T) {
        T text = (T) item;
        // 텍스트 처리
    } else if (item instanceof Table) {
        Table table = (Table) item;
        // 표 처리
    } else if (item instanceof Picture) {
        Picture picture = (Picture) item;
        // 그림 처리
    }
}
```

### 패턴 5: 예외 처리

hwpxlib의 많은 메서드가 Exception을 던지므로 적절한 예외 처리가 필요합니다.

```java
public class SafeHwpxOperation {
    public void processDocument(String filepath) {
        HWPXFile hwpxFile = null;
        try {
            hwpxFile = HWPXReader.fromFilepath(filepath);

            // 문서 처리

            HWPXWriter.toFilepath(hwpxFile, "output.hwpx");

        } catch (Exception e) {
            System.err.println("문서 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 패턴 6: 리소스 정리

파일 처리 시 적절한 리소스 정리를 위해 try-with-resources 또는 finally 블록을 사용합니다.

```java
public void processWithStream(String inputPath, String outputPath) {
    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
        HWPXFile hwpxFile = HWPXReader.fromFilepath(inputPath);

        // 처리

        HWPXWriter.toStream(hwpxFile, fos);

    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### 패턴 7: 문서 구조 순회 헬퍼 메서드

복잡한 문서 구조 순회를 위한 헬퍼 메서드를 작성합니다.

```java
public class HwpxHelper {

    // 모든 텍스트 객체(T) 찾기
    public static List<T> findAllTextObjects(HWPXFile hwpxFile) {
        List<T> textList = new ArrayList<>();

        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            for (Para para : section.paras()) {
                for (Run run : para.runs()) {
                    for (int i = 0; i < run.countOfRunItem(); i++) {
                        if (run.getRunItem(i) instanceof T) {
                            textList.add((T) run.getRunItem(i));
                        }
                    }
                }
            }
        }

        return textList;
    }

    // 특정 텍스트를 포함하는 문단 찾기
    public static List<Para> findParagraphsContaining(
            SectionXMLFile section, String searchText) {
        List<Para> matchingParas = new ArrayList<>();

        for (Para para : section.paras()) {
            for (Run run : para.runs()) {
                for (int i = 0; i < run.countOfRunItem(); i++) {
                    if (run.getRunItem(i) instanceof T) {
                        T text = (T) run.getRunItem(i);
                        if (text.isOnlyText() &&
                            text.onlyText().contains(searchText)) {
                            matchingParas.add(para);
                            break;
                        }
                    }
                }
            }
        }

        return matchingParas;
    }

    // 문단 개수 세기
    public static int getTotalParagraphCount(HWPXFile hwpxFile) {
        int count = 0;
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            count += section.countOfPara();
        }
        return count;
    }
}
```

### 패턴 8: 템플릿 기반 문서 생성

템플릿 문서를 읽고 특정 플레이스홀더를 바꾸는 패턴입니다.

```java
public class TemplateProcessor {

    public static HWPXFile fillTemplate(
            String templatePath,
            Map<String, String> replacements) throws Exception {

        // 1. 템플릿 읽기
        HWPXFile template = HWPXReader.fromFilepath(templatePath);

        // 2. 플레이스홀더 바꾸기
        for (SectionXMLFile section : template.sectionXMLFileList().items()) {
            for (Para para : section.paras()) {
                for (Run run : para.runs()) {
                    for (int i = 0; i < run.countOfRunItem(); i++) {
                        if (run.getRunItem(i) instanceof T) {
                            T text = (T) run.getRunItem(i);

                            if (text.isOnlyText()) {
                                String content = text.onlyText();

                                // 모든 플레이스홀더 바꾸기
                                for (Map.Entry<String, String> entry :
                                        replacements.entrySet()) {
                                    content = content.replace(
                                        "{{" + entry.getKey() + "}}",
                                        entry.getValue()
                                    );
                                }

                                text.clear();
                                text.addText(content);
                            }
                        }
                    }
                }
            }
        }

        return template;
    }

    public static void main(String[] args) throws Exception {
        // 사용 예시
        Map<String, String> data = new HashMap<>();
        data.put("name", "홍길동");
        data.put("date", "2024-01-01");
        data.put("amount", "100,000원");

        HWPXFile filled = fillTemplate("invoice_template.hwpx", data);
        HWPXWriter.toFilepath(filled, "invoice_output.hwpx");
    }
}
```

### 패턴 9: 문서 유효성 검사

```java
public class DocumentValidator {

    public static boolean isValidDocument(HWPXFile hwpxFile) {
        if (hwpxFile == null) return false;

        // 필수 구성 요소 확인
        if (hwpxFile.sectionXMLFileList() == null ||
            hwpxFile.sectionXMLFileList().size() == 0) {
            return false;
        }

        if (hwpxFile.headerXMLFile() == null) {
            return false;
        }

        return true;
    }

    public static List<String> validateAndGetIssues(HWPXFile hwpxFile) {
        List<String> issues = new ArrayList<>();

        if (hwpxFile == null) {
            issues.add("문서가 null입니다.");
            return issues;
        }

        if (hwpxFile.sectionXMLFileList().size() == 0) {
            issues.add("섹션이 없습니다.");
        }

        // 빈 문단 확인
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            if (section.countOfPara() == 0) {
                issues.add("빈 섹션이 있습니다.");
            }
        }

        return issues;
    }
}
```

---

## 테이블 생성 시 주의사항 및 시행착오 정리

테이블 생성은 hwpxlib에서 가장 복잡한 작업 중 하나입니다. 다음 내용은 실제 시행착오를 통해 얻은 교훈입니다.

### 문제 1: BlankFileMaker로 생성 시 테이블이 표시되지 않음

**증상:**
```java
HWPXFile hwpxFile = BlankFileMaker.make();
// ... 테이블 생성
// 파일은 만들어지지만 한글 프로그램에서 열리지 않거나 테이블이 보이지 않음
```

**원인:**
- 테이블 속성이 불완전함 (sz, pos 등)
- 셀의 필수 속성 누락 (cellAddr, cellSpan, cellSz 등)
- 타입 불일치 (int vs Long)

### 문제 2: 테이블은 보이지만 테두리가 투명함

**증상:**
- 테이블 구조는 있지만 선이 보이지 않음
- borderFillIDRef가 설정되어 있어도 효과 없음

**원인:**
- header.xml에 borderFill 스타일 정의가 없거나 불완전함
- borderFillIDRef가 존재하지 않는 ID를 참조

### 해결 방법: 기존 파일 복제 방식 (권장)

테이블을 처음부터 만드는 대신, **작동하는 테이블 파일을 복제하고 수정**하는 방식이 가장 안전합니다.

```java
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.RunItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;

public class CreateTableCorrectly {
    public static void main(String[] args) {
        try {
            // 1. 작동하는 테이블이 있는 파일 읽기
            HWPXFile sourceFile = HWPXReader.fromFilepath("working_table.hwpx");

            // 2. 전체 파일 복제 (헤더 정보도 함께 복사됨)
            HWPXFile newFile = sourceFile.clone();

            // 3. 섹션 가져오기
            SectionXMLFile section = newFile.sectionXMLFileList().get(0);
            section.removeAllParas();

            // 4. 원본 테이블 찾기
            SectionXMLFile sourceSection = sourceFile.sectionXMLFileList().get(0);
            Para sourcePara = sourceSection.getPara(0);
            Run sourceRun = sourcePara.getRun(0);

            Table sourceTable = null;
            for (int i = 0; i < sourceRun.countOfRunItem(); i++) {
                RunItem item = sourceRun.getRunItem(i);
                if (item instanceof Table) {
                    sourceTable = (Table) item;
                    break;
                }
            }

            // 5. 템플릿 셀 가져오기
            Tc templateCell = sourceTable.getTr(0).getTc(0);

            // 6. 새 테이블 생성 (원본 복제)
            Table newTable = sourceTable.clone();

            // 7. 테이블 크기 수정
            newTable.rowCnt((short) 4);
            newTable.colCnt((short) 4);

            // 기존 행 제거
            while (newTable.countOfTr() > 0) {
                newTable.removeTr(0);
            }

            // 8. 셀 크기 계산
            int cellWidth = 19000 / 4;
            int cellHeight = 8000 / 4;

            // 9. 새 행/셀 추가
            for (int row = 0; row < 4; row++) {
                Tr tr = newTable.addNewTr();

                for (int col = 0; col < 4; col++) {
                    // 템플릿 셀 복제 (테두리 설정 포함)
                    Tc cell = templateCell.clone();

                    // 셀 주소 수정
                    cell.cellAddr().colAddr((short) col);
                    cell.cellAddr().rowAddr((short) row);

                    // 셀 병합 정보 수정
                    cell.cellSpan().colSpan((short) 1);
                    cell.cellSpan().rowSpan((short) 1);

                    // 셀 크기 수정
                    cell.cellSz().width((long) cellWidth);
                    cell.cellSz().height((long) cellHeight);

                    // 셀 내용 비우기
                    if (cell.subList() != null) {
                        cell.subList().removeAllParas();
                        Para cellPara = cell.subList().addNewPara();
                        Run cellRun = cellPara.addNewRun();
                        cellRun.addNewT().addText("");
                    }

                    tr.addTc(cell);
                }
            }

            // 10. 문단에 테이블 추가
            Para newPara = section.addNewPara();
            Run newRun = newPara.addNewRun();
            newRun.addRunItem(newTable);

            // 11. 파일 저장
            HWPXWriter.toFilepath(newFile, "output_table.hwpx");

            System.out.println("테이블이 생성되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 핵심 포인트

1. **전체 파일 복제 사용**
   ```java
   HWPXFile newFile = sourceFile.clone();  // 헤더 정보도 함께 복사
   ```
   - BlankFileMaker 대신 작동하는 파일을 복제
   - header.xml의 borderFill, 스타일 정보가 자동으로 복사됨

2. **템플릿 셀 복제**
   ```java
   Tc templateCell = sourceTable.getTr(0).getTc(0);
   Tc cell = templateCell.clone();  // 모든 속성 복사
   ```
   - 셀의 모든 필수 속성이 자동으로 복사됨
   - borderFillIDRef, cellMargin 등이 유지됨

3. **create 메서드 사용 패턴**
   ```java
   // 잘못된 방법 (컴파일 에러)
   table.sz(new ShapeSize());  // sz는 getter 메서드

   // 올바른 방법
   table.createSZ();           // create 메서드로 객체 생성
   table.sz().width(19000L);   // getter로 가져와서 설정
   ```

4. **타입 주의**
   ```java
   // 잘못된 방법
   cell.cellSz().width(cellWidth);  // int는 안됨

   // 올바른 방법
   cell.cellSz().width((long) cellWidth);  // Long 타입 필요
   ```

5. **셀 필수 속성**
   - `cellAddr`: 셀 주소 (col, row)
   - `cellSpan`: 병합 정보 (colSpan, rowSpan)
   - `cellSz`: 셀 크기 (width, height)
   - `cellMargin`: 셀 여백
   - `subList`: 셀 내용 (문단 포함)

### 테이블 생성 체크리스트

테이블을 만들 때 다음을 확인하세요:

- [ ] 작동하는 테이블 파일을 기반으로 복제했는가?
- [ ] 테이블의 rowCnt, colCnt가 실제 Tr/Tc 개수와 일치하는가?
- [ ] 각 셀의 cellAddr가 올바르게 설정되었는가? (col, row)
- [ ] 각 셀의 cellSpan이 설정되었는가? (최소 1, 1)
- [ ] 각 셀의 cellSz가 설정되었는가?
- [ ] 각 셀의 subList에 최소 1개의 Para가 있는가?
- [ ] borderFillIDRef가 header.xml의 실제 ID를 참조하는가?

### 패키지 경로 주의사항

테이블 관련 클래스들의 정확한 경로:

```java
// Table은 paragraph.object에 있음
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;

// Tr, Tc는 paragraph.object.table에 있음
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;

// CellAddr, CellSpan 등도 같은 위치
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.CellAddr;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.CellSpan;

// ShapeSize, ShapePosition은 shapeobject에 있음
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapeSize;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapePosition;
```

---

## 라이센스

Apache-2.0 License

## 참고

- hwp 파일 라이브러리: https://github.com/neolord0/hwplib
- hwp → hwpx 변환: https://github.com/neolord0/hwp2hwpx
- 확장 라이브러리: https://github.com/neolord0/hwpxlib_ext