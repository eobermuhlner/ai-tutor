# Task 0013: Create Missing Curriculum Files

## Problem
The application has course configurations in `application.yml` but missing corresponding curriculum files in `src/main/resources/course-content/`. The new validation in `SeedDataService` prevents the application from starting when there are configured courses without curriculum files.

## Current Situation
Application configuration includes courses in application.yml that don't have corresponding directories in the file system, causing startup failures.

### Courses in Application Configuration:
- es-ES: Spanish for Travelers
- it-IT: Italian for Travelers, Italian Grammar Fundamentals  
- fr-FR: French for Travelers, French Grammar Fundamentals
- de-DE: German for Travelers, German Grammar Fundamentals
- de-CH: Swiss German Essentials, Swiss German Conversation
- ja-JP: Japanese for Travelers, Japanese Grammar Fundamentals
- ko-KR: Korean for K-pop fans, Korean for Manhwa fans, Korean Grammar Fundamentals
- zh-CN: Mandarin Pinyin Starter, Mandarin Character Bridge, Mandarin for Travelers
- zh-TW: Conversational Mandarin Taiwan, Mandarin Character Bridge, Mandarin for Travelers
- zh-HK: Cantonese Conversation, Cantonese Character Bridge, Cantonese for Travelers
- ru-RU: Russian Grammar Fundamentals
- pt-BR: Brazilian Portuguese for Travelers
- pt-PT: European Portuguese Essentials, Portuguese Conversation
- en-US: Conversational American English, American English for Travelers, American English Grammar
- en-GB: Conversational British English, British English for Travelers, British English Grammar

### Existing Directories:
- de-conversational-german
- es-conversational-spanish
- fr-conversational-french
- it-conversational-italian
- ja-conversational-japanese
- ko-conversational-korean
- pt-br-conversational-portuguese
- pt-pt-european-portuguese
- ru-conversational-russian
- zh-cn-conversational-mandarin

## Solution Required
Create missing curriculum directories with:
1. curriculum.yml files for the course structure
2. Initial lesson files following the existing structure
3. Follow the existing lesson format with YAML frontmatter and structured sections

## Action Items
1. Create each missing curriculum directory
2. Generate appropriate curriculum.yml files
3. Create initial lesson files for each course
4. Ensure proper CEFR levels and progression criteria
5. Include 8-10 weeks of content per course

## Required Files to Create

### Spanish Courses
- `src/main/resources/course-content/es-spanish-for-travelers/`
- `src/main/resources/course-content/es-travelers.yml` → `curriculum.yml`
- 10 lesson files (week-01 to week-10)

### Italian Courses
- `src/main/resources/course-content/it-italian-for-travelers/`
- `src/main/resources/course-content/it-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/it-italian-grammar-fundamentals/`
- `src/main/resources/course-content/it-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### French Courses
- `src/main/resources/course-content/fr-french-for-travelers/`
- `src/main/resources/course-content/fr-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/fr-french-grammar-fundamentals/`
- `src/main/resources/course-content/fr-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### German Courses
- `src/main/resources/course-content/de-german-for-travelers/`
- `src/main/resources/course-content/de-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/de-german-grammar-fundamentals/`
- `src/main/resources/course-content/de-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### Swiss German Courses
- `src/main/resources/course-content/de-ch-swiss-german-essentials/`
- `src/main/resources/course-content/de-ch-swiss-essentials.yml` → `curriculum.yml`
- 12 lesson files

- `src/main/resources/course-content/de-ch-swiss-german-conversation/`
- `src/main/resources/course-content/de-ch-swiss-conversation.yml` → `curriculum.yml`
- 10 lesson files

### Japanese Courses
- `src/main/resources/course-content/ja-japanese-for-travelers/`
- `src/main/resources/course-content/ja-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/ja-japanese-grammar-fundamentals/`
- `src/main/resources/course-content/ja-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### Korean Courses
- `src/main/resources/course-content/ko-korean-for-k-pop-fans/`
- `src/main/resources/course-content/ko-k-pop-fans.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/ko-korean-for-manhwa-fans/`
- `src/main/resources/course-content/ko-manhwa-fans.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/ko-korean-grammar-fundamentals/`
- `src/main/resources/course-content/ko-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### Mandarin Courses
- `src/main/resources/course-content/zh-cn-mandarin-pinyin-starter/`
- `src/main/resources/course-content/zh-cn-pinyin-starter.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/zh-cn-mandarin-character-bridge/`
- `src/main/resources/course-content/zh-cn-character-bridge.yml` → `curriculum.yml`
- 12 lesson files

- `src/main/resources/course-content/zh-cn-mandarin-for-travelers/`
- `src/main/resources/course-content/zh-cn-travelers.yml` → `curriculum.yml`
- 10 lesson files

### Traditional Mandarin Courses
- `src/main/resources/course-content/zh-tw-mandarin-conversation/`
- `src/main/resources/course-content/zh-tw-conversation.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/zh-tw-mandarin-character-bridge/`
- `src/main/resources/course-content/zh-tw-character-bridge.yml` → `curriculum.yml`
- 12 lesson files

- `src/main/resources/course-content/zh-tw-mandarin-for-travelers/`
- `src/main/resources/course-content/zh-tw-travelers.yml` → `curriculum.yml`
- 10 lesson files

### Cantonese Courses
- `src/main/resources/course-content/zh-hk-cantonese-conversation/`
- `src/main/resources/course-content/zh-hk-cantonese.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/zh-hk-cantonese-character-bridge/`
- `src/main/resources/course-content/zh-hk-character-bridge.yml` → `curriculum.yml`
- 12 lesson files

- `src/main/resources/course-content/zh-hk-cantonese-for-travelers/`
- `src/main/resources/course-content/zh-hk-travelers.yml` → `curriculum.yml`
- 10 lesson files

### Russian Courses
- `src/main/resources/course-content/ru-russian-grammar-fundamentals/`
- `src/main/resources/course-content/ru-grammar-fundamentals.yml` → `curriculum.yml`
- 12 lesson files

### Brazilian Portuguese Courses
- `src/main/resources/course-content/pt-br-brazilian-portuguese-for-travelers/`
- `src/main/resources/course-content/pt-br-travelers.yml` → `curriculum.yml`
- 10 lesson files

### European Portuguese Courses
- `src/main/resources/course-content/pt-pt-european-portuguese-essentials/`
- `src/main/resources/course-content/pt-pt-essentials.yml` → `curriculum.yml`
- 12 lesson files

- `src/main/resources/course-content/pt-pt-portuguese-conversation/`
- `src/main/resources/course-content/pt-pt-conversation.yml` → `curriculum.yml`
- 10 lesson files

### American English Courses
- `src/main/resources/course-content/en-us-american-english-for-travelers/`
- `src/main/resources/course-content/en-us-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/en-us-american-english-grammar/`
- `src/main/resources/course-content/en-us-grammar.yml` → `curriculum.yml`
- 12 lesson files

### British English Courses
- `src/main/resources/course-content/en-gb-british-english-for-travelers/`
- `src/main/resources/course-content/en-gb-travelers.yml` → `curriculum.yml`
- 10 lesson files

- `src/main/resources/course-content/en-gb-british-english-grammar/`
- `src/main/resources/course-content/en-gb-grammar.yml` → `curriculum.yml`
- 12 lesson files

## Expected Structure for Each Course
Each course directory should contain:
- `curriculum.yml` - Defines the course structure, lessons, progression mode
- `week-01-*.md` through appropriate week numbers - Individual lesson files with:
  - YAML frontmatter with lesson metadata
  - "This Week's Goals"
  - "Grammar Focus" 
  - "Essential Vocabulary"
  - "Conversation Scenarios"
  - "Practice Patterns"
  - "Common Mistakes to Watch"
  - "Cultural Notes"

## Success Criteria
- Application starts successfully without validation errors
- All configured courses have corresponding curriculum files
- Each course has appropriate number of weeks/lessons based on estimated duration
- Content follows pedagogical quality standards of existing lessons