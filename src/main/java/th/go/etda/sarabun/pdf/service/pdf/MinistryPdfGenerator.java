package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.constant.SignBoxType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * Generator สำหรับ หนังสือภายใต้กระทรวง (Ministry)
 * 
 * BookNameId: 4B3EB169-6203-4A71-A3BD-A442FEAAA91F
 * 
 * โครงสร้างเอกสาร: คล้ายบันทึกข้อความ
 * - โลโก้ ETDA
 * - หัวข้อ "บันทึกข้อความ"
 * - ส่วนราชการ
 * - ที่ + วันที่
 * - เรื่อง
 * - เรียน
 * - เนื้อหา
 * - ลายเซ็น
 * 
 * ใช้ MemoPdfGenerator เพราะรูปแบบคล้ายกัน
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinistryPdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    private final HtmlContentRenderer htmlContentRenderer;
    
    @Override
    public BookType getBookType() {
        return BookType.MINISTRY;
    }
    
    @Override
    public String getGeneratorName() {
        return "MinistryPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<String> allPdfsToMerge = new ArrayList<>();
        List<String> recipientDescriptions = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือภายใต้กระทรวง แยกตามผู้รับแต่ละหน่วยงาน (ใช้ bookRecipients)
        if (request.getBookRecipients() != null && !request.getBookRecipients().isEmpty()) {
            for (int i = 0; i < request.getBookRecipients().size(); i++) {
                GeneratePdfRequest.BookRecipient recipient = request.getBookRecipients().get(i);
                
                // สร้าง PDF สำหรับผู้รับแต่ละหน่วยงาน (documentIndex = i+1)
                String ministryPdfBase64 = generateMinistryPdfForRecipient(request, recipient, i + 1);
                allPdfsToMerge.add(ministryPdfBase64);
                
                String recipientName = buildRecipientName(recipient);
                recipientDescriptions.add("หนังสือภายใต้กระทรวง ถึง " + recipientName);
                
                log.info("Generated ministry PDF {} for: {}", i + 1, recipientName);
            }
            
            // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ) - แค่ 1 ฉบับ (เฉพาะเมื่อมี bookRecipients)
            String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
            allPdfsToMerge.add(memoPdfBase64);
            recipientDescriptions.add("บันทึกข้อความ (สำเนาเก็บ)");
        } else {
            // Fallback: ถ้าไม่มี bookRecipients ใช้ MemoPdfGenerator (แค่ 1 ฉบับ ไม่ต้องสำเนาเก็บ)
            String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
            allPdfsToMerge.add(memoPdfBase64);
            recipientDescriptions.add("หนังสือภายใต้กระทรวง");
        }
        
        log.info("Total PDFs to merge: {} ({} ministry + 1 memo)", 
                allPdfsToMerge.size(), allPdfsToMerge.size() - 1);
        
        // 3. รวมทุก PDF เป็น 1 ไฟล์
        String mergedPdfBase64 = mergePdfFiles(allPdfsToMerge);
        
        // สร้าง description สรุป
        String description = String.join(" + ", recipientDescriptions);
        
        log.info("Merged PDF created successfully: {}", description);
        
        // คืน PdfResult เดียว (รวมแล้ว)
        List<PdfResult> results = new ArrayList<>();
        results.add(createMainPdfResult(mergedPdfBase64, description));
        
        return results;
    }
    
    /**
     * สร้างชื่อผู้รับจาก BookRecipient
     */
    private String buildRecipientName(GeneratePdfRequest.BookRecipient recipient) {
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            return recipient.getOrganizeName();
        }
        if (recipient.getDivisionName() != null && !recipient.getDivisionName().isEmpty()) {
            return recipient.getDivisionName();
        }
        if (recipient.getDepartmentName() != null && !recipient.getDepartmentName().isEmpty()) {
            return recipient.getDepartmentName();
        }
        if (recipient.getMinistryName() != null && !recipient.getMinistryName().isEmpty()) {
            return recipient.getMinistryName();
        }
        return "ผู้รับ";
    }
    
    /**
     * สร้าง PDF หนังสือภายใต้กระทรวงสำหรับผู้รับเฉพาะราย (หน่วยงานภายนอก)
     * โครงสร้างคล้าย Memo แต่หัวเปลี่ยนเป็น "หนังสือภายใต้กระทรวง"
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateMinistryPdfForRecipient(GeneratePdfRequest request, 
                                                    GeneratePdfRequest.BookRecipient recipient,
                                                    int documentIndex) throws Exception {
        log.info("Generating ministry for recipient: {}, docIndex: {}", recipient.getOrganizeName(), documentIndex);
        
        // รวบรวมข้อมูล
        String govName = request.getDivisionName() != null ? request.getDivisionName() : 
                        (request.getDepartment() != null ? request.getDepartment() : "");
        String dateThai = request.getDateThai();
        String title = request.getBookTitle() != null ? request.getBookTitle() : "";
        String bookNo = request.getBookNo();
        
        // ใช้ salutation + salutationContent จาก recipient (ถ้ามี)
        String recipients = "";
        if (recipient.getSalutation() != null && !recipient.getSalutation().isEmpty()) {
            recipients = recipient.getSalutation();
            if (recipient.getSalutationContent() != null && !recipient.getSalutationContent().isEmpty()) {
                recipients += " " + recipient.getSalutationContent();
            }
        } else if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            recipients = recipient.getOrganizeName();
        } else if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            recipients = request.getRecipients();
        } else if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            recipients = request.getBookLearner().stream()
                .map(l -> l.getPositionName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining("\n"));
        }
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // ตรวจสอบและรวบรวม HTML content (bookContent เป็น Object ไม่ใช่ Array)
        String htmlContent = null;
        if (request.getDocumentSub() != null && 
            request.getDocumentSub().getBookContent() != null &&
            hasHtmlContent(request.getDocumentSub().getBookContent())) {
            htmlContent = buildHtmlContent(request.getDocumentSub().getBookContent());
        }
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        // ใช้ endDoc จาก recipient (ถ้ามี) มิเช่นนั้นใช้จาก request
        String endDoc = (recipient.getEndDoc() != null && !recipient.getEndDoc().isEmpty()) 
                        ? recipient.getEndDoc() 
                        : request.getEndDoc();
        
        log.info("Generating ministry - govName: {}, recipients: {}, endDoc: {}, hasHtml: {}, docIndex: {}", 
                govName, recipients, endDoc, htmlContent != null, documentIndex);
        
        return generateMinistryPdfInternal(govName, dateThai, bookNo, title, recipients, content, htmlContent,
                                          request.getSpeedLayer(), signers, endDoc, documentIndex);
    }
    
    /**
     * สร้าง PDF หนังสือภายใต้กระทรวง (internal)
     * โครงสร้างคล้าย Memo แต่หัวเปลี่ยนเป็น "หนังสือภายใต้กระทรวง"
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateMinistryPdfInternal(String govName,
                                               String date,
                                               String bookNo,
                                               String title,
                                               String recipients,
                                               String content,
                                               String htmlContent,
                                               String speedLayer,
                                               List<SignerInfo> signers,
                                               String endDoc,
                                               int documentIndex) throws Exception {
        log.info("=== Generating ministry PDF internal, hasHtml: {} ===", htmlContent != null && !htmlContent.isEmpty());
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // โหลด fonts
            PDFont fontRegular = loadRegularFont(document);
            PDFont fontBold = loadBoldFont(document);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาด debug borders
                drawDebugBorders(contentStream);
                
                // SECTION 0: Logo ETDA (ซ้ายบน)
                drawLogo(contentStream, document, yPosition, LogoPosition.LEFT);
                
                // SECTION 0.5: Speed Layer (ด่วนที่สุด) - มุมขวาบน สีแดง
                drawSpeedLayer(contentStream, speedLayer, fontBold, yPosition, LogoPosition.RIGHT);
                
                yPosition -= LOGO_SPACING;
                
                // SECTION 1: หัวข้อ "หนังสือภายใต้กระทรวง" (แทน "บันทึกข้อความ")
                yPosition = drawCenteredText(contentStream, "หนังสือภายใต้กระทรวง", 
                                            fontBold, FONT_SIZE_HEADER, yPosition);
                yPosition -= SPACING_AFTER_HEADER;
                
                // SECTION 2: ส่วนราชการ
                if (govName != null && !govName.isEmpty()) {
                    yPosition = drawFieldWithUnderline(contentStream, "ส่วนราชการ", govName, 
                                                 fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                                 MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // SECTION 3: ที่ และ วันที่ (ในบรรทัดเดียวกัน)
                float fieldStartY = yPosition;
                
                // "ที่" ทางซ้าย
                float maxUnderlineForRef = DATE_X_POSITION - 20;
                yPosition = drawFieldWithUnderlineCustomWidth(contentStream, "ที่", bookNo != null ? bookNo : "", 
                                   fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                   MARGIN_LEFT, yPosition, maxUnderlineForRef);
                
                // "วันที่" ทางขวา
                if (date != null && !date.isEmpty()) {
                    float maxUnderlineForDate = PAGE_WIDTH - MARGIN_RIGHT;
                    drawFieldWithUnderlineCustomWidth(contentStream, "วันที่", date, 
                            fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                            DATE_X_POSITION, fieldStartY, maxUnderlineForDate);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 4: เรื่อง
                if (title != null && !title.isEmpty()) {
                    yPosition = drawFieldWithUnderline(contentStream, "เรื่อง", title, 
                                                 fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                                 MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // SECTION 5: เรียน
                if (recipients != null && !recipients.isEmpty()) {
                    String recipientsText = "เรียน  " + recipients;
                    yPosition = drawMultilineTextWithIndent(contentStream, recipientsText, 
                                        fontRegular, FONT_SIZE_FIELD_VALUE, 
                                        MARGIN_LEFT, yPosition,
                                        PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT,
                                        "เรียน  ");
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // SECTION 6: เนื้อหา (รองรับทั้ง plain text และ HTML table inline)
                PDPage currentPage = page; // track หน้าปัจจุบัน
                
                // ตรวจสอบว่ามี HTML content หรือไม่
                boolean hasHtml = htmlContent != null && !htmlContent.isEmpty();
                
                // วาด plain text content (เฉพาะกรณีที่ไม่มี HTML content)
                if (!hasHtml && content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    String[] lines = content.split("\n");
                    
                    for (String line : lines) {
                        if (yPosition < MIN_Y_POSITION) {
                            contentStream.close();
                            
                            currentPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, currentPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        yPosition = drawMultilineText(contentStream, line, 
                                                    fontRegular, FONT_SIZE_CONTENT, 
                                                    MARGIN_LEFT, yPosition, 
                                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                    }
                }
                
                // SECTION 6.5: วาด HTML content (ทั้งข้อความและตารางผสมกัน)
                if (hasHtml) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    ContentContext ctx = drawMixedHtmlContent(document, currentPage, contentStream,
                            htmlContent, fontRegular, fontBold, MARGIN_LEFT, yPosition,
                            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT, bookNo);
                    
                    contentStream = ctx.getContentStream();
                    currentPage = ctx.getCurrentPage();
                    yPosition = ctx.getYPosition();
                    
                    log.info("Mixed HTML content drawn in ministry PDF, new yPosition: {}", yPosition);
                }
                
                // SECTION 7: ช่องลงนาม
                if (signers != null && !signers.isEmpty()) {
                    yPosition -= SPACING_BEFORE_SIGNATURES;
                    
                    for (int i = 0; i < signers.size(); i++) {
                        SignerInfo signer = signers.get(i);
                        float requiredHeight = 120f;
                        if (yPosition < MIN_Y_POSITION + requiredHeight) {
                            contentStream.close();
                            
                            currentPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, currentPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        // ใช้ endDoc เป็น label (ถ้ามี)
                        String boxLabel = (endDoc != null && !endDoc.isEmpty()) ? endDoc : "";
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Learner", documentIndex, i, boxLabel, false);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            String pdfBase64 = convertToBase64(document);
            
            // NOTE: HTML tables are now drawn inline in SECTION 6.5
            
            return pdfBase64;
            
        } catch (Exception e) {
            log.error("Error generating ministry PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือภายใต้กระทรวงได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * สร้างรายการผู้รับ (bookLearner) สำหรับเจาะช่องลงนามในหนังสือหลัก
     * signBoxType = "เรียน" (สำหรับหนังสือภายใต้กระทรวง - เหมือน StampPdfGenerator)
     */
    private List<SignerInfo> buildSigners(GeneratePdfRequest request) {
        List<SignerInfo> signers = new ArrayList<>();
        if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            for (var learner : request.getBookLearner()) {
                signers.add(SignerInfo.builder()
                    .prefixName(learner.getPrefixName())
                    .firstname(learner.getFirstname())
                    .lastname(learner.getLastname())
                    .positionName(learner.getPositionName())
                    .departmentName(learner.getDepartmentName())
                    .email(learner.getEmail())
                    .signatureBase64(learner.getSignatureBase64())
                    .signBoxType(SignBoxType.LEARNER)
                    .build());
            }
        }
        return signers;
    }
}
