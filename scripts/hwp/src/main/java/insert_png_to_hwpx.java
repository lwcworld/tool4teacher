import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Picture;
import kr.dogfoot.hwpxlib.object.content.context_hpf.ManifestItem;
import kr.dogfoot.hwpxlib.object.common.AttachedFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.picture.ImageRect;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.picture.ImageDim;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapeSize;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapePosition;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.borderfill.Image;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.ImageEffect;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.*;
import kr.dogfoot.hwpxlib.object.common.baseobject.LeftRightTopBottom;
import kr.dogfoot.hwpxlib.object.common.baseobject.XAndY;
import kr.dogfoot.hwpxlib.object.common.baseobject.WidthAndHeight;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.Flip;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.RotationInfo;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.RenderingInfo;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.Matrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayOutputStream;

/**
 * PNG 파일을 HWPX 문서에 삽입하는 예제
 *
 * 사용법:
 *   mvn compile exec:java -Dexec.mainClass="insert_png_to_hwpx" \
 *     -Dexec.args="<PNG_FILE_PATH> [OUTPUT_HWPX_PATH]"
 *
 * 예시:
 *   mvn compile exec:java -Dexec.mainClass="insert_png_to_hwpx" \
 *     -Dexec.args="../../dataset/downloads/suneung/수학영역_문제지/page_0005_0013_picture_0001.png"
 */
public class insert_png_to_hwpx {

    /**
     * PNG 파일을 바이트 배열로 읽기
     */
    public static byte[] readPngFile(String pngFilePath) throws Exception {
        File pngFile = new File(pngFilePath);
        if (!pngFile.exists()) {
            throw new Exception("PNG 파일을 찾을 수 없습니다: " + pngFilePath);
        }

        BufferedImage image = ImageIO.read(pngFile);
        if (image == null) {
            throw new Exception("PNG 파일을 읽을 수 없습니다: " + pngFilePath);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();

        return imageBytes;
    }

    /**
     * PNG 이미지 크기 가져오기
     */
    public static int[] getImageSize(String pngFilePath) throws Exception {
        File pngFile = new File(pngFilePath);
        BufferedImage image = ImageIO.read(pngFile);
        if (image == null) {
            throw new Exception("PNG 파일을 읽을 수 없습니다: " + pngFilePath);
        }

        return new int[]{image.getWidth(), image.getHeight()};
    }

    /**
     * HWPX 파일에 이미지 추가
     */
    public static void addImageToManifest(HWPXFile hwpxFile, String imageId, byte[] imageData) {
        ManifestItem manifestItem = new ManifestItem();
        manifestItem.id(imageId);
        manifestItem.href("BinData/" + imageId + ".png");  // BinData 디렉토리 사용
        manifestItem.mediaType("image/png");
        manifestItem.embedded(true);  // 임베디드로 설정

        manifestItem.createAttachedFile();
        AttachedFile attachedFile = manifestItem.attachedFile();
        attachedFile.data(imageData);

        hwpxFile.contentHPFFile().manifest().add(manifestItem);
    }

    /**
     * Picture 객체 생성 (SimplePicture.hwpx 구조 참고)
     */
    public static Picture createPicture(String imageId, int width, int height) {
        Picture picture = new Picture();

        // 기본 속성 설정
        picture.idAnd(String.valueOf(System.currentTimeMillis() + (int)(Math.random() * 1000)));
        picture.zOrderAnd(1);  // 0이 아닌 1로 설정
        picture.numberingTypeAnd(NumberingType.PICTURE);
        picture.textWrapAnd(TextWrapMethod.SQUARE);
        picture.textFlowAnd(TextFlowSide.BOTH_SIDES);
        picture.lockAnd(false);
        picture.dropcapstyleAnd(DropCapStyle.None);
        picture.hrefAnd("");  // 빈 문자열로 설정
        picture.groupLevelAnd((short)0);
        picture.instidAnd(String.valueOf(System.currentTimeMillis()));
        picture.reverseAnd(false);

        // offset 설정 (필수)
        picture.createOffset();
        XAndY offset = picture.offset();
        offset.xAnd((long)0);
        offset.yAnd((long)0);

        // orgSz (원본 크기) 설정 (필수)
        picture.createOrgSz();
        WidthAndHeight orgSz = picture.orgSz();
        orgSz.widthAnd((long)width);
        orgSz.heightAnd((long)height);

        // curSz (현재 크기) 설정 (필수) - orgSz와 동일하게
        picture.createCurSz();
        WidthAndHeight curSz = picture.curSz();
        curSz.widthAnd((long)width);
        curSz.heightAnd((long)height);

        // flip 설정 (필수)
        picture.createFlip();
        Flip flip = picture.flip();
        flip.horizontalAnd(false);
        flip.verticalAnd(false);

        // rotationInfo 설정 (필수)
        picture.createRotationInfo();
        RotationInfo rotationInfo = picture.rotationInfo();
        rotationInfo.angleAnd((short)0);
        rotationInfo.centerXAnd((long)(width / 2));
        rotationInfo.centerYAnd((long)(height / 2));
        rotationInfo.rotateimageAnd(true);

        // renderingInfo 설정 (필수) - 3개의 변환 행렬
        picture.createRenderingInfo();
        RenderingInfo renderingInfo = picture.renderingInfo();

        // transMatrix (이동 행렬)
        Matrix transMatrix = renderingInfo.addNewTransMatrix();
        transMatrix.e1And(1f);
        transMatrix.e2And(0f);
        transMatrix.e3And(0f);
        transMatrix.e4And(0f);
        transMatrix.e5And(1f);
        transMatrix.e6And(0f);

        // scaMatrix (스케일 행렬)
        Matrix scaMatrix = renderingInfo.addNewScaMatrix();
        scaMatrix.e1And(1f);
        scaMatrix.e2And(0f);
        scaMatrix.e3And(0f);
        scaMatrix.e4And(0f);
        scaMatrix.e5And(1f);
        scaMatrix.e6And(0f);

        // rotMatrix (회전 행렬)
        Matrix rotMatrix = renderingInfo.addNewRotMatrix();
        rotMatrix.e1And(1f);
        rotMatrix.e2And(0f);
        rotMatrix.e3And(0f);
        rotMatrix.e4And(0f);
        rotMatrix.e5And(1f);
        rotMatrix.e6And(0f);

        // ImageRect 설정 (이미지의 네 모서리 좌표)
        picture.createImgRect();
        ImageRect imgRect = picture.imgRect();

        imgRect.createPt0();
        imgRect.pt0().xAnd((long)0);
        imgRect.pt0().yAnd((long)0);

        imgRect.createPt1();
        imgRect.pt1().xAnd((long)width);
        imgRect.pt1().yAnd((long)0);

        imgRect.createPt2();
        imgRect.pt2().xAnd((long)width);
        imgRect.pt2().yAnd((long)height);

        imgRect.createPt3();
        imgRect.pt3().xAnd((long)0);
        imgRect.pt3().yAnd((long)height);

        // imgClip 설정 (필수)
        picture.createImgClip();
        LeftRightTopBottom imgClip = picture.imgClip();
        long clipWidth = (long)(width * 100);
        long clipHeight = (long)(height * 100);
        imgClip.leftAnd(0L);
        imgClip.rightAnd(clipWidth);
        imgClip.topAnd(0L);
        imgClip.bottomAnd(clipHeight);

        // inMargin 설정 (필수)
        picture.createInMargin();
        LeftRightTopBottom inMargin = picture.inMargin();
        inMargin.leftAnd(0L);
        inMargin.rightAnd(0L);
        inMargin.topAnd(0L);
        inMargin.bottomAnd(0L);

        // ImageDim 설정 (필수)
        picture.createImgDim();
        ImageDim imgDim = picture.imgDim();
        imgDim.dimwidthAnd(clipWidth);
        imgDim.dimheightAnd(clipHeight);

        // Image 정보 설정 (필수)
        picture.createImg();
        Image img = picture.img();
        img.binaryItemIDRefAnd(imageId);
        img.brightAnd(0);
        img.contrastAnd(0);
        img.effectAnd(ImageEffect.REAL_PIC);
        img.alphaAnd(0f);

        // sz (크기) 설정
        picture.createSZ();
        ShapeSize sz = picture.sz();
        long hwpxWidth = (long)(width * 100);
        long hwpxHeight = (long)(height * 100);
        sz.widthAnd(hwpxWidth);
        sz.heightAnd(hwpxHeight);
        sz.widthRelToAnd(WidthRelTo.ABSOLUTE);
        sz.heightRelToAnd(HeightRelTo.ABSOLUTE);
        sz.protectAnd(false);

        // pos (위치) 설정
        picture.createPos();
        ShapePosition pos = picture.pos();
        pos.treatAsCharAnd(false);
        pos.affectLSpacingAnd(false);
        pos.flowWithTextAnd(true);
        pos.allowOverlapAnd(true);
        pos.holdAnchorAndSOAnd(false);
        pos.vertRelToAnd(VertRelTo.PARA);
        pos.horzRelToAnd(HorzRelTo.PARA);
        pos.vertAlignAnd(VertAlign.TOP);
        pos.horzAlignAnd(HorzAlign.LEFT);
        pos.vertOffsetAnd(0L);
        pos.horzOffsetAnd(0L);

        // outMargin 설정
        picture.createOutMargin();
        LeftRightTopBottom outMargin = picture.outMargin();
        outMargin.leftAnd(0L);
        outMargin.rightAnd(0L);
        outMargin.topAnd(0L);
        outMargin.bottomAnd(0L);

        return picture;
    }

    /**
     * PNG 파일을 HWPX 문서에 삽입
     */
    public static void insertPngToHwpx(String pngFilePath, String outputPath) throws Exception {
        System.out.println("PNG 파일 읽기: " + pngFilePath);

        // 1. PNG 파일 읽기
        byte[] imageData = readPngFile(pngFilePath);
        int[] imageSize = getImageSize(pngFilePath);
        int width = imageSize[0];
        int height = imageSize[1];

        System.out.println("  이미지 크기: " + width + "x" + height + " 픽셀");
        System.out.println("  이미지 데이터: " + imageData.length + " bytes");

        // 2. 빈 HWPX 문서 생성
        HWPXFile hwpxFile = BlankFileMaker.make();
        System.out.println("빈 HWPX 문서 생성 완료");

        // 3. 이미지를 Manifest에 추가
        String imageId = "image1";
        addImageToManifest(hwpxFile, imageId, imageData);
        System.out.println("Manifest에 이미지 추가 완료 (ID: " + imageId + ")");

        // 4. Picture 객체 생성
        Picture picture = createPicture(imageId, width, height);
        System.out.println("Picture 객체 생성 완료");

        // 5. 섹션에 Picture 추가
        SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

        // 제목 문단 추가
        Para titlePara = section.addNewPara();
        Run titleRun = titlePara.addNewRun();
        T titleText = titleRun.addNewT();
        titleText.addText("PNG 이미지 삽입 예제");
        System.out.println("제목 문단 추가 완료");

        // Picture를 포함하는 문단 추가
        Para picturePara = section.addNewPara();
        Run pictureRun = picturePara.addNewRun();
        pictureRun.addRunItem(picture);
        System.out.println("Picture 문단 추가 완료");

        // 설명 문단 추가
        Para descPara = section.addNewPara();
        Run descRun = descPara.addNewRun();
        T descText = descRun.addNewT();
        descText.addText(String.format("원본 파일: %s (크기: %dx%d)",
            new File(pngFilePath).getName(), width, height));
        System.out.println("설명 문단 추가 완료");

        // 6. HWPX 파일 저장
        HWPXWriter.toFilepath(hwpxFile, outputPath);
        System.out.println("HWPX 파일 저장 완료: " + outputPath);
    }

    public static void main(String[] args) {
        try {
            // 기본 경로 설정
            String defaultPngPath = "../../dataset/downloads/suneung/수학영역_문제지/page_0005_0013_picture_0001.png";
            String defaultOutputPath = "output/inserted_image.hwpx";

            String pngFilePath = defaultPngPath;
            String outputPath = defaultOutputPath;

            // 명령줄 인수 처리
            if (args.length > 0) {
                pngFilePath = args[0];
            }
            if (args.length > 1) {
                outputPath = args[1];
            }

            // 출력 디렉토리가 없으면 생성
            File outputFile = new File(outputPath);
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
                System.out.println("출력 디렉토리 생성: " + outputDir.getAbsolutePath());
            }

            System.out.println("========================================");
            System.out.println("PNG → HWPX 삽입 시작");
            System.out.println("========================================");
            System.out.println();

            insertPngToHwpx(pngFilePath, outputPath);

            System.out.println();
            System.out.println("========================================");
            System.out.println("✅ 완료!");
            System.out.println("========================================");
            System.out.println("출력 파일: " + new File(outputPath).getAbsolutePath());
            System.out.println();

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
