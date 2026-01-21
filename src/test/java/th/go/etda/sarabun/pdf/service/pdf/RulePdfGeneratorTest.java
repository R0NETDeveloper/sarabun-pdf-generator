package th.go.etda.sarabun.pdf.service.pdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Unit tests for RulePdfGenerator
 *
 * ทดสอบการสร้าง PDF หนังสือข้อบังคับ
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RulePdfGenerator Tests")
class RulePdfGeneratorTest {

    @Mock
    private MemoPdfGenerator memoPdfGenerator;

    private RulePdfGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        when(memoPdfGenerator.generateMemoPdf(any())).thenReturn(createMockBase64Pdf());
        generator = new RulePdfGenerator(memoPdfGenerator);
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct BookType.RULE")
        void shouldReturnCorrectBookType() {
            assertEquals(BookType.RULE, generator.getBookType());
        }

        @Test
        @DisplayName("Should return correct generator name")
        void shouldReturnCorrectGeneratorName() {
            assertEquals("RulePdfGenerator", generator.getGeneratorName());
        }
    }

    @Nested
    @DisplayName("PDF Generation Tests")
    class PdfGenerationTests {

        @Test
        @DisplayName("Should generate PDF with minimal request data")
        void shouldGeneratePdfWithMinimalRequest() throws Exception {
            // Arrange
            GeneratePdfRequest request = createMinimalRequest();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size()); // 1 rule + 1 memo
        }

        @Test
        @DisplayName("Should generate PDF with document data")
        void shouldGeneratePdfWithDocumentData() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithDocumentData();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("หนังสือข้อบังคับ", results.get(0).getDescription());
            assertEquals("บันทึกข้อความ (สำเนาเก็บ)", results.get(1).getDescription());
        }

        @Test
        @DisplayName("Should generate PDF with edition and year")
        void shouldGeneratePdfWithEditionAndYear() throws Exception {
            // Arrange
            GeneratePdfRequest request = createMinimalRequest();
            GeneratePdfRequest.Document doc = request.getDocument();
            doc.setYear("๑"); // ฉบับที่
            doc.setDateThai("1 มกราคม 2568"); // พ.ศ.

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with signers")
        void shouldGeneratePdfWithSigners() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSigners();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with HTML content")
        void shouldGeneratePdfWithHtmlContent() throws Exception {
            // Arrange
            GeneratePdfRequest request = createMinimalRequest();
            GeneratePdfRequest.Document doc = request.getDocument();
            GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
            bookContent.setContent("<p>ทดสอบ HTML</p>");
            bookContent.setContentType("html");
            doc.setBookContent(bookContent);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null document")
        void shouldHandleNullDocument() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setDocument(null);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should handle empty document")
        void shouldHandleEmptyDocument() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setDocument(new GeneratePdfRequest.Document());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should handle speed layer")
        void shouldHandleSpeedLayer() throws Exception {
            // Arrange
            GeneratePdfRequest request = createMinimalRequest();
            request.getDocument().setSpeedLayer("ด่วนที่สุด");

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
        }
    }

    // ==================== Helper Methods ====================

    private String createMockBase64Pdf() {
        return "JVBERi0xLjQKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNjEyIDc5Ml0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgo+PgplbmRvYmoKeHJlZgowIDQKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDE0NyAwMDAwMCBuIAp0cmFpbGVyCjw8Ci9TaXplIDQKL1Jvb3QgMSAwIFIKPj4Kc3RhcnR4cmVmCjE5OAolJUVPRg==";
    }

    private GeneratePdfRequest createMinimalRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
        doc.setBookNo("ดศ 0001/123");
        doc.setDateThai("1 มกราคม 2568");
        doc.setBookTitle("ทดสอบหนังสือข้อบังคับ");
        doc.setDepartment("คณะกรรมการธุรกรรมทางอิเล็กทรอนิกส์");
        request.setDocument(doc);
        return request;
    }

    private GeneratePdfRequest createRequestWithDocumentData() {
        GeneratePdfRequest request = new GeneratePdfRequest();

        GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
        doc.setBookNo("ดศ 0001/123");
        doc.setDateThai("1 มกราคม 2568");
        doc.setBookTitle("ทดสอบหนังสือข้อบังคับ");
        doc.setDepartment("คณะกรรมการธุรกรรมทางอิเล็กทรอนิกส์");
        doc.setYear("๑"); // ฉบับที่

        GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
        bookContent.setContent("เนื้อหาทดสอบหนังสือข้อบังคับ");
        doc.setBookContent(bookContent);

        request.setDocument(doc);

        return request;
    }

    private GeneratePdfRequest createRequestWithSigners() {
        GeneratePdfRequest request = createRequestWithDocumentData();

        List<GeneratePdfRequest.BookRelate> learners = new ArrayList<>();
        GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
        learner.setPrefixName("นาย");
        learner.setFirstname("ทดสอบ");
        learner.setLastname("ระบบ");
        learner.setPositionName("ประธานกรรมการ");
        learners.add(learner);
        request.setBookLearner(learners);

        return request;
    }
}
