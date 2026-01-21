# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sarabun PDF API is a Spring Boot application for generating Thai government document PDFs. Migrated from .NET to Java, using Apache PDFBox 2.x instead of iText (open source licensing). Supports 9 document types with specialized PDF generators using Factory Pattern.

**Tech Stack:** Java 17, Spring Boot 3.5.9, Apache PDFBox 2.0.31, Maven

## Build and Development Commands

```bash
# Build the project
./mvnw clean package

# Run locally (development)
./mvnw spring-boot:run

# Access the application
http://localhost:8888/api/pdf/health
http://localhost:8888/swagger-ui.html
http://localhost:8888/api/pdf/view    # Quick PDF test viewer
http://localhost:8888/api-tester.html # API test interface

# Run with Docker
docker build -t sarabun-pdf-api:1.0.0 .
docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0

# Rebuild Docker (Windows PowerShell)
.\docker-rebuild.ps1

# Quick rebuild for testing
docker stop sarabun-pdf-api && docker rm sarabun-pdf-api && docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

## Security Considerations

**⚠️ IMPORTANT - Current Development Configuration:**

The codebase currently has security features **DISABLED** for development convenience:
- Authentication is **DISABLED** (`permitAll()` in SecurityConfig.java)
- CSRF protection is **DISABLED**
- CORS allows **all origins** (`origins = "*"`)

**Before Production Deployment:**
1. **Enable JWT authentication** (dependencies already included)
2. Configure proper CORS origins (limit to specific domains)
3. Review and enable CSRF for session-based endpoints (if applicable)
4. Set `logging.level=INFO` (not DEBUG to avoid logging sensitive data)
5. Review rate limiting settings for production load
6. Scan dependencies for vulnerabilities: `mvn org.owasp:dependency-check-maven:check`

**Security Features Already Implemented:**
- ✅ Rate limiting (Bucket4j) - configurable per-IP and global limits
- ✅ Input validation (RequestValidator) - XSS prevention, length limits, format validation
- ✅ HTML sanitization (Jsoup Safelist) - prevents script injection
- ✅ GUID validation - ensures proper UUID format
- ✅ Base64 validation - size and format checks

## Architecture and Design Patterns

### Factory Pattern for PDF Generators

The application uses **Factory Pattern** to select the appropriate PDF generator based on document type. This architecture enables:
- Clear separation of concerns - each generator handles one document type
- Easy maintenance - modifying one document type doesn't affect others
- Simple extensibility - add new document types by creating a new generator and registering it in the factory
- Testable components - each generator can be unit tested independently

**Key Classes:**
- `PdfGeneratorFactory` (src/main/java/th/go/etda/sarabun/pdf/service/pdf/PdfGeneratorFactory.java) - Factory that maps BookType to generator
- `PdfGeneratorBase` (src/main/java/th/go/etda/sarabun/pdf/service/pdf/PdfGeneratorBase.java) - Abstract base class for all generators (2,092 lines)
- Individual generators: `MemoPdfGenerator`, `OutboundPdfGenerator`, `InboundPdfGenerator`, `StampPdfGenerator`, `RegulationPdfGenerator`, `AnnouncementPdfGenerator`, `OrderPdfGenerator`, `MinistryPdfGenerator`, `RulePdfGenerator`

### Document Type System

Documents are identified by `bookNameId` (UUID) and mapped to `BookType` enum:

| Document Type | BookType | bookNameId UUID |
|--------------|----------|-----------------|
| บันทึกข้อความ (Memo) | MEMO | BB4A2F11-722D-449A-BCC5-22208C7A4DEC |
| หนังสือส่งออก (Outbound) | OUTBOUND | 90F72F0E-528D-4992-907A-F2C6B37AD9A5 |
| หนังสือรับเข้า (Inbound) | INBOUND | 03241AA7-0E85-4C5C-A2CC-688212A79B84 |
| หนังสือประทับตรา (Stamp) | STAMP | AF3E7697-6F7E-4AD8-B76C-E2134DB98747 |
| หนังสือคำสั่ง (Order) | ORDER | 3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D |
| หนังสือประกาศ (Announcement) | ANNOUNCEMENT | 23065068-BB18-49EA-8CE7-22945E16CB6D |
| หนังสือระเบียบ (Regulation) | REGULATION | 50792880-F85A-4343-9672-7B61AF828A5B |
| หนังสือข้อบังคับ (Rule) | RULE | 4AB1EC00-9E5E-4113-B577-D8ED46BA7728 |
| หนังสือภายใต้กระทรวง (Ministry) | MINISTRY | 4B3EB169-6203-4A71-A3BD-A442FEAAA91F |

### Request Flow

1. Client sends POST to `/api/pdf/preview` or `/api/pdf/generate` with `GeneratePdfRequest` JSON
2. `GeneratePdfController` validates rate limiting (via `RateLimitConfig`) and input validation (via `RequestValidator`)
3. `GeneratePdfService` routes request to appropriate generator via `PdfGeneratorFactory`
4. Generator creates PDF bytes and returns as Base64
5. For preview endpoint: multiple PDFs are merged using `PDFMergerUtility`
6. Response returned as `ApiResponse<String>` (Base64 PDF) or `ApiResponse<List<PdfResult>>` (separate files)

### Font Management

`FontManager` (src/main/java/th/go/etda/sarabun/pdf/service/pdf/FontManager.java) is a critical component:
- Singleton pattern with Spring Bean (@Component)
- Loads Thai font bytes (THSarabunNew Regular and Bold) once at startup into memory cache via @PostConstruct
- Each PDDocument creates PDFont instances from cached bytes (PDFont must be recreated per document due to PDFBox internals)
- Fonts located in `src/main/resources/fonts/`
- This caching strategy significantly improves performance (~30% faster) by avoiding repeated file I/O
- Provides statistics via `getRegularFontLoadCount()` and `getBoldFontLoadCount()`

### Input Validation & Rate Limiting

**Rate Limiting** (via `RateLimitConfig` + Bucket4j):
- Per-IP rate limiting (configurable via `ratelimit.per-ip.requests-per-minute`)
- Global rate limiting (configurable via `ratelimit.global.requests-per-minute`)
- Can be enabled/disabled via `ratelimit.enabled` property
- Uses Bucket4j library for token bucket algorithm
- Extracts client IP from `X-Forwarded-For` header (for proxy/load balancer scenarios)

**Input Validation** (via `RequestValidator`):
- Content length limits (default: 500,000 chars, configurable via `validation.max-content-length`)
- Base64 size validation (default: 10MB, configurable via `validation.max-base64-size`)
- Maximum signers/recipients/attachments limits
- GUID format validation (ensures valid UUID format)
- XSS prevention - detects and blocks `<script>` tags and event handlers
- HTML sanitization using Jsoup Safelist (whitelist-based, allows only safe HTML tags)
- Validates all nested objects and arrays

### PDF Generation Components

**Base Generator (`PdfGeneratorBase`):**
- Provides common utilities: drawing text, creating pages, rendering HTML, adding signature boxes
- Contains Thai font loading methods
- Implements coordinate system helpers for PDFBox (Y-axis starts at bottom)
- Handles Thai number conversion (๐-๙ to 0-9 and vice versa)
- Manages page breaks and pagination
- **Note**: This is a large file (2,092 lines) that could benefit from refactoring into smaller utilities

**HTML Rendering:**
- `HtmlContentRenderer` - Renders HTML content to PDF using OpenHTMLtoPDF library
- `TableRenderer` - Specialized handler for HTML tables with auto-sizing
- `HtmlUtils` - HTML parsing and sanitization using Jsoup
- Supports Thai fonts (THSarabunNew)
- Word-break handling for Thai text (no spaces between words)

**Signature Handling:**
- Signature boxes are drawn directly in generators (not post-processed)
- Three signer types: `bookSigned` (signers), `bookSubmited` (submit-through), `bookLearner` (internal recipients)
- Submit and learner pages are appended as separate pages via `MemoPdfGenerator.addSubmitPages()` and `addLearnerPages()`
- Supports Base64 signature images

## Request Model Structure

`GeneratePdfRequest` (src/main/java/th/go/etda/sarabun/pdf/model/GeneratePdfRequest.java) uses a dual-document structure:

```java
{
  "bookNameId": "UUID",           // Document type identifier
  "memo": { ... },                // Memo document (บันทึกข้อความ) data
  "document": { ... },            // Main document (หนังสือส่งออก, etc.) data
  "bookSigned": [ ... ],          // Signers
  "bookSubmited": [ ... ],        // Submit-through persons
  "bookLearner": [ ... ],         // Internal recipients
  "toRecipients": [ ... ],        // External recipients (for Outbound/Stamp)
  "base64Pdf": "..."              // For Inbound only - existing PDF to annotate
}
```

**Document Types and Fields:**
- **MEMO**: Uses `memo` only
- **OUTBOUND**: Uses `document` for outbound letter + `memo` for attached memo copy
- **INBOUND**: Uses `base64Pdf` (existing PDF) + `memo.bookNo`
- **Other types** (Order, Announcement, Regulation, Rule, Ministry): Use `document` for main + `memo` for attached memo

**⚠️ BREAKING CHANGE**: Previous versions used `documentMain` and `documentSub`. These field names are **NO LONGER SUPPORTED**. You must use `memo` and `document` in all API requests. Old field names will be ignored or cause validation errors.

**BookContent Structure:**
- `subject`: Document subject line (drawn after "เรื่อง")
- `content`: Main content (HTML or plain text)
- `contentType`: "text" or "html" (determines rendering method)

**Important**: See `docs/analysis/API_REQUEST_FIELDS_GUIDE.md` for comprehensive field documentation.

## Configuration

**Rate Limiting** (`application.properties`):
- `ratelimit.enabled` - Enable/disable rate limiting (default: true)
- `ratelimit.global.requests-per-minute` - System-wide limit (default: 100)
- `ratelimit.per-ip.requests-per-minute` - Per-IP limit (default: 100)
- Uses Bucket4j library

**Input Validation** (`application.properties`):
- `validation.max-content-length` - Max content size (default: 500,000 chars)
- `validation.max-base64-size` - Max Base64 signature size (default: 10MB)
- `validation.max-signers` - Max number of signers (default: 10)
- `validation.max-recipients` - Max number of recipients (default: 50)
- `validation.max-attachments` - Max number of attachments (default: 20)

**Security** (`SecurityConfig.java`):
- Currently disabled for development (all endpoints permitAll)
- JWT dependencies included (JJWT 0.12.6) but not configured
- CSRF disabled
- CORS allows all origins

## Important Implementation Notes

### PDFBox 2.x Specifics
- Must use `PDFMergerUtility` with `MemoryUsageSetting.setupMixed()` to prevent OOM
- Each PDFont must be created per PDDocument (cannot be cached)
- Font bytes can be cached and reused across documents (handled by FontManager)
- Y-axis coordinate system starts at bottom of page (use helper methods in PdfGeneratorBase)
- For large PDFs, consider using `MemoryUsageSetting.setupTempFileOnly()`

### Thai Language Considerations

**Character Encoding:**
- ⚠️ **CRITICAL**: Use `c >= '0' && c <= '9'` instead of `Character.isDigit(c)` when checking for Arabic digits
- `Character.isDigit()` returns true for Thai digits (๐-๙), causing unwanted conversions
- Thai text requires proper font (THSarabunNew) to render correctly
- Thai text has no spaces between words - must use `word-break: break-word` in CSS/HTML

**Number Conversion:**
- Arabic (0-9) ↔ Thai (๐-๙) conversion utilities available in PdfGeneratorBase
- Used for document numbers (bookNo) and dates (dateThai)
- Methods: `convertToThaiNumbers()`, `convertToArabicNumbers()`

**Date Formatting:**
- Thai Buddhist calendar (พ.ศ.) = Gregorian year + 543
- Month names in Thai (มกราคม, กุมภาพันธ์, etc.)
- Format: "DD MMMM พ.ศ. YYYY" (e.g., "8 มกราคม พ.ศ. 2569")

### Two PDF Generation Modes

1. **Preview Mode** (`/api/pdf/preview`):
   - Merges all PDFs into single file
   - Appends submit pages and learner pages at the end
   - Returns single Base64 string
   - Used for preview in browser

2. **Generate Mode** (`/api/pdf/generate`):
   - Returns separate PDF files (not merged)
   - For Outbound/Stamp: creates one PDF per recipient
   - Returns `List<PdfResult>` with filename and description
   - Used for final document generation

### Adding New Document Types

1. Create new generator extending `PdfGeneratorBase` in `src/main/java/th/go/etda/sarabun/pdf/service/pdf/`
2. Add new `BookType` enum entry in `BookType.java` with UUID
3. Register generator in `PdfGeneratorFactory.init()` method
4. Implement `generate(GeneratePdfRequest)` method
5. Handle specific layout and fields for the new document type

### HTML Content Rendering

When `contentType: "html"`:
- Content is rendered via `HtmlContentRenderer` using OpenHTMLtoPDF
- Tables rendered with `TableRenderer` (auto-sizing columns)
- HTML is sanitized via `HtmlUtils` using Jsoup Safelist
- Supports Thai fonts (THSarabunNew)
- CSS properties supported: basic styling, tables, lists
- Word-wrap configured for Thai text (no spaces)

**Frontend Integration:**
- Use Quill Editor with width: 455pt (PDF content width)
- Load THSarabunNew font in browser
- Apply CSS: `word-break: break-word; word-wrap: break-word;`
- See `docs/analysis/QUILL_FRONTEND_CONFIG.md` for detailed setup

## Debugging Features

**Debug Borders** (in `PdfGeneratorBase.java`):

Set `ENABLE_DEBUG_BORDERS = true` to show visual guides:
- **Red border**: Content area margins (MARGIN_TOP, MARGIN_BOTTOM, MARGIN_LEFT, MARGIN_RIGHT = 70pt)
- **Orange dashed line**: MIN_Y_POSITION (120pt from bottom) - where page breaks occur
- **Blue line**: Page number position (PAGE_NUMBER_Y_OFFSET = 40pt above content area)

Useful for:
- Adjusting layout and positioning
- Troubleshooting content overflow
- Understanding page break behavior
- Debugging signature box placement

**Configuration Values** (in `PdfConstants.java`):
```java
MARGIN_TOP = 70f          // Top margin
MARGIN_BOTTOM = 70f       // Bottom margin
MARGIN_LEFT = 70f         // Left margin
MARGIN_RIGHT = 70f        // Right margin
MIN_Y_OFFSET = 50f        // Offset from bottom before page break
MIN_Y_POSITION = MARGIN_BOTTOM + MIN_Y_OFFSET  // Page break threshold (120pt)
PAGE_NUMBER_Y_OFFSET = 40f             // Page number position
```

See `docs/analysis/DEBUG_BORDERS_CONFIG.md` for detailed configuration guide.

## Common Bugs and Solutions

### Bug: "No glyph for U+1C78" Error
**Cause**: Using `Character.isDigit()` which includes Thai digits (๐-๙)
**Solution**: Use `c >= '0' && c <= '9'` for Arabic digits only
**Location**: PdfGeneratorBase.java, number conversion methods

### Bug: Wrong Document Type Displayed
**Cause**: Incorrect bookNameId UUID in request
**Solution**: Verify UUID matches BookType enum (see Document Type System section)
**Location**: api-tester.html, frontend requests

### Bug: Thai Date Double Conversion
**Cause**: Converting Thai numbers twice (once in request, once in generator)
**Solution**: Check if text is already in Thai format before converting
**Location**: Date formatting methods in generators

### Bug: Content Overflow (Text Cut Off)
**Cause**: Not checking MIN_Y_POSITION before drawing text
**Solution**: Check `yPosition < MIN_Y_POSITION` and create new page
**Enable**: Debug borders to visualize the issue

### Bug: Font Not Loading
**Cause**: Font file missing or FontManager not initialized
**Solution**: Check `/api/pdf/health` endpoint for font load statistics
**Location**: src/main/resources/fonts/

### Bug: Memory Leak with PDDocument
**Cause**: Not closing PDDocument after use
**Solution**: Use try-with-resources: `try (PDDocument doc = new PDDocument()) { ... }`

### Bug: Rate Limit IP Spoofing
**Cause**: X-Forwarded-For header can be spoofed by clients
**Solution**: Validate requests come from trusted proxies before using X-Forwarded-For
**Location**: RateLimitConfig.java

## Performance Tuning

**Font Loading Optimization:**
- ✅ Font bytes cached at startup (FontManager @PostConstruct)
- ✅ ~30% performance improvement vs loading from file each time
- Monitor cache hits via health endpoint

**Memory Management:**
- Use `MemoryUsageSetting.setupMixed()` for balanced memory/disk usage
- For very large PDFs (>100 pages), use `setupTempFileOnly()`
- Monitor container memory usage: `docker stats sarabun-pdf-api`

**PDF Generation:**
- Avoid creating unnecessary PDDocument instances
- Close resources promptly with try-with-resources
- Reuse Base64 decoded bytes instead of decoding multiple times

**Rate Limiting:**
- Adjust per-IP limits based on actual usage patterns
- Consider Redis-backed rate limiting for multi-instance deployments
- Monitor rate limit rejections in logs

**Potential Optimizations:**
- Cache frequently generated PDFs (if applicable)
- Async PDF generation for large documents (use queue system)
- Lazy-load images and resources
- Pre-size ByteArrayOutputStream based on expected PDF size

## Dependency Notes

**Core Dependencies:**
- Spring Boot 3.5.9 - REST framework
- Apache PDFBox 2.0.31 - PDF generation (Open Source, no license fees)
- FontBox 2.0.31 - Font management (required by PDFBox)
- Bucket4j 8.10.1 - Rate limiting
- Jsoup 1.18.3 - HTML parsing/sanitization
- Lombok - Reduce boilerplate

**Included but Unused** (consider removing to reduce JAR size ~10MB):
- `OpenHTMLtoPDF` artifacts (3 jars) - Not currently used in codebase (~3-5MB)
- `JJWT` libraries (3 jars) - Security disabled, JWT not configured (~1MB)
- `Commons IO` 2.18.0 - No imports found in code (~300KB)

**Note**: Before removing, verify with: `mvn dependency:tree` and search codebase for imports.

## File Structure

```
src/main/java/th/go/etda/sarabun/pdf/
├── SarabunPdfApplication.java          # Main Spring Boot application
├── controller/
│   └── GeneratePdfController.java      # REST API endpoints
├── service/
│   ├── GeneratePdfService.java         # Main service (orchestrates generation)
│   ├── RequestValidator.java           # Input validation
│   ├── UtilityService.java
│   └── pdf/                            # PDF generation components
│       ├── PdfGeneratorFactory.java    # Factory for selecting generator
│       ├── PdfGeneratorBase.java       # Abstract base for all generators (2,092 lines)
│       ├── MemoPdfGenerator.java       # Memo generator
│       ├── OutboundPdfGenerator.java   # Outbound generator
│       ├── InboundPdfGenerator.java    # Inbound generator
│       ├── StampPdfGenerator.java      # Stamp generator
│       ├── [...]PdfGenerator.java      # Other document type generators
│       ├── FontManager.java            # Font caching and loading
│       ├── HtmlContentRenderer.java    # HTML to PDF rendering
│       ├── TableRenderer.java          # Table rendering
├── model/
│   ├── GeneratePdfRequest.java         # Request DTO (with inner classes)
│   ├── ApiResponse.java                # Response wrapper
│   └── PdfResult.java                  # PDF result model
├── constant/
│   ├── BookType.java                   # Document type enum
│   ├── SignBoxType.java                # Signature box types
│   └── PdfConstants.java               # Constants
├── config/
│   ├── SecurityConfig.java             # Spring Security config
│   └── RateLimitConfig.java            # Rate limiting config
├── util/
│   └── HtmlUtils.java                  # HTML utilities
└── exception/
    └── PdfGenerationException.java     # Custom exception

src/main/resources/
├── fonts/                              # Thai fonts (THSarabunNew, TH NiramitIT)
├── images/                             # Logos and stamps (ETDA, MDES, Thai Gov)
├── application.properties              # Main configuration

docs/
└── analysis/                           # Project analysis and guides
    ├── PROJECT_ANALYSIS_FINAL_20260120.md  # Comprehensive analysis report
    ├── API_REQUEST_FIELDS_GUIDE.md         # API field documentation
    ├── DEBUG_BORDERS_CONFIG.md             # Debug configuration guide
    └── QUILL_FRONTEND_CONFIG.md            # Frontend integration guide
```

## Common Patterns

### Creating a new PDF page with Thai font:
```java
PDPage page = new PDPage(PDRectangle.A4);
document.addPage(page);
PDPageContentStream content = new PDPageContentStream(document, page);
PDFont font = loadThaiFontRegular(document);
content.setFont(font, 16);
// Draw text (remember Y-axis starts at bottom)
float yPosition = page.getMediaBox().getHeight() - MARGIN_TOP;
```

### Using FontManager:
```java
// In PdfGeneratorBase subclass
PDFont regularFont = loadThaiFontRegular(document);
PDFont boldFont = loadThaiFontBold(document);

// FontManager handles caching internally
// Font bytes loaded once at startup via @PostConstruct
```

### Proper Resource Cleanup:
```java
// Always use try-with-resources for PDDocument
try (PDDocument document = new PDDocument()) {
    // Generate PDF
    // ...
} // Automatically closed, prevents memory leaks
```

### Checking for Page Breaks:
```java
// Before drawing text, check if new page needed
if (yPosition < MIN_Y_POSITION) {
    PDPage newPage = new PDPage(PDRectangle.A4);
    document.addPage(newPage);
    contentStream.close();
    contentStream = new PDPageContentStream(document, newPage);
    yPosition = PAGE_HEIGHT - MARGIN_TOP;
}
```

### Drawing signature box:
```java
// Signature boxes are drawn during PDF generation in generators
// See PdfGeneratorBase.drawSignatureBox() method
```

### Merging PDFs:
```java
// Use PDFMergerUtility with memory management
PDFMergerUtility merger = new PDFMergerUtility();
merger.addSource(new ByteArrayInputStream(pdfBytes));
merger.setDestinationStream(outputStream);
merger.mergeDocuments(MemoryUsageSetting.setupMixed(10L * 1024 * 1024));
```

### Thai Number Conversion:
```java
// Convert bookNo to Thai numbers for display
String thaiBookNo = convertToThaiNumbers(bookNo);

// Convert back to Arabic when needed for processing
String arabicBookNo = convertToArabicNumbers(thaiBookNo);
```

## Testing and Debugging

**Quick Testing:**
- Use `/api/pdf/view` endpoint to view generated PDF directly in browser (bypasses Base64 copying)
- Swagger UI at `http://localhost:8888/swagger-ui.html`
- API Tester at `http://localhost:8888/api-tester.html`
- Health check at `http://localhost:8888/api/pdf/health`

**Health Check Response:**
```json
{
  "status": "UP",
  "fontManager": {
    "regularFontLoaded": true,
    "boldFontLoaded": true,
    "regularFontLoadCount": 150,
    "boldFontLoadCount": 75
  }
}
```

**Test Data:**
- See `GeneratePdfController.createTestRequest()` for sample request structure
- Use api-tester.html for interactive testing with predefined templates

**Debugging PDF Issues:**
- Enable debug borders: Set `ENABLE_DEBUG_BORDERS = true` in PdfGeneratorBase.java
- Check logs for FontManager initialization
- Verify font loading success in health endpoint
- Use Docker for consistent Linux environment testing
- Compare output with expected layout in `docs/analysis/`

**Docker Logs:**
```bash
# View recent logs
docker logs sarabun-pdf-api --tail 100

# Follow logs in real-time
docker logs -f sarabun-pdf-api

# Check container stats
docker stats sarabun-pdf-api
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/pdf/preview` | Generate merged PDF (single Base64 string) |
| POST | `/api/pdf/generate` | Generate separate PDF files (List<PdfResult>) |
| GET | `/api/pdf/view` | Quick test viewer (renders sample PDF in browser) |
| GET | `/api/pdf/health` | Health check with font cache stats |
| GET | `/api/pdf/book-types` | List supported document types |
| GET | `/swagger-ui.html` | Swagger API documentation |
| GET | `/api-tester.html` | Interactive API testing interface |
| GET | `/actuator/health` | Spring Actuator health check |

## Additional Resources

**Documentation:**
- Comprehensive analysis: `docs/analysis/PROJECT_ANALYSIS_FINAL_20260120.md`
- API field guide: `docs/analysis/API_REQUEST_FIELDS_GUIDE.md`
- Debug configuration: `docs/analysis/DEBUG_BORDERS_CONFIG.md`
- Frontend integration: `docs/analysis/QUILL_FRONTEND_CONFIG.md`
- Docker deployment: `DOCKER.md`

**External Documentation:**
- Apache PDFBox: https://pdfbox.apache.org/
- Spring Boot: https://spring.io/projects/spring-boot
- Bucket4j: https://github.com/bucket4j/bucket4j
- Jsoup: https://jsoup.org/

## Pre-Production Checklist

Before deploying to production:

- [ ] Enable JWT authentication in `SecurityConfig.java`
- [ ] Configure proper CORS origins (remove `origins = "*"`)
- [ ] Review and adjust rate limiting thresholds
- [ ] Set `logging.level=INFO` (not DEBUG)
- [ ] Disable debug borders: `ENABLE_DEBUG_BORDERS = false`
- [ ] Scan dependencies: `mvn org.owasp:dependency-check-maven:check`
- [ ] Test all 9 document types thoroughly
- [ ] Load test with expected production volume
- [ ] Configure proper memory limits in Docker
- [ ] Set up monitoring and alerting
- [ ] Review and remove unused dependencies
- [ ] Backup font files and images
- [ ] Document any custom configuration changes
