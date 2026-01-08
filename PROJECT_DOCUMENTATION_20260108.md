# 📄 Sarabun PDF Generator - เอกสารสรุปโครงการ

**สร้างเมื่อ:** 2026-01-08 16:30:00  
**เวอร์ชัน:** 1.0.0  
**ผู้พัฒนา:** Migrated from .NET to Java

---

## 📋 สารบัญ

1. [ภาพรวมโครงการ](#1-ภาพรวมโครงการ)
2. [โครงสร้างโปรเจค](#2-โครงสร้างโปรเจค)
3. [Libraries ที่ใช้](#3-libraries-ที่ใช้)
4. [API Endpoints](#4-api-endpoints)
5. [ประเภทเอกสารที่รองรับ](#5-ประเภทเอกสารที่รองรับ)
6. [โครงสร้างข้อมูล Request](#6-โครงสร้างข้อมูล-request)
7. [การทำงานของระบบ](#7-การทำงานของระบบ)
8. [ข้อระวังสำคัญ](#8-ข้อระวังสำคัญ)
9. [วิธีเพิ่ม Template ใหม่](#9-วิธีเพิ่ม-template-ใหม่)
10. [การ Deploy และ Configuration](#10-การ-deploy-และ-configuration)

---

## 1. ภาพรวมโครงการ

### 1.1 คำอธิบาย
**Sarabun PDF API** เป็นระบบสร้าง PDF สำหรับเอกสารราชการไทย ที่ migrate มาจาก .NET ไปเป็น Java โดยใช้ **Apache PDFBox 2.x** (Open Source, Apache License 2.0) แทน iText

### 1.2 เทคโนโลยีหลัก
| เทคโนโลยี | เวอร์ชัน | หน้าที่ |
|-----------|----------|---------|
| Java | 17 | ภาษาหลัก |
| Spring Boot | 3.5.9 | Web Framework |
| Apache PDFBox | 2.0.31 | สร้าง PDF (ฟรี 100%) |
| Lombok | - | ลด boilerplate code |
| Maven | - | Build tool |

### 1.3 ฟีเจอร์หลัก
- ✅ สร้าง PDF หนังสือราชการไทย
- ✅ รองรับฟอนต์ภาษาไทย (TH Sarabun New)
- ✅ AcroForm Signature Fields (ช่องลงนามดิจิทัล)
- ✅ Merge หลาย PDF เป็นไฟล์เดียว
- ✅ รองรับหลายประเภทเอกสาร
- ✅ ส่งกลับเป็น Base64 string

---

## 2. โครงสร้างโปรเจค

```
sarabun-pdf-generator/
├── pom.xml                          # Maven configuration
├── mvnw, mvnw.cmd                   # Maven wrapper
│
├── src/main/
│   ├── java/th/go/etda/sarabun/pdf/
│   │   ├── SarabunPdfApplication.java     # Main Application
│   │   │
│   │   ├── config/
│   │   │   └── SecurityConfig.java        # Spring Security config
│   │   │
│   │   ├── controller/
│   │   │   └── GeneratePdfController.java # REST API endpoints
│   │   │
│   │   ├── model/
│   │   │   ├── GeneratePdfRequest.java    # Request model (สำคัญ!)
│   │   │   ├── ApiResponse.java           # Response wrapper
│   │   │   └── PdfResult.java             # PDF result model
│   │   │
│   │   ├── service/
│   │   │   ├── GeneratePdfService.java    # 📌 Orchestration layer
│   │   │   ├── PdfService.java            # 📌 Core PDF generation
│   │   │   └── UtilityService.java        # Utility functions
│   │   │
│   │   └── util/
│   │       └── HtmlUtils.java             # HTML to Plain text
│   │
│   └── resources/
│       ├── application.properties         # App configuration
│       ├── fonts/                         # Thai fonts
│       │   ├── THSarabunNew.ttf          # Regular
│       │   └── THSarabunNew Bold.ttf     # Bold
│       ├── images/                        # Logo images
│       │   └── etda-logo.png
│       └── static/
│           └── api-tester.html            # Test page
│
└── target/                                # Build output
```

### 2.1 ไฟล์สำคัญ

| ไฟล์ | หน้าที่ | บรรทัด |
|------|--------|--------|
| `GeneratePdfController.java` | รับ HTTP Request, ส่งต่อไป Service | ~260 |
| `GeneratePdfService.java` | **Orchestration** - เลือกประเภทเอกสาร, merge PDF | ~840 |
| `PdfService.java` | **Core** - วาด PDF ด้วย PDFBox | ~1900 |
| `GeneratePdfRequest.java` | Request Model (ข้อมูลทั้งหมด) | ~280 |

---

## 3. Libraries ที่ใช้

### 3.1 Apache PDFBox 2.0.31 (Core)
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.31</version>
</dependency>
```

**ใช้ทำอะไร:**
- `PDDocument` - สร้างเอกสาร PDF
- `PDPage` - สร้างหน้า
- `PDPageContentStream` - วาดข้อความ, รูปภาพ
- `PDType0Font` - โหลดฟอนต์ไทย (TrueType)
- `PDImageXObject` - แทรกรูปภาพ (logo)
- `PDAcroForm` + `PDSignatureField` - ช่องลงนามดิจิทัล
- `PDFMergerUtility` - รวมหลาย PDF

### 3.2 FontBox 2.0.31
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>fontbox</artifactId>
    <version>2.0.31</version>
</dependency>
```
**ใช้ทำอะไร:** จัดการฟอนต์ (dependency ของ PDFBox)

### 3.3 OpenHTMLtoPDF 1.0.10 (สำรอง)
```xml
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
</dependency>
```
**ใช้ทำอะไร:** แปลง HTML เป็น PDF (สำหรับ use case อื่น)

### 3.4 JJWT 0.12.6
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```
**ใช้ทำอะไร:** JWT Authentication (ยังไม่ได้ implement)

### 3.5 Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```
**ใช้ทำอะไร:** 
- `@Data` - getter/setter/toString
- `@Builder` - Builder pattern
- `@Slf4j` - Logging
- `@RequiredArgsConstructor` - Constructor injection

### 3.6 Springdoc OpenAPI (Swagger)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```
**ใช้ทำอะไร:** Swagger UI documentation

---

## 4. API Endpoints

### 4.1 POST `/api/pdf/preview`
**สร้าง PDF และส่งกลับเป็น Base64**

```bash
curl -X POST http://localhost:8888/api/pdf/preview \
  -H "Content-Type: application/json" \
  -d '{
    "bookNameId": "4B3EB169-6203-4A71-A3BD-A442FEAAA91F",
    "bookTitle": "ขอเชิญประชุม",
    "bookNo": "สพธอ. 0102/2568",
    "dateThai": "8 มกราคม พ.ศ. 2569",
    "department": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์",
    "bookContent": [{ "bookContent": "เนื้อหา..." }],
    "bookSigned": [{ "firstname": "ประสิทธิ์", "lastname": "พัฒนา", ... }]
  }'
```

**Response:**
```json
{
  "isOk": true,
  "message": "สร้าง PDF สำเร็จ",
  "data": "JVBERi0xLjQK..."  // Base64 encoded PDF
}
```

### 4.2 GET `/api/pdf/health`
**Health check**

```bash
curl http://localhost:8888/api/pdf/health
```

### 4.3 GET `/api/pdf/download` (optional)
**ดาวน์โหลด PDF โดยตรง**

### 4.4 หน้าทดสอบ
```
http://localhost:8888/api-tester.html
```

---

## 5. ประเภทเอกสารที่รองรับ

### 5.1 BookNameId Constants

| BookNameId (GUID) | ประเภทเอกสาร | Method |
|-------------------|--------------|--------|
| `90F72F0E-528D-4992-907A-F2C6B37AD9A5` | **หนังสือส่งออก** | `generateOutgoingLetterPdf()` |
| `4B3EB169-6203-4A71-A3BD-A442FEAAA91F` | **บันทึกข้อความ** | `generateOfficialMemoPdf()` |
| `50792880-F85A-4343-9672-7B61AF828A5B` | ระเบียบ | `generateOfficialMemoPdf()` |
| `23065068-BB18-49EA-8CE7-22945E16CB6D` | ประกาศ | `generateOfficialMemoPdf()` |
| `3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D` | คำสั่ง | `generateOfficialMemoPdf()` |
| `4AB1EC00-9E5E-4113-B577-D8ED46BA7728` | ข้อบังคับ | `generateOfficialMemoPdf()` |
| ... | ... | ... |

### 5.2 Layout ที่แตกต่างกัน

#### หนังสือส่งออก (`generateOutgoingLetterPdf`)
```
┌─────────────────────────────────────────┐
│ สพธอ. 0102/2568           [LOGO]        │  ← Logo ขวา
│                     ที่อยู่สำนักงาน      │  ← ที่อยู่ขวา
│                     ศูนย์ราชการ...       │
│                     8 มกราคม พ.ศ. 2569  │  ← วันที่ขวา
│                                         │
│ เรื่อง ขอเชิญประชุม                     │
│ ขอประทานกราบทูล ทูลกระหม่อมหญิง...      │  ← salutation
│ เรียน การท่องเที่ยวแห่งประเทศไทย        │
│ อ้างถึง หนังสือ...                      │
│ สิ่งที่ส่งมาด้วย 1. กำหนดการ 2. ...     │
│                                         │
│         เนื้อหาหนังสือ...               │
│                                         │
│           ควรมิควรแล้วแต่จะโปรดเกล้าฯ   │  ← endDoc (ขวา)
│               ┌─────────┐               │
│               │  เรียน  │               │  ← ช่องลงนาม
│               └─────────┘               │
│                (ชื่อผู้ลงนาม)            │
│                                         │
│ สำนักงานพัฒนาฯ                          │  ← ข้อมูลติดต่อ (ซ้ายล่าง)
│ เลขหมาย ๐ ๒๑๒๓ ๑๒๓๔                    │
│ โทรสาร ๐ ๒๑๒๓ ๑๒๐๐                     │
│ อีเมล helpdesk@etda.or.th              │
└─────────────────────────────────────────┘
```

#### บันทึกข้อความ (`generateOfficialMemoPdf`)
```
┌─────────────────────────────────────────┐
│ [LOGO]            บันทึกข้อความ         │  ← Logo ซ้าย, หัวกลาง
│ ส่วนราชการ กองพัฒนาระบบบริการ           │
│ ที่ สพธอ. 0102/2568   วันที่ 8 มกราคมฯ │
│ เรื่อง ขอเชิญประชุม                     │
│ เรียน การท่องเที่ยวแห่งประเทศไทย        │
│                                         │
│         เนื้อหาหนังสือ...               │
│                                         │
│               ┌─────────┐               │
│               │ รับทราบ │               │  ← ช่องลงนาม
│               └─────────┘               │
│                (ชื่อผู้ลงนาม)            │
└─────────────────────────────────────────┘
```

---

## 6. โครงสร้างข้อมูล Request

### 6.1 GeneratePdfRequest (หลัก)

```java
public class GeneratePdfRequest {
    // === ฟิลด์สำคัญ ===
    String bookNameId;      // 📌 GUID กำหนดประเภทเอกสาร
    String bookTitle;       // เรื่อง
    String bookNo;          // ที่หนังสือ
    String dateThai;        // วันที่ภาษาไทย
    String department;      // ชื่อหน่วยงาน
    String divisionName;    // ชื่อส่วนงาน
    String recipients;      // เรียน...
    String formatPdf;       // "A4"
    
    // === สำหรับหนังสือส่งออก ===
    String address;         // ที่อยู่สำนักงาน (มุมขวาบน)
    String contact;         // ข้อมูลติดต่อ (มุมซ้ายล่าง)
    String salutation;      // คำขึ้นต้น (เช่น "ขอประทานกราบทูล")
    String salutationEnding; // ลงท้ายขึ้นต้น (เช่น "ทูลกระหม่อมหญิงฯ")
    String endDoc;          // ข้อความท้าย (เช่น "ควรมิควรแล้วแต่จะโปรดเกล้าฯ")
    
    // === รายการย่อย ===
    List<BookContent> bookContent;     // เนื้อหา
    List<DocumentAttachment> attachment; // สิ่งที่ส่งมาด้วย
    List<BookReferTo> bookReferTo;     // อ้างถึง
    List<BookRelate> bookSigned;       // 📌 ผู้ลงนาม
    List<BookRelate> bookSubmited;     // เสนอผ่าน (หน้าใหม่)
    List<BookRelate> bookLearner;      // ผู้รับทราบ (หน้าใหม่)
}
```

### 6.2 BookContent (เนื้อหา)
```java
public static class BookContent {
    String bookContentTitle;   // หัวข้อ (อาจเป็น HTML)
    String bookContent;        // เนื้อหา (อาจเป็น HTML)
    Boolean nextPageStatus;    // ขึ้นหน้าใหม่
}
```

### 6.3 BookRelate (ผู้ลงนาม/เสนอผ่าน/รับทราบ)
```java
public static class BookRelate {
    String prefixName;        // คำนำหน้า (นาย, นาง, ดร.)
    String firstname;         // ชื่อ
    String lastname;          // นามสกุล
    String positionName;      // ตำแหน่ง
    String departmentName;    // หน่วยงาน
    String email;             // 📌 ใช้สร้าง Signature Field Name
    String signatureBase64;   // ลายเซ็น (ถ้ามี)
}
```

---

## 7. การทำงานของระบบ

### 7.1 Flow การสร้าง PDF

```
┌─────────────────────────────────────────────────────────────────┐
│                     HTTP Request (JSON)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              GeneratePdfController.previewPdf()                 │
│              - รับ request                                      │
│              - log ข้อมูล                                       │
│              - ส่งต่อไป Service                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              GeneratePdfService.previewPdf()                    │
│              📌 Orchestration Layer                             │
│              1. ตรวจสอบ bookNameId                              │
│              2. เลือก method สร้าง PDF ที่เหมาะสม               │
│              3. รวม PDF (merge) ถ้ามีหลายไฟล์                  │
│              4. เพิ่มหน้า เสนอผ่าน/รับทราบ                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                     ┌────────┴────────┐
                     │                 │
                     ▼                 ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│  generateOutgoingLetter  │  │  generateMainPdf         │
│  Pdf()                   │  │  ()                      │
│  - หนังสือส่งออก          │  │  - บันทึกข้อความ          │
└──────────────────────────┘  └──────────────────────────┘
                     │                 │
                     └────────┬────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  PdfService                                     │
│                  📌 Core PDF Generation                         │
│                  - สร้าง PDDocument                             │
│                  - โหลดฟอนต์ไทย                                 │
│                  - วาดข้อความ, รูปภาพ                           │
│                  - สร้าง AcroForm Fields                        │
│                  - แปลงเป็น Base64                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Response (Base64 PDF)                       │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Method หลักใน PdfService

| Method | หน้าที่ |
|--------|---------|
| `generateOfficialMemoPdf()` | สร้าง PDF บันทึกข้อความ |
| `generateOutgoingLetterPdf()` | สร้าง PDF หนังสือส่งออก |
| `addSubmitPages()` | เพิ่มหน้า "เสนอผ่าน" |
| `addLearnerPages()` | เพิ่มหน้า "ผู้รับทราบ" |
| `drawSignatureBlock()` | วาดกรอบลายเซ็น + AcroForm |
| `drawText()` | วาดข้อความ (รองรับหลายบรรทัด) |
| `createNewPage()` | สร้างหน้าใหม่พร้อมหมายเลขหน้า |

---

## 8. ข้อระวังสำคัญ

### 8.1 ⚠️ PDFBox Version
```
❌ ห้ามใช้ PDFBox 3.x
✅ ต้องใช้ PDFBox 2.x เท่านั้น (ตาม migrateToJava.txt)
```
**เหตุผล:** API แตกต่างกันมาก, 3.x ยังไม่เสถียร

### 8.2 ⚠️ Font Loading
```java
// ❌ ผิด - ใช้ file path โดยตรง
PDType0Font font = PDType0Font.load(document, new File("fonts/THSarabunNew.ttf"));

// ✅ ถูก - ใช้ ClassPathResource
ClassPathResource resource = new ClassPathResource("fonts/THSarabunNew.ttf");
PDType0Font font = PDType0Font.load(document, resource.getInputStream());
```
**เหตุผล:** ต้องทำงานได้ทั้งใน IDE และ JAR file

### 8.3 ⚠️ ContentStream ต้อง Close
```java
// ✅ ใช้ try-with-resources
try (PDPageContentStream contentStream = new PDPageContentStream(...)) {
    // วาดข้อความ
}

// หรือ close ก่อนสร้างหน้าใหม่
contentStream.close();
PDPage newPage = new PDPage(PDRectangle.A4);
contentStream = new PDPageContentStream(document, newPage, ...);
```

### 8.4 ⚠️ HTML Content
```java
// เนื้อหาอาจมาเป็น HTML จาก Rich Text Editor
String content = item.getBookContent();

// ต้องแปลงเป็น plain text ก่อนวาด
if (HtmlUtils.isHtml(content)) {
    content = HtmlUtils.htmlToPlainText(content);
}
```

### 8.5 ⚠️ Signature Field Name
```java
// Field name ต้อง unique และไม่มีอักขระพิเศษ
String fieldName = fieldPrefix + "_" + index + "_" + 
    email.replaceAll("[^a-zA-Z0-9]", "_");
// ผลลัพธ์: "OutgoingSigner_0_prasit_etda_or_th"
```

### 8.6 ⚠️ Memory Management
```java
// ใช้ MemoryUsageSetting สำหรับ merge หลาย PDF
PDFMergerUtility merger = new PDFMergerUtility();
merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
```

### 8.7 ⚠️ Multi-page Handling
```java
// ตรวจสอบพื้นที่ก่อนวาดเสมอ
if (yPosition < MIN_Y_POSITION) {
    contentStream.close();
    PDPage newPage = createNewPage(document, font, bookNo);
    contentStream = new PDPageContentStream(document, newPage, ...);
    yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
}
```

---

## 9. วิธีเพิ่ม Template ใหม่

### 9.1 ขั้นตอนที่ 1: กำหนด BookNameId

เพิ่มค่าคงที่ใน `GeneratePdfService.java`:
```java
// เพิ่มที่บรรทัด ~143
private static final String BOOK_NAME_ID_NEW_TYPE = "YOUR-GUID-HERE";
```

### 9.2 ขั้นตอนที่ 2: เพิ่ม Method ตรวจสอบ

```java
// เพิ่มใน GeneratePdfService.java
private boolean isNewDocumentType(String bookNameId) {
    return BOOK_NAME_ID_NEW_TYPE.equalsIgnoreCase(bookNameId);
}
```

### 9.3 ขั้นตอนที่ 3: แก้ไข generatePdfArray()

```java
// แก้ไข method generatePdfArray() ~บรรทัด 158
private List<PdfResult> generatePdfArray(GeneratePdfRequest request) throws Exception {
    List<PdfResult> results = new ArrayList<>();
    String bookNameId = request.getBookNameId();
    
    if (isOutgoingLetter(bookNameId)) {
        // ... existing code ...
    } else if (isNewDocumentType(bookNameId)) {  // 📌 เพิ่มตรงนี้
        String newTypePdf = generateNewTypePdf(request);
        results.add(PdfResult.builder()
            .pdfBase64(newTypePdf)
            .type("Main")
            .description("เอกสารประเภทใหม่")
            .build());
    } else {
        // บันทึกข้อความ (default)
        // ... existing code ...
    }
    
    return results;
}
```

### 9.4 ขั้นตอนที่ 4: สร้าง Method ใน GeneratePdfService

```java
/**
 * สร้าง PDF ประเภทใหม่
 */
private String generateNewTypePdf(GeneratePdfRequest request) throws Exception {
    log.debug("Generating new type PDF");
    
    // รวบรวมข้อมูลจาก request
    String title = request.getBookTitle();
    String content = // ... รวบรวมเนื้อหา ...
    
    // เรียก PdfService
    return pdfService.generateNewTypePdf(
        title,
        content,
        // ... parameters อื่นๆ ...
    );
}
```

### 9.5 ขั้นตอนที่ 5: สร้าง Method ใน PdfService

```java
/**
 * สร้าง PDF ประเภทใหม่
 * 
 * @param title หัวข้อ
 * @param content เนื้อหา
 * @return Base64 encoded PDF
 */
public String generateNewTypePdf(String title, String content, ...) throws Exception {
    log.info("=== Generating new type PDF ===");
    
    try (PDDocument document = new PDDocument()) {
        // 1. สร้างหน้า
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        // 2. โหลดฟอนต์
        PDFont fontRegular = loadThaiFont(document, false);
        PDFont fontBold = loadThaiFont(document, true);
        
        // 3. เริ่มวาด
        try (PDPageContentStream contentStream = 
                new PDPageContentStream(document, page)) {
            
            float yPosition = PAGE_HEIGHT - MARGIN_TOP;
            
            // 📌 วาด layout ตามที่ต้องการ
            // SECTION 1: หัวข้อ
            yPosition = drawText(contentStream, title, fontBold, 
                                FONT_SIZE_HEADER, MARGIN_LEFT, yPosition);
            
            // SECTION 2: เนื้อหา
            yPosition = drawMultilineText(contentStream, content, fontRegular,
                                         FONT_SIZE_CONTENT, MARGIN_LEFT, yPosition);
            
            // SECTION 3: ลายเซ็น (ถ้าต้องการ)
            // ...
        }
        
        // 4. แปลงเป็น Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
```

### 9.6 ขั้นตอนที่ 6: อัปเดต api-tester.html

```javascript
// เพิ่มใน BOOK_NAME_IDS
const BOOK_NAME_IDS = {
    memo: "4B3EB169-6203-4A71-A3BD-A442FEAAA91F",
    officialDocument: "90F72F0E-528D-4992-907A-F2C6B37AD9A5",
    newType: "YOUR-GUID-HERE"  // 📌 เพิ่มตรงนี้
};

// เพิ่มใน sampleData
newType: {
    bookNameId: BOOK_NAME_IDS.newType,
    bookTitle: "ทดสอบเอกสารประเภทใหม่",
    // ... fields อื่นๆ ...
}
```

### 9.7 ตัวอย่าง Layout Constants ที่ปรับได้

```java
// ใน PdfService.java (บรรทัด ~57-99)

// ขนาดหน้ากระดาษ
private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();   // 595 pt
private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 842 pt

// Margins
private static final float MARGIN_TOP = 70f;
private static final float MARGIN_BOTTOM = 70f;
private static final float MARGIN_LEFT = 70f;
private static final float MARGIN_RIGHT = 70f;

// Font Sizes
private static final float FONT_SIZE_HEADER = 24f;
private static final float FONT_SIZE_FIELD = 18f;
private static final float FONT_SIZE_CONTENT = 16f;

// Spacing
private static final float SPACING_BETWEEN_FIELDS = 5f;
private static final float MIN_Y_POSITION = MARGIN_BOTTOM + 100; // เมื่อใดต้องขึ้นหน้าใหม่
```

---

## 10. การ Deploy และ Configuration

### 10.1 Build JAR
```bash
./mvnw clean package -DskipTests
# Output: target/sarabun-pdf-api-1.0.0.jar
```

### 10.2 Run
```bash
java -jar target/sarabun-pdf-api-1.0.0.jar
```

### 10.3 Configuration (application.properties)
```properties
# Server
server.port=8888

# Logging
logging.level.th.go.etda.sarabun.pdf=DEBUG

# PDF temp directory
pdf.temp-directory=${java.io.tmpdir}/sarabun_pdf_files
```

### 10.4 Environment Variables
```bash
# Override port
SERVER_PORT=9999

# Override logging
LOGGING_LEVEL_TH_GO_ETDA_SARABUN_PDF=INFO
```

### 10.5 Docker (ตัวอย่าง)
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/sarabun-pdf-api-1.0.0.jar app.jar
COPY src/main/resources/fonts/ /app/fonts/
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📝 Changelog

| วันที่ | การเปลี่ยนแปลง |
|--------|---------------|
| 2026-01-08 | เพิ่มฟีเจอร์หนังสือส่งออก (bookNameId: 90F72F0E...) |
| 2026-01-08 | เพิ่มข้อมูลติดต่อมุมซ้ายล่าง (contact) |
| 2026-01-08 | เพิ่ม salutation, salutationEnding, endDoc |
| 2026-01-08 | ปรับ layout: logo ขวา, ที่อยู่ขวา, endDoc ขวา |
| 2026-01-08 | แก้ไข merge logic: Main + Memo = 1 PDF |
| 2026-01-08 | เปลี่ยน "รับทราบ" เป็น "เรียน" สำหรับ OutgoingSigner |

---

## 📞 ติดต่อ

- **Project:** Sarabun PDF Generator
- **Organization:** ETDA (สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์)
- **Repository:** sarabun-pdf-generator

---

*เอกสารนี้สร้างโดยอัตโนมัติเมื่อ 2026-01-08*
