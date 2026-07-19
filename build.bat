@echo off
chcp 65001 >nul
set JAVA_HOME=C:\Users\12209\.jdks\jdk-17.0.15+6
echo Cleaning project...
.\gradlew clean --no-daemon 2>&1
if %errorlevel% neq 0 (
    echo Clean failed, but continuing...
)
echo Building project...
.\gradlew assembleRelease --no-daemon 2>&1
pause
