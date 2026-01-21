package th.go.etda.sarabun.pdf.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
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
 * Unit tests for MemoPdfGenerator
 *
 * ทดสอบการสร้าง PDF บันทึกข้อความ
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemoPdfGenerator Tests")
class MemoPdfGeneratorTest {

    private MemoPdfGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MemoPdfGenerator();
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct BookType.MEMO")
        void shouldReturnCorrectBookType() {
            assertEquals(BookType.MEMO, generator.getBookType());
        }

        @Test
        @DisplayName("Should return correct generator name")
        void shouldReturnCorrectGeneratorName() {
            assertEquals("MemoPdfGenerator", generator.getGeneratorName());
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
            assertEquals(1, results.size());
            assertNotNull(results.get(0).getPdfBase64());
            assertTrue(results.get(0).getPdfBase64().length() > 0);
        }

        @Test
        @DisplayName("Should generate PDF with full request data")
        void shouldGeneratePdfWithFullRequest() throws Exception {
            // Arrange
            GeneratePdfRequest request = createFullRequest();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());

            PdfResult result = results.get(0);
            assertNotNull(result.getPdfBase64());
            assertEquals("หนังสือบันทึกข้อความ", result.getDescription());
        }

        @Test
        @DisplayName("Should generate PDF with memo data")
        void shouldGeneratePdfWithMemoData() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
            memo.setDivisionName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
            memo.setBookNo("ดศ 0001/123");
            memo.setDateThai("1 มกราคม 2568");
            memo.setBookTitle("ทดสอบการสร้างบันทึกข้อความ");
            request.setMemo(memo);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
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
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with HTML content")
        void shouldGeneratePdfWithHtmlContent() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
            memo.setBookTitle("ทดสอบ HTML Content");

            GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
            bookContent.setContent("<p>ทดสอบ HTML content</p><table><tr><td>Col1</td><td>Col2</td></tr></table>");
            bookContent.setContentType("html");
            memo.setBookContent(bookContent);

            request.setMemo(memo);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with speed layer")
        void shouldGeneratePdfWithSpeedLayer() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
            memo.setBookTitle("ทดสอบ Speed Layer");
            memo.setSpeedLayer("ด่วนที่สุด");
            request.setMemo(memo);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null memo gracefully")
        void shouldHandleNullMemo() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setMemo(null);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle empty memo gracefully")
        void shouldHandleEmptyMemo() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setMemo(new GeneratePdfRequest.Memo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle very long content")
        void shouldHandleVeryLongContent() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
            memo.setBookTitle("ทดสอบ Long Content");

            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longContent.append("บรรทัดที่ ").append(i + 1).append(" - ทดสอบเนื้อหายาวมาก\n");
            }

            GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
            bookContent.setContent(longContent.toString());
            memo.setBookContent(bookContent);

            request.setMemo(memo);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    // ==================== Helper Methods ====================

    private GeneratePdfRequest createMinimalRequest() {
        return new GeneratePdfRequest();
    }

    private GeneratePdfRequest createFullRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();

        // Memo
        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setDivisionName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        memo.setDepartment("สพธอ.");
        memo.setBookNo("ดศ 0001/123");
        memo.setDateThai("1 มกราคม 2568");
        memo.setBookTitle("ทดสอบการสร้างบันทึกข้อความ");

        GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
        bookContent.setContent("เนื้อหาทดสอบบันทึกข้อความ");
        memo.setBookContent(bookContent);

        request.setMemo(memo);

        // Book Learner
        List<GeneratePdfRequest.BookRelate> learners = new ArrayList<>();
        GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
        learner.setPositionName("ผู้อำนวยการ");
        learners.add(learner);
        request.setBookLearner(learners);

        return request;
    }

    private GeneratePdfRequest createRequestWithSigners() {
        GeneratePdfRequest request = createFullRequest();

        List<GeneratePdfRequest.BookRelate> signers = new ArrayList<>();
        GeneratePdfRequest.BookRelate signer = new GeneratePdfRequest.BookRelate();
        signer.setPrefixName("นาย");
        signer.setFirstname("ทดสอบ");
        signer.setLastname("ระบบ");
        signer.setPositionName("ผู้อำนวยการ");
        signer.setDepartmentName("สพธอ.");
        signers.add(signer);
        request.setBookSigned(signers);

        return request;
    }
}
