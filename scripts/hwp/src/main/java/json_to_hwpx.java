import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Equation;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.*;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapeSize;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapePosition;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DotsOCR로부터 추출한 JSON 파일을 HWPX 파일로 변환
 *
 * JSON 구조:
 * [
 *   {
 *     "bbox": [x1, y1, x2, y2],
 *     "category": "Text",
 *     "text": "1."
 *   },
 *   {
 *     "bbox": [x1, y1, x2, y2],
 *     "category": "Text",
 *     "text": "$\\sqrt{24} \\times 3^3$ 의 값은? [2점]"
 *   }
 * ]
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="json_to_hwpx" \
 *     -Dexec.args="<JSON_FILE_PATH> [OUTPUT_PATH]"
 *
 * 예시:
 *   mvn compile exec:java -Dexec.mainClass="json_to_hwpx" \
 *     -Dexec.args="../../dataset/downloads/suneung/수학영역_문제지/page_0001_0001.json"
 */
public class json_to_hwpx {

    /**
     * JSON 요소 클래스
     */
    static class JsonElement {
        int[] bbox;
        String category;
        String text;

        public int getY1() {
            return bbox != null && bbox.length >= 2 ? bbox[1] : 0;
        }

        public int getY2() {
            return bbox != null && bbox.length >= 4 ? bbox[3] : 0;
        }

        public int getX1() {
            return bbox != null && bbox.length >= 1 ? bbox[0] : 0;
        }

        public boolean isPicture() {
            return "Picture".equalsIgnoreCase(category);
        }
    }

    /**
     * 텍스트 구성 요소 (텍스트 또는 수식)
     */
    static class TextComponent {
        String type; // "text" or "equation"
        String content;
        boolean isInline; // 수식의 경우 inline ($...$) vs block ($$...$$)

        public TextComponent(String type, String content, boolean isInline) {
            this.type = type;
            this.content = content;
            this.isInline = isInline;
        }
    }

    /**
     * 텍스트를 파싱하여 일반 텍스트와 수식으로 분리
     */
    public static List<TextComponent> parseText(String text) {
        List<TextComponent> components = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return components;
        }

        // 블록 수식 패턴: $$...$$
        Pattern blockPattern = Pattern.compile("\\$\\$([^$]+?)\\$\\$");
        // 인라인 수식 패턴: $...$
        Pattern inlinePattern = Pattern.compile("\\$([^$]+?)\\$");

        // 전체 패턴 (블록 또는 인라인)
        Pattern combinedPattern = Pattern.compile("\\$\\$([^$]+?)\\$\\$|\\$([^$]+?)\\$");
        Matcher matcher = combinedPattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // 수식 앞의 일반 텍스트 추가
            if (matcher.start() > lastEnd) {
                String plainText = text.substring(lastEnd, matcher.start());
                if (!plainText.isEmpty()) {
                    components.add(new TextComponent("text", plainText, false));
                }
            }

            // 수식 추가
            if (matcher.group(1) != null) {
                // 블록 수식 ($$...$$)
                components.add(new TextComponent("equation", matcher.group(1), false));
            } else if (matcher.group(2) != null) {
                // 인라인 수식 ($...$)
                components.add(new TextComponent("equation", matcher.group(2), true));
            }

            lastEnd = matcher.end();
        }

        // 마지막 수식 이후의 일반 텍스트 추가
        if (lastEnd < text.length()) {
            String plainText = text.substring(lastEnd);
            if (!plainText.isEmpty()) {
                components.add(new TextComponent("text", plainText, false));
            }
        }

        // 수식이 없으면 전체를 일반 텍스트로 추가
        if (components.isEmpty() && !text.isEmpty()) {
            components.add(new TextComponent("text", text, false));
        }

        return components;
    }

    /**
     * Equation 객체 생성 (LaTeX -> HWP 변환 포함)
     */
    public static Equation createEquation(String latexText, boolean isInline) {
        String hwpScript = LatexToHwpTranslator.translate(latexText);
        return createEquationFromScript(hwpScript, isInline);
    }

    /**
     * HWP 수식 스크립트로 Equation 객체 생성
     */
    public static Equation createEquationFromScript(String hwpScript, boolean isInline) {
        Equation eq = new Equation();

        // 기본 속성 설정
        eq.versionAnd("Equation Version 60");
        eq.baseLineAnd(61);
        eq.textColorAnd("#000000");
        eq.baseUnitAnd(1100);
        eq.lineModeAnd(EquationLineMode.CHAR);
        eq.fontAnd("HYhwpEQ");

        // ID 생성
        eq.idAnd(String.valueOf(System.currentTimeMillis() + (int)(Math.random() * 1000)));
        eq.zOrderAnd(0);
        eq.numberingTypeAnd(NumberingType.EQUATION);
        eq.textWrapAnd(TextWrapMethod.TOP_AND_BOTTOM);
        eq.textFlowAnd(TextFlowSide.BOTH_SIDES);
        eq.lockAnd(false);
        eq.dropcapstyleAnd(DropCapStyle.None);

        // 크기 설정 (수식 내용에 따라 더 넉넉하게)
        eq.createSZ();
        ShapeSize sz = eq.sz();

        // 수식 길이에 따라 동적으로 크기 설정
        int scriptLength = hwpScript.length();
        long width;
        long height;

        if (isInline) {
            // 인라인 수식: 길이에 비례하여 너비 설정
            width = Math.max(2000L, Math.min(15000L, scriptLength * 150L));
            height = 1800L;
        } else {
            // 블록 수식: 더 넉넉하게
            width = Math.max(5000L, Math.min(20000L, scriptLength * 200L));
            height = 3000L;
        }

        sz.widthAnd(width);
        sz.heightAnd(height);
        sz.widthRelToAnd(WidthRelTo.ABSOLUTE);
        sz.heightRelToAnd(HeightRelTo.ABSOLUTE);
        sz.protectAnd(false);

        // 위치 설정 (텍스트처럼 취급하여 다른 텍스트가 침범하지 못하도록)
        eq.createPos();
        ShapePosition pos = eq.pos();
        pos.treatAsCharAnd(true);          // 글자처럼 취급
        pos.affectLSpacingAnd(true);        // 줄 간격에 영향을 줌
        pos.flowWithTextAnd(false);         // 텍스트와 함께 흐르지 않음 (공간 확보)
        pos.allowOverlapAnd(false);         // 겹침 허용 안 함
        pos.holdAnchorAndSOAnd(false);
        pos.vertRelToAnd(VertRelTo.PARA);
        pos.horzRelToAnd(HorzRelTo.PARA);
        pos.vertAlignAnd(VertAlign.BOTTOM); // 베이스라인 정렬
        pos.horzAlignAnd(HorzAlign.LEFT);
        pos.vertOffsetAnd(0L);
        pos.horzOffsetAnd(0L);

        // 여백 설정 (좌우 여백을 더 넉넉하게)
        eq.createOutMargin();
        eq.outMargin().leftAnd(100L);      // 왼쪽 여백 증가
        eq.outMargin().rightAnd(100L);     // 오른쪽 여백 증가
        eq.outMargin().topAnd(50L);
        eq.outMargin().bottomAnd(50L);

        // 수식 스크립트 설정
        eq.createScript();
        eq.script().addText(hwpScript);

        return eq;
    }

    /**
     * 텍스트 정리 (제어 문자 제거 등)
     */
    public static String cleanText(String text) {
        if (text == null) return "";

        // 제어 문자를 공백으로 대체
        String cleaned = text.replaceAll("[\\t\\r\\n]", " ");

        // 연속된 공백을 하나로
        cleaned = cleaned.replaceAll("\\s+", " ");

        // 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * 같은 줄에 있는 요소들을 그룹화
     * y 좌표가 겹치는 요소들을 같은 줄로 간주
     */
    public static List<List<JsonElement>> groupByLine(List<JsonElement> elements) {
        List<List<JsonElement>> lines = new ArrayList<>();

        for (JsonElement element : elements) {
            boolean added = false;

            // 기존 줄 중에서 y 좌표가 겹치는 줄 찾기
            for (List<JsonElement> line : lines) {
                if (!line.isEmpty()) {
                    JsonElement first = line.get(0);
                    // y 좌표가 겹치는지 확인 (허용 오차 20픽셀)
                    if (Math.abs(element.getY1() - first.getY1()) < 20 ||
                        (element.getY1() <= first.getY2() && element.getY2() >= first.getY1())) {
                        line.add(element);
                        added = true;
                        break;
                    }
                }
            }

            // 새로운 줄 생성
            if (!added) {
                List<JsonElement> newLine = new ArrayList<>();
                newLine.add(element);
                lines.add(newLine);
            }
        }

        // 각 줄 내에서 x 좌표로 정렬
        for (List<JsonElement> line : lines) {
            line.sort((a, b) -> Integer.compare(a.getX1(), b.getX1()));
        }

        return lines;
    }

    /**
     * JSON 파일을 HWPX 파일로 변환
     */
    public static void convertJsonToHwpx(String jsonFilePath, String outputPath) throws Exception {
        // 1. JSON 파일 읽기
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFilePath);
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
        reader.close();

        // 2. JsonElement 리스트로 변환
        List<JsonElement> elements = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.get(i).getAsJsonObject();
            JsonElement element = gson.fromJson(obj, JsonElement.class);
            // Picture 타입은 text가 없어도 추가
            if (element.isPicture() || (element.text != null && !element.text.isEmpty())) {
                elements.add(element);
            }
        }

        // 3. 같은 줄에 있는 요소들을 그룹화
        List<List<JsonElement>> lines = groupByLine(elements);

        // 4. 빈 한글 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();

        // 5. 첫 번째 섹션 가져오기
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 6. 각 줄을 문단으로 추가
        // PNG 파일 경로 추출 (JSON 파일과 같은 디렉토리, 페이지 번호로 찾기)
        String jsonDir = new File(jsonFilePath).getParent();
        String jsonFileName = new File(jsonFilePath).getName();
        // page_0005_0013.json -> page_0005.png
        String pageNumber = jsonFileName.split("_")[1];
        String pngFilePath = jsonDir + File.separator + "page_" + pageNumber + ".png";

        int imageCounter = 1;  // 이미지 ID 카운터

        for (List<JsonElement> line : lines) {
            // Picture 타입과 텍스트 타입 분리
            List<JsonElement> pictures = new ArrayList<>();
            List<JsonElement> textElements = new ArrayList<>();

            for (JsonElement element : line) {
                if (element.isPicture()) {
                    pictures.add(element);
                } else {
                    textElements.add(element);
                }
            }

            // Picture 요소 처리
            for (JsonElement pictureElement : pictures) {
                Para para = section.addNewPara();
                Run run = para.addNewRun();

                try {
                    String imageId = "image" + imageCounter++;
                    kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Picture picture =
                        HwpxImageInserter.addCroppedImage(hwpxFile, pngFilePath, pictureElement.bbox, imageId);

                    run.addRunItem(picture);

                    System.out.println(String.format("이미지 삽입: %s (bbox: [%d, %d, %d, %d])",
                        imageId,
                        pictureElement.bbox[0], pictureElement.bbox[1],
                        pictureElement.bbox[2], pictureElement.bbox[3]));
                } catch (Exception e) {
                    System.err.println("이미지 삽입 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 텍스트 요소가 없으면 다음 줄로
            if (textElements.isEmpty()) {
                continue;
            }

            // 줄의 모든 텍스트 요소를 하나의 문자열로 합치기
            StringBuilder lineText = new StringBuilder();
            for (int i = 0; i < textElements.size(); i++) {
                if (i > 0) {
                    lineText.append(" ");
                }
                lineText.append(textElements.get(i).text);
            }

            String text = lineText.toString();
            if (text.isEmpty()) {
                continue;
            }

            // 텍스트 정리
            String cleanedText = cleanText(text);

            // 텍스트를 구성 요소로 파싱 (일반 텍스트 + 수식)
            List<TextComponent> components = parseText(cleanedText);

            // 새 문단 생성
            Para para = section.addNewPara();
            Run run = para.addNewRun();

            // 각 구성 요소를 Run에 추가
            for (TextComponent component : components) {
                if (component.type.equals("text")) {
                    // 일반 텍스트 추가
                    T textNode = run.addNewT();
                    textNode.addText(component.content);
                } else if (component.type.equals("equation")) {
                    // 수식 추가
                    try {
                        Equation equation = createEquation(component.content, component.isInline);
                        run.addRunItem(equation);

                        System.out.println(String.format("수식 변환: %s -> %s",
                            component.content,
                            LatexToHwpTranslator.translate(component.content)));
                    } catch (Exception e) {
                        // 수식 변환 실패 시 원본 텍스트 추가
                        System.err.println("수식 변환 실패: " + component.content);
                        T textNode = run.addNewT();
                        textNode.addText("$" + component.content + "$");
                    }
                }
            }
        }

        // 7. 파일 저장
        HWPXWriter.toFilepath(hwpxFile, outputPath);

        System.out.println("HWPX 파일이 생성되었습니다: " + outputPath);
    }

    /**
     * 디렉토리에서 page_*_*.json 패턴의 파일 찾기
     */
    public static List<File> findPageJsonFiles(String dirPath) {
        List<File> jsonFiles = new ArrayList<>();
        File dir = new File(dirPath);

        if (!dir.exists() || !dir.isDirectory()) {
            return jsonFiles;
        }

        File[] files = dir.listFiles((d, name) -> name.matches("page_\\d+_\\d+\\.json"));
        if (files != null) {
            for (File file : files) {
                jsonFiles.add(file);
            }
            // 파일명으로 정렬
            jsonFiles.sort((a, b) -> a.getName().compareTo(b.getName()));
        }

        return jsonFiles;
    }

    public static void main(String[] args) {
        try {
            // 기본 경로
            String inputPath = "../../dataset/downloads/suneung/수학영역_문제지";

            // 명령줄 인수 처리
            if (args.length > 0) {
                inputPath = args[0];
            }

            File inputFile = new File(inputPath);

            List<File> jsonFiles = new ArrayList<>();
            String outputDir = null;

            // 입력이 디렉토리인 경우 page_*_*.json 파일들 찾기
            if (inputFile.isDirectory()) {
                jsonFiles = findPageJsonFiles(inputPath);
                if (jsonFiles.isEmpty()) {
                    System.err.println("오류: " + inputPath + " 디렉토리에서 page_*_*.json 파일을 찾을 수 없습니다.");
                    return;
                }
                // 출력 디렉토리는 입력 디렉토리와 동일
                outputDir = inputPath;
                System.out.println("발견된 JSON 파일: " + jsonFiles.size() + "개");
                System.out.println();
            } else if (inputFile.isFile() && inputFile.getName().endsWith(".json")) {
                // 단일 파일 처리
                jsonFiles.add(inputFile);
                // 출력 디렉토리는 입력 파일과 같은 디렉토리
                outputDir = inputFile.getParent();
                if (outputDir == null) {
                    outputDir = ".";
                }
            } else {
                System.err.println("오류: 입력 경로를 찾을 수 없습니다: " + inputPath);
                return;
            }

            // 각 JSON 파일 변환
            int successCount = 0;
            int failCount = 0;

            System.out.println("========================================");
            System.out.println("JSON → HWPX 변환 시작");
            System.out.println("========================================");
            System.out.println();

            for (File jsonFile : jsonFiles) {
                String baseName = jsonFile.getName().replaceAll("\\.json$", "");
                String outputPath = outputDir + File.separator + baseName + ".hwpx";

                try {
                    System.out.println("처리 중: " + jsonFile.getName());
                    convertJsonToHwpx(jsonFile.getAbsolutePath(), outputPath);
                    successCount++;
                    System.out.println("  → 완료: " + outputPath);
                    System.out.println();
                } catch (Exception e) {
                    failCount++;
                    System.err.println("  → 실패: " + e.getMessage());
                    System.out.println();
                }
            }

            // 결과 요약
            System.out.println("========================================");
            System.out.println("변환 완료");
            System.out.println("========================================");
            System.out.println("성공: " + successCount + "개");
            System.out.println("실패: " + failCount + "개");
            System.out.println("출력 디렉토리: " + outputDir);
            System.out.println();

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
