@echo off
chcp 65001 >nul
$env:JAVA_HOME="C:\Users\12209\.jdks\jdk-17.0.15+6"; .\gradlew assembleRelease --no-daemon 2>&1
$env:JAVA_HOME="C:\Users\12209\.jdks\jdk-17.0.15+6"; .\gradlew assembleMobilePythonArm64_v8aRelease --no-daemon 2>&1
 
pause