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

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.constant.SignBoxType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * Generator สำหรับ หนังสือบันทึกข้อความ (Memo)
 * 
 * BookNameId: BB4A2F11-722D-449A-BCC5-22208C7A4DEC
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA (ซ้ายบน)
 * - หัวข้อ "บันทึกข้อความ" (ตรงกลาง)
 * - ส่วนราชการ
 * - ที่ + วันที่ (บรรทัดเดียวกัน)
 * - เรื่อง
 * - เรียน
 * - เนื้อหา
 * - ลายเซ็น
 */
@Slf4j
@Component
public class MemoPdfGenerator extends PdfGeneratorBase {
    
    @Override
    public BookType getBookType() {
        return BookType.MEMO;
    }
    
    @Override
    public String getGeneratorName() {
        return "MemoPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<PdfResult> results = new ArrayList<>();
        
        // สร้าง PDF บันทึกข้อความ
        String memoPdfBase64 = generateMemoPdf(request);
        results.add(createMainPdfResult(memoPdfBase64, "หนังสือบันทึกข้อความ"));
        
        return results;
    }
    
    /**
     * สร้าง PDF บันทึกข้อความ
     */
    public String generateMemoPdf(GeneratePdfRequest request) throws Exception {
        // รวบรวมข้อมูล
        String govName = request.getDivisionName() != null ? request.getDivisionName() : 
                        (request.getDepartment() != null ? request.getDepartment() : "");
        String dateThai = request.getDateThai();
        String title = request.getBookTitle() != null ? request.getBookTitle() : "";
        String bookNo = request.getBookNo();
        
        // รวบรวมรายชื่อผู้รับ
        String recipients = "";
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
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
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        log.info("Generating memo - govName: {}, title: {}, content length: {}", 
                govName, title, content.length());
        
        return generatePdfInternal(govName, dateThai, bookNo, title, recipients, content, 
                                  request.getSpeedLayer(), signers);
    }
    
    /**
     * สร้าง PDF ภายใน
     */
    private String generatePdfInternal(String govName,
                                       String date,
                                       String bookNo,
                                       String title,
                                       String recipients,
                                       String content,
                                       String speedLayer,
                                       List<SignerInfo> signers) throws Exception {
        log.info("=== Generating memo PDF internal ===");
        
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
                
                // SECTION 1: หัวข้อ "บันทึกข้อความ"
                yPosition = drawCenteredText(contentStream, "บันทึกข้อความ", 
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
                
                // SECTION 6: เนื้อหา
                if (content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    String[] lines = content.split("\n");
                    
                    for (String line : lines) {
                        if (yPosition < MIN_Y_POSITION) {
                            contentStream.close();
                            
                            PDPage newPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, newPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        yPosition = drawMultilineText(contentStream, line, 
                                                    fontRegular, FONT_SIZE_CONTENT, 
                                                    MARGIN_LEFT, yPosition, 
                                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                    }
                }
                
                // SECTION 7: ช่องลงนาม (เจาะ Signature Field จริง)
                if (signers != null && !signers.isEmpty()) {
                    yPosition -= SPACING_BEFORE_SIGNATURES;
                    
                    PDPage currentPage = page; // track หน้าปัจจุบัน
                    
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
                        
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Sign", 1, i, "ช่องลงนาม", false);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            return convertToBase64(document);
            
        } catch (Exception e) {
            log.error("Error generating memo PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF บันทึกข้อความได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * สร้างรายการผู้ลงนามจาก request
     * signBoxType = "ลงนาม" (สำหรับบันทึกข้อความ)
     */
    private List<SignerInfo> buildSigners(GeneratePdfRequest request) {
        List<SignerInfo> signers = new ArrayList<>();
        if (request.getBookSigned() != null && !request.getBookSigned().isEmpty()) {
            for (var signer : request.getBookSigned()) {
                signers.add(SignerInfo.builder()
                    .prefixName(signer.getPrefixName())
                    .firstname(signer.getFirstname())
                    .lastname(signer.getLastname())
                    .positionName(signer.getPositionName())
                    .departmentName(signer.getDepartmentName())
                    .email(signer.getEmail())
                    .signatureBase64(signer.getSignatureBase64())
                    .signBoxType(SignBoxType.SIGN)
                    .build());
            }
        }
        return signers;
    }
}
