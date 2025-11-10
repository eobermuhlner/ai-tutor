# Task 0016: Add EDITOR Role & Course Management System

## Objective
Implement a new EDITOR role that enables users to create and edit courses through a web UI. The admin user will have all three roles (USER, ADMIN, EDITOR). Courses will support a draft/publish workflow with lessons stored in the database and edited via a markdown editor.

## Requirements

### Functional Requirements
1. **EDITOR Role**: New user role with permissions to manage courses and tutors (but not users)
2. **Admin Initialization**: Admin user created at startup will have all three roles (USER, ADMIN, EDITOR)
3. **Course Management UI**: Editors can create, edit, delete, and publish courses
4. **Draft/Publish Workflow**: Courses have draft and published states
5. **Lesson Content Management**: Lessons stored in database with web-based markdown editor
6. **Curriculum Management**: UI forms to manage lesson sequences and progression rules
7. **Draft Filtering**: Non-editors cannot see draft courses
8. **Backward Compatibility**: Existing file-based lessons still readable

### Technical Requirements
1. Database entities for draft state, lessons, and curriculum
2. REST APIs for course, lesson, and curriculum management
3. Authorization checks for EDITOR role
4. Data migration for existing courses
5. Frontend components for course/lesson/curriculum management

---

## Implementation Phases

### Phase 1: Backend - Role & Authorization

#### Tasks
1. **Add EDITOR to UserRole enum**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/user/domain/UserRole.kt`
   - Add `EDITOR` enum value

2. **Update AdminUserInitializer**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/auth/config/AdminUserInitializer.kt`
   - Modify to assign admin user three roles: `USER`, `ADMIN`, `EDITOR`

3. **Extend AuthorizationService**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/auth/service/AuthorizationService.kt`
   - Add `isEditor(): Boolean`
   - Add `isEditorOrAdmin(): Boolean`
   - Add `requireEditor()` throws InsufficientPermissionsException

#### Testing
- Verify admin user has all three roles after initialization
- Verify authorization methods return correct values

---

### Phase 2: Backend - Database Schema

#### Tasks
1. **Extend CourseTemplateEntity**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/domain/CourseTemplateEntity.kt`
   - Add fields:
     - `isDraft: Boolean = false`
     - `publishedAt: Instant? = null`
     - `lastEditedBy: UUID? = null`
     - `version: Int = 1`

2. **Create LessonContentEntity**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/domain/LessonContentEntity.kt`
   - Fields:
     - `id: UUID`
     - `courseId: UUID` (foreign key to CourseTemplateEntity)
     - `lessonId: String` (e.g., "week-01-greetings")
     - `title: String`
     - `content: String` (markdown)
     - `displayOrder: Int`
     - `minimumDays: Int? = null`
     - `requiredTurns: Int? = null`
     - `createdAt: Instant`
     - `updatedAt: Instant`

3. **Create CurriculumRuleEntity**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/domain/CurriculumRuleEntity.kt`
   - Fields:
     - `id: UUID`
     - `courseId: UUID` (foreign key)
     - `progressionMode: String` (TIME_BASED/LINEAR/ADAPTIVE)
     - `allowSkipping: Boolean = false`
     - `requireCompletion: Boolean = false`
     - `createdAt: Instant`
     - `updatedAt: Instant`

4. **Create Repositories**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/repository/LessonContentRepository.kt`
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/repository/CurriculumRuleRepository.kt`

#### Database Migration
- Create Flyway migration script to add columns and tables

#### Testing
- Verify entities save/load correctly
- Verify foreign key constraints

---

### Phase 3: Backend - Course Management API

#### Tasks
1. **Create CourseManagementController**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/controller/CourseManagementController.kt`
   - Endpoints:
     - `POST /api/v1/courses` - Create new course (draft by default)
     - `PUT /api/v1/courses/{id}` - Update course metadata
     - `POST /api/v1/courses/{id}/publish` - Publish a draft course
     - `POST /api/v1/courses/{id}/unpublish` - Revert to draft
     - `DELETE /api/v1/courses/{id}` - Delete course (editors can only delete own drafts, admins can delete any)
     - `GET /api/v1/courses?includeDrafts=true` - List all courses
   - Authorization: Require EDITOR role

2. **Create LessonManagementController**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/controller/LessonManagementController.kt`
   - Endpoints:
     - `GET /api/v1/courses/{courseId}/lessons` - List all lessons
     - `POST /api/v1/courses/{courseId}/lessons` - Create new lesson
     - `PUT /api/v1/courses/{courseId}/lessons/{lessonId}` - Update lesson
     - `DELETE /api/v1/courses/{courseId}/lessons/{lessonId}` - Delete lesson
     - `PUT /api/v1/courses/{courseId}/lessons/reorder` - Reorder lessons (body: [{id, displayOrder}])
   - Authorization: Require EDITOR role

3. **Create CurriculumController**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/controller/CurriculumController.kt`
   - Endpoints:
     - `GET /api/v1/courses/{courseId}/curriculum` - Get curriculum rules
     - `PUT /api/v1/courses/{courseId}/curriculum` - Update curriculum settings
   - Authorization: Require EDITOR role

4. **Create Request/Response DTOs**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/dto/CourseManagementDto.kt`
   - DTOs: CreateCourseRequest, UpdateCourseRequest, CourseResponse, LessonRequest, LessonResponse, CurriculumRequest, CurriculumResponse

5. **Create Services**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/CourseManagementService.kt`
   - Implement course CRUD logic

#### Testing
- Unit tests for service methods
- Integration tests for endpoints (authentication, authorization)
- Test draft filtering

---

### Phase 4: Backend - Migration & Compatibility

#### Tasks
1. **Data Migration Service**
   - New file: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/LessonMigrationService.kt`
   - Implement migration of filesystem lessons to database
   - Runs at startup if migration hasn't been done

2. **Update LessonContentService**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/LessonContentService.kt`
   - Modify to read from database first, fallback to filesystem
   - Prioritize database for published courses

3. **Update CatalogController**
   - File: `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/controller/CatalogController.kt`
   - Add `includeDrafts` query parameter to course listing endpoints
   - Filter out draft courses for non-editors

#### Testing
- Verify existing courses and lessons still work
- Verify migration completes successfully
- Test draft filtering in catalog endpoints

---

### Phase 5: Frontend - Auth & Navigation

#### Tasks
1. **Extend useAuthStore**
   - File: `frontend/src/stores/authStore.ts`
   - Add `isEditor` property
   - Add `canManageCourses` computed property (checks EDITOR or ADMIN)

2. **Update Navigation**
   - File: `frontend/src/components/Navigation.tsx` (or similar)
   - Add "Course Management" link visible only to editors
   - Add draft badge display logic for course cards

#### Testing
- Verify EDITOR role is correctly detected
- Verify navigation only shows for editors
- Verify draft badges display correctly

---

### Phase 6: Frontend - Course Management UI

#### Tasks
1. **Create CourseManagementPage**
   - New file: `frontend/src/pages/CourseManagementPage.tsx`
   - Features:
     - Table view of courses (all for editors, published only for others)
     - Columns: Name, Language, Status (Draft/Published), Last Updated, Actions
     - Actions: Edit, Delete, Publish/Unpublish
     - "Create New Course" button
     - Delete confirmation dialog
   - Use API endpoints from Phase 3

2. **Create CourseEditorPage**
   - New file: `frontend/src/pages/CourseEditorPage.tsx`
   - Multi-step form (stepper component):
     - **Step 1**: Basic Info
       - Select language (from catalog)
       - Course name (localized - tabs for languages)
       - Short description (localized)
       - Full description (localized)
       - Category dropdown
     - **Step 2**: Levels & Goals
       - Starting level (CEFR: None, A1, A2, B1, B2, C1, C2)
       - Target level
       - Target audience (localized)
       - Learning goals (localized, array)
     - **Step 3**: Settings
       - Default conversation phase (Auto/Free/Correction/Drill)
       - Estimated weeks (optional)
       - Tags (array)
       - Suggested tutors (multi-select from tutor list)
     - **Step 4**: Review & Save
       - Summary of all fields
       - "Save as Draft" button
       - "Save & Publish" button
   - Handle both create and edit modes
   - Redirect to LessonManagementPage after save

3. **Update CourseDetailPage**
   - File: `frontend/src/pages/CourseDetailPage.tsx`
   - Add "Edit Course" button visible to editors
   - Show draft/published indicator

#### Testing
- Test form validation
- Test saving draft course
- Test publishing course
- Test editing existing course
- Test course list display

---

### Phase 7: Frontend - Lesson Management UI

#### Tasks
1. **Create LessonManagementPage**
   - New file: `frontend/src/pages/LessonManagementPage.tsx`
   - Features:
     - List of lessons with drag-and-drop reordering
     - Columns: Order, Title, Minimum Days, Required Turns, Actions
     - Actions: Edit, Delete, Preview
     - "Add Lesson" button
     - Drag-drop reordering (update via API)
   - Accessible from CourseEditorPage or CourseManagementPage
   - Back button to return to course list

2. **Create LessonEditorPage**
   - New file: `frontend/src/pages/LessonEditorPage.tsx`
   - Split-pane layout:
     - **Left pane**: Markdown editor
       - Toolbar: headings (H1-H6), bold, italic, lists, blockquote, code, links, images
       - Text area for markdown content
     - **Right pane**: Live preview of rendered markdown
   - Form fields (above or below editor):
     - Lesson ID (slug-like, e.g., "week-01-greetings") - read-only for edits, editable for creates
     - Title
     - Display order
     - Minimum days before next lesson
     - Required conversation turns before next lesson
   - "Save Lesson" button (back to LessonManagementPage)
   - "Delete Lesson" button with confirmation
   - Handle both create and edit modes

3. **Markdown Editor Component**
   - New file: `frontend/src/components/MarkdownEditor.tsx`
   - Features:
     - Toolbar with formatting buttons
     - Keyboard shortcuts (Cmd/Ctrl+B for bold, etc.)
     - Auto-save to localStorage (debounced)
     - Character/word count
   - Use a library like `react-markdown` for preview or implement custom renderer

#### Testing
- Test lesson creation and editing
- Test markdown editor functionality
- Test drag-and-drop reordering
- Test lesson deletion
- Test markdown preview

---

### Phase 8: Frontend - Curriculum Management UI

#### Tasks
1. **Create CurriculumEditorPage**
   - New file: `frontend/src/pages/CurriculumEditorPage.tsx`
   - Form-based curriculum settings:
     - **Progression Mode**: Radio buttons
       - Time-Based: Users advance after minimum days
       - Linear: Users advance in order, no skipping
       - Adaptive: Tutors recommend next lesson
     - **Global Rules**: Checkboxes
       - Allow skipping lessons
       - Require lesson completion before next
     - **Lesson Sequence**: Display as read-only list or tree
   - "Save Curriculum" button
   - Accessible from CourseEditorPage or CourseManagementPage

#### Testing
- Test curriculum form submission
- Test different progression modes
- Test rule toggles

---

### Phase 9: Testing & Validation

#### Backend Tests
1. Create `backend/src/test/kotlin/ch/obermuhlner/aitutor/catalog/controller/CourseManagementControllerTest.kt`
   - Test CREATE course endpoint (EDITOR required)
   - Test UPDATE course endpoint
   - Test PUBLISH/UNPUBLISH endpoints
   - Test DELETE endpoint (authorization)
   - Test draft filtering

2. Create `backend/src/test/kotlin/ch/obermuhlner/aitutor/catalog/controller/LessonManagementControllerTest.kt`
   - Test lesson CRUD endpoints
   - Test reordering endpoint

3. Create `backend/src/test/kotlin/ch/obermuhlner/aitutor/catalog/service/CourseManagementServiceTest.kt`
   - Test business logic (draft state, publishing, etc.)

4. Create `backend/src/test/kotlin/ch/obermuhlner/aitutor/auth/service/AuthorizationServiceTest.kt`
   - Add tests for `isEditor()` and `requireEditor()`

#### Frontend Tests
1. Test CourseManagementPage (list, create, edit, delete flows)
2. Test CourseEditorPage (form validation, localization)
3. Test LessonManagementPage (drag-drop, list)
4. Test LessonEditorPage (markdown editor, save)
5. Test CurriculumEditorPage (form submission)

#### Manual Testing Checklist
- [ ] Admin user has all three roles after startup
- [ ] Non-admin users cannot access /courses/manage
- [ ] Editors can create courses (appear as drafts)
- [ ] Editors can edit course metadata
- [ ] Editors can add/edit/delete lessons
- [ ] Editors can manage curriculum settings
- [ ] Non-editors cannot see draft courses in catalog
- [ ] Published courses visible to all users
- [ ] Existing courses and lessons still work
- [ ] Draft courses do not appear in regular catalog browsing
- [ ] Editors can publish/unpublish courses
- [ ] Markdown editor works (bold, italic, lists, etc.)
- [ ] Lesson preview renders correctly
- [ ] Drag-drop lesson reordering works

---

### Phase 10: Documentation

#### Tasks
1. **Update CLAUDE.md**
   - Add EDITOR role to architecture overview
   - Document course management API endpoints
   - Document new database entities

2. **Update README.md**
   - Add section on course authoring via web UI
   - Document editor role assignment

3. **Add OpenAPI Annotations**
   - Annotate all new endpoints with `@Operation`, `@ApiResponse`, etc.
   - Verify Swagger UI shows new endpoints at http://localhost:8080/swagger-ui.html

#### Testing
- Verify Swagger UI displays correctly
- Verify README is clear and complete

---

## Implementation Order (Recommended)

1. **Phase 1** - Add EDITOR role (quick, unlocks everything)
2. **Phase 2** - Database schema (required for Phase 3)
3. **Phase 3** - Backend APIs (can test with HTTP client)
4. **Phase 4** - Migration & compatibility (ensures nothing breaks)
5. **Phase 5** - Frontend auth setup (foundation for UI)
6. **Phase 6** - Course management UI (core feature)
7. **Phase 7** - Lesson management UI (core feature)
8. **Phase 8** - Curriculum UI (optional but nice-to-have)
9. **Phase 9** - Testing & validation (verify everything works)
10. **Phase 10** - Documentation (finalize)

---

## Acceptance Criteria

- [ ] EDITOR role exists and admin user has all three roles
- [ ] Editors can create, edit, and delete courses via web UI
- [ ] Courses support draft/publish workflow
- [ ] Lessons stored in database with markdown editor
- [ ] Curriculum settings manageable via structured forms
- [ ] Non-editors cannot see draft courses
- [ ] All existing functionality still works
- [ ] Authorization checks prevent unauthorized access
- [ ] Tests pass for all new features
- [ ] Documentation updated

---

## Risk Mitigation

1. **Backward Compatibility**: Keep filesystem lesson reading as fallback
2. **Data Loss**: Test migration thoroughly before production
3. **Authorization**: Double-check role checks in all new endpoints
4. **Performance**: Index foreign keys and add query pagination
5. **User Experience**: Clear UI feedback for draft/published states

