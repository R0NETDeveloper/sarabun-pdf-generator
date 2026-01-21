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
 * Unit tests for MinistryPdfGenerator
 *
 * ทดสอบการสร้าง PDF หนังสือภายใต้กระทรวง
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MinistryPdfGenerator Tests")
class MinistryPdfGeneratorTest {

    @Mock
    private MemoPdfGenerator memoPdfGenerator;

    private MinistryPdfGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        when(memoPdfGenerator.generateMemoPdf(any())).thenReturn(createMockBase64Pdf());
        generator = new MinistryPdfGenerator(memoPdfGenerator);
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct BookType.MINISTRY")
        void shouldReturnCorrectBookType() {
            assertEquals(BookType.MINISTRY, generator.getBookType());
        }

        @Test
        @DisplayName("Should return correct generator name")
        void shouldReturnCorrectGeneratorName() {
            assertEquals("MinistryPdfGenerator", generator.getGeneratorName());
        }
    }

    @Nested
    @DisplayName("PDF Generation Tests")
    class PdfGenerationTests {

        @Test
        @DisplayName("Should generate PDF with minimal request data (fallback to memo)")
        void shouldGeneratePdfWithMinimalRequest() throws Exception {
            // Arrange
            GeneratePdfRequest request = createMinimalRequest();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size()); // ใช้ memo generator
        }

        @Test
        @DisplayName("Should generate PDF with single recipient")
        void shouldGeneratePdfWithSingleRecipient() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSingleRecipient();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size()); // Merged: 1 ministry + 1 memo
        }

        @Test
        @DisplayName("Should generate PDF with multiple recipients")
        void shouldGeneratePdfWithMultipleRecipients() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithMultipleRecipients();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size()); // Merged PDF
        }

        @Test
        @DisplayName("Should generate PDF with memo data")
        void shouldGeneratePdfWithMemoData() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithMemoData();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
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
        }

        @Test
        @DisplayName("Should generate PDF with endDoc from recipient")
        void shouldGeneratePdfWithEndDocFromRecipient() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSingleRecipient();
            request.getToRecipients().get(0).setEndDoc("ขอแสดงความนับถือ");

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("Recipient Name Building Tests")
    class RecipientNameBuildingTests {

        @Test
        @DisplayName("Should use organizeName for recipient name")
        void shouldUseOrganizeNameForRecipientName() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
            GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
            recipient.setOrganizeName("กระทรวงดิจิทัล");
            recipients.add(recipient);
            request.setToRecipients(recipients);
            request.setMemo(createBasicMemo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }

        @Test
        @DisplayName("Should fallback to departmentName when no organizeName")
        void shouldFallbackToDepartmentName() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
            GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
            recipient.setDepartmentName("สำนักงานปลัดกระทรวง");
            recipients.add(recipient);
            request.setToRecipients(recipients);
            request.setMemo(createBasicMemo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }

        @Test
        @DisplayName("Should use salutation and salutationContent")
        void shouldUseSalutationAndSalutationContent() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
            GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
            recipient.setSalutation("เรียน");
            recipient.setSalutationContent("ท่านปลัดกระทรวง");
            recipients.add(recipient);
            request.setToRecipients(recipients);
            request.setMemo(createBasicMemo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null toRecipients (fallback to memo)")
        void shouldHandleNullToRecipients() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setToRecipients(null);
            request.setMemo(createBasicMemo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle empty toRecipients (fallback to memo)")
        void shouldHandleEmptyToRecipients() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setToRecipients(new ArrayList<>());
            request.setMemo(createBasicMemo());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle HTML content in memo")
        void shouldHandleHtmlContentInMemo() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSingleRecipient();
            GeneratePdfRequest.Memo memo = request.getMemo();
            GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
            bookContent.setContent("<p>ทดสอบ HTML</p>");
            bookContent.setContentType("html");
            memo.setBookContent(bookContent);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }

        @Test
        @DisplayName("Should handle speed layer")
        void shouldHandleSpeedLayer() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSingleRecipient();
            request.getMemo().setSpeedLayer("ด่วนที่สุด");

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }
    }

    // ==================== Helper Methods ====================

    private String createMockBase64Pdf() {
        return "JVBERi0xLjQKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNjEyIDc5Ml0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgo+PgplbmRvYmoKeHJlZgowIDQKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDE0NyAwMDAwMCBuIAp0cmFpbGVyCjw8Ci9TaXplIDQKL1Jvb3QgMSAwIFIKPj4Kc3RhcnR4cmVmCjE5OAolJUVPRg==";
    }

    private GeneratePdfRequest.Memo createBasicMemo() {
        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setDivisionName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        memo.setBookNo("ดศ 0001/123");
        memo.setDateThai("1 มกราคม 2568");
        memo.setBookTitle("ทดสอบหนังสือภายใต้กระทรวง");
        return memo;
    }

    private GeneratePdfRequest createMinimalRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setMemo(createBasicMemo());
        return request;
    }

    private GeneratePdfRequest createRequestWithSingleRecipient() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setMemo(createBasicMemo());

        List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
        GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
        recipient.setOrganizeName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipients.add(recipient);
        request.setToRecipients(recipients);

        return request;
    }

    private GeneratePdfRequest createRequestWithMultipleRecipients() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setMemo(createBasicMemo());

        List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();

        GeneratePdfRequest.BookRecipient recipient1 = new GeneratePdfRequest.BookRecipient();
        recipient1.setOrganizeName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipients.add(recipient1);

        GeneratePdfRequest.BookRecipient recipient2 = new GeneratePdfRequest.BookRecipient();
        recipient2.setOrganizeName("สำนักงานปลัดกระทรวง");
        recipients.add(recipient2);

        request.setToRecipients(recipients);

        return request;
    }

    private GeneratePdfRequest createRequestWithMemoData() {
        GeneratePdfRequest request = new GeneratePdfRequest();

        GeneratePdfRequest.Memo memo = new GeneratePdfRequest.Memo();
        memo.setDivisionName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        memo.setDepartment("สพธอ.");
        memo.setBookNo("ดศ 0001/123");
        memo.setDateThai("1 มกราคม 2568");
        memo.setBookTitle("ทดสอบหนังสือภายใต้กระทรวง");
        memo.setSpeedLayer("ด่วนที่สุด");

        GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
        bookContent.setContent("เนื้อหาทดสอบ");
        memo.setBookContent(bookContent);

        request.setMemo(memo);

        return request;
    }

    private GeneratePdfRequest createRequestWithSigners() {
        GeneratePdfRequest request = createRequestWithSingleRecipient();

        List<GeneratePdfRequest.BookRelate> learners = new ArrayList<>();
        GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
        learner.setPrefixName("นาย");
        learner.setFirstname("ทดสอบ");
        learner.setLastname("ระบบ");
        learner.setPositionName("ผู้อำนวยการ");
        learners.add(learner);
        request.setBookLearner(learners);

        return request;
    }
}
