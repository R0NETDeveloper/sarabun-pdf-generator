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
import th.go.etda.sarabun.pdf.constant.SignBoxType;
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
    private final HtmlContentRenderer htmlContentRenderer;
    
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
        
        List<String> allPdfsToMerge = new ArrayList<>();
        List<String> recipientDescriptions = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือส่งออก แยกตามผู้รับแต่ละหน่วยงาน
        // ใช้ toRecipients สำหรับผู้รับภายนอก (หน่วยงาน) - New Format
        if (request.getToRecipients() != null && !request.getToRecipients().isEmpty()) {
            for (int i = 0; i < request.getToRecipients().size(); i++) {
                GeneratePdfRequest.BookRecipient recipient = request.getToRecipients().get(i);
                
                // สร้าง PDF สำหรับผู้รับแต่ละหน่วยงาน (documentIndex = i+1)
                String outboundPdfBase64 = generateOutboundPdfForRecipient(request, recipient, i + 1);
                allPdfsToMerge.add(outboundPdfBase64);
                
                String recipientName = buildRecipientName(recipient);
                recipientDescriptions.add("หนังสือส่งออก ถึง " + recipientName);
                
                log.info("Generated outbound PDF {} for: {}", i + 1, recipientName);
            }
        } else {
            // Fallback: ถ้าไม่มี toRecipients ให้สร้าง PDF เดียว (documentIndex = 1)
            String outboundPdfBase64 = generateOutboundPdf(request, 1);
            allPdfsToMerge.add(outboundPdfBase64);
            recipientDescriptions.add("หนังสือส่งออก");
        }
        
        // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ) - แค่ 1 ฉบับ
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        allPdfsToMerge.add(memoPdfBase64);
        recipientDescriptions.add("บันทึกข้อความ (สำเนาเก็บ)");
        
        log.info("Total PDFs to merge: {} ({} outbound + 1 memo)", 
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
     * สร้างชื่อผู้รับจาก BookRecipient (New Format)
     */
    private String buildRecipientName(GeneratePdfRequest.BookRecipient recipient) {
        // ใช้ salutationContent ก่อน (สำหรับแสดงใน "เรียน" เช่น "ท่านปลัดกระทรวง...")
        if (recipient.getSalutationContent() != null && !recipient.getSalutationContent().isEmpty()) {
            return recipient.getSalutationContent();
        }
        // ถ้าไม่มี ใช้ชื่อองค์กร
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            return recipient.getOrganizeName();
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
     * สร้าง PDF หนังสือส่งออกสำหรับผู้รับเฉพาะราย (หน่วยงานภายนอก)
     * อ่านข้อมูลจาก documentSub (New Format)
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateOutboundPdfForRecipient(GeneratePdfRequest request, 
                                                    GeneratePdfRequest.BookRecipient recipient,
                                                    int documentIndex) throws Exception {
        // อ่านข้อมูลจาก documentSub (New Format)
        GeneratePdfRequest.DocumentSub docSub = request.getDocumentSub();
        
        String bookNo = docSub != null ? docSub.getBookNo() : "";
        String address = docSub != null && docSub.getAddress() != null ? docSub.getAddress() : "";
        String date = docSub != null ? docSub.getDateThai() : "";
        String title = docSub != null && docSub.getBookTitle() != null ? docSub.getBookTitle() : "";
        String speedLayer = docSub != null ? docSub.getSpeedLayer() : "";
        String contact = docSub != null ? docSub.getContact() : "";
        
        // ใช้ salutationContent สำหรับ "เรียน"
        String recipients = recipient.getSalutationContent() != null ? recipient.getSalutationContent() : "";
        
        // ไม่ใช้ที่อยู่ผู้รับแล้ว
        String recipientsAddress = "";
        
        // รวบรวมอ้างถึง จาก documentSub
        String referTo = buildReferTo(docSub);
        
        // รวบรวมสิ่งที่ส่งมาด้วย จาก documentSub
        List<String> attachments = buildAttachments(docSub);
        
        // รวบรวมเนื้อหา จาก documentSub
        String content = buildContentFromDocSub(docSub);
        
        // รวบรวม HTML content (ถ้ามี)
        String htmlContent = hasHtmlContentInDocSub(docSub) ? buildHtmlContentFromDocSub(docSub) : null;
        
        // สร้าง SignerInfo จาก bookSigned (ผู้ลงนาม)
        List<SignerInfo> signers = buildSigners(request);
        
        // ข้อมูลติดต่อ จาก documentSub
        ContactInfo contactInfo = buildContactInfoFromString(contact);
        
        // ใช้ salutation, salutationContent, endDoc จาก recipient
        String salutation = recipient.getSalutation();
        String salutationContent = recipient.getSalutationContent();
        String endDoc = recipient.getEndDoc();
        
        log.info("Generating outbound for recipient: {}, salutation: {}, endDoc: {}, docIndex: {}, hasHtml: {}", 
                recipients, salutation, endDoc, documentIndex, htmlContent != null);
        
        return generatePdfInternal(bookNo, address, date, title, recipients, recipientsAddress,
                                  referTo, attachments, content, htmlContent, signers,
                                  salutation, salutationContent, 
                                  endDoc, contactInfo, speedLayer, documentIndex);
    }
    
    /**
     * สร้าง PDF หนังสือส่งออก (fallback เมื่อไม่มี toRecipients)
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
     */
    private String generateOutboundPdf(GeneratePdfRequest request, int documentIndex) throws Exception {
        // อ่านข้อมูลจาก documentSub (New Format)
        GeneratePdfRequest.DocumentSub docSub = request.getDocumentSub();
        
        String bookNo = docSub != null ? docSub.getBookNo() : "";
        String address = docSub != null && docSub.getAddress() != null ? docSub.getAddress() : "";
        String date = docSub != null ? docSub.getDateThai() : "";
        String title = docSub != null && docSub.getBookTitle() != null ? docSub.getBookTitle() : "";
        String speedLayer = docSub != null ? docSub.getSpeedLayer() : "";
        String contact = docSub != null ? docSub.getContact() : "";
        
        String recipients = "";
        String recipientsAddress = "";
        
        // รวบรวมอ้างถึง จาก documentSub
        String referTo = buildReferTo(docSub);
        
        // รวบรวมสิ่งที่ส่งมาด้วย จาก documentSub
        List<String> attachments = buildAttachments(docSub);
        
        // รวบรวมเนื้อหา จาก documentSub
        String content = buildContentFromDocSub(docSub);
        
        // รวบรวม HTML content (ถ้ามี)
        String htmlContent = hasHtmlContentInDocSub(docSub) ? buildHtmlContentFromDocSub(docSub) : null;
        
        // รวบรวมผู้ลงนาม
        List<SignerInfo> signers = buildSigners(request);
        
        // ข้อมูลติดต่อ จาก documentSub
        ContactInfo contactInfo = buildContactInfoFromString(contact);
        
        log.info("Generating outbound - bookNo: {}, title: {}, content length: {}, hasHtml: {}, docIndex: {}", 
                bookNo, title, content.length(), htmlContent != null, documentIndex);
        
        return generatePdfInternal(bookNo, address, date, title, recipients, recipientsAddress,
                                  referTo, attachments, content, htmlContent, signers,
                                  null, null, 
                                  null, contactInfo, speedLayer, documentIndex);
    }
    
    /**
     * สร้าง PDF ภายใน
     * @param documentIndex ลำดับเอกสาร (1, 2, 3...) สำหรับสร้าง unique field name
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
                                       String htmlContent,
                                       List<SignerInfo> signers,
                                       String salutation,
                                       String salutationEnding,
                                       String endDoc,
                                       ContactInfo contactInfo,
                                       String speedLayer,
                                       int documentIndex) throws Exception {
        log.info("=== Generating outbound PDF internal, hasHtmlContent: {} ===", htmlContent != null && !htmlContent.isEmpty());
        
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
                float logoBottomY = drawLogo(contentStream, document, yPosition, LogoPosition.RIGHT);
                
                // วาด Speed Layer (ถ้ามี)
                drawSpeedLayer(contentStream, speedLayer, fontBold, yPosition, LogoPosition.RIGHT);
                
                // SECTION 1: "ที่" (ซ้าย) + ที่อยู่สำนักงาน (ขวา)
                yPosition = logoBottomY - 15;
                float fieldY = yPosition;
                drawText(contentStream, "ที่", fontBold, FONT_SIZE_FIELD, MARGIN_LEFT, fieldY);
                
                // วาดที่อยู่สำนักงานทางขวา
                float addressX = PAGE_WIDTH - MARGIN_RIGHT - 180;
                float addressY = fieldY;
                
                if (address != null && !address.isEmpty()) {
                    // แปลง literal \n จาก JSON เป็น newline จริง
                    String cleanAddress = address.replace("\\n", "\n");
                    String[] addressLines = cleanAddress.split("\n");
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
                
                // SECTION 4: เรียน (ใช้ salutation + salutationContent หรือ recipients)
                String salutationLabel = (salutation != null && !salutation.isEmpty()) ? salutation : "เรียน";
                String salutationValue = "";
                
                // ใช้ salutationContent ก่อน (สำหรับผู้รับเฉพาะราย เช่น "ท่านปลัดกระทรวง...")
                if (salutationEnding != null && !salutationEnding.isEmpty()) {
                    salutationValue = salutationEnding;
                } else if (recipients != null && !recipients.isEmpty()) {
                    // fallback ใช้ recipients
                    salutationValue = recipients;
                }
                
                yPosition = drawText(contentStream, salutationLabel + "  " + salutationValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // SECTION 5: อ้างถึง (รองรับหลายรายการ - ขึ้นบรรทัดใหม่พร้อม indent)
                if (referTo != null && !referTo.isEmpty()) {
                    String[] referToLines = referTo.split("\n");
                    if (referToLines.length == 1) {
                        // มี 1 รายการ - แสดงต่อท้าย "อ้างถึง"
                        yPosition = drawText(contentStream, "อ้างถึง  " + referToLines[0], fontRegular, 
                                           FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    } else {
                        // มีหลายรายการ - แสดง "อ้างถึง" แล้วขึ้นบรรทัดใหม่พร้อม indent
                        yPosition = drawText(contentStream, "อ้างถึง", fontRegular, 
                                           FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                        float indentX = MARGIN_LEFT + fontRegular.getStringWidth("อ้างถึง  ") / 1000 * FONT_SIZE_FIELD_VALUE;
                        for (int i = 0; i < referToLines.length; i++) {
                            String referItem = convertToThaiNumber(i + 1) + ". " + referToLines[i];
                            yPosition = drawText(contentStream, referItem, fontRegular, 
                                               FONT_SIZE_FIELD_VALUE, indentX, yPosition);
                        }
                    }
                } else {
                    yPosition = drawText(contentStream, "อ้างถึง  ", fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                }
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
                
                // SECTION 7: เนื้อหา (รองรับทั้ง plain text และ HTML mixed content)
                PDPage currentPage = page; // track หน้าปัจจุบัน
                
                // วาด plain text content ก่อน (ถ้ามี)
                if (content != null && !content.isEmpty()) {
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
                
                // SECTION 7.5: วาด HTML content (ทั้งข้อความและตารางผสมกัน)
                if (htmlContent != null && !htmlContent.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    
                    // ใช้ drawMixedHtmlContent เพื่อวาดทั้งข้อความและตารางตามลำดับ
                    ContentContext ctx = drawMixedHtmlContent(document, currentPage, contentStream,
                            htmlContent, fontRegular, fontBold, MARGIN_LEFT, yPosition,
                            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT, bookNo);
                    
                    contentStream = ctx.getContentStream();
                    currentPage = ctx.getCurrentPage();
                    yPosition = ctx.getYPosition();
                    
                    log.info("Mixed HTML content drawn, new yPosition: {}", yPosition);
                }
                
                // SECTION 8+9: ช่องลงนาม (เจาะ Signature Field จริง + รวม endDoc เป็น label บนกรอบ)
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
                            
                            currentPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, currentPage, 
                                    PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        // กำหนด label: signer แรกใช้ endDoc (คำลงท้าย), ที่เหลือใช้ SignBoxType.LEARNER
                        String boxLabel = SignBoxType.LEARNER;
                        boolean isEndDocLabel = false;
                        if (i == 0 && endDoc != null && !endDoc.isEmpty()) {
                            boxLabel = endDoc;
                            isEndDocLabel = true; // endDoc ต้องแสดงเหนือกรอบ
                        }
                        
                        yPosition = drawSignerBoxWithSignatureField(document, currentPage, 
                                                  contentStream, signer, fontRegular, yPosition,
                                                  "Learner", documentIndex, i, boxLabel, isEndDocLabel);
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
                // SECTION 10: ข้อมูลติดต่อ
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
                }
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            String pdfBase64 = convertToBase64(document);
            
            // NOTE: HTML tables are now drawn inline in SECTION 7.5
            // No need to append as separate pages
            
            return pdfBase64;
            
        } catch (Exception e) {
            log.error("Error generating outbound PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือส่งออกได้: " + e.getMessage(), e);
        }
    }

    // ============================================
    // Helper methods
    // ============================================
    
    /**
     * รวบรวมอ้างถึงจาก documentSub (New Format)
     */
    private String buildReferTo(GeneratePdfRequest.DocumentSub docSub) {
        if (docSub == null || docSub.getBookReferTo() == null || docSub.getBookReferTo().isEmpty()) {
            return "";
        }
        
        return docSub.getBookReferTo().stream()
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
    
    /**
     * รวบรวมสิ่งที่ส่งมาด้วยจาก documentSub (New Format)
     */
    private List<String> buildAttachments(GeneratePdfRequest.DocumentSub docSub) {
        List<String> attachments = new ArrayList<>();
        if (docSub != null && docSub.getAttachment() != null && !docSub.getAttachment().isEmpty()) {
            for (var attach : docSub.getAttachment()) {
                String attachName = attach.getName() != null ? attach.getName() : "";
                String attachRemark = attach.getRemark() != null ? " " + attach.getRemark() : "";
                attachments.add(attachName + attachRemark);
            }
        }
        return attachments;
    }
    
    /**
     * รวบรวมเนื้อหาจาก documentSub (New Format)
     * สำหรับ plain text rendering (ใช้ drawMultilineText)
     */
    private String buildContentFromDocSub(GeneratePdfRequest.DocumentSub docSub) {
        if (docSub == null || docSub.getBookContent() == null || docSub.getBookContent().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (var bc : docSub.getBookContent()) {
            // ถ้าเป็น HTML content ให้ skip (จะ render แยก)
            if (bc.isHtmlContent()) {
                continue;
            }
            
            // ใช้ contentTitle และ content (New Format)
            if (bc.getContentTitle() != null && !bc.getContentTitle().isEmpty()) {
                sb.append(bc.getContentTitle()).append("\n");
            }
            if (bc.getContent() != null && !bc.getContent().isEmpty()) {
                String content = bc.getContent();
                // แปลง HTML เป็น plain text
                if (HtmlUtils.isHtml(content)) {
                    content = HtmlUtils.htmlToPlainText(content);
                }
                sb.append(content).append("\n");
            }
        }
        return sb.toString().trim();
    }
    
    /**
     * ตรวจสอบว่า documentSub มี HTML content หรือไม่
     */
    private boolean hasHtmlContentInDocSub(GeneratePdfRequest.DocumentSub docSub) {
        if (docSub == null || docSub.getBookContent() == null) {
            return false;
        }
        for (var bc : docSub.getBookContent()) {
            if (bc.isHtmlContent()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * สร้าง HTML content จาก documentSub (สำหรับ HTML rendering)
     */
    private String buildHtmlContentFromDocSub(GeneratePdfRequest.DocumentSub docSub) {
        if (docSub == null || docSub.getBookContent() == null || docSub.getBookContent().isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        for (var bc : docSub.getBookContent()) {
            // เฉพาะ HTML content
            if (!bc.isHtmlContent()) {
                continue;
            }
            
            if (bc.getContentTitle() != null && !bc.getContentTitle().isEmpty()) {
                html.append("<p><strong>").append(bc.getContentTitle()).append("</strong></p>\n");
            }
            if (bc.getContent() != null && !bc.getContent().isEmpty()) {
                html.append(bc.getContent()).append("\n");
            }
        }
        return html.toString().trim();
    }
    
    /**
     * สร้างรายการผู้รับ (bookLearner) สำหรับเจาะช่องลงนามในหนังสือหลัก
     * โดย signBoxType = "เรียน"
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
    
    /**
     * สร้าง ContactInfo จาก string (New Format)
     */
    private ContactInfo buildContactInfoFromString(String contact) {
        ContactInfo info = new ContactInfo();
        
        if (contact != null && !contact.isEmpty()) {
            // แปลง HTML เป็น plain text ก่อน (ถ้าเป็น HTML)
            String contactText = contact;
            if (HtmlUtils.isHtml(contactText)) {
                contactText = HtmlUtils.htmlToPlainText(contactText);
            }
            
            // เก็บเป็น rawContact เพื่อแสดงตาม format ที่ส่งมา
            info.setRawContact(contactText);
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
