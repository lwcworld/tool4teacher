import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Picture;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.picture.ImageRect;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.picture.ImageDim;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapeSize;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.ShapePosition;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.borderfill.Image;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.ImageEffect;
import kr.dogfoot.hwpxlib.object.content.context_hpf.ManifestItem;
import kr.dogfoot.hwpxlib.object.common.AttachedFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.*;
import kr.dogfoot.hwpxlib.object.common.baseobject.LeftRightTopBottom;
import kr.dogfoot.hwpxlib.object.common.baseobject.Point;
import kr.dogfoot.hwpxlib.object.common.baseobject.XAndY;
import kr.dogfoot.hwpxlib.object.common.baseobject.WidthAndHeight;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.Flip;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.RotationInfo;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.RenderingInfo;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapecomponent.Matrix;

/**
 * HWPX 파일에 이미지를 삽입하는 유틸리티
 *
 * hwpxlib를 활용하여 이미지 데이터를 HWPX 문서에 추가하는 기능 제공
 */
public class HwpxImageInserter {

    /**
     * HWPX 파일에 이미지 바이너리 데이터를 ManifestItem으로 추가
     *
     * @param hwpxFile HWPX 파일 객체
     * @param imageId 이미지 ID (예: "image1", "image2")
     * @param imageData PNG 이미지 바이트 배열
     * @return 추가된 ManifestItem 객체
     */
    public static ManifestItem addImageToManifest(HWPXFile hwpxFile, String imageId, byte[] imageData) {
        // ManifestItem 생성
        ManifestItem manifestItem = new ManifestItem();
        manifestItem.id(imageId);
        manifestItem.href("BinData/" + imageId + ".png");  // BinData 디렉토리 사용
        manifestItem.mediaType("image/png");
        manifestItem.embedded(true);  // 임베디드로 설정

        // AttachedFile 생성 및 이미지 데이터 설정
        manifestItem.createAttachedFile();
        AttachedFile attachedFile = manifestItem.attachedFile();
        attachedFile.data(imageData);

        // Manifest에 추가
        hwpxFile.contentHPFFile().manifest().add(manifestItem);

        return manifestItem;
    }

    /**
     * Picture 객체 생성 (SimplePicture.hwpx 구조 참고)
     *
     * @param imageId 이미지 ID (ManifestItem의 ID와 동일해야 함)
     * @param width 이미지 너비 (픽셀)
     * @param height 이미지 높이 (픽셀)
     * @return Picture 객체
     */
    public static Picture createPicture(String imageId, int width, int height) {
        Picture picture = new Picture();

        // 기본 속성 설정
        picture.idAnd(String.valueOf(System.currentTimeMillis() + (int)(Math.random() * 1000)));
        picture.zOrderAnd(1);  // 0이 아닌 1로 설정
        picture.numberingTypeAnd(NumberingType.PICTURE);
        picture.textWrapAnd(TextWrapMethod.TOP_AND_BOTTOM);  // 그림 위아래로만 텍스트 배치
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

        // pos (위치) 설정 - 글자처럼 취급하지 않음
        picture.createPos();
        ShapePosition pos = picture.pos();
        pos.treatAsCharAnd(false);  // 글자처럼 취급 안 함
        pos.affectLSpacingAnd(false);  // 줄 간격에 영향 안 줌
        pos.flowWithTextAnd(true);  // 텍스트와 함께 흐름
        pos.allowOverlapAnd(false);  // 겹침 허용 안함
        pos.holdAnchorAndSOAnd(false);
        pos.vertRelToAnd(VertRelTo.PARA);
        pos.horzRelToAnd(HorzRelTo.PARA);
        pos.vertAlignAnd(VertAlign.TOP);  // 상단 정렬
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
     * HWPX 파일에 크롭된 이미지 추가 (전체 프로세스)
     *
     * @param hwpxFile HWPX 파일 객체
     * @param pngFilePath 원본 PNG 파일 경로
     * @param bbox 크롭할 영역 [x1, y1, x2, y2]
     * @param imageId 이미지 ID
     * @return 생성된 Picture 객체
     * @throws Exception 이미지 처리 중 오류 발생 시
     */
    public static Picture addCroppedImage(HWPXFile hwpxFile, String pngFilePath, int[] bbox, String imageId) throws Exception {
        // 1. 이미지 크롭 (PictureExtractor 사용)
        java.awt.image.BufferedImage croppedImage = PictureExtractor.cropImage(pngFilePath, bbox);

        // BufferedImage를 바이트 배열로 변환
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(croppedImage, "png", baos);
        baos.flush();
        byte[] imageData = baos.toByteArray();
        baos.close();

        // 2. ManifestItem에 추가
        addImageToManifest(hwpxFile, imageId, imageData);

        // 3. Picture 객체 생성
        int width = croppedImage.getWidth();
        int height = croppedImage.getHeight();
        Picture picture = createPicture(imageId, width, height);

        return picture;
    }
}
