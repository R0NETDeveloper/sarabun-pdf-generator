# 📘 Guide: Sarabun PDF Generator - โครงสร้างและการพัฒนาต่อ

## 📋 สารบัญ
1. [ภาพรวมโครงสร้างระบบ](#1-ภาพรวมโครงสร้างระบบ)
2. [Flow การทำงาน](#2-flow-การทำงาน)
3. [ไฟล์สำคัญ](#3-ไฟล์สำคัญ)
4. [การเพิ่ม HTML Template ใหม่](#4-การเพิ่ม-html-template-ใหม่)
5. [ประเภทเอกสารที่รองรับ](#5-ประเภทเอกสารที่รองรับ)
6. [การปรับแต่ง Layout](#6-การปรับแต่ง-layout)
7. [ขั้นตอนการพัฒนาต่อ](#7-ขั้นตอนการพัฒนาต่อ)

---

## 1. ภาพรวมโครงสร้างระบบ

### 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser)                              │
│                       api-tester.html                                │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ HTTP POST (JSON)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GeneratePdfController.java                        │
│                    /api/pdf/preview                                  │
│                    /api/pdf/health                                   │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GeneratePdfService.java                           │
│                    - previewPdf()                                    │
│                    - generatePdfArray()                              │
│                    - mergePdfArray()                                 │
│                    - addSignaturesToPdfs()                           │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       PdfService.java                                │
│                    - generateOfficialMemoPdf()        ◄── ปัจจุบัน   │
│                    - generateOfficialDocumentPdf()    ◄── TODO       │
│                    - generateRegulationPdf()          ◄── TODO       │
│                    - generateAnnouncePdf()            ◄── TODO       │
│                    - generateCommandPdf()             ◄── TODO       │
│                    - generateRulesPdf()               ◄── TODO       │
│                    - generateSealPdf()                ◄── TODO       │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Apache PDFBox 2.0.31                               │
│                    - PDDocument                                      │
│                    - PDPageContentStream                             │
│                    - PDType0Font (Thai)                              │
│                    - PDFMergerUtility                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 📁 โครงสร้างโปรเจค

```
sarabun-pdf-generator/
├── src/main/java/th/go/etda/sarabun/pdf/
│   ├── SarabunPdfApplication.java          # Main Application
│   ├── config/
│   │   └── SecurityConfig.java              # Security Configuration
│   ├── controller/
│   │   └── GeneratePdfController.java       # REST API Endpoints
│   ├── model/
│   │   ├── ApiResponse.java                 # Response Wrapper
│   │   ├── GeneratePdfRequest.java          # Request Model (สำคัญ!)
│   │   └── PdfResult.java                   # PDF Result Model
│   ├── service/
│   │   ├── GeneratePdfService.java          # PDF Generation Orchestration
│   │   ├── PdfService.java                  # Core PDF Creation (PDFBox)
│   │   └── UtilityService.java              # Utilities
│   └── util/
│       └── HtmlUtils.java                   # HTML to Plain Text Converter
│
├── src/main/resources/
│   ├── application.properties               # Server Config (port 8888)
│   ├── fonts/                               # Thai Fonts (TH Sarabun)
│   │   ├── THSarabunNew.ttf
│   │   └── THSarabunNew Bold.ttf
│   ├── images/
│   │   └── logoETDA.png                     # Logo สำหรับหัวกระดาษ
│   ├── static/
│   │   ├── api-tester.html                  # หน้าทดสอบ API
│   │   └── test.html
│   └── templates/                           # 📌 HTML Templates (ยังว่าง)
│       ├── official-documents/              # สำหรับ Template หนังสือ
│       └── styles/                          # CSS Styles
│
└── pom.xml                                  # Maven Dependencies
```

---

## 2. Flow การทำงาน

### 🔄 ขั้นตอนการสร้าง PDF

```
1. Client ส่ง POST Request
   └── JSON Body: GeneratePdfRequest
       ├── bookNameId (GUID ระบุประเภทเอกสาร)
       ├── bookTitle, bookNo, dateThai, department
       ├── bookContent[] (เนื้อหา - อาจเป็น HTML)
       ├── bookSigned[] (ผู้ลงนาม)
       ├── bookLearner[] (ผู้รับทราบ)
       └── speedLayer (ด่วน/ด่วนมาก/ด่วนที่สุด)

2. GeneratePdfController
   └── รับ Request และ Log ข้อมูล
   └── เรียก GeneratePdfService.previewPdf()

3. GeneratePdfService.previewPdf()
   ├── ตรวจสอบ bookNameId (ประเภทเอกสาร)
   ├── generatePdfArray() → สร้าง PDF หลัก + PDF รอง
   ├── addSignaturesToPdfs() → เพิ่มลายเซ็น
   └── mergePdfArray() → รวม PDF ทั้งหมด

4. PdfService.generateOfficialMemoPdf()
   ├── สร้าง PDDocument
   ├── โหลด Thai Font (TH Sarabun)
   ├── วาด Layout:
   │   ├── Logo ETDA
   │   ├── หัวข้อ "บันทึกข้อความ"
   │   ├── ส่วนราชการ, ที่, วันที่
   │   ├── เรื่อง, เนื้อหา
   │   └── ลายเซ็น
   └── Return Base64 String

5. Response กลับ Client
   └── ApiResponse { isOK: true, data: "base64..." }
```

---

## 3. ไฟล์สำคัญ

### 📄 GeneratePdfRequest.java (Model)

ไฟล์นี้สำคัญมาก! กำหนด structure ของ JSON ที่รับเข้ามา

```java
// ฟิลด์หลัก
private String bookNameId;       // GUID ระบุประเภทเอกสาร (สำคัญ!)
private String bookTitle;        // หัวเรื่อง
private String bookNo;           // เลขที่หนังสือ
private String dateThai;         // วันที่ (ภาษาไทย)
private String department;       // หน่วยงาน
private String divisionName;     // ชื่อส่วนงาน
private String speedLayer;       // ชั้นความเร็ว
private String formatPdf;        // รูปแบบ PDF (A4)

// Inner Classes
private List<BookContent> bookContent;    // เนื้อหา
private List<BookRelate> bookSigned;      // ผู้ลงนาม
private List<BookRelate> bookLearner;     // ผู้รับทราบ
private List<BookRelate> bookSubmited;    // ผู้เสนอผ่าน
```

### 📄 PdfService.java (Core PDF)

ไฟล์นี้รับผิดชอบการสร้าง PDF จริงๆ โดยใช้ PDFBox

```java
// ค่าคงที่ปรับแต่ง Layout
private static final float MARGIN_TOP = 70f;
private static final float MARGIN_LEFT = 70f;
private static final float FONT_SIZE_HEADER = 24f;
private static final float FONT_SIZE_CONTENT = 16f;

// Method หลัก
public String generateOfficialMemoPdf(...) {
    // สร้าง PDF บันทึกข้อความ
}
```

### 📄 HtmlUtils.java (HTML Converter)

แปลง HTML จาก Editor เป็น Plain Text สำหรับใส่ใน PDF

```java
// แปลง <br>, <p>, <li> เป็น \n
// ลบ HTML tags
// แปลง &nbsp; เป็น space
public static String htmlToPlainText(String html)
```

---

## 4. การเพิ่ม HTML Template ใหม่

### 📍 ที่อยู่ไฟล์ Template

```
src/main/resources/templates/
├── official-documents/          # สร้างไฟล์ Template ที่นี่
│   ├── official-memo.html       # บันทึกข้อความ
│   ├── official-document.html   # หนังสือราชการภายนอก
│   ├── regulation.html          # หนังสือระเบียบ
│   ├── announce.html            # หนังสือประกาศ
│   ├── command.html             # หนังสือคำสั่ง
│   ├── rules.html               # หนังสือข้อบังคับ
│   └── seal.html                # หนังสือประทับตรา
└── styles/
    └── document-style.css       # CSS สำหรับ PDF
```

### 🛠️ วิธีเพิ่ม Template ใหม่ (แบบ HTML)

**ขั้นตอนที่ 1: สร้าง HTML Template**

```html
<!-- src/main/resources/templates/official-documents/official-memo.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        @font-face {
            font-family: 'TH Sarabun';
            src: url('/fonts/THSarabunNew.ttf');
        }
        body {
            font-family: 'TH Sarabun', sans-serif;
            font-size: 16pt;
            margin: 2.5cm;
        }
        .header { text-align: center; font-size: 24pt; font-weight: bold; }
        .field { margin: 5px 0; }
        .field-label { font-weight: bold; }
        .content { text-indent: 2.5cm; line-height: 1.5; }
        .signature { text-align: right; margin-top: 30px; }
    </style>
</head>
<body>
    <!-- Logo -->
    <div class="logo">
        <img src="/images/logoETDA.png" width="120" />
    </div>
    
    <!-- Header -->
    <div class="header">บันทึกข้อความ</div>
    
    <!-- Fields -->
    <div class="field">
        <span class="field-label">ส่วนราชการ</span> {{department}}
    </div>
    <div class="field">
        <span class="field-label">ที่</span> {{bookNo}}
        <span class="field-label" style="margin-left: 50px;">วันที่</span> {{dateThai}}
    </div>
    <div class="field">
        <span class="field-label">เรื่อง</span> {{bookTitle}}
    </div>
    
    <!-- Content -->
    <div class="content">
        {{content}}
    </div>
    
    <!-- Signature -->
    <div class="signature">
        {{#each signatures}}
        <div>{{name}}</div>
        <div>{{position}}</div>
        {{/each}}
    </div>
</body>
</html>
```

**ขั้นตอนที่ 2: สร้าง TemplateService.java**

```java
// src/main/java/.../service/TemplateService.java
@Service
public class TemplateService {
    
    public String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource(
            "templates/official-documents/" + templateName + ".html");
        return new String(resource.getInputStream().readAllBytes(), 
                         StandardCharsets.UTF_8);
    }
    
    public String renderTemplate(String template, Map<String, Object> data) {
        // ใช้ Thymeleaf หรือ simple replace
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", 
                                   String.valueOf(entry.getValue()));
        }
        return result;
    }
}
```

**ขั้นตอนที่ 3: เพิ่ม Method ใน PdfService**

```java
// PdfService.java - เพิ่ม method ใหม่

public String generateFromTemplate(String templateName, 
                                   Map<String, Object> data) throws Exception {
    // 1. โหลด template
    String template = templateService.loadTemplate(templateName);
    
    // 2. Render ข้อมูล
    String html = templateService.renderTemplate(template, data);
    
    // 3. แปลง HTML เป็น PDF (ใช้ OpenHTMLtoPDF)
    return convertHtmlToPdf(html);
}

private String convertHtmlToPdf(String html) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    PdfRendererBuilder builder = new PdfRendererBuilder();
    builder.useFastMode();
    builder.withHtmlContent(html, "/");
    builder.toStream(baos);
    builder.run();
    
    return Base64.getEncoder().encodeToString(baos.toByteArray());
}
```

### 🛠️ วิธีที่ 2: ใช้ PDFBox โดยตรง (ปัจจุบัน)

ปัจจุบันระบบใช้ PDFBox วาด PDF โดยตรง ไม่ผ่าน HTML Template

**เพิ่ม Method ใหม่ใน PdfService.java:**

```java
/**
 * สร้างหนังสือราชการภายนอก
 */
public String generateOfficialDocumentPdf(
    String sender,           // จาก
    String receiver,         // ถึง
    String date,
    String bookNo,
    String title,
    String content,
    String speedLayer,
    List<String> signatures
) throws Exception {
    try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDFont fontRegular = loadThaiFont(document, FONT_PATH);
        PDFont fontBold = loadThaiFont(document, FONT_BOLD_PATH);
        
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            float y = PAGE_HEIGHT - MARGIN_TOP;
            
            // Logo ตรงกลางบน (ต่างจากบันทึกข้อความ)
            // ...
            
            // ที่ + วันที่ (ขวาบน)
            // ...
            
            // จาก (sender)
            // ถึง (receiver)
            // เรื่อง
            // เนื้อหา
            // ลายเซ็น
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
```

---

## 5. ประเภทเอกสารที่รองรับ

### 📌 BookNameId Mapping (16 GUIDs → 8 ประเภท)

```java
// ใน GeneratePdfService.java

public enum DocumentType {
    OFFICIAL_MEMO,       // บันทึกข้อความ
    OFFICIAL_DOCUMENT,   // หนังสือราชการภายนอก
    REGULATION,          // หนังสือระเบียบ
    ANNOUNCE,            // หนังสือประกาศ
    COMMAND,             // หนังสือคำสั่ง
    RULES,               // หนังสือข้อบังคับ
    SEAL,                // หนังสือประทับตรา
    SPECIAL              // หนังสือพิเศษ/อื่นๆ
}

private static final Map<String, DocumentType> BOOK_NAME_ID_MAP = Map.ofEntries(
    // บันทึกข้อความ
    Map.entry("90F72F0E-528D-4992-907A-F2C6B37AD9A5", DocumentType.OFFICIAL_MEMO),
    Map.entry("50792880-F85A-4343-9672-7B61AF828A5B", DocumentType.OFFICIAL_MEMO),
    
    // หนังสือราชการภายนอก  
    Map.entry("23065068-BB18-49EA-8CE7-22945E16CB6D", DocumentType.OFFICIAL_DOCUMENT),
    Map.entry("3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D", DocumentType.OFFICIAL_DOCUMENT),
    
    // หนังสือระเบียบ
    Map.entry("4AB1EC00-9E5E-4113-B577-D8ED46BA7728", DocumentType.REGULATION),
    Map.entry("4B3EB169-6203-4A71-A3BD-A442FEAAA91F", DocumentType.REGULATION),
    
    // หนังสือประกาศ
    Map.entry("AF3E7697-6F7E-4AD8-B76C-E2134DB98747", DocumentType.ANNOUNCE),
    Map.entry("03241AA7-0E85-4C5C-A2CC-688212A79B84", DocumentType.ANNOUNCE),
    
    // หนังสือคำสั่ง
    Map.entry("C2905724-04D3-46AF-81EA-BF3045A59BF2", DocumentType.COMMAND),
    Map.entry("11B56C3B-1C8E-4574-8B5D-72659BE74E6A", DocumentType.COMMAND),
    
    // หนังสือข้อบังคับ
    Map.entry("8F6A1804-C340-49B4-9BFB-FE523E640AA1", DocumentType.RULES),
    Map.entry("63E72391-0261-4C71-A8DE-F23CBB3033A9", DocumentType.RULES),
    
    // หนังสือประทับตรา
    Map.entry("DD65959E-06BD-40C9-9793-211DB2084A65", DocumentType.SEAL),
    Map.entry("1AD2CD13-D938-4DD9-9407-9E922BA4652E", DocumentType.SEAL),
    
    // หนังสือพิเศษ
    Map.entry("969D9478-5A0E-42CF-8BBE-6B188C71588B", DocumentType.SPECIAL),
    Map.entry("0BF965C9-095B-4B73-BAB4-A0BDADA6993D", DocumentType.SPECIAL)
);
```

---

## 6. การปรับแต่ง Layout

### 📐 ค่าคงที่ใน PdfService.java

```java
// === ขนาดหน้ากระดาษ ===
PAGE_WIDTH = 595f;    // A4 width (points)
PAGE_HEIGHT = 842f;   // A4 height (points)

// === Margins (ระยะขอบ) ===
MARGIN_TOP = 70f;     // ขอบบน
MARGIN_BOTTOM = 70f;  // ขอบล่าง
MARGIN_LEFT = 70f;    // ขอบซ้าย
MARGIN_RIGHT = 70f;   // ขอบขวา

// === Logo ===
LOGO_WIDTH = 120f;    // ความกว้าง logo
LOGO_HEIGHT = 40f;    // ความสูง logo
LOGO_SPACING = 30f;   // ระยะห่างหลัง logo

// === Font Sizes ===
FONT_SIZE_HEADER = 24f;        // "บันทึกข้อความ"
FONT_SIZE_FIELD = 18f;         // label
FONT_SIZE_FIELD_VALUE = 16f;   // value
FONT_SIZE_CONTENT = 16f;       // เนื้อหา

// === Spacing ===
SPACING_AFTER_HEADER = 30f;
SPACING_BETWEEN_FIELDS = 5f;
SPACING_BEFORE_CONTENT = 14f;
SPACING_BEFORE_SIGNATURES = 40f;

// === Debug Mode ===
ENABLE_DEBUG_BORDERS = false;  // true = แสดงเส้นขอบแดง
```

### 🎨 วิธีปรับ Layout

1. **เปลี่ยน Margin**: แก้ค่า `MARGIN_*`
2. **เปลี่ยนขนาด Font**: แก้ค่า `FONT_SIZE_*`
3. **เปลี่ยนระยะห่าง**: แก้ค่า `SPACING_*`
4. **Debug Mode**: ตั้ง `ENABLE_DEBUG_BORDERS = true` เพื่อดูขอบเขต

---

## 7. ขั้นตอนการพัฒนาต่อ

### ✅ ที่ทำเสร็จแล้ว

- [x] Migration จาก .NET ไป Java
- [x] ใช้ PDFBox 2.0.31 (ไม่มี license)
- [x] สร้าง PDF บันทึกข้อความพื้นฐาน
- [x] รองรับ Thai Font (TH Sarabun)
- [x] API Tester หน้าเว็บ
- [x] รวม PDF หลายไฟล์
- [x] เพิ่มลายเซ็น

### 🔲 TODO: งานที่ต้องทำต่อ

#### Priority 1: เพิ่ม API Endpoints แยกตามประเภท

```java
// GeneratePdfController.java - เพิ่ม endpoints

@PostMapping("/Pdf/GenerateOfficialMemoPdfAsync")
public ResponseEntity<ApiResponse<String>> generateOfficialMemo(...)

@PostMapping("/Pdf/GenerateOfficialDocumentHtmlAsync")
public ResponseEntity<ApiResponse<String>> generateOfficialDocument(...)

@PostMapping("/Pdf/GenerateRegulationDocumentHtmlPdfAsync_flow")
public ResponseEntity<ApiResponse<String>> generateRegulation(...)

@PostMapping("/Pdf/GenerateAnnounceDocumentHtmlPdfAsync_flow")
public ResponseEntity<ApiResponse<String>> generateAnnounce(...)

@PostMapping("/Pdf/GenerateCommandDocumentHtmlPdfAsync_flow")
public ResponseEntity<ApiResponse<String>> generateCommand(...)

@PostMapping("/Pdf/GenerateRulesDocumentHtmlPdfAsync_flow")
public ResponseEntity<ApiResponse<String>> generateRules(...)

@PostMapping("/Pdf/GenerateSealDocumentHtmlPdfAsync_flow")
public ResponseEntity<ApiResponse<String>> generateSeal(...)
```

#### Priority 2: เพิ่ม Methods ใน PdfService.java

| Method | ประเภท | Layout ต่างจากบันทึกข้อความ |
|--------|--------|---------------------------|
| `generateOfficialDocumentPdf()` | หนังสือราชการภายนอก | Logo กลาง, มี "จาก/ถึง" |
| `generateRegulationPdf()` | หนังสือระเบียบ | มีหมายเลขข้อ |
| `generateAnnouncePdf()` | หนังสือประกาศ | หัวข้อ "ประกาศ" |
| `generateCommandPdf()` | หนังสือคำสั่ง | หัวข้อ "คำสั่ง", มีข้อ |
| `generateRulesPdf()` | หนังสือข้อบังคับ | มีหมวด/ข้อ |
| `generateSealPdf()` | หนังสือประทับตรา | ไม่มีลายเซ็น, มีตรา |

#### Priority 3: สร้าง HTML Templates

```
src/main/resources/templates/official-documents/
├── official-memo.html
├── official-document.html
├── regulation.html
├── announce.html
├── command.html
├── rules.html
└── seal.html
```

#### Priority 4: เพิ่ม Library สำหรับ HTML to PDF

```xml
<!-- pom.xml - เพิ่ม dependency -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
</dependency>
```

### 📝 Checklist การเพิ่มประเภทเอกสารใหม่

1. [ ] สร้าง HTML Template ใน `templates/official-documents/`
2. [ ] เพิ่ม Method ใน `PdfService.java`
3. [ ] เพิ่ม BookNameId ใน `BOOK_NAME_ID_MAP`
4. [ ] เพิ่ม Case ใน `GeneratePdfService.generateMainPdf()`
5. [ ] เพิ่ม API Endpoint ใน `GeneratePdfController.java`
6. [ ] เพิ่มตัวเลือกใน `api-tester.html`
7. [ ] ทดสอบ

---

## 📚 Resources

- [Apache PDFBox 2.x Documentation](https://pdfbox.apache.org/2.0/)
- [OpenHTMLtoPDF GitHub](https://github.com/danfickle/openhtmltopdf)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

*Last Updated: January 8, 2026*
