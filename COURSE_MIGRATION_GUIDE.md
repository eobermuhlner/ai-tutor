# Course Migration & Upload Guide

This guide explains how to migrate existing course files from the embedded resources to the database, and how to upload courses via REST API.

## Table of Contents

1. [Quick Migration (Recommended)](#quick-migration-recommended)
2. [Extract Files for Manual Upload](#extract-files-for-manual-upload)
3. [Upload via REST API](#upload-via-rest-api)
4. [Upload via UI](#upload-via-ui)
5. [Troubleshooting](#troubleshooting)

---

## Quick Migration (Recommended)

**Best for:** Migrating all existing courses in one go.

### Run the Migration Script

```bash
./gradlew :backend:migrateCoursesFromFiles
```

**What it does:**
- Scans `backend/src/main/resources/course-content/` for all courses
- Reads all `curriculum.yml` files and lesson `.md` files
- Creates database entities with `sourceType = SEEDED`
- Skips already migrated courses (idempotent)
- Generates a migration report

**Output Example:**
```
================================================================================
Starting Course Migration: File System → Database
================================================================================
Found 53 curriculum files to migrate
--------------------------------------------------------------------------------
Processing course: de-conversational-german
Found 12 lesson files
Created course entity: abc-123-def-456
Created 12 lesson entities
--------------------------------------------------------------------------------
...
Migration Summary:
  Courses created:     53
  Courses skipped:     0
  Course errors:       0
  Lessons created:     624
  Lesson errors:       0
  Total time:          4532ms

Status: SUCCESS
```

### After Migration

1. **Verify in UI**: Navigate to Course Management page to see all migrated courses
2. **Disable seeding** (optional): Edit `application.yml` and set `ai-tutor.catalog.useSeeding=false`
3. **Keep files** (recommended): Keep original files in `course-content/` as backup

---

## Extract Files for Manual Upload

**Best for:** When you want to manually review or modify courses before uploading.

### Extract Course Files

```bash
# Extract all courses to external directory
./gradlew :backend:extractCourseFiles
```

**Output:**
- Creates directory: `course-content-extracted/` in project root
- Contains subdirectories for each course (e.g., `de-conversational-german/`)
- Each subdirectory has:
  - `curriculum.yml`
  - Lesson files (`week-01-greetings.md`, etc.)

**Directory Structure:**
```
course-content-extracted/
├── de-conversational-german/
│   ├── curriculum.yml
│   ├── week-01-greetings.md
│   ├── week-02-introductions.md
│   └── ...
├── es-conversational-spanish/
│   ├── curriculum.yml
│   └── ...
└── [50+ other courses]
```

---

## Upload via REST API

### Prerequisites

1. **Backend running**: `./gradlew :backend:bootRun`
2. **Editor/Admin account**: Login to get access token
l
### Get Access Token

**Option 1: Via UI**
1. Login to the application
2. Open browser DevTools → Application/Storage → Local Storage
3. Copy the `accessToken` value

**Option 2: Via API**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo"}'
```

### Upload a Single Course

#### Using the Helper Script (Linux/Mac)

```bash
chmod +x upload-course-example.sh
./upload-course-example.sh course-content-extracted/de-conversational-german YOUR_TOKEN
```

#### Using the Helper Script (Windows)

```batch
upload-course-example.bat course-content-extracted\de-conversational-german YOUR_TOKEN
```

#### Manual curl Command

```bash
curl -X POST http://localhost:8080/api/v1/courses/import/file \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "curriculumFile=@course-content-extracted/de-conversational-german/curriculum.yml" \
  -F "lessonFiles=@course-content-extracted/de-conversational-german/week-01-greetings.md" \
  -F "lessonFiles=@course-content-extracted/de-conversational-german/week-02-introductions.md" \
  -F "languageCode=de" \
  -F "courseName=Conversational German" \
  -F "category=Conversational" \
  -F "startingLevel=A1" \
  -F "targetLevel=B2"
```

**Note:** Repeat `-F "lessonFiles=@..."` for each lesson file.

### Upload Multiple Courses

Create a batch script:

```bash
#!/bin/bash
TOKEN="your-access-token"

for course_dir in course-content-extracted/*/; do
    echo "Uploading: $course_dir"
    ./upload-course-example.sh "$course_dir" "$TOKEN"
    sleep 2  # Rate limiting
done
```

---

## Upload via UI

**Best for:** Non-technical users or one-off imports.

### Steps

1. **Navigate to Course Management**
   - Login as Editor or Admin
   - Go to Course Management page

2. **Click "Import Course" Button**
   - Located next to "Create New Course"

3. **Fill in Course Information**
   - Course Name: e.g., "Conversational German"
   - Language: Select from dropdown
   - Category: Conversational, Grammar, Travel, etc.
   - Starting Level: A1, A2, B1, B2, C1, C2
   - Target Level: A1, A2, B1, B2, C1, C2
   - Description: Optional

4. **Upload Files**
   - **Curriculum File**: Click or drag `curriculum.yml` (required)
   - **Lesson Files**: Click or drag all `.md` lesson files (required, multiple)

5. **Optional: Validate Files**
   - Click "Validate Files" to check format before importing
   - Review any error messages

6. **Import Course**
   - Click "Import Course"
   - Wait for progress indicator
   - Success message shows number of lessons imported

7. **Review & Publish**
   - Imported course opens in editor as draft
   - Review content
   - Click "Publish" when ready

### UI Features

- **Drag & Drop**: Drag files directly onto upload zones
- **Multiple Files**: Upload all lesson files at once
- **Validation**: Real-time file validation (format, naming)
- **Error Reporting**: Detailed error messages for failed imports
- **Progress Indicators**: Visual feedback during upload
- **Auto-Navigation**: Automatically opens imported course

---

## Troubleshooting

### Migration Script Issues

**Problem:** `Curriculum file not found: course-slug/curriculum.yml`

**Solution:**
- Ensure `curriculum.yml` exists in each course directory
- Check file naming (must be exactly `curriculum.yml`)
- Verify course is in `backend/src/main/resources/course-content/`

**Problem:** `Failed to parse lesson: invalid markdown format`

**Solution:**
- Check YAML frontmatter is properly formatted
- Ensure frontmatter is between `---` delimiters
- Validate YAML syntax

### API Upload Issues

**Problem:** `401 Unauthorized`

**Solution:**
- Verify access token is valid and not expired
- Ensure user has EDITOR or ADMIN role
- Get a fresh token by logging in again

**Problem:** `400 Bad Request: curriculum.yml file is required`

**Solution:**
- Ensure file is named exactly `curriculum.yml`
- Check the `-F "curriculumFile=@..."` parameter in curl command
- Verify file path is correct

**Problem:** `400 Bad Request: At least one lesson file is required`

**Solution:**
- Include at least one `.md` lesson file
- Check all `-F "lessonFiles=@..."` parameters
- Verify lesson files exist and have `.md` extension

### UI Upload Issues

**Problem:** File upload shows "File must be named curriculum.yml"

**Solution:**
- Rename file to exactly `curriculum.yml` (not `curriculum.yaml`)

**Problem:** "Failed to parse curriculum file"

**Solution:**
- Validate YAML syntax using online YAML validator
- Check for tabs (use spaces instead)
- Ensure proper indentation

**Problem:** Import succeeds but some lessons missing

**Solution:**
- Check lesson file naming matches `curriculum.yml` references
- Ensure `lessonId` in frontmatter matches `id` in curriculum
- Review import response for specific errors

---

## Best Practices

### For Development

1. **Use Migration Script**: Fastest way to get all courses into database
2. **Keep Original Files**: Maintain `course-content/` as source of truth
3. **Version Control**: Commit course files to Git for history

### For Production

1. **Extract & Review**: Extract files, review content, then upload
2. **Upload as Drafts**: All imports create drafts - review before publishing
3. **Backup Database**: Before migration, backup H2 database file
4. **Test First**: Test with 1-2 courses before bulk migration

### For Content Management

1. **UI for Small Changes**: Use Course Editor for individual course edits
2. **API for Bulk**: Use API/migration for adding many courses
3. **Source Type Tracking**: Check `sourceType` field to identify import source
4. **Disable Seeding After Migration**: Set `useSeeding=false` once migrated

---

## Summary

| Method | Best For | Pros | Cons |
|--------|----------|------|------|
| **Migration Script** | Bulk migration | Fast, automatic | All-or-nothing |
| **Extract + API** | Selective upload | Flexible, reviewable | Manual process |
| **Extract + UI** | Non-technical users | User-friendly, visual | One course at a time |

**Recommended Flow:**
1. Development: Use migration script
2. Staging: Extract → review → upload via API
3. Production: Use UI for ongoing management
