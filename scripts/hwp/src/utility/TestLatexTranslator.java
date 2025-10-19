import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * LaTeX to HWP 번역기 테스트 프로그램
 * dataset/equation_examples/equations.json 파일의 테스트 케이스를 사용하여
 * 번역기의 정확도를 검증합니다.
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="TestLatexTranslator"
 */
public class TestLatexTranslator {

    static class TestCase {
        String latex;
        String expectedHwp;
        String description;
        String status;
        String category;
        String note;

        public TestCase(String latex, String expectedHwp, String description, String status, String category, String note) {
            this.latex = latex;
            this.expectedHwp = expectedHwp;
            this.description = description;
            this.status = status;
            this.category = category;
            this.note = note;
        }
    }

    static class TestResult {
        TestCase testCase;
        String actualHwp;
        boolean passed;

        public TestResult(TestCase testCase, String actualHwp, boolean passed) {
            this.testCase = testCase;
            this.actualHwp = actualHwp;
            this.passed = passed;
        }
    }

    /**
     * JSON 파일에서 테스트 케이스 로드
     */
    public static List<TestCase> loadTestCases(String jsonFilePath) throws Exception {
        List<TestCase> testCases = new ArrayList<>();

        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFilePath);
        JsonObject root = gson.fromJson(reader, JsonObject.class);
        reader.close();

        JsonArray categories = root.getAsJsonArray("categories");

        for (int i = 0; i < categories.size(); i++) {
            JsonObject category = categories.get(i).getAsJsonObject();
            String categoryName = category.get("category").getAsString();
            JsonArray equations = category.getAsJsonArray("equations");

            for (int j = 0; j < equations.size(); j++) {
                JsonObject eq = equations.get(j).getAsJsonObject();

                String latex = eq.get("latex").getAsString();
                String expectedHwp = eq.get("expected_hwp").getAsString();
                String description = eq.get("description").getAsString();
                String status = eq.get("status").getAsString();
                String note = eq.has("note") ? eq.get("note").getAsString() : null;

                testCases.add(new TestCase(latex, expectedHwp, description, status, categoryName, note));
            }
        }

        return testCases;
    }

    /**
     * 테스트 실행
     */
    public static List<TestResult> runTests(List<TestCase> testCases) {
        List<TestResult> results = new ArrayList<>();

        for (TestCase testCase : testCases) {
            String actualHwp = LatexToHwpTranslator.translate(testCase.latex);
            boolean passed = actualHwp.equals(testCase.expectedHwp);
            results.add(new TestResult(testCase, actualHwp, passed));
        }

        return results;
    }

    /**
     * 테스트 결과 출력
     */
    public static void printResults(List<TestResult> results) {
        int totalTests = results.size();
        int passedTests = 0;
        int failedTests = 0;
        int pendingTests = 0;

        System.out.println("=".repeat(80));
        System.out.println("LaTeX to HWP Translator - Test Results");
        System.out.println("=".repeat(80));
        System.out.println();

        String currentCategory = "";

        for (TestResult result : results) {
            TestCase tc = result.testCase;

            // 카테고리가 바뀌면 헤더 출력
            if (!tc.category.equals(currentCategory)) {
                currentCategory = tc.category;
                System.out.println();
                System.out.println("[" + currentCategory + "]");
                System.out.println("-".repeat(80));
            }

            // 상태 카운트
            if (tc.status.equals("pending")) {
                pendingTests++;
            } else if (result.passed) {
                passedTests++;
            } else {
                failedTests++;
            }

            // 결과 출력
            String statusIcon;
            if (tc.status.equals("pending")) {
                statusIcon = "⏸ PENDING";
            } else if (result.passed) {
                statusIcon = "✓ PASS";
            } else {
                statusIcon = "✗ FAIL";
            }

            System.out.printf("%s | %s%n", statusIcon, tc.description);
            System.out.printf("  LaTeX:    %s%n", tc.latex);
            System.out.printf("  Expected: %s%n", tc.expectedHwp);
            System.out.printf("  Actual:   %s%n", result.actualHwp);

            if (!result.passed && !tc.status.equals("pending")) {
                System.out.println("  >>> MISMATCH! <<<");
            }

            if (tc.note != null) {
                System.out.printf("  Note:     %s%n", tc.note);
            }

            System.out.println();
        }

        // 요약 출력
        System.out.println("=".repeat(80));
        System.out.println("Summary");
        System.out.println("=".repeat(80));
        System.out.printf("Total Tests:   %d%n", totalTests);
        System.out.printf("Passed:        %d (%.1f%%)%n", passedTests, 100.0 * passedTests / totalTests);
        System.out.printf("Failed:        %d (%.1f%%)%n", failedTests, 100.0 * failedTests / totalTests);
        System.out.printf("Pending:       %d (%.1f%%)%n", pendingTests, 100.0 * pendingTests / totalTests);
        System.out.println("=".repeat(80));

        // 실패한 테스트만 따로 출력
        if (failedTests > 0) {
            System.out.println();
            System.out.println("Failed Tests (Need Attention):");
            System.out.println("-".repeat(80));
            for (TestResult result : results) {
                if (!result.passed && !result.testCase.status.equals("pending")) {
                    TestCase tc = result.testCase;
                    System.out.printf("- [%s] %s%n", tc.category, tc.description);
                    System.out.printf("  LaTeX: %s%n", tc.latex);
                    System.out.printf("  Expected: %s%n", tc.expectedHwp);
                    System.out.printf("  Actual:   %s%n", result.actualHwp);
                    System.out.println();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // 기본 경로
            String jsonFilePath = "../../dataset/equation_examples/equations.json";

            // 명령줄 인수로 경로 지정 가능
            if (args.length > 0) {
                jsonFilePath = args[0];
            }

            System.out.println("Loading test cases from: " + jsonFilePath);
            System.out.println();

            // 테스트 케이스 로드
            List<TestCase> testCases = loadTestCases(jsonFilePath);

            // 테스트 실행
            List<TestResult> results = runTests(testCases);

            // 결과 출력
            printResults(results);

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
