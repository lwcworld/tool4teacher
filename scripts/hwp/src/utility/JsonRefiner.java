import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility for refining dotsocr JSON outputs into text/math segments suitable for HWPX authoring.
 *
 * Heuristics:
 *  - Existing LaTeX blocks (delimited by $...$, $$...$$, \(..\), \[..\]) are preserved as math.
 *  - Digits, latin letters, LaTeX commands, and typical math symbols are grouped into math segments.
 *  - Korean text and circled-number bullets remain as text.
 *  - Leading/trailing spaces around math segments are preserved outside of $...$ delimiters.
 *
 * Usage (via refine_json.java):
 *   mvn compile exec:java -Dexec.mainClass="refine_json" \
 *     -Dexec.args="path/to/page_0001_0001.json output/refined"
 */
public class JsonRefiner {

    public static class Segment {
        final String type;
        final String value;

        Segment(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    private static final Pattern INLINE_MATH_PATTERN = Pattern.compile(
            "(\\$\\$.*?\\$\\$|\\$.*?\\$|\\\\\\(.*?\\\\\\)|\\\\\\[.*?\\\\\\])",
            Pattern.DOTALL
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\s+"
                    + "|\\\\[A-Za-z]+"
                    + "|\\d+[0-9A-Za-z/_^]*"
                    + "|[A-Za-z]+[0-9A-Za-z/_^]*"
                    + "|[\\u3131-\\u318E\\uAC00-\\uD7A3]+"
                    + "|[\\u2460-\\u24FF]"
                    + "|."
    );

    private static final Pattern TEXT_MACRO_PATTERN = Pattern.compile("\\\\text\\s*\\{");

    private static final Pattern PURE_NUMBER = Pattern.compile("^\\d+(?:[.,]\\d+)?(?:[/_^][0-9A-Za-z]+)*$");
    private static final Pattern LATIN_WORD = Pattern.compile("^[A-Za-z]+[0-9A-Za-z/_^]*$");
    private static final Pattern KOREAN_CHARS = Pattern.compile("^[\\u1100-\\u11FF\\u3130-\\u318F\\uAC00-\\uD7A3]+$");
    private static final Pattern SUB_SUPERSCRIPT_CHARS = Pattern.compile(".*[\\u2070-\\u209F].*");

    private static final char[] MATH_SYMBOLS = (
            "+-±×÷*/=<>≤≥≠∑√∫∮∏∂∇∠∥∧∨∩∪≈≅≡∀∃∝∞π·:^_{}[]()|%" +
            "→←↔⇔∞∴∵≒≡≜≞≟⊂⊃⊆⊇⊕⊗⊥∥∦∥∠⋯∑∏∐∫∬∭∮∯∰∇∂∂∝∆∈∉∋∌⊆⊇⊂⊃"
    ).toCharArray();

    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static boolean containsMathSymbol(String token) {
        for (char c : token.toCharArray()) {
            for (char mathChar : MATH_SYMBOLS) {
                if (c == mathChar) {
                    return true;
                }
            }
            if ("^_{}[]()|/\\<>×÷".indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCircledNumber(String token) {
        if (token.length() != 1) {
            return false;
        }
        int codePoint = token.codePointAt(0);
        return (codePoint >= 0x2460 && codePoint <= 0x2473) || (codePoint >= 0x3251 && codePoint <= 0x325F);
    }

    private static List<Segment> refinePlainChunk(String chunk) {
        if (chunk.isEmpty()) {
            return Collections.emptyList();
        }

        List<Segment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentType = null;

        Matcher matcher = TOKEN_PATTERN.matcher(chunk);
        while (matcher.find()) {
            String token = matcher.group();
            int tokenStart = matcher.start();
            int tokenEnd = matcher.end();
            String tokenType = classifyToken(token, chunk, tokenStart, tokenEnd, currentType);

            if (tokenType == null) {
                continue;
            }

            if (currentType == null || !currentType.equals(tokenType)) {
                if (currentType != null && current.length() > 0) {
                    segments.add(new Segment(currentType, current.toString()));
                }
                current.setLength(0);
                current.append(token);
                currentType = tokenType;
            } else {
                current.append(token);
            }
        }

        if (currentType != null && current.length() > 0) {
            segments.add(new Segment(currentType, current.toString()));
        }

        return segments;
    }

    private static boolean isProblemNumberContext(String chunk, int tokenStart, int tokenEnd) {
        int idx = tokenStart;
        while (idx > 0 && Character.isWhitespace(chunk.charAt(idx - 1))) {
            idx--;
        }
        if (idx != 0) {
            return false;
        }

        int next = tokenEnd;
        while (next < chunk.length() && Character.isWhitespace(chunk.charAt(next))) {
            next++;
        }
        if (next >= chunk.length()) {
            return false;
        }
        char nextChar = chunk.charAt(next);
        return nextChar == '.' || nextChar == ')' || nextChar == '．';
    }

    private static String classifyToken(String token, String chunk, int tokenStart, int tokenEnd, String currentType) {
        if (token == null || token.isEmpty()) {
            return currentType == null ? "text" : currentType;
        }

        if (token.trim().isEmpty()) {
            return currentType == null ? "text" : currentType;
        }

        if (isCircledNumber(token)) {
            return "text";
        }

        if (token.startsWith("\\")) {
            return "math";
        }

        if (token.contains("_") || token.contains("^")) {
            return "math";
        }

        if (PURE_NUMBER.matcher(token).matches()) {
            if (isProblemNumberContext(chunk, tokenStart, tokenEnd)) {
                return "text";
            }
            return "math";
        }

        if (LATIN_WORD.matcher(token).matches()) {
            return "math";
        }

        if (containsMathSymbol(token)) {
            return "math";
        }

        if (SUB_SUPERSCRIPT_CHARS.matcher(token).matches()) {
            return "math";
        }

        if (KOREAN_CHARS.matcher(token).matches()) {
            return "text";
        }

        if (Character.isDigit(token.codePointAt(0))) {
            return "math";
        }

        return "text";
    }

    private static String stripMathDelimiters(String chunk) {
        String trimmed = chunk.trim();
        if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length() >= 4) {
            return trimmed.substring(2, trimmed.length() - 2).trim();
        }
        if (trimmed.startsWith("$") && trimmed.endsWith("$") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.startsWith("\\(") && trimmed.endsWith("\\)") && trimmed.length() >= 4) {
            return trimmed.substring(2, trimmed.length() - 2).trim();
        }
        if (trimmed.startsWith("\\[") && trimmed.endsWith("\\]") && trimmed.length() >= 4) {
            return trimmed.substring(2, trimmed.length() - 2).trim();
        }
        return trimmed;
    }

    private static String extractLeadingWhitespace(String value) {
        int idx = 0;
        while (idx < value.length() && Character.isWhitespace(value.charAt(idx))) {
            idx++;
        }
        return value.substring(0, idx);
    }

    private static String extractTrailingWhitespace(String value) {
        int idx = value.length() - 1;
        while (idx >= 0 && Character.isWhitespace(value.charAt(idx))) {
            idx--;
        }
        return value.substring(idx + 1);
    }

    private static void appendText(List<Segment> segments, String text) {
        if (text.isEmpty()) {
            return;
        }
        if (!segments.isEmpty()) {
            Segment last = segments.get(segments.size() - 1);
            if ("text".equals(last.type)) {
                segments.set(segments.size() - 1, new Segment("text", last.value + text));
                return;
            }
        }
        segments.add(new Segment("text", text));
    }

    private static List<Segment> normalizeMathSegments(List<Segment> segments) {
        List<Segment> normalized = new ArrayList<>();

        for (Segment segment : segments) {
            if (!"math".equals(segment.type)) {
                appendText(normalized, segment.value);
                continue;
            }

            String leadingWs = extractLeadingWhitespace(segment.value);
            String trailingWs = extractTrailingWhitespace(segment.value);
            String core = segment.value.substring(leadingWs.length(),
                    segment.value.length() - trailingWs.length());

            appendText(normalized, leadingWs);

            String trimmedCore = core.trim();
            if (!trimmedCore.isEmpty()) {
                normalized.add(new Segment("math", trimmedCore));
            }

            appendText(normalized, trailingWs);
        }

        return normalized;
    }

    private static List<Segment> mergeSegments(List<Segment> segments) {
        List<Segment> merged = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.value == null || segment.value.isEmpty()) {
                continue;
            }
            if (!merged.isEmpty()) {
                Segment last = merged.get(merged.size() - 1);
                if (last.type.equals(segment.type)) {
                    merged.set(merged.size() - 1, new Segment(
                            last.type,
                            last.value + segment.value
                    ));
                    continue;
                }
            }
            merged.add(segment);
        }
        return merged;
    }

    public static List<Segment> refineText(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.singletonList(new Segment("text", text == null ? "" : text));
        }

        if (text.trim().matches("^\\d+\\.?$")) {
            return Collections.singletonList(new Segment("text", text));
        }

        List<Segment> segments = new ArrayList<>();
        Matcher matcher = INLINE_MATH_PATTERN.matcher(text);
        int index = 0;

        while (matcher.find()) {
            if (matcher.start() > index) {
                String plain = text.substring(index, matcher.start());
                segments.addAll(refinePlainChunk(plain));
            }

            String mathChunk = stripMathDelimiters(matcher.group());
            segments.addAll(refineMathChunk(mathChunk));
            index = matcher.end();
        }

        if (index < text.length()) {
            segments.addAll(refinePlainChunk(text.substring(index)));
        }

        segments = normalizeMathSegments(segments);
        segments = mergeSegments(segments);
        return segments;
    }

    private static void enrichElement(JsonObject element) {
        if (!element.has("text") || element.get("text").isJsonNull()) {
            return;
        }

        String originalText = element.get("text").getAsString();
        List<Segment> segments = refineText(originalText);

        JsonArray refinedSegments = new JsonArray();
        StringBuilder builder = new StringBuilder();

        for (Segment segment : segments) {
            JsonObject segObj = new JsonObject();
            segObj.addProperty("type", segment.type);
            String value = "math".equals(segment.type)
                    ? "$" + segment.value + "$"
                    : segment.value;
            segObj.addProperty("value", value);
            refinedSegments.add(segObj);
            builder.append(value);
        }

        element.add("refined_segments", refinedSegments);
        element.addProperty("refined_text", builder.toString());
    }

    public static Path processFile(Path inputPath, Path outputDirHint) throws IOException {
        JsonArray elements;
        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            elements = JsonParser.parseReader(reader).getAsJsonArray();
        }

        int refinedCount = 0;
        for (JsonElement jsonElement : elements) {
            if (jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                if (obj.has("text")) {
                    refinedCount++;
                }
                enrichElement(obj);
            }
        }

        Path resolvedOutputDir = inputPath.getParent();
        if (resolvedOutputDir == null) {
            resolvedOutputDir = outputDirHint;
        }
        if (resolvedOutputDir != null) {
            Files.createDirectories(resolvedOutputDir);
        } else {
            throw new IOException("Cannot determine output directory for " + inputPath);
        }

        String baseName = inputPath.getFileName().toString();
        if (baseName.endsWith(".json")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        Path outputFile = resolvedOutputDir.resolve(baseName + "_refine.json");

        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            PRETTY_GSON.toJson(elements, writer);
        }

        System.out.printf("  • %s -> %s (%d entries)%n",
                inputPath.getFileName(),
                outputFile.getFileName(),
                refinedCount);

        return outputFile;
    }

    public static void processPath(String targetPath, String outputDir) throws IOException {
        Path input = Paths.get(targetPath);
        Path outDir = outputDir == null ? null : Paths.get(outputDir);

        if (Files.isDirectory(input)) {
            List<Path> jsonFiles = new ArrayList<>();
            try (Stream<Path> stream = Files.list(input)) {
                stream.filter(path -> {
                            String name = path.getFileName().toString();
                            return name.endsWith(".json") && !name.endsWith("_refine.json");
                        })
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(jsonFiles::add);
            }

            if (jsonFiles.isEmpty()) {
                System.err.printf("No JSON files found in %s%n", input);
                return;
            }

            System.out.printf("Processing %d JSON files in %s%n", jsonFiles.size(), input);
            for (Path jsonFile : jsonFiles) {
                processFile(jsonFile, outDir);
            }
        } else if (Files.isRegularFile(input)) {
            processFile(input, outDir);
        } else {
            throw new IOException("Path not found: " + input);
        }
    }

    private static int findMatchingBrace(String source, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<Segment> refineMathChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return Collections.singletonList(new Segment("math", ""));
        }

        List<Segment> segments = new ArrayList<>();
        int index = 0;

        while (index < chunk.length()) {
            Matcher matcher = TEXT_MACRO_PATTERN.matcher(chunk);
            if (!matcher.find(index)) {
                String remainder = chunk.substring(index);
                if (!remainder.isEmpty()) {
                    segments.add(new Segment("math", remainder));
                }
                break;
            }

            int start = matcher.start();
            if (start > index) {
                String mathPart = chunk.substring(index, start);
                if (!mathPart.isEmpty()) {
                    segments.add(new Segment("math", mathPart));
                }
            }

            int openBraceIndex = matcher.end() - 1;
            int closeBraceIndex = findMatchingBrace(chunk, openBraceIndex);

            if (closeBraceIndex == -1) {
                String remainder = chunk.substring(start);
                if (!remainder.isEmpty()) {
                    segments.add(new Segment("math", remainder));
                }
                break;
            }

            String textContent = chunk.substring(openBraceIndex + 1, closeBraceIndex);
            if (!textContent.isEmpty()) {
                textContent = textContent.replace("\\\\", "\n");
                segments.add(new Segment("text", textContent));
            }

            index = closeBraceIndex + 1;
        }

        List<Segment> refined = new ArrayList<>();
        for (Segment segment : segments) {
            if ("math".equals(segment.type)) {
                refined.addAll(refinePlainChunk(segment.value));
            } else {
                refined.add(segment);
            }
        }

        return refined;
    }

}
