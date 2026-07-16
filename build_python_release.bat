@echo off
chcp 65001 >nul
$env:JAVA_HOME="C:\Users\12209\.jdks\jdk-17.0.15+6"; .\gradlew assembleRelease --no-daemon 2>&1
pause