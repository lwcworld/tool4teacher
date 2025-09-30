import kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;

/**
 * output 디렉토리에 "hello world"라는 텍스트가 적힌 한글 파일을 생성하는 예제
 */
public class CreateHangulFile {
    public static void main(String[] args) {
        try {
            // 1. 빈 한글 문서 생성
            HWPXFile hwpxFile = BlankFileMaker.make();

            // 2. 첫 번째 섹션 가져오기
            SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

            // 3. 새 문단 추가
            Para para = section.addNewPara();

            // 4. Run 추가
            Run run = para.addNewRun();

            // 5. 텍스트 추가
            T text = run.addNewT();
            text.addText("hello world");

            // 6. output 디렉토리에 파일 저장
            String outputPath = "output/hello_world.hwpx";
            HWPXWriter.toFilepath(hwpxFile, outputPath);

            System.out.println("한글 파일이 생성되었습니다: " + outputPath);

        } catch (Exception e) {
            System.err.println("파일 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}