package com.autoquickvideo;

/**
 * Represents a single media item (image or video) to be included in the output video.
 */
public class MediaItem {

    public enum MediaType {
        IMAGE, VIDEO
    }

    private final String filePath;
    private final double durationSeconds;
    private final MediaType mediaType;

    public MediaItem(String filePath, double durationSeconds) {
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
        this.mediaType = detectMediaType(filePath);
    }

    /**
     * Detects whether the file is an image or video based on its extension.
     */
    private static MediaType detectMediaType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".bmp") || lower.endsWith(".tiff") || lower.endsWith(".tif")
                || lower.endsWith(".webp") || lower.endsWith(".gif")) {
            return MediaType.IMAGE;
        }
        return MediaType.VIDEO;
    }

    public String getFilePath() {
        return filePath;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public String toString() {
        return String.format("MediaItem{file='%s', duration=%.1fs, type=%s}", filePath, durationSeconds, mediaType);
    }
}
