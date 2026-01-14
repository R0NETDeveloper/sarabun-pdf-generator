package th.go.etda.sarabun.pdf.service.pdf;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TableRenderer - วาดตาราง HTML ลงใน PDFBox โดยตรง
 * 
 * รองรับ:
 * - &lt;table&gt;, &lt;tr&gt;, &lt;th&gt;, &lt;td&gt;
 * - colspan, rowspan (พื้นฐาน)
 * - border, padding
 * - text wrapping ภายใน cell
 * - ตัดหน้าอัตโนมัติเมื่อตารางยาว
 */
@Slf4j
public class TableRenderer {
    
    // Default settings
    private static final float DEFAULT_CELL_PADDING = 5f;
    private static final float DEFAULT_BORDER_WIDTH = 0.5f;
    private static final float DEFAULT_FONT_SIZE = 14f;
    private static final float MIN_ROW_HEIGHT = 20f;
    
    /**
     * ข้อมูล Cell ที่ parse มาจาก HTML
     */
    @Data
    @Builder
    public static class TableCell {
        private String text;
        private int colspan;
        private int rowspan;
        private boolean isHeader;
        private float width;
    }
    
    /**
     * ข้อมูล Row
     */
    @Data
    @Builder
    public static class TableRow {
        private List<TableCell> cells;
        private float height;
    }
    
    /**
     * ข้อมูลตารางทั้งหมด
     */
    @Data
    @Builder
    public static class TableData {
        private List<TableRow> rows;
        private List<Float> columnWidths;
        private int numColumns;
    }
    
    /**
     * Result จากการวาดตาราง
     */
    @Data
    @Builder
    public static class DrawResult {
        private float newYPosition;
        private PDPageContentStream contentStream;
        private PDPage currentPage;
        private boolean needsNewPage;
    }
    
    /**
     * Context สำหรับวาดตาราง (รวมข้อมูลที่จำเป็นทั้งหมด)
     */
    @Data
    @Builder
    public static class DrawContext {
        private PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private PDFont font;
        private PDFont fontBold;
        private float fontSize;
        private float startX;
        private float startY;
        private float maxWidth;
        private float marginTop;
        private float marginBottom;
        private float pageHeight;
        private String bookNo;  // สำหรับวาดเลขที่หนังสือเมื่อขึ้นหน้าใหม่
    }
    
    /**
     * Parse HTML table เป็น TableData
     * 
     * @param htmlTable HTML string ที่มี &lt;table&gt;
     * @param maxWidth ความกว้างสูงสุดของตาราง
     * @return TableData
     */
    public TableData parseHtmlTable(String htmlTable, float maxWidth) {
        Document doc = Jsoup.parse(htmlTable);
        Element table = doc.selectFirst("table");
        
        if (table == null) {
            log.warn("No table found in HTML");
            return null;
        }
        
        List<TableRow> rows = new ArrayList<>();
        int maxCols = 0;
        
        // Parse rows
        Elements trs = table.select("tr");
        for (Element tr : trs) {
            List<TableCell> cells = new ArrayList<>();
            Elements tds = tr.select("th, td");
            
            int colCount = 0;
            for (Element td : tds) {
                int colspan = 1;
                int rowspan = 1;
                
                try {
                    String colspanAttr = td.attr("colspan");
                    if (!colspanAttr.isEmpty()) {
                        colspan = Integer.parseInt(colspanAttr);
                    }
                } catch (NumberFormatException ignored) {}
                
                try {
                    String rowspanAttr = td.attr("rowspan");
                    if (!rowspanAttr.isEmpty()) {
                        rowspan = Integer.parseInt(rowspanAttr);
                    }
                } catch (NumberFormatException ignored) {}
                
                boolean isHeader = td.tagName().equalsIgnoreCase("th");
                String text = td.text().trim();
                
                cells.add(TableCell.builder()
                    .text(text)
                    .colspan(colspan)
                    .rowspan(rowspan)
                    .isHeader(isHeader)
                    .build());
                
                colCount += colspan;
            }
            
            maxCols = Math.max(maxCols, colCount);
            rows.add(TableRow.builder().cells(cells).build());
        }
        
        // คำนวณความกว้างของแต่ละ column (แบ่งเท่าๆ กัน)
        List<Float> columnWidths = new ArrayList<>();
        float colWidth = maxWidth / maxCols;
        for (int i = 0; i < maxCols; i++) {
            columnWidths.add(colWidth);
        }
        
        return TableData.builder()
            .rows(rows)
            .columnWidths(columnWidths)
            .numColumns(maxCols)
            .build();
    }
    
    /**
     * วาดตารางลงใน PDF
     * 
     * @param ctx DrawContext รวมข้อมูลทั้งหมด
     * @param tableData ข้อมูลตาราง
     * @return DrawResult
     */
    public DrawResult drawTable(DrawContext ctx, TableData tableData) throws IOException {
        if (tableData == null || tableData.getRows().isEmpty()) {
            return DrawResult.builder()
                .newYPosition(ctx.getStartY())
                .contentStream(ctx.getContentStream())
                .currentPage(ctx.getPage())
                .build();
        }
        
        float currentY = ctx.getStartY();
        PDPageContentStream stream = ctx.getContentStream();
        PDPage currentPage = ctx.getPage();
        PDDocument document = ctx.getDocument();
        
        List<Float> colWidths = tableData.getColumnWidths();
        
        for (TableRow row : tableData.getRows()) {
            // คำนวณความสูงของ row (จาก text wrapping)
            float rowHeight = calculateRowHeight(row, colWidths, ctx.getFont(), ctx.getFontSize());
            
            // ตรวจสอบว่าต้องขึ้นหน้าใหม่หรือไม่
            if (currentY - rowHeight < ctx.getMarginBottom()) {
                // ปิด content stream เดิม
                stream.close();
                
                // สร้างหน้าใหม่
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                
                stream = new PDPageContentStream(document, currentPage);
                currentY = ctx.getPageHeight() - ctx.getMarginTop() - 30; // ลดลงเล็กน้อยจาก top margin
                
                // วาดหมายเลขหน้า (ถ้าต้องการ)
                int pageNum = document.getNumberOfPages();
                drawPageNumber(stream, pageNum, ctx.getFont());
                
                log.info("Table continued on new page {}", pageNum);
            }
            
            // วาด row
            drawRow(stream, row, ctx.getStartX(), currentY, colWidths, ctx.getFont(), ctx.getFontBold(), ctx.getFontSize());
            
            currentY -= rowHeight;
        }
        
        return DrawResult.builder()
            .newYPosition(currentY - 10) // เว้นระยะหลังตาราง
            .contentStream(stream)
            .currentPage(currentPage)
            .build();
    }
    
    /**
     * วาด row เดียว
     */
    private void drawRow(PDPageContentStream stream, 
                         TableRow row,
                         float startX, 
                         float startY,
                         List<Float> colWidths,
                         PDFont font,
                         PDFont fontBold,
                         float fontSize) throws IOException {
        
        float rowHeight = calculateRowHeight(row, colWidths, font, fontSize);
        float currentX = startX;
        int colIndex = 0;
        
        for (TableCell cell : row.getCells()) {
            // คำนวณความกว้างของ cell (รวม colspan)
            float cellWidth = 0;
            for (int i = 0; i < cell.getColspan() && (colIndex + i) < colWidths.size(); i++) {
                cellWidth += colWidths.get(colIndex + i);
            }
            
            // วาดกรอบ cell
            stream.setStrokingColor(Color.BLACK);
            stream.setLineWidth(DEFAULT_BORDER_WIDTH);
            stream.addRect(currentX, startY - rowHeight, cellWidth, rowHeight);
            stream.stroke();
            
            // วาดพื้นหลังสำหรับ header (สีเทาอ่อน)
            if (cell.isHeader()) {
                stream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                stream.addRect(currentX + 0.5f, startY - rowHeight + 0.5f, cellWidth - 1, rowHeight - 1);
                stream.fill();
                stream.setNonStrokingColor(Color.BLACK);
            }
            
            // วาดข้อความใน cell
            PDFont cellFont = cell.isHeader() ? fontBold : font;
            float textX = currentX + DEFAULT_CELL_PADDING;
            float textY = startY - DEFAULT_CELL_PADDING - fontSize;
            
            // วาดข้อความ (รองรับ wrap)
            List<String> lines = wrapText(cell.getText(), cellFont, fontSize, cellWidth - (2 * DEFAULT_CELL_PADDING));
            for (String line : lines) {
                if (textY > startY - rowHeight + DEFAULT_CELL_PADDING) {
                    stream.beginText();
                    stream.setFont(cellFont, fontSize);
                    stream.newLineAtOffset(textX, textY);
                    stream.showText(line);
                    stream.endText();
                    textY -= fontSize + 2;
                }
            }
            
            currentX += cellWidth;
            colIndex += cell.getColspan();
        }
    }
    
    /**
     * คำนวณความสูงของ row
     */
    private float calculateRowHeight(TableRow row, List<Float> colWidths, PDFont font, float fontSize) 
            throws IOException {
        float maxHeight = MIN_ROW_HEIGHT;
        int colIndex = 0;
        
        for (TableCell cell : row.getCells()) {
            // คำนวณความกว้างของ cell (รวม colspan)
            float cellWidth = 0;
            for (int i = 0; i < cell.getColspan() && (colIndex + i) < colWidths.size(); i++) {
                cellWidth += colWidths.get(colIndex + i);
            }
            
            // คำนวณจำนวนบรรทัดที่ต้องการ
            List<String> lines = wrapText(cell.getText(), font, fontSize, cellWidth - (2 * DEFAULT_CELL_PADDING));
            float cellHeight = (lines.size() * (fontSize + 2)) + (2 * DEFAULT_CELL_PADDING);
            
            maxHeight = Math.max(maxHeight, cellHeight);
            colIndex += cell.getColspan();
        }
        
        return maxHeight;
    }
    
    /**
     * Wrap text ให้พอดีกับความกว้าง
     */
    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
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
     * วาดหมายเลขหน้า
     */
    private void drawPageNumber(PDPageContentStream stream, int pageNumber, PDFont font) throws IOException {
        // แปลงเป็นเลขไทย
        String thaiNum = convertToThaiNumber(pageNumber);
        String pageText = "- " + thaiNum + " -";
        
        float fontSize = 16f;
        float textWidth = font.getStringWidth(pageText) / 1000 * fontSize;
        float x = (PDRectangle.A4.getWidth() - textWidth) / 2;
        float y = PDRectangle.A4.getHeight() - 55; // ตำแหน่ง y ของเลขหน้า
        
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(pageText);
        stream.endText();
    }
    
    /**
     * แปลงเลขอารบิกเป็นเลขไทย
     */
    private String convertToThaiNumber(int number) {
        char[] thaiDigits = {'๐', '๑', '๒', '๓', '๔', '๕', '๖', '๗', '๘', '๙'};
        StringBuilder result = new StringBuilder();
        String numStr = String.valueOf(number);
        for (char c : numStr.toCharArray()) {
            if (Character.isDigit(c)) {
                result.append(thaiDigits[c - '0']);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * ตรวจสอบว่า HTML มีตารางหรือไม่
     */
    public static boolean containsTable(String html) {
        if (html == null || html.isEmpty()) {
            return false;
        }
        String lower = html.toLowerCase();
        return lower.contains("<table");
    }
    
    /**
     * แยกตารางออกจาก HTML
     * คืน List ของ HTML table strings
     */
    public static List<String> extractTables(String html) {
        List<String> tables = new ArrayList<>();
        if (html == null || html.isEmpty()) {
            return tables;
        }
        
        Document doc = Jsoup.parse(html);
        Elements tableElements = doc.select("table");
        
        for (Element table : tableElements) {
            tables.add(table.outerHtml());
        }
        
        return tables;
    }
    
    /**
     * แยก HTML content เป็นส่วนๆ: ข้อความก่อนตาราง, ตาราง, ข้อความหลังตาราง
     * 
     * @param html HTML string ที่อาจมีตารางและข้อความผสมกัน
     * @return List ของ ContentPart (text หรือ table)
     */
    public static List<ContentPart> splitHtmlContent(String html) {
        List<ContentPart> parts = new ArrayList<>();
        if (html == null || html.isEmpty()) {
            return parts;
        }
        
        Document doc = Jsoup.parse(html);
        Element body = doc.body();
        
        StringBuilder textBuffer = new StringBuilder();
        
        for (org.jsoup.nodes.Node node : body.childNodes()) {
            if (node instanceof Element) {
                Element elem = (Element) node;
                if (elem.tagName().equalsIgnoreCase("table")) {
                    // ถ้ามี text สะสมอยู่ ให้ flush ก่อน
                    if (textBuffer.length() > 0) {
                        String text = textBuffer.toString().trim();
                        if (!text.isEmpty()) {
                            parts.add(new ContentPart(ContentPartType.TEXT, text));
                        }
                        textBuffer = new StringBuilder();
                    }
                    // เพิ่มตาราง
                    parts.add(new ContentPart(ContentPartType.TABLE, elem.outerHtml()));
                } else {
                    // ดึง text จาก element (รวม <p>, <div>, etc.)
                    textBuffer.append(elem.text()).append("\n");
                }
            } else if (node instanceof org.jsoup.nodes.TextNode) {
                org.jsoup.nodes.TextNode textNode = (org.jsoup.nodes.TextNode) node;
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    textBuffer.append(text).append("\n");
                }
            }
        }
        
        // Flush remaining text
        if (textBuffer.length() > 0) {
            String text = textBuffer.toString().trim();
            if (!text.isEmpty()) {
                parts.add(new ContentPart(ContentPartType.TEXT, text));
            }
        }
        
        return parts;
    }
    
    /**
     * ประเภทของ content part
     */
    public enum ContentPartType {
        TEXT,
        TABLE
    }
    
    /**
     * ส่วนของ content (ข้อความหรือตาราง)
     */
    @Data
    public static class ContentPart {
        private final ContentPartType type;
        private final String content;
        
        public ContentPart(ContentPartType type, String content) {
            this.type = type;
            this.content = content;
        }
        
        public boolean isTable() {
            return type == ContentPartType.TABLE;
        }
        
        public boolean isText() {
            return type == ContentPartType.TEXT;
        }
    }
}
