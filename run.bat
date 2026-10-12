@echo off
setlocal enabledelayedexpansion
echo ========================================================
echo Starting Finbuddy CRM Backend (Java 25 LTS / Spring Boot)
echo Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
echo ========================================================

:: Free port 8080 if already in use by a previous run
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Freeing port 8080 currently occupied by PID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;c:\finbuddy1\tools\apache-maven-3.9.9\bin;%PATH%"
cd /d "%~dp0backend"
call "c:\finbuddy1\tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
