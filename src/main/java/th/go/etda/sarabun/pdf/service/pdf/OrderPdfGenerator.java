package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.exception.PdfGenerationException;

// Static import สำหรับใช้ค่าคงที่จาก PdfConstants โดยตรง
import static th.go.etda.sarabun.pdf.constant.PdfConstants.*;
import th.go.etda.sarabun.pdf.constant.SignBoxType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือคำสั่ง (Order)
 * 
 * BookNameId: 3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA (ตรงกลางบน)
 * - หัวข้อ "คำสั่ง..." (ตรงกลาง)
 * - "ที่ {{เลขที่}}" (ตรงกลาง)
 * - "เรื่อง {{เรื่อง}}" (ตรงกลาง)
 * - "ฉบับที่ {{ฉบับที่}}" (ตรงกลาง)
 * - เส้นขีดใต้
 * - เนื้อหา
 * - "สั่ง ณ วันที่ ..." 
 * - ลายเซ็น
 * 
 * หมายเหตุ: หนังสือคำสั่งจะสร้างบันทึกข้อความ (Memo) พ่วงด้วยเสมอ
 */
@Slf4j
@Component
public class OrderPdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    
    public OrderPdfGenerator(MemoPdfGenerator memoPdfGenerator) {
        this.memoPdfGenerator = memoPdfGenerator;
    }
    
    @Override
    public BookType getBookType() {
        return BookType.ORDER;
    }
    
    @Override
    public String getGeneratorName() {
        return "OrderPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<PdfResult> results = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือคำสั่ง
        String orderPdfBase64 = generateOrderPdf(request);
        results.add(createMainPdfResult(orderPdfBase64, "หนังสือคำสั่ง"));
        
        // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ)
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        results.add(createMemoPdfResult(memoPdfBase64, "บันทึกข้อความ (สำเนาเก็บ)"));
        
        return results;
    }
    
    /**
     * สร้าง PDF หนังสือคำสั่ง
     * อ่านข้อมูลจาก document (เดิมคือ documentSub)
     */
    public String generateOrderPdf(GeneratePdfRequest request) throws Exception {
        // อ่านจาก document (New Format v2)
        GeneratePdfRequest.Document doc = request.getDocument();
        
        // รวบรวมข้อมูล - ใช้จาก document ก่อน, fallback เป็น memo
        String govName = "";
        String title = "";
        String bookNo = "";
        String dateThai = "";
        String speedLayer = "";
        
        if (doc != null) {
            // กำหนด govName จาก department หรือ divisionName
            if (doc.getDepartment() != null) {
                govName = doc.getDepartment();
            } else if (doc.getDivisionName() != null) {
                govName = doc.getDivisionName();
            }
            title = doc.getBookTitle() != null ? doc.getBookTitle() : "";
            bookNo = convertStringToThaiNumber(doc.getBookNo());
            dateThai = convertStringToThaiNumber(doc.getDateThai());
            speedLayer = doc.getSpeedLayer();
        } else {
            // Fallback to memo - กำหนด govName จาก department หรือ divisionName
            if (request.getDepartment() != null) {
                govName = request.getDepartment();
            } else if (request.getDivisionName() != null) {
                govName = request.getDivisionName();
            }
            title = request.getBookTitle() != null ? request.getBookTitle() : "";
            bookNo = convertStringToThaiNumber(request.getBookNo());
            dateThai = convertStringToThaiNumber(request.getDateThai());
            speedLayer = request.getSpeedLayer();
        }
        
        // ดึง edition (ฉบับที่) จาก document หรือ subDetail
        String edition = extractEdition(request, doc);
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // ตรวจสอบและรวบรวม HTML content จาก document
        String htmlContent = null;
        if (doc != null && doc.getBookContent() != null &&
            hasHtmlContent(doc.getBookContent())) {
            htmlContent = buildHtmlContent(doc.getBookContent());
        }
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        log.info("Generating order - govName: {}, title: {}, edition: {}, content length: {}, hasHtml: {}", 
                govName, title, edition, content.length(), htmlContent != null);
        
        return generatePdfInternal(govName, title, edition, dateThai, content, htmlContent, signers, bookNo, speedLayer);
    }
    
    /**
     * ดึงฉบับที่ จาก request
     */
    private String extractEdition(GeneratePdfRequest request, GeneratePdfRequest.Document doc) {
        // ลองดึงจาก document.year ก่อน (ถ้ามี)
        if (doc != null && doc.getYear() != null && !doc.getYear().isEmpty()) {
            return doc.getYear();
        }
        // ลองดึงจาก subDetail.docNo
        if (request.getSubDetail() != null && request.getSubDetail().getDocNo() != null 
            && !request.getSubDetail().getDocNo().isEmpty()) {
            return request.getSubDetail().getDocNo();
        }
        // ถ้าไม่มี return placeholder
        return "{{ฉบับที่}}";
    }
    
    /**
     * สร้าง PDF ภายใน
     */
    private String generatePdfInternal(String govName,
                                       String title,
                                       String edition,
                                       String dateThai,
                                       String content,
                                       String htmlContent,
                                       List<SignerInfo> signers,
                                       String bookNo,
                                       String speedLayer) throws Exception {
        log.info("=== Generating order PDF internal, hasHtml: {} ===", htmlContent != null && !htmlContent.isEmpty());
        
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
                
                // SECTION 0: Logo ETDA (ตรงกลางบน) - ใช้ function กลางจาก PdfGeneratorBase
                float logoBottomY = drawLogo(contentStream, document, yPosition, LogoPosition.CENTER);
                
                // วาด Speed Layer (ถ้ามี)
                drawSpeedLayer(contentStream, speedLayer, fontBold, yPosition, LogoPosition.CENTER);
                
                yPosition = logoBottomY - 15;
                
                // SECTION 1: หัวข้อ "คำสั่ง..."
                String headerText = "คำสั่ง" + (govName != null && !govName.isEmpty() ? govName : "");
                yPosition = drawCenteredText(contentStream, headerText, fontBold, FONT_SIZE_HEADER, yPosition - 10);
                yPosition -= 5;
                
                // SECTION 2: "ที่ {{เลขที่}}" (ตรงกลาง)
                String orderNoText = "ที่ " + (bookNo != null && !bookNo.isEmpty() ? bookNo : "{{เลขที่}}");
                yPosition = drawCenteredText(contentStream, orderNoText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 5;
                
                // SECTION 3: "เรื่อง {{เรื่อง}}" (ตรงกลาง)
                String subjectText = "เรื่อง " + (title != null && !title.isEmpty() ? title : "{{เรื่อง}}");
                yPosition = drawCenteredText(contentStream, subjectText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 5;
                
                // SECTION 4: "ฉบับที่ {{ฉบับที่}}" (ตรงกลาง)
                String editionText = "ฉบับที่" + (edition != null && !edition.isEmpty() ? edition : "{{ฉบับที่}}");
                yPosition = drawCenteredText(contentStream, editionText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 10;
                
                // SECTION 5: เส้นขีดใต้ (ตรงกลาง)
                float lineWidth = 150;
                float lineStartX = (PAGE_WIDTH - lineWidth) / 2;
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(lineStartX, yPosition);
                contentStream.lineTo(lineStartX + lineWidth, yPosition);
                contentStream.stroke();
                yPosition -= 20;
                
                // SECTION 6: เนื้อหา (รองรับทั้ง plain text และ HTML table inline)
                PDPage currentPage = page;
                
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
                    yPosition -= 10;
                    
                    ContentContext ctx = drawMixedHtmlContent(document, currentPage, contentStream,
                            htmlContent, fontRegular, fontBold, MARGIN_LEFT, yPosition,
                            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT, bookNo);
                    
                    contentStream = ctx.getContentStream();
                    currentPage = ctx.getCurrentPage();
                    yPosition = ctx.getYPosition();
                    
                    log.info("Mixed HTML content drawn in order PDF, new yPosition: {}", yPosition);
                }
                
                // SECTION 7: "สั่ง ณ วันที่ ..."
                if (dateThai != null && !dateThai.isEmpty()) {
                    yPosition -= 20;
                    String orderDate = "สั่ง ณ วันที่ " + convertToThaiDate(dateThai);
                    yPosition = drawCenteredText(contentStream, orderDate, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                }
                
                // SECTION 8: ช่องลงนาม (เจาะ Signature Field จริง)
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
                                                  "Sign", 1, i, "เรียน", false);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            // NOTE: HTML tables are now drawn inline in SECTION 6.5
            
            return convertToBase64(document);
            
        } catch (Exception e) {
            log.error("Error generating order PDF: ", e);
            throw new PdfGenerationException("ไม่สามารถสร้าง PDF หนังสือคำสั่งได้: " + e.getMessage(), e);
        }
    }
    
    // หมายเหตุ: convertToThaiDate() ย้ายไป PdfGeneratorBase แล้ว (DRY principle)
    
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
                    .signBoxType(SignBoxType.SIGN)
                    .build());
            }
        }
        return signers;
    }
}
