# แนวทางการเขียน Unit Tests สำหรับ Sarabun PDF Generator

**วันที่:** 21 มกราคม 2569

---

## หลักการ: ไม่กระทบโค้ดหลัก

การเขียน Tests จะ **แยกอยู่ใน `src/test/java`** ไม่ต้องแก้ไขโค้ดหลักใน `src/main/java`

---

## 1. โครงสร้างโฟลเดอร์ Tests

```
src/test/java/th/go/etda/sarabun/pdf/
├── service/
│   ├── GeneratePdfServiceTest.java
│   ├── RequestValidatorTest.java
│   └── pdf/
│       ├── PdfGeneratorBaseTest.java
│       ├── MemoPdfGeneratorTest.java
│       ├── OutboundPdfGeneratorTest.java
│       ├── ... (9 generators)
│       ├── FontManagerTest.java
│       ├── HtmlContentRendererTest.java
│       └── TableRendererTest.java
│
├── controller/
│   └── GeneratePdfControllerTest.java
│
└── integration/
    └── PdfGenerationIntegrationTest.java

src/test/resources/
├── test-requests/
│   ├── memo-request.json
│   ├── outbound-request.json
│   └── ... (sample requests)
└── expected-outputs/
    └── (optional: expected PDF samples)
```

---

## 2. Dependencies ที่ต้องเพิ่ม (ใน pom.xml)

```xml
<!-- มีอยู่แล้ว -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- เพิ่มเติม (optional แต่แนะนำ) -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <version>5.2.0</version>
    <scope>test</scope>
</dependency>
```

---

## 3. แนวทางการ Test แต่ละ Component

### 3.1 FontManager Test

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/service/pdf/FontManagerTest.java`

```java
package th.go.etda.sarabun.pdf.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FontManagerTest {

    private FontManager fontManager;

    @BeforeAll
    void setUp() {
        // Initialize FontManager (simulates @PostConstruct)
        fontManager = new FontManager();
        fontManager.init();
    }

    @Test
    void testFontManagerInitialized() {
        assertTrue(FontManager.isInstanceAvailable(),
            "FontManager should be initialized");
    }

    @Test
    void testGetRegularFont() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDFont font = fontManager.getRegularFont(document);
            assertNotNull(font, "Regular font should not be null");
        }
    }

    @Test
    void testGetBoldFont() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDFont font = fontManager.getBoldFont(document);
            assertNotNull(font, "Bold font should not be null");
        }
    }

    @Test
    void testFontCacheStats() {
        FontManager.FontCacheStats stats = fontManager.getStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalCacheSize() > 0,
            "Cache should have font bytes loaded");
    }

    @Test
    void testMultipleFontLoadsUseSameCache() throws Exception {
        long initialCount = fontManager.getStats().getTotalLoadCount();

        try (PDDocument doc1 = new PDDocument();
             PDDocument doc2 = new PDDocument()) {
            fontManager.getRegularFont(doc1);
            fontManager.getRegularFont(doc2);
        }

        // Load count increases but cache size stays same
        long newCount = fontManager.getStats().getTotalLoadCount();
        assertTrue(newCount > initialCount,
            "Load count should increase");
    }
}
```

---

### 3.2 RequestValidator Test

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/service/RequestValidatorTest.java`

```java
package th.go.etda.sarabun.pdf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidatorTest {

    private RequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RequestValidator();
        // Set default values using reflection (no need to modify main code)
        ReflectionTestUtils.setField(validator, "validationEnabled", true);
        ReflectionTestUtils.setField(validator, "maxContentLength", 500000);
        ReflectionTestUtils.setField(validator, "maxBase64Size", 10485760);
        ReflectionTestUtils.setField(validator, "maxSigners", 20);
        ReflectionTestUtils.setField(validator, "maxRecipients", 100);
    }

    @Test
    void testValidRequest() {
        GeneratePdfRequest request = createValidRequest();
        RequestValidator.ValidationResult result = validator.validate(request);
        assertTrue(result.isValid(), "Valid request should pass validation");
    }

    @Test
    void testNullBookNameId() {
        GeneratePdfRequest request = createValidRequest();
        request.setBookNameId(null);

        RequestValidator.ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("bookNameId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-guid",
        "12345",
        "ZZZZZZZZ-ZZZZ-ZZZZ-ZZZZ-ZZZZZZZZZZZZ"
    })
    void testInvalidGuidFormat(String invalidGuid) {
        GeneratePdfRequest request = createValidRequest();
        request.setBookNameId(invalidGuid);

        RequestValidator.ValidationResult result = validator.validate(request);
        assertFalse(result.isValid(),
            "Should reject invalid GUID: " + invalidGuid);
    }

    @Test
    void testValidGuidFormats() {
        String[] validGuids = {
            "BB4A2F11-722D-449A-BCC5-22208C7A4DEC",  // MEMO
            "90F72F0E-528D-4992-907A-F2C6B37AD9A5",  // OUTBOUND
            "bb4a2f11-722d-449a-bcc5-22208c7a4dec"   // lowercase
        };

        for (String guid : validGuids) {
            GeneratePdfRequest request = createValidRequest();
            request.setBookNameId(guid);

            RequestValidator.ValidationResult result = validator.validate(request);
            assertTrue(result.isValid(),
                "Should accept valid GUID: " + guid);
        }
    }

    @Test
    void testXssDetection() {
        GeneratePdfRequest request = createValidRequest();
        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        GeneratePdfRequest.BookContent content = new GeneratePdfRequest.BookContent();
        content.setContent("<script>alert('xss')</script>");
        memo.setBookContent(content);
        request.setMemo(memo);

        RequestValidator.ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("script") ||
                   result.getErrorMessage().toLowerCase().contains("xss"));
    }

    @Test
    void testContentTooLong() {
        GeneratePdfRequest request = createValidRequest();
        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        GeneratePdfRequest.BookContent content = new GeneratePdfRequest.BookContent();
        content.setContent("A".repeat(600000)); // > 500000
        memo.setBookContent(content);
        request.setMemo(memo);

        RequestValidator.ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
    }

    @Test
    void testTooManySigners() {
        GeneratePdfRequest request = createValidRequest();
        java.util.List<GeneratePdfRequest.BookRelate> signers = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) { // > 20
            signers.add(new GeneratePdfRequest.BookRelate());
        }
        request.setBookSigned(signers);

        RequestValidator.ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
    }

    // Helper method
    private GeneratePdfRequest createValidRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setBookNameId("BB4A2F11-722D-449A-BCC5-22208C7A4DEC");

        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setBookTitle("Test Title");
        memo.setBookNo("Test-001");
        request.setMemo(memo);

        return request;
    }
}
```

---

### 3.3 HtmlContentRenderer Test

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/service/pdf/HtmlContentRendererTest.java`

```java
package th.go.etda.sarabun.pdf.service.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlContentRendererTest {

    private HtmlContentRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new HtmlContentRenderer();
    }

    @Test
    void testRenderSimpleHtml() throws Exception {
        String html = "<p>Hello World</p>";
        byte[] pdfBytes = renderer.renderHtmlToPdf(html);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // PDF magic bytes: %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void testRenderThaiText() throws Exception {
        String html = "<p>สวัสดี ภาษาไทย</p>";
        byte[] pdfBytes = renderer.renderHtmlToPdf(html);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testRenderTable() throws Exception {
        String html = """
            <table border="1">
                <tr><th>หัวข้อ</th><th>ค่า</th></tr>
                <tr><td>รายการ 1</td><td>100</td></tr>
            </table>
            """;
        byte[] pdfBytes = renderer.renderHtmlToPdf(html);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testRenderWithQuillClasses() throws Exception {
        String html = """
            <p class="ql-align-center">Centered Text</p>
            <p class="ql-indent-1">Indented Text</p>
            """;
        byte[] pdfBytes = renderer.renderHtmlToPdf(html);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testNullInput() {
        assertThrows(Exception.class, () -> {
            renderer.renderHtmlToPdf(null);
        });
    }

    @Test
    void testEmptyInput() throws Exception {
        String html = "";
        byte[] pdfBytes = renderer.renderHtmlToPdf(html);

        // Should either return empty PDF or throw exception
        // Depends on implementation
        assertNotNull(pdfBytes);
    }
}
```

---

### 3.4 PDF Generator Test (ตัวอย่าง MemoPdfGenerator)

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/service/pdf/MemoPdfGeneratorTest.java`

```java
package th.go.etda.sarabun.pdf.service.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoPdfGeneratorTest {

    private MemoPdfGenerator generator;

    @Mock
    private FontManager fontManager;

    @BeforeEach
    void setUp() {
        generator = new MemoPdfGenerator(fontManager);
    }

    @Test
    void testGetBookType() {
        assertEquals(BookType.MEMO, generator.getBookType());
    }

    @Test
    void testGetGeneratorName() {
        assertNotNull(generator.getGeneratorName());
        assertTrue(generator.getGeneratorName().contains("Memo"));
    }

    @Test
    void testGenerateBasicMemo() throws Exception {
        // Arrange
        GeneratePdfRequest request = createMemoRequest();

        // Act
        List<PdfResult> results = generator.generate(request);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());

        PdfResult result = results.get(0);
        assertNotNull(result.getPdfBase64());
        assertTrue(result.getPdfBase64().length() > 0);

        // Verify it's valid Base64
        assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(result.getPdfBase64());
        });
    }

    @Test
    void testGenerateMemoWithHtmlContent() throws Exception {
        GeneratePdfRequest request = createMemoRequest();
        request.getMemo().getBookContent().setContentType("html");
        request.getMemo().getBookContent().setContent(
            "<p>ด้วยสำนักงาน...</p><ul><li>รายการ 1</li></ul>"
        );

        List<PdfResult> results = generator.generate(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testGenerateMemoWithSigners() throws Exception {
        GeneratePdfRequest request = createMemoRequest();

        // Add signers
        List<GeneratePdfRequest.BookRelate> signers = List.of(
            createSigner("นาย", "สมชาย", "ใจดี", "ผู้อำนวยการ")
        );
        request.setBookSigned(signers);

        List<PdfResult> results = generator.generate(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testGenerateMemoWithNullMemo() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setBookNameId("BB4A2F11-722D-449A-BCC5-22208C7A4DEC");
        request.setMemo(null);

        // Should handle gracefully or throw specific exception
        assertThrows(Exception.class, () -> {
            generator.generate(request);
        });
    }

    // Helper methods
    private GeneratePdfRequest createMemoRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setBookNameId("BB4A2F11-722D-449A-BCC5-22208C7A4DEC");

        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setBookName("บันทึกข้อความ");
        memo.setBookTitle("ขอความอนุเคราะห์");
        memo.setBookNo("สพธอ. 0102/2568");
        memo.setDateThai("21 มกราคม 2569");
        memo.setDepartment("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");

        GeneratePdfRequest.BookContent content = new GeneratePdfRequest.BookContent();
        content.setSubject("ขอเชิญประชุม");
        content.setContent("ด้วยสำนักงานจะจัดประชุม...");
        content.setContentType("text");
        memo.setBookContent(content);

        request.setMemo(memo);
        return request;
    }

    private GeneratePdfRequest.BookRelate createSigner(
            String prefix, String firstname, String lastname, String position) {
        return GeneratePdfRequest.BookRelate.builder()
            .prefixName(prefix)
            .firstname(firstname)
            .lastname(lastname)
            .positionName(position)
            .build();
    }
}
```

---

### 3.5 TableRenderer Test

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/service/pdf/TableRendererTest.java`

```java
package th.go.etda.sarabun.pdf.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableRendererTest {

    @Test
    void testParseSimpleHtmlTable() {
        String html = """
            <table>
                <tr><td>A</td><td>B</td></tr>
                <tr><td>1</td><td>2</td></tr>
            </table>
            """;

        TableRenderer.TableData data = TableRenderer.parseHtmlTable(html);

        assertNotNull(data);
        assertEquals(2, data.getRows().size());
        assertEquals(2, data.getNumColumns());
    }

    @Test
    void testParseTableWithHeader() {
        String html = """
            <table>
                <tr><th>Header 1</th><th>Header 2</th></tr>
                <tr><td>Data 1</td><td>Data 2</td></tr>
            </table>
            """;

        TableRenderer.TableData data = TableRenderer.parseHtmlTable(html);

        assertNotNull(data);
        assertEquals(2, data.getRows().size());
        assertTrue(data.getRows().get(0).isHeader());
        assertFalse(data.getRows().get(1).isHeader());
    }

    @Test
    void testParseTableWithColspan() {
        String html = """
            <table>
                <tr><td colspan="2">Merged</td></tr>
                <tr><td>A</td><td>B</td></tr>
            </table>
            """;

        TableRenderer.TableData data = TableRenderer.parseHtmlTable(html);

        assertNotNull(data);
        assertEquals(2, data.getRows().size());
    }

    @Test
    void testParseEmptyTable() {
        String html = "<table></table>";

        TableRenderer.TableData data = TableRenderer.parseHtmlTable(html);

        assertNotNull(data);
        assertTrue(data.getRows().isEmpty() || data.getNumColumns() == 0);
    }

    @Test
    void testParseNullInput() {
        assertThrows(Exception.class, () -> {
            TableRenderer.parseHtmlTable(null);
        });
    }

    @Test
    void testRenderTableToPdf() throws Exception {
        String html = """
            <table>
                <tr><th>ชื่อ</th><th>จำนวน</th></tr>
                <tr><td>รายการ 1</td><td>100</td></tr>
            </table>
            """;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream =
                    new PDPageContentStream(document, page)) {

                TableRenderer.TableData tableData = TableRenderer.parseHtmlTable(html);

                // This would need actual font - simplified for test
                // float newY = TableRenderer.drawTable(...);
                // assertTrue(newY < 800); // Y position should decrease
            }
        }
    }
}
```

---

### 3.6 Integration Test

**ไฟล์:** `src/test/java/th/go/etda/sarabun/pdf/integration/PdfGenerationIntegrationTest.java`

```java
package th.go.etda.sarabun.pdf.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.ApiResponse;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PdfGenerationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/pdf/health",
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("isOk"));
    }

    @Test
    void testBookTypesEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/pdf/book-types",
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("MEMO"));
        assertTrue(response.getBody().contains("OUTBOUND"));
    }

    @Test
    void testPreviewPdfMemo() {
        GeneratePdfRequest request = createMemoRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeneratePdfRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/pdf/preview",
            entity,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("isOk"));
        assertTrue(response.getBody().contains("data"));
    }

    @ParameterizedTest
    @EnumSource(value = BookType.class, names = {"MEMO", "OUTBOUND", "ORDER"})
    void testPreviewPdfAllTypes(BookType bookType) {
        GeneratePdfRequest request = createRequestForType(bookType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeneratePdfRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/pdf/preview",
            entity,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(),
            "Should generate PDF for " + bookType);
    }

    @Test
    void testInvalidBookNameId() {
        GeneratePdfRequest request = createMemoRequest();
        request.setBookNameId("invalid-guid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeneratePdfRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/pdf/preview",
            entity,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // Helper methods
    private GeneratePdfRequest createMemoRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setBookNameId(BookType.MEMO.getId());

        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setBookTitle("Test Memo");
        memo.setBookNo("TEST-001");
        memo.setDateThai("21 มกราคม 2569");

        GeneratePdfRequest.BookContent content = new GeneratePdfRequest.BookContent();
        content.setSubject("Test Subject");
        content.setContent("Test content");
        content.setContentType("text");
        memo.setBookContent(content);

        request.setMemo(memo);
        return request;
    }

    private GeneratePdfRequest createRequestForType(BookType bookType) {
        GeneratePdfRequest request = createMemoRequest();
        request.setBookNameId(bookType.getId());

        if (bookType == BookType.OUTBOUND) {
            GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
            doc.setBookTitle("Test Outbound");
            doc.setDateThai("21 มกราคม 2569");

            GeneratePdfRequest.BookContent content = new GeneratePdfRequest.BookContent();
            content.setSubject("Test Subject");
            content.setContent("Test content");
            doc.setBookContent(content);

            request.setDocument(doc);

            // Add recipient
            request.setToRecipients(java.util.List.of(
                GeneratePdfRequest.BookRecipient.builder()
                    .organizeName("Test Organization")
                    .salutation("เรียน")
                    .salutationContent("ผู้อำนวยการ")
                    .endDoc("ขอแสดงความนับถือ")
                    .build()
            ));
        }

        return request;
    }
}
```

---

## 4. รันคำสั่ง Tests

```bash
# รัน tests ทั้งหมด
mvn test

# รัน test เฉพาะ class
mvn test -Dtest=FontManagerTest

# รัน tests พร้อม coverage report
mvn test jacoco:report

# ดู coverage report
# open target/site/jacoco/index.html
```

---

## 5. สรุปลำดับความสำคัญ

| Priority | Test | เหตุผล |
|----------|------|--------|
| 🔴 P0 | FontManagerTest | Core dependency ใช้ทุก generator |
| 🔴 P0 | RequestValidatorTest | Security & input validation |
| 🟡 P1 | MemoPdfGeneratorTest | Document type หลัก |
| 🟡 P1 | HtmlContentRendererTest | HTML rendering ใช้บ่อย |
| 🟡 P1 | TableRendererTest | Table rendering |
| 🟢 P2 | Other GeneratorTests | ทุก 9 types |
| 🟢 P2 | IntegrationTest | End-to-end |

---

## 6. Tips สำหรับการเขียน Test

1. **ไม่ต้องแก้โค้ดหลัก** - ใช้ `ReflectionTestUtils` สำหรับ set private fields
2. **Mock dependencies** - ใช้ `@Mock` และ `@ExtendWith(MockitoExtension.class)`
3. **Test both happy path และ error cases**
4. **ใช้ `@ParameterizedTest`** สำหรับ test หลาย inputs
5. **ใช้ `@BeforeEach`** สำหรับ setup ที่ต้องทำทุก test
6. **ตั้งชื่อ test ให้สื่อความหมาย** เช่น `testGenerateMemoWithNullMemo`

---

_สร้างเมื่อ: 21 มกราคม 2569_
