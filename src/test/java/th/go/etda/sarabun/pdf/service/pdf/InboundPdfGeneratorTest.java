package th.go.etda.sarabun.pdf.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Unit tests for InboundPdfGenerator
 *
 * ทดสอบการสร้าง PDF หนังสือรับเข้า
 *
 * หมายเหตุ: หนังสือรับเข้าต้องมี base64Pdf เป็นเอกสารตั้งต้น (บังคับ)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InboundPdfGenerator Tests")
class InboundPdfGeneratorTest {

    private InboundPdfGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new InboundPdfGenerator();
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct BookType.INBOUND")
        void shouldReturnCorrectBookType() {
            assertEquals(BookType.INBOUND, generator.getBookType());
        }

        @Test
        @DisplayName("Should return correct generator name")
        void shouldReturnCorrectGeneratorName() {
            assertEquals("InboundPdfGenerator", generator.getGeneratorName());
        }
    }

    @Nested
    @DisplayName("PDF Generation Tests")
    class PdfGenerationTests {

        @Test
        @DisplayName("Should generate PDF with valid base64Pdf")
        void shouldGeneratePdfWithValidBase64Pdf() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithValidPdf();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("หนังสือรับเข้า", results.get(0).getDescription());
        }

        @Test
        @DisplayName("Should generate PDF with learner pages")
        void shouldGeneratePdfWithLearnerPages() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithLearners();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle base64Pdf with data URI prefix")
        void shouldHandleBase64PdfWithDataUriPrefix() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            String pdfContent = createMinimalPdfBase64();
            request.setBase64Pdf("data:application/pdf;base64," + pdfContent);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when base64Pdf is null")
        void shouldThrowExceptionWhenBase64PdfIsNull() {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setBase64Pdf(null);

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                generator.generate(request);
            });

            assertTrue(exception.getMessage().contains("base64Pdf"));
        }

        @Test
        @DisplayName("Should throw exception when base64Pdf is empty")
        void shouldThrowExceptionWhenBase64PdfIsEmpty() {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setBase64Pdf("");

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                generator.generate(request);
            });

            assertTrue(exception.getMessage().contains("base64Pdf"));
        }

        @Test
        @DisplayName("Should throw exception when base64Pdf is too short")
        void shouldThrowExceptionWhenBase64PdfIsTooShort() {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setBase64Pdf("abc123"); // Less than 100 chars

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                generator.generate(request);
            });

            assertTrue(exception.getMessage().contains("ขนาดน้อยเกินไป") ||
                      exception.getMessage().contains("base64Pdf"));
        }

        @Test
        @DisplayName("Should throw exception for invalid base64")
        void shouldThrowExceptionForInvalidBase64() {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            // Valid length but invalid base64
            request.setBase64Pdf("This is not valid base64 content and it should be at least 100 characters long to pass the first check! Adding more text.");

            // Act & Assert
            assertThrows(Exception.class, () -> {
                generator.generate(request);
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle request without learners")
        void shouldHandleRequestWithoutLearners() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithValidPdf();
            request.setBookLearner(null);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle empty learners list")
        void shouldHandleEmptyLearnersList() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithValidPdf();
            request.setBookLearner(new ArrayList<>());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should use bookNo from memo if present")
        void shouldUseBookNoFromMemo() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithValidPdf();
            GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
            memo.setBookNo("ดศ 0001/123");
            request.setMemo(memo);

            // Add learners to trigger learner page generation
            List<GeneratePdfRequest.BookRelate> learners = new ArrayList<>();
            GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
            learner.setPositionName("ผู้อำนวยการ");
            learners.add(learner);
            request.setBookLearner(learners);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    // ==================== Helper Methods ====================

    private String createMinimalPdfBase64() {
        // Minimal valid PDF
        String minimalPdf = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 /MediaBox [0 0 612 792] >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R >>\n" +
                "endobj\n" +
                "xref\n" +
                "0 4\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000058 00000 n \n" +
                "0000000147 00000 n \n" +
                "trailer\n" +
                "<< /Size 4 /Root 1 0 R >>\n" +
                "startxref\n" +
                "198\n" +
                "%%EOF";
        return Base64.getEncoder().encodeToString(minimalPdf.getBytes());
    }

    private GeneratePdfRequest createRequestWithValidPdf() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setBase64Pdf(createMinimalPdfBase64());
        return request;
    }

    private GeneratePdfRequest createRequestWithLearners() {
        GeneratePdfRequest request = createRequestWithValidPdf();

        List<GeneratePdfRequest.BookRelate> learners = new ArrayList<>();
        GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
        learner.setPrefixName("นาย");
        learner.setFirstname("ทดสอบ");
        learner.setLastname("ระบบ");
        learner.setPositionName("ผู้อำนวยการ");
        learner.setDepartmentName("สพธอ.");
        learners.add(learner);
        request.setBookLearner(learners);

        return request;
    }
}
