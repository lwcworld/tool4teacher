import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * LaTeX 수식 문법을 HWP 수식 문법으로 변환하는 번역기
 *
 * LaTeX vs HWP 문법 차이:
 * - LaTeX: \frac{a}{b}    -> HWP: {a} over {b}
 * - LaTeX: \sqrt{x}       -> HWP: sqrt {x}
 * - LaTeX: x^{2}          -> HWP: x^{2}  (동일)
 * - LaTeX: x_{i}          -> HWP: x_{i}  (동일)
 * - LaTeX: \sum           -> HWP: sum
 * - LaTeX: \int           -> HWP: int
 * - LaTeX: \lim           -> HWP: lim
 * - LaTeX: \sin, \cos     -> HWP: sin, cos
 */
public class LatexToHwpTranslator {

    /**
     * LaTeX 수식을 HWP 수식으로 변환
     */
    public static String translate(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }

        String hwp = latex;

        // 1. n제곱근 변환 먼저: \sqrt[n]{내용} -> nroot {n} {내용}
        hwp = translateNthRoot(hwp);

        // 2. 제곱근 변환: \sqrt{내용} -> sqrt {내용}
        hwp = translateSqrt(hwp);

        // 3. 분수 변환: \frac{분자}{분모} -> {분자} over {분모} (제곱근 변환 후 수행)
        hwp = translateFractions(hwp);

        // 4. 특수 기호 변환 (공백 명령어 포함: \quad, \qquad 등)
        hwp = translateSymbols(hwp);

        // 5. 선 위에 표시 (\overline 등) 변환
        hwp = translateOverline(hwp);

        // 5. 그리스 문자 변환: \alpha, \beta, \theta 등
        hwp = translateGreekLetters(hwp);

        // 6. 수학 함수 변환: \sin, \cos, \tan, \log 등
        hwp = translateMathFunctions(hwp);

        // 7. 극한 변환: \lim_{x \to a} -> lim from {x -> a}
        hwp = translateLimit(hwp);

        // 8. 합과 적분 변환: \sum_{i=1}^{n} -> sum _{i=1} ^{n}
        hwp = translateSumAndIntegral(hwp);

        // 9. 대소비교 기호 변환
        hwp = translateComparison(hwp);

        // 10. 좌/우 구분자 변환: \left, \right
        hwp = translateLeftRightDelimiters(hwp);

        // 11. 수동 중괄호 (\{...\}) 변환
        hwp = translateExplicitSetBraces(hwp);

        // 12. 중괄호 변환: \{ \} -> lbrace rbrace
        hwp = translateBraces(hwp);

        // 13. LaTeX 환경 제거 (cases, matrix 등)
        hwp = removeLatexEnvironments(hwp);

        // 14. HWP 수식 공백 규칙 보정
        hwp = normalizeHwpSpacing(hwp);

        // 15. 첨자/지수 그룹 보정
        hwp = ensureScriptBraces(hwp);

        // 16. 공백 정리
        hwp = cleanupSpaces(hwp);

        return hwp;
    }

    /**
     * 분수 변환: \frac{분자}{분모} -> {분자} over {분모}
     * 중첩된 분수를 처리하기 위해 재귀적으로 변환
     */
    private static String translateFractions(String latex) {
        String current = latex;
        int maxIterations = 10;
        int iteration = 0;

        while (iteration < maxIterations) {
            String prev = current;

            // \frac을 찾아서 중괄호 매칭하여 변환
            int pos = 0;
            StringBuilder result = new StringBuilder();

            while (pos < current.length()) {
                // \frac 찾기
                int fracPos = current.indexOf("\\frac", pos);
                if (fracPos == -1) {
                    // 더 이상 \frac이 없으면 나머지 추가
                    result.append(current.substring(pos));
                    break;
                }

                // \frac 이전 부분 추가
                result.append(current.substring(pos, fracPos));

                // 첫 번째 중괄호 블록 (분자) 추출
                int start1 = fracPos + 5; // "\frac" 다음
                if (start1 >= current.length() || current.charAt(start1) != '{') {
                    result.append("\\frac");
                    pos = fracPos + 5;
                    continue;
                }

                String numerator = extractBracedContent(current, start1);
                if (numerator == null) {
                    result.append("\\frac");
                    pos = fracPos + 5;
                    continue;
                }

                // 두 번째 중괄호 블록 (분모) 추출
                int start2 = start1 + numerator.length() + 2; // +2 for {}
                if (start2 >= current.length() || current.charAt(start2) != '{') {
                    result.append("\\frac{").append(numerator).append("}");
                    pos = start2;
                    continue;
                }

                String denominator = extractBracedContent(current, start2);
                if (denominator == null) {
                    result.append("\\frac{").append(numerator).append("}");
                    pos = start2;
                    continue;
                }

                // 변환: {분자} over {분모}
                result.append("{").append(numerator).append("} over {").append(denominator).append("}");
                pos = start2 + denominator.length() + 2;
            }

            current = result.toString();

            // 변화가 없으면 종료
            if (current.equals(prev)) {
                break;
            }
            iteration++;
        }

        return current;
    }

    /**
     * 중괄호로 감싸진 내용 추출 (중첩된 중괄호 지원)
     * @param text 전체 텍스트
     * @param startPos 시작 중괄호 '{' 위치
     * @return 중괄호 안의 내용 (중괄호 제외), 실패 시 null
     */
    private static String extractBracedContent(String text, int startPos) {
        if (startPos >= text.length() || text.charAt(startPos) != '{') {
            return null;
        }

        int depth = 0;
        int start = startPos + 1; // '{' 다음부터

        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    // 매칭되는 닫는 중괄호 찾음
                    return text.substring(start, i);
                }
            }
        }

        // 매칭되는 닫는 중괄호를 찾지 못함
        return null;
    }

    /**
     * 제곱근 변환: \sqrt{내용} -> sqrt {내용}
     */
    private static String translateSqrt(String latex) {
        Pattern pattern = Pattern.compile("\\\\sqrt\\{([^{}]+)\\}");
        Matcher matcher = pattern.matcher(latex);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            String replacement = "sqrt {" + content + "}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * n제곱근 변환: \sqrt[n]{내용} -> nroot {n} {내용}
     */
    private static String translateNthRoot(String latex) {
        Pattern pattern = Pattern.compile("\\\\sqrt\\[([^\\]]+)\\]\\{([^{}]+)\\}");
        Matcher matcher = pattern.matcher(latex);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String n = matcher.group(1);
            String content = matcher.group(2);
            String replacement = "nroot {" + n + "} {" + content + "}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 그리스 문자 변환
     * 뒤에 알파벳이 바로 오는 경우 또는 앞에 특수 문자가 있는 경우 공백 추가
     */
    private static String translateGreekLetters(String latex) {
        // 소문자 그리스 문자 - 뒤에 문자가 오거나 앞에 } 또는 숫자가 오면 공백 추가
        latex = latex.replaceAll("([\\}0-9])\\\\alpha", "$1 alpha");
        latex = latex.replaceAll("\\\\alpha(?=[a-zA-Z])", "alpha ");
        latex = latex.replaceAll("\\\\alpha", "alpha");
        latex = latex.replaceAll("([\\}0-9])\\\\beta", "$1 beta");
        latex = latex.replaceAll("\\\\beta(?=[a-zA-Z])", "beta ");
        latex = latex.replaceAll("\\\\beta", "beta");
        latex = latex.replaceAll("(\\})\\\\gamma", "$1 gamma");
        latex = latex.replaceAll("\\\\gamma(?=[a-zA-Z])", "gamma ");
        latex = latex.replaceAll("\\\\gamma", "gamma");
        latex = latex.replaceAll("(\\})\\\\delta", "$1 delta");
        latex = latex.replaceAll("\\\\delta(?=[a-zA-Z])", "delta ");
        latex = latex.replaceAll("\\\\delta", "delta");
        latex = latex.replaceAll("(\\})\\\\epsilon", "$1 epsilon");
        latex = latex.replaceAll("\\\\epsilon(?=[a-zA-Z])", "epsilon ");
        latex = latex.replaceAll("\\\\epsilon", "epsilon");
        latex = latex.replaceAll("(\\})\\\\zeta", "$1 zeta");
        latex = latex.replaceAll("\\\\zeta(?=[a-zA-Z])", "zeta ");
        latex = latex.replaceAll("\\\\zeta", "zeta");
        latex = latex.replaceAll("(\\})\\\\eta", "$1 eta");
        latex = latex.replaceAll("\\\\eta(?=[a-zA-Z])", "eta ");
        latex = latex.replaceAll("\\\\eta", "eta");
        latex = latex.replaceAll("(\\})\\\\theta", "$1 theta");
        latex = latex.replaceAll("\\\\theta(?=[a-zA-Z])", "theta ");
        latex = latex.replaceAll("\\\\theta", "theta");
        latex = latex.replaceAll("(\\})\\\\iota", "$1 iota");
        latex = latex.replaceAll("\\\\iota(?=[a-zA-Z])", "iota ");
        latex = latex.replaceAll("\\\\iota", "iota");
        latex = latex.replaceAll("(\\})\\\\kappa", "$1 kappa");
        latex = latex.replaceAll("\\\\kappa(?=[a-zA-Z])", "kappa ");
        latex = latex.replaceAll("\\\\kappa", "kappa");
        latex = latex.replaceAll("(\\})\\\\lambda", "$1 lambda");
        latex = latex.replaceAll("\\\\lambda(?=[a-zA-Z])", "lambda ");
        latex = latex.replaceAll("\\\\lambda", "lambda");
        latex = latex.replaceAll("(\\})\\\\mu", "$1 mu");
        latex = latex.replaceAll("\\\\mu(?=[a-zA-Z])", "mu ");
        latex = latex.replaceAll("\\\\mu", "mu");
        latex = latex.replaceAll("(\\})\\\\nu", "$1 nu");
        latex = latex.replaceAll("\\\\nu(?=[a-zA-Z])", "nu ");
        latex = latex.replaceAll("\\\\nu", "nu");
        latex = latex.replaceAll("(\\})\\\\xi", "$1 xi");
        latex = latex.replaceAll("\\\\xi(?=[a-zA-Z])", "xi ");
        latex = latex.replaceAll("\\\\xi", "xi");
        latex = latex.replaceAll("([\\}0-9])\\\\pi", "$1 pi");
        latex = latex.replaceAll("\\\\pi(?=[a-zA-Z])", "pi ");
        latex = latex.replaceAll("\\\\pi", "pi");
        latex = latex.replaceAll("(\\})\\\\rho", "$1 rho");
        latex = latex.replaceAll("\\\\rho(?=[a-zA-Z])", "rho ");
        latex = latex.replaceAll("\\\\rho", "rho");
        latex = latex.replaceAll("(\\})\\\\sigma", "$1 sigma");
        latex = latex.replaceAll("\\\\sigma(?=[a-zA-Z])", "sigma ");
        latex = latex.replaceAll("\\\\sigma", "sigma");
        latex = latex.replaceAll("(\\})\\\\tau", "$1 tau");
        latex = latex.replaceAll("\\\\tau(?=[a-zA-Z])", "tau ");
        latex = latex.replaceAll("\\\\tau", "tau");
        latex = latex.replaceAll("(\\})\\\\upsilon", "$1 upsilon");
        latex = latex.replaceAll("\\\\upsilon(?=[a-zA-Z])", "upsilon ");
        latex = latex.replaceAll("\\\\upsilon", "upsilon");
        latex = latex.replaceAll("(\\})\\\\phi", "$1 phi");
        latex = latex.replaceAll("\\\\phi(?=[a-zA-Z])", "phi ");
        latex = latex.replaceAll("\\\\phi", "phi");
        latex = latex.replaceAll("(\\})\\\\chi", "$1 chi");
        latex = latex.replaceAll("\\\\chi(?=[a-zA-Z])", "chi ");
        latex = latex.replaceAll("\\\\chi", "chi");
        latex = latex.replaceAll("(\\})\\\\psi", "$1 psi");
        latex = latex.replaceAll("\\\\psi(?=[a-zA-Z])", "psi ");
        latex = latex.replaceAll("\\\\psi", "psi");
        latex = latex.replaceAll("(\\})\\\\omega", "$1 omega");
        latex = latex.replaceAll("\\\\omega(?=[a-zA-Z])", "omega ");
        latex = latex.replaceAll("\\\\omega", "omega");

        // 대문자 그리스 문자
        latex = latex.replaceAll("\\\\Gamma(?=[a-zA-Z])", "GAMMA ");
        latex = latex.replaceAll("\\\\Gamma", "GAMMA");
        latex = latex.replaceAll("\\\\Delta(?=[a-zA-Z])", "DELTA ");
        latex = latex.replaceAll("\\\\Delta", "DELTA");
        latex = latex.replaceAll("\\\\Theta(?=[a-zA-Z])", "THETA ");
        latex = latex.replaceAll("\\\\Theta", "THETA");
        latex = latex.replaceAll("\\\\Lambda(?=[a-zA-Z])", "LAMBDA ");
        latex = latex.replaceAll("\\\\Lambda", "LAMBDA");
        latex = latex.replaceAll("\\\\Xi(?=[a-zA-Z])", "XI ");
        latex = latex.replaceAll("\\\\Xi", "XI");
        latex = latex.replaceAll("\\\\Pi(?=[a-zA-Z])", "PI ");
        latex = latex.replaceAll("\\\\Pi", "PI");
        latex = latex.replaceAll("\\\\Sigma(?=[a-zA-Z])", "SIGMA ");
        latex = latex.replaceAll("\\\\Sigma", "SIGMA");
        latex = latex.replaceAll("\\\\Upsilon(?=[a-zA-Z])", "UPSILON ");
        latex = latex.replaceAll("\\\\Upsilon", "UPSILON");
        latex = latex.replaceAll("\\\\Phi(?=[a-zA-Z])", "PHI ");
        latex = latex.replaceAll("\\\\Phi", "PHI");
        latex = latex.replaceAll("\\\\Psi(?=[a-zA-Z])", "PSI ");
        latex = latex.replaceAll("\\\\Psi", "PSI");
        latex = latex.replaceAll("\\\\Omega(?=[a-zA-Z])", "OMEGA ");
        latex = latex.replaceAll("\\\\Omega", "OMEGA");

        return latex;
    }

    /**
     * 수학 함수 변환
     * 뒤에 알파벳이 바로 오는 경우 공백 추가
     */
    private static String translateMathFunctions(String latex) {
        // 삼각함수 - 뒤에 문자가 오면 공백 추가
        latex = latex.replaceAll("\\\\sin(?=[a-zA-Z])", "sin ");
        latex = latex.replaceAll("\\\\sin", "sin");
        latex = latex.replaceAll("\\\\cos(?=[a-zA-Z])", "cos ");
        latex = latex.replaceAll("\\\\cos", "cos");
        latex = latex.replaceAll("\\\\tan(?=[a-zA-Z])", "tan ");
        latex = latex.replaceAll("\\\\tan", "tan");
        latex = latex.replaceAll("\\\\cot(?=[a-zA-Z])", "cot ");
        latex = latex.replaceAll("\\\\cot", "cot");
        latex = latex.replaceAll("\\\\sec(?=[a-zA-Z])", "sec ");
        latex = latex.replaceAll("\\\\sec", "sec");
        latex = latex.replaceAll("\\\\csc(?=[a-zA-Z])", "csc ");
        latex = latex.replaceAll("\\\\csc", "csc");

        // 로그
        latex = latex.replaceAll("\\\\log(?=[a-zA-Z])", "log ");
        latex = latex.replaceAll("\\\\log", "log");
        latex = latex.replaceAll("\\\\ln(?=[a-zA-Z])", "ln ");
        latex = latex.replaceAll("\\\\ln", "ln");

        // 지수
        latex = latex.replaceAll("\\\\exp(?=[a-zA-Z])", "exp ");
        latex = latex.replaceAll("\\\\exp", "exp");

        return latex;
    }

    /**
     * 극한 변환: \lim_{x \to a} -> lim from {x -> a}
     */
    private static String translateLimit(String latex) {
        // \lim_{조건} 패턴
        Pattern pattern = Pattern.compile("\\\\lim_\\{([^{}]+)\\}");
        Matcher matcher = pattern.matcher(latex);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String condition = matcher.group(1);
            // \to를 ->로 변환
            condition = condition.replaceAll("\\\\to", "->");
            String replacement = "lim from {" + condition + "}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        // 아래첨자 없는 경우
        latex = result.toString().replaceAll("\\\\lim", "lim");

        return latex;
    }

    /**
     * 합과 적분 변환
     */
    private static String translateSumAndIntegral(String latex) {
        latex = latex.replaceAll("\\\\sum", "sum");
        latex = latex.replaceAll("\\\\prod", "prod");
        latex = latex.replaceAll("\\\\int", "int");

        return latex;
    }

    /**
     * 대소비교 기호 변환
     */
    private static String translateComparison(String latex) {
        latex = latex.replaceAll("\\\\leq", "<=");
        latex = latex.replaceAll("\\\\geq", ">=");
        latex = latex.replaceAll("\\\\neq", "!=");
        latex = latex.replaceAll("\\\\lt", "<");
        latex = latex.replaceAll("\\\\gt", ">");
        latex = latex.replaceAll("\\\\le(?![a-zA-Z])", "<=");
        latex = latex.replaceAll("\\\\ge(?![a-zA-Z])", ">=");

        return latex;
    }

    /**
     * \left, \right 구분자를 HWP 형식으로 변환
     */
    private static String translateLeftRightDelimiters(String latex) {
        latex = replaceLeftOrRightDelimiter(latex, true);
        latex = replaceLeftOrRightDelimiter(latex, false);
        return latex;
    }

    private static String replaceLeftOrRightDelimiter(String latex, boolean isLeft) {
        Pattern pattern = isLeft
            ? Pattern.compile("\\\\left\\s*(\\\\[a-zA-Z]+|\\\\.|.)")
            : Pattern.compile("\\\\right\\s*(\\\\[a-zA-Z]+|\\\\.|.)");

        Matcher matcher = pattern.matcher(latex);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String token = matcher.group(1);
            String delimiter = normalizeLeftRightDelimiter(token);
            String replacement = (isLeft ? "LEFT " : "RIGHT ") + delimiter;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String normalizeLeftRightDelimiter(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }

        String symbol = token;
        if (token.startsWith("\\")) {
            symbol = token.substring(1);
            if (symbol.length() == 1) {
                return symbol;
            }

            switch (symbol) {
                case "lbrace":
                case "{":
                    return "{";
                case "rbrace":
                case "}":
                    return "}";
                case "lparen":
                    return "(";
                case "rparen":
                    return ")";
                case "lbrack":
                    return "[";
                case "rbrack":
                    return "]";
                case "vert":
                    return "|";
                case "Vert":
                    return "||";
                case "langle":
                    return "<";
                case "rangle":
                    return ">";
                case "lfloor":
                case "rfloor":
                case "lceil":
                case "rceil":
                case "lgroup":
                case "rgroup":
                case "lmoustache":
                case "rmoustache":
                    return symbol;
                case ".":
                    return ".";
                default:
                    return symbol;
            }
        }

        return symbol;
    }

    /**
     * \{...\} 패턴을 HWP 구분자로 변환
     */
    private static String translateExplicitSetBraces(String latex) {
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (index < latex.length()) {
            if (latex.startsWith("\\{", index)) {
                int closing = findMatchingExplicitBrace(latex, index + 1);
                if (closing != -1) {
                    String content = latex.substring(index + 2, closing).trim();
                    content = normalizeHwpSubscripts(content);
                    result.append("LEFT { ");
                    result.append(content);
                    result.append(" RIGHT }");
                    index = closing + 2;
                    continue;
                }
            }
            result.append(latex.charAt(index));
            index++;
        }

        return result.toString();
    }

    private static int findMatchingExplicitBrace(String text, int openBraceIndex) {
        int depth = 1;
        int i = openBraceIndex + 1;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '\\') {
                if (i + 1 < text.length()) {
                    char next = text.charAt(i + 1);
                    if (next == '{') {
                        depth++;
                        i += 2;
                        continue;
                    }
                    if (next == '}') {
                        depth--;
                        if (depth == 0) {
                            return i;
                        }
                        i += 2;
                        continue;
                    }
                }
                i++;
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }

        return -1;
    }

    private static String normalizeHwpSubscripts(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '_' && i + 1 < text.length()) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                sb.append('_');

                if (text.charAt(i + 1) == '{') {
                    sb.append('{');
                    i += 2;
                    int depth = 1;
                    int start = i;

                    while (i < text.length() && depth > 0) {
                        char ch = text.charAt(i);
                        if (ch == '{') {
                            depth++;
                        } else if (ch == '}') {
                            depth--;
                        }
                        if (depth == 0) {
                            break;
                        }
                        i++;
                    }

                    sb.append(text, start, i);
                    if (i < text.length()) {
                        sb.append('}');
                        i++;
                    }
                } else {
                    sb.append('{').append(text.charAt(i + 1)).append('}');
                    i += 2;
                }
                continue;
            }

            sb.append(c);
            i++;
        }

        return sb.toString();
    }

    /**
     * 중괄호 변환
     */
    private static String translateBraces(String latex) {
        latex = latex.replaceAll("\\\\\\{", "lbrace");
        latex = latex.replaceAll("\\\\\\}", "rbrace");

        return latex;
    }

    /**
     * 특수 기호 변환
     */
    private static String translateSymbols(String latex) {
        // 공백 명령어
        latex = latex.replaceAll("\\\\qquad", "~~~~~~~~"); // 8칸 공백(~는 1칸)
        latex = latex.replaceAll("\\\\quad", "~~~~");      // 4칸 공백(~는 1칸)
        latex = latex.replaceAll("\\\\,", " ");        // 작은 공백
        latex = latex.replaceAll("\\\\;", "  ");       // 중간 공백
        latex = latex.replaceAll("\\\\:", "  ");       // 중간 공백
        latex = latex.replaceAll("\\\\!", "");         // 음수 공백 (무시)

        // 곱하기
        latex = latex.replaceAll("\\\\times", " * ");
        latex = latex.replaceAll("\\\\cdot", " * ");

        // 나누기
        latex = latex.replaceAll("\\\\div", " / ");

        // 플러스/마이너스
        latex = latex.replaceAll("\\\\pm", " +- ");
        latex = latex.replaceAll("\\\\mp", " -+ ");

        // 무한대
        latex = latex.replaceAll("\\\\infty", "infinity");

        // 각도 기호
        latex = latex.replaceAll("\\\\angle", "ANGLE");

        return latex;
    }

    /**
     * \overline 변환: \overline{AB} -> bar{AB}
     */
    private static String translateOverline(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int index = 0;
        String command = "\\overline";
        int commandLength = command.length();

        while (index < latex.length()) {
            int pos = latex.indexOf(command, index);
            if (pos == -1) {
                result.append(latex.substring(index));
                break;
            }

            result.append(latex, index, pos);
            int braceStart = pos + commandLength;

            // 공백 건너뛰기
            while (braceStart < latex.length() && Character.isWhitespace(latex.charAt(braceStart))) {
                braceStart++;
            }

            if (braceStart < latex.length() && latex.charAt(braceStart) == '{') {
                String content = extractBracedContent(latex, braceStart);
                if (content != null) {
                    result.append("bar{").append(content).append("}");
                    index = braceStart + content.length() + 2;
                    continue;
                }
            } else if (braceStart < latex.length()) {
                char single = latex.charAt(braceStart);
                result.append("bar{").append(single).append("}");
                index = braceStart + 1;
                continue;
            }

            // 실패 시 원본 유지
            result.append(command);
            index = pos + commandLength;
        }

        return result.toString();
    }

    /**
     * LaTeX 환경 제거/단순화
     */
    private static String removeLatexEnvironments(String latex) {
        // \begin{cases} ... \end{cases}
        // LaTeX: \begin{cases} a & (조건1) \\ b & (조건2) \end{cases}
        // HWP: left lbrace matrix {rows} {cols} ... right none 형태로 구성

        Pattern casesPattern = Pattern.compile("\\\\begin\\{cases\\}(.+?)\\\\end\\{cases\\}", Pattern.DOTALL);
        Matcher casesMatcher = casesPattern.matcher(latex);
        StringBuffer result = new StringBuffer();

        while (casesMatcher.find()) {
            String content = casesMatcher.group(1);
            String hwpCases = convertCasesToMatrix(content);
            casesMatcher.appendReplacement(result, Matcher.quoteReplacement(hwpCases));
        }
        casesMatcher.appendTail(result);
        latex = result.toString();

        // \begin{matrix} ... \end{matrix}
        latex = latex.replaceAll("\\\\begin\\{matrix\\}", "");
        latex = latex.replaceAll("\\\\end\\{matrix\\}", "");
        latex = latex.replaceAll("\\\\begin\\{pmatrix\\}", "(");
        latex = latex.replaceAll("\\\\end\\{pmatrix\\}", ")");
        latex = latex.replaceAll("\\\\begin\\{bmatrix\\}", "[");
        latex = latex.replaceAll("\\\\end\\{bmatrix\\}", "]");

        return latex;
    }

    /**
     * LaTeX cases 환경을 HWP matrix 구조로 변환
     * left lbrace matrix {rows} {cols} ... right none 형태 생성
     */
    private static String convertCasesToMatrix(String content) {
        if (content == null) {
            return "";
        }

        String[] rawRows = content.trim().split("\\\\\\\\");
        List<List<String>> matrixRows = new ArrayList<>();
        for (String rawRow : rawRows) {
            String row = rawRow.trim();
            if (row.isEmpty()) {
                continue;
            }

            List<String> columns = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c == '&' && (i == 0 || row.charAt(i - 1) != '\\')) {
                    columns.add(postProcessCasesCell(current.toString()));
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            columns.add(postProcessCasesCell(current.toString()));

            matrixRows.add(columns);
        }

        if (matrixRows.isEmpty()) {
            return content.trim();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{cases{");

        for (int i = 0; i < matrixRows.size(); i++) {
            List<String> columns = matrixRows.get(i);
            if (columns.isEmpty()) {
                builder.append(" ");
            }
            for (int j = 0; j < columns.size(); j++) {
                String cell = columns.get(j);
                builder.append(cell);
                if (j < columns.size() - 1) {
                    builder.append(" & ");
                }
            }
            if (i < matrixRows.size() - 1) {
                builder.append(" # ");
            }
        }

        builder.append("}}");
        return builder.toString();
    }

    /**
     * cases 셀 내용을 후처리 (trim 및 이스케이프된 & 복구)
     */
    private static String postProcessCasesCell(String cell) {
        String trimmed = cell.trim();
        return trimmed.replace("\\&", "&");
    }

    /**
     * HWP 수식에서 스크립트(첨자/지수) 주위 공백과 특수 공백(~)을 정리
     */
    private static String normalizeHwpSpacing(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < latex.length(); i++) {
            char current = latex.charAt(i);

            if ((current == '_' || current == '^')) {
                if (shouldInsertSpaceBeforeScript(builder)) {
                    builder.append(' ');
                }
                builder.append(current);
                continue;
            }

            builder.append(current);
        }

        String normalized = builder.toString();

        // 닫는 중괄호 뒤에 오는 +, - 앞에는 공백 추가
        normalized = normalized.replaceAll("\\}([\\+\\-])", "} $1");
        // \quad / \qquad 치환 결과 주위의 실제 공백 제거
        normalized = normalized.replaceAll("\\s*~~~~~~~~\\s*", "~~~~~~~~");
        normalized = normalized.replaceAll("\\s*~~~~\\s*", "~~~~");

        return normalized;
    }

    /**
     * 첨자/지수 앞에 공백을 넣어야 하는지 판단
     */
    private static boolean shouldInsertSpaceBeforeScript(StringBuilder builder) {
        if (builder.length() == 0) {
            return false;
        }

        char prev = builder.charAt(builder.length() - 1);

        if (prev == ' ' || prev == '~') {
            return false;
        }

        if (Character.isLetterOrDigit(prev)) {
            return true;
        }

        switch (prev) {
            case '}':
            case ')':
            case ']':
                return true;
            default:
                return false;
        }
    }

    /**
     * 첨자/지수 구성이 명시적인 중괄호를 사용하도록 보정
     */
    private static String ensureScriptBraces(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }

        Pattern subPattern = Pattern.compile("_(?!\\{)\\s*([A-Za-z0-9]+)");
        Matcher subMatcher = subPattern.matcher(latex);
        StringBuffer subBuffer = new StringBuffer();

        while (subMatcher.find()) {
            String replacement = "_{" + subMatcher.group(1) + "}";
            subMatcher.appendReplacement(subBuffer, Matcher.quoteReplacement(replacement));
        }
        subMatcher.appendTail(subBuffer);

        Pattern supPattern = Pattern.compile("\\^(?!\\{)\\s*([A-Za-z0-9]+)");
        Matcher supMatcher = supPattern.matcher(subBuffer.toString());
        StringBuffer supBuffer = new StringBuffer();

        while (supMatcher.find()) {
            String replacement = "^{" + supMatcher.group(1) + "}";
            supMatcher.appendReplacement(supBuffer, Matcher.quoteReplacement(replacement));
        }
        supMatcher.appendTail(supBuffer);

        return supBuffer.toString();
    }

    /**
     * 공백 정리
     */
    private static String cleanupSpaces(String latex) {
        // 연속된 공백을 하나로
        latex = latex.replaceAll("\\s+", " ");
        // 앞뒤 공백 제거
        latex = latex.trim();

        return latex;
    }

    /**
     * 테스트용 메인 함수
     */
    public static void main(String[] args) {
        // 테스트 케이스
        String[] testCases = {
            "\\frac{1}{2}",
            "\\sqrt{24}",
            "\\sqrt{24} \\times 3^3",
            "\\frac{\\sqrt{2}}{2}",
            "\\theta",
            "\\sin(x)",
            "\\lim_{h \\to 0} \\frac{f(2+h) - f(2)}{h}",
            "\\sum_{i=1}^{n} i",
            "x^{2} + y^{2}",
            "f(x) = \\begin{cases} 3x - a & (x < 2) \\\\ x^2 + a & (x \\ge 2) \\end{cases}"
        };

        System.out.println("LaTeX to HWP Translator Test\n");
        for (String test : testCases) {
            String result = translate(test);
            System.out.println("LaTeX: " + test);
            System.out.println("HWP:   " + result);
            System.out.println();
        }
    }
}
