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
 * Unit tests for OutboundPdfGenerator
 *
 * ทดสอบการสร้าง PDF หนังสือส่งออก
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OutboundPdfGenerator Tests")
class OutboundPdfGeneratorTest {

    @Mock
    private MemoPdfGenerator memoPdfGenerator;

    private OutboundPdfGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        when(memoPdfGenerator.generateMemoPdf(any())).thenReturn(createMockBase64Pdf());
        generator = new OutboundPdfGenerator(memoPdfGenerator);
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct BookType.OUTBOUND")
        void shouldReturnCorrectBookType() {
            assertEquals(BookType.OUTBOUND, generator.getBookType());
        }

        @Test
        @DisplayName("Should return correct generator name")
        void shouldReturnCorrectGeneratorName() {
            assertEquals("OutboundPdfGenerator", generator.getGeneratorName());
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
            assertEquals(1, results.size()); // Merged PDF
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
            assertEquals(1, results.size()); // Merged: 1 outbound + 1 memo
            verify(memoPdfGenerator, times(1)).generateMemoPdf(any());
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
        @DisplayName("Should generate PDF with document data")
        void shouldGeneratePdfWithDocumentData() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithDocumentData();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertTrue(results.get(0).getPdfBase64().length() > 0);
        }

        @Test
        @DisplayName("Should generate PDF with attachments")
        void shouldGeneratePdfWithAttachments() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithAttachments();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with referTo data")
        void shouldGeneratePdfWithReferToData() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithReferTo();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should generate PDF with signers (bookLearner)")
        void shouldGeneratePdfWithSigners() throws Exception {
            // Arrange
            GeneratePdfRequest request = createRequestWithSigners();

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    @Nested
    @DisplayName("Recipient Name Building Tests")
    class RecipientNameBuildingTests {

        @Test
        @DisplayName("Should use salutationContent for recipient name")
        void shouldUseSalutationContentForRecipientName() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
            GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
            recipient.setSalutationContent("ท่านปลัดกระทรวงดิจิทัล");
            recipients.add(recipient);
            request.setToRecipients(recipients);
            request.setDocument(createBasicDocument());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
        }

        @Test
        @DisplayName("Should fallback to organizeName when no salutationContent")
        void shouldFallbackToOrganizeName() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
            GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
            recipient.setOrganizeName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
            recipients.add(recipient);
            request.setToRecipients(recipients);
            request.setDocument(createBasicDocument());

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
        @DisplayName("Should handle null toRecipients (fallback mode)")
        void shouldHandleNullToRecipients() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setToRecipients(null);
            request.setDocument(createBasicDocument());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle empty toRecipients (fallback mode)")
        void shouldHandleEmptyToRecipients() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setToRecipients(new ArrayList<>());
            request.setDocument(createBasicDocument());

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should handle HTML content in document")
        void shouldHandleHtmlContentInDocument() throws Exception {
            // Arrange
            GeneratePdfRequest request = new GeneratePdfRequest();

            GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
            GeneratePdfRequest.BookContent bookContent = new GeneratePdfRequest.BookContent();
            bookContent.setContent("<p>ทดสอบ HTML</p>");
            bookContent.setContentType("html");
            doc.setBookContent(bookContent);

            request.setDocument(doc);

            // Act
            List<PdfResult> results = generator.generate(request);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    // ==================== Helper Methods ====================

    private String createMockBase64Pdf() {
        return "JVBERi0xLjQKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNjEyIDc5Ml0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgo+PgplbmRvYmoKeHJlZgowIDQKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDE0NyAwMDAwMCBuIAp0cmFpbGVyCjw8Ci9TaXplIDQKL1Jvb3QgMSAwIFIKPj4Kc3RhcnR4cmVmCjE5OAolJUVPRg==";
    }

    private GeneratePdfRequest createMinimalRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setDocument(createBasicDocument());
        return request;
    }

    private GeneratePdfRequest.Document createBasicDocument() {
        GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
        doc.setBookNo("ดศ 0001/123");
        doc.setDateThai("1 มกราคม 2568");
        doc.setBookTitle("ทดสอบหนังสือส่งออก");
        return doc;
    }

    private GeneratePdfRequest createRequestWithSingleRecipient() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setDocument(createBasicDocument());

        List<GeneratePdfRequest.BookRecipient> recipients = new ArrayList<>();
        GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
        recipient.setOrganizeName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipient.setSalutationContent("ปลัดกระทรวงดิจิทัล");
        recipients.add(recipient);
        request.setToRecipients(recipients);

        return request;
    }

    private GeneratePdfRequest createRequestWithMultipleRecipients() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        request.setDocument(createBasicDocument());

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

    private GeneratePdfRequest createRequestWithDocumentData() {
        GeneratePdfRequest request = new GeneratePdfRequest();

        GeneratePdfRequest.Document doc = new GeneratePdfRequest.Document();
        doc.setBookNo("ดศ 0001/123");
        doc.setAddress("ที่อยู่\nแถวที่ 2");
        doc.setDateThai("1 มกราคม 2568");
        doc.setBookTitle("ทดสอบหนังสือส่งออก");
        doc.setSpeedLayer("ด่วนที่สุด");
        doc.setContact("โทร. 0-2142-1234");
        request.setDocument(doc);

        return request;
    }

    private GeneratePdfRequest createRequestWithAttachments() {
        GeneratePdfRequest request = createMinimalRequest();

        GeneratePdfRequest.Document doc = request.getDocument();
        List<GeneratePdfRequest.DocumentAttachment> attachments = new ArrayList<>();
        GeneratePdfRequest.DocumentAttachment att = new GeneratePdfRequest.DocumentAttachment();
        att.setName("สำเนาหนังสือ");
        att.setRemark("จำนวน 1 ฉบับ");
        attachments.add(att);
        doc.setAttachment(attachments);

        return request;
    }

    private GeneratePdfRequest createRequestWithReferTo() {
        GeneratePdfRequest request = createMinimalRequest();

        GeneratePdfRequest.Document doc = request.getDocument();
        List<GeneratePdfRequest.BookReferTo> referTos = new ArrayList<>();
        GeneratePdfRequest.BookReferTo referTo = new GeneratePdfRequest.BookReferTo();
        referTo.setBookReferToName("หนังสืออ้างถึง");
        referTo.setBookReferToNo("ดศ 0001/100");
        referTos.add(referTo);
        doc.setBookReferTo(referTos);

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
