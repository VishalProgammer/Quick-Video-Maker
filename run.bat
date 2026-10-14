@echo off
setlocal

:: Build first if JAR doesn't exist
if not exist "auto-quick-video.jar" (
    echo JAR not found, building first...
    call build.bat
    echo.
)

:: Create input folder if it doesn't exist
if not exist "input" mkdir "input"

:: Create output folder if it doesn't exist
if not exist "output" mkdir "output"

:: Refresh PATH so ffmpeg is found (in case it was just installed)
set "PATH=%PATH%;C:\Program Files\FFmpeg\bin"

:: Run the interactive tool
java -jar auto-quick-video.jar

endlocal
pause
