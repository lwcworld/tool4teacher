import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 파일에서 Picture 타입 요소를 찾아 원본 PNG에서 크롭하여 저장
 *
 * PictureExtractor 유틸리티를 사용하여 구현
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="extract_pictures" \
 *     -Dexec.args="<JSON_FILE_OR_DIR>"
 *
 * 예시:
 *   mvn compile exec:java -Dexec.mainClass="extract_pictures" \
 *     -Dexec.args="../../dataset/downloads/suneung/수학영역_문제지/page_0005_0013.json"
 */
public class extract_pictures {

    /**
     * JSON 파일에서 Picture 요소 추출 및 크롭하여 저장
     * PictureExtractor 유틸리티 사용
     */
    public static int extractPictures(String jsonFilePath) throws Exception {
        List<PictureExtractor.ExtractionResult> results = PictureExtractor.extractPicturesAuto(jsonFilePath);

        // 결과 출력
        if (results.isEmpty()) {
            System.out.println("  Picture 요소 없음");
            return 0;
        }

        for (PictureExtractor.ExtractionResult result : results) {
            System.out.println(String.format("  ✓ 저장: %s (bbox: [%d, %d, %d, %d], 크기: %dx%d)",
                new File(result.outputPath).getName(),
                result.bbox[0], result.bbox[1], result.bbox[2], result.bbox[3],
                result.width, result.height));
        }

        return results.size();
    }

    /**
     * 디렉토리에서 page_*_*.json 패턴의 파일 찾기
     * PictureExtractor 유틸리티 사용
     */
    public static List<File> findPageJsonFiles(String dirPath) {
        return PictureExtractor.findPageJsonFiles(dirPath);
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

            // 입력이 디렉토리인 경우
            if (inputFile.isDirectory()) {
                jsonFiles = findPageJsonFiles(inputPath);
                if (jsonFiles.isEmpty()) {
                    System.err.println("오류: " + inputPath + " 디렉토리에서 page_*_*.json 파일을 찾을 수 없습니다.");
                    return;
                }
                System.out.println("발견된 JSON 파일: " + jsonFiles.size() + "개");
                System.out.println();
            } else if (inputFile.isFile() && inputFile.getName().endsWith(".json")) {
                // 단일 파일 처리
                jsonFiles.add(inputFile);
            } else {
                System.err.println("오류: 입력 경로를 찾을 수 없습니다: " + inputPath);
                return;
            }

            // 각 JSON 파일 처리
            int totalPictures = 0;

            System.out.println("========================================");
            System.out.println("Picture 추출 시작");
            System.out.println("========================================");
            System.out.println();

            for (File jsonFile : jsonFiles) {
                System.out.println("처리 중: " + jsonFile.getName());
                try {
                    int count = extractPictures(jsonFile.getAbsolutePath());
                    totalPictures += count;
                } catch (Exception e) {
                    System.err.println("  오류: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println();
            }

            // 결과 요약
            System.out.println("========================================");
            System.out.println("추출 완료");
            System.out.println("========================================");
            System.out.println("총 추출된 이미지: " + totalPictures + "개");
            System.out.println();

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
