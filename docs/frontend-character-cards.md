# Frontend Implementation Guide: Character Cards

## Overview

Character cards are a new card type for teaching individual characters/symbols in special writing systems (hiragana, katakana, hangul, cyrillic, kanji, etc.). They appear alongside word cards in chat messages.

## API Response Structure

### Message Response Format

```json
{
  "id": "uuid",
  "role": "ASSISTANT",
  "content": "Let's learn some hiragana vowels!",
  "corrections": null,
  "newVocabulary": null,
  "wordCards": [
    {
      "titleSourceLanguage": "apple",
      "titleTargetLanguage": "りんご",
      "descriptionSourceLanguage": "A round fruit",
      "descriptionTargetLanguage": "丸い果物",
      "conceptName": "apple",
      "imageUrl": "/api/v1/images/concept/apple/data"
    }
  ],
  "characterCards": [
    {
      "character": "あ",
      "pronunciation": "a",
      "description": "Pronounced like 'a' in 'father'. First vowel in the hiragana syllabary. Appears in words like ありがとう (arigatou)."
    },
    {
      "character": "い",
      "pronunciation": "i",
      "description": "Pronounced like 'ee' in 'see'. Second vowel in hiragana. Used in いい (ii - good)."
    }
  ],
  "errorMessage": null,
  "createdAt": "2025-10-28T20:30:00Z"
}
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `character` | `string` | 1-3 characters in target language (e.g., "あ", "Д", "食") - the character being taught |
| `pronunciation` | `string` | Romanization/pronunciation guide (e.g., "a", "d", "shoku") |
| `description` | `string` | Learning guidance in learner's native language (pronunciation tips, usage examples, stroke order, mnemonics, etc.) |

**Important:** Character cards are **optional** and may be `null` or an empty array `[]` if not present in the message.

---

## Display Design Recommendations

### 1. Flashcard-Style Layout

Character cards should be displayed with a **large-font flashcard aesthetic** to emphasize the character being learned.

#### Recommended Layout (Desktop/Tablet)

```
┌─────────────────────────────────────┐
│  FRONT                              │
│                                     │
│            あ                       │  ← Large font (72-96px)
│                                     │
├─────────────────────────────────────┤
│  BACK                               │
│                                     │
│  Pronunciation: a                   │  ← Medium font (18-20px), semi-bold
│                                     │
│  Pronounced like 'a' in 'father'.   │  ← Normal font (14-16px)
│  First vowel in the hiragana        │
│  syllabary. Appears in words like   │
│  ありがとう (arigatou).              │
│                                     │
└─────────────────────────────────────┘
```

#### Recommended Layout (Mobile)

```
┌──────────────────────┐
│  FRONT               │
│                      │
│        あ            │  ← Large font (56-72px)
│                      │
├──────────────────────┤
│  BACK                │
│                      │
│  Pronunciation: a    │  ← Medium font (16-18px)
│                      │
│  Pronounced like     │  ← Normal font (14px)
│  'a' in 'father'.    │
│  First vowel in...   │
│                      │
└──────────────────────┘
```

---

## Implementation Guidelines

### 2. Visual Design Specifications

#### Typography

- **Character (Front)**:
  - Font size: **72-96px** (desktop), **56-72px** (mobile)
  - Font weight: **500-600** (medium to semi-bold)
  - Line height: **1.0-1.2**
  - Text align: **center**
  - Font family: Use a web font that supports the target writing system
    - Japanese: `"Noto Sans JP", "Hiragino Sans", sans-serif`
    - Korean: `"Noto Sans KR", "Malgun Gothic", sans-serif`
    - Cyrillic: `"Noto Sans", "Roboto", sans-serif`
    - Chinese: `"Noto Sans SC", "Microsoft YaHei", sans-serif`
    - Fallback: `sans-serif`

- **Pronunciation (Back)**:
  - Font size: **18-20px** (desktop), **16-18px** (mobile)
  - Font weight: **600** (semi-bold)
  - Color: Primary accent color or darker neutral
  - Prefix label: "Pronunciation: " in lighter weight

- **Description (Back)**:
  - Font size: **14-16px** (desktop), **14px** (mobile)
  - Font weight: **400** (normal)
  - Line height: **1.5-1.6**
  - Color: Secondary text color
  - Max width: **400-500px** (prevent overly long lines)

#### Colors & Styling

- **Card background**: Light neutral (`#FFFFFF` or `#F9FAFB`)
- **Card border**: Subtle border (`1px solid #E5E7EB` or similar)
- **Border radius**: `8-12px` for modern look
- **Shadow**: Subtle elevation (`box-shadow: 0 2px 8px rgba(0,0,0,0.08)`)
- **Divider line**: Between FRONT and BACK (`1px solid #E5E7EB`)

#### Spacing

- **Card padding**: `24px` (desktop), `16px` (mobile)
- **Front section height**: `120-160px` (fixed height for consistency)
- **Back section padding**: `16-20px` vertical, `20-24px` horizontal
- **Pronunciation margin**: `8px` bottom (space before description)
- **Card gap**: `16px` between multiple cards

---

### 3. Responsive Behavior

#### Desktop (≥1024px)
- Display character cards in a **2-column grid**
- Card width: `calc(50% - 8px)` (with 16px gap)
- Maximum 2 cards per row

#### Tablet (768px - 1023px)
- Display character cards in a **2-column grid** (same as desktop)
- Slightly reduce font sizes if needed

#### Mobile (<768px)
- Display character cards in a **single column** (100% width)
- Stack cards vertically with 16px gap
- Reduce font sizes per mobile specs above

---

### 4. Integration with Word Cards

#### Display Order

Character cards should appear **after word cards** in the message response:

```
┌─────────────────────────────────────┐
│  Chat Message Content               │
│  "Let's learn some vocabulary..."   │
└─────────────────────────────────────┘

🃏 Word Cards (if present)
┌─────────────────────────────────────┐
│  りんご / apple                      │
│  [image]                            │
└─────────────────────────────────────┘

🔤 Character Cards (if present)
┌─────────────────────────────────────┐
│  FRONT: あ                           │
│  BACK: Pronunciation: a...          │
└─────────────────────────────────────┘
```

#### Section Headers

- **Word Cards**: Use icon 🃏 or "💳" + "Vocabulary Cards"
- **Character Cards**: Use icon 🔤 or "📝" + "Character Cards"
- Font size: **16-18px**, semi-bold
- Margin: **16px** top, **12px** bottom

#### Visual Distinction

- Word cards have **images** (via `imageUrl` field)
- Character cards have **large-font character display** (no images)
- Use different icons/headers to distinguish sections

---

### 5. Interactive Features (Optional Enhancements)

#### Flip Animation (Recommended)

Implement a flip animation to toggle between FRONT and BACK:

1. **Initial state**: Show FRONT (large character)
2. **On click/tap**: Flip card to show BACK (pronunciation + description)
3. **Animation**: 3D CSS flip transform (0.6s duration, `ease-in-out`)
4. **State indicator**: Small "Tap to flip" hint on first card

**Benefits**: Mimics physical flashcards, encourages active recall

#### Example CSS (Optional)

```css
.character-card {
  perspective: 1000px;
  cursor: pointer;
}

.character-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.character-card.flipped .character-card-inner {
  transform: rotateY(180deg);
}

.character-card-front,
.character-card-back {
  position: absolute;
  width: 100%;
  backface-visibility: hidden;
}

.character-card-back {
  transform: rotateY(180deg);
}
```

#### Alternative: Accordion/Expand (Simpler)

If flip animation is too complex:
- Show FRONT by default (collapsed)
- Add "Show pronunciation" button/link
- Expand to reveal BACK section below
- Icon changes to indicate expanded state

---

### 6. Accessibility Considerations

#### Screen Readers

- Add `role="article"` or `role="region"` to each card
- Use `aria-label="Character card: {character}"` on card container
- Provide clear semantic structure with heading tags

#### Keyboard Navigation

- Cards should be **focusable** (`tabindex="0"`)
- Press **Enter** or **Space** to flip/expand card
- Visual focus indicator (outline/shadow)

#### Text Contrast

- Ensure WCAG AA compliance (4.5:1 contrast ratio)
- Character text should be **dark on light** or vice versa
- Test with color contrast checker tools

#### Font Rendering

- Test character rendering across browsers/devices
- Some characters may not render well in all fonts
- Provide font fallback chain for writing system support

---

### 7. Example React Component (TypeScript)

```tsx
import React, { useState } from 'react';

interface CharacterCard {
  character: string;
  pronunciation: string;
  description: string;
}

interface CharacterCardsProps {
  cards: CharacterCard[];
}

const CharacterCards: React.FC<CharacterCardsProps> = ({ cards }) => {
  if (!cards || cards.length === 0) {
    return null;
  }

  return (
    <div className="character-cards-section">
      <h3 className="cards-header">
        <span className="icon">🔤</span> Character Cards
      </h3>
      <div className="character-cards-grid">
        {cards.map((card, index) => (
          <CharacterCardItem key={index} card={card} />
        ))}
      </div>
    </div>
  );
};

const CharacterCardItem: React.FC<{ card: CharacterCard }> = ({ card }) => {
  const [isFlipped, setIsFlipped] = useState(false);

  const handleToggle = () => {
    setIsFlipped(!isFlipped);
  };

  return (
    <div
      className={`character-card ${isFlipped ? 'flipped' : ''}`}
      onClick={handleToggle}
      onKeyPress={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          handleToggle();
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`Character card: ${card.character}`}
    >
      <div className="character-card-inner">
        {/* FRONT */}
        <div className="character-card-front">
          <div className="card-label">FRONT</div>
          <div className="character-display">{card.character}</div>
          <div className="tap-hint">Tap to reveal</div>
        </div>

        {/* BACK */}
        <div className="character-card-back">
          <div className="card-label">BACK</div>
          <div className="pronunciation">
            <span className="pronunciation-label">Pronunciation:</span>{' '}
            {card.pronunciation}
          </div>
          <div className="description">{card.description}</div>
        </div>
      </div>
    </div>
  );
};

export default CharacterCards;
```

#### Corresponding CSS (Tailwind-style)

```css
.character-cards-section {
  margin-top: 1rem;
}

.cards-header {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.character-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.character-card {
  perspective: 1000px;
  cursor: pointer;
  min-height: 280px;
}

.character-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.character-card.flipped .character-card-inner {
  transform: rotateY(180deg);
}

.character-card-front,
.character-card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.character-card-back {
  transform: rotateY(180deg);
}

.card-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 1rem;
}

.character-display {
  font-size: 5rem;
  font-weight: 500;
  text-align: center;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Noto Sans JP', 'Noto Sans KR', 'Noto Sans', sans-serif;
}

.tap-hint {
  font-size: 0.875rem;
  color: #9ca3af;
  text-align: center;
  margin-top: 0.5rem;
}

.pronunciation {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #1f2937;
}

.pronunciation-label {
  font-weight: 400;
  color: #6b7280;
}

.description {
  font-size: 0.9375rem;
  line-height: 1.6;
  color: #4b5563;
}

/* Mobile adjustments */
@media (max-width: 768px) {
  .character-cards-grid {
    grid-template-columns: 1fr;
  }

  .character-display {
    font-size: 4rem;
  }

  .character-card {
    min-height: 240px;
  }
}
```

---

### 8. Example Vue Component (TypeScript)

```vue
<template>
  <div v-if="cards && cards.length > 0" class="character-cards-section">
    <h3 class="cards-header">
      <span class="icon">🔤</span> Character Cards
    </h3>
    <div class="character-cards-grid">
      <CharacterCardItem
        v-for="(card, index) in cards"
        :key="index"
        :card="card"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
interface CharacterCard {
  character: string;
  pronunciation: string;
  description: string;
}

interface Props {
  cards: CharacterCard[];
}

defineProps<Props>();
</script>

<script setup lang="ts">
import { ref } from 'vue';

interface CharacterCard {
  character: string;
  pronunciation: string;
  description: string;
}

interface Props {
  card: CharacterCard;
}

defineProps<Props>();

const isFlipped = ref(false);

const handleToggle = () => {
  isFlipped.value = !isFlipped.value;
};
</script>

<template>
  <div
    :class="['character-card', { flipped: isFlipped }]"
    @click="handleToggle"
    @keypress.enter="handleToggle"
    @keypress.space.prevent="handleToggle"
    role="button"
    tabindex="0"
    :aria-label="`Character card: ${card.character}`"
  >
    <div class="character-card-inner">
      <!-- FRONT -->
      <div class="character-card-front">
        <div class="card-label">FRONT</div>
        <div class="character-display">{{ card.character }}</div>
        <div class="tap-hint">Tap to reveal</div>
      </div>

      <!-- BACK -->
      <div class="character-card-back">
        <div class="card-label">BACK</div>
        <div class="pronunciation">
          <span class="pronunciation-label">Pronunciation:</span>
          {{ card.pronunciation }}
        </div>
        <div class="description">{{ card.description }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Use the same CSS as React example above */
</style>
```

---

### 9. Testing Checklist

Before deploying character cards to production, test the following:

#### Visual Testing
- ✅ Character renders correctly in all target writing systems (Japanese, Korean, Russian, Chinese)
- ✅ Layout looks good on desktop, tablet, and mobile
- ✅ Card flip animation (if implemented) works smoothly
- ✅ Font fallbacks work when primary font unavailable
- ✅ No text overflow or layout breaking with long descriptions

#### Functional Testing
- ✅ Cards display only when `characterCards` array is present and non-empty
- ✅ Clicking/tapping toggles card state (flip or expand)
- ✅ Keyboard navigation works (Tab to focus, Enter/Space to flip)
- ✅ Screen reader announces card content correctly
- ✅ Multiple cards display in correct grid layout

#### Edge Cases
- ✅ Empty `characterCards` array: No section displayed
- ✅ `characterCards` is `null`: No section displayed
- ✅ Very long description text: Wraps correctly, no overflow
- ✅ Single character vs. multiple characters (e.g., "あい"): Both display well
- ✅ Mix of word cards and character cards: Both sections display correctly

#### Cross-Browser Testing
- ✅ Chrome, Firefox, Safari, Edge (latest versions)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)
- ✅ Font rendering quality across browsers

---

### 10. Example API Responses by Language

#### Japanese (Hiragana)

```json
{
  "characterCards": [
    {
      "character": "あ",
      "pronunciation": "a",
      "description": "Pronounced like 'a' in 'father'. First vowel in the hiragana syllabary. Appears in words like ありがとう (arigatou - thank you)."
    },
    {
      "character": "い",
      "pronunciation": "i",
      "description": "Pronounced like 'ee' in 'see'. Second vowel in hiragana. Used in いい (ii - good)."
    }
  ]
}
```

#### Russian (Cyrillic)

```json
{
  "characterCards": [
    {
      "character": "Д",
      "pronunciation": "d",
      "description": "Pronounced like 'd' in 'dog'. Capital form shown; lowercase is 'д'. Example: Дом (dom) means 'house'. Written with 2 strokes."
    },
    {
      "character": "Л",
      "pronunciation": "l",
      "description": "Pronounced like 'l' in 'lamp'. Lowercase is 'л'. Example: Луна (luna) means 'moon'."
    }
  ]
}
```

#### Korean (Hangul)

```json
{
  "characterCards": [
    {
      "character": "ㄱ",
      "pronunciation": "g/k",
      "description": "Consonant 'giyeok'. Sounds like 'g' in 'go' at start of word, 'k' at end. First letter of Hangul alphabet. Example: 가 (ga)."
    },
    {
      "character": "ㅏ",
      "pronunciation": "a",
      "description": "Vowel 'a'. Pronounced like 'a' in 'father'. Combines with consonants to form syllables. Example: 가 (ga) = ㄱ + ㅏ."
    }
  ]
}
```

#### Japanese (Kanji)

```json
{
  "characterCards": [
    {
      "character": "食",
      "pronunciation": "shoku / ta(beru)",
      "description": "Kanji for 'eat' or 'food'. Readings: 'shoku' (on-reading), 'taberu' (kun-reading). 9 strokes. Used in 食べる (taberu - to eat), 食事 (shokuji - meal)."
    }
  ]
}
```

---

## Summary

- **Character cards** (`characterCards`) teach individual characters/symbols in special writing systems
- Display with **large-font flashcard layout**: FRONT (character) + BACK (pronunciation + description)
- Show **after word cards** in the message response
- Use **2-column grid** (desktop/tablet) or **1-column** (mobile)
- Implement **flip animation** (recommended) or **expand/collapse** (simpler)
- Ensure **accessibility**: keyboard navigation, screen reader support, text contrast
- Test across **browsers, devices, and writing systems**

This design provides an effective, visually distinct way to teach writing systems while maintaining consistency with the existing word card interface.
