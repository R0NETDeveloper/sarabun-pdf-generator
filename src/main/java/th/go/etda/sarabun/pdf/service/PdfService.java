package th.go.etda.sarabun.pdf.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Core PDF Service สำหรับการสร้างและจัดการ PDF โดยใช้ Apache PDFBox
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Service/PdfService.cs
 * 
 * การเปลี่ยนแปลงสำคัญ:
 * 1. แทนที่ iText7 ด้วย Apache PDFBox (ฟรี 100%)
 * 2. ใช้ PDType0Font สำหรับโหลด TrueType fonts (TH Sarabun)
 * 3. รองรับ UTF-8 และ Thai language
 * 4. ใช้ Java NIO สำหรับจัดการไฟล์
 * 
 * PDFBox เป็น open-source library จาก Apache:
 * - License: Apache License 2.0 (ฟรี 100%)
 * - รองรับ PDF 1.7 และ 2.0
 * - รองรับ Unicode และ Thai fonts
 * - Cross-platform (Windows, Linux, Mac)
 * 
 * @author Migrated from .NET to Java
 */
@Slf4j
@Service
public class PdfService {
    
    private static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    private static final String FONT_BOLD_PATH = "fonts/THSarabunNew Bold.ttf";
    
    // ============================================
    // ค่าคงที่สำหรับปรับแต่ง Layout
    // ============================================
    
    // ขนาดหน้ากระดาษ A4 (595 x 842 points)
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();   // 595 pt
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 842 pt
    
    // ⚙️ Margins - ระยะขอบกระดาษ (ปรับได้)
    // เพิ่มค่า = เนื้อหาห่างจากขอบมากขึ้น
    private static final float MARGIN_TOP = 70f;      // ขอบบน (เพิ่ม = เนื้อหาเลื่อนลง)
    private static final float MARGIN_BOTTOM = 70f;   // ขอบล่าง
    private static final float MARGIN_LEFT = 70f;     // ขอบซ้าย (เพิ่ม = เนื้อหาเลื่อนขวา)
    private static final float MARGIN_RIGHT = 70f;    // ขอบขวา
    
    // ⚙️ Logo Settings - ขนาดและตำแหน่งโลโก้ (ปรับได้)
    private static final float LOGO_WIDTH = 120f;      // ความกว้างโลโก้ (เพิ่ม = โลโก้ใหญ่ขึ้น)
    private static final float LOGO_HEIGHT = 40f;     // ความสูงโลโก้
    private static final float LOGO_SPACING = 30f;    // ระยะห่างหลังโลโก้ (เพิ่ม = เนื้อหาเลื่อนลงมากขึ้น)
    
    // ⚙️ Font Sizes - ขนาดฟอนต์ (ปรับได้)
    private static final float FONT_SIZE_HEADER = 24f;        // หัวข้อ "บันทึกข้อความ"
    private static final float FONT_SIZE_FIELD = 18f;         // ฟิลด์ label (ส่วนราชการ, ที่, วันที่, เรื่อง)
    private static final float FONT_SIZE_FIELD_VALUE = 16f;   // ฟิลด์ value (ข้อความ model)
    private static final float FONT_SIZE_CONTENT = 14f;       // เนื้อหา
    private static final float FONT_SIZE_SIGNATURE = 14f;     // ลายเซ็น
    
    // ⚙️ Vertical Spacing - ระยะห่างระหว่างแต่ละบรรทัด (ปรับได้)
    private static final float SPACING_AFTER_HEADER = 30f;  // หลัง "บันทึกข้อความ"
    private static final float SPACING_BETWEEN_FIELDS = 5f; // ระหว่างฟิลด์ต่างๆ (ลดจาก 25f)
    private static final float SPACING_BEFORE_CONTENT = 14f; // ก่อนเนื้อหา (แยกจากฟิลด์)
    private static final float SPACING_BEFORE_SIGNATURES = 40f; // ก่อนลายเซ็น
    private static final float SPACING_BETWEEN_SIGNATURES = 20f; // ระหว่างลายเซ็น
    
    // ⚙️ Field Positions - ตำแหน่งแนวนอนของฟิลด์ต่างๆ (ปรับได้)
    private static final float DATE_X_POSITION = PAGE_WIDTH - 320; // ตำแหน่ง "วันที่" (ขวามือ)
    
    // ⚙️ Signature Settings - ตำแหน่งและขนาดลายเซ็น (ปรับได้)
    private static final float CLOSING_TEXT_X = PAGE_WIDTH - MARGIN_RIGHT - 170; // ตำแหน่ง "จึงเรียนมา..." (เพิ่ม = เลื่อนขวา)
    private static final float CLOSING_TEXT_Y_OFFSET = 30f; // ระยะห่างก่อนลายเซ็น (เพิ่ม = เว้นช่องมากขึ้น)
    private static final float SIGNATURE_PLACEHOLDER_X = PAGE_WIDTH - MARGIN_RIGHT - 120; // ตำแหน่ง "(ลายเซ็น)" - ชิดขวามาก
    private static final float SIGNATURE_NAME_X = PAGE_WIDTH - MARGIN_RIGHT - 140; // ตำแหน่งชื่อผู้ลงนาม
    private static final float SIGNATURE_POSITION_X = CLOSING_TEXT_X; // ตำแหน่งตำแหน่งงาน - กึ่งกลางแบบ "จึงเรียนมา..."
    private static final float SIGNATURE_BOX_WIDTH = 150f; // ความกว้างกรอบลายเซ็น
    private static final float SIGNATURE_BOX_HEIGHT = 60f; // ความสูงกรอบลายเซ็น
    
    // ⚙️ Multi-page Settings - การจัดการหลายหน้า (ปรับได้)
    private static final float MIN_Y_POSITION = MARGIN_BOTTOM + 100; // พื้นที่ขั้นต่ำก่อนขึ้นหน้าใหม่ (เพิ่ม = ขึ้นหน้าเร็วขึ้น)
    private static final float PAGE_NUMBER_Y_OFFSET = 15f; // ระยะห่างหมายเลขหน้าจากขอบบน
    
    // ⚙️ Debug Mode - แสดงเส้นขอบสีแดงเพื่อ debug margins (เปลี่ยน true/false)
    private static final boolean ENABLE_DEBUG_BORDERS = false; // true = แสดงเส้นขอบสีแดง, false = ซ่อน
    
    /**
     * วาดเส้นขอบสีแดงเพื่อแสดง margins (สำหรับ debug)
     * 
     * @param stream PDPageContentStream
     */
    
    /**
     * วาดเส้นขอบสีแดงเพื่อแสดง margins (สำหรับ debug)
     * เส้นสี่เหลี่ยมแสดงกรอบสี่เหลี่ยมขอบกระดาษ: บน, ล่าง, ซ้าย, ขวา
     * 
     * @param stream PDPageContentStream
     */
    private void drawDebugBorders(PDPageContentStream stream) throws IOException {
        if (!ENABLE_DEBUG_BORDERS) {
            return; // ถ้าปิดโหมด debug ไม่ต้องวาด
        }
        
        stream.setStrokingColor(java.awt.Color.RED);
        stream.setLineWidth(0.5f);
        
        // วาดกรอบสี่เหลี่ยมแสดงขอบเขตเนื้อหา (ภายใน margins)
        // มุมซ้ายล่าง (x, y), ความกว้าง, ความสูง
        stream.addRect(
            MARGIN_LEFT,                           // x (ขอบซ้าย)
            MARGIN_BOTTOM,                         // y (ขอบล่าง)
            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT,  // width (ความกว้างพื้นที่เนื้อหา)
            PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM  // height (ความสูงพื้นที่เนื้อหา)
        );
        stream.stroke();
        
        log.debug("Debug borders drawn: Left={}, Right={}, Top={}, Bottom={}", 
                 MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);
    }
    
    /**
     * สร้างหน้าใหม่และวาดหมายเลขหน้า + เลขที่หนังสือ
     * 
     * @param document PDF document
     * @param fontRegular ฟอนต์สำหรับหมายเลขหน้า
     * @param bookNo เลขที่หนังสือ (แสดงที่ขอบล่างซ้าย)
     * @return PDPage หน้าใหม่ที่สร้าง
     */
    private PDPage createNewPage(PDDocument document, PDFont fontRegular, String bookNo) throws IOException {
        PDPage newPage = new PDPage(PDRectangle.A4);
        document.addPage(newPage);
        
        // วาดหมายเลขหน้าและเลขที่หนังสือทันทีหลังสร้างหน้า
        int pageNumber = document.getNumberOfPages();
        try (PDPageContentStream stream = new PDPageContentStream(document, newPage)) {
            drawPageNumber(stream, pageNumber, fontRegular);
            drawBookNumber(stream, bookNo, fontRegular);
            drawDebugBorders(stream); // วาดเส้นขอบ debug
        }
        
        log.info("Created new page {}", pageNumber);
        return newPage;
    }
    
    /**
     * วาดหมายเลขหน้าที่กลางบน (รูปแบบเลขไทย: -๑, -๒, -๓)
     * 
     * @param stream Content stream
     * @param pageNumber หมายเลขหน้า
     * @param font ฟอนต์ที่ใช้
     */
    private void drawPageNumber(PDPageContentStream stream, int pageNumber, PDFont font) throws IOException {
        String thaiPageNumber = convertToThaiNumber(pageNumber);
        String pageText = "-" + thaiPageNumber;
        
        // คำนวณตำแหน่งกลาง
        float textWidth = font.getStringWidth(pageText) / 1000 * FONT_SIZE_CONTENT;
        float x = (PAGE_WIDTH - textWidth) / 2; // ตรงกลางหน้า
        float y = PAGE_HEIGHT - MARGIN_TOP + PAGE_NUMBER_Y_OFFSET;
        
        stream.beginText();
        stream.setFont(font, FONT_SIZE_CONTENT);
        stream.newLineAtOffset(x, y);
        stream.showText(pageText);
        stream.endText();
        
        log.debug("Drew page number: {}", pageText);
    }
    
    /**
     * วาดเลขที่หนังสือที่ขอบล่างซ้าย (ทุกหน้า)
     * 
     * @param stream Content stream
     * @param bookNo เลขที่หนังสือ
     * @param font ฟอนต์ที่ใช้
     */
    private void drawBookNumber(PDPageContentStream stream, String bookNo, PDFont font) throws IOException {
        if (bookNo == null || bookNo.isEmpty()) {
            return; // ถ้าไม่มีเลขที่หนังสือ ไม่ต้องวาด
        }
        
        float x = MARGIN_LEFT - 20; // ตำแหน่งซ้าย
        float y = MARGIN_BOTTOM - 30; // ตำแหน่งด้านล่าง
        
        stream.beginText();
        stream.setFont(font, FONT_SIZE_FIELD_VALUE);
        stream.newLineAtOffset(x, y);
        stream.showText(bookNo);
        stream.endText();
        
        log.debug("Drew book number: {}", bookNo);
    }
    
    /**
     * สร้างหนังสือบันทึกข้อความทางราชการ
     * 
     * แปลงมาจาก: GenerateOfficialMemoPdfAsync() method
     * 
     * @param govName ชื่อหน่วยงาน
     * @param date วันที่ (รูปแบบไทย)
     * @param bookNo เลขที่หนังสือ
     * @param title หัวเรื่อง
     * @param recipients รายชื่อผู้รับ
     * @param content เนื้อหาเอกสาร
     * @param speedLayer ชั้นความเร็ว
     * @param formatPdf รูปแบบ PDF
     * @param signatures รายการผู้ลงนาม
     * @return PDF ในรูปแบบ Base64
     */
    public String generateOfficialMemoPdf(String govName,
                                         String date,
                                         String bookNo,
                                         String title,
                                         String recipients,
                                         String content,
                                         String speedLayer,
                                         String formatPdf,
                                         List<String> signatures) throws Exception {
        log.info("=== Generating official memo PDF ===");
        log.info("govName: {}", govName);
        log.info("date: {}", date);
        log.info("bookNo: {}", bookNo);
        log.info("title: {}", title);
        log.info("recipients: {}", recipients);
        log.info("content length: {}", content != null ? content.length() : 0);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            log.info("PDF page created");
            
            // โหลด fonts
            log.info("Loading fonts...");
            PDFont fontRegular = loadThaiFont(document, FONT_PATH);
            PDFont fontBold = loadThaiFont(document, FONT_BOLD_PATH);
            log.info("Fonts loaded successfully");
            
            // สร้าง content stream (ไม่ใช้ try-with-resources เพราะต้อง reassign เมื่อขึ้นหน้าใหม่)
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                log.info("Content stream created, starting to draw...");
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // ============================================
                // 📍 หมายเลขหน้า (กลางบน) - ตามมาตรฐานเอกสารราชการ (เลขไทย)
                // หน้าแรก: -๑, หน้าที่สอง: -๒, หน้าที่สาม: -๓
                // ============================================
                int pageNumber = document.getNumberOfPages();
                String thaiPageNumber = convertToThaiNumber(pageNumber);
                String pageNumberText = "-" + thaiPageNumber;
                
                // คำนวณตำแหน่งกลาง
                float textWidth = fontRegular.getStringWidth(pageNumberText) / 1000 * FONT_SIZE_CONTENT;
                float pageNumX = (PAGE_WIDTH - textWidth) / 2; // ตรงกลางหน้า
                float pageNumY = PAGE_HEIGHT - MARGIN_TOP + PAGE_NUMBER_Y_OFFSET;
                
                drawText(contentStream, pageNumberText, fontRegular, FONT_SIZE_CONTENT, pageNumX, pageNumY);
                
                // วาดเลขที่หนังสือในหน้าแรกด้วย (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาดเส้นขอบ debug (ถ้าเปิด)
                drawDebugBorders(contentStream);
                
                // วาดเส้นขอบ debug (ถ้าเปิด)
                drawDebugBorders(contentStream);
                
                // ============================================
                // 📍 SECTION 0: Logo ETDA (ซ้ายบน)
                // ปรับแต่งได้ที่: LOGO_WIDTH, LOGO_HEIGHT, LOGO_SPACING
                // ============================================
                try {
                    InputStream logoStream = getClass().getClassLoader()
                        .getResourceAsStream("images/logoETDA.png");
                    if (logoStream != null) {
                        PDImageXObject logoImage = PDImageXObject.createFromByteArray(
                            document, logoStream.readAllBytes(), "logo");
                        
                        // 🎨 ตำแหน่งโลโก้ (ปรับได้)
                        float logoX = MARGIN_LEFT; // ซ้ายมือ
                        // หรือใช้: (PAGE_WIDTH - LOGO_WIDTH) / 2 = ตรงกลาง
                        // หรือใช้: PAGE_WIDTH - MARGIN_RIGHT - LOGO_WIDTH = ขวามือ
                        
                        float logoY = yPosition - LOGO_HEIGHT;
                        
                        contentStream.drawImage(logoImage, logoX, logoY, LOGO_WIDTH, LOGO_HEIGHT);
                        log.info("ETDA logo drawn at ({}, {}), size: {}x{}", 
                                logoX, logoY, LOGO_WIDTH, LOGO_HEIGHT);
                        logoStream.close();
                    }
                } catch (Exception e) {
                    log.warn("Could not load ETDA logo: {}", e.getMessage());
                }
                
                // เว้นระยะหลังโลโก้
                yPosition -= LOGO_SPACING;
                
                // ============================================
                // 📍 SECTION 1: หัวข้อ "บันทึกข้อความ" (ตรงกลาง, Bold)
                // ปรับแต่งได้ที่: FONT_SIZE_HEADER, SPACING_AFTER_HEADER
                // ============================================
                log.info("Drawing header: บันทึกข้อความ");
                yPosition = drawCenteredText(contentStream, "บันทึกข้อความ", 
                                            fontBold, FONT_SIZE_HEADER, yPosition);
                yPosition -= SPACING_AFTER_HEADER;
                
                // ============================================
                // 📍 SECTION 2: ส่วนราชการ (พร้อมเส้นใต้)
                // ปรับแต่งได้ที่: FONT_SIZE_FIELD, SPACING_BETWEEN_FIELDS, UNDERLINE_LENGTH
                // ============================================
                if (govName != null && !govName.isEmpty()) {
                    log.info("Drawing department: {}", govName);
                    yPosition = drawFieldWithUnderline(contentStream, "ส่วนราชการ", govName, 
                                                 fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                                 MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // ============================================
                // 📍 SECTION 3: ที่ และ วันที่ (ในบรรทัดเดียวกัน)
                // ปรับแต่งได้ที่: DATE_X_POSITION
                // ============================================
                float fieldStartY = yPosition;
                
                // 🎨 "ที่" ทางซ้าย (พร้อมจุดไข่ปลา)
                String referenceNumber = bookNo != null ? bookNo : ""; // เลขที่หนังสือ
                // จุดไข่ปลายาวถึงตำแหน่งก่อน "วันที่" (DATE_X_POSITION - 20)
                float maxUnderlineForRef = DATE_X_POSITION - 20;
                yPosition = drawFieldWithUnderlineCustomWidth(contentStream, "ที่", referenceNumber, 
                                   fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                   MARGIN_LEFT, yPosition, maxUnderlineForRef);
                
                // 🎨 "วันที่" ตามตำแหน่งที่กำหนด (ขวามือ พร้อมจุดไข่ปลา)
                if (date != null && !date.isEmpty()) {
                    log.info("Drawing date: {} at X={}", date, DATE_X_POSITION);
                    // จุดไข่ปลายาวถึงขอบขวา (PAGE_WIDTH - MARGIN_RIGHT)
                    float maxUnderlineForDate = PAGE_WIDTH - MARGIN_RIGHT;
                    drawFieldWithUnderlineCustomWidth(contentStream, "วันที่", date, 
                            fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                            DATE_X_POSITION, fieldStartY, maxUnderlineForDate);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // ============================================
                // 📍 SECTION 4: เรื่อง (พร้อมเส้นใต้)
                // ============================================
                if (title != null && !title.isEmpty()) {
                    log.info("Drawing subject: {}", title);
                    yPosition = drawFieldWithUnderline(contentStream, "เรื่อง", title, 
                                                 fontBold, fontRegular, FONT_SIZE_FIELD, FONT_SIZE_FIELD_VALUE, 
                                                 MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // ============================================
                // 📍 SECTION 5: เรียน (ฟอนต์ธรรมดา ไม่มีเส้นใต้ + รองรับขึ้นบรรทัดใหม่พร้อม indent)
                // ============================================
                if (recipients != null && !recipients.isEmpty()) {
                    log.info("Drawing recipients: {}", recipients);
                    
                    // วาด "เรียน" + ชื่อผู้รับ (บรรทัดที่ขึ้นใหม่จะเยื้องหลัง "เรียน  ")
                    String recipientsText = "เรียน  " + recipients;
                    yPosition = drawMultilineTextWithIndent(contentStream, recipientsText, 
                                        fontRegular, FONT_SIZE_FIELD_VALUE, 
                                        MARGIN_LEFT, yPosition,
                                        PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT,
                                        "เรียน  "); // indent ตามความกว้างของ "เรียน  "
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // ============================================
                // 📍 SECTION 6: เนื้อหา (รองรับหลายบรรทัด + ขึ้นหน้าใหม่อัตโนมัติ)
                // ปรับแต่งได้ที่: FONT_SIZE_CONTENT, SPACING_BEFORE_CONTENT
                // ============================================
                if (content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT; // เว้นระยะห่างก่อนเนื้อหา
                    log.info("Drawing content, length: {}", content.length());
                    
                    // แยกเนื้อหาเป็นบรรทัด
                    String[] lines = content.split("\n");
                    
                    for (String line : lines) {
                        // เช็คว่าพอดีหรือไม่ก่อนวาดแต่ละบรรทัด
                        if (yPosition < MIN_Y_POSITION) {
                            log.info("Content overflow, creating new page...");
                            contentStream.close();
                            
                            PDPage newPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50; // เริ่มหน้าใหม่
                        }
                        
                        yPosition = drawMultilineText(contentStream, line, 
                                                    fontRegular, FONT_SIZE_CONTENT, 
                                                    MARGIN_LEFT, yPosition, 
                                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                    }
                }
                
                // ============================================
                // 📍 SECTION 7: ลายเซ็น (ไม่วาดกรอบ - ใช้ช่องที่เจาะไว้แล้ว + ขึ้นหน้าใหม่ถ้าจำเป็น)
                // ============================================
                if (signatures != null && !signatures.isEmpty()) {
                    log.info("Drawing {} signatures (text only)", signatures.size());
                    
                    // เช็คว่ามีพื้นที่พอสำหรับลายเซ็นหรือไม่ (ต้องการอย่างน้อย 150 points)
                    if (yPosition < MIN_Y_POSITION + 150) {
                        log.info("Not enough space for signatures, creating new page...");
                        contentStream.close();
                        
                        PDPage newPage = createNewPage(document, fontRegular, bookNo);
                        contentStream = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true);
                        yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                    }
                    
                    yPosition -= SPACING_BEFORE_SIGNATURES;
                    
                    // วางข้อความปิดท้าย
                    yPosition = drawText(contentStream, "จึงเรียนมาเพื่อทราบและพิจารณา", 
                                       fontRegular, FONT_SIZE_CONTENT, 
                                       CLOSING_TEXT_X, yPosition);
                    yPosition -= CLOSING_TEXT_Y_OFFSET;
                    
                    // วาดลายเซ็นแต่ละคน แบบแยกเป็นบรรทัด: (ลายเซ็น), ชื่อ, ตำแหน่ง
                    for (String signature : signatures) {
                        // แยกข้อมูลลายเซ็นออกเป็นชื่อและตำแหน่ง (คาดว่า format: ชื่อ\nตำแหน่ง)
                        String[] parts = signature.split("\\n");
                        
                        // วาด "(ลายเซ็น)" - ชิดขวามาก
                        yPosition = drawText(contentStream, "(ลายเซ็น)",
                                           fontRegular, FONT_SIZE_SIGNATURE,
                                           SIGNATURE_PLACEHOLDER_X, yPosition);
                        yPosition -= 20f;
                        
                        // วาดชื่อ (ส่วนแรก) - ชิดขวาปานกลาง
                        if (parts.length > 0) {
                            yPosition = drawText(contentStream, parts[0],
                                               fontRegular, FONT_SIZE_SIGNATURE,
                                               SIGNATURE_NAME_X, yPosition);
                            yPosition -= 20f;
                        }
                        
                        // วาดตำแหน่ง (ส่วนที่สอง) - ชิดซ้ายกว่า
                        if (parts.length > 1) {
                            yPosition = drawText(contentStream, parts[1],
                                               fontRegular, FONT_SIZE_SIGNATURE,
                                               SIGNATURE_POSITION_X, yPosition);
                        }
                        
                        yPosition -= SPACING_BETWEEN_SIGNATURES;
                    }
                }
                
                // ============================================
                // 📍 SECTION 8: เลขที่หนังสือถูกวาดแล้วในแต่ละหน้า
                // ไม่ต้องวาดซ้ำที่นี่เพราะวาดไปแล้วใน:
                // - หน้าแรก: หลังวาด page number (บรรทัด ~254)
                // - หน้าอื่น ๆ: ใน createNewPage method
                // ============================================
                
                log.info("All content drawn successfully");
                
            } finally {
                // ปิด content stream
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            // แปลงเป็น Base64
            log.info("Converting to Base64...");
            String base64 = convertToBase64(document);
            log.info("PDF generated successfully, Base64 length: {}", base64.length());
            return base64;
            
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF ได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * เพิ่มฟิลด์ลายเซ็นลงใน PDF
     * 
     * แปลงมาจาก: AddMultipleSignatureFields() method
     * 
     * @param inputFile ไฟล์ PDF input
     * @param outputFile ไฟล์ PDF output
     * @param signatureFields รายการฟิลด์ลายเซ็น
     */
    public void addSignatureFields(File inputFile, 
                                  File outputFile,
                                  List<GeneratePdfService.SignatureFieldInfo> signatureFields) throws Exception {
        log.debug("Adding {} signature fields to PDF", signatureFields.size());
        
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(inputFile)) {
            
            // ถ้าไม่มีหน้า ให้สร้างหน้าใหม่
            if (document.getNumberOfPages() == 0) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            
            // เพิ่มลายเซ็นในหน้าสุดท้าย
            PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
            PDFont font = loadThaiFont(document, FONT_PATH);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, lastPage, PDPageContentStream.AppendMode.APPEND, true)) {
                
                float yPosition = 200; // เริ่มจากด้านล่างของหน้า
                
                for (GeneratePdfService.SignatureFieldInfo field : signatureFields) {
                    // วาดข้อความลายเซ็น
                    drawText(contentStream, 
                           field.getType() + " " + field.getName(), 
                           font, 12, MARGIN_LEFT, yPosition);
                    
                    if (field.getPosition() != null) {
                        yPosition -= 15;
                        drawText(contentStream, field.getPosition(), font, 10, 
                               MARGIN_LEFT + 20, yPosition);
                    }
                    
                    yPosition -= 30;
                    
                    // ถ้า yPosition ต่ำเกินไป ให้เพิ่มหน้าใหม่
                    if (yPosition < MARGIN_BOTTOM) {
                        contentStream.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        document.addPage(newPage);
                        
                        try (PDPageContentStream newContentStream = new PDPageContentStream(
                                document, newPage)) {
                            yPosition = PAGE_HEIGHT - MARGIN_TOP;
                        }
                        break;
                    }
                }
            }
            
            document.save(outputFile);
            log.debug("Signature fields added successfully");
            
        } catch (Exception e) {
            log.error("Error adding signature fields: ", e);
            throw new Exception("ไม่สามารถเพิ่มลายเซ็นได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * ตรวจสอบและเพิ่มเลขหน้า
     * 
     * แปลงมาจาก: CheckAndAddPageNumbers() method
     */
    public void addPageNumbers(File inputFile, File outputFile) throws Exception {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(inputFile)) {
            PDFont font = loadThaiFont(document, FONT_PATH);
            
            int totalPages = document.getNumberOfPages();
            
            for (int i = 0; i < totalPages; i++) {
                PDPage page = document.getPage(i);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    
                    String pageNumber = String.format("หน้า %d/%d", i + 1, totalPages);
                    float textWidth = font.getStringWidth(pageNumber) / 1000 * 10;
                    float xPosition = (PAGE_WIDTH - textWidth) / 2;
                    
                    drawText(contentStream, pageNumber, font, 10, xPosition, MARGIN_BOTTOM - 20);
                }
            }
            
            document.save(outputFile);
            
        } catch (Exception e) {
            log.error("Error adding page numbers: ", e);
            throw new Exception("ไม่สามารถเพิ่มเลขหน้าได้: " + e.getMessage(), e);
        }
    }
    
    // ===== Helper Methods =====
    
    /**
     * โหลด Thai font จาก resources
     */
    private PDFont loadThaiFont(PDDocument document, String fontPath) throws Exception {
        try {
            log.debug("Loading Thai font from: {}", fontPath);
            ClassPathResource resource = new ClassPathResource(fontPath);
            
            if (!resource.exists()) {
                log.error("Font file not found in classpath: {}", fontPath);
                throw new Exception("ไม่พบไฟล์ฟอนต์: " + fontPath);
            }
            
            try (InputStream is = resource.getInputStream()) {
                PDFont font = PDType0Font.load(document, is);
                log.debug("Font loaded successfully");
                return font;
            }
            
        } catch (Exception e) {
            log.error("Error loading Thai font: ", e);
            throw new Exception("ไม่สามารถโหลดฟอนต์ภาษาไทยได้: " + e.getMessage(), e);
        }
    }
    
    /**
     * วาดข้อความ (sanitize newline characters)
     */
    private float drawText(PDPageContentStream contentStream, 
                          String text, 
                          PDFont font, 
                          float fontSize, 
                          float x, 
                          float y) throws IOException {
        if (text == null || text.isEmpty()) {
            return y;
        }
        
        // แทนที่ \n ด้วย space เพราะ PDFBox ไม่รองรับ \n ใน showText
        // multiline ควรใช้ drawMultilineText แทน
        String sanitizedText = text.replace("\n", " ").replace("\r", " ").replace("\t", "    ");
        
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(sanitizedText);
        contentStream.endText();
        return y - fontSize - 5;
    }
    
    /**
     * วาดข้อความตรงกลาง
     */
    private float drawCenteredText(PDPageContentStream contentStream,
                                  String text,
                                  PDFont font,
                                  float fontSize,
                                  float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        return drawText(contentStream, text, font, fontSize, x, y);
    }
    
    /**
     * วาดฟิลด์ที่มีเส้นใต้ (underline) แบบจุดไข่ปลา เช่น ส่วนราชการ __________
     * รองรับข้อความหลายบรรทัด โดยแต่ละบรรทัดจะมีจุดไข่ปลาใต้เต็มความยาว
     * labelFont = font สำหรับ label (เช่น "ส่วนราชการ") - ตัวหนา
     * valueFont = font สำหรับ value (ข้อความ model) - ตัวธรรมดา
     * labelFontSize = ขนาดฟอนต์สำหรับ label
     * valueFontSize = ขนาดฟอนต์สำหรับ value (สามารถต่างจาก label ได้)
     */
    private float drawFieldWithUnderline(PDPageContentStream contentStream,
                                        String label,
                                        String value,
                                        PDFont labelFont,
                                        PDFont valueFont,
                                        float labelFontSize,
                                        float valueFontSize,
                                        float x,
                                        float y) throws IOException {
        // วาดป้ายกำกับ (เช่น "ส่วนราชการ") - ใช้ labelFont (ตัวหนา) กับ labelFontSize
        contentStream.beginText();
        contentStream.setFont(labelFont, labelFontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + " ");
        contentStream.endText();
        
        // คำนวณความกว้างของป้ายกำกับ
        float labelWidth = labelFont.getStringWidth(label + " ") / 1000 * labelFontSize;
        float valueX = x + labelWidth;
        float maxWidth = PAGE_WIDTH - MARGIN_RIGHT - valueX; // พื้นที่ที่เหลือสำหรับข้อความ
        
        float currentY = y;
        
        // วาดค่า (ถ้ามี)
        if (value != null && !value.isEmpty()) {
            String sanitizedValue = value.replace("\n", " ").replace("\r", " ").replace("\t", "    ");
            
            // แบ่งข้อความเป็นหลายบรรทัดถ้ายาวเกิน (ใช้ valueFont และ valueFontSize สำหรับคำนวณความกว้าง)
            List<String> lines = splitTextToFitWidth(sanitizedValue, valueFont, valueFontSize, maxWidth);
            
            for (String line : lines) {
                // วาดข้อความ - ใช้ valueFont (ตัวธรรมดา) กับ valueFontSize
                contentStream.beginText();
                contentStream.setFont(valueFont, valueFontSize);
                contentStream.newLineAtOffset(valueX, currentY);
                contentStream.showText(line);
                contentStream.endText();
                
                // วาดเส้นประจุดไข่ปลาใต้ข้อความเต็มความยาว
                float underlineY = currentY - 3;
                float underlineEndX = PAGE_WIDTH - MARGIN_RIGHT;
                
                // ตั้งค่าเป็นเส้นประแบบจุดไข่ปลา: 1pt เส้น, 2pt ช่องว่าง
                contentStream.setLineDashPattern(new float[]{1, 2}, 0);
                contentStream.moveTo(valueX, underlineY);
                contentStream.lineTo(underlineEndX, underlineY);
                contentStream.stroke();
                // รีเซ็ตกลับเป็นเส้นตรง
                contentStream.setLineDashPattern(new float[]{}, 0);
                
                // เลื่อนลงไปบรรทัดถัดไป
                currentY -= valueFontSize + 5;
            }
        } else {
            // วาดเส้นใต้เต็มความยาว (กรณีไม่มีข้อความ)
            float underlineY = currentY - 3;
            float underlineEndX = PAGE_WIDTH - MARGIN_RIGHT;
            
            // ตั้งค่าเป็นเส้นประแบบจุดไข่ปลา
            contentStream.setLineDashPattern(new float[]{1, 2}, 0);
            contentStream.moveTo(valueX, underlineY);
            contentStream.lineTo(underlineEndX, underlineY);
            contentStream.stroke();
            // รีเซ็ตกลับเป็นเส้นตรง
            contentStream.setLineDashPattern(new float[]{}, 0);
            
            currentY -= valueFontSize + 5;
        }
        
        return currentY;
    }
    
    /**
     * วาดฟิลด์ที่มีเส้นใต้แบบจุดไข่ปลา พร้อมกำหนดความยาวจุดไข่ปลาเอง
     * ใช้สำหรับ "ที่" และ "วันที่" ที่อยู่ในบรรทัดเดียวกัน
     * 
     * @param maxUnderlineX ตำแหน่ง X ที่จุดไข่ปลาจะสิ้นสุด (เพื่อไม่ให้ทับกับฟิลด์อื่น)
     */
    private float drawFieldWithUnderlineCustomWidth(PDPageContentStream contentStream,
                                        String label,
                                        String value,
                                        PDFont labelFont,
                                        PDFont valueFont,
                                        float labelFontSize,
                                        float valueFontSize,
                                        float x,
                                        float y,
                                        float maxUnderlineX) throws IOException {
        // วาดป้ายกำกับ (เช่น "ที่") - ใช้ labelFont (ตัวหนา) กับ labelFontSize
        contentStream.beginText();
        contentStream.setFont(labelFont, labelFontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + " ");
        contentStream.endText();
        
        // คำนวณความกว้างของป้ายกำกับ
        float labelWidth = labelFont.getStringWidth(label + " ") / 1000 * labelFontSize;
        float valueX = x + labelWidth;
        
        // วาดค่า (ถ้ามี) - ใช้ valueFont (ตัวธรรมดา) กับ valueFontSize
        if (value != null && !value.isEmpty()) {
            String sanitizedValue = value.replace("\n", " ").replace("\r", " ").replace("\t", "    ");
            contentStream.beginText();
            contentStream.setFont(valueFont, valueFontSize);
            contentStream.newLineAtOffset(valueX, y);
            contentStream.showText(sanitizedValue);
            contentStream.endText();
        }
        
        // วาดเส้นประจุดไข่ปลาใต้ข้อความ ยาวถึง maxUnderlineX
        float underlineY = y - 3;
        
        // ตั้งค่าเป็นเส้นประแบบจุดไข่ปลา: 1pt เส้น, 2pt ช่องว่าง
        contentStream.setLineDashPattern(new float[]{1, 2}, 0);
        contentStream.moveTo(valueX, underlineY);
        contentStream.lineTo(maxUnderlineX, underlineY);
        contentStream.stroke();
        // รีเซ็ตกลับเป็นเส้นตรง
        contentStream.setLineDashPattern(new float[]{}, 0);
        
        return y - Math.max(labelFontSize, valueFontSize) - 5;
    }
    
    /**
     * แบ่งข้อความเป็นหลายบรรทัดให้พอดีกับความกว้างที่กำหนด
     * รองรับทั้งการตัดคำ (มี space) และตัดตัวอักษร (ไม่มี space)
     */
    private List<String> splitTextToFitWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        // แบ่งเป็นคำด้วย space
        String[] words = text.split(" ");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String separator = (i == 0 || currentLine.length() == 0) ? "" : " ";
            String testLine = currentLine.toString() + separator + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (testWidth > maxWidth) {
                // ถ้าบรรทัดปัจจุบันยังว่างอยู่ แต่คำเดียวยาวเกิน = ต้องตัดทีละตัวอักษร
                if (currentLine.length() == 0) {
                    // ตัดคำยาวๆ ทีละตัวอักษร
                    for (int j = 0; j < word.length(); j++) {
                        char c = word.charAt(j);
                        String testChar = currentLine.toString() + c;
                        float charWidth = font.getStringWidth(testChar) / 1000 * fontSize;
                        
                        if (charWidth > maxWidth && currentLine.length() > 0) {
                            // บรรทัดเต็มแล้ว บันทึกและเริ่มใหม่
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(String.valueOf(c));
                        } else {
                            currentLine.append(c);
                        }
                    }
                    // เพิ่ม space หลังคำ ถ้ายังมีคำถัดไป
                    if (i < words.length - 1) {
                        float spaceWidth = font.getStringWidth(currentLine.toString() + " ") / 1000 * fontSize;
                        if (spaceWidth <= maxWidth) {
                            currentLine.append(" ");
                        } else {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                        }
                    }
                } else {
                    // บรรทัดปัจจุบันมีข้อความอยู่แล้ว บันทึกและนำคำนี้ไปบรรทัดใหม่
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                    
                    // เช็คว่าคำที่ย้ายมายาวเกินหรือไม่ ถ้าเกินต้องตัดทีละตัวอักษร
                    float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
                    if (wordWidth > maxWidth) {
                        // ตัดคำยาวๆ ทีละตัวอักษร
                        currentLine = new StringBuilder();
                        for (int j = 0; j < word.length(); j++) {
                            char c = word.charAt(j);
                            String testChar = currentLine.toString() + c;
                            float charWidth = font.getStringWidth(testChar) / 1000 * fontSize;
                            
                            if (charWidth > maxWidth && currentLine.length() > 0) {
                                lines.add(currentLine.toString());
                                currentLine = new StringBuilder(String.valueOf(c));
                            } else {
                                currentLine.append(c);
                            }
                        }
                    }
                    
                    // เพิ่ม space หลังคำ ถ้ายังมีคำถัดไป
                    if (i < words.length - 1 && currentLine.length() > 0) {
                        float spaceWidth = font.getStringWidth(currentLine.toString() + " ") / 1000 * fontSize;
                        if (spaceWidth <= maxWidth) {
                            currentLine.append(" ");
                        } else {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                        }
                    }
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        // เพิ่มบรรทัดสุดท้าย
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        // ป้องกันกรณี return list ว่าง
        if (lines.isEmpty()) {
            lines.add("");
        }
        
        return lines;
    }
    
    /**
     * วาดข้อความหลายบรรทัด พร้อม indent สำหรับบรรทัดที่ขึ้นใหม่
     * เหมาะสำหรับ "เรียน" ที่ต้องการให้บรรทัดที่ 2 เป็นต้นไปเยื้องให้ชื่อเรียงกัน
     * 
     * @param indentText ข้อความที่ใช้คำนวณการเยื้อง (เช่น "เรียน  ") - บรรทัดถัดไปจะเยื้องเท่ากับความกว้างของข้อความนี้
     */
    private float drawMultilineTextWithIndent(PDPageContentStream contentStream,
                                              String text,
                                              PDFont font,
                                              float fontSize,
                                              float x,
                                              float y,
                                              float maxWidth,
                                              String indentText) throws IOException {
        // คำนวณความกว้างของ indent
        float indentWidth = font.getStringWidth(indentText) / 1000 * fontSize;
        
        // แยกข้อความเป็นบรรทัด (split by maxWidth)
        List<String> lines = splitTextToLines(text, font, fontSize, maxWidth);
        
        float currentY = y;
        float lineHeight = fontSize + 5; // ระยะห่างระหว่างบรรทัด
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // บรรทัดแรก: ใช้ x ปกติ
            // บรรทัดถัดไป: เยื้องเข้ามาตาม indentWidth
            float lineX = (i == 0) ? x : (x + indentWidth);
            
            currentY = drawText(contentStream, line, font, fontSize, lineX, currentY);
            currentY -= 5; // spacing ระหว่างบรรทัด
        }
        
        return currentY;
    }
    
    /**
     * วาดข้อความหลายบรรทัด
     */
    private float drawMultilineText(PDPageContentStream contentStream,
                                   String text,
                                   PDFont font,
                                   float fontSize,
                                   float x,
                                   float y,
                                   float maxWidth) throws IOException {
        List<String> lines = splitTextToLines(text, font, fontSize, maxWidth);
        
        float currentY = y;
        for (String line : lines) {
            currentY = drawText(contentStream, line, font, fontSize, x, currentY);
            currentY -= 5; // spacing ระหว่างบรรทัด
        }
        
        return currentY;
    }
    
    /**
     * แบ่งข้อความเป็นบรรทัดตามความกว้าง
     * รองรับ newline (\n) และ Thai text
     */
    private List<String> splitTextToLines(String text, PDFont font, float fontSize, float maxWidth) 
            throws IOException {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        // แยกตาม newline ก่อน
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add(""); // เก็บบรรทัดว่างไว้
                continue;
            }
            
            // แยกตามช่องว่าง
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                if (word.isEmpty()) continue;
                
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                
                if (width > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    /**
     * แปลงเลขอารบิก (0-9) เป็นเลขไทย (๐-๙)
     * 
     * @param number ตัวเลขที่ต้องการแปลง
     * @return ตัวเลขไทย
     */
    private String convertToThaiNumber(int number) {
        String arabicNumber = String.valueOf(number);
        StringBuilder thaiNumber = new StringBuilder();
        
        for (char digit : arabicNumber.toCharArray()) {
            switch (digit) {
                case '0': thaiNumber.append('๐'); break;
                case '1': thaiNumber.append('๑'); break;
                case '2': thaiNumber.append('๒'); break;
                case '3': thaiNumber.append('๓'); break;
                case '4': thaiNumber.append('๔'); break;
                case '5': thaiNumber.append('๕'); break;
                case '6': thaiNumber.append('๖'); break;
                case '7': thaiNumber.append('๗'); break;
                case '8': thaiNumber.append('๘'); break;
                case '9': thaiNumber.append('๙'); break;
                default: thaiNumber.append(digit); // กรณีอื่น ๆ (ไม่น่าเกิด)
            }
        }
        
        return thaiNumber.toString();
    }
    
    /**
     * แปลง PDDocument เป็น Base64
     */
    private String convertToBase64(PDDocument document) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        byte[] pdfBytes = baos.toByteArray();
        log.debug("PDF converted to Base64, size: {} bytes", pdfBytes.length);
        return Base64.getEncoder().encodeToString(pdfBytes);
    }
}
