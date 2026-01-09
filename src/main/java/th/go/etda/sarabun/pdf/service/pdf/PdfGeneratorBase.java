package th.go.etda.sarabun.pdf.service.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.springframework.core.io.ClassPathResource;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Base class สำหรับ PDF Generator ทุกประเภท
 * 
 * รวม common methods ที่ใช้ร่วมกัน:
 * - โหลดฟอนต์
 * - วาดข้อความ
 * - จัดการหน้า
 * - แปลง Base64
 */
@Slf4j
public abstract class PdfGeneratorBase {
    
    // ============================================
    // Font Paths
    // ============================================
    protected static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    protected static final String FONT_BOLD_PATH = "fonts/THSarabunNew Bold.ttf";
    
    // ============================================
    // Page Settings (A4)
    // ============================================
    protected static final float PAGE_WIDTH = PDRectangle.A4.getWidth();   // 595 pt
    protected static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 842 pt
    
    // ============================================
    // Margins
    // ============================================
    protected static final float MARGIN_TOP = 70f;
    protected static final float MARGIN_BOTTOM = 70f;
    protected static final float MARGIN_LEFT = 70f;
    protected static final float MARGIN_RIGHT = 70f;
    
    // ============================================
    // Font Sizes
    // ============================================
    protected static final float FONT_SIZE_HEADER = 24f;
    protected static final float FONT_SIZE_FIELD = 18f;
    protected static final float FONT_SIZE_FIELD_VALUE = 16f;
    protected static final float FONT_SIZE_CONTENT = 16f;
    
    // ============================================
    // Spacing
    // ============================================
    protected static final float SPACING_AFTER_HEADER = 30f;
    protected static final float SPACING_BETWEEN_FIELDS = 5f;
    protected static final float SPACING_BEFORE_CONTENT = 14f;
    protected static final float SPACING_BEFORE_SIGNATURES = 40f;
    protected static final float SPACING_BETWEEN_SIGNATURES = 20f;
    
    // ============================================
    // Logo Settings
    // ============================================
    protected static final float LOGO_WIDTH = 120f;
    protected static final float LOGO_HEIGHT = 40f;
    protected static final float LOGO_SPACING = 30f;
    
    /**
     * ตำแหน่งโลโก้บนหน้าเอกสาร
     * 
     * LEFT   - ซ้ายชิดขอบ (ใช้กับ: บันทึกข้อความ)
     * CENTER - บนสุดกลาง (ใช้กับ: ประกาศ, ระเบียบ, ข้อบังคับ, คำสั่ง)
     * RIGHT  - ขวาบนชิดขอบ (ใช้กับ: หนังสือส่งออก, หนังสือประทับตรา)
     */
    public enum LogoPosition {
        LEFT,    // ซ้ายชิดขอบ: x = MARGIN_LEFT
        CENTER,  // บนสุดกลาง: x = (PAGE_WIDTH - LOGO_WIDTH) / 2
        RIGHT    // ขวาบนชิดขอบ: x = PAGE_WIDTH - MARGIN_RIGHT - LOGO_WIDTH
    }
    
    // ============================================
    // Multi-page Settings
    // ============================================
    protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 100;
    protected static final float PAGE_NUMBER_Y_OFFSET = 15f;
    
    // ============================================
    // Debug Mode
    // ============================================
    protected static final boolean ENABLE_DEBUG_BORDERS = false;
    
    // ============================================
    // Field Positions
    // ============================================
    protected static final float DATE_X_POSITION = PAGE_WIDTH - 320;
    
    // ============================================
    // Thai Number Mapping
    // ============================================
    private static final char[] THAI_DIGITS = {'๐', '๑', '๒', '๓', '๔', '๕', '๖', '๗', '๘', '๙'};
    private static final String[] THAI_MONTHS = {
        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    };
    
    // ============================================
    // Abstract Methods - ต้อง implement ใน subclass
    // ============================================
    
    /**
     * สร้าง PDF สำหรับประเภทเอกสารนี้
     * 
     * @param request ข้อมูล request
     * @return List ของ PdfResult (อาจมีหลายไฟล์)
     */
    public abstract List<PdfResult> generate(GeneratePdfRequest request) throws Exception;
    
    /**
     * คืนค่าประเภทเอกสารที่ Generator นี้รองรับ
     */
    public abstract BookType getBookType();
    
    /**
     * คืนค่าชื่อ Generator
     */
    public abstract String getGeneratorName();
    
    // ============================================
    // Common Helper Methods
    // ============================================
    
    /**
     * โหลด Thai font จาก resources
     */
    protected PDFont loadThaiFont(PDDocument document, String fontPath) throws Exception {
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
     * โหลด font ปกติ
     */
    protected PDFont loadRegularFont(PDDocument document) throws Exception {
        return loadThaiFont(document, FONT_PATH);
    }
    
    /**
     * โหลด font ตัวหนา
     */
    protected PDFont loadBoldFont(PDDocument document) throws Exception {
        return loadThaiFont(document, FONT_BOLD_PATH);
    }
    
    /**
     * แปลง PDDocument เป็น Base64 string
     */
    protected String convertToBase64(PDDocument document) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.save(baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
    
    /**
     * วาดข้อความ (sanitize newline characters)
     */
    protected float drawText(PDPageContentStream contentStream, 
                            String text, 
                            PDFont font, 
                            float fontSize, 
                            float x, 
                            float y) throws IOException {
        if (text == null || text.isEmpty()) {
            return y;
        }
        
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
    protected float drawCenteredText(PDPageContentStream contentStream,
                                    String text,
                                    PDFont font,
                                    float fontSize,
                                    float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        return drawText(contentStream, text, font, fontSize, x, y);
    }
    
    /**
     * วาดฟิลด์ที่มีเส้นใต้ (underline) แบบจุดไข่ปลา
     */
    protected float drawFieldWithUnderline(PDPageContentStream contentStream,
                                          String label,
                                          String value,
                                          PDFont labelFont,
                                          PDFont valueFont,
                                          float labelFontSize,
                                          float valueFontSize,
                                          float x,
                                          float y) throws IOException {
        // วาดป้ายกำกับ
        contentStream.beginText();
        contentStream.setFont(labelFont, labelFontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + " ");
        contentStream.endText();
        
        float labelWidth = labelFont.getStringWidth(label + " ") / 1000 * labelFontSize;
        float valueX = x + labelWidth;
        float maxWidth = PAGE_WIDTH - MARGIN_RIGHT - valueX;
        
        float currentY = y;
        
        if (value != null && !value.isEmpty()) {
            String sanitizedValue = value.replace("\n", " ").replace("\r", " ").replace("\t", "    ");
            List<String> lines = splitTextToFitWidth(sanitizedValue, valueFont, valueFontSize, maxWidth);
            
            for (String line : lines) {
                contentStream.beginText();
                contentStream.setFont(valueFont, valueFontSize);
                contentStream.newLineAtOffset(valueX, currentY);
                contentStream.showText(line);
                contentStream.endText();
                
                float underlineY = currentY - 3;
                float underlineEndX = PAGE_WIDTH - MARGIN_RIGHT;
                
                contentStream.setLineDashPattern(new float[]{1, 2}, 0);
                contentStream.moveTo(valueX, underlineY);
                contentStream.lineTo(underlineEndX, underlineY);
                contentStream.stroke();
                contentStream.setLineDashPattern(new float[]{}, 0);
                
                currentY -= valueFontSize + 5;
            }
        } else {
            float underlineY = currentY - 3;
            float underlineEndX = PAGE_WIDTH - MARGIN_RIGHT;
            
            contentStream.setLineDashPattern(new float[]{1, 2}, 0);
            contentStream.moveTo(valueX, underlineY);
            contentStream.lineTo(underlineEndX, underlineY);
            contentStream.stroke();
            contentStream.setLineDashPattern(new float[]{}, 0);
            
            currentY -= valueFontSize + 5;
        }
        
        return currentY;
    }
    
    /**
     * วาดฟิลด์ที่มีเส้นใต้แบบจุดไข่ปลา พร้อมกำหนดความยาวจุดไข่ปลาเอง
     */
    protected float drawFieldWithUnderlineCustomWidth(PDPageContentStream contentStream,
                                                     String label,
                                                     String value,
                                                     PDFont labelFont,
                                                     PDFont valueFont,
                                                     float labelFontSize,
                                                     float valueFontSize,
                                                     float x,
                                                     float y,
                                                     float maxUnderlineX) throws IOException {
        contentStream.beginText();
        contentStream.setFont(labelFont, labelFontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + " ");
        contentStream.endText();
        
        float labelWidth = labelFont.getStringWidth(label + " ") / 1000 * labelFontSize;
        float valueX = x + labelWidth;
        
        if (value != null && !value.isEmpty()) {
            String sanitizedValue = value.replace("\n", " ").replace("\r", " ").replace("\t", "    ");
            contentStream.beginText();
            contentStream.setFont(valueFont, valueFontSize);
            contentStream.newLineAtOffset(valueX, y);
            contentStream.showText(sanitizedValue);
            contentStream.endText();
        }
        
        float underlineY = y - 3;
        
        contentStream.setLineDashPattern(new float[]{1, 2}, 0);
        contentStream.moveTo(valueX, underlineY);
        contentStream.lineTo(maxUnderlineX, underlineY);
        contentStream.stroke();
        contentStream.setLineDashPattern(new float[]{}, 0);
        
        return y - Math.max(labelFontSize, valueFontSize) - 5;
    }
    
    /**
     * แบ่งข้อความเป็นหลายบรรทัดให้พอดีกับความกว้างที่กำหนด
     */
    protected List<String> splitTextToFitWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        String[] words = text.split(" ");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String separator = (i == 0 || currentLine.length() == 0) ? "" : " ";
            String testLine = currentLine.toString() + separator + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (testWidth > maxWidth) {
                if (currentLine.length() == 0) {
                    // ตัดคำยาวๆ ทีละตัวอักษร
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
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        if (lines.isEmpty()) {
            lines.add("");
        }
        
        return lines;
    }
    
    /**
     * วาดข้อความหลายบรรทัด
     */
    protected float drawMultilineText(PDPageContentStream contentStream,
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
            currentY -= 5;
        }
        
        return currentY;
    }
    
    /**
     * วาดข้อความหลายบรรทัด พร้อม indent สำหรับบรรทัดที่ขึ้นใหม่
     */
    protected float drawMultilineTextWithIndent(PDPageContentStream contentStream,
                                               String text,
                                               PDFont font,
                                               float fontSize,
                                               float x,
                                               float y,
                                               float maxWidth,
                                               String indentText) throws IOException {
        float indentWidth = font.getStringWidth(indentText) / 1000 * fontSize;
        List<String> lines = splitTextToLines(text, font, fontSize, maxWidth);
        
        float currentY = y;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineX = (i == 0) ? x : (x + indentWidth);
            currentY = drawText(contentStream, line, font, fontSize, lineX, currentY);
            currentY -= 5;
        }
        
        return currentY;
    }
    
    /**
     * แบ่งข้อความเป็นบรรทัดตามความกว้าง
     */
    protected List<String> splitTextToLines(String text, PDFont font, float fontSize, float maxWidth) 
            throws IOException {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            
            int leadingSpaces = 0;
            while (leadingSpaces < paragraph.length() && paragraph.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }
            String indent = leadingSpaces > 0 ? paragraph.substring(0, leadingSpaces) : "";
            String content = paragraph.substring(leadingSpaces);
            
            String[] words = content.split(" ");
            StringBuilder currentLine = new StringBuilder();
            boolean isFirstLine = true;
            
            for (String word : words) {
                if (word.isEmpty()) continue;
                
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (isFirstLine && !indent.isEmpty()) {
                    testLine = indent + testLine;
                }
                
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                
                if (width > maxWidth && currentLine.length() > 0) {
                    String lineToAdd = isFirstLine && !indent.isEmpty() ? indent + currentLine.toString() : currentLine.toString();
                    lines.add(lineToAdd);
                    currentLine = new StringBuilder(word);
                    isFirstLine = false;
                } else {
                    currentLine = new StringBuilder(testLine);
                    if (isFirstLine && !indent.isEmpty()) {
                        currentLine = new StringBuilder(testLine.substring(indent.length()));
                    }
                }
            }
            
            if (currentLine.length() > 0) {
                String lineToAdd = isFirstLine && !indent.isEmpty() ? indent + currentLine.toString() : currentLine.toString();
                lines.add(lineToAdd);
            }
        }
        
        return lines;
    }
    
    /**
     * แปลงตัวเลขอารบิกเป็นเลขไทย
     */
    protected String convertToThaiNumber(int number) {
        StringBuilder result = new StringBuilder();
        String numStr = String.valueOf(number);
        for (char c : numStr.toCharArray()) {
            if (Character.isDigit(c)) {
                result.append(THAI_DIGITS[c - '0']);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * แปลงวันที่เป็นรูปแบบไทย
     * @param day วัน
     * @param month เดือน (1-12)
     * @param year ปี พ.ศ. (ถ้าส่งมาเป็น ค.ศ. ให้บวก 543 ก่อนส่ง)
     */
    protected String toThaiDate(int day, int month, int year) {
        String thaiDay = convertToThaiNumber(day);
        String thaiMonth = THAI_MONTHS[month - 1];
        String thaiYear = convertToThaiNumber(year);
        return thaiDay + " " + thaiMonth + " " + thaiYear;
    }
    
    /**
     * แปลงวันที่เป็นรูปแบบไทย (จาก ค.ศ.)
     * @param day วัน
     * @param month เดือน (1-12)
     * @param yearAD ปี ค.ศ. (จะแปลงเป็น พ.ศ. โดยอัตโนมัติ)
     */
    protected String toThaiDateFromAD(int day, int month, int yearAD) {
        return toThaiDate(day, month, yearAD + 543);
    }
    
    /**
     * วาด debug borders (ถ้าเปิด)
     */
    protected void drawDebugBorders(PDPageContentStream stream) throws IOException {
        if (!ENABLE_DEBUG_BORDERS) {
            return;
        }
        
        stream.setStrokingColor(java.awt.Color.RED);
        stream.setLineWidth(0.5f);
        stream.addRect(
            MARGIN_LEFT,
            MARGIN_BOTTOM,
            PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT,
            PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM
        );
        stream.stroke();
    }
    
    /**
     * สร้างหน้าใหม่พร้อมหมายเลขหน้าและเลขที่หนังสือ
     */
    protected PDPage createNewPage(PDDocument document, PDFont fontRegular, String bookNo) throws IOException {
        PDPage newPage = new PDPage(PDRectangle.A4);
        document.addPage(newPage);
        
        int pageNumber = document.getNumberOfPages();
        try (PDPageContentStream stream = new PDPageContentStream(document, newPage)) {
            if (pageNumber >= 2) {
                drawPageNumber(stream, pageNumber, fontRegular);
            }
            drawBookNumber(stream, bookNo, fontRegular);
            drawDebugBorders(stream);
        }
        
        log.info("Created new page {}", pageNumber);
        return newPage;
    }
    
    /**
     * วาดหมายเลขหน้าที่กลางบน (รูปแบบเลขไทย)
     */
    protected void drawPageNumber(PDPageContentStream stream, int pageNumber, PDFont font) throws IOException {
        String thaiPageNumber = convertToThaiNumber(pageNumber);
        String pageText = "- " + thaiPageNumber + " -";
        
        float textWidth = font.getStringWidth(pageText) / 1000 * FONT_SIZE_CONTENT;
        float x = (PAGE_WIDTH - textWidth) / 2;
        float y = PAGE_HEIGHT - MARGIN_TOP + PAGE_NUMBER_Y_OFFSET;
        
        stream.beginText();
        stream.setFont(font, FONT_SIZE_CONTENT);
        stream.newLineAtOffset(x, y);
        stream.showText(pageText);
        stream.endText();
    }
    
    /**
     * วาดเลขที่หนังสือที่ขอบล่างซ้าย
     */
    protected void drawBookNumber(PDPageContentStream stream, String bookNo, PDFont font) throws IOException {
        if (bookNo == null || bookNo.isEmpty()) {
            return;
        }
        
        float x = MARGIN_LEFT - 20;
        float y = MARGIN_BOTTOM - 30;
        
        stream.beginText();
        stream.setFont(font, FONT_SIZE_FIELD_VALUE);
        stream.newLineAtOffset(x, y);
        stream.showText(bookNo);
        stream.endText();
    }
    
    /**
     * โหลดและวาดโลโก้
     * 
     * @param contentStream PDPageContentStream
     * @param document PDDocument
     * @param yPosition ตำแหน่ง Y ปัจจุบัน
     * @param position ตำแหน่งโลโก้ (LEFT, CENTER, RIGHT)
     * @return ตำแหน่ง Y ใหม่ (ขอบล่างของโลโก้)
     * 
     * การปรับตำแหน่ง:
     * - ปรับซ้าย/ขวา: เปลี่ยน LogoPosition (LEFT, CENTER, RIGHT)
     * - ปรับขึ้น/ลง: เปลี่ยน yPosition ที่ส่งเข้ามา หรือ MARGIN_TOP
     * - ปรับขนาด: เปลี่ยน LOGO_WIDTH, LOGO_HEIGHT
     */
    protected float drawLogo(PDPageContentStream contentStream, PDDocument document, float yPosition, LogoPosition position) throws IOException {
        try {
            InputStream logoStream = getClass().getClassLoader().getResourceAsStream("images/logoETDA.png");
            if (logoStream != null) {
                PDImageXObject logoImage = PDImageXObject.createFromByteArray(
                    document, logoStream.readAllBytes(), "logo");
                
                // คำนวณตำแหน่ง X ตาม LogoPosition
                float logoX = switch (position) {
                    case LEFT   -> MARGIN_LEFT;                                    // ซ้ายชิดขอบ
                    case CENTER -> (PAGE_WIDTH - LOGO_WIDTH) / 2;                  // กลาง
                    case RIGHT  -> PAGE_WIDTH - MARGIN_RIGHT - LOGO_WIDTH + 5;     // ขวาชิดขอบ (+5 ขวา)
                };
                float logoY = yPosition - LOGO_HEIGHT + (position == LogoPosition.RIGHT ? 5: 0);  // RIGHT สูงขึ้น 5
                
                contentStream.drawImage(logoImage, logoX, logoY, LOGO_WIDTH, LOGO_HEIGHT);
                log.info("Logo drawn at ({}, {}) position={}", logoX, logoY, position);
                logoStream.close();
                
                return logoY;
            }
        } catch (Exception e) {
            log.warn("Could not load logo: {}", e.getMessage());
        }
        
        return yPosition - LOGO_HEIGHT;
    }
    
    /**
     * สร้าง PdfResult สำหรับ PDF หลัก
     */
    protected PdfResult createMainPdfResult(String pdfBase64, String description) {
        return PdfResult.builder()
            .pdfBase64(pdfBase64)
            .type("Main")
            .description(description)
            .build();
    }
    
    /**
     * สร้าง PdfResult สำหรับ บันทึกข้อความ
     */
    protected PdfResult createMemoPdfResult(String pdfBase64, String description) {
        return PdfResult.builder()
            .pdfBase64(pdfBase64)
            .type("Memo")
            .description(description)
            .build();
    }
    
    /**
     * สร้าง PdfResult สำหรับ PDF รอง
     */
    protected PdfResult createOtherPdfResult(String pdfBase64, String description) {
        return PdfResult.builder()
            .pdfBase64(pdfBase64)
            .type("Other")
            .description(description)
            .build();
    }
    
    /**
     * รวม PDF files หลายไฟล์เป็น 1 ไฟล์
     * 
     * ใช้ PDFMergerUtility + MemoryUsageSetting
     * 
     * @param base64Pdfs List ของ PDF ในรูปแบบ Base64
     * @return PDF ที่รวมแล้วในรูปแบบ Base64
     */
    protected String mergePdfFiles(List<String> base64Pdfs) throws Exception {
        if (base64Pdfs == null || base64Pdfs.isEmpty()) {
            throw new IllegalArgumentException("PDF list is empty");
        }
        
        if (base64Pdfs.size() == 1) {
            return base64Pdfs.get(0);
        }
        
        PDFMergerUtility merger = new PDFMergerUtility();
        
        for (String base64Pdf : base64Pdfs) {
            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
            merger.addSource(new ByteArrayInputStream(pdfBytes));
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outputStream);
        
        // ใช้ MemoryUsageSetting ตาม migrateToJava.txt
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        
        log.info("Merged {} PDFs successfully", base64Pdfs.size());
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    // ============================================
    // Signature Box - ฟังก์ชันกลางสำหรับวาดช่องลงนาม
    // ============================================
    
    /**
     * วาดช่องลงนาม (รูปแบบมาตรฐาน - ใช้ร่วมกันทุก Generator)
     * 
     * โครงสร้าง:
     *   ┌─────────────────────────┐
     *   │       ช่องลงนาม          │  ← ข้อความในกรอบ (ตรงกลาง)
     *   └─────────────────────────┘
     *       (ชื่อ-นามสกุล)           ← ใต้กรอบ (ตรงกลาง)
     *         ตำแหน่ง               ← ใต้ชื่อ (ตรงกลาง)
     * 
     * @param contentStream PDPageContentStream
     * @param prefixName คำนำหน้า (นาย, นาง, นางสาว)
     * @param firstname ชื่อ
     * @param lastname นามสกุล
     * @param positionName ตำแหน่ง
     * @param font ฟอนต์
     * @param yPosition ตำแหน่ง Y ปัจจุบัน
     * @return ตำแหน่ง Y ใหม่หลังวาดเสร็จ
     */
    protected float drawSignerBox(PDPageContentStream contentStream, 
                                 String prefixName,
                                 String firstname,
                                 String lastname,
                                 String positionName,
                                 PDFont font, 
                                 float yPosition) throws Exception {
        return drawSignerBoxWithLabel(contentStream, prefixName, firstname, lastname, 
                                      positionName, font, yPosition, "ช่องลงนาม");
    }
    
    /**
     * วาดช่องลงนาม พร้อมกำหนด label เอง (เช่น "เสนอผ่าน", "เรียน", "รับทราบ")
     * 
     * @param contentStream PDPageContentStream
     * @param prefixName คำนำหน้า
     * @param firstname ชื่อ
     * @param lastname นามสกุล
     * @param positionName ตำแหน่ง
     * @param font ฟอนต์
     * @param yPosition ตำแหน่ง Y ปัจจุบัน
     * @param labelText ข้อความ label ในกรอบ (เช่น "เสนอผ่าน", "ช่องลงนาม")
     * @return ตำแหน่ง Y ใหม่หลังวาดเสร็จ
     */
    protected float drawSignerBoxWithLabel(PDPageContentStream contentStream, 
                                          String prefixName,
                                          String firstname,
                                          String lastname,
                                          String positionName,
                                          PDFont font, 
                                          float yPosition,
                                          String labelText) throws Exception {
        // กำหนดขนาดและตำแหน่งกรอบ (ตรงกลาง-ขวา)
        float boxWidth = 180f;
        float boxHeight = 50f;
        float boxX = PAGE_WIDTH / 2 + 20;  // ขยับไปทางขวา
        
        // ตรวจสอบความยาว label
        float labelWidth = font.getStringWidth(labelText) / 1000 * 14f;
        boolean isLongLabel = labelWidth > boxWidth - 20;
        
        float currentY = yPosition;
        
        // ถ้า label ยาว → วางไว้บนหัวกรอบ (ตรงกลางเหนือกรอบ)
        if (isLongLabel) {
            // center label เหนือกรอบ (boxX + boxWidth/2 เป็นจุดกึ่งกลางกรอบ)
            float boxCenterX = boxX + (boxWidth / 2);
            float longLabelX = boxCenterX - (labelWidth / 2);
            drawText(contentStream, labelText, font, 14f, longLabelX, currentY);
            currentY -= 25;
        }
        
        float boxY = currentY - boxHeight;
        
        // วาดกรอบสีฟ้าประ
        contentStream.setStrokingColor(0.4f, 0.7f, 0.9f); // สีฟ้าอ่อน
        contentStream.setLineDashPattern(new float[]{5, 3}, 0); // เส้นประ
        contentStream.setLineWidth(1.5f);
        contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
        contentStream.stroke();
        
        // รีเซ็ตเส้นกลับเป็นปกติ
        contentStream.setLineDashPattern(new float[]{}, 0);
        contentStream.setStrokingColor(0, 0, 0);
        
        // ถ้า label สั้น → วางในกรอบ
        if (!isLongLabel) {
            float textX = boxX + (boxWidth - labelWidth) / 2;
            float textY = boxY + (boxHeight / 2) - 5;
            drawText(contentStream, labelText, font, 14f, textX, textY);
        }
        
        currentY = boxY - 15;
        
        // สร้างชื่อเต็ม
        String fullName = buildFullName(prefixName, firstname, lastname);
        
        // วาดชื่อผู้ลงนาม (ตรงกลางใต้กรอบ)
        if (!fullName.isEmpty()) {
            String nameText = "(" + fullName + ")";
            float nameWidth = font.getStringWidth(nameText) / 1000 * 14f;
            float nameX = boxX + (boxWidth - nameWidth) / 2;
            drawText(contentStream, nameText, font, 14f, nameX, currentY);
            currentY -= 20;
        }
        
        // วาดตำแหน่ง (ตรงกลางใต้ชื่อ)
        if (positionName != null && !positionName.isEmpty()) {
            float posWidth = font.getStringWidth(positionName) / 1000 * 12f;
            float posX = boxX + (boxWidth - posWidth) / 2;
            drawText(contentStream, positionName, font, 12f, posX, currentY);
            currentY -= 25;
        }
        
        return currentY;
    }
    
    /**
     * สร้างชื่อเต็มจาก prefix, firstname, lastname
     */
    protected String buildFullName(String prefixName, String firstname, String lastname) {
        StringBuilder fullName = new StringBuilder();
        if (prefixName != null) {
            fullName.append(prefixName);
        }
        if (firstname != null) {
            fullName.append(firstname);
        }
        if (lastname != null) {
            fullName.append(" ").append(lastname);
        }
        return fullName.toString().trim();
    }
    
    // ============================================
    // Signature Field Methods (เจาะช่องจริง)
    // ============================================
    
    /**
     * วาดช่องลงนามพร้อมเจาะ AcroForm Signature Field จริง
     * 
     * @param document PDDocument
     * @param page PDPage ที่จะเพิ่ม signature field
     * @param contentStream PDPageContentStream
     * @param signer ข้อมูลผู้ลงนาม
     * @param font ฟอนต์
     * @param yPosition ตำแหน่ง Y ปัจจุบัน
     * @param fieldPrefix prefix สำหรับชื่อ field (เช่น "Sign", "Submit")
     * @param index ลำดับผู้ลงนาม
     * @param labelText ข้อความ label (เช่น "ช่องลงนาม", "เสนอผ่าน")
     * @return ตำแหน่ง Y ใหม่หลังวาดเสร็จ
     */
    protected float drawSignerBoxWithSignatureField(PDDocument document,
                                                    PDPage page,
                                                    PDPageContentStream contentStream,
                                                    SignerInfo signer,
                                                    PDFont font, 
                                                    float yPosition,
                                                    String fieldPrefix,
                                                    int index,
                                                    String labelText) throws Exception {
        // กำหนดขนาดและตำแหน่งกรอบ (ตรงกลาง-ขวา)
        float boxWidth = 180f;
        float boxHeight = 50f;
        float boxX = PAGE_WIDTH / 2 + 20;
        
        // ตรวจสอบความยาว label
        float labelWidth = font.getStringWidth(labelText) / 1000 * 14f;
        boolean isLongLabel = labelWidth > boxWidth - 20;
        
        float currentY = yPosition;
        
        // ถ้า label ยาว → วางไว้บนหัวกรอบ
        if (isLongLabel) {
            float boxCenterX = boxX + (boxWidth / 2);
            float longLabelX = boxCenterX - (labelWidth / 2);
            drawText(contentStream, labelText, font, 14f, longLabelX, currentY);
            currentY -= 25;
        }
        
        float boxY = currentY - boxHeight;
        
        // วาดกรอบสีฟ้าประ (visual)
        contentStream.setStrokingColor(0.4f, 0.7f, 0.9f);
        contentStream.setLineDashPattern(new float[]{5, 3}, 0);
        contentStream.setLineWidth(1.5f);
        contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
        contentStream.stroke();
        
        // รีเซ็ตเส้นกลับเป็นปกติ
        contentStream.setLineDashPattern(new float[]{}, 0);
        contentStream.setStrokingColor(0, 0, 0);
        
        // ถ้า label สั้น → วางในกรอบ
        if (!isLongLabel) {
            float textX = boxX + (boxWidth - labelWidth) / 2;
            float textY = boxY + (boxHeight / 2) - 5;
            drawText(contentStream, labelText, font, 14f, textX, textY);
        }
        
        // ===== สร้าง AcroForm Signature Field จริง =====
        try {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }
            
            String email = signer.getEmail() != null ? signer.getEmail() : "user" + index;
            String fieldName = fieldPrefix + "_" + index + "_" + 
                              email.replace("@", "_").replace(".", "_");
            
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName(fieldName);
            
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(boxX, boxY, boxWidth, boxHeight);
            widget.setRectangle(rect);
            widget.setPage(page);
            
            // เพิ่ม widget เข้า page
            page.getAnnotations().add(widget);
            
            acroForm.getFields().add(signatureField);
            
            log.debug("Created signature field: {} at ({}, {})", fieldName, boxX, boxY);
            
        } catch (Exception e) {
            log.warn("Could not create AcroForm signature field: {}", e.getMessage());
        }
        
        currentY = boxY - 15;
        
        // วาดชื่อผู้ลงนาม
        String fullName = buildFullName(signer.getPrefixName(), signer.getFirstname(), signer.getLastname());
        if (!fullName.isEmpty()) {
            String nameText = "(" + fullName + ")";
            float nameWidth = font.getStringWidth(nameText) / 1000 * 14f;
            float nameX = boxX + (boxWidth - nameWidth) / 2;
            drawText(contentStream, nameText, font, 14f, nameX, currentY);
            currentY -= 20;
        }
        
        // วาดตำแหน่ง
        String positionName = signer.getPositionName();
        if (positionName != null && !positionName.isEmpty()) {
            float posWidth = font.getStringWidth(positionName) / 1000 * 12f;
            float posX = boxX + (boxWidth - posWidth) / 2;
            drawText(contentStream, positionName, font, 12f, posX, currentY);
            currentY -= 25;
        }
        
        return currentY;
    }
    
    // ============================================
    // Inner Classes (ใช้ร่วมกันทุก Generator)
    // ============================================
    
    /**
     * ข้อมูลผู้ลงนาม - ใช้ร่วมกันทุก PDF Generator
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SignerInfo {
        private String prefixName;
        private String firstname;
        private String lastname;
        private String positionName;
        private String departmentName;
        private String email;
        private String signatureBase64;
    }
    
    /**
     * ข้อมูลผู้ติดต่อ - ใช้ร่วมกันทุก PDF Generator
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContactInfo {
        private String department;
        private String phone;
        private String fax;
        private String email;
        
        public boolean hasAnyInfo() {
            return (department != null && !department.isEmpty()) ||
                   (phone != null && !phone.isEmpty()) ||
                   (fax != null && !fax.isEmpty()) ||
                   (email != null && !email.isEmpty());
        }
    }
}
