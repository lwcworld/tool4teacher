import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.RunItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;

/**
 * 테스트 파일의 테이블과 헤더를 함께 복사하여 4x4 테이블 생성
 */
public class Create4x4Table {
    public static void main(String[] args) {
        try {
            // 1. 테스트 파일 읽기 (테이블이 있는 파일 - 테두리 설정 포함)
            HWPXFile sourceFile = HWPXReader.fromFilepath("third_party/hwpxlib/testFile/reader_writer/SimpleTable.hwpx");

            // 2. 새 파일로 복제 (헤더 정보도 함께 복사됨)
            HWPXFile newFile = sourceFile.clone();

            // 3. 섹션 가져오기
            SectionXMLFile section = newFile.sectionXMLFileList().get(0);

            // 4. 기존 문단 제거
            section.removeAllParas();

            // 5. 원본 테이블 가져오기
            SectionXMLFile sourceSection = sourceFile.sectionXMLFileList().get(0);
            Para sourcePara = sourceSection.getPara(0);
            Run sourceRun = sourcePara.getRun(0);

            // 테이블 찾기
            Table sourceTable = null;
            for (int i = 0; i < sourceRun.countOfRunItem(); i++) {
                RunItem item = sourceRun.getRunItem(i);
                if (item instanceof Table) {
                    sourceTable = (Table) item;
                    break;
                }
            }

            if (sourceTable == null) {
                System.err.println("테이블을 찾을 수 없습니다.");
                return;
            }

            // 6. 템플릿 셀 가져오기
            Tc templateCell = sourceTable.getTr(0).getTc(0);

            // 7. 새 테이블 생성 (원본 테이블 복제)
            Table newTable = sourceTable.clone();

            // 8. 테이블을 4x4로 수정
            newTable.rowCnt((short) 4);
            newTable.colCnt((short) 4);

            // 기존 행 제거
            while (newTable.countOfTr() > 0) {
                newTable.removeTr(0);
            }

            // 9. 셀 크기 계산
            int cellWidth = 19000 / 4;
            int cellHeight = 8000 / 4;

            // 10. 4x4 행 추가
            for (int row = 0; row < 4; row++) {
                Tr tr = newTable.addNewTr();

                for (int col = 0; col < 4; col++) {
                    // 템플릿 셀 복제 (테두리 설정 포함)
                    Tc cell = templateCell.clone();

                    // 셀 주소 수정
                    cell.cellAddr().colAddr((short) col);
                    cell.cellAddr().rowAddr((short) row);

                    // 셀 병합 정보 수정
                    cell.cellSpan().colSpan((short) 1);
                    cell.cellSpan().rowSpan((short) 1);

                    // 셀 크기 수정
                    cell.cellSz().width((long) cellWidth);
                    cell.cellSz().height((long) cellHeight);

                    // 셀 내용 비우기
                    if (cell.subList() != null) {
                        cell.subList().removeAllParas();
                        Para cellPara = cell.subList().addNewPara();
                        Run cellRun = cellPara.addNewRun();
                        cellRun.addNewT().addText("");
                    }

                    tr.addTc(cell);
                }
            }

            // 11. 새 문단 생성하고 테이블 추가
            Para newPara = section.addNewPara();
            Run newRun = newPara.addNewRun();
            newRun.addRunItem(newTable);

            // 12. 파일 저장
            String outputPath = "output/table_4x4.hwpx";
            HWPXWriter.toFilepath(newFile, outputPath);

            System.out.println("테두리가 있는 4x4 테이블이 생성되었습니다: " + outputPath);

        } catch (Exception e) {
            System.err.println("파일 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}