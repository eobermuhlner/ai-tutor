# Task 0004: Frontend Implementation - Image Integration for Vocabulary and Word Cards

**Status**: Ready for Implementation
**Theme**: Visual Learning with Language-Independent Images
**Created**: 2025-10-03
**Backend Status**: ✅ Complete

## Overview

The AI Tutor backend now returns image URLs for vocabulary items and word cards in chat responses. Images are served based on a **language-independent concept name** (e.g., "apple", "running", "coffee-cup") that the LLM provides alongside vocabulary and word cards.

## API Response Structure

### Chat Message Response

**Endpoint**: `POST /api/v1/chat/sessions/{id}/messages`

**Response Example**:
```json
{
  "id": "uuid",
  "role": "ASSISTANT",
  "content": "¡Hola! ¿Cómo estás?",
  "corrections": [...],
  "newVocabulary": [
    {
      "lemma": "manzana",
      "context": "Me gusta comer una manzana",
      "conceptName": "apple",
      "imageUrl": "/api/v1/images/concept/apple/data"
    }
  ],
  "wordCards": [
    {
      "titleSourceLanguage": "Apple",
      "titleTargetLanguage": "Manzana",
      "descriptionSourceLanguage": "A red or green fruit",
      "descriptionTargetLanguage": "Una fruta roja o verde",
      "conceptName": "apple",
      "imageUrl": "/api/v1/images/concept/apple/data"
    }
  ],
  "createdAt": "2025-10-03T20:00:00Z"
}
```

### Vocabulary List Response

**Endpoint**: `GET /api/v1/vocabulary?userId={id}&lang={lang}`

**Response Example**:
```json
[
  {
    "id": "uuid",
    "lemma": "corriendo",
    "lang": "es-ES",
    "exposures": 3,
    "lastSeenAt": "2025-10-03T20:00:00Z",
    "createdAt": "2025-10-01T10:00:00Z",
    "imageUrl": "/api/v1/images/concept/running/data"
  }
]
```

## Image Endpoints

### Get Image by Concept

```
GET /api/v1/images/concept/{conceptName}/data
```

**Response Headers**:
- Content-Type: `image/png`, `image/jpg`, or `image/webp`
- Cache-Control: `public, max-age=31536000` (1 year)

**Examples**:
- `/api/v1/images/concept/apple/data` → Apple image
- `/api/v1/images/concept/running/data` → Running image
- `/api/v1/images/concept/coffee-cup/data` → Coffee cup image

**Error Handling**:
- Returns `404 Not Found` if concept has no image
- Requires authentication (same as other API endpoints)

## Concept Naming Convention

Concepts use **simple English identifiers** with hyphens for compound words:

- **Basic nouns**: `apple`, `book`, `car`, `house`, `water`
- **Compound nouns**: `coffee-cup`, `airplane-ticket`, `cell-phone`
- **Actions/Verbs**: `running`, `eating`, `studying`, `reading`
- **Adjectives**: `red`, `happy`, `small`, `cold`

**Important**: Not all vocabulary/word cards will have a `conceptName` or `imageUrl`. Handle `null` values gracefully.

## TypeScript Interfaces

```typescript
// Vocabulary with image
interface VocabularyWithImage {
  lemma: string;
  context: string;
  conceptName?: string | null;
  imageUrl?: string | null;
}

// Word card with image
interface WordCardWithImage {
  titleSourceLanguage: string;
  titleTargetLanguage: string;
  descriptionSourceLanguage: string;
  descriptionTargetLanguage: string;
  conceptName?: string | null;
  imageUrl?: string | null;
}

// Chat message response
interface MessageResponse {
  id: string;
  role: string;
  content: string;
  corrections?: Correction[] | null;
  newVocabulary?: VocabularyWithImage[] | null;
  wordCards?: WordCardWithImage[] | null;
  createdAt: string;
}

// Vocabulary item from vocabulary list
interface VocabularyItem {
  id: string;
  lemma: string;
  lang: string;
  exposures: number;
  lastSeenAt: string;
  createdAt: string;
  imageUrl?: string | null;
}
```

## UI Implementation Examples

### 1. Vocabulary Card Component

```typescript
function VocabularyCard({ item }: { item: VocabularyItem }) {
  return (
    <div className="vocabulary-card">
      {item.imageUrl ? (
        <img
          src={item.imageUrl}
          alt={`Visual representation of ${item.lemma}`}
          className="vocabulary-image"
          loading="lazy"
          onError={(e) => {
            // Hide image if it fails to load
            e.currentTarget.style.display = 'none';
          }}
        />
      ) : (
        <div className="image-placeholder">
          <span className="placeholder-icon">📝</span>
        </div>
      )}
      <div className="vocabulary-details">
        <h3>{item.lemma}</h3>
        <p className="exposure-count">Seen {item.exposures} times</p>
        <p className="last-seen">
          Last: {new Date(item.lastSeenAt).toLocaleDateString()}
        </p>
      </div>
    </div>
  );
}
```

### 2. Chat Message with Vocabulary

```typescript
function ChatMessage({ message }: { message: MessageResponse }) {
  return (
    <div className="chat-message">
      <p className="message-content">{message.content}</p>

      {message.newVocabulary && message.newVocabulary.length > 0 && (
        <div className="new-vocabulary-section">
          <h4 className="section-title">New Vocabulary</h4>
          <div className="vocabulary-grid">
            {message.newVocabulary.map((vocab, idx) => (
              <div key={idx} className="vocab-item">
                {vocab.imageUrl && (
                  <img
                    src={vocab.imageUrl}
                    alt={vocab.lemma}
                    className="vocab-thumbnail"
                    loading="lazy"
                  />
                )}
                <div className="vocab-text">
                  <strong className="vocab-lemma">{vocab.lemma}</strong>
                  <p className="vocab-context">{vocab.context}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
```

### 3. Word Card with Flip Animation

```typescript
function WordCardFlip({ card }: { card: WordCardWithImage }) {
  const [flipped, setFlipped] = useState(false);

  return (
    <div
      className={`word-card ${flipped ? 'flipped' : ''}`}
      onClick={() => setFlipped(!flipped)}
      role="button"
      tabIndex={0}
      onKeyPress={(e) => e.key === 'Enter' && setFlipped(!flipped)}
      aria-label={`Flash card: ${card.titleTargetLanguage}`}
    >
      <div className="card-front">
        {card.imageUrl && (
          <img
            src={card.imageUrl}
            alt={card.titleTargetLanguage}
            className="card-image"
            loading="lazy"
          />
        )}
        <h2 className="card-title">{card.titleTargetLanguage}</h2>
        <p className="card-description">{card.descriptionTargetLanguage}</p>
        <span className="flip-hint">Click to flip</span>
      </div>

      <div className="card-back">
        <h2 className="card-title">{card.titleSourceLanguage}</h2>
        <p className="card-description">{card.descriptionSourceLanguage}</p>
        <span className="flip-hint">Click to flip back</span>
      </div>
    </div>
  );
}
```

### 4. Complete Vocabulary List Component

```typescript
function VocabularyList({ userId, language }: { userId: string; language: string }) {
  const [vocabulary, setVocabulary] = useState<VocabularyItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    fetch(`/api/v1/vocabulary?userId=${userId}&lang=${language}`, {
      headers: {
        'Authorization': `Bearer ${getAccessToken()}`
      }
    })
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch vocabulary');
        return res.json();
      })
      .then(data => {
        setVocabulary(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, [userId, language]);

  if (loading) return <div className="loading">Loading vocabulary...</div>;
  if (error) return <div className="error">Error: {error}</div>;

  return (
    <div className="vocabulary-list">
      <h2 className="list-title">Your Vocabulary ({vocabulary.length} words)</h2>
      <div className="vocabulary-grid">
        {vocabulary.map(item => (
          <VocabularyCard key={item.id} item={item} />
        ))}
      </div>
    </div>
  );
}
```

## CSS Styling Guidelines

### Recommended Styles

```css
/* Vocabulary Card */
.vocabulary-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  transition: box-shadow 0.2s;
}

.vocabulary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.vocabulary-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
  flex-shrink: 0;
}

.image-placeholder {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  flex-shrink: 0;
}

.placeholder-icon {
  font-size: 32px;
  opacity: 0.3;
}

/* Chat Vocabulary Thumbnails */
.vocab-thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 12px;
  flex-shrink: 0;
}

.vocab-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.vocab-text {
  flex: 1;
}

.vocab-lemma {
  font-size: 18px;
  color: #333;
}

.vocab-context {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

/* Word Card */
.word-card {
  width: 320px;
  height: 400px;
  perspective: 1000px;
  cursor: pointer;
}

.word-card.flipped .card-front {
  transform: rotateY(180deg);
}

.word-card.flipped .card-back {
  transform: rotateY(0deg);
}

.card-front,
.card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  transition: transform 0.6s;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: white;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.card-back {
  transform: rotateY(180deg);
}

.card-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.card-description {
  flex: 1;
  font-size: 16px;
  color: #666;
  line-height: 1.5;
}

.flip-hint {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin-top: auto;
}

/* Vocabulary Grid */
.vocabulary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

/* Responsive */
@media (max-width: 768px) {
  .vocabulary-grid {
    grid-template-columns: 1fr;
  }

  .word-card {
    width: 100%;
    max-width: 400px;
  }
}
```

## Image Size Guidelines

1. **Thumbnail Size**: 60x60px to 100x100px for inline vocabulary
2. **Card Images**: 200x200px to 300x300px for word cards
3. **List Images**: 80x80px for vocabulary lists
4. **Aspect Ratio**: Use `object-fit: cover` to handle varying image sizes
5. **Loading**: Use `loading="lazy"` for images below the fold

## Caching Strategy

Images have a 1-year cache lifetime. Implement browser caching:

```typescript
// Preload images for better UX
function preloadImage(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve();
    img.onerror = reject;
    img.src = url;
  });
}

// Preload vocabulary images when message arrives
useEffect(() => {
  if (message.newVocabulary) {
    message.newVocabulary.forEach(vocab => {
      if (vocab.imageUrl) {
        preloadImage(vocab.imageUrl).catch(() => {
          // Silently fail - image will just not display
        });
      }
    });
  }
}, [message]);
```

## Error Handling Checklist

- [ ] Handle missing `imageUrl` (null/undefined) gracefully
- [ ] Hide broken images with `onError` handler
- [ ] Provide fallback placeholder when no image available
- [ ] Handle slow image loading with loading state
- [ ] Handle network errors (show vocabulary without images)
- [ ] Handle 404 responses from image endpoint

## Accessibility Requirements

```typescript
// Good example
<img
  src={vocab.imageUrl}
  alt={`Visual representation of ${vocab.lemma}`}
  role="img"
  aria-label={`Image illustrating the word: ${vocab.lemma}`}
  loading="lazy"
/>

// Word card accessibility
<div
  className="word-card"
  onClick={handleFlip}
  onKeyPress={(e) => e.key === 'Enter' && handleFlip()}
  role="button"
  tabIndex={0}
  aria-label={`Flash card: ${card.titleTargetLanguage}. Press Enter to flip.`}
>
  {/* card content */}
</div>
```

## Performance Optimization

### Lazy Loading

```typescript
// Use Intersection Observer for lazy loading
const VocabularyImage = ({ src, alt }: { src: string; alt: string }) => {
  const [isVisible, setIsVisible] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin: '50px' }
    );

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <img
      ref={imgRef}
      src={isVisible ? src : undefined}
      alt={alt}
      className="vocabulary-image"
    />
  );
};
```

### Image Optimization

- Use `loading="lazy"` attribute for native lazy loading
- Implement progressive image loading for large images
- Consider using WebP format when supported
- Cache images in browser storage for offline use

## Testing Checklist

### Functional Tests
- [ ] Vocabulary items display images when `imageUrl` is present
- [ ] Layout remains clean when `imageUrl` is null
- [ ] Broken image links don't show broken image icon
- [ ] Word cards flip correctly with images
- [ ] Images display in chat message vocabulary section
- [ ] Image placeholder shows when no image available

### Performance Tests
- [ ] Images load efficiently with lazy loading
- [ ] Page doesn't block on image loading
- [ ] Images are properly cached
- [ ] Network tab shows correct cache headers

### Responsive Tests
- [ ] Images scale correctly on mobile
- [ ] Touch interactions work for word card flip
- [ ] Grid layout adapts to screen size
- [ ] Images don't cause horizontal scroll

### Accessibility Tests
- [ ] Alt text present for all images
- [ ] ARIA labels present for interactive elements
- [ ] Keyboard navigation works for word cards
- [ ] Screen reader announces image descriptions

### Error Handling Tests
- [ ] 404 image responses handled gracefully
- [ ] Network errors don't break UI
- [ ] Missing imageUrl doesn't cause errors
- [ ] onError handler prevents broken image icons

## Implementation Phases

### Phase 1: Basic Display (MVP)
1. Update TypeScript interfaces for vocabulary and word cards
2. Display images in vocabulary list
3. Display images in chat message vocabulary
4. Handle null/missing imageUrl cases
5. Add basic error handling (onError)

### Phase 2: Enhanced UX
1. Add word card flip animation with images
2. Implement image placeholders
3. Add lazy loading for images
4. Optimize image loading performance
5. Add loading states/skeletons

### Phase 3: Polish
1. Implement image preloading for better UX
2. Add progressive image loading
3. Optimize mobile experience
4. Add animations and transitions
5. Implement offline image caching

## Key Takeaways

1. **Optional Feature**: Images enhance learning but aren't required—handle nulls gracefully
2. **Language-Independent**: Concept names are in English regardless of target language
3. **Heavily Cached**: Images have 1-year cache lifetime, safe to preload
4. **Visual Learning**: Use images prominently for vocabulary cards and word cards
5. **Fallback UI**: Always provide a good experience even without images
6. **Accessibility**: Ensure images have proper alt text and ARIA labels
7. **Performance**: Use lazy loading and caching for optimal performance

## Backend Integration Complete

The backend implementation includes:
- ✅ Image storage in database with auto-loading from filesystem
- ✅ Image service with concept-based lookup
- ✅ REST API endpoints for image retrieval
- ✅ `conceptName` field in vocabulary and word cards
- ✅ LLM instructed to provide concept names
- ✅ Image URLs automatically included in all API responses
- ✅ Comprehensive test coverage

All backend endpoints are ready for frontend integration!
