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

/**
 * HWP 수식에서 cases (조건부 함수) 형태를 테스트
 */
public class test_cases_format {

    public static Equation createEquation(String script) {
        Equation eq = new Equation();
        eq.versionAnd("Equation Version 60");
        eq.baseLineAnd(61);
        eq.textColorAnd("#000000");
        eq.baseUnitAnd(1100);
        eq.lineModeAnd(EquationLineMode.CHAR);
        eq.fontAnd("HYhwpEQ");
        eq.idAnd(String.valueOf(System.currentTimeMillis()));
        eq.zOrderAnd(0);
        eq.numberingTypeAnd(NumberingType.EQUATION);
        eq.textWrapAnd(TextWrapMethod.TOP_AND_BOTTOM);
        eq.textFlowAnd(TextFlowSide.BOTH_SIDES);
        eq.lockAnd(false);
        eq.dropcapstyleAnd(DropCapStyle.None);

        eq.createSZ();
        ShapeSize sz = eq.sz();
        sz.widthAnd(8000L);
        sz.widthRelToAnd(WidthRelTo.ABSOLUTE);
        sz.heightAnd(3000L);
        sz.heightRelToAnd(HeightRelTo.ABSOLUTE);
        sz.protectAnd(false);

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

        eq.createOutMargin();
        eq.outMargin().leftAnd(56L);
        eq.outMargin().rightAnd(56L);
        eq.outMargin().topAnd(0L);
        eq.outMargin().bottomAnd(0L);

        eq.createScript();
        eq.script().addText(script);

        return eq;
    }

    public static void main(String[] args) {
        try {
            HWPXFile hwpxFile = BlankFileMaker.make();
            SectionXMLFile section = hwpxFile.sectionXMLFileList().get(0);

            // 제목
            Para titlePara = section.addNewPara();
            Run titleRun = titlePara.addNewRun();
            T titleText = titleRun.addNewT();
            titleText.addText("HWP 수식 cases 형식 테스트");

            // 테스트 1: stack 사용
            Para test1Para = section.addNewPara();
            Run test1Run = test1Para.addNewRun();
            T test1Label = test1Run.addNewT();
            test1Label.addText("테스트 1 (stack): ");
            Equation eq1 = createEquation("f(x) = { stack{3x-a # x^2+a}");
            test1Run.addRunItem(eq1);

            // 테스트 2: atop 사용
            Para test2Para = section.addNewPara();
            Run test2Run = test2Para.addNewRun();
            T test2Label = test2Run.addNewT();
            test2Label.addText("테스트 2 (atop): ");
            Equation eq2 = createEquation("f(x) = { {3x-a} atop {x^2+a}");
            test2Run.addRunItem(eq2);

            // 테스트 3: matrix 사용
            Para test3Para = section.addNewPara();
            Run test3Run = test3Para.addNewRun();
            T test3Label = test3Run.addNewT();
            test3Label.addText("테스트 3 (matrix): ");
            Equation eq3 = createEquation("f(x) = { matrix{2}{2}{3x-a # (x<2) ## x^2+a # (x>=2)}");
            test3Run.addRunItem(eq3);

            // 테스트 4: 간단한 형태
            Para test4Para = section.addNewPara();
            Run test4Run = test4Para.addNewRun();
            T test4Label = test4Run.addNewT();
            test4Label.addText("테스트 4 (simple): ");
            Equation eq4 = createEquation("f(x) = 3x-a & (x<2) ; x^2+a & (x>=2)");
            test4Run.addRunItem(eq4);

            String outputPath = "output/test_cases_format.hwpx";
            HWPXWriter.toFilepath(hwpxFile, outputPath);
            System.out.println("테스트 파일 생성: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
