package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * Generator สำหรับ หนังสือข้อบังคับ (Rule)
 * 
 * BookNameId: 4AB1EC00-9E5E-4113-B577-D8ED46BA7728
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA (ตรงกลางบน)
 * - หัวข้อ "ข้อบังคับคณะกรรมการ..." (ตรงกลาง)
 * - "ว่าด้วย {{เรื่อง}}" (ตรงกลาง)
 * - "ฉบับที่{{ฉบับที่}}" (ตรงกลาง)
 * - "พ.ศ." (ตรงกลาง)
 * - เส้นขีดใต้
 * - เนื้อหา
 * - "ประกาศ ณ วันที่ ..." 
 * - ลายเซ็น (พร้อม label "เรียน")
 * 
 * หมายเหตุ: หนังสือข้อบังคับจะสร้างบันทึกข้อความ (Memo) พ่วงด้วยเสมอ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RulePdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    
    @Override
    public BookType getBookType() {
        return BookType.RULE;
    }
    
    @Override
    public String getGeneratorName() {
        return "RulePdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<PdfResult> results = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือข้อบังคับ
        String rulePdfBase64 = generateRulePdf(request);
        results.add(createMainPdfResult(rulePdfBase64, "หนังสือข้อบังคับ"));
        
        // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ)
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        results.add(createMemoPdfResult(memoPdfBase64, "บันทึกข้อความ (สำเนาเก็บ)"));
        
        return results;
    }
    
    /**
     * สร้าง PDF หนังสือข้อบังคับ
     */
    public String generateRulePdf(GeneratePdfRequest request) throws Exception {
        // รวบรวมข้อมูล
        String govName = request.getDepartment() != null ? request.getDepartment() : 
                        (request.getDivisionName() != null ? request.getDivisionName() : "");
        String title = request.getBookTitle() != null ? request.getBookTitle() : "";
        String bookNo = request.getBookNo();
        String dateThai = request.getDateThai();
        
        // ดึง edition (ฉบับที่) และ year (พ.ศ.) จาก bookNo หรือ subDetail
        String edition = extractEdition(request);
        String year = extractYear(request, dateThai);
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        log.info("Generating rule - govName: {}, title: {}, edition: {}, year: {}, content length: {}", 
                govName, title, edition, year, content.length());
        
        return generatePdfInternal(govName, title, edition, year, dateThai, content, signers, bookNo);
    }
    
    /**
     * ดึงฉบับที่ จาก request
     */
    private String extractEdition(GeneratePdfRequest request) {
        // ลองดึงจาก subDetail.docNo ก่อน
        if (request.getSubDetail() != null && request.getSubDetail().getDocNo() != null 
            && !request.getSubDetail().getDocNo().isEmpty()) {
            return request.getSubDetail().getDocNo();
        }
        // ถ้าไม่มี return placeholder
        return "{{ฉบับ}}";
    }
    
    /**
     * ดึงปี พ.ศ. จาก dateThai
     */
    private String extractYear(GeneratePdfRequest request, String dateThai) {
        if (dateThai != null && !dateThai.isEmpty()) {
            // หาปี พ.ศ. จากวันที่ไทย เช่น "8 มกราคม พ.ศ. 2569"
            String[] parts = dateThai.split("\\s+");
            if (parts.length >= 3) {
                String lastPart = parts[parts.length - 1];
                // ลองแปลงเป็นตัวเลข
                try {
                    Integer.parseInt(lastPart);
                    return lastPart;
                } catch (NumberFormatException e) {
                    // ไม่ใช่ตัวเลข ลองหาจาก pattern อื่น
                }
            }
        }
        return "";
    }
    
    /**
     * สร้าง PDF ภายใน
     */
    private String generatePdfInternal(String govName,
                                       String title,
                                       String edition,
                                       String year,
                                       String dateThai,
                                       String content,
                                       List<SignerInfo> signers,
                                       String bookNo) throws Exception {
        log.info("=== Generating rule PDF internal ===");
        
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
                
                // SECTION 0: Logo ETDA (ตรงกลางบน)
                float logoBottomY = drawLogo(contentStream, document, yPosition, LogoPosition.CENTER);
                yPosition = logoBottomY - 15;
                
                // SECTION 1: หัวข้อ "ข้อบังคับคณะกรรมการ..."
                String headerText = "ข้อบังคับ" + (govName != null && !govName.isEmpty() ? govName : "");
                yPosition = drawCenteredText(contentStream, headerText, fontBold, FONT_SIZE_HEADER, yPosition-10);
                yPosition -= 5;
                
                // SECTION 2: "ว่าด้วย {{ชื่อเรื่องหนังสือข้อบังคับ}}"
                String subjectText = "ว่าด้วย " + (title != null && !title.isEmpty() ? title : "{{ชื่อเรื่องหนังสือข้อบังคับ}}");
                yPosition = drawCenteredText(contentStream, subjectText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 5;
                
                // SECTION 3: "ฉบับที่{{ฉบับ}}"
                String editionText = "ฉบับที่" + (edition != null && !edition.isEmpty() ? edition : "{{ฉบับ}}");
                yPosition = drawCenteredText(contentStream, editionText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 5;
                
                // SECTION 4: "พ.ศ."
                String yearText = "พ.ศ." + (year != null && !year.isEmpty() ? " " + convertToThaiDate(year) : "");
                yPosition = drawCenteredText(contentStream, yearText, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                yPosition -= 10;
                
                // SECTION 5: เส้นขีดใต้ (ตรงกลาง)
                float lineWidth = 150;
                float lineStartX = (PAGE_WIDTH - lineWidth) / 2;
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(lineStartX, yPosition);
                contentStream.lineTo(lineStartX + lineWidth, yPosition);
                contentStream.stroke();
                yPosition -= 20;
                
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
                
                // SECTION 7: "ประกาศ ณ วันที่ ..."
                if (dateThai != null && !dateThai.isEmpty()) {
                    yPosition -= 20;
                    String announcementDate = "ประกาศ ณ วันที่ " + convertToThaiDate(dateThai);
                    yPosition = drawCenteredText(contentStream, announcementDate, fontRegular, FONT_SIZE_FIELD_VALUE, yPosition);
                }
                
                // SECTION 8: ช่องลงนาม (เจาะ Signature Field จริง พร้อม label "เรียน")
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
                        
                        // ใช้ label "เรียน" สำหรับหนังสือข้อบังคับ
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Sign", i, "เรียน");
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
            log.error("Error generating rule PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือข้อบังคับได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * แปลงวันที่เป็นรูปแบบไทย (ตัวเลขไทย)
     * เช่น "8 มกราคม พ.ศ. 2569" -> "๘ มกราคม ๒๕๖๙"
     */
    private String convertToThaiDate(String dateThai) {
        if (dateThai == null || dateThai.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : dateThai.toCharArray()) {
            if (Character.isDigit(c)) {
                // แปลงตัวเลขอารบิกเป็นตัวเลขไทย
                result.append((char) ('๐' + (c - '0')));
            } else {
                result.append(c);
            }
        }
        
        // ลบ "พ.ศ." ออก (ถ้ามี) เพราะจะใช้ "ประกาศ ณ วันที่" แทน
        String text = result.toString().replace("พ.ศ. ", "").replace("พ.ศ.", "");
        return text.trim();
    }
    
    /**
     * สร้างเนื้อหาจาก request
     */
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
}
