package th.go.etda.sarabun.pdf.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Core PDF Service สำหรับการสร้างและจัดการ PDF โดยใช้ Apache PDFBox 2.x
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Service/PdfService.cs
 * 
 * การเปลี่ยนแปลงสำคัญ:
 * 1. แทนที่ iText7 ด้วย Apache PDFBox 2.0.31 (ฟรี 100%)
 * 2. ใช้ PDType0Font สำหรับโหลด TrueType fonts (TH Sarabun)
 * 3. รองรับ UTF-8 และ Thai language
 * 4. ใช้ Java NIO สำหรับจัดการไฟล์
 * 5. ใช้ stream แทนการ save file ชั่วคราว
 * 
 * PDFBox 2.x เป็น open-source library จาก Apache:
 * - License: Apache License 2.0 (ฟรี 100%)
 * - รองรับ PDF 1.7
 * - รองรับ Unicode และ Thai fonts
 * - Cross-platform (Windows, Linux, Mac)
 * 
 * หมายเหตุ: ตาม migrateToJava.txt ต้องใช้ PDFBox 2.x เท่านั้น (ไม่ใช่ 3.x)
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

    
    // ⚙️ Font Sizes - ขนาดฟอนต์ (ปรับได้)

    private static final float FONT_SIZE_FIELD = 18f;         // ฟิลด์ label (ส่วนราชการ, ที่, วันที่, เรื่อง)
    private static final float FONT_SIZE_FIELD_VALUE = 16f;   // ฟิลด์ value (ข้อความ model)
    private static final float FONT_SIZE_CONTENT = 16f;       // เนื้อหา

    
    // ⚙️ Vertical Spacing - ระยะห่างระหว่างแต่ละบรรทัด (ปรับได้)

    private static final float SPACING_BETWEEN_FIELDS = 5f; // ระหว่างฟิลด์ต่างๆ (ลดจาก 25f)
    private static final float SPACING_BEFORE_CONTENT = 14f; // ก่อนเนื้อหา (แยกจากฟิลด์)


    
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
            // วาดหมายเลขหน้าเฉพาะหน้าที่ 2 ขึ้นไป (หน้าแรกไม่มีเลขหน้า)
            if (pageNumber >= 2) {
                drawPageNumber(stream, pageNumber, fontRegular);
            }
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
        String pageText = "- " + thaiPageNumber + " -";
        
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
     * เพิ่มฟิลด์ลายเซ็นลงใน PDF (Legacy method)
     * 
     * หมายเหตุ: Method นี้ถูกแทนที่ด้วยการวาดช่องลงนามใน generateOfficialMemoPdf() SECTION 7
     * คง method ไว้เพื่อ backward compatibility แต่ไม่ทำอะไรแล้ว
     * 
     * @param inputFile ไฟล์ PDF input
     * @param outputFile ไฟล์ PDF output
     * @param signatureFields รายการฟิลด์ลายเซ็น
     */
    public void addSignatureFields(File inputFile, 
                                  File outputFile,
                                  List<GeneratePdfService.SignatureFieldInfo> signatureFields) throws Exception {
        log.debug("addSignatureFields() called - SKIPPED (signature boxes are now drawn in main PDF generation)");
        
        // เพียงแค่ copy ไฟล์ต้นฉบับไปยัง output โดยไม่เพิ่มอะไร
        // เพราะช่องลงนามถูกวาดใน generateOfficialMemoPdf() แล้ว
        try (PDDocument document = PDDocument.load(inputFile)) {
            document.save(outputFile);
        }
    }
    
    /**
     * ตรวจสอบและเพิ่มเลขหน้า
     * 
     * แปลงมาจาก: CheckAndAddPageNumbers() method
     */
    public void addPageNumbers(File inputFile, File outputFile) throws Exception {
        try (PDDocument document = PDDocument.load(inputFile)) {
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
            
            // เก็บ leading spaces (indent) ไว้
            int leadingSpaces = 0;
            while (leadingSpaces < paragraph.length() && paragraph.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }
            String indent = leadingSpaces > 0 ? paragraph.substring(0, leadingSpaces) : "";
            String content = paragraph.substring(leadingSpaces);
            
            // แยกตามช่องว่าง
            String[] words = content.split(" ");
            StringBuilder currentLine = new StringBuilder();
            boolean isFirstLine = true;
            
            for (String word : words) {
                if (word.isEmpty()) continue;
                
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                // เพิ่ม indent ถ้าเป็นบรรทัดแรก
                if (isFirstLine && !indent.isEmpty()) {
                    testLine = indent + testLine;
                }
                
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                
                if (width > maxWidth && currentLine.length() > 0) {
                    // ถ้าเป็นบรรทัดแรก ให้ใส่ indent
                    String lineToAdd = isFirstLine && !indent.isEmpty() ? indent + currentLine.toString() : currentLine.toString();
                    lines.add(lineToAdd);
                    currentLine = new StringBuilder(word);
                    isFirstLine = false; // บรรทัดต่อไปไม่ใส่ indent
                } else {
                    currentLine = new StringBuilder(testLine);
                    if (isFirstLine && !indent.isEmpty()) {
                        // ลบ indent ออกจาก currentLine เพื่อคำนวณครั้งต่อไป
                        currentLine = new StringBuilder(testLine.substring(indent.length()));
                    }
                }
            }
            
            if (currentLine.length() > 0) {
                // ถ้าเป็นบรรทัดแรก ให้ใส่ indent
                String lineToAdd = isFirstLine && !indent.isEmpty() ? indent + currentLine.toString() : currentLine.toString();
                lines.add(lineToAdd);
            }
        }
        
        return lines;
    }
    
    /**
     * วางรูปภาพลายเซ็นลงใน PDF
     * 
     * @param contentStream PDPageContentStream
     * @param document PDDocument
     * @param imagePath path ของรูปภาพลายเซ็น (รองรับ classpath:images/signature.png หรือ file path)
     * @param x ตำแหน่ง x
     * @param y ตำแหน่ง y (จะวาดลงมาจากตำแหน่งนี้)
     * @return ตำแหน่ง y ใหม่หลังจากวาดรูปภาพ
     */
    private float drawSignatureImage(PDPageContentStream contentStream,
                                    PDDocument document,
                                    String imagePath,
                                    float x,
                                    float y) throws IOException {
        try {
            PDImageXObject signatureImage;
            
            // ตรวจสอบว่าเป็น classpath resource หรือ file path
            if (imagePath.startsWith("classpath:")) {
                // โหลดจาก classpath
                String resourcePath = imagePath.substring("classpath:".length());
                InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (imageStream != null) {
                    signatureImage = PDImageXObject.createFromByteArray(
                        document, imageStream.readAllBytes(), "signature");
                    imageStream.close();
                } else {
                    log.warn("Signature image not found in classpath: {}", resourcePath);
                    return y - 50f; // return default spacing
                }
            } else {
                // โหลดจาก file system
                ClassPathResource resource = new ClassPathResource(imagePath);
                if (resource.exists()) {
                    try (InputStream imageStream = resource.getInputStream()) {
                        signatureImage = PDImageXObject.createFromByteArray(
                            document, imageStream.readAllBytes(), "signature");
                    }
                } else {
                    log.warn("Signature image file not found: {}", imagePath);
                    return y - 50f; // return default spacing
                }
            }
            
            // กำหนดขนาดรูปภาพลายเซ็น (ปรับได้ตามต้องการ)
            float signatureWidth = 80f;  // ความกว้าง
            float signatureHeight = 40f; // ความสูง
            
            // วาดรูปภาพลายเซ็น (วาดจากล่างขึ้นบน)
            float imageY = y - signatureHeight;
            contentStream.drawImage(signatureImage, x, imageY, signatureWidth, signatureHeight);
            
            log.debug("Signature image drawn at ({}, {}), size: {}x{}", 
                     x, imageY, signatureWidth, signatureHeight);
            
            return imageY; // return ตำแหน่งล่างสุดของรูปภาพ
            
        } catch (Exception e) {
            log.error("Error drawing signature image: {}", e.getMessage());
            return y - 50f; // return default spacing กรณีเกิด error
        }
    }
    
    // ============================================
    // 📍 หนังสือส่งออก (Outgoing Letter)
    // ============================================
    
    /**
     * สร้างหนังสือส่งออก (หนังสือราชการภายนอก)
     * 
     * โครงสร้างตามรูปแบบราชการ:
     * - โลโก้ ETDA (ขวาบน)
     * - "ที่" (ซ้าย ว่างๆ) + ที่อยู่สำนักงาน (ขวา)
     * - วันที่ (ขวา ใต้ที่อยู่)
     * - เรื่อง (แสดงแม้ว่าง)
     * - เรียน (2 บรรทัด: หน่วยงาน + ที่อยู่)
     * - อ้างถึง (แสดงแม้ว่าง)
     * - สิ่งที่ส่งมาด้วย (แสดงแม้ว่าง)
     * - เนื้อหา
     * - ข้อความท้ายเอกสาร (เช่น "ขอแสดงความนับถือ")
     * - ลายเซ็น (1 จุด)
     */
    public String generateOutgoingLetterPdf(String bookNo,
                                            String address,
                                            String date,
                                            String title,
                                            String recipients,
                                            String recipientsAddress,
                                            String referTo,
                                            List<String> attachments,
                                            String content,
                                            String speedLayer,
                                            List<SignerInfo> signers,
                                            String salutation,
                                            String salutationEnding,
                                            String endDoc,
                                            String contactDepartment,
                                            String contactPhone,
                                            String contactFax,
                                            String contactEmail) throws Exception {
        log.info("=== Generating outgoing letter PDF ===");
        log.info("bookNo: {}", bookNo);
        log.info("address: {}", address);
        log.info("date: {}", date);
        log.info("title: {}", title);
        log.info("recipients: {}", recipients);
        log.info("referTo: {}", referTo);
        log.info("attachments count: {}", attachments != null ? attachments.size() : 0);
        log.info("content length: {}", content != null ? content.length() : 0);
        log.info("salutation: {}", salutation);
        log.info("salutationEnding: {}", salutationEnding);
        log.info("endDoc: {}", endDoc);
        log.info("signers count: {}", signers != null ? signers.size() : 0);
        log.info("contactDepartment: {}", contactDepartment);
        log.info("contactPhone: {}", contactPhone);
        log.info("contactFax: {}", contactFax);
        log.info("contactEmail: {}", contactEmail);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            log.info("PDF page created");
            
            // โหลด fonts
            log.info("Loading fonts...");
            PDFont fontRegular = loadThaiFont(document, FONT_PATH);
            PDFont fontBold = loadThaiFont(document, FONT_BOLD_PATH);
            log.info("Fonts loaded successfully");
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                log.info("Content stream created, starting to draw...");
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // วาดเลขที่หนังสือในหน้าแรกด้วย (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาดเส้นขอบ debug (ถ้าเปิด)
                drawDebugBorders(contentStream);
                
                // ============================================
                // 📍 SECTION 0: Logo ETDA (ขวาบน - ไม่ทับข้อความ)
                // ============================================
                float logoBottomY = yPosition; // ตำแหน่งล่างสุดของ logo
                try {
                    InputStream logoStream = getClass().getClassLoader()
                        .getResourceAsStream("images/logoETDA.png");
                    if (logoStream != null) {
                        PDImageXObject logoImage = PDImageXObject.createFromByteArray(
                            document, logoStream.readAllBytes(), "logo");
                        
                        // ตำแหน่งโลโก้ขวาบน
                        float logoX = PAGE_WIDTH - MARGIN_RIGHT - LOGO_WIDTH;
                        float logoY = yPosition - LOGO_HEIGHT;
                        logoBottomY = logoY; // เก็บตำแหน่งล่างสุดของ logo
                        
                        contentStream.drawImage(logoImage, logoX, logoY, LOGO_WIDTH, LOGO_HEIGHT);
                        log.info("ETDA logo drawn at ({}, {}), size: {}x{}", 
                                logoX, logoY, LOGO_WIDTH, LOGO_HEIGHT);
                        logoStream.close();
                    }
                } catch (Exception e) {
                    log.warn("Could not load ETDA logo: {}", e.getMessage());
                }
                
                // ============================================
                // 📍 SECTION 1: "ที่" (ซ้าย) + ที่อยู่สำนักงาน (ขวา)
                // ขยับลงมาใต้ logo เล็กน้อย
                // ============================================
                yPosition = logoBottomY - 15; // ขยับลงมาใต้ logo
                
                // วาด "ที่" ทางซ้าย (ไม่มีค่า - แสดงแค่หัวข้อ)
                float fieldY = yPosition;
                drawText(contentStream, "ที่", fontBold, FONT_SIZE_FIELD, MARGIN_LEFT, fieldY);
                
                // วาดที่อยู่สำนักงานทางขวา (ขยับขวาอีก แต่ไม่ตกขอบ)
                float addressX = PAGE_WIDTH - MARGIN_RIGHT - 180; // ขยับขวาอีก (ห่างจากขอบขวา 180pt)
                float addressY = fieldY;
                
                if (address != null && !address.isEmpty()) {
                    String[] addressLines = address.split("\n");
                    for (String line : addressLines) {
                        addressY = drawText(contentStream, line.trim(), fontRegular, FONT_SIZE_FIELD_VALUE, 
                                           addressX, addressY);
                    }
                }
                
                // ============================================
                // 📍 SECTION 2: วันที่ (ขวา ใต้ที่อยู่)
                // ============================================
                if (date != null && !date.isEmpty()) {
                    log.info("Drawing date: {}", date);
                    addressY -= 5;
                    drawText(contentStream, date, fontRegular, FONT_SIZE_FIELD_VALUE, addressX, addressY);
                }
                
                // กำหนด yPosition สำหรับส่วนถัดไป (ใช้ค่าต่ำสุด)
                yPosition = Math.min(fieldY - 30, addressY - 30);
                
                // ============================================
                // 📍 SECTION 3: เรื่อง (แสดงแม้ว่าง)
                // ============================================
                log.info("Drawing subject: {}", title);
                String titleValue = (title != null && !title.isEmpty()) ? title : "";
                yPosition = drawText(contentStream, "เรื่อง  " + titleValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // ============================================
                // 📍 SECTION 3.5: คำขึ้นต้น (salutation) ถ้ามี
                // เช่น "ขอประทานกราบทูล เรียนถึงคนนั้น"
                // ============================================
                if (salutation != null && !salutation.isEmpty()) {
                    log.info("Drawing salutation: {} {}", salutation, salutationEnding);
                    String salutationLine = salutation;
                    if (salutationEnding != null && !salutationEnding.isEmpty()) {
                        salutationLine += "  " + salutationEnding;
                    }
                    yPosition = drawText(contentStream, salutationLine, fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                    yPosition -= SPACING_BETWEEN_FIELDS;
                }
                
                // ============================================
                // 📍 SECTION 4: เรียน (2 บรรทัด: หน่วยงาน + ที่อยู่)
                // ============================================
                log.info("Drawing recipients: {}", recipients);
                // บรรทัดที่ 1: เรียน + ชื่อหน่วยงาน
                String recipientsValue = (recipients != null && !recipients.isEmpty()) ? recipients : "";
                yPosition = drawText(contentStream, "เรียน  " + recipientsValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                
                // บรรทัดที่ 2: เรียน + ที่อยู่หน่วยงานผู้รับ (ถ้ามี)
                if (recipientsAddress != null && !recipientsAddress.isEmpty()) {
                    yPosition = drawText(contentStream, "เรียน  " + recipientsAddress, fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // ============================================
                // 📍 SECTION 5: อ้างถึง (แสดงแม้ว่าง)
                // ============================================
                log.info("Drawing referTo: {}", referTo);
                String referToValue = (referTo != null && !referTo.isEmpty()) ? referTo : "";
                yPosition = drawText(contentStream, "อ้างถึง  " + referToValue, fontRegular, 
                                    FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // ============================================
                // 📍 SECTION 6: สิ่งที่ส่งมาด้วย (แสดงแม้ว่าง)
                // ============================================
                log.info("Drawing attachments");
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
                    // แสดง "สิ่งที่ส่งมาด้วย" แม้ไม่มีค่า
                    yPosition = drawText(contentStream, "สิ่งที่ส่งมาด้วย", fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, MARGIN_LEFT, yPosition);
                }
                yPosition -= SPACING_BETWEEN_FIELDS;
                
                // ============================================
                // 📍 SECTION 7: เนื้อหา (รองรับขึ้นหน้าใหม่)
                // ============================================
                if (content != null && !content.isEmpty()) {
                    yPosition -= SPACING_BEFORE_CONTENT;
                    log.info("Drawing content, length: {}", content.length());
                    
                    String[] lines = content.split("\n");
                    
                    for (String line : lines) {
                        if (yPosition < MIN_Y_POSITION) {
                            log.info("Content overflow, creating new page...");
                            contentStream.close();
                            
                            PDPage newPage = createNewPage(document, fontRegular, bookNo);
                            contentStream = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true);
                            yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                        }
                        
                        yPosition = drawMultilineText(contentStream, line, 
                                                    fontRegular, FONT_SIZE_CONTENT, 
                                                    MARGIN_LEFT, yPosition, 
                                                    PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                    }
                }
                
                // ============================================
                // 📍 SECTION 8: ข้อความท้ายเอกสาร (endDoc)
                // เช่น "ขอแสดงความนับถือ", "ควรมิควรแล้วแต่จะโปรดเกล้าฯ"
                // ============================================
                if (endDoc != null && !endDoc.isEmpty()) {
                    log.info("Drawing endDoc: {}", endDoc);
                    yPosition -= SPACING_BETWEEN_FIELDS * 2;
                    
                    // ตรวจสอบพื้นที่เหลือ
                    if (yPosition < MIN_Y_POSITION + 150) {
                        log.info("EndDoc overflow, creating new page...");
                        contentStream.close();
                        
                        PDPage newPage = createNewPage(document, fontRegular, bookNo);
                        contentStream = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true);
                        yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                    }
                    
                    // วาดข้อความท้ายเอกสาร (ทางขวา ตรงกับช่องลงนาม)
                    float endDocWidth = fontRegular.getStringWidth(endDoc) / 1000 * FONT_SIZE_FIELD_VALUE;
                    // ช่องลงนามอยู่ที่ PAGE_WIDTH / 2 + 20, กว้าง 180
                    float signatureBoxX = PAGE_WIDTH / 2 + 20;
                    float signatureBoxWidth = 180f;
                    float endDocX = signatureBoxX + (signatureBoxWidth - endDocWidth) / 2;
                    yPosition = drawText(contentStream, endDoc, fontRegular, 
                                        FONT_SIZE_FIELD_VALUE, endDocX, yPosition);
                    yPosition -= 20; // ระยะห่างระหว่าง endDoc กับช่องลงนาม
                }
                
                // ============================================
                // 📍 SECTION 9: ช่องลงนาม (1 จุด)
                // ใช้ผู้ลงนามคนแรกจาก bookSigned
                // ============================================
                if (signers != null && !signers.isEmpty()) {
                    log.info("Drawing single signature block for outgoing letter");
                    SignerInfo firstSigner = signers.get(0);
                    
                    // ตรวจสอบพื้นที่เหลือ
                    if (yPosition < MIN_Y_POSITION + 150) {
                        log.info("Signature overflow, creating new page...");
                        contentStream.close();
                        
                        PDPage newPage = createNewPage(document, fontRegular, bookNo);
                        contentStream = new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true);
                        yPosition = PAGE_HEIGHT - MARGIN_TOP - 50;
                    }
                    
                    yPosition -= 20;
                    
                    // สร้าง AcroForm ถ้ายังไม่มี
                    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
                    if (acroForm == null) {
                        acroForm = new PDAcroForm(document);
                        document.getDocumentCatalog().setAcroForm(acroForm);
                    }
                    
                    // วาดช่องลงนาม
                    yPosition = drawSignatureBlock(contentStream, document, acroForm,
                                                   firstSigner, "OutgoingSigner", 0, yPosition, fontRegular,
                                                   document.getNumberOfPages() - 1);
                }
                
                // ============================================
                // 📍 SECTION 10: ข้อมูลติดต่อ (มุมซ้ายล่าง)
                // ============================================
                float contactY = MARGIN_BOTTOM + 60; // ตำแหน่ง Y สำหรับข้อมูลติดต่อ
                float contactX = MARGIN_LEFT;
                float contactFontSize = 14f;
                
                if (contactDepartment != null && !contactDepartment.isEmpty()) {
                    log.info("Drawing contact info at bottom left");
                    drawText(contentStream, contactDepartment, fontRegular, contactFontSize, contactX, contactY);
                    contactY -= 18;
                }
                if (contactPhone != null && !contactPhone.isEmpty()) {
                    drawText(contentStream, "เลขหมาย " + contactPhone, fontRegular, contactFontSize, contactX, contactY);
                    contactY -= 18;
                }
                if (contactFax != null && !contactFax.isEmpty()) {
                    drawText(contentStream, "โทรสาร " + contactFax, fontRegular, contactFontSize, contactX, contactY);
                    contactY -= 18;
                }
                if (contactEmail != null && !contactEmail.isEmpty()) {
                    drawText(contentStream, "อีเมล " + contactEmail, fontRegular, contactFontSize, contactX, contactY);
                }

                log.info("All content drawn successfully");
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            log.info("Converting to Base64...");
            String base64 = convertToBase64(document);
            log.info("PDF generated successfully, Base64 length: {}", base64.length());
            return base64;
            
        } catch (Exception e) {
            log.error("Error generating outgoing letter PDF: ", e);
            throw new Exception("ไม่สามารถสร้าง PDF หนังสือส่งออกได้: " + e.getMessage(), e);
        }
    }
    
    // ============================================
    // 📍 เสนอผ่าน / ผู้เรียน - Signature Pages
    // ============================================
    
    /**
     * สร้าง PDF หน้า "เสนอผ่าน" พร้อม AcroForm Signature Fields
     * 
     * @param existingPdfBase64 PDF เดิมในรูปแบบ Base64 (จะต่อหน้าจากนี้)
     * @param submiters รายการผู้เสนอผ่าน
     * @param bookNo เลขที่หนังสือ (แสดงที่ขอบล่างซ้ายทุกหน้า)
     * @return PDF ที่มีหน้าเสนอผ่านต่อท้าย ในรูปแบบ Base64
     */
    public String addSubmitPages(String existingPdfBase64, 
                                  List<SignerInfo> submiters,
                                  String bookNo) throws Exception {
        if (submiters == null || submiters.isEmpty()) {
            return existingPdfBase64;
        }
        
        log.info("Adding submit pages for {} submiters", submiters.size());
        
        // แปลง Base64 เป็น bytes
        String cleanBase64 = existingPdfBase64;
        if (existingPdfBase64.startsWith("data:application/pdf;base64,")) {
            cleanBase64 = existingPdfBase64.substring("data:application/pdf;base64,".length());
        }
        byte[] pdfBytes = Base64.getDecoder().decode(cleanBase64);
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFont fontRegular = loadThaiFont(document, FONT_PATH);
            PDFont fontBold = loadThaiFont(document, FONT_BOLD_PATH);
            
            // คำนวณเลขหน้าที่จะต่อ
            int currentPageNumber = document.getNumberOfPages() + 1;
            
            // สร้างหน้าใหม่สำหรับ "เสนอผ่าน"
            PDPage newPage = new PDPage(PDRectangle.A4);
            document.addPage(newPage);
            
            // สร้าง AcroForm ถ้ายังไม่มี
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }
            
            PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
            try {
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาดเลขหน้า (ต่อจากหน้าเดิม)
                String pageNumThai = "- " + convertToThaiNumber(currentPageNumber) + " -";
                drawCenteredText(contentStream, pageNumThai, fontRegular, 16, yPosition);
                yPosition -= 50;
                
                // วาดหัวข้อ "เสนอผ่าน"
                drawCenteredText(contentStream, "เสนอผ่าน", fontBold, 28, yPosition);
                yPosition -= 80;
                
                // วาดลายเซ็นแต่ละคน
                for (int i = 0; i < submiters.size(); i++) {
                    SignerInfo submiter = submiters.get(i);
                    
                    // ตรวจสอบพื้นที่เหลือ - ถ้าไม่พอให้ขึ้นหน้าใหม่ (ลดจาก 150 เป็น 50 เพื่อวาง 3 คน/หน้า)
                    if (yPosition < MIN_Y_POSITION + 50) {
                        contentStream.close();
                        
                        // สร้างหน้าใหม่
                        PDPage nextPage = new PDPage(PDRectangle.A4);
                        document.addPage(nextPage);
                        currentPageNumber++;
                        
                        contentStream = new PDPageContentStream(document, nextPage);
                        yPosition = PAGE_HEIGHT - MARGIN_TOP;
                        
                        // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                        drawBookNumber(contentStream, bookNo, fontRegular);
                        
                        // วาดเลขหน้า
                        String nextPageNum = "- " + convertToThaiNumber(currentPageNumber) + " -";
                        drawCenteredText(contentStream, nextPageNum, fontRegular, 16, yPosition);
                        yPosition -= 50;
                    }
                    
                    // วาดลายเซ็น
                    yPosition = drawSignatureBlock(contentStream, document, acroForm,
                                                   submiter, "Submit", i, yPosition, fontRegular,
                                                   document.getNumberOfPages() - 1);
                    
                    // วาดเส้นแบ่ง (ถ้าไม่ใช่คนสุดท้าย)
                    if (i < submiters.size() - 1) {
                        yPosition = drawDashedLine(contentStream, yPosition);
                    }
                }
            } finally {
                contentStream.close();
            }
            
            return convertToBase64(document);
        }
    }
    
    /**
     * สร้าง PDF หน้า "ผู้เรียน/รับทราบ" พร้อม AcroForm Signature Fields
     * 
     * @param existingPdfBase64 PDF เดิมในรูปแบบ Base64 (จะต่อหน้าจากนี้)
     * @param learners รายการผู้เรียน/รับทราบ
     * @param signers รายการผู้ลงนาม (สำหรับแสดง "เรียน" ที่ด้านบน)
     * @param bookNo เลขที่หนังสือ (แสดงที่ขอบล่างซ้ายทุกหน้า)
     * @return PDF ที่มีหน้าผู้เรียนต่อท้าย ในรูปแบบ Base64
     */
    public String addLearnerPages(String existingPdfBase64, 
                                   List<SignerInfo> learners,
                                   List<SignerInfo> signers,
                                   String bookNo) throws Exception {
        if (learners == null || learners.isEmpty()) {
            return existingPdfBase64;
        }
        
        log.info("Adding learner pages for {} learners", learners.size());
        
        // แปลง Base64 เป็น bytes
        String cleanBase64 = existingPdfBase64;
        if (existingPdfBase64.startsWith("data:application/pdf;base64,")) {
            cleanBase64 = existingPdfBase64.substring("data:application/pdf;base64,".length());
        }
        byte[] pdfBytes = Base64.getDecoder().decode(cleanBase64);
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFont fontRegular = loadThaiFont(document, FONT_PATH);
            
            // คำนวณเลขหน้าที่จะต่อ
            int currentPageNumber = document.getNumberOfPages() + 1;
            
            // สร้างหน้าใหม่สำหรับ "ผู้เรียน/รับทราบ"
            PDPage newPage = new PDPage(PDRectangle.A4);
            document.addPage(newPage);
            
            // สร้าง AcroForm ถ้ายังไม่มี
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }
            
            PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
            try {
                float yPosition = PAGE_HEIGHT - MARGIN_TOP;
                
                // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                drawBookNumber(contentStream, bookNo, fontRegular);
                
                // วาดเลขหน้า
                String pageNumThai = "- " + convertToThaiNumber(currentPageNumber) + " -";
                drawCenteredText(contentStream, pageNumThai, fontRegular, 16, yPosition);
                yPosition -= 50;
                
                // วาด "เรียน ชื่อผู้ลงนาม1, ชื่อผู้ลงนาม2, ..." ที่ด้านบน (ใช้ signers ไม่ใช่ learners)
                StringBuilder namesBuilder = new StringBuilder("เรียน ");
                List<SignerInfo> displayNames = (signers != null && !signers.isEmpty()) ? signers : learners;
                for (int i = 0; i < displayNames.size(); i++) {
                    SignerInfo signer = displayNames.get(i);
                    String fullName = String.format("%s%s %s",
                        signer.getPrefixName() != null ? signer.getPrefixName() : "",
                        signer.getFirstname() != null ? signer.getFirstname() : "",
                        signer.getLastname() != null ? signer.getLastname() : "");
                    namesBuilder.append(fullName);
                    if (i < displayNames.size() - 1) {
                        namesBuilder.append(", ");
                    }
                }
                
                // วาดรายชื่อ (รองรับหลายบรรทัดถ้ายาวเกิน)
                yPosition = drawMultilineText(contentStream, namesBuilder.toString(), 
                                             fontRegular, 16, MARGIN_LEFT, yPosition,
                                             PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
                yPosition -= 50;
                
                // วาดลายเซ็นแต่ละคน
                for (int i = 0; i < learners.size(); i++) {
                    SignerInfo learner = learners.get(i);
                    
                    // ตรวจสอบพื้นที่เหลือ - ถ้าไม่พอให้ขึ้นหน้าใหม่ (ลดจาก 150 เป็น 50 เพื่อวาง 3 คน/หน้า)
                    if (yPosition < MIN_Y_POSITION + 50) {
                        contentStream.close();
                        
                        // สร้างหน้าใหม่
                        PDPage nextPage = new PDPage(PDRectangle.A4);
                        document.addPage(nextPage);
                        currentPageNumber++;
                        
                        contentStream = new PDPageContentStream(document, nextPage);
                        yPosition = PAGE_HEIGHT - MARGIN_TOP;
                        
                        // วาดเลขที่หนังสือ (ขอบล่างซ้าย)
                        drawBookNumber(contentStream, bookNo, fontRegular);
                        
                        // วาดเลขหน้า
                        String nextPageNum = "- " + convertToThaiNumber(currentPageNumber) + " -";
                        drawCenteredText(contentStream, nextPageNum, fontRegular, 16, yPosition);
                        yPosition -= 50;
                    }
                    
                    // วาดลายเซ็น
                    yPosition = drawSignatureBlock(contentStream, document, acroForm,
                                                   learner, "Learner", i, yPosition, fontRegular,
                                                   document.getNumberOfPages() - 1);
                    
                    // วาดเส้นแบ่ง (ถ้าไม่ใช่คนสุดท้าย)
                    if (i < learners.size() - 1) {
                        yPosition = drawDashedLine(contentStream, yPosition);
                    }
                }
            } finally {
                contentStream.close();
            }
            
            return convertToBase64(document);
        }
    }
    
    /**
     * วาดกรอบลายเซ็น (Signature Block) พร้อม AcroForm Field
     * แสดงกรอบสีฟ้าประ พร้อมชื่อและตำแหน่ง
     */
    private float drawSignatureBlock(PDPageContentStream contentStream,
                                     PDDocument document,
                                     PDAcroForm acroForm,
                                     SignerInfo signer,
                                     String fieldPrefix,
                                     int index,
                                     float yPosition,
                                     PDFont font,
                                     int pageIndex) throws IOException {
        // กำหนดตำแหน่งกรอบลายเซ็น (ตรงกลาง-ขวา)
        float boxWidth = 180f;
        float boxHeight = 50f;
        float boxX = PAGE_WIDTH / 2 + 20;  // ขยับไปทางขวา
        float boxY = yPosition - boxHeight;
        
        // วาดกรอบสีฟ้าประ (dashed border)
        contentStream.setStrokingColor(0.4f, 0.7f, 0.9f); // สีฟ้าอ่อน
        contentStream.setLineDashPattern(new float[]{5, 3}, 0); // เส้นประ
        contentStream.setLineWidth(1.5f);
        contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
        contentStream.stroke();
        
        // รีเซ็ตเส้นกลับเป็นปกติ
        contentStream.setLineDashPattern(new float[]{}, 0);
        contentStream.setStrokingColor(0, 0, 0);
        
        // วาดข้อความในกรอบตาม fieldPrefix
        // "Submit" = "เสนอผ่าน", "Learner" หรือ "OutgoingSigner" = "เรียน", อื่นๆ = "รับทราบ"
        String labelText;
        if ("Submit".equals(fieldPrefix)) {
            labelText = "เสนอผ่าน";
        } else if ("Learner".equals(fieldPrefix) || "OutgoingSigner".equals(fieldPrefix)) {
            labelText = "เรียน";
        } else {
            labelText = "รับทราบ";
        }
        float labelWidth = font.getStringWidth(labelText) / 1000 * 14;
        float textX = boxX + (boxWidth - labelWidth) / 2;
        float textY = boxY + (boxHeight / 2) - 5;
        drawText(contentStream, labelText, font, 14, textX, textY);
        
        yPosition = boxY - 15;
        
        // วาดชื่อ (พร้อมวงเล็บ)
        String fullName = String.format("(%s%s %s)",
            signer.getPrefixName() != null ? signer.getPrefixName() : "",
            signer.getFirstname() != null ? signer.getFirstname() : "",
            signer.getLastname() != null ? signer.getLastname() : "");
        
        float nameWidth = font.getStringWidth(fullName) / 1000 * 14;
        float nameX = boxX + (boxWidth - nameWidth) / 2;
        drawText(contentStream, fullName, font, 14, nameX, yPosition);
        yPosition -= 20;
        
        // วาดตำแหน่ง
        if (signer.getPositionName() != null && !signer.getPositionName().isEmpty()) {
            float posWidth = font.getStringWidth(signer.getPositionName()) / 1000 * 12;
            float posX = boxX + (boxWidth - posWidth) / 2;
            drawText(contentStream, signer.getPositionName(), font, 12, posX, yPosition);
            yPosition -= 25;
        }
        
        // สร้าง AcroForm Signature Field
        try {
            String fieldName = fieldPrefix + "_" + index + "_" + 
                              (signer.getEmail() != null ? signer.getEmail().replace("@", "_").replace(".", "_") : "user" + index);
            
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName(fieldName);
            
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(boxX, boxY, boxWidth, boxHeight);
            widget.setRectangle(rect);
            widget.setPage(document.getPage(pageIndex));
            
            // เพิ่ม widget เข้า page
            document.getPage(pageIndex).getAnnotations().add(widget);
            
            acroForm.getFields().add(signatureField);
            
            log.debug("Created signature field: {} at ({}, {})", fieldName, boxX, boxY);
            
        } catch (Exception e) {
            log.warn("Could not create AcroForm signature field: {}", e.getMessage());
        }
        
        return yPosition - 20;
    }
    
    /**
     * วาดเส้นประแบ่งระหว่างผู้ลงนามแต่ละคน
     */
    private float drawDashedLine(PDPageContentStream contentStream, float yPosition) throws IOException {
        float lineY = yPosition - 15;
        
        contentStream.setStrokingColor(0.6f, 0.6f, 0.6f); // สีเทา
        contentStream.setLineDashPattern(new float[]{8, 4}, 0); // เส้นประ
        contentStream.setLineWidth(0.5f);
        
        contentStream.moveTo(MARGIN_LEFT + 50, lineY);
        contentStream.lineTo(PAGE_WIDTH - MARGIN_RIGHT - 50, lineY);
        contentStream.stroke();
        
        // รีเซ็ตเส้น
        contentStream.setLineDashPattern(new float[]{}, 0);
        contentStream.setStrokingColor(0, 0, 0);
        
        return lineY - 30;
    }
    
    /**
     * Inner class สำหรับข้อมูลผู้ลงนาม
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
    
    // ============================================
    // PDF Merge Methods (PDFBox 2.x)
    // ตาม migrateToJava.txt Section 8.3
    // ============================================
    
    /**
     * รวม PDF หลายไฟล์ (PDFBox 2.x)
     * 
     * แปลงมาจาก: MergeMultiplePdfFiles() method
     * ใช้ PDFMergerUtility + MemoryUsageSetting ตาม migrateToJava.txt
     * 
     * @param pdfBytesList รายการ PDF ในรูปแบบ byte array
     * @return PDF รวมในรูปแบบ byte array
     */
    public byte[] mergePdfFiles(List<byte[]> pdfBytesList) throws IOException {
        if (pdfBytesList == null || pdfBytesList.isEmpty()) {
            throw new IllegalArgumentException("PDF list is empty");
        }
        
        if (pdfBytesList.size() == 1) {
            return pdfBytesList.get(0);
        }
        
        PDFMergerUtility merger = new PDFMergerUtility();
        
        for (byte[] pdfBytes : pdfBytesList) {
            merger.addSource(new ByteArrayInputStream(pdfBytes));
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outputStream);
        
        // ใช้ MemoryUsageSetting ตาม migrateToJava.txt Section 9.5
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        
        log.info("Merged {} PDFs successfully", pdfBytesList.size());
        return outputStream.toByteArray();
    }
    
    /**
     * รวม PDF 2 ไฟล์ (PDFBox 2.x)
     * 
     * @param pdf1 PDF แรก
     * @param pdf2 PDF ที่สอง
     * @return PDF รวม
     */
    public byte[] mergeTwoPdfs(byte[] pdf1, byte[] pdf2) throws IOException {
        List<byte[]> pdfList = new ArrayList<>();
        pdfList.add(pdf1);
        pdfList.add(pdf2);
        return mergePdfFiles(pdfList);
    }
    
    /**
     * รวม PDF จาก Base64 strings
     * 
     * @param base64Pdfs รายการ PDF ในรูปแบบ Base64
     * @return PDF รวมในรูปแบบ Base64
     */
    public String mergePdfBase64(List<String> base64Pdfs) throws IOException {
        List<byte[]> pdfBytesList = new ArrayList<>();
        
        for (String base64 : base64Pdfs) {
            // ลบ prefix "data:application/pdf;base64," ถ้ามี
            String cleanBase64 = base64;
            if (base64.startsWith("data:application/pdf;base64,")) {
                cleanBase64 = base64.substring("data:application/pdf;base64,".length());
            }
            pdfBytesList.add(Base64.getDecoder().decode(cleanBase64));
        }
        
        byte[] mergedPdf = mergePdfFiles(pdfBytesList);
        return Base64.getEncoder().encodeToString(mergedPdf);
    }
    
    // ============================================
    // Utility Methods
    // ============================================
    
    /**
     * แปลงเลขอารบิกเป็นเลขไทย (public)
     * 
     * ตาม migrateToJava.txt Section 8.4
     */
    public String convertToThaiDigits(int number) {
        return convertToThaiNumber(number);
    }
    
    /**
     * แปลงเดือนเป็นภาษาไทย
     * 
     * ตาม migrateToJava.txt Section 7.4
     */
    public String getThaiMonth(int month) {
        String[] thaiMonths = {
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", 
            "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม",
            "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
        };
        
        if (month >= 1 && month <= 12) {
            return thaiMonths[month - 1];
        }
        return "";
    }
    
    /**
     * แปลงวันที่เป็นรูปแบบไทย (วัน เดือน พ.ศ.)
     * 
     * ตาม migrateToJava.txt Section 7.4
     */
    public String toThaiDate(int day, int month, int year) {
        String thaiDay = convertToThaiNumber(day);
        String thaiMonth = getThaiMonth(month);
        String thaiYear = convertToThaiNumber(year + 543); // แปลง ค.ศ. เป็น พ.ศ.
        
        return String.format("%s %s %s", thaiDay, thaiMonth, thaiYear);
    }
}
