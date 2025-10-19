import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 파일들에서 추출한 LaTeX 수식을 분석하여
 * 유니크한 패턴을 찾고 equations.json에 추가할 테스트 케이스를 생성하는 도구
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="AnalyzeEquations" \
 *     -Dexec.args="<JSON_DIR> <OUTPUT_FILE>"
 */
public class AnalyzeEquations {

    static class EquationInfo {
        String latex;
        String type;
        int count;

        public EquationInfo(String latex, String type) {
            this.latex = latex;
            this.type = type;
            this.count = 1;
        }
    }

    /**
     * JSON 파일에서 모든 LaTeX 수식 추출
     */
    public static List<EquationInfo> extractEquationsFromJson(String jsonFilePath) throws Exception {
        List<EquationInfo> equations = new ArrayList<>();

        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFilePath);
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
        reader.close();

        Pattern blockPattern = Pattern.compile("\\$\\$([^$]+)\\$\\$");
        Pattern inlinePattern = Pattern.compile("\\$([^$]+)\\$");

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.get(i).getAsJsonObject();

            if (obj.has("text")) {
                String text = obj.get("text").getAsString();

                Matcher blockMatcher = blockPattern.matcher(text);
                while (blockMatcher.find()) {
                    String latex = blockMatcher.group(1);
                    equations.add(new EquationInfo(latex, "block"));
                }

                String textWithoutBlock = blockPattern.matcher(text).replaceAll("@@BLOCK@@");

                Matcher inlineMatcher = inlinePattern.matcher(textWithoutBlock);
                while (inlineMatcher.find()) {
                    String latex = inlineMatcher.group(1);
                    equations.add(new EquationInfo(latex, "inline"));
                }
            }
        }

        return equations;
    }

    /**
     * 디렉토리에서 모든 JSON 파일 찾기
     */
    public static List<String> findJsonFiles(String dirPath) {
        List<String> jsonFiles = new ArrayList<>();
        File dir = new File(dirPath);

        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    jsonFiles.add(f.getAbsolutePath());
                }
            }
        }

        return jsonFiles;
    }

    /**
     * 모든 JSON 파일에서 수식 추출 및 통계 생성
     */
    public static Map<String, EquationInfo> collectEquations(List<String> jsonFiles) throws Exception {
        Map<String, EquationInfo> uniqueEquations = new HashMap<>();

        for (String jsonFile : jsonFiles) {
            try {
                List<EquationInfo> equations = extractEquationsFromJson(jsonFile);
                for (EquationInfo eq : equations) {
                    String key = eq.latex;
                    if (uniqueEquations.containsKey(key)) {
                        uniqueEquations.get(key).count++;
                    } else {
                        uniqueEquations.put(key, eq);
                    }
                }
            } catch (Exception e) {
                System.err.println("경고: " + jsonFile + " 읽기 실패");
            }
        }

        return uniqueEquations;
    }

    /**
     * LaTeX 패턴 분류
     */
    public static String classifyPattern(String latex) {
        if (latex.contains("\\frac")) return "fractions";
        if (latex.contains("\\sqrt")) return "roots";
        if (latex.contains("\\lim")) return "limits";
        if (latex.contains("\\sum") || latex.contains("\\int") || latex.contains("\\prod")) return "summation_integration";
        if (latex.contains("\\sin") || latex.contains("\\cos") || latex.contains("\\tan")) return "trigonometric";
        if (latex.contains("\\log") || latex.contains("\\ln")) return "logarithms";
        if (latex.contains("\\begin{")) return "environments";
        if (latex.matches(".*\\\\[a-zA-Z]+.*")) return "symbols";
        if (latex.matches(".*[a-z]\\^\\{.*\\}.*")) return "polynomials";
        return "basic";
    }

    /**
     * 통계 출력
     */
    public static void printStatistics(Map<String, EquationInfo> equations) {
        System.out.println("=" .repeat(80));
        System.out.println("수식 통계");
        System.out.println("=" .repeat(80));
        System.out.println("총 유니크한 수식 개수: " + equations.size());
        System.out.println();

        // 카테고리별 분류
        Map<String, Integer> categoryCount = new HashMap<>();
        for (EquationInfo eq : equations.values()) {
            String category = classifyPattern(eq.latex);
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }

        System.out.println("카테고리별 분류:");
        categoryCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(e -> System.out.println(String.format("  %-25s: %4d개", e.getKey(), e.getValue())));

        System.out.println();

        // 가장 자주 나타나는 수식 (Top 20)
        System.out.println("가장 자주 나타나는 수식 (Top 20):");
        equations.values().stream()
            .sorted((a, b) -> b.count - a.count)
            .limit(20)
            .forEach(eq -> System.out.println(String.format("  [%3d회] %s", eq.count, eq.latex)));

        System.out.println();
    }

    /**
     * 샘플 수식을 파일로 저장
     */
    public static void saveSampleEquations(Map<String, EquationInfo> equations, String outputPath) throws Exception {
        FileWriter writer = new FileWriter(outputPath);

        writer.write("# 수학영역 문제지에서 추출된 LaTeX 수식 샘플\n");
        writer.write("# 총 " + equations.size() + "개의 유니크한 수식\n\n");

        // 카테고리별로 그룹화
        Map<String, List<EquationInfo>> categorized = new HashMap<>();
        for (EquationInfo eq : equations.values()) {
            String category = classifyPattern(eq.latex);
            categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(eq);
        }

        for (Map.Entry<String, List<EquationInfo>> entry : categorized.entrySet()) {
            writer.write("\n## " + entry.getKey() + "\n\n");

            // 각 카테고리에서 최대 30개만 샘플로 저장
            entry.getValue().stream()
                .sorted((a, b) -> b.count - a.count)
                .limit(30)
                .forEach(eq -> {
                    try {
                        writer.write(String.format("[%d회] %s\n", eq.count, eq.latex));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        }

        writer.close();
        System.out.println("샘플 수식이 저장되었습니다: " + outputPath);
    }

    public static void main(String[] args) {
        try {
            // 기본 경로
            String inputDir = "../../dataset/downloads/suneung/수학영역_문제지";
            String outputFile = "output/equation_samples.txt";

            if (args.length > 0) {
                inputDir = args[0];
            }
            if (args.length > 1) {
                outputFile = args[1];
            }

            System.out.println("입력 디렉토리: " + inputDir);
            System.out.println("출력 파일: " + outputFile);
            System.out.println();

            // JSON 파일 찾기
            List<String> jsonFiles = findJsonFiles(inputDir);
            System.out.println("발견된 JSON 파일: " + jsonFiles.size() + "개");
            System.out.println();

            // 수식 수집
            System.out.println("수식 추출 중...");
            Map<String, EquationInfo> equations = collectEquations(jsonFiles);

            // 통계 출력
            printStatistics(equations);

            // 샘플 저장
            saveSampleEquations(equations, outputFile);

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
