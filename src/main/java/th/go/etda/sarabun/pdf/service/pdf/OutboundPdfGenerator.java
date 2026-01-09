package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;
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
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * Generator สำหรับ หนังสือส่งออก (Outbound)
 * 
 * BookNameId: 90F72F0E-528D-4992-907A-F2C6B37AD9A5
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA (ขวาบน)
 * - "ที่" (ซ้าย) + ที่อยู่สำนักงาน (ขวา)
 * - วันที่ (ขวา ใต้ที่อยู่)
 * - เรื่อง
 * - คำขึ้นต้น (ถ้ามี)
 * - เรียน (2 บรรทัด: หน่วยงาน + ที่อยู่)
 * - อ้างถึง
 * - สิ่งที่ส่งมาด้วย
 * - เนื้อหา
 * - ข้อความท้ายเอกสาร (เช่น "ขอแสดงความนับถือ")
 * - ลายเซ็น
 * - ข้อมูลติดต่อ
 * 
 * หมายเหตุ: หนังสือส่งออกจะสร้างบันทึกข้อความ (Memo) พ่วงด้วยเสมอ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundPdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    
    @Override
    public BookType getBookType() {
        return BookType.OUTBOUND;
    }
    
    @Override
    public String getGeneratorName() {
        return "OutboundPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<PdfResult> results = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือส่งออก
        String outboundPdfBase64 = generateOutboundPdf(request);
        results.add(createMainPdfResult(outboundPdfBase64, "หนังสือส่งออก"));
        
        // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ)
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        results.add(createMemoPdfResult(memoPdfBase64, "บันทึกข้อความ (สำเนาเก็บ)"));
        
        return results;
    }
    
    /**
     * สร้าง PDF หนังสือส่งออก
     */
    private String generateOutboundPdf(GeneratePdfRequest request) throws Exception {
        // รวบรวมข้อมูล
        String bookNo = request.getBookNo();
        String address = request.getAddress() != null ? request.getAddress() : "";
        String date = request.getDateThai();
        String title = request.getBookTitle() != null ? request.getBookTitle() : "";
        
        // รวบรวมรายชื่อผู้รับ
        String recipients = "";
        String recipientsAddress = "";
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            recipients = request.getRecipients();
        } else if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            var firstLearner = request.getBookLearner().get(0);
            if (firstLearner.getPositionName() != null) {
                recipients = firstLearner.getPositionName();
            }
            if (firstLearner.getDepartmentName() != null) {
                recipientsAddress = firstLearner.getDepartmentName();
            }
        }
        
        // รวบรวมอ้างถึง
        String referTo = buildReferTo(request);
        
        // รวบรวมสิ่งที่ส่งมาด้วย
        List<String> attachments = buildAttachments(request);
        
        // รวบรวมเนื้อหา
        String content = buildContent(request);
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        // ข้อมูลติดต่อ
        ContactInfo contactInfo = buildContactInfo(request);
        
        log.info("Generating outbound - bookNo: {}, title: {}, content length: {}", 
                bookNo, title, content.length());
        
        return generatePdfInternal(bookNo, address, date, title, recipients, recipientsAddress,
                                  referTo, attachments, content, signers,
                                  request.getSalutation(), request.getSalutationEnding(), 
                                  request.getEndDoc(), contactInfo);
    }
    
    /**
     * สร้าง PDF ภายใน
     */
    private String generatePdfInternal(String bookNo,
                                       String address,
                                       String date,
                                       String title,
                                       String recipients,
                                       String recipientsAddress,
                                       String referTo,
                                       List<String> attachments,
                                       String content,
                                       List<SignerInfo> signers,
                                       String salutation,
                                       String salutationEnding,
                                       String endDoc,
                                       ContactInfo contactInfo) throws Exception {
        log.info("=== Generating outbound PDF internal ===");
        
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
                float logoBottomY = drawLogo(contentStream, document, yPosition, true);
                
                // SECTION 1: "ที่" (ซ้าย) + ที่อยู่สำนักงาน (ขวา)
                yPosition = logoBottomY - 15;
                float fieldY = yPosition;
                drawText(contentStream, "ที่", fontBold, FONT_SIZE_FIELD, MARGIN_LEFT, fieldY);
                
                // วาดที่อยู่สำนักงานทางขวา
                float addressX = PAGE_WIDTH - MARGIN_RIGHT - 180;
                float addressY = fieldY;
                
                if (address != null && !address.isEmpty()) {
                    String[] addressLines = address.split("\n");
                    for (String line : addressLines) {
                        addressY = drawText(contentStream, line.trim(), fontRegular, FONT_SIZE_FIELD_VALUE, 
                                           addressX, addressY);
                    }
                }
                
                // SECTION 2: วันที่ (ขวา ใต้ที่อยู่)
                if (date != null && !date.isEmpty()) {
                    addressY -= 5;
                    drawText(contentStream, date, fontRegular, FONT_SIZE_FIELD_VALUE, addressX, addressY);
                }
                
                yPosition = Math.min(fieldY - 30, addressY - 30);
                
                // SECTION 3: เรื่อง
                String titleValue = (title != null && !title.isEmpty()) ? title : "";
                yPosition = drawText(contentStream, "เรื่อง  " + titleValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 3.5: คำขึ้นต้น (ถ้ามี)
                if (salutation != null && !salutation.isEmpty()) {
                    String salutationLine = salutation;
                    if (salutationEnding != null && !salutationEnding.isEmpty()) {
                        salutationLine += "  " + salutationEnding;
                    }
                    yPosition = drawText(contentStream, salutationLine, fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // SECTION 4: เรียน (2 บรรทัด)
                String recipientsValue = (recipients != null && !recipients.isEmpty()) ? recipients : "";
                yPosition = drawText(contentStream, "เรียน  " + recipientsValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                
                if (recipientsAddress != null && !recipientsAddress.isEmpty()) {
                    yPosition = drawText(contentStream, "เรียน  " + recipientsAddress, fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 5: อ้างถึง
                String referToValue = (referTo != null && !referTo.isEmpty()) ? referTo : "";
                yPosition = drawText(contentStream, "อ้างถึง  " + referToValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 6: สิ่งที่ส่งมาด้วย
                if (attachments != null && !attachments.isEmpty()) {
                    if (attachments.size() == 1) {
                        yPosition = drawText(contentStream, "สิ่งที่ส่งมาด้วย  " + attachments.get(0), 
                                           fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    } else {
                        yPosition = drawText(contentStream, "สิ่งที่ส่งมาด้วย", fontRegular, 
                                           FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                        float indentX = MARGIN_LEFT + fontRegular.getStringWidth("สิ่งที่ส่งมาด้วย  ") / 1000 * FONT_SIZE_FIELD_VALUE;
                        for (int i = 0; i < attachments.size(); i++) {
                            String attachItem = convertToThaiNumber(i + 1) + ". " + attachments.get(i);
                            yPosition = drawText(contentStream, attachItem, fontRegular, 
                                               FONT_SIZE_FIELD_VALUE, indentX, yPosition);
                        }
                    }
                } else {
                    yPosition = drawText(contentStream, "สิ่งที่ส่งมาด้วย  ", fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 7: เนื้อหา
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
                
                // SECTION 8+9: ช่องลงนาม (รวม endDoc เป็น label บนกรอบ)
                if (signers != null && !signers.isEmpty()) {
                    yPosition -= SPACING_BEFORE_SIGNATURES;
                    
                    for (int i = 0; i < signers.size(); i++) {
                        SignerInfo signer = signers.get(i);
                        
                        // คำนวณความสูงที่ต้องการ (รวม label ยาวถ้ามี)
                        float requiredHeight = 100f;
                        if (i == 0 && endDoc != null && !endDoc.isEmpty()) {
                            requiredHeight += 25f; // เพิ่มพื้นที่สำหรับ label บนกรอบ
                        }
                        
                        if (yPosition < MIN_Y_POSITION + requiredHeight) {
                            contentStream.close();
                            
                            PDPage newPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, newPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        // กำหนด label: signer แรกใช้ endDoc, ที่เหลือใช้ "ช่องลงนาม"
                        String boxLabel = "ช่องลงนาม";
                        if (i == 0 && endDoc != null && !endDoc.isEmpty()) {
                            boxLabel = endDoc;
                        }
                        
                        yPosition = drawSignerBoxWithLabel(contentStream, 
                                                  signer.getPrefixName(),
                                                  signer.getFirstname(),
                                                  signer.getLastname(),
                                                  signer.getPositionName(),
                                                  fontRegular, yPosition, boxLabel);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
                // SECTION 10: ข้อมูลติดต่อ
                if (contactInfo != null && contactInfo.hasAnyInfo()) {
                    yPosition -= 20;
                    
                    if (contactInfo.getDepartment() != null && !contactInfo.getDepartment().isEmpty()) {
                        yPosition = drawText(contentStream, contactInfo.getDepartment(), 
                                           fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    }
                    if (contactInfo.getPhone() != null && !contactInfo.getPhone().isEmpty()) {
                        yPosition = drawText(contentStream, "โทร. " + contactInfo.getPhone(), 
                                           fontRegular, FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    }
                    if (contactInfo.getFax() != null && !contactInfo.getFax().isEmpty()) {
                        yPosition = drawText(contentStream, "โทรสาร " + contactInfo.getFax(), 
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
            log.error("Error generating outbound PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือส่งออกได้: " + e.getMessage(), e);
        }
    }

    // ============================================
    // Helper methods
    // ============================================
    
    private String buildReferTo(GeneratePdfRequest request) {
        if (request.getBookReferTo() == null || request.getBookReferTo().isEmpty()) {
            return "";
        }
        
        return request.getBookReferTo().stream()
            .map(ref -> {
                StringBuilder sb = new StringBuilder();
                if (ref.getBookReferToName() != null) {
                    sb.append(ref.getBookReferToName());
                }
                if (ref.getBookReferToNo() != null && !ref.getBookReferToNo().isEmpty()) {
                    sb.append(" ที่ ").append(ref.getBookReferToNo());
                }
                if (ref.getCreateDate() != null) {
                    sb.append(" ลงวันที่ ").append(formatThaiDate(ref.getCreateDate()));
                }
                return sb.toString();
            })
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("\n"));
    }
    
    private List<String> buildAttachments(GeneratePdfRequest request) {
        List<String> attachments = new ArrayList<>();
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            for (var attach : request.getAttachment()) {
                String attachName = attach.getName() != null ? attach.getName() : "";
                String attachRemark = attach.getRemark() != null ? " (" + attach.getRemark() + ")" : "";
                attachments.add(attachName + attachRemark);
            }
        }
        return attachments;
    }
    
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
    
    private String formatThaiDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear() + 543;
        return toThaiDate(day, month, year);
    }
}
