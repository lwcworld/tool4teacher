import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point for refining dotsocr JSON outputs into text/math segments.
 *
 * Usage:
 *   java refine_json <input.json|input_dir> [output_dir]
 */
public class refine_json {

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java refine_json <input.json> [output_dir]");
        System.out.println("  java refine_json <input_dir> [output_dir]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java refine_json ../../dataset/downloads/suneung/수학영역_문제지/page_0001_0001.json");
        System.out.println("  java refine_json ../../dataset/downloads/suneung/수학영역_문제지/");
        System.out.println();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String targetPath = args[0];

        if (!Files.exists(Paths.get(targetPath))) {
            System.err.printf("Input path not found: %s%n", targetPath);
            System.exit(1);
        }

        try {
            JsonRefiner.processPath(targetPath, null);
        } catch (IOException e) {
            System.err.printf("Refinement failed: %s%n", e.getMessage());
            System.exit(1);
        }

        Path parent = Paths.get(targetPath).toAbsolutePath().getParent();
        if (parent != null) {
            System.out.printf("Refined files saved alongside originals in %s%n", parent);
        }
    }
}
