@echo off
REM Example script to upload an extracted course via REST API (Windows)
REM Usage: upload-course-example.bat <course-directory> <access-token>

setlocal enabledelayedexpansion

set COURSE_DIR=%1
set ACCESS_TOKEN=%2
set API_BASE_URL=http://localhost:8080/api/v1

if "%COURSE_DIR%"=="" goto usage
if "%ACCESS_TOKEN%"=="" goto usage
goto main

:usage
echo Usage: %0 ^<course-directory^> ^<access-token^>
echo.
echo Example:
echo   %0 course-content-extracted\de-conversational-german eyJhbGc...
echo.
echo To get an access token:
echo   1. Login via UI and copy token from browser storage, OR
echo   2. Use the auth API:
echo      curl -X POST http://localhost:8080/api/v1/auth/login ^
echo        -H "Content-Type: application/json" ^
echo        -d "{\"username\":\"demo\",\"password\":\"demo\"}"
exit /b 1

:main
if not exist "%COURSE_DIR%" (
    echo Error: Directory not found: %COURSE_DIR%
    exit /b 1
)

REM Extract course name from directory
for %%F in ("%COURSE_DIR%") do set COURSE_SLUG=%%~nxF

REM Extract language code (first part before dash)
for /f "tokens=1 delims=-" %%a in ("%COURSE_SLUG%") do set LANGUAGE_CODE=%%a

REM Extract course name (everything after first dash, replace dashes with spaces)
set COURSE_NAME=%COURSE_SLUG:*-=%
set COURSE_NAME=%COURSE_NAME:-= %

echo ==================================
echo Uploading Course via REST API
echo ==================================
echo Course directory: %COURSE_DIR%
echo Course slug: %COURSE_SLUG%
echo Language code: %LANGUAGE_CODE%
echo Course name: %COURSE_NAME%
echo API URL: %API_BASE_URL%
echo.

REM Check if curriculum.yml exists
if not exist "%COURSE_DIR%\curriculum.yml" (
    echo Error: curriculum.yml not found in %COURSE_DIR%
    exit /b 1
)

echo Found curriculum.yml

REM Count lesson files
set LESSON_COUNT=0
for %%F in ("%COURSE_DIR%\*.md") do set /a LESSON_COUNT+=1
echo Found %LESSON_COUNT% lesson files
echo.

REM Build curl command
set CURL_CMD=curl -X POST %API_BASE_URL%/courses/import/file
set CURL_CMD=%CURL_CMD% -H "Authorization: Bearer %ACCESS_TOKEN%"
set CURL_CMD=%CURL_CMD% -F "curriculumFile=@%COURSE_DIR%\curriculum.yml"
set CURL_CMD=%CURL_CMD% -F "languageCode=%LANGUAGE_CODE%"
set CURL_CMD=%CURL_CMD% -F "courseName=%COURSE_NAME%"
set CURL_CMD=%CURL_CMD% -F "category=Conversational"
set CURL_CMD=%CURL_CMD% -F "startingLevel=A1"
set CURL_CMD=%CURL_CMD% -F "targetLevel=B2"

REM Add all lesson files
for %%F in ("%COURSE_DIR%\*.md") do (
    set CURL_CMD=!CURL_CMD! -F "lessonFiles=@%%F"
)

echo Uploading course...
echo.

REM Execute the curl command
%CURL_CMD%

echo.
echo.
echo Upload complete! Check the response above for details.
echo If successful, the course is now available in the database as a draft.
echo You can publish it via the Course Management UI.

endlocal
