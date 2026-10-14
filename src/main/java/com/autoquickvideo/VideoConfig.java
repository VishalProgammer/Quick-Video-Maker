package com.autoquickvideo;

/**
 * Holds the output video configuration: resolution, frame rate, and codec settings.
 */
public class VideoConfig {

    private final int width;
    private final int height;
    private final int fps;
    private final String outputPath;

    public VideoConfig(int width, int height, int fps, String outputPath) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive. Got " + width + "x" + height);
        }
        if (fps <= 0) {
            throw new IllegalArgumentException("FPS must be positive. Got " + fps);
        }
        // FFmpeg's libx264 requires even dimensions
        this.width = width % 2 == 0 ? width : width + 1;
        this.height = height % 2 == 0 ? height : height + 1;
        this.fps = fps;
        this.outputPath = outputPath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFps() {
        return fps;
    }

    public String getOutputPath() {
        return outputPath;
    }

    /** Returns the FFmpeg video-filter string that scales any input to zoom-to-fill this resolution. */
    public String getScaleCropFilter() {
        // scale so the *smaller* dimension matches, then center-crop to exact size
        return String.format(
                "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d:(iw-%d)/2:(ih-%d)/2,setsar=1",
                width, height, width, height, width, height);
    }

    @Override
    public String toString() {
        return String.format("VideoConfig{%dx%d @%dfps -> %s}", width, height, fps, outputPath);
    }
}
