package th.go.etda.sarabun.pdf.service.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * HTML Content Renderer - แปลง HTML เป็น PDF elements
 * 
 * ใช้ OpenHTMLtoPDF library ที่ทำงานร่วมกับ PDFBox
 * รองรับ:
 * - HTML5 tags (p, div, span, etc.)
 * - Tables (table, tr, td, th)
 * - Images (base64 และ URL)
 * - CSS styling
 * - Thai fonts
 */
@Slf4j
@Service
public class HtmlContentRenderer {
    
    private static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    private static final String FONT_BOLD_PATH = "fonts/THSarabunNew Bold.ttf";
    
    // Page settings (A4)
    private static final float PAGE_WIDTH_PT = 595f;
    private static final float PAGE_HEIGHT_PT = 842f;
    private static final float MARGIN_LEFT = 70f;
    private static final float MARGIN_RIGHT = 70f;
    private static final float MARGIN_TOP = 70f;
    private static final float MARGIN_BOTTOM = 70f;
    
    /**
     * แปลง HTML content เป็น PDF bytes
     * 
     * @param htmlContent เนื้อหา HTML (ไม่ต้องมี html, head, body tags)
     * @return PDF bytes
     */
    public byte[] renderHtmlToPdf(String htmlContent) throws Exception {
        return renderHtmlToPdf(htmlContent, PAGE_WIDTH_PT, PAGE_HEIGHT_PT);
    }
    
    /**
     * แปลง HTML content เป็น PDF bytes พร้อมกำหนดขนาดหน้า
     * 
     * @param htmlContent เนื้อหา HTML
     * @param pageWidth ความกว้างหน้า (points)
     * @param pageHeight ความสูงหน้า (points)
     * @return PDF bytes
     */
    public byte[] renderHtmlToPdf(String htmlContent, float pageWidth, float pageHeight) throws Exception {
        log.info("Rendering HTML content to PDF, content length: {}", htmlContent.length());
        
        // Wrap HTML content with proper structure + Thai font CSS
        String fullHtml = wrapWithTemplate(htmlContent, pageWidth, pageHeight);
        
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            
            // Register Thai fonts
            registerFonts(builder);
            
            // Set HTML content
            builder.withHtmlContent(fullHtml, null);
            builder.toStream(os);
            builder.run();
            
            log.info("HTML to PDF conversion successful, output size: {} bytes", os.size());
            return os.toByteArray();
        }
    }
    
    /**
     * แปลง HTML content เป็น PDF Base64 string
     * 
     * @param htmlContent เนื้อหา HTML
     * @return PDF ในรูปแบบ Base64
     */
    public String renderHtmlToPdfBase64(String htmlContent) throws Exception {
        byte[] pdfBytes = renderHtmlToPdf(htmlContent);
        return Base64.getEncoder().encodeToString(pdfBytes);
    }
    
    /**
     * แปลง HTML content เป็น PDF และคืนค่าเป็น PDDocument
     * (สำหรับใช้ในการ merge กับ PDF อื่น)
     * 
     * ⚠️ IMPORTANT: ผู้เรียกต้องปิด PDDocument ด้วย try-with-resources หรือ close() เสมอ
     * เพื่อป้องกัน Memory Leak
     * 
     * ตัวอย่างการใช้งาน:
     * <pre>
     * try (PDDocument doc = renderer.renderHtmlToPDDocument(html)) {
     *     // ใช้งาน doc
     * }
     * </pre>
     * 
     * @param htmlContent เนื้อหา HTML
     * @return PDDocument (ผู้เรียกต้องปิดเอง)
     */
    public PDDocument renderHtmlToPDDocument(String htmlContent) throws Exception {
        byte[] pdfBytes = renderHtmlToPdf(htmlContent);
        return PDDocument.load(new ByteArrayInputStream(pdfBytes));
    }
    
    /**
     * Render HTML และ insert เข้าไปใน PDF ที่มีอยู่
     * 
     * @param existingPdfBase64 PDF เดิมในรูปแบบ Base64
     * @param htmlContent เนื้อหา HTML ที่จะเพิ่ม
     * @return PDF ที่รวมแล้วในรูปแบบ Base64
     */
    public String appendHtmlToPdf(String existingPdfBase64, String htmlContent) throws Exception {
        // แปลง HTML เป็น PDF
        byte[] htmlPdfBytes = renderHtmlToPdf(htmlContent);
        
        // แปลง existing PDF จาก Base64
        byte[] existingPdfBytes = Base64.getDecoder().decode(existingPdfBase64);
        
        // Merge PDFs with try-with-resources
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.addSource(new ByteArrayInputStream(existingPdfBytes));
        merger.addSource(new ByteArrayInputStream(htmlPdfBytes));
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            merger.setDestinationStream(outputStream);
            // ใช้ setupMixed เพื่อป้องกัน OOM กรณี PDF ขนาดใหญ่ (threshold 10MB)
            merger.mergeDocuments(MemoryUsageSetting.setupMixed(10 * 1024 * 1024));
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }
    
    /**
     * Register Thai fonts กับ PDF renderer
     */
    private void registerFonts(PdfRendererBuilder builder) {
        try {
            // Load font files from classpath
            ClassPathResource regularFontResource = new ClassPathResource(FONT_PATH);
            ClassPathResource boldFontResource = new ClassPathResource(FONT_BOLD_PATH);
            
            if (regularFontResource.exists()) {
                builder.useFont(() -> {
                    try {
                        return regularFontResource.getInputStream();
                    } catch (Exception e) {
                        log.error("Error loading regular font", e);
                        return null;
                    }
                }, "THSarabunNew");
                log.debug("Registered THSarabunNew regular font");
            }
            
            if (boldFontResource.exists()) {
                builder.useFont(() -> {
                    try {
                        return boldFontResource.getInputStream();
                    } catch (Exception e) {
                        log.error("Error loading bold font", e);
                        return null;
                    }
                }, "THSarabunNew", 700, com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true);
                log.debug("Registered THSarabunNew bold font");
            }
            
        } catch (Exception e) {
            log.warn("Could not register Thai fonts: {}", e.getMessage());
        }
    }
    
    /**
     * Wrap HTML content with proper HTML structure + Thai font CSS
     */
    private String wrapWithTemplate(String content, float pageWidth, float pageHeight) {
        // Calculate content area dimensions
        float contentWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;
        float contentHeight = pageHeight - MARGIN_TOP - MARGIN_BOTTOM;
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                    @page {
                        size: %spx %spx;
                        margin: 0;
                    }
                    
                    body {
                        font-family: 'THSarabunNew', 'TH Sarabun New', 'Sarabun', sans-serif;
                        font-size: 16pt;
                        line-height: 1.4;
                        margin: 0;
                        padding: 0;
                        color: #000;
                    }
                    
                    .content-wrapper {
                        width: %spx;
                        padding: 0;
                        margin: 0;
                    }
                    
                    /* Paragraph styles */
                    p {
                        margin: 0 0 8pt 0;
                        text-align: justify;
                    }
                    
                    /* Table styles */
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin: 10pt 0;
                        font-size: 14pt;
                    }
                    
                    th, td {
                        border: 1px solid #000;
                        padding: 6pt 8pt;
                        text-align: left;
                        vertical-align: top;
                    }
                    
                    th {
                        background-color: #f0f0f0;
                        font-weight: bold;
                        text-align: center;
                    }
                    
                    /* List styles */
                    ul, ol {
                        margin: 8pt 0;
                        padding-left: 24pt;
                    }
                    
                    li {
                        margin-bottom: 4pt;
                    }
                    
                    /* Heading styles */
                    h1, h2, h3, h4, h5, h6 {
                        margin: 12pt 0 8pt 0;
                        font-weight: bold;
                    }
                    
                    h1 { font-size: 24pt; }
                    h2 { font-size: 20pt; }
                    h3 { font-size: 18pt; }
                    h4 { font-size: 16pt; }
                    
                    /* Indent for Thai document style */
                    .indent {
                        text-indent: 40pt;
                    }
                    
                    /* Strong/Bold */
                    strong, b {
                        font-weight: bold;
                    }
                    
                    /* Emphasis/Italic */
                    em, i {
                        font-style: italic;
                    }
                    
                    /* Underline */
                    u {
                        text-decoration: underline;
                    }
                    
                    /* Center alignment */
                    .center, .text-center {
                        text-align: center;
                    }
                    
                    /* Right alignment */
                    .right, .text-right {
                        text-align: right;
                    }
                    
                    /* Image styles */
                    img {
                        max-width: 100%%;
                        height: auto;
                    }
                    
                    /* Horizontal rule */
                    hr {
                        border: none;
                        border-top: 1px solid #000;
                        margin: 12pt 0;
                    }
                    
                    /* Preserve whitespace */
                    .preserve-space {
                        white-space: pre-wrap;
                    }
                </style>
            </head>
            <body>
                <div class="content-wrapper">
                    %s
                </div>
            </body>
            </html>
            """.formatted(
                String.valueOf((int) pageWidth),
                String.valueOf((int) pageHeight),
                String.valueOf((int) contentWidth),
                content
            );
    }
    
    /**
     * ตรวจสอบว่า content เป็น HTML หรือไม่
     * 
     * @param content เนื้อหาที่ต้องการตรวจสอบ
     * @return true ถ้าเป็น HTML
     */
    public boolean isHtmlContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim().toLowerCase();
        
        // Check for common HTML patterns
        return trimmed.contains("<p>") || 
               trimmed.contains("<p ") ||
               trimmed.contains("<table") ||
               trimmed.contains("<div") ||
               trimmed.contains("<span") ||
               trimmed.contains("<br") ||
               trimmed.contains("<ul") ||
               trimmed.contains("<ol") ||
               trimmed.contains("<li") ||
               trimmed.contains("<h1") ||
               trimmed.contains("<h2") ||
               trimmed.contains("<h3") ||
               trimmed.contains("<img") ||
               trimmed.contains("<strong") ||
               trimmed.contains("<em") ||
               trimmed.contains("<b>") ||
               trimmed.contains("<i>") ||
               trimmed.contains("<u>") ||
               trimmed.startsWith("<!doctype") ||
               trimmed.startsWith("<html");
    }
    
    /**
     * แปลง plain text เป็น HTML (preserve whitespace และ newlines)
     * 
     * @param plainText ข้อความ plain text
     * @return HTML string
     */
    public String plainTextToHtml(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        
        // Escape HTML entities
        String escaped = plainText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
        
        // Convert newlines to <br>
        escaped = escaped.replace("\n", "<br/>");
        
        // Preserve leading spaces (Thai document indentation)
        escaped = escaped.replaceAll("^(\\s+)", "<span style=\"white-space:pre\">$1</span>");
        escaped = escaped.replaceAll("<br/>(\\s+)", "<br/><span style=\"white-space:pre\">$1</span>");
        
        return escaped;
    }
}
