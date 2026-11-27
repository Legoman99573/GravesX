@echo off
REM Installs the bundled libs into your local Maven repository: lib\ (global, shared) and server\libs\ (server-only).
REM Run once per machine (and again if the jars in lib\ or server\libs change), then build with: mvn clean package
setlocal
cd /d "%~dp0"

call :install server\libs authlib-7.0.61-graves.jar com.mojang authlib 7.0.61-graves || exit /b 1
call :install lib libby-2.0.1-graves2.jar com.alessiodp libby 2.0.1-graves2 || exit /b 1
call :install server\libs playernpc-2023.6.jar dev.sergiferry PlayerNPC 2023.6 || exit /b 1
call :install server\libs FurnitureEngine-3.3.jar com.mira furnitureengine 3.3 || exit /b 1

echo Done. Now run: mvn clean package
exit /b 0

:install
if not exist "%~1\%~2" (
    echo Missing %~1\%~2
    exit /b 1
)
echo Installing %~3:%~4:%~5
call mvn -B -q install:install-file -Dfile="%~1\%~2" -DgroupId=%~3 -DartifactId=%~4 -Dversion=%~5 -Dpackaging=jar
exit /b %errorlevel%
