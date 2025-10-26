import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 파일에서 Picture 타입 요소를 찾아 원본 PNG에서 크롭하여 저장하는 유틸리티
 */
public class PictureExtractor {

    /**
     * JSON 요소 클래스
     */
    public static class JsonElement {
        public int[] bbox;
        public String category;
        public String text;

        public boolean isPicture() {
            return "Picture".equalsIgnoreCase(category);
        }
    }

    /**
     * 추출 결과 클래스
     */
    public static class ExtractionResult {
        public String outputPath;
        public int[] bbox;
        public int width;
        public int height;

        public ExtractionResult(String outputPath, int[] bbox, int width, int height) {
            this.outputPath = outputPath;
            this.bbox = bbox;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * JSON 파일에서 Picture 요소 찾기
     *
     * @param jsonFilePath JSON 파일 경로
     * @return Picture 요소 리스트
     * @throws Exception 파일 읽기 오류
     */
    public static List<JsonElement> findPictures(String jsonFilePath) throws Exception {
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFilePath);
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
        reader.close();

        List<JsonElement> pictures = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.get(i).getAsJsonObject();
            JsonElement element = gson.fromJson(obj, JsonElement.class);
            if (element.isPicture()) {
                pictures.add(element);
            }
        }

        return pictures;
    }

    /**
     * bbox 영역을 원본 PNG에서 크롭
     *
     * @param pngFilePath 원본 PNG 파일 경로
     * @param bbox        크롭할 영역 [x1, y1, x2, y2]
     * @return 크롭된 이미지
     * @throws Exception 이미지 처리 오류
     */
    public static BufferedImage cropImage(String pngFilePath, int[] bbox) throws Exception {
        if (bbox == null || bbox.length != 4) {
            throw new IllegalArgumentException("bbox는 [x1, y1, x2, y2] 형식의 4개 요소를 가져야 합니다.");
        }

        int x1 = bbox[0];
        int y1 = bbox[1];
        int x2 = bbox[2];
        int y2 = bbox[3];

        File pngFile = new File(pngFilePath);
        if (!pngFile.exists()) {
            throw new IllegalArgumentException("PNG 파일을 찾을 수 없습니다: " + pngFilePath);
        }

        BufferedImage originalImage = ImageIO.read(pngFile);
        if (originalImage == null) {
            throw new Exception("PNG 파일을 읽을 수 없습니다: " + pngFilePath);
        }

        // 이미지 범위 내로 좌표 조정
        if (x1 < 0) x1 = 0;
        if (y1 < 0) y1 = 0;
        if (x2 > originalImage.getWidth()) x2 = originalImage.getWidth();
        if (y2 > originalImage.getHeight()) y2 = originalImage.getHeight();

        int width = x2 - x1;
        int height = y2 - y1;

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("유효하지 않은 크기: width=" + width + ", height=" + height);
        }

        return originalImage.getSubimage(x1, y1, width, height);
    }

    /**
     * 크롭된 이미지를 PNG 파일로 저장
     *
     * @param image      저장할 이미지
     * @param outputPath 출력 파일 경로
     * @throws Exception 파일 저장 오류
     */
    public static void saveImage(BufferedImage image, String outputPath) throws Exception {
        File outputFile = new File(outputPath);

        // 출력 디렉토리가 없으면 생성
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        ImageIO.write(image, "png", outputFile);
    }

    /**
     * JSON 파일과 원본 PNG에서 Picture를 추출하여 파일로 저장
     *
     * @param jsonFilePath JSON 파일 경로
     * @param pngFilePath  원본 PNG 파일 경로
     * @param outputDir    출력 디렉토리 (null이면 JSON과 같은 디렉토리)
     * @return 추출된 이미지 정보 리스트
     * @throws Exception 처리 중 오류
     */
    public static List<ExtractionResult> extractPictures(String jsonFilePath, String pngFilePath, String outputDir) throws Exception {
        List<ExtractionResult> results = new ArrayList<>();

        // Picture 요소 찾기
        List<JsonElement> pictures = findPictures(jsonFilePath);
        if (pictures.isEmpty()) {
            return results;
        }

        // 출력 디렉토리 설정
        if (outputDir == null) {
            outputDir = new File(jsonFilePath).getParent();
        }

        // 기본 파일명 생성
        String jsonFileName = new File(jsonFilePath).getName();
        String baseName = jsonFileName.replaceAll("\\.json$", "");

        // 각 Picture 크롭 및 저장
        for (int i = 0; i < pictures.size(); i++) {
            JsonElement picture = pictures.get(i);
            int[] bbox = picture.bbox;

            if (bbox == null || bbox.length != 4) {
                System.err.println("  경고: 유효하지 않은 bbox (인덱스 " + i + ")");
                continue;
            }

            try {
                // 이미지 크롭
                BufferedImage croppedImage = cropImage(pngFilePath, bbox);

                // 출력 파일명: page_0005_0013_picture_0001.png
                String outputFileName = String.format("%s_picture_%04d.png", baseName, i + 1);
                String outputPath = outputDir + File.separator + outputFileName;

                // 저장
                saveImage(croppedImage, outputPath);

                // 결과 추가
                results.add(new ExtractionResult(
                    outputPath,
                    bbox,
                    croppedImage.getWidth(),
                    croppedImage.getHeight()
                ));

            } catch (Exception e) {
                System.err.println("  오류: Picture " + (i + 1) + " 처리 실패 - " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * JSON 파일에서 Picture 자동 추출 (PNG 파일 자동 찾기)
     *
     * @param jsonFilePath JSON 파일 경로
     * @return 추출된 이미지 정보 리스트
     * @throws Exception 처리 중 오류
     */
    public static List<ExtractionResult> extractPicturesAuto(String jsonFilePath) throws Exception {
        // PNG 파일 경로 추출 (JSON과 같은 디렉토리)
        String jsonDir = new File(jsonFilePath).getParent();
        String jsonFileName = new File(jsonFilePath).getName();

        // page_0005_0013.json -> page_0005.png
        String[] parts = jsonFileName.split("_");
        if (parts.length < 2) {
            throw new IllegalArgumentException("파일명 형식이 올바르지 않습니다: " + jsonFileName);
        }

        String pageNumber = parts[1];
        String pngFilePath = jsonDir + File.separator + "page_" + pageNumber + ".png";

        File pngFile = new File(pngFilePath);
        if (!pngFile.exists()) {
            throw new IllegalArgumentException("PNG 파일을 찾을 수 없습니다: " + pngFilePath);
        }

        return extractPictures(jsonFilePath, pngFilePath, null);
    }

    /**
     * 디렉토리에서 page_*_*.json 패턴의 파일 찾기
     *
     * @param dirPath 디렉토리 경로
     * @return JSON 파일 리스트
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

    /**
     * 디렉토리의 모든 JSON 파일에서 Picture 추출
     *
     * @param dirPath 디렉토리 경로
     * @return 추출된 이미지 총 개수
     */
    public static int extractPicturesFromDirectory(String dirPath) {
        List<File> jsonFiles = findPageJsonFiles(dirPath);
        int totalPictures = 0;

        for (File jsonFile : jsonFiles) {
            try {
                List<ExtractionResult> results = extractPicturesAuto(jsonFile.getAbsolutePath());
                totalPictures += results.size();

                if (!results.isEmpty()) {
                    System.out.println("처리: " + jsonFile.getName());
                    for (ExtractionResult result : results) {
                        System.out.println(String.format("  ✓ 저장: %s (bbox: [%d, %d, %d, %d], 크기: %dx%d)",
                            new File(result.outputPath).getName(),
                            result.bbox[0], result.bbox[1], result.bbox[2], result.bbox[3],
                            result.width, result.height));
                    }
                }
            } catch (Exception e) {
                System.err.println("오류: " + jsonFile.getName() + " - " + e.getMessage());
            }
        }

        return totalPictures;
    }
}
