# Auto Quick Video

A Java CLI tool that assembles images and video clips into a single output video. Each input is **zoom-cropped** to perfectly fill the output resolution — no black bars, no letterboxing.

## Requirements

- **Java 17+**
- **FFmpeg** — must be installed and on your `PATH`

### Installing FFmpeg

```
winget install Gyan.FFmpeg
```
Then restart your terminal.

## Quick Start

1. **Build** (one time):
   ```
   build.bat
   ```

2. **Drop your files** into the `input/` folder (images + videos)

3. **Double-click `run.bat`** — it will ask you:
   - How long the total video should be (seconds)
   - Which dimension ratio (16:9, 9:16, 1:1, etc.)
   - How long each image/video stays on screen

4. **Output** is saved to `output/output.mp4`

## Interactive Prompts

```
  Found 5 file(s) in 'input' folder:

     1. [IMG] 01_intro.jpg
     2. [VID] 02_clip.mp4
     3. [IMG] 03_transition.png
     4. [VID] 04_main.mp4
     5. [IMG] 05_outro.jpg

  How long should the output video be (in seconds)? 30

  Choose a dimension ratio:
    1. 16:9  (1920x1080) - YouTube / Landscape
    2. 9:16  (1080x1920) - Reels / Shorts / TikTok
    3. 4:3   (1440x1080) - Classic
    4. 3:4   (1080x1440) - Portrait Classic
    5. 1:1   (1080x1080) - Instagram Square
    6. 21:9  (2560x1080) - Ultra-wide
    7. Custom (enter your own width x height)

  Your choice [1-7]: 1

  With 5 files and 30.0s total, each item would be ~6.0s.
  How long should each image/video be on screen (in seconds)?
  (Press Enter to use auto-calculated 6.0s):
```

## Supported Formats

| Type | Extensions |
|------|-----------|
| Images | `.jpg` `.jpeg` `.png` `.bmp` `.tiff` `.webp` `.gif` |
| Videos | `.mp4` `.mov` `.avi` `.mkv` `.wmv` `.flv` `.webm` `.m4v` |

## CLI Mode (Advanced)

You can also use it with command-line arguments:

```
java -jar auto-quick-video.jar -w 1280 -h 720 -o my_video.mp4 photo.jpg:5 clip.mp4:10 outro.png:3
```

Run `java -jar auto-quick-video.jar --help` for all options.

## Project Structure

```
auto quick video/
├── input/                     ← Put your images & videos here
├── output/                    ← Output video goes here
├── src/main/java/com/autoquickvideo/
│   ├── AutoQuickVideo.java    ← Interactive prompts & CLI parsing
│   ├── MediaItem.java         ← Input file model
│   ├── VideoConfig.java       ← Resolution & FFmpeg filter config
│   └── VideoAssembler.java    ← FFmpeg orchestration engine
├── run.bat                    ← Double-click to run
├── build.bat                  ← Compile & package JAR
└── auto-quick-video.jar       ← Compiled tool
```
