package th.go.etda.sarabun.pdf.service.pdf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.constant.SignBoxType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * Generator สำหรับ หนังสือประทับตรา (Stamp)
 * 
 * BookNameId: AF3E7697-6F7E-4AD8-B76C-E2134DB98747
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA (ขวาบน)
 * - "ที่" (ซ้าย)
 * - "ถึง" + ผู้รับ
 * - "เนื้อหา" + เนื้อหา
 * - ชื่อหน่วยงาน (ขวา)
 * - ช่องลงนาม "เรียน" + ลายน้ำ ETDA สีแดง
 * - ข้อมูลติดต่อ (เลขหมาย, โทรสาร, อีเมล)
 * 
 * หมายเหตุ: หนังสือประทับตราจะสร้างบันทึกข้อความ (Memo) พ่วงด้วยเสมอ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StampPdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    private final HtmlContentRenderer htmlContentRenderer;
    
    @Override
    public BookType getBookType() {
        return BookType.STAMP;
    }
    
    @Override
    public String getGeneratorName() {
        return "StampPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<String> allPdfsToMerge = new ArrayList<>();
        List<String> recipientDescriptions = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือประทับตรา แยกตามผู้รับแต่ละหน่วยงาน (ใช้ bookRecipients)
        if (request.getBookRecipients() != null && !request.getBookRecipients().isEmpty()) {
            for (int i = 0; i < request.getBookRecipients().size(); i++) {
                GeneratePdfRequest.BookRecipient recipient = request.getBookRecipients().get(i);
                
                // สร้าง PDF สำหรับผู้รับแต่ละหน่วยงาน (documentIndex = i+1)
                String stampPdfBase64 = generateStampPdfForRecipient(request, recipient, i + 1);
                allPdfsToMerge.add(stampPdfBase64);
                
                String recipientName = buildRecipientName(recipient);
                recipientDescriptions.add("หนังสือประทับตรา ถึง " + recipientName);
                
                log.info("Generated stamp PDF {} for: {}", i + 1, recipientName);
            }
        } else {
            // Fallback: ถ้าไม่มี bookRecipients ให้สร้าง PDF เดียว (documentIndex = 1)
            String stampPdfBase64 = generateStampPdf(request, 1);
            allPdfsToMerge.add(stampPdfBase64);
            recipientDescriptions.add("หนังสือประทับตรา");
        }
        
        // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ) - แค่ 1 ฉบับ
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        allPdfsToMerge.add(memoPdfBase64);
        recipientDescriptions.add("บันทึกข้อความ (สำเนาเก็บ)");
        
        log.info("Total PDFs to merge: {} ({} stamp + 1 memo)", 
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
        // ใช้ชื่อองค์กรก่อน (สำหรับแสดงใน "ถึง")
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            return recipient.getOrganizeName();
        }
        // ถ้าไม่มี ใช้ชื่อหน่วยงาน
        if (recipient.getDivisionName() != null && !recipient.getDivisionName().isEmpty()) {
            return recipient.getDivisionName();
        }
        // ถ้าไม่มี ใช้ชื่อกรม
        if (recipient.getDepartmentName() != null && !recipient.getDepartmentName().isEmpty()) {
            return recipient.getDepartmentName();
        }
        // ถ้าไม่มี ใช้ชื่อกระทรวง
        if (recipient.getMinistryName() != null && !recipient.getMinistryName().isEmpty()) {
            return recipient.getMinistryName();
        }
        // Fallback
        return "ผู้รับ";
    }
    
    /**
     * สร้าง PDF หนังสือประทับตราสำหรับผู้รับเฉพาะราย (หน่วยงานภายนอก)
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateStampPdfForRecipient(GeneratePdfRequest request, 
                                                 GeneratePdfRequest.BookRecipient recipient,
                                                 int documentIndex) throws Exception {
        // รวบรวมข้อมูล
        String bookNo = request.getBookNo();
        
        // ใช้ organizeName สำหรับ "ถึง"
        String recipients = recipient.getOrganizeName() != null ? recipient.getOrganizeName() : "";
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // ตรวจสอบและรวบรวม HTML content (bookContent เป็น Object ไม่ใช่ Array)
        String htmlContent = null;
        if (request.getDocumentSub() != null && 
            request.getDocumentSub().getBookContent() != null &&
            hasHtmlContent(request.getDocumentSub().getBookContent())) {
            htmlContent = buildHtmlContent(request.getDocumentSub().getBookContent());
        }
        
        // สร้าง SignerInfo จาก bookSigned (ผู้ลงนาม) - ไม่ใช่จาก recipient
        List<SignerInfo> signers = buildSigners(request);
        
        // ชื่อหน่วยงาน
        String departmentName = request.getDepartment() != null ? request.getDepartment() : 
                               (request.getDivisionName() != null ? request.getDivisionName() : "");
        
        // ข้อมูลติดต่อ
        ContactInfo contactInfo = buildContactInfo(request);
        
        // ใช้ endDoc จาก recipient ก่อน ถ้าไม่มีใช้จาก request
        String endDoc = (recipient.getEndDoc() != null && !recipient.getEndDoc().isEmpty()) 
                        ? recipient.getEndDoc() 
                        : request.getEndDoc();
        
        log.info("Generating stamp for recipient: {}, endDoc: {}, hasHtml: {}, docIndex: {}", recipients, endDoc, htmlContent != null, documentIndex);
        
        return generatePdfInternal(bookNo, recipients, content, htmlContent, signers, departmentName, contactInfo, endDoc, request.getSpeedLayer(), documentIndex);
    }
    
    /**
     * สร้าง PDF หนังสือประทับตรา
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateStampPdf(GeneratePdfRequest request, int documentIndex) throws Exception {
        // รวบรวมข้อมูล
        String bookNo = request.getBookNo();
        
        // รวบรวมผู้รับ (ถึง)
        String recipients = "";
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            recipients = request.getRecipients();
        } else if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            recipients = request.getBookLearner().stream()
                .map(l -> {
                    String pos = l.getPositionName() != null ? l.getPositionName() : "";
                    String dept = l.getDepartmentName() != null ? " " + l.getDepartmentName() : "";
                    return pos + dept;
                })
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.joining(", "));
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
        
        // ชื่อหน่วยงาน
        String departmentName = request.getDepartment() != null ? request.getDepartment() : 
                               (request.getDivisionName() != null ? request.getDivisionName() : "");
        
        // ข้อมูลติดต่อ
        ContactInfo contactInfo = buildContactInfo(request);
        
        // หนังสือประทับตราไม่มี endDoc (ไม่มีคำลงท้าย)
        String endDoc = null;
        
        log.info("Generating stamp - bookNo: {}, recipients: {}, content length: {}, hasHtml: {}, docIndex: {}", 
                bookNo, recipients, content.length(), htmlContent != null, documentIndex);
        
        return generatePdfInternal(bookNo, recipients, content, htmlContent, signers, departmentName, contactInfo, endDoc, request.getSpeedLayer(), documentIndex);
    }
    
    /**
     * สร้าง PDF ภายใน
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generatePdfInternal(String bookNo,
                                       String recipients,
                                       String content,
                                       String htmlContent,
                                       List<SignerInfo> signers,
                                       String departmentName,
                                       ContactInfo contactInfo,
                                       String endDoc,
                                       String speedLayer,
                                       int documentIndex) throws Exception {
        log.info("=== Generating stamp PDF internal, hasHtml: {} ===", htmlContent != null && !htmlContent.isEmpty());
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDFont fontRegular = loadRegularFont(document);
            PDFont fontBold = loadBoldFont(document);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาด debug borders
                drawDebugBorders(contentStream);
                
                // SECTION 0: Logo ETDA (ขวาบน)
                drawLogo(contentStream, document, yPosition, LogoPosition.RIGHT);
                
                // วาด Speed Layer (ถ้ามี)
                drawSpeedLayer(contentStream, speedLayer, fontBold, yPosition, LogoPosition.RIGHT);
                
                yPosition -= LOGO_SPACING + LOGO_HEIGHT - 30;
                
                // SECTION 1: "ที่" (ซ้าย)
                drawText(contentStream, "ที่", fontBold, FONT_SIZE_FIELD, MARGIN_LEFT, yPosition);
                yPosition -= 30;
                
                // SECTION 2: "ถึง" + ผู้รับ
                String recipientsValue = (recipients != null && !recipients.isEmpty()) ? recipients : "";
                yPosition = drawMultilineTextWithIndent(contentStream, "ถึง  " + recipientsValue, 
                                    fontRegular, FONT_SIZE_FIELD_VALUE, 
                                    MARGIN_LEFT, yPosition, 
                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT, "ถึง  ");
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 3: เนื้อหา (รองรับทั้ง plain text และ HTML table inline)
                PDPage currentPage = page;
                
                // ตรวจสอบว่ามี HTML content หรือไม่
                boolean hasHtml = htmlContent != null && !htmlContent.isEmpty();
                
                // วาด plain text content (เฉพาะกรณีที่ไม่มี HTML content)
                if (!hasHtml && content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    // วาด "เนื้อหา" + เนื้อหาจริง
                    String[] lines = content.split("\n");
                    boolean isFirst = true;
                    
                    for (String line : lines) {
                        if (yPosition < MIN_Y_POSITION) {
                            contentStream.close();
                            
                            currentPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, currentPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        // บรรทัดแรกใส่ "เนื้อหา" นำหน้า
                        String textToDraw = isFirst ? "เนื้อหา  " + line : line;
                        isFirst = false;
                        
                        yPosition = drawMultilineText(contentStream, textToDraw, 
                                                    fontRegular, FONT_SIZE_CONTENT, 
                                                    MARGIN_LEFT, yPosition, 
                                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                    }
                }
                
                // SECTION 3.5: วาด HTML content (ทั้งข้อความและตารางผสมกัน)
                if (hasHtml) {
                    yPosition -= 10;
                    
                    ContentContext ctx = drawMixedHtmlContent(document, currentPage, contentStream,
                            htmlContent, fontRegular, fontBold, MARGIN_LEFT, yPosition,
                            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT, bookNo);
                    
                    contentStream = ctx.getContentStream();
                    currentPage = ctx.getCurrentPage();
                    yPosition = ctx.getYPosition();
                    
                    log.info("Mixed HTML content drawn in stamp PDF, new yPosition: {}", yPosition);
                }
                
                yPosition -= 30;
                
                // SECTION 4 + 5: ช่องลงนาม พร้อม label "เรียน" + ลายน้ำ ETDA สีแดง
                // ชื่อหน่วยงานจะวาดอยู่บนหัวของช่องลงนาม
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
                        
                        // วาด Watermark ลายน้ำ ETDA สีแดง เป็นพื้นหลัง (วาดก่อนทุกอย่าง)
                        drawRedWatermark(contentStream, document, yPosition);
                        
                        // วาดชื่อหน่วยงาน (อยู่บนหัวของช่องลงนาม - ตรงกลางขวา)
                        if (departmentName != null && !departmentName.isEmpty()) {
                            float deptTextWidth = fontRegular.getStringWidth(departmentName) / 1000 * FONT_SIZE_FIELD_VALUE;
                            // ตำแหน่ง X: ตรงกลางของ signer box (ขวาของหน้า)
                            float signerBoxCenterX = (PAGE_WIDTH / 2 + PAGE_WIDTH - MARGIN_RIGHT) / 2;
                            float deptX = signerBoxCenterX - (deptTextWidth / 2);
                            yPosition = drawText(contentStream, departmentName, fontRegular, 
                                                FONT_SIZE_FIELD_VALUE, deptX, yPosition);
                            yPosition -= 5; // ห่างจากช่องลงนามนิดเดียว
                        }
                        
                        // ใช้ endDoc เป็น label สำหรับหนังสือประทับตรา (เช่น "ขอแสดงความนับถือ")
                        String boxLabel = (endDoc != null && !endDoc.isEmpty()) ? endDoc : "";
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Learner", documentIndex, i, boxLabel, true);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
                // SECTION 6: ข้อมูลติดต่อ
                if (contactInfo != null && contactInfo.hasAnyInfo()) {
                    yPosition -= 20;
                    
                    // แสดงชื่อหน่วยงานก่อน
                    if (contactInfo.getDepartment() != null && !contactInfo.getDepartment().isEmpty()) {
                        yPosition = drawText(contentStream, contactInfo.getDepartment(), 
                                           fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    }
                    
                    // ถ้าใช้ rawContact แสดงตาม line, ถ้าไม่ใช้ แสดงแบบ parsed fields
                    if (contactInfo.useRawContact()) {
                        // แสดงตาม format ที่ส่งมา
                        // รองรับทั้ง newline จริง (\n) และ literal "\n" จาก JSON
                        String rawText = contactInfo.getRawContact()
                            .replace("\\n", "\n");  // แปลง literal \n เป็น newline จริง
                        String[] lines = rawText.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                yPosition = drawText(contentStream, line, 
                                               fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                            }
                        }
                    } else {
                        // แสดงแบบแยก field (เมื่อใช้ contactInfo object)
                        if (contactInfo.getPhone() != null && !contactInfo.getPhone().isEmpty()) {
                            yPosition = drawText(contentStream, "เลขหมาย " + convertToThaiNumber(contactInfo.getPhone()), 
                                               fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                        }
                        if (contactInfo.getFax() != null && !contactInfo.getFax().isEmpty()) {
                            yPosition = drawText(contentStream, "โทรสาร " + convertToThaiNumber(contactInfo.getFax()), 
                                               fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                        }
                        if (contactInfo.getEmail() != null && !contactInfo.getEmail().isEmpty()) {
                            yPosition = drawText(contentStream, "อีเมล " + contactInfo.getEmail(), 
                                               fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                        }
                    }
                }
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            String pdfBase64 = convertToBase64(document);
            
            // NOTE: HTML tables are now drawn inline in SECTION 3.5
            
            return pdfBase64;
            
        } catch (Exception e) {
            log.error("Error generating stamp PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือประทับตราได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * วาดลายน้ำ ETDA สีแดงโปร่งใส (หลัง signer box)
     */
    private void drawRedWatermark(PDPageContentStream contentStream, PDDocument document, float yPosition) {
        try {
            InputStream stampStream = getClass().getClassLoader().getResourceAsStream("images/etda_stamp_red.png");
            if (stampStream != null) {
                PDImageXObject stampImage = PDImageXObject.createFromByteArray(
                    document, stampStream.readAllBytes(), "watermark");
                
                // ตั้งค่าความโปร่งใส (จางๆ)
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(0.20f); // 5% opacity (จางมาก)
                graphicsState.setStrokingAlphaConstant(0.05f);
                contentStream.setGraphicsStateParameters(graphicsState);
                
                // ขนาดลายน้ำ (กว้าง x สูง)
                float watermarkWidth = 120f;   // กว้าง
                float watermarkHeight = 60f;   // สูงขึ้น (เดิม 42f)
                
                // ตำแหน่ง: ตรงกลาง signer box (ขวาของหน้า)
                // Signer box อยู่ที่ประมาณ X = PAGE_WIDTH/2 ถึง PAGE_WIDTH - MARGIN_RIGHT
                // ตรงกลาง signer box = (PAGE_WIDTH/2 + PAGE_WIDTH - MARGIN_RIGHT) / 2
                float signerBoxCenterX = (PAGE_WIDTH / 2 + PAGE_WIDTH - MARGIN_RIGHT) / 2;
                float watermarkX = signerBoxCenterX - (watermarkWidth / 2);
                
                // Y: ขยับสูงขึ้น 1 นิ้ว (72 points) จากเดิม -50 เป็น +22
                float watermarkY = yPosition -20;
                
                contentStream.drawImage(stampImage, watermarkX, watermarkY, watermarkWidth, watermarkHeight);
                
                // รีเซ็ตความโปร่งใสกลับ
                graphicsState.setNonStrokingAlphaConstant(1.0f);
                graphicsState.setStrokingAlphaConstant(1.0f);
                contentStream.setGraphicsStateParameters(graphicsState);
                
                stampStream.close();
                log.info("Red stamp watermark drawn at ({}, {}) size {}x{}", 
                        watermarkX, watermarkY, watermarkWidth, watermarkHeight);
            } else {
                log.warn("Stamp image not found: images/etda_stamp_red.png");
            }
        } catch (Exception e) {
            log.warn("Failed to draw stamp watermark: {}", e.getMessage());
        }
    }
    
    /**
     * แปลง string ตัวเลขเป็นตัวเลขไทย
     */
    private String convertToThaiNumber(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                result.append((char) ('๐' + (c - '0')));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    // ============================================
    // Helper methods
    // ============================================
    
    /**
     * สร้างรายการผู้รับ (bookLearner) สำหรับเจาะช่องลงนามในหนังสือหลัก
     * signBoxType = "เรียน"
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
    
    private ContactInfo buildContactInfo(GeneratePdfRequest request) {
        // ถ้ามี contactInfo object ให้ใช้เลย (แบบ parsed fields)
        if (request.getContactInfo() != null && request.getContactInfo().hasAnyInfo()) {
            GeneratePdfRequest.ContactInfo reqContact = request.getContactInfo();
            return ContactInfo.builder()
                .department(reqContact.getDepartment() != null ? reqContact.getDepartment() : request.getDepartment())
                .phone(reqContact.getPhone())
                .fax(reqContact.getFax())
                .email(reqContact.getEmail())
                .build();
        }
        
        // Fallback: ใช้ contact string โดยตรง (แสดงตาม format ที่ส่งมา)
        ContactInfo info = new ContactInfo();
        info.setDepartment(request.getDepartment());
        
        if (request.getContact() != null && !request.getContact().isEmpty()) {
            // แปลง HTML เป็น plain text ก่อน (ถ้าเป็น HTML)
            String contactText = request.getContact();
            if (HtmlUtils.isHtml(contactText)) {
                contactText = HtmlUtils.htmlToPlainText(contactText);
            }
            
            // เก็บเป็น rawContact เพื่อแสดงตาม format ที่ส่งมา
            info.setRawContact(contactText);
        }
        
        return info;
    }
}
