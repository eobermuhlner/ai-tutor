# Task 0002: Virtual Keyboard Implementation

## Requirements

### User Story
As a language learner, I want to type special characters (accented letters, diacritics) that are not available on my physical keyboard, so that I can write correctly in my target language.

### Functional Requirements
1. **Keyboard Icon**: Add a keyboard icon button near the message input area in chat
2. **Virtual Keyboard Panel**: Display a popup/dropdown virtual keyboard when icon is clicked
3. **Two Keyboard Modes**:
   - **Simplified Mode**: Only special/accented characters (default)
   - **Full Mode**: Complete language-specific keyboard layout
4. **Mode Toggle**: Switch between simplified and full keyboard layouts
5. **Character Insertion**: Clicking a character inserts it at the current cursor position in the textarea
6. **Language-Specific**: Keyboard layout adapts to the target language of the current session
7. **Common Languages**: Support major languages with special characters:
   - Spanish (á, é, í, ó, ú, ñ, ü, ¿, ¡)
   - French (é, è, ê, ë, à, â, ù, û, ç, î, ï, ô, œ, æ)
   - German (ä, ö, ü, ß, Ä, Ö, Ü)
   - Italian (à, è, é, ì, í, ò, ó, ù, ú)
   - Portuguese (á, â, ã, à, é, ê, í, ó, ô, õ, ú, ç)
   - And others as needed

### UI/UX Requirements
1. **Icon Placement**: Inside textarea border (right side) or as button between textarea and Send button
2. **Keyboard Position**: Dropdown panel appearing below the textarea
3. **Visual Style**: Match existing UI design (Tailwind CSS, rounded corners, shadows)
4. **Responsive**: Work on desktop and mobile devices
5. **Keyboard Focus**: Maintain focus on textarea after character insertion
6. **Close Behavior**: Close on outside click, ESC key, or clicking icon again

## Implementation Plan

### Architecture

#### Component Structure
```
MessageInput (existing)
├── textarea
├── VirtualKeyboard (new)
│   ├── Keyboard Icon Button
│   └── Keyboard Panel (conditional)
│       ├── Mode Toggle (Simplified ↔ Full)
│       └── Character Buttons
└── Send Button
```

#### Data Flow
1. `ChatPage` passes `targetLanguageCode` to `MessageInput`
2. `MessageInput` passes `languageCode` to `VirtualKeyboard`
3. `VirtualKeyboard` loads appropriate layout from `keyboardLayouts.ts`
4. User clicks character → `onCharacterInsert` callback → `MessageInput` updates state
5. Character inserted at cursor position in textarea

### Files to Create

#### 1. `src/utils/keyboardLayouts.ts`
**Purpose**: Define language-specific keyboard character mappings

**Type Definitions**:
```typescript
interface KeyboardLayout {
  languageCode: string;
  languageName: string;
  simplified: string[]; // Special characters only
  full?: string[][]; // Complete keyboard rows (optional for now)
}

const keyboardLayouts: Record<string, KeyboardLayout>;
```

**Implementation**:
- Export `keyboardLayouts` object with mappings
- Export `getKeyboardLayout(languageCode: string)` function
- Fallback to empty array if language not supported

#### 2. `src/components/chat/VirtualKeyboard.tsx`
**Purpose**: Virtual keyboard component with character buttons

**Props**:
```typescript
interface VirtualKeyboardProps {
  languageCode: string;
  textareaRef: RefObject<HTMLTextAreaElement>;
  onCharacterInsert: (char: string) => void;
  isOpen: boolean;
  onClose: () => void;
}
```

**State**:
- `mode: 'simplified' | 'full'` (default: 'simplified')

**Features**:
- Render keyboard icon button
- Conditional rendering of keyboard panel
- Mode toggle button
- Character buttons in grid layout
- Click outside to close (useEffect with document click listener)
- ESC key to close

**Styling**:
- Match existing UI patterns (rounded-xl, border, shadow)
- Grid layout for characters (grid-cols-8 or flex-wrap)
- Button hover states

### Files to Modify

#### 3. `src/components/chat/MessageInput.tsx`
**Changes**:
- Add prop: `languageCode?: string`
- Add state: `isKeyboardOpen: boolean`
- Add state for cursor position tracking
- Add function: `insertCharacterAtCursor(char: string)`
  - Get current cursor position from textarea
  - Insert character at position
  - Update message state
  - Restore focus to textarea
- Import and render `VirtualKeyboard` component
- Position keyboard icon appropriately

**Character Insertion Logic**:
```typescript
const insertCharacterAtCursor = (char: string) => {
  const textarea = textareaRef.current;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const newValue = message.slice(0, start) + char + message.slice(end);

  setMessage(newValue);

  // Restore cursor position after character
  setTimeout(() => {
    textarea.focus();
    textarea.setSelectionRange(start + char.length, start + char.length);
  }, 0);
};
```

#### 4. `src/pages/ChatPage.tsx`
**Changes**:
- Pass `targetLanguageCode` as `languageCode` prop to `MessageInput`

```typescript
<MessageInput
  onSend={handleSendMessage}
  disabled={isSending}
  languageCode={targetLanguageCode}
/>
```

### Implementation Steps

1. **Create keyboard layouts utility** (`keyboardLayouts.ts`)
   - Define layouts for 5-10 major languages
   - Focus on simplified layouts (special characters only)
   - Add helper function to get layout by language code

2. **Create VirtualKeyboard component** (`VirtualKeyboard.tsx`)
   - Implement keyboard icon button (inline SVG)
   - Implement panel with character buttons
   - Add mode toggle functionality
   - Add close-on-outside-click behavior
   - Style to match existing UI

3. **Update MessageInput component**
   - Add languageCode prop
   - Add keyboard open/close state
   - Implement character insertion at cursor
   - Integrate VirtualKeyboard component
   - Position icon appropriately

4. **Update ChatPage**
   - Pass targetLanguageCode to MessageInput

5. **Testing**
   - Test character insertion at different cursor positions
   - Test with multiple languages
   - Test keyboard open/close behavior
   - Test on mobile (touch events)
   - Test focus management

### UI Design Details

#### Keyboard Icon
- Use inline SVG keyboard icon (⌨️)
- Position: absolute right inside textarea border, or as separate button
- Size: 20x20px
- Color: slate-400, hover: brand-500
- Add tooltip: "Virtual Keyboard"

#### Keyboard Panel
- Background: white with border-slate-200
- Shadow: shadow-lg
- Border radius: rounded-xl
- Padding: p-4
- Position: absolute, below textarea
- Z-index: z-50
- Width: fit content (min 300px, max 600px)

#### Character Buttons
- Grid layout: grid-cols-8 md:grid-cols-10
- Gap: gap-2
- Button size: w-10 h-10
- Background: bg-slate-100 hover:bg-brand-100
- Border: border border-slate-300
- Border radius: rounded-lg
- Text: text-lg font-medium
- Transition: smooth hover effect

#### Mode Toggle
- Position: top-right of keyboard panel
- Button: small outline button
- Text: "Simplified" / "Full"

### Future Enhancements
1. Add full keyboard layouts (complete QWERTY with language-specific modifications)
2. Add frequently used characters tracking
3. Add keyboard shortcuts (Alt+K to toggle)
4. Add support for more languages
5. Persist user's preferred mode (simplified/full)
6. Add copy-paste support for special character combinations

## Technical Considerations

### TypeScript Imports
- Use regular import for enums: `import { MessageRole } from '../../types'`
- Use type import for interfaces: `import type { Language } from '../../types'`

### Accessibility
- Add aria-label to keyboard icon button
- Add aria-label to character buttons
- Support ESC key to close
- Maintain keyboard focus management

### Performance
- Lazy load keyboard layouts only when needed
- Memoize character buttons rendering
- Use React.memo for VirtualKeyboard if needed

### Browser Compatibility
- Ensure textarea selection API works (selectionStart, setSelectionRange)
- Test on major browsers (Chrome, Firefox, Safari)

## Definition of Done
- [ ] Keyboard icon appears in MessageInput on ChatPage
- [ ] Clicking icon opens virtual keyboard panel
- [ ] Clicking character inserts it at cursor position in textarea
- [ ] Cursor position is maintained correctly after insertion
- [ ] Textarea remains focused after character insertion
- [ ] Keyboard layout adapts to session's target language
- [ ] Simplified mode shows only special characters
- [ ] Mode toggle switches between simplified/full (if full is implemented)
- [ ] Close on outside click works
- [ ] Close on ESC key works
- [ ] UI matches existing design system
- [ ] Works on mobile devices
- [ ] No TypeScript errors
- [ ] No ESLint warnings
