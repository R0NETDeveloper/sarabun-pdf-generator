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
        
        // 1. สร้าง PDF หนังสือประทับตรา แยกตามผู้รับแต่ละหน่วยงาน
        if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            for (int i = 0; i < request.getBookLearner().size(); i++) {
                GeneratePdfRequest.BookRelate recipient = request.getBookLearner().get(i);
                
                // สร้าง PDF สำหรับผู้รับแต่ละคน
                String stampPdfBase64 = generateStampPdfForRecipient(request, recipient);
                allPdfsToMerge.add(stampPdfBase64);
                
                String recipientName = buildRecipientName(recipient);
                recipientDescriptions.add("หนังสือประทับตรา ถึง " + recipientName);
                
                log.info("Generated stamp PDF {} for: {}", i + 1, recipientName);
            }
        } else {
            // Fallback: ถ้าไม่มี bookLearner ให้สร้าง PDF เดียว
            String stampPdfBase64 = generateStampPdf(request);
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
     * สร้างชื่อผู้รับจาก BookRelate
     */
    private String buildRecipientName(GeneratePdfRequest.BookRelate recipient) {
        // ใช้ชื่อองค์กรก่อน
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            return recipient.getOrganizeName();
        }
        // ถ้าไม่มี ใช้ชื่อหน่วยงาน
        if (recipient.getDepartmentName() != null && !recipient.getDepartmentName().isEmpty()) {
            return recipient.getDepartmentName();
        }
        // ถ้าไม่มี ใช้ชื่อตำแหน่ง
        if (recipient.getPositionName() != null && !recipient.getPositionName().isEmpty()) {
            return recipient.getPositionName();
        }
        // Fallback
        return "ผู้รับ";
    }
    
    /**
     * สร้าง PDF หนังสือประทับตราสำหรับผู้รับเฉพาะราย
     */
    private String generateStampPdfForRecipient(GeneratePdfRequest request, 
                                                 GeneratePdfRequest.BookRelate recipient) throws Exception {
        // รวบรวมข้อมูล
        String bookNo = request.getBookNo();
        
        // ใช้ข้อมูลผู้รับจาก recipient ที่ส่งเข้ามา
        String recipients = "";
        if (recipient.getPositionName() != null) {
            recipients = recipient.getPositionName();
        }
        if (recipient.getDepartmentName() != null && !recipient.getDepartmentName().isEmpty()) {
            recipients += (recipients.isEmpty() ? "" : " ") + recipient.getDepartmentName();
        }
        // ถ้ามีชื่อองค์กร ให้ใช้แทน
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            recipients = recipient.getOrganizeName();
        }
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // สร้าง SignerInfo จาก recipient (สำหรับช่องลงนาม "เรียน")
        List<SignerInfo> signers = new ArrayList<>();
        signers.add(SignerInfo.builder()
            .prefixName(recipient.getPrefixName())
            .firstname(recipient.getFirstname())
            .lastname(recipient.getLastname())
            .positionName(recipient.getPositionName())
            .departmentName(recipient.getDepartmentName())
            .email(recipient.getEmail())
            .signatureBase64(recipient.getSignatureBase64())
            .build());
        
        // ชื่อหน่วยงาน
        String departmentName = request.getDepartment() != null ? request.getDepartment() : 
                               (request.getDivisionName() != null ? request.getDivisionName() : "");
        
        // ข้อมูลติดต่อ
        ContactInfo contactInfo = buildContactInfo(request);
        
        log.info("Generating stamp for recipient: {}", recipients);
        
        return generatePdfInternal(bookNo, recipients, content, signers, departmentName, contactInfo);
    }
    
    /**
     * สร้าง PDF หนังสือประทับตรา
     */
    private String generateStampPdf(GeneratePdfRequest request) throws Exception {
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
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        // ชื่อหน่วยงาน
        String departmentName = request.getDepartment() != null ? request.getDepartment() : 
                               (request.getDivisionName() != null ? request.getDivisionName() : "");
        
        // ข้อมูลติดต่อ
        ContactInfo contactInfo = buildContactInfo(request);
        
        log.info("Generating stamp - bookNo: {}, recipients: {}, content length: {}", 
                bookNo, recipients, content.length());
        
        return generatePdfInternal(bookNo, recipients, content, signers, departmentName, contactInfo);
    }
    
    /**
     * สร้าง PDF ภายใน
     */
    private String generatePdfInternal(String bookNo,
                                       String recipients,
                                       String content,
                                       List<SignerInfo> signers,
                                       String departmentName,
                                       ContactInfo contactInfo) throws Exception {
        log.info("=== Generating stamp PDF internal ===");
        
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
                
                // SECTION 3: เนื้อหา
                if (content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    // วาด "เนื้อหา" + เนื้อหาจริง
                    String[] lines = content.split("\n");
                    boolean isFirst = true;
                    
                    for (String line : lines) {
                        if (yPosition < MIN_Y_POSITION) {
                            contentStream.close();
                            
                            PDPage newPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, newPage, 
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
                
                yPosition -= 30;
                
                // SECTION 4 + 5: ช่องลงนาม พร้อม label "เรียน" + ลายน้ำ ETDA สีแดง
                // ชื่อหน่วยงานจะวาดอยู่บนหัวของช่องลงนาม
                if (signers != null && !signers.isEmpty()) {
                    yPosition -= SPACING_BEFORE_SIGNATURES;
                    
                    PDPage currentPage = page;
                    
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
                        
                        // วาด Watermark ลายน้ำ ETDA สีแดง (ก่อนวาด signer box)
                        drawRedWatermark(contentStream, document, yPosition);
                        
                        // ใช้ label "เรียน" สำหรับหนังสือประทับตรา
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Sign", i, "เรียน");
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
                // SECTION 6: ข้อมูลติดต่อ
                if (contactInfo != null && contactInfo.hasAnyInfo()) {
                    yPosition -= 20;
                    
                    if (contactInfo.getDepartment() != null && !contactInfo.getDepartment().isEmpty()) {
                        yPosition = drawText(contentStream, contactInfo.getDepartment(), 
                                           fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    }
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
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            return convertToBase64(document);
            
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
                graphicsState.setNonStrokingAlphaConstant(0.15f); // 15% opacity
                graphicsState.setStrokingAlphaConstant(0.15f);
                contentStream.setGraphicsStateParameters(graphicsState);
                
                // ขนาดลายน้ำ
                float watermarkWidth = 80f;
                float watermarkHeight = 28f;
                
                // ตำแหน่ง: ตรงกลาง signer box (ขวาของหน้า)
                // Signer box อยู่ที่ประมาณ X = PAGE_WIDTH/2 ถึง PAGE_WIDTH - MARGIN_RIGHT
                // ตรงกลาง signer box = (PAGE_WIDTH/2 + PAGE_WIDTH - MARGIN_RIGHT) / 2
                float signerBoxCenterX = (PAGE_WIDTH / 2 + PAGE_WIDTH - MARGIN_RIGHT) / 2;
                float watermarkX = signerBoxCenterX - (watermarkWidth / 2);
                
                // Y: อยู่ตรงกลาง signer box (ใต้ label "เรียน" และเหนือชื่อผู้ลงนาม)
                // signer box height = 80, ดังนั้นกลาง box ≈ yPosition - 35
                float watermarkY = yPosition - 35;
                
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
    
    private String buildContent(GeneratePdfRequest request) {
        StringBuilder contentBuilder = new StringBuilder();
        if (request.getBookContent() != null && !request.getBookContent().isEmpty()) {
            for (var item : request.getBookContent()) {
                if (item.getBookContentTitle() != null && !item.getBookContentTitle().isEmpty()) {
                    String titleText = HtmlUtils.isHtml(item.getBookContentTitle()) 
                        ? HtmlUtils.htmlToPlainText(item.getBookContentTitle())
                        : item.getBookContentTitle();
                    contentBuilder.append(titleText).append("  ");
                }
                if (item.getBookContent() != null && !item.getBookContent().isEmpty()) {
                    String contentText = HtmlUtils.isHtml(item.getBookContent())
                        ? HtmlUtils.htmlToPlainText(item.getBookContent())
                        : item.getBookContent();
                    contentBuilder.append(contentText);
                }
                contentBuilder.append("\n\n");
            }
        }
        return contentBuilder.toString().trim();
    }
    
    /**
     * สร้างรายการผู้รับ (bookLearner) สำหรับเจาะช่องลงนามในหนังสือหลัก
     * หมายเหตุ: หนังสือหลักใช้ bookLearner, Memo ใช้ bookSigned
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
                    .build());
            }
        }
        return signers;
    }
    
    private ContactInfo buildContactInfo(GeneratePdfRequest request) {
        ContactInfo info = new ContactInfo();
        info.setDepartment(request.getDepartment());
        
        if (request.getContact() != null && !request.getContact().isEmpty()) {
            String[] contactLines = request.getContact().split("\n");
            for (String line : contactLines) {
                line = line.trim();
                if (line.startsWith("เลขหมาย") || line.startsWith("โทร.") || line.startsWith("โทรศัพท์")) {
                    info.setPhone(line.replaceFirst("^(เลขหมาย|โทร\\.|โทรศัพท์)\\s*", ""));
                } else if (line.startsWith("โทรสาร") || line.startsWith("แฟกซ์")) {
                    info.setFax(line.replaceFirst("^(โทรสาร|แฟกซ์)\\s*", ""));
                } else if (line.startsWith("อีเมล") || line.contains("@")) {
                    info.setEmail(line.replaceFirst("^อีเมล\\s*", ""));
                }
            }
        }
        
        return info;
    }
}
