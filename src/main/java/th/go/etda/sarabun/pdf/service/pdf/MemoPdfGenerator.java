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
    
    private final HtmlContentRenderer htmlContentRenderer;
    
    public MemoPdfGenerator(HtmlContentRenderer htmlContentRenderer) {
        this.htmlContentRenderer = htmlContentRenderer;
    }
    
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
     * อ่านข้อมูลจาก documentMain (New Format)
     */
    public String generateMemoPdf(GeneratePdfRequest request) throws Exception {
        // อ่านจาก documentMain (New Format)
        GeneratePdfRequest.DocumentMain docMain = request.getDocumentMain();
        
        String govName = "";
        String dateThai = "";
        String title = "";
        String bookNo = "";
        String speedLayer = "";
        
        if (docMain != null) {
            govName = docMain.getDivisionName() != null ? docMain.getDivisionName() : 
                     (docMain.getDepartment() != null ? docMain.getDepartment() : "");
            dateThai = docMain.getDateThai();
            title = docMain.getBookTitle() != null ? docMain.getBookTitle() : "";
            bookNo = docMain.getBookNo();
            speedLayer = docMain.getSpeedLayer();
        }
        
        // รวบรวมรายชื่อผู้รับจาก bookLearner
        String recipients = "";
        if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            recipients = request.getBookLearner().stream()
                .map(l -> l.getPositionName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining("\n"));
        }
        
        // รวบรวมเนื้อหาจาก documentMain
        String content = buildContentFromDocMain(docMain);
        
        // ตรวจสอบและรวบรวม HTML content
        String htmlContent = hasHtmlContentInDocMain(docMain) ? buildHtmlContentFromDocMain(docMain) : null;
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        log.info("Generating memo - govName: {}, title: {}, content length: {}, hasHtml: {}", 
                govName, title, content.length(), htmlContent != null);
        
        return generatePdfInternal(govName, dateThai, bookNo, title, recipients, content, htmlContent,
                                  speedLayer, signers);
    }
    
    /**
     * รวบรวมเนื้อหาจาก documentMain.bookContent (New Format - Object)
     */
    private String buildContentFromDocMain(GeneratePdfRequest.DocumentMain docMain) {
        if (docMain == null || docMain.getBookContent() == null) {
            return "";
        }
        
        GeneratePdfRequest.BookContent bc = docMain.getBookContent();
        String content = bc.getContent();
        
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // ถ้าเป็น HTML content ให้แปลงเป็น plain text
        if (bc.isHtmlContent() || HtmlUtils.isHtml(content)) {
            return HtmlUtils.htmlToPlainText(content);
        }
        
        return content;
    }
    
    /**
     * ดึง subject จาก documentMain.bookContent
     */
    private String getSubjectFromDocMain(GeneratePdfRequest.DocumentMain docMain) {
        if (docMain == null || docMain.getBookContent() == null) {
            return null;
        }
        return docMain.getBookContent().getSubject();
    }
    
    /**
     * ตรวจสอบว่า documentMain.bookContent เป็น HTML content หรือไม่
     */
    private boolean hasHtmlContentInDocMain(GeneratePdfRequest.DocumentMain docMain) {
        if (docMain == null || docMain.getBookContent() == null) {
            return false;
        }
        
        GeneratePdfRequest.BookContent bc = docMain.getBookContent();
        if (bc.isHtmlContent()) {
            return true;
        }
        
        String content = bc.getContent();
        return content != null && HtmlUtils.isHtml(content);
    }
    
    /**
     * สร้าง HTML content จาก documentMain.bookContent (สำหรับ HTML rendering)
     */
    private String buildHtmlContentFromDocMain(GeneratePdfRequest.DocumentMain docMain) {
        if (docMain == null || docMain.getBookContent() == null) {
            return "";
        }
        
        GeneratePdfRequest.BookContent bc = docMain.getBookContent();
        String content = bc.getContent();
        
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // ถ้าเป็น HTML แล้ว return ตรงๆ
        if (bc.isHtmlContent() || HtmlUtils.isHtml(content)) {
            return content;
        }
        
        // ถ้าเป็น plain text ให้แปลงเป็น HTML
        return plainTextToHtml(content);
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
                                       String htmlContent,
                                       String speedLayer,
                                       List<SignerInfo> signers) throws Exception {
        log.info("=== Generating memo PDF internal, hasHtml: {} ===", htmlContent != null && !htmlContent.isEmpty());
        
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
                
                // SECTION 6: เนื้อหา (รองรับทั้ง plain text และ HTML mixed content)
                PDPage currentPage = page; // track หน้าปัจจุบัน
                
                // ตรวจสอบว่ามี HTML content หรือไม่
                boolean hasHtml = htmlContent != null && !htmlContent.isEmpty();
                
                // วาด plain text content (เฉพาะกรณีที่ไม่มี HTML content)
                // ถ้ามี HTML content จะใช้ HTML renderer แทน
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
                    
                    log.info("Mixed HTML content drawn in memo, new yPosition: {}", yPosition);
                }
                
                // SECTION 7: ช่องลงนาม (เจาะ Signature Field จริง)
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
            
            String pdfBase64 = convertToBase64(document);
            
            // NOTE: HTML tables are now drawn inline in SECTION 6.5
            // No need to append as separate pages
            
            return pdfBase64;
            
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
