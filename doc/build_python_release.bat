@echo off
chcp 65001 >nul
$env:JAVA_HOME="C:\Users\12209\.jdks\jdk-17.0.15+6"; .\gradlew assembleMobilePythonArm64_v8aRelease
$env:JAVA_HOME="C:\Users\12209\.jdks\jbr-21.0.7";
$env:JAVA_HOME="C:\Users\12209\.jdks\jdk-17.0.15+6"; 

.\gradlew assembleMobileJavaArm64_v8aRelease
.\gradlew assembleMobileJavaArmeabi_v7aRelease
.\gradlew assembleMobileJavaX86Release
.\gradlew assembleMobilePythonArm64_v8aRelease
.\gradlew assembleMobilePythonArmeabi_v7aRelease
.\gradlew assembleMobilePythonX86Release

.\gradlew assembleLeanbackJavaArm64_v8aRelease
.\gradlew assembleLeanbackJavaArmeabi_v7aRelease
.\gradlew assembleLeanbackJavaX86Release
.\gradlew assembleLeanbackPythonArm64_v8aRelease
.\gradlew assembleLeanbackPythonArmeabi_v7aRelease
.\gradlew assembleLeanbackPythonX86Release


pause