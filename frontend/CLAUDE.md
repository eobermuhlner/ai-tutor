# AI Tutor Frontend

## Important Note
CLAUDE.md and QWEN.md should always be maintained in sync. When making changes to development guidelines, project information, or technical details, ensure both files are updated accordingly.

## Tech Stack

- **Framework**: React 19.1.1
- **Build Tool**: Vite 5.4.20
- **Language**: TypeScript 5.9.3
- **Styling**: Tailwind CSS 3.4.18
- **Linting**: ESLint 9.36.0

## Development

```bash
npm run dev      # Start dev server
npm run build    # Build for production
npm run lint     # Run linter
npm run preview  # Preview production build
```

### Development Environment

**IMPORTANT**: Development servers are typically already running during active development:

- **Frontend**: Dev server usually running on `http://localhost:5173`
  - If you need to test with a separate instance, inform the user or use an alternative port
  - Check if port 5173 is in use before starting a new dev server

- **Backend**: API server usually running on `http://localhost:8080`
  - Default test credentials: username `demo`, password `demo`
  - Backend provides REST API at `/api/v1/*`
  - Check if port 8080 is responding before assuming the backend needs to be started

## Project Structure

- `src/` - Source code
- `dist/` - Production build output
- `tailwind.config.js` - Tailwind configuration
- `vite.config.ts` - Vite configuration
- `tsconfig.json` - TypeScript configuration

## REST API Integration Guidelines

**CRITICAL**: When implementing frontend API calls, ALWAYS follow this process to avoid mismatches:

### Step-by-Step Process

1. **Find the Controller** in `/home/ero/IdeaProjects/ai-tutor/src/main/kotlin/ch/obermuhlner/aitutor/*/controller/`
   ```bash
   # Example: Find user language endpoints
   find /home/ero/IdeaProjects/ai-tutor -name "*Controller.kt" -exec grep -l "languages" {} \;
   ```

2. **Read the Controller Method** to identify:
   - HTTP method (GET, POST, PUT, PATCH, DELETE)
   - Exact endpoint path
   - Request body DTO class name
   - Response DTO class name

3. **Read the Request DTO** (if applicable) in `*/dto/*Request.kt`:
   - Check exact field names
   - Check field types
   - Check which fields are required vs optional
   - Check default values

4. **Read the Response DTO** in `*/dto/*Response.kt`:
   - Check exact field names returned
   - Check field types
   - Check nullable fields

5. **Create/Update Frontend Types** in `src/types.ts`:
   - Match field names EXACTLY (camelCase in TypeScript, camelCase in Kotlin)
   - Use `| null` for nullable Kotlin fields
   - Use `?` for optional fields

6. **Create/Update API Function** in `src/api/*.ts`:
   - Use correct HTTP method
   - Use exact endpoint path from controller
   - Send request body matching request DTO
   - Type response matching response DTO

### Example Workflow

```typescript
// WRONG: Assuming API structure
export async function addLanguage(userId: string, languageCode: string, level: string) {
  return apiClient.post(`/users/${userId}/languages`, { languageCode, level });
}

// RIGHT: After checking controller + DTOs
// 1. Found: UserLanguageController.kt @PostMapping("/{userId}/languages")
// 2. Request DTO: AddLanguageRequest(languageCode, type, cefrLevel?, isNative)
// 3. Response DTO: UserLanguageProficiencyResponse(id, userId, languageCode, proficiencyType, ...)
export async function addLanguageProficiency(
  userId: string,
  languageCode: string,
  type: LanguageProficiencyType,
  cefrLevel?: CEFRLevel
): Promise<LanguageProficiency> {
  const response = await apiClient.post<LanguageProficiency>(
    `/users/${userId}/languages`,
    {
      languageCode,
      type,
      cefrLevel,
      isNative: type === LanguageProficiencyType.Native,
    }
  );
  return response.data;
}
```

### Never Assume

- ❌ Field names from domain models are used in DTOs
- ❌ Request and response have same structure
- ❌ API follows RESTful conventions
- ❌ Documentation or markdown files are up to date

### Always Verify

- ✅ Read the actual controller source code
- ✅ Read the actual DTO source code
- ✅ Check for enum types and their exact values
- ✅ Test the endpoint after implementation

## TypeScript Import Guidelines

**CRITICAL**: This project uses `verbatimModuleSyntax: true` in TypeScript configuration, which requires strict separation between runtime values and type-only imports.

### Import Rules

Follow these patterns when importing from `src/types.ts`:

**For Enums** (runtime values):
```typescript
import { CEFRLevel, ConversationPhase, ErrorType } from '../../types';
```

**For Interfaces/Types** (type-only):
```typescript
import type { LanguageProficiency, Language, Message } from '../../types';
```

**Mixed imports** (both enums and interfaces):
```typescript
import { CEFRLevel } from '../../types';
import type { LanguageProficiency } from '../../types';
```

### Why This Matters

- **Enums** generate JavaScript code at runtime and must be imported as regular values
- **Interfaces/Types** are erased during compilation and must use `import type`
- Incorrect imports will cause module resolution errors: "does not provide an export named X"
- Always check existing components for reference patterns before adding new imports

### Examples

✅ **Correct**:
```typescript
// Importing an enum (runtime value)
import { CEFRLevel } from '../../types';

// Importing an interface (type-only)
import type { LanguageProficiency } from '../../types';

// Using the imports
const level: CEFRLevel = CEFRLevel.A1;
const proficiency: LanguageProficiency = { ... };
```

❌ **Incorrect**:
```typescript
// Wrong: Using regular import for interface
import { LanguageProficiency } from '../../types';  // ERROR!

// Wrong: Using import type for enum
import type { CEFRLevel } from '../../types';  // ERROR!
```

## Development Guidelines

### Module Import and Export Guidelines

**CRITICAL**: Always verify module exports and imports to avoid syntax errors during development.

When modifying React components or modules:

1. **Check existing exports before modifying:**
   - Verify that components have proper `export default` statements
   - Check that named exports match expected import statements
   - Ensure export structure matches import patterns in dependent files

2. **Follow import conventions:**
   - Use correct import syntax: `import ComponentName from './path/to/Component'`
   - For named exports: `import { ComponentName } from './path/to/Component'`
   - Avoid modifying export structure unless absolutely necessary

3. **Verify dependent files:**
   - Check files that import the module you're modifying
   - Look for import statements that might break with your changes
   - Ensure import/export consistency across the codebase

4. **Test imports after modifications:**
   - Run the development server after making changes
   - Watch for syntax errors in the console
   - If errors occur, check module export/import patterns immediately

### Preserving Existing Functionality

**CRITICAL**: Never remove or break existing features without explicit user approval.

When implementing new features or making changes:

1. **Preserve existing UI/UX patterns**
   - Keep existing user workflows intact
   - Maintain familiar interaction patterns
   - Don't remove buttons, links, or navigation elements users rely on

2. **Add, don't replace**
   - Add new functionality alongside existing features
   - Provide new options without removing old ones
   - If replacement is necessary, ask user first

3. **Test existing flows**
   - Verify all existing user journeys still work
   - Check that forms still submit correctly
   - Ensure navigation paths remain functional
   - Confirm existing keyboard shortcuts and interactions work

4. **Visual consistency**
   - Match existing design patterns and component styling
   - Use established color schemes and spacing
   - Follow existing responsive breakpoints

5. **When in doubt, ask**
   - If a change might affect user experience, clarify with the user first
   - Present options rather than making assumptions
   - Get approval before removing any visible UI elements

### Git Commit Guidelines

**Commit message format:**
- **First line**: Concise summary (imperative mood, no period)
- **Body** (optional): Brief explanation of what and why (one sentence per line)
- **No attribution**: Don't include "Generated with Claude Code" or similar references

**Example:**
```
Add effectivePhase to separate user preference from active phase

User-controlled conversationPhase (Auto/Free/Correction/Drill) now separate from effectivePhase (actual active phase).
LLM suggestions only update effectivePhase when in Auto mode, never override manual user choices.
```