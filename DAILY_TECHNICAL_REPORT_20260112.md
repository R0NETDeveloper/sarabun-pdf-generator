# 🗓 Daily Technical Analysis Report

**วันที่:** 12 มกราคม 2569 (2026-01-12)  
**โปรเจ็ค:** Sarabun PDF API  
**เวอร์ชัน:** 1.0.0  
**เทคโนโลยี:** Java 17 + Spring Boot 3.5.9 + Apache PDFBox 2.0.31

---

## 1. ภาพรวมระบบ (System Overview)

### ระบบนี้ทำอะไร

**Sarabun PDF API** คือระบบ RESTful API สำหรับ **สร้างหนังสือราชการในรูปแบบ PDF** ตามมาตรฐานสำนักนายกรัฐมนตรี รองรับ 9 ประเภทเอกสาร:

| รหัส         | ประเภท               | ชื่อไทย                         |
| ------------ | -------------------- | ------------------------------- |
| MEMO         | บันทึกข้อความ        | บันทึกข้อความภายใน              |
| OUTBOUND     | หนังสือส่งออก        | หนังสือราชการภายนอก             |
| INBOUND      | หนังสือรับเข้า       | หนังสือราชการรับเข้า            |
| ANNOUNCEMENT | หนังสือประกาศ        | ประกาศหน่วยงาน                  |
| ORDER        | หนังสือคำสั่ง        | คำสั่งแต่งตั้ง/มอบหมาย          |
| REGULATION   | หนังสือระเบียบ       | ระเบียบข้อบังคับ                |
| RULE         | หนังสือข้อบังคับ     | ข้อบังคับองค์กร                 |
| STAMP        | หนังสือประทับตรา     | หนังสือประทับตรา                |
| MINISTRY     | หนังสือภายใต้กระทรวง | หนังสือระหว่างหน่วยงานในกระทรวง |

### ใช้ในบริบทใด

-   **หน่วยงานราชการ** ที่ต้องการสร้างเอกสารราชการรูปแบบมาตรฐาน
-   **ระบบ e-Saraban** (ระบบสารบรรณอิเล็กทรอนิกส์) เรียกใช้ผ่าน API
-   เป็น **Microservice** ที่แยกออกจากระบบหลัก (Migrated from .NET)

### ผู้ใช้งานคือใคร

-   **Backend Systems** ที่เรียกใช้ผ่าน REST API
-   **Developer** ที่ทดสอบผ่าน `/api/pdf/view` หรือ Swagger UI
-   **ระบบ e-Office** ของหน่วยงานราชการ

---

## 2. Flow การทำงาน (Source → Destination)

### 2.1 ต้นทางข้อมูล (Source)

```
┌──────────────────────────────────────────────────────────────────┐
│  HTTP POST /api/pdf/preview                                      │
│  Content-Type: application/json                                   │
├──────────────────────────────────────────────────────────────────┤
│  GeneratePdfRequest (JSON Body)                                  │
│  ├─ bookNameId (UUID) → ระบุประเภทเอกสาร                         │
│  ├─ bookTitle → ชื่อเรื่อง                                        │
│  ├─ bookNo → เลขที่หนังสือ                                        │
│  ├─ dateThai → วันที่ภาษาไทย                                     │
│  ├─ bookContent[] → เนื้อหา (HTML/Text)                          │
│  ├─ bookSigned[] → ผู้ลงนาม                                       │
│  ├─ bookSubmited[] → ผู้เสนอผ่าน                                  │
│  ├─ bookLearner[] → ผู้รับภายใน                                   │
│  ├─ bookRecipients[] → ผู้รับภายนอก (หน่วยงาน)                    │
│  └─ ... (อื่นๆ)                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**รูปแบบข้อมูล:**

-   JSON Request Body ขนาดใหญ่ (~50+ fields)
-   เนื้อหาอาจเป็น HTML (จาก WYSIWYG editor) หรือ Plain Text
-   ลายเซ็นเป็น Base64 encoded image

### 2.2 ขั้นตอนประมวลผล (Processing)

```
┌─────────────────────────────────────────────────────────────────────────┐
│  LAYER 1: Controller                                                     │
│  GeneratePdfController.previewPdf()                                     │
│  └─ รับ JSON → Validate → เรียก Service                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  LAYER 2: Service                                                        │
│  GeneratePdfService.previewPdf()                                        │
│  ├─ 1. แปลง bookNameId → BookType enum                                  │
│  ├─ 2. เรียก PdfGeneratorFactory.getGenerator(bookType)                 │
│  ├─ 3. Generator.generate() → สร้าง PDF array                           │
│  ├─ 4. addSignaturesToPdfs() → เพิ่มลายเซ็น                              │
│  ├─ 5. addSubmitPages() → เพิ่มหน้าเสนอผ่าน                              │
│  ├─ 6. addLearnerPages() → เพิ่มหน้าผู้เรียน                             │
│  └─ 7. mergePdfArray() → รวม PDF                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  LAYER 3: Factory Pattern                                                │
│  PdfGeneratorFactory                                                     │
│  └─ เลือก Generator ตาม BookType:                                        │
│      ├─ MEMO → MemoPdfGenerator                                          │
│      ├─ OUTBOUND → OutboundPdfGenerator                                  │
│      ├─ ANNOUNCEMENT → AnnouncementPdfGenerator                          │
│      ├─ ORDER → OrderPdfGenerator                                        │
│      ├─ REGULATION → RegulationPdfGenerator                              │
│      ├─ RULE → RulePdfGenerator                                          │
│      ├─ STAMP → StampPdfGenerator                                        │
│      ├─ MINISTRY → MinistryPdfGenerator                                  │
│      └─ INBOUND → InboundPdfGenerator                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  LAYER 4: PDF Generator (Template Method Pattern)                        │
│  PdfGeneratorBase (Abstract)                                             │
│  └─ Common methods:                                                      │
│      ├─ loadThaiFont() → โหลดฟอนต์ TH Sarabun                            │
│      ├─ drawText() → วาดข้อความ                                          │
│      ├─ drawLogo() → วาดโลโก้                                            │
│      ├─ drawSignerBox() → วาดกรอบลายเซ็น                                 │
│      ├─ drawSignerBoxWithSignatureField() → สร้าง AcroForm field         │
│      ├─ splitTextToLines() → ตัดข้อความหลายบรรทัด                        │
│      └─ convertToBase64() → แปลง PDF เป็น Base64                         │
├─────────────────────────────────────────────────────────────────────────┤
│  LAYER 5: Utilities                                                      │
│  HtmlUtils                                                               │
│  └─ htmlToPlainText() → แปลง HTML เป็น plain text (ใช้ Jsoup)             │
└─────────────────────────────────────────────────────────────────────────┘
```

**ลำดับการทำงานหลัก:**

```
1. Controller รับ Request
       ↓
2. BookType.fromId(bookNameId) → แปลง UUID เป็น Enum
       ↓
3. Factory.getGenerator(bookType) → เลือก Generator
       ↓
4. Generator.generate(request) → สร้าง PDF
   ├─ โหลดฟอนต์ TH Sarabun
   ├─ สร้าง PDDocument (PDFBox)
   ├─ วาดองค์ประกอบ (Logo, Header, Content, Signature)
   └─ แปลงเป็น Base64
       ↓
5. เพิ่มหน้าเสนอผ่าน/ผู้เรียน (ถ้ามี)
       ↓
6. รวม PDF ทั้งหมด (PDFMergerUtility)
       ↓
7. ส่งกลับ ApiResponse<Base64 PDF>
```

### 2.3 ปลายทาง (Destination)

```
┌──────────────────────────────────────────────────────────────────┐
│  HTTP Response                                                    │
├──────────────────────────────────────────────────────────────────┤
│  ApiResponse<String>                                             │
│  {                                                                │
│    "isOk": true,                                                  │
│    "message": "สร้าง PDF สำเร็จ (บันทึกข้อความ)",                  │
│    "data": "JVBERi0xLjQKJeLjz9MK..." // Base64 encoded PDF       │
│  }                                                                │
└──────────────────────────────────────────────────────────────────┘
```

**Output:**

-   PDF เป็น Base64 string (ในฟิลด์ `data`)
-   รองรับ inline display ใน browser ผ่าน `/api/pdf/view`

**Side Effects:**

-   ❌ ไม่บันทึกลง Database
-   ❌ ไม่เขียนไฟล์ลง disk (stateless)
-   ✅ Log ลง console (SLF4J)
-   ✅ สร้าง AcroForm Signature Fields ใน PDF

---

## 3. Library / Plugin ที่ใช้

| Library / Plugin            | เวอร์ชัน | ใช้ทำอะไร                    | จำเป็น | ข้อสังเกต                                                  |
| --------------------------- | -------- | ---------------------------- | ------ | ---------------------------------------------------------- |
| **Spring Boot Starter Web** | 3.5.9    | REST API framework           | ✅     | เหมาะสม, ใหม่ล่าสุด                                        |
| **Spring Boot Security**    | 3.5.9    | Authentication/Authorization | ⚠️     | ปัจจุบันปิด auth ทั้งหมด (permitAll) อาจไม่จำเป็นถ้าไม่ใช้ |
| **Apache PDFBox**           | 2.0.31   | สร้าง/แก้ไข PDF              | ✅     | เลือกถูกต้อง (Open Source, รองรับ AcroForm)                |
| **FontBox**                 | 2.0.31   | จัดการฟอนต์                  | ✅     | ต้องใช้คู่กับ PDFBox                                       |
| **OpenHTMLtoPDF**           | 1.0.10   | แปลง HTML เป็น PDF           | ❓     | **ไม่เห็นถูกใช้งานจริงในโค้ด** → อาจลบได้                  |
| **JJWT**                    | 0.12.6   | JWT Token                    | ⚠️     | ไม่เห็นถูกใช้งาน (Security ปิดอยู่) → อาจลบได้             |
| **Lombok**                  | -        | ลด boilerplate               | ✅     | เหมาะสม                                                    |
| **Spring Validation**       | -        | Validate request             | ⚠️     | มีแต่ไม่เห็นใช้ @Valid annotation                          |
| **Springdoc OpenAPI**       | 2.7.0    | Swagger UI                   | ✅     | มีประโยชน์สำหรับ API documentation                         |
| **Jackson**                 | -        | JSON serialization           | ✅     | จำเป็น                                                     |
| **Commons IO**              | 2.18.0   | File utilities               | ⚠️     | ไม่เห็นถูกใช้งานจริง → อาจลบได้                            |
| **Jsoup**                   | 1.18.3   | HTML parsing                 | ✅     | ใช้ใน HtmlUtils                                            |
| **DevTools**                | -        | Hot reload                   | ✅     | development only                                           |

### สรุปภาพรวม Dependency

**ควรลบ/พิจารณา:**

1. **OpenHTMLtoPDF** (3 artifacts) - มีใน pom.xml แต่ไม่เห็นถูกใช้ → ลด ~3-5MB
2. **JJWT** (3 artifacts) - Security ปิดอยู่ → ลด ~1MB
3. **Commons IO** - ไม่มี import ในโค้ด → ลด ~300KB
4. **Spring Security** - ถ้าไม่ต้องการ auth เลย → ลด ~2MB

**ประมาณการลดขนาด JAR:** ~10MB (ถ้าลบทั้งหมด)

---

## 4. ข้อดีของโครงสร้างปัจจุบัน

### ด้าน Readability ⭐⭐⭐⭐

-   ✅ **Package structure ชัดเจน**: controller, service, model, util, constant
-   ✅ **Naming convention ดี**: ชื่อ class/method สื่อความหมาย
-   ✅ **Comments ภาษาไทยครบถ้วน**: เข้าใจ business logic ง่าย
-   ✅ **Lombok ลด boilerplate**: @Data, @Builder, @RequiredArgsConstructor

### ด้าน Maintainability ⭐⭐⭐⭐⭐

-   ✅ **Factory Pattern**: เพิ่ม Generator ใหม่ง่าย (แค่สร้าง class + ลงทะเบียน)
-   ✅ **Template Method Pattern**: PdfGeneratorBase รวม common logic
-   ✅ **Single Responsibility**: แต่ละ Generator ดูแลเอกสารประเภทเดียว
-   ✅ **Enum สำหรับ BookType**: Type-safe, ไม่ต้องจำ magic strings

### ด้าน Extensibility ⭐⭐⭐⭐

-   ✅ **เพิ่มประเภทเอกสารใหม่ง่าย**:
    1. สร้าง XxxPdfGenerator extends PdfGeneratorBase
    2. เพิ่ม enum ใน BookType
    3. ลงทะเบียนใน PdfGeneratorFactory
-   ✅ **Override ได้ทุก method ใน Base class**
-   ✅ **ไม่ hardcode**: ใช้ constants และ enum

### ด้าน Architecture ⭐⭐⭐⭐

-   ✅ **Layered Architecture**: Controller → Service → Generator
-   ✅ **Dependency Injection**: ใช้ Spring @Component, @RequiredArgsConstructor
-   ✅ **Stateless Design**: ไม่มี session/state → scale ง่าย
-   ✅ **RESTful API**: มาตรฐาน HTTP methods

---

## 5. ข้อโหว่ / จุดอ่อน

### 5.1 Design Smells

| ปัญหา                   | ที่พบ                                              | ความรุนแรง |
| ----------------------- | -------------------------------------------------- | ---------- |
| **God Class**           | PdfGeneratorBase (1361 lines)                      | 🔴 สูง     |
| **Feature Envy**        | GeneratePdfService รู้เรื่อง merge logic มากเกินไป | 🟡 กลาง    |
| **Primitive Obsession** | ใช้ String แทน Value Object (เช่น Email, BookNo)   | 🟡 กลาง    |
| **Magic Numbers**       | Constants กระจายทั่ว Base class                    | 🟢 ต่ำ     |

### 5.2 Coupling สูง

```java
// GeneratePdfService ต้องรู้จัก MemoPdfGenerator โดยตรง
private final MemoPdfGenerator memoPdfGenerator;

// OutboundPdfGenerator ต้อง inject MemoPdfGenerator
private final MemoPdfGenerator memoPdfGenerator;
```

→ ควรใช้ Interface หรือผ่าน Factory

### 5.3 Logic กระจุกตัว

-   **PdfGeneratorBase** ทำหลายหน้าที่เกินไป:
    -   Font management
    -   Text rendering
    -   Page management
    -   Signature field creation
    -   PDF merging
    -   Thai number/date conversion

### 5.4 ความเสี่ยงในอนาคต

| ความเสี่ยง           | รายละเอียด                           | ผลกระทบ                          |
| -------------------- | ------------------------------------ | -------------------------------- |
| **Security**         | API ไม่มี authentication (permitAll) | 🔴 ถูกเรียกใช้โดยไม่ได้รับอนุญาต |
| **Input Validation** | ไม่มี @Valid, ไม่เช็ค null           | 🔴 NullPointerException          |
| **Error Handling**   | catch Exception ทั่วไป               | 🟡 Debug ยาก                     |
| **Font Missing**     | ถ้าไม่มี THSarabunNew.ttf            | 🔴 ระบบ fail                     |
| **Memory**           | PDF ใหญ่ + Merge หลายไฟล์            | 🟡 OutOfMemoryError              |

### 5.5 Missing Features

-   ❌ Unit Tests (มีแค่ 1 test class ว่างเปล่า)
-   ❌ Integration Tests
-   ❌ Request Validation (@Valid)
-   ❌ Rate Limiting
-   ❌ Caching
-   ❌ Health Check endpoint ครบถ้วน (มีแค่ simple check)

---

## 6. Performance & Scalability

### 6.1 จุดที่อาจช้า

| จุด                      | ปัญหา                                        | แนวทางแก้                               |
| ------------------------ | -------------------------------------------- | --------------------------------------- |
| **Font Loading**         | โหลดฟอนต์ทุกครั้งที่สร้าง PDF                | Cache font ใน static field              |
| **PDF Merge**            | ใช้ MemoryUsageSetting.setupMainMemoryOnly() | ใช้ setupTempFileOnly() สำหรับ PDF ใหญ่ |
| **Base64 Encode/Decode** | แปลง byte[] ↔ String หลายครั้ง               | ส่ง byte[] ตลอด, แปลงตอนสุดท้าย         |
| **HTML to Text**         | Parse HTML ทุกครั้ง                          | Cache ถ้า content ซ้ำ                   |

### 6.2 การทำงานซ้ำ

```java
// PdfGeneratorBase.java - โหลดฟอนต์ซ้ำทุก PDF
PDFont fontRegular = loadRegularFont(document);  // ทุกครั้ง!
PDFont fontBold = loadBoldFont(document);        // ทุกครั้ง!
```

```java
// GeneratePdfService.java + PdfGeneratorBase.java - มี mergePdfFiles() ซ้ำกัน!
// Line 256-279 ใน GeneratePdfService
// Line 755-776 ใน PdfGeneratorBase
```

### 6.3 การใช้ Resource ที่ไม่จำเป็น

```java
// สร้าง ArrayList ใหม่ทุกครั้งแม้มีรายการเดียว
List<String> pdfsToMerge = new ArrayList<>();
pdfsToMerge.add(...);
// ควรใช้ List.of() หรือ Collections.singletonList()

// ByteArrayOutputStream ไม่ระบุ initial size
ByteArrayOutputStream baos = new ByteArrayOutputStream();
// ควรระบุขนาดโดยประมาณ เช่น new ByteArrayOutputStream(50_000)
```

### 6.4 โอกาสในการ Optimize

| ปรับปรุง                       | ผลลัพธ์ที่คาดหวัง       |
| ------------------------------ | ----------------------- |
| Cache loaded fonts             | -30% response time      |
| Pre-size ByteArrayOutputStream | -10% memory churn       |
| Async PDF generation           | Better throughput       |
| Connection pooling (ถ้ามี DB)  | N/A                     |
| Lazy loading images            | -20% memory ต่อ request |

---

## 7. Method / Code ที่เข้าข่าย "ขยะ"

### 7.1 Unused Methods

| Method                  | Class               | เหตุผล                                 |
| ----------------------- | ------------------- | -------------------------------------- |
| `addSignaturesToPdfs()` | GeneratePdfService  | มีแค่ `return pdfArray;` (passthrough) |
| `isHtml()`              | HtmlUtils           | ถูกเรียกใช้แต่อาจเกินความจำเป็น        |
| `main()`                | HtmlUtils           | สำหรับ debug เท่านั้น                  |
| `getGeneratorByCode()`  | PdfGeneratorFactory | ไม่เห็นถูกเรียกใช้                     |

### 7.2 Dead Code

```java
// GeneratePdfService.java:116-121
@SuppressWarnings("unused")
private List<PdfResult> addSignaturesToPdfs(List<PdfResult> pdfArray,
                                            GeneratePdfRequest request) {
    log.debug("Adding signatures to PDFs (passthrough - signatures are drawn in generators)");
    return pdfArray;  // ไม่ทำอะไรเลย!
}
```

### 7.3 Methods ที่ทำหลายหน้าที่

| Method                                     | Lines | หน้าที่                                             |
| ------------------------------------------ | ----- | --------------------------------------------------- |
| `drawSignerBoxWithSignatureField()`        | 100+  | วาด box + สร้าง AcroForm + วาดชื่อ + วาดตำแหน่ง     |
| `generatePdfInternal()` (MemoPdfGenerator) | 100+  | สร้าง document + วาดทุก section + จัดการ pagination |
| `addSubmitPages()`                         | 80+   | สร้างหน้า + วาดหัวข้อ + loop วาดลายเซ็น             |

### 7.4 Duplicate Logic

| Logic                           | พบใน                                         | ควรรวมเป็น            |
| ------------------------------- | -------------------------------------------- | --------------------- |
| `mergePdfFiles()`               | GeneratePdfService + PdfGeneratorBase        | PdfMergeUtility class |
| `cleanBase64Prefix()`           | GeneratePdfService + PdfGeneratorBase        | Base64Utils class     |
| สร้าง SignerInfo จาก BookRelate | ทุก Generator                                | RequestMapper class   |
| วาดหัวข้อ + วันที่ + เลขที่     | MemoPdfGenerator, OutboundPdfGenerator, etc. | HeaderRenderer class  |

---

## 8. จุดที่ควรทำเป็นฟังก์ชันกลาง (Shared Function)

### 8.1 แนะนำ Utility Classes ใหม่

```
src/main/java/th/go/etda/sarabun/pdf/
├── util/
│   ├── HtmlUtils.java          (มีอยู่แล้ว)
│   ├── Base64Utils.java        ⭐ NEW
│   ├── ThaiDateUtils.java      ⭐ NEW
│   ├── PdfMergeUtils.java      ⭐ NEW
│   └── FontManager.java        ⭐ NEW
├── renderer/                    ⭐ NEW PACKAGE
│   ├── HeaderRenderer.java
│   ├── ContentRenderer.java
│   ├── SignatureRenderer.java
│   └── FooterRenderer.java
├── mapper/                      ⭐ NEW PACKAGE
│   └── RequestMapper.java
```

### 8.2 รายละเอียดแต่ละ Utility

#### 1. `Base64Utils.java`

```java
public class Base64Utils {
    public static String cleanPrefix(String base64);
    public static byte[] decode(String base64);
    public static String encode(byte[] bytes);
    public static String encodeWithPrefix(byte[] bytes, String mimeType);
}
```

**เหตุผล:** ลบ duplicate `cleanBase64Prefix()` ใน 2+ classes

#### 2. `ThaiDateUtils.java`

```java
public class ThaiDateUtils {
    public static String toThaiDate(LocalDate date);
    public static String toThaiNumber(int number);
    public static String getThaiMonth(int month);
}
```

**เหตุผล:** ย้าย logic จาก PdfGeneratorBase

#### 3. `FontManager.java`

```java
@Component
public class FontManager {
    private PDFont cachedRegularFont;
    private PDFont cachedBoldFont;

    public PDFont getRegularFont(PDDocument doc);
    public PDFont getBoldFont(PDDocument doc);
    public void clearCache();
}
```

**เหตุผล:** Cache font + ลด duplicate loading

#### 4. `PdfMergeUtils.java`

```java
public class PdfMergeUtils {
    public static String mergeToBase64(List<String> base64Pdfs);
    public static byte[] mergeToBytes(List<byte[]> pdfs);
    public static void merge(List<InputStream> sources, OutputStream dest);
}
```

**เหตุผล:** รวม duplicate `mergePdfFiles()`

### 8.3 แนะนำ Refactor PdfGeneratorBase

**ปัจจุบัน:** 1361 lines (God Class)

**แนะนำแยกเป็น:**

| Class               | หน้าที่                              | Lines (ประมาณ) |
| ------------------- | ------------------------------------ | -------------- |
| `PdfGeneratorBase`  | Abstract base + template method      | ~200           |
| `PageManager`       | สร้างหน้า + pagination + page number | ~150           |
| `TextRenderer`      | วาดข้อความ + multiline + underline   | ~200           |
| `SignatureRenderer` | วาดกรอบลายเซ็น + AcroForm            | ~250           |
| `HeaderRenderer`    | วาดหัวเอกสาร + logo + speed layer    | ~150           |
| `ThaiFormatter`     | แปลงเลขไทย + วันที่ไทย               | ~100           |

---

## 9. แนะนำการเขียนเอกสาร

### 9.1 เอกสารที่ควรมี

| เอกสาร                                  | ความสำคัญ  | สถานะ             |
| --------------------------------------- | ---------- | ----------------- |
| **README.md**                           | ⭐⭐⭐⭐⭐ | ❌ ไม่มี          |
| **API Documentation** (Swagger)         | ⭐⭐⭐⭐⭐ | ✅ มี (Springdoc) |
| **Architecture Decision Records (ADR)** | ⭐⭐⭐⭐   | ❌ ไม่มี          |
| **Developer Guide**                     | ⭐⭐⭐⭐   | ❌ ไม่มี          |
| **Deployment Guide**                    | ⭐⭐⭐⭐   | ❌ ไม่มี          |
| **CHANGELOG.md**                        | ⭐⭐⭐     | ❌ ไม่มี          |
| **CONTRIBUTING.md**                     | ⭐⭐       | ❌ ไม่มี          |

### 9.2 โครงสร้าง Doc ที่แนะนำ

```
docs/
├── README.md                    # Quick start + overview
├── CHANGELOG.md                 # Version history
├── CONTRIBUTING.md              # How to contribute
├── architecture/
│   ├── overview.md              # System architecture
│   ├── adr/                     # Architecture Decision Records
│   │   ├── 001-use-pdfbox.md
│   │   ├── 002-factory-pattern.md
│   │   └── 003-base64-response.md
│   └── diagrams/
│       ├── flow.png
│       └── class-diagram.png
├── api/
│   ├── endpoints.md             # API endpoints reference
│   ├── request-samples/         # Sample JSON requests
│   │   ├── memo.json
│   │   ├── outbound.json
│   │   └── announcement.json
│   └── response-samples/        # Sample responses
├── development/
│   ├── setup.md                 # Local development setup
│   ├── coding-standards.md      # Code style guide
│   ├── testing.md               # How to test
│   └── debugging.md             # Common issues & solutions
├── deployment/
│   ├── docker.md                # Docker deployment
│   ├── kubernetes.md            # K8s deployment
│   └── environment-vars.md      # Configuration
└── fonts/
    └── installation.md          # Font installation guide
```

### 9.3 README.md ที่ควรมี

````markdown
# Sarabun PDF API

> RESTful API สำหรับสร้างหนังสือราชการในรูปแบบ PDF

## Features

-   รองรับ 9 ประเภทเอกสาร
-   รองรับฟอนต์ภาษาไทย (TH Sarabun New)
-   สร้าง AcroForm Signature Fields
-   Stateless design (scale ง่าย)

## Quick Start

### Prerequisites

-   Java 17+
-   Maven 3.8+
-   Font: THSarabunNew.ttf

### Run

```bash
./mvnw spring-boot:run
```
````

### Test

```bash
curl -X POST http://localhost:8888/api/pdf/preview \
  -H "Content-Type: application/json" \
  -d @test-request.json
```

## API Endpoints

| Method | Endpoint            | Description        |
| ------ | ------------------- | ------------------ |
| POST   | /api/pdf/preview    | สร้าง PDF          |
| GET    | /api/pdf/health     | Health check       |
| GET    | /api/pdf/view       | ดู PDF ใน browser  |
| GET    | /api/pdf/book-types | รายการประเภทเอกสาร |

## Architecture

![Flow Diagram](docs/architecture/diagrams/flow.png)

## License

Proprietary - ETDA

```

---

## 10. Action Items (สรุป)

### 🔴 Priority High (ควรทำทันที)

| # | รายการ | ประโยชน์ |
|---|--------|---------|
| 1 | เพิ่ม Input Validation (@Valid) | ป้องกัน NullPointerException |
| 2 | เปิด Security หรือลบถ้าไม่ใช้ | ลด attack surface |
| 3 | เพิ่ม Unit Tests | เพิ่มความมั่นใจในการแก้ไข |
| 4 | สร้าง README.md | Developer experience |

### 🟡 Priority Medium (ควรทำในสัปดาห์นี้)

| # | รายการ | ประโยชน์ |
|---|--------|---------|
| 5 | ลบ unused dependencies | ลด JAR size ~10MB |
| 6 | แยก PdfGeneratorBase เป็น smaller classes | Maintainability |
| 7 | สร้าง Utility classes (Base64Utils, FontManager) | Reduce duplication |
| 8 | Cache loaded fonts | Performance +30% |

### 🟢 Priority Low (ทำเมื่อว่าง)

| # | รายการ | ประโยชน์ |
|---|--------|---------|
| 9 | สร้าง Architecture documentation | Knowledge sharing |
| 10 | เพิ่ม Integration tests | Regression testing |
| 11 | ปรับ MemoryUsageSetting สำหรับ PDF ใหญ่ | Prevent OOM |
| 12 | เพิ่ม Metrics & Monitoring | Observability |

---

## 11. Appendix

### A. Dependency Tree (Summary)

```

sarabun-pdf-api:1.0.0
├── spring-boot-starter-web:3.5.9
├── spring-boot-starter-security:3.5.9 ⚠️ (unused)
├── pdfbox:2.0.31
├── fontbox:2.0.31
├── openhtmltopdf-_:1.0.10 ⚠️ (unused)
├── jjwt-_:0.12.6 ⚠️ (unused)
├── lombok
├── spring-boot-starter-validation ⚠️ (unused)
├── springdoc-openapi:2.7.0
├── commons-io:2.18.0 ⚠️ (unused)
├── jsoup:1.18.3
└── spring-boot-devtools

````

### B. Key Files Reference

| File | Purpose | Lines |
|------|---------|-------|
| `PdfGeneratorBase.java` | Abstract base class | 1361 |
| `MemoPdfGenerator.java` | บันทึกข้อความ | ~300 |
| `OutboundPdfGenerator.java` | หนังสือส่งออก | 624 |
| `GeneratePdfRequest.java` | Request model | 341 |
| `GeneratePdfService.java` | Main service | 296 |
| `PdfGeneratorFactory.java` | Factory pattern | ~120 |
| `HtmlUtils.java` | HTML utilities | ~170 |

### C. BookType UUID Mapping

```java
MEMO         = "BB4A2F11-722D-449A-BCC5-22208C7A4DEC"
REGULATION   = "50792880-F85A-4343-9672-7B61AF828A5B"
ANNOUNCEMENT = "23065068-BB18-49EA-8CE7-22945E16CB6D"
ORDER        = "3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D"
INBOUND      = "03241AA7-0E85-4C5C-A2CC-688212A79B84"
OUTBOUND     = "90F72F0E-528D-4992-907A-F2C6B37AD9A5"
STAMP        = "AF3E7697-6F7E-4AD8-B76C-E2134DB98747"
MINISTRY     = "4B3EB169-6203-4A71-A3BD-A442FEAAA91F"
RULE         = "4AB1EC00-9E5E-4113-B577-D8ED46BA7728"
````

---

**รายงานจัดทำโดย:** GitHub Copilot  
**วันที่:** 12 มกราคม 2569  
**เวลา:** ตามที่สร้างรายงาน
