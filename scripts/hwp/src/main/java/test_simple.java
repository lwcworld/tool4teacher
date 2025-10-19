import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;

/**
 * 간단한 테스트 파일 생성
 */
public class test_simple {
    public static void main(String[] args) {
        try {
            // 1. 빈 한글 문서 생성
            HWPXFile hwpxFile = BlankFileMaker.make();

            // 2. 첫 번째 섹션 가져오기
            SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

            // 3. 제목
            Para para1 = section.addNewPara();
            Run run1 = para1.addNewRun();
            T text1 = run1.addNewT();
            text1.addText("수식 테스트");

            // 4. 수식 1
            Para para2 = section.addNewPara();
            Run run2 = para2.addNewRun();
            T text2 = run2.addNewT();
            text2.addText("수식 1: x + y = z");

            // 5. 수식 2
            Para para3 = section.addNewPara();
            Run run3 = para3.addNewRun();
            T text3 = run3.addNewT();
            text3.addText("수식 2: a^2 + b^2 = c^2");

            // 6. 파일 저장
            String outputPath = "output/test_simple.hwpx";
            HWPXWriter.toFilepath(hwpxFile, outputPath);

            System.out.println("파일 생성 완료: " + outputPath);

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
