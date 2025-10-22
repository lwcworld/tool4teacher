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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 파일에서 LaTeX 수식을 추출하여 HWPX 파일의 수식으로 변환하는 예제
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="equations_from_jsons" \
 *     -Dexec.args="<JSON_FILE_PATH>"
 *
 * 예시:
 *   mvn compile exec:java -Dexec.mainClass="equations_from_jsons" \
 *     -Dexec.args="../../dataset/downloads/suneung/수학영역_문제지/page_0001.json"
 */
public class equations_from_jsons {

    /**
     * JSON 요소 클래스
     */
    static class JsonElement {
        String category;
        String text;
    }

    /**
     * LaTeX 수식 정보 클래스
     */
    static class EquationInfo {
        String latex;
        String type; // "inline" ($...$) or "block" ($$...$$)
        String originalText;

        public EquationInfo(String latex, String type, String originalText) {
            this.latex = latex;
            this.type = type;
            this.originalText = originalText;
        }
    }

    /**
     * JSON 파일에서 모든 LaTeX 수식 추출
     */
    public static List<EquationInfo> extractEquationsFromJson(String jsonFilePath) throws Exception {
        List<EquationInfo> equations = new ArrayList<>();

        // JSON 파일 읽기
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFilePath);
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
        reader.close();

        // 블록 수식 패턴: $$...$$
        Pattern blockPattern = Pattern.compile("\\$\\$([^$]+)\\$\\$");
        // 인라인 수식 패턴: $...$
        Pattern inlinePattern = Pattern.compile("\\$([^$]+)\\$");

        // 각 요소 순회
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.get(i).getAsJsonObject();

            if (obj.has("text")) {
                String text = obj.get("text").getAsString();

                // 블록 수식 먼저 추출 ($$...$$)
                Matcher blockMatcher = blockPattern.matcher(text);
                while (blockMatcher.find()) {
                    String latex = blockMatcher.group(1);
                    equations.add(new EquationInfo(latex, "block", blockMatcher.group(0)));
                }

                // 블록 수식을 임시 문자로 대체하여 인라인 수식 추출 시 중복 방지
                String textWithoutBlock = blockPattern.matcher(text).replaceAll("@@BLOCK@@");

                // 인라인 수식 추출 ($...$)
                Matcher inlineMatcher = inlinePattern.matcher(textWithoutBlock);
                while (inlineMatcher.find()) {
                    String latex = inlineMatcher.group(1);
                    equations.add(new EquationInfo(latex, "inline", "$" + latex + "$"));
                }
            }
        }

        return equations;
    }

    /**
     * Equation 객체 생성 헬퍼 함수
     */
    public static Equation createEquation(String latexText) {
        String hwpScript = LatexToHwpTranslator.translate(latexText);
        return createEquationFromScript(hwpScript);
    }

    /**
     * 이미 변환된 HWP 수식 스크립트로 Equation 객체 생성
     */
    public static Equation createEquationFromScript(String hwpScript) {
        Equation eq = new Equation();

        // 기본 속성 설정 (SimpleEquation.hwpx 참고)
        eq.versionAnd("Equation Version 60");
        eq.baseLineAnd(61);
        eq.textColorAnd("#000000");
        eq.baseUnitAnd(1100);
        eq.lineModeAnd(EquationLineMode.CHAR);
        eq.fontAnd("HYhwpEQ");

        // ID 생성 (랜덤)
        eq.idAnd(String.valueOf(System.currentTimeMillis()));
        eq.zOrderAnd(0);
        eq.numberingTypeAnd(NumberingType.EQUATION);
        eq.textWrapAnd(TextWrapMethod.TOP_AND_BOTTOM);
        eq.textFlowAnd(TextFlowSide.BOTH_SIDES);
        eq.lockAnd(false);
        eq.dropcapstyleAnd(DropCapStyle.None);

        // 크기 설정
        eq.createSZ();
        ShapeSize sz = eq.sz();
        sz.widthAnd(5000L);  // 적당한 기본 크기
        sz.widthRelToAnd(WidthRelTo.ABSOLUTE);
        sz.heightAnd(2000L);
        sz.heightRelToAnd(HeightRelTo.ABSOLUTE);
        sz.protectAnd(false);

        // 위치 설정 (텍스트처럼 취급)
        eq.createPos();
        ShapePosition pos = eq.pos();
        pos.treatAsCharAnd(true);
        pos.affectLSpacingAnd(false);
        pos.flowWithTextAnd(true);
        pos.allowOverlapAnd(false);
        pos.holdAnchorAndSOAnd(false);
        pos.vertRelToAnd(VertRelTo.PARA);
        pos.horzRelToAnd(HorzRelTo.PARA);
        pos.vertAlignAnd(VertAlign.TOP);
        pos.horzAlignAnd(HorzAlign.LEFT);
        pos.vertOffsetAnd(0L);
        pos.horzOffsetAnd(0L);

        // 여백 설정
        eq.createOutMargin();
        eq.outMargin().leftAnd(56L);
        eq.outMargin().rightAnd(56L);
        eq.outMargin().topAnd(0L);
        eq.outMargin().bottomAnd(0L);

        // 수식 스크립트 설정 (LaTeX를 HWP 포맷으로 번역)
        eq.createScript();
        eq.script().addText(hwpScript);

        return eq;
    }

    /**
     * 텍스트에서 문제가 될 수 있는 문자를 LaTeX 대체 표현으로 변환
     */
    public static String cleanText(String text) {
        if (text == null) return "";

        // 0. OCR 오류 패턴 수정 (탭 문자가 백슬래시를 대체한 경우)
        // \theta가 [탭]heta로 잘못 인식된 경우 복구
        text = text.replaceAll("\\t(heta|theta)", "\\\\theta");
        text = text.replaceAll("\\t(alpha|beta|gamma|delta|epsilon|zeta|eta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)", "\\\\$1");

        // 1. 탭, 줄바꿈 등 제어 문자를 공백으로 대체
        String cleaned = text.replaceAll("[\\t\\r\\n]", " ");

        // 2. XML 특수 문자를 LaTeX 대체 표현으로 변환 (순서 중요!)

        // 2-1. 먼저 복합 연산자 처리 (<=, >=, !=)
        cleaned = cleaned.replace("<=", " \\leq ");
        cleaned = cleaned.replace(">=", " \\geq ");
        cleaned = cleaned.replace("!=", " \\neq ");

        // 2-2. 단일 비교 연산자 처리
        cleaned = cleaned.replace("<", " \\lt ");
        cleaned = cleaned.replace(">", " \\gt ");

        // 2-3. & 기호 처리 (LaTeX에서 &는 표 정렬에 사용)
        // 단, 이미 \로 시작하는 경우는 건너뛰기 (예: \begin, \end 등)
        cleaned = cleaned.replaceAll("(?<!\\\\)&", "\\\\&");

        // 3. 연속된 공백을 하나로
        cleaned = cleaned.replaceAll("\\s+", " ");

        // 4. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * HWPX 파일에 수식을 텍스트로 추가
     */
    public static void createHwpxWithEquations(List<EquationInfo> equations, String outputPath) throws Exception {
        // 1. 빈 한글 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();

        // 2. 첫 번째 섹션 가져오기
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 3. 제목 추가
        Para titlePara = section.addNewPara();
        Run titleRun = titlePara.addNewRun();
        T titleText = titleRun.addNewT();
        titleText.addText("JSON에서 추출한 수식");

        // 빈 줄 추가
        Para emptyPara1 = section.addNewPara();
        Run emptyRun1 = emptyPara1.addNewRun();
        T emptyText1 = emptyRun1.addNewT();
        emptyText1.addText("");

        // 4. 각 수식을 문단으로 추가 (텍스트 + Equation 객체)
        for (int i = 0; i < equations.size(); i++) {
            EquationInfo eqInfo = equations.get(i);

            // 4-1. 텍스트로 수식 표시
            Para textPara = section.addNewPara();
            Run textRun = textPara.addNewRun();
            T textT = textRun.addNewT();

            // 텍스트 정리 (제어 문자 제거)
            String cleanedText = cleanText(eqInfo.latex);
            String hwpScript = LatexToHwpTranslator.translate(cleanedText);

            System.out.println(String.format("수식 %d 입력: %s", i + 1, cleanedText));
            System.out.println(String.format("수식 %d 출력: %s", i + 1, hwpScript));

            textT.addText(String.format("수식 %d (텍스트): %s", i + 1, cleanedText));

            // 4-2. Equation 객체로 수식 표시
            Para eqPara = section.addNewPara();
            Run eqRun = eqPara.addNewRun();

            // 라벨 추가
            T labelT = eqRun.addNewT();
            labelT.addText(String.format("수식 %d (객체): ", i + 1));

            // Equation 객체 추가
            Equation equation = createEquationFromScript(hwpScript);
            eqRun.addRunItem(equation);

            // 빈 줄 추가
            Para spacePara = section.addNewPara();
            Run spaceRun = spacePara.addNewRun();
            T spaceT = spaceRun.addNewT();
            spaceT.addText("");
        }

        // 빈 줄 추가
        Para emptyPara2 = section.addNewPara();
        Run emptyRun2 = emptyPara2.addNewRun();
        T emptyText2 = emptyRun2.addNewT();
        emptyText2.addText("");

        // 5. 통계 추가
        Para statsPara = section.addNewPara();
        Run statsRun = statsPara.addNewRun();
        T statsText = statsRun.addNewT();
        statsText.addText(String.format("총 %d개의 수식이 발견되었습니다.", equations.size()));

        // 6. output 디렉토리에 파일 저장
        HWPXWriter.toFilepath(hwpxFile, outputPath);

        System.out.println("수식이 포함된 한글 파일이 생성되었습니다: " + outputPath);
        System.out.println("총 수식 개수: " + equations.size());
    }

    /**
     * 디렉토리에서 모든 JSON 파일 찾기
     */
    public static List<String> findJsonFiles(String path) {
        List<String> jsonFiles = new ArrayList<>();
        File file = new File(path);

        if (file.isDirectory()) {
            // 디렉토리인 경우 모든 JSON 파일 찾기
            File[] files = file.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                Arrays.sort(files); // 파일명 순서대로 정렬
                for (File f : files) {
                    jsonFiles.add(f.getAbsolutePath());
                }
            }
        } else if (file.exists() && file.getName().endsWith(".json")) {
            // 파일인 경우 해당 파일만 추가
            jsonFiles.add(file.getAbsolutePath());
        }

        return jsonFiles;
    }

    /**
     * 여러 JSON 파일에서 모든 수식 추출
     */
    public static List<EquationInfo> extractEquationsFromMultipleFiles(List<String> jsonFiles) throws Exception {
        List<EquationInfo> allEquations = new ArrayList<>();

        for (String jsonFile : jsonFiles) {
            try {
                List<EquationInfo> equations = extractEquationsFromJson(jsonFile);
                allEquations.addAll(equations);
                System.out.println(String.format("  %s: %d개 수식",
                    new File(jsonFile).getName(), equations.size()));
            } catch (Exception e) {
                System.err.println("경고: " + jsonFile + " 읽기 실패 - " + e.getMessage());
            }
        }

        return allEquations;
    }

    public static void main(String[] args) {
        try {
            // 기본 경로: 수학영역 문제지 디렉토리
            String inputPath = "../../dataset/downloads/suneung/수학영역_문제지";

            // 명령줄 인수로 JSON 파일 또는 디렉토리 경로 지정 가능
            if (args.length > 0) {
                inputPath = args[0];
            }

            System.out.println("입력 경로: " + inputPath);
            System.out.println();

            // JSON 파일 목록 가져오기
            List<String> jsonFiles = findJsonFiles(inputPath);

            if (jsonFiles.isEmpty()) {
                System.err.println("오류: JSON 파일을 찾을 수 없습니다.");
                return;
            }

            System.out.println("발견된 JSON 파일: " + jsonFiles.size() + "개");
            System.out.println();

            // 모든 JSON 파일에서 수식 추출
            System.out.println("수식 추출 중...");
            List<EquationInfo> equations = extractEquationsFromMultipleFiles(jsonFiles);

            System.out.println();
            System.out.println("=" .repeat(60));
            System.out.println("총 발견된 수식 개수: " + equations.size());
            System.out.println("=" .repeat(60));

            // 수식 목록 출력 (처음 20개만)
            System.out.println();
            System.out.println("수식 미리보기 (처음 20개):");
            for (int i = 0; i < Math.min(20, equations.size()); i++) {
                EquationInfo eq = equations.get(i);
                System.out.println(String.format("  %d. [%s] %s", i + 1, eq.type, eq.latex));
            }
            if (equations.size() > 20) {
                System.out.println("  ... (생략된 수식: " + (equations.size() - 20) + "개)");
            }

            // HWPX 파일 생성
            System.out.println();
            String outputPath = "output/equations_from_jsons.hwpx";
            createHwpxWithEquations(equations, outputPath);

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
