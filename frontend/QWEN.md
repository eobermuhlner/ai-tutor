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
- **Backend**: API server usually running on `http://localhost:8080` (default test credentials: username `demo`, password `demo`)

## Project Structure
- `src/` - Source code
- `dist/` - Production build output

## REST API Integration Guidelines
**CRITICAL**: When implementing frontend API calls, ALWAYS follow this process:
1. Find the Controller in the backend
2. Read the Controller Method to identify HTTP method, endpoint path, request/response DTO class names
3. Read the Request DTO (if applicable) to check field names, types, required vs optional fields
4. Read the Response DTO to check field names, types, nullable fields
5. Create/Update Frontend Types in `src/types.ts`
6. Create/Update API Function in `src/api/*.ts`

**Never Assume** field names or API structure. **Always Verify** by reading actual controller and DTO source code.

## TypeScript Import Guidelines
This project uses `verbatimModuleSyntax: true`, requiring strict separation between runtime values and type-only imports:
- Regular imports for Enums: `import { CEFRLevel } from '../../types';`
- Import type for Interfaces/Types: `import type { LanguageProficiency } from '../../types';`

## Development Guidelines
**CRITICAL**: Never remove or break existing features without explicit user approval. Preserve existing UI/UX patterns, add functionality alongside existing features, and maintain visual consistency.

### Module Import and Export Guidelines
**CRITICAL**: Always verify module exports and imports to avoid syntax errors during development.

When modifying React components or modules:
1. Check existing exports before modifying - verify components have proper `export default` statements
2. Follow import conventions - use correct import syntax and match export structure
3. Verify dependent files - check files that import the module you're modifying
4. Test imports after modifications - watch for syntax errors in the console