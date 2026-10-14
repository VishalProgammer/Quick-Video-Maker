@echo off
setlocal

set JDK_BIN=C:\Program Files\Java\jdk-26.0.1\bin

echo ════════════════════════════════════════════
echo   Building Auto Quick Video...
echo ════════════════════════════════════════════

:: Clean previous build
if exist "build" rmdir /s /q "build"
mkdir "build\classes"

:: Compile
echo Compiling Java sources...
"%JDK_BIN%\javac" -d build\classes src\main\java\com\autoquickvideo\*.java
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed.
    exit /b 1
)

:: Create manifest
echo Main-Class: com.autoquickvideo.AutoQuickVideo> build\MANIFEST.MF

:: Package JAR
echo Packaging JAR...
cd build\classes
"%JDK_BIN%\jar" cfm ..\auto-quick-video.jar ..\MANIFEST.MF com\autoquickvideo\*.class
if %errorlevel% neq 0 (
    echo ERROR: JAR packaging failed.
    cd ..\..
    exit /b 1
)
cd ..\..

:: Copy JAR to project root
copy /y build\auto-quick-video.jar auto-quick-video.jar >nul

echo.
echo ════════════════════════════════════════════
echo   Build successful!
echo   JAR: auto-quick-video.jar
echo ════════════════════════════════════════════
echo.
echo Usage:
echo   java -jar auto-quick-video.jar --help

endlocal
