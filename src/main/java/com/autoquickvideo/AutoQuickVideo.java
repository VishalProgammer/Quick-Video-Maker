package com.autoquickvideo;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <h2>Auto Quick Video — Interactive Mode</h2>
 *
 * <p>Drop your images and videos into the {@code input/} folder, then run
 * the program. It will walk you through a series of prompts to configure
 * the output video.
 *
 * <p>Also supports the original CLI mode when arguments are provided.
 */
public class AutoQuickVideo {

    private static final String VERSION = "1.0.0";

    /** File extensions recognized as images. */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".tif", ".webp", ".gif");

    /** File extensions recognized as videos. */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".mov", ".avi", ".mkv", ".wmv", ".flv", ".webm", ".m4v", ".ts", ".mts");

    /** All supported extensions. */
    private static final Set<String> ALL_EXTENSIONS;
    static {
        Set<String> all = new HashSet<>();
        all.addAll(IMAGE_EXTENSIONS);
        all.addAll(VIDEO_EXTENSIONS);
        ALL_EXTENSIONS = Collections.unmodifiableSet(all);
    }

    /** Preset dimension ratios with 1080p-based resolutions. */
    private static final LinkedHashMap<String, int[]> RATIO_PRESETS = new LinkedHashMap<>();
    static {
        RATIO_PRESETS.put("16:9  (1920x1080) - YouTube / Landscape",   new int[]{1920, 1080});
        RATIO_PRESETS.put("9:16  (1080x1920) - Reels / Shorts / TikTok", new int[]{1080, 1920});
        RATIO_PRESETS.put("4:3   (1440x1080) - Classic",               new int[]{1440, 1080});
        RATIO_PRESETS.put("3:4   (1080x1440) - Portrait Classic",      new int[]{1080, 1440});
        RATIO_PRESETS.put("1:1   (1080x1080) - Instagram Square",      new int[]{1080, 1080});
        RATIO_PRESETS.put("21:9  (2560x1080) - Ultra-wide",            new int[]{2560, 1080});
    }

    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                // Legacy CLI mode: pass arguments like before
                runCli(args);
            } else {
                // Interactive mode
                runInteractive();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("\nError: " + e.getMessage());
            System.exit(1);
        } catch (IOException | InterruptedException e) {
            System.err.println("\nError: " + e.getMessage());
            System.exit(2);
        }
    }

    // =====================================================================
    //  INTERACTIVE MODE
    // =====================================================================

    private static void runInteractive() throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        printBanner();

        // ── 1. Locate & scan input folder ────────────────────────────────
        Path inputDir = Path.of("input");
        if (!Files.exists(inputDir)) {
            Files.createDirectories(inputDir);
        }

        List<Path> inputFiles = scanInputFolder(inputDir);

        if (inputFiles.isEmpty()) {
            System.out.println("  The 'input' folder is empty!");
            System.out.println("  Put your images and videos into the 'input' folder and run again.");
            System.out.println();
            System.out.println("  Supported formats:");
            System.out.println("    Images : .jpg .jpeg .png .bmp .tiff .webp .gif");
            System.out.println("    Videos : .mp4 .mov .avi .mkv .wmv .flv .webm");
            System.out.println();
            waitForEnter(scanner, "Press Enter to exit...");
            return;
        }

        System.out.println("  Found " + inputFiles.size() + " file(s) in 'input' folder:");
        System.out.println();
        for (int i = 0; i < inputFiles.size(); i++) {
            Path f = inputFiles.get(i);
            String ext = getExtension(f.getFileName().toString());
            String type = IMAGE_EXTENSIONS.contains(ext) ? "IMG" : "VID";
            System.out.printf("    %2d. [%s] %s%n", i + 1, type, f.getFileName());
        }
        System.out.println();

        // ── 2. Ask for total output video duration ───────────────────────
        System.out.println("─────────────────────────────────────────────");
        double totalDuration = askDouble(scanner,
                "  How long should the output video be (in seconds)? ",
                0.1, 36000); // up to 10 hours

        // ── 3. Ask for dimension ratio ───────────────────────────────────
        System.out.println();
        System.out.println("─────────────────────────────────────────────");
        System.out.println("  Choose a dimension ratio:");
        System.out.println();

        List<String> ratioKeys = new ArrayList<>(RATIO_PRESETS.keySet());
        for (int i = 0; i < ratioKeys.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, ratioKeys.get(i));
        }
        System.out.printf("    %d. Custom (enter your own width x height)%n", ratioKeys.size() + 1);
        System.out.println();

        int ratioChoice = askInt(scanner, "  Your choice [1-" + (ratioKeys.size() + 1) + "]: ",
                1, ratioKeys.size() + 1);

        int width, height;
        if (ratioChoice <= ratioKeys.size()) {
            int[] dims = RATIO_PRESETS.get(ratioKeys.get(ratioChoice - 1));
            width = dims[0];
            height = dims[1];
        } else {
            width = askInt(scanner, "  Enter width  (pixels): ", 100, 7680);
            height = askInt(scanner, "  Enter height (pixels): ", 100, 4320);
        }

        // ── 4. Ask for per-item duration ─────────────────────────────────
        System.out.println();
        System.out.println("─────────────────────────────────────────────");
        double autoPerItem = Math.round(totalDuration / inputFiles.size() * 10.0) / 10.0;
        System.out.printf("  With %d files and %.1fs total, each item would be ~%.1fs.%n",
                inputFiles.size(), totalDuration, autoPerItem);
        System.out.println();
        System.out.println("  How long should each image/video be on screen (in seconds)?");
        System.out.printf("  (Press Enter to use auto-calculated %.1fs): ", autoPerItem);

        String perItemInput = scanner.nextLine().trim();
        double perItemDuration;
        if (perItemInput.isEmpty()) {
            perItemDuration = autoPerItem;
        } else {
            try {
                perItemDuration = Double.parseDouble(perItemInput);
                if (perItemDuration <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input, using auto-calculated value: " + autoPerItem + "s");
                perItemDuration = autoPerItem;
            }
        }

        // ── 5. Build media items list ────────────────────────────────────
        List<MediaItem> items = new ArrayList<>();
        for (Path file : inputFiles) {
            items.add(new MediaItem(file.toAbsolutePath().toString(), perItemDuration));
        }

        // Compute actual total duration
        double actualTotal = items.size() * perItemDuration;

        // ── 6. Show summary & confirm ────────────────────────────────────
        System.out.println();
        System.out.println("═════════════════════════════════════════════");
        System.out.println("  SUMMARY");
        System.out.println("═════════════════════════════════════════════");
        System.out.printf("  Resolution      : %d x %d%n", width, height);
        System.out.printf("  Per-item duration: %.1fs%n", perItemDuration);
        System.out.printf("  Total duration   : %.1fs  (%d files x %.1fs)%n",
                actualTotal, items.size(), perItemDuration);
        // ── 7. Create output directory & find next available filename ──
        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);
        Path outputFile = nextAvailablePath(outputDir, "output", ".mp4");
        String outputPath = outputFile.toAbsolutePath().toString();

        System.out.printf("  Output file      : %s%n", outputDir.relativize(outputFile).toString().isEmpty()
                ? outputFile.getFileName() : "output/" + outputFile.getFileName());
        System.out.println("═════════════════════════════════════════════");
        System.out.println();

        waitForEnter(scanner, "  Press Enter to start processing (or Ctrl+C to cancel)...");
        System.out.println();

        VideoConfig config = new VideoConfig(width, height, 30, outputPath);
        VideoAssembler assembler = new VideoAssembler(config, items, false);

        long startMs = System.currentTimeMillis();
        assembler.assemble();
        long elapsed = System.currentTimeMillis() - startMs;

        System.out.println();
        System.out.println("═════════════════════════════════════════════");
        System.out.printf("  DONE!  Completed in %.1f seconds.%n", elapsed / 1000.0);
        System.out.println("  Output saved to: output/" + outputFile.getFileName());
        System.out.println("═════════════════════════════════════════════");
        System.out.println();
        waitForEnter(scanner, "  Press Enter to exit...");
    }

    // =====================================================================
    //  CLI MODE (original behavior, kept for advanced usage)
    // =====================================================================

    private static void runCli(String[] args) throws IOException, InterruptedException {
        // ── Defaults ────────────────────────────────────────────────────
        int width = 1920;
        int height = 1080;
        int fps = 30;
        String outputPath = "output.mp4";
        boolean verbose = false;
        List<MediaItem> items = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printCliHelp();
                    return;
                case "--version":
                    System.out.println("Auto Quick Video v" + VERSION);
                    return;
                case "-w": case "--width":
                    width = parseIntArg(args, ++i, "width");
                    break;
                case "-h": case "--height":
                    height = parseIntArg(args, ++i, "height");
                    break;
                case "-f": case "--fps":
                    fps = parseIntArg(args, ++i, "fps");
                    break;
                case "-o": case "--output":
                    outputPath = requireArg(args, ++i, "output");
                    break;
                case "-v": case "--verbose":
                    verbose = true;
                    break;
                default:
                    items.add(parseMediaItem(args[i]));
                    break;
            }
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("No input files specified.");
        }
        for (MediaItem item : items) {
            if (!Files.exists(Path.of(item.getFilePath()))) {
                throw new IllegalArgumentException("Input file not found: " + item.getFilePath());
            }
        }

        // Auto-increment output filename to avoid overwriting
        Path outPath = Path.of(outputPath);
        Path outDir = outPath.getParent() != null ? outPath.getParent() : Path.of(".");
        String outName = outPath.getFileName().toString();
        int dotIdx = outName.lastIndexOf('.');
        String baseName = dotIdx > 0 ? outName.substring(0, dotIdx) : outName;
        String ext = dotIdx > 0 ? outName.substring(dotIdx) : ".mp4";
        Path resolvedOutput = nextAvailablePath(outDir, baseName, ext);
        outputPath = resolvedOutput.toAbsolutePath().toString();

        VideoConfig config = new VideoConfig(width, height, fps, outputPath);
        System.out.printf("Resolution: %dx%d @ %dfps | %d file(s) -> %s%n",
                config.getWidth(), config.getHeight(), config.getFps(), items.size(), resolvedOutput.getFileName());

        VideoAssembler assembler = new VideoAssembler(config, items, verbose);
        long startMs = System.currentTimeMillis();
        assembler.assemble();
        System.out.printf("%nCompleted in %.1f seconds.%n", (System.currentTimeMillis() - startMs) / 1000.0);
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    /** Scans the input folder for supported image/video files, sorted by name. */
    private static List<Path> scanInputFolder(Path dir) throws IOException {
        try (Stream<Path> walk = Files.list(dir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String ext = getExtension(p.getFileName().toString());
                        return ALL_EXTENSIONS.contains(ext);
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    /** Returns the lowercase file extension including the dot, e.g. ".jpg". */
    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }

    /**
     * Finds the next available filename in a directory to avoid overwriting.
     * If {@code dir/base.ext} doesn't exist, returns that.
     * Otherwise tries {@code dir/base_2.ext}, {@code dir/base_3.ext}, etc.
     */
    private static Path nextAvailablePath(Path dir, String baseName, String ext) {
        Path candidate = dir.resolve(baseName + ext);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        int counter = 2;
        while (true) {
            candidate = dir.resolve(baseName + "_" + counter + ext);
            if (!Files.exists(candidate)) {
                return candidate;
            }
            counter++;
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════╗");
        System.out.println("  ║       AUTO QUICK VIDEO  v" + VERSION + "          ║");
        System.out.println("  ╠═══════════════════════════════════════════╣");
        System.out.println("  ║  Assemble images & videos into one clip  ║");
        System.out.println("  ║  Zoom-to-fill — no black bars!           ║");
        System.out.println("  ╚═══════════════════════════════════════════╝");
        System.out.println();
    }

    /** Asks for a double within [min, max]. Repeats until valid. */
    private static double askDouble(Scanner scanner, String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(line);
                if (val >= min && val <= max) return val;
                System.out.printf("  Please enter a value between %.1f and %.0f.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number. Please try again.");
            }
        }
    }

    /** Asks for an int within [min, max]. Repeats until valid. */
    private static int askInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                System.out.printf("  Please enter a value between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number. Please try again.");
            }
        }
    }

    private static void waitForEnter(Scanner scanner, String message) {
        System.out.print(message);
        scanner.nextLine();
    }

    // ─── CLI argument helpers ───────────────────────────────────────────

    private static MediaItem parseMediaItem(String token) {
        int colonIdx = token.lastIndexOf(':');
        if (colonIdx <= 0 || colonIdx == token.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid input format: '" + token + "'. Expected file:duration (e.g. photo.jpg:5)");
        }
        String path = token.substring(0, colonIdx);
        double duration = Double.parseDouble(token.substring(colonIdx + 1));
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive in: " + token);
        return new MediaItem(path, duration);
    }

    private static int parseIntArg(String[] args, int idx, String name) {
        return Integer.parseInt(requireArg(args, idx, name));
    }

    private static String requireArg(String[] args, int idx, String name) {
        if (idx >= args.length) throw new IllegalArgumentException("Missing value for --" + name);
        return args[idx];
    }

    private static void printCliHelp() {
        System.out.println("""
                Auto Quick Video v%s
                
                INTERACTIVE MODE (recommended):
                  1. Put images/videos into the 'input' folder
                  2. Run: java -jar auto-quick-video.jar
                  3. Follow the prompts!
                
                CLI MODE:
                  java -jar auto-quick-video.jar [OPTIONS] file1:duration file2:duration ...
                
                OPTIONS:
                  -w, --width   WIDTH    Output width in pixels       (default: 1920)
                  -h, --height  HEIGHT   Output height in pixels      (default: 1080)
                  -f, --fps     FPS      Frame rate                    (default: 30)
                  -o, --output  FILE     Output path                   (default: output.mp4)
                  -v, --verbose          Show FFmpeg output
                      --help             Show this help
                """.formatted(VERSION));
    }
}
