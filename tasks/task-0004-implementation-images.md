# Task 0004: Image Service Implementation

**Status**: Design Phase
**Theme**: Language-Independent Image Management for Vocabulary
**Created**: 2025-10-03

## Overview

Implement a simple image service that provides visual content for vocabulary word cards. Images are identified by language-independent English concept names that come from the LLM when creating vocabulary items.

## Goals

1. **Language Independence**: LLM provides English concept name when creating vocabulary (e.g., Spanish "manzana" → concept "apple")
2. **Simplicity**: Store images in database, serve via REST API
3. **Auto-loading**: Load images from filesystem directory at startup (filename = concept name)

## Use Case

**Vocabulary creation with images:**
1. LLM creates vocabulary for Spanish "manzana" → includes `conceptName: "apple"`
2. `VocabularyItemEntity` stores `conceptName = "apple"`
3. Frontend requests vocabulary → includes `imageUrl: "/api/v1/images/concept/apple/data"`
4. Image service serves apple.png from database (auto-loaded at startup)

## Domain Model

```kotlin
@Entity
@Table(name = "images")
class ImageEntity(
    @field:Id
    val id: UUID = UUID.randomUUID(),

    @field:Column(unique = true, nullable = false, length = 256)
    val concept: String,  // e.g., "apple", "book", "running"

    @field:Column(nullable = false, length = 16)
    val format: String,  // "png", "jpg", "webp"

    @field:Column(nullable = false)
    val widthPx: Int,

    @field:Column(nullable = false)
    val heightPx: Int,

    @field:Lob
    @field:Column(nullable = false)
    val data: ByteArray,  // Image stored directly in DB

    @field:CreationTimestamp
    @field:Column(nullable = false, updatable = false)
    val createdAt: Instant
)
```

**VocabularyItemEntity** gets new field:

```kotlin
@field:Column(name = "concept_name", length = 256)
var conceptName: String? = null  // English concept from LLM (e.g., "apple")
```

## Repository

```kotlin
interface ImageRepository : JpaRepository<ImageEntity, UUID> {
    fun findByConcept(concept: String): ImageEntity?
}
```

## Service

```kotlin
interface ImageService {
    fun getImageByConcept(concept: String): ImageEntity?
    fun createImage(concept: String, data: ByteArray, format: String, width: Int, height: Int): ImageEntity
    fun deleteImage(concept: String)
    fun loadImagesFromDirectory(directory: Path)  // Called at startup
}

@Service
class ImageServiceImpl(
    private val imageRepository: ImageRepository
) : ImageService, ApplicationRunner {

    @Value("\${ai-tutor.images.seed-directory:src/main/resources/images}")
    private lateinit var seedDirectory: String

    override fun run(args: ApplicationArguments) {
        // Auto-load images at startup
        val dir = Paths.get(seedDirectory)
        if (Files.exists(dir)) {
            loadImagesFromDirectory(dir)
        }
    }

    override fun loadImagesFromDirectory(directory: Path) {
        Files.list(directory)
            .filter { Files.isRegularFile(it) }
            .forEach { path ->
                val filename = path.fileName.toString()
                val concept = filename.substringBeforeLast('.')

                // Skip if already exists
                if (imageRepository.findByConcept(concept) != null) return@forEach

                val imageData = Files.readAllBytes(path)
                val image = ImageIO.read(ByteArrayInputStream(imageData))
                val format = filename.substringAfterLast('.', "png")

                createImage(concept, imageData, format, image.width, image.height)
            }
    }
}
```

## Concept Naming

Simple English with hyphens:
- Basic: `apple`, `book`, `car`, `house`, `water`
- Compound: `coffee-cup`, `airplane-ticket`
- Actions: `running`, `eating`, `studying`

## Storage

Database BLOB storage (max 500KB per image, WebP/PNG/JPG format).

## Vocabulary Integration

**VocabularyService** stores `conceptName` when LLM creates vocabulary:

```kotlin
@Transactional
fun recordVocabulary(newVocab: NewVocabulary) {
    val item = vocabularyItemRepository.findByUserIdAndLemmaAndLang(...)
        ?: VocabularyItemEntity(...)

    item.conceptName = newVocab.conceptName  // NEW: Store from LLM
    vocabularyItemRepository.save(item)
}
```

**VocabularyQueryService** returns image URL:

```kotlin
fun getVocabularyItemResponse(item: VocabularyItemEntity): VocabularyItemResponse {
    return VocabularyItemResponse(
        id = item.id,
        lemma = item.lemma,
        // ...
        imageUrl = item.conceptName?.let { "/api/v1/images/concept/$it/data" }
    )
}
```

## REST API

```kotlin
@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    private val imageService: ImageService
) {
    @GetMapping("/concept/{concept}/data")
    fun getImageData(@PathVariable concept: String): ResponseEntity<ByteArray> {
        val image = imageService.getImageByConcept(concept)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/${image.format}"))
            .header("Cache-Control", "public, max-age=31536000")
            .body(image.data)
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadImage(
        @RequestParam concept: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<Void> {
        val image = ImageIO.read(file.inputStream)
        imageService.createImage(
            concept = concept,
            data = file.bytes,
            format = file.contentType?.substringAfter("/") ?: "png",
            width = image.width,
            height = image.height
        )
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/concept/{concept}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteImage(@PathVariable concept: String): ResponseEntity<Void> {
        imageService.deleteImage(concept)
        return ResponseEntity.ok().build()
    }
}
```

## Implementation Steps

1. Add `conceptName` field to `VocabularyItemEntity`
2. Update `NewVocabulary` domain model to include `conceptName`
3. Create `ImageEntity` and `ImageRepository`
4. Implement `ImageService` with auto-loading from directory
5. Create `ImageController` with 3 endpoints
6. Update LLM prompt to return `conceptName` for vocabulary
7. Update `VocabularyService` to store `conceptName`
8. Update `VocabularyQueryService` to include `imageUrl` in responses
9. Add ~20 images to `src/main/resources/images/` (e.g., `apple.png`, `book.jpg`)
10. Test with HTTP client

## Seed Images

Place images in `src/main/resources/images/`:
- `apple.png`, `bread.png`, `coffee.png`, `water.png`, `rice.png`, `fish.png`
- `book.png`, `pen.png`, `phone.png`, `car.png`, `house.png`, `door.png`
- `running.png`, `eating.png`, `studying.png`, `reading.png`
- `person.png`, `family.png`, `friend.png`, `day.png`, `night.png`

Source: Unsplash/Pexels (public domain, no attribution required).

## Configuration

```yaml
ai-tutor:
  images:
    seed-directory: src/main/resources/images  # Auto-load images at startup
```

## Package Structure

```
ch.obermuhlner.aitutor.image/
├── controller/
│   └── ImageController
├── service/
│   ├── ImageService
│   ├── ImageServiceImpl
│   ├── ConceptMappingService
│   └── ConceptMappingServiceImpl
├── repository/
│   └── ImageRepository
└── domain/
    └── ImageEntity
```
