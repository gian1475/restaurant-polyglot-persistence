@REM Maven Wrapper script for Windows
@REM Auto-generated for restaurante-multimotor project

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile %WRAPPER_JAR%"
)

set MAVEN_CMD="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\apache-maven-3.9.6\bin\mvn.cmd"

if not exist "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\apache-maven-3.9.6" (
    echo Downloading Maven 3.9.6...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven.zip' -DestinationPath '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\' -Force"
    del "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven.zip"
)

%MAVEN_CMD% %*
