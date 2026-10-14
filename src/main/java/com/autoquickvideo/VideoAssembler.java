package com.autoquickvideo;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Assembles individual media items into a single output video using FFmpeg.
 *
 * <p>Workflow:
 * <ol>
 *   <li>For each {@link MediaItem}, produce a temporary clip scaled/cropped to
 *       the target resolution.</li>
 *   <li>Write an FFmpeg concat-demuxer manifest listing all clips.</li>
 *   <li>Concatenate the clips into the final output file.</li>
 *   <li>Clean up temporary files.</li>
 * </ol>
 */
public class VideoAssembler {

    private final VideoConfig config;
    private final List<MediaItem> items;
    private final Path tempDir;
    private final boolean verbose;

    public VideoAssembler(VideoConfig config, List<MediaItem> items, boolean verbose) {
        this.config = config;
        this.items = items;
        this.verbose = verbose;
        this.tempDir = Path.of(System.getProperty("java.io.tmpdir"),
                "aqv_" + UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Runs the full assembly pipeline and returns the path to the output file.
     */
    public String assemble() throws IOException, InterruptedException {
        Files.createDirectories(tempDir);
        log("Temporary directory: " + tempDir);

        try {
            // 1. Verify FFmpeg is available
            verifyFfmpeg();

            // 2. Build individual clips
            List<Path> clips = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                MediaItem item = items.get(i);
                Path clipPath = tempDir.resolve(String.format("clip_%03d.mp4", i));
                log(String.format("[%d/%d] Processing %s", i + 1, items.size(), item));
                buildClip(item, clipPath);
                clips.add(clipPath);
            }

            // 3. Write concat manifest
            Path manifest = tempDir.resolve("concat.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(manifest)) {
                for (Path clip : clips) {
                    // FFmpeg concat demuxer requires forward slashes and escaped quotes
                    bw.write("file '" + clip.toAbsolutePath().toString().replace("\\", "/") + "'");
                    bw.newLine();
                }
            }
            log("Concat manifest written: " + manifest);

            // 4. Concatenate into final output
            concatenate(manifest);

            log("Done! Output saved to: " + config.getOutputPath());
            return config.getOutputPath();
        } finally {
            cleanup();
        }
    }

    // ─── Internal helpers ───────────────────────────────────────────────

    private void verifyFfmpeg() throws IOException, InterruptedException {
        try {
            int code = runProcess(List.of("ffmpeg", "-version"), true);
            if (code != 0) {
                throw new IOException("ffmpeg exited with code " + code);
            }
        } catch (IOException e) {
            throw new IOException(
                    "FFmpeg not found on PATH. Please install FFmpeg (https://ffmpeg.org/download.html) "
                            + "and make sure `ffmpeg` is accessible from the command line.", e);
        }
    }

    /**
     * Builds a single clip from a {@link MediaItem}, scaled and cropped to the
     * target resolution.
     */
    private void buildClip(MediaItem item, Path output) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y"); // overwrite without asking

        String vf = config.getScaleCropFilter();

        if (item.getMediaType() == MediaItem.MediaType.IMAGE) {
            // -loop 1: treat the image as an infinite stream
            // -t: duration
            cmd.addAll(List.of(
                    "-loop", "1",
                    "-i", item.getFilePath(),
                    "-t", String.valueOf(item.getDurationSeconds()),
                    "-vf", vf,
                    "-r", String.valueOf(config.getFps()),
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-pix_fmt", "yuv420p",
                    "-an", // no audio for image clips
                    output.toAbsolutePath().toString()
            ));
        } else {
            // Video input
            cmd.addAll(List.of(
                    "-i", item.getFilePath(),
                    "-t", String.valueOf(item.getDurationSeconds()),
                    "-vf", vf,
                    "-r", String.valueOf(config.getFps()),
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-pix_fmt", "yuv420p",
                    "-an", // strip audio to avoid concat issues
                    output.toAbsolutePath().toString()
            ));
        }

        int exitCode = runProcess(cmd, false);
        if (exitCode != 0) {
            throw new IOException("FFmpeg failed (exit " + exitCode + ") while processing: " + item.getFilePath());
        }
    }

    /**
     * Concatenates all temporary clips into the final output using the concat
     * demuxer.
     */
    private void concatenate(Path manifest) throws IOException, InterruptedException {
        log("Concatenating clips into final output...");
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", manifest.toAbsolutePath().toString(),
                "-c", "copy",
                config.getOutputPath()
        );
        int exitCode = runProcess(cmd, false);
        if (exitCode != 0) {
            throw new IOException("FFmpeg concat failed with exit code " + exitCode);
        }
    }

    /**
     * Executes a system process and optionally suppresses its output.
     *
     * @param quiet if true, discard stdout/stderr; if false, respect verbose flag
     * @return the process exit code
     */
    private int runProcess(List<String> command, boolean quiet) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (quiet || !verbose) {
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // Drain output to prevent blocking
            try (InputStream is = p.getInputStream()) {
                is.readAllBytes();
            }
            return p.waitFor();
        } else {
            pb.inheritIO();
            Process p = pb.start();
            return p.waitFor();
        }
    }

    /** Removes all temporary files. */
    private void cleanup() {
        try {
            if (Files.exists(tempDir)) {
                try (var walk = Files.walk(tempDir)) {
                    walk.sorted((a, b) -> b.compareTo(a)) // deepest first
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
                log("Cleaned up temp directory.");
            }
        } catch (IOException e) {
            System.err.println("Warning: could not clean up temp dir " + tempDir + ": " + e.getMessage());
        }
    }

    private void log(String msg) {
        System.out.println("[AutoQuickVideo] " + msg);
    }
}
