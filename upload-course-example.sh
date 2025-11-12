#!/bin/bash
# Example script to upload an extracted course via REST API
# Usage: ./upload-course-example.sh <course-directory> <access-token>

set -e

COURSE_DIR=$1
ACCESS_TOKEN=$2
API_BASE_URL=${API_BASE_URL:-http://localhost:8080/api/v1}

if [ -z "$COURSE_DIR" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo "Usage: $0 <course-directory> <access-token>"
    echo ""
    echo "Example:"
    echo "  $0 course-content-extracted/de-conversational-german eyJhbGc..."
    echo ""
    echo "To get an access token:"
    echo "  1. Login via UI and copy token from browser storage, OR"
    echo "  2. Use the auth API:"
    echo "     curl -X POST http://localhost:8080/api/v1/auth/login \\"
    echo "       -H 'Content-Type: application/json' \\"
    echo "       -d '{\"username\":\"demo\",\"password\":\"demo\"}'"
    exit 1
fi

if [ ! -d "$COURSE_DIR" ]; then
    echo "Error: Directory not found: $COURSE_DIR"
    exit 1
fi

# Extract course metadata from directory name
COURSE_SLUG=$(basename "$COURSE_DIR")
LANGUAGE_CODE=$(echo "$COURSE_SLUG" | cut -d'-' -f1)
COURSE_NAME=$(echo "$COURSE_SLUG" | cut -d'-' -f2- | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2))}1')

echo "=================================="
echo "Uploading Course via REST API"
echo "=================================="
echo "Course directory: $COURSE_DIR"
echo "Course slug: $COURSE_SLUG"
echo "Language code: $LANGUAGE_CODE"
echo "Course name: $COURSE_NAME"
echo "API URL: $API_BASE_URL"
echo ""

# Check if curriculum.yml exists
CURRICULUM_FILE="$COURSE_DIR/curriculum.yml"
if [ ! -f "$CURRICULUM_FILE" ]; then
    echo "Error: curriculum.yml not found in $COURSE_DIR"
    exit 1
fi

echo "Found curriculum.yml"

# Count lesson files
LESSON_COUNT=$(find "$COURSE_DIR" -name "*.md" | wc -l)
echo "Found $LESSON_COUNT lesson files"
echo ""

# Build curl command
CURL_CMD="curl -X POST $API_BASE_URL/courses/import/file"
CURL_CMD="$CURL_CMD -H 'Authorization: Bearer $ACCESS_TOKEN'"
CURL_CMD="$CURL_CMD -F 'curriculumFile=@$CURRICULUM_FILE'"
CURL_CMD="$CURL_CMD -F 'languageCode=$LANGUAGE_CODE'"
CURL_CMD="$CURL_CMD -F 'courseName=$COURSE_NAME'"
CURL_CMD="$CURL_CMD -F 'category=Conversational'"
CURL_CMD="$CURL_CMD -F 'startingLevel=A1'"
CURL_CMD="$CURL_CMD -F 'targetLevel=B2'"

# Add all lesson files
for lesson_file in "$COURSE_DIR"/*.md; do
    if [ -f "$lesson_file" ]; then
        CURL_CMD="$CURL_CMD -F 'lessonFiles=@$lesson_file'"
    fi
done

echo "Uploading course..."
echo ""

# Execute the curl command
eval "$CURL_CMD"

echo ""
echo ""
echo "Upload complete! Check the response above for details."
echo "If successful, the course is now available in the database as a draft."
echo "You can publish it via the Course Management UI."
