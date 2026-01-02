package th.go.etda.sarabun.pdf.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

/**
 * Utility สำหรับจัดการ HTML content จาก editor
 * 
 * ใช้สำหรับแปลง HTML ที่มาจาก WYSIWYG editor (TinyMCE, CKEditor, Quill)
 * เป็น plain text ที่เหมาะสำหรับใส่ใน PDF
 * 
 * Features:
 * - แปลง HTML tags เป็น plain text
 * - จัดการ line breaks (<br>, <p>)
 * - แปลง HTML entities (&nbsp;, &amp;, &lt;, &gt;)
 * - ลบ inline styles และ scripts
 * - รักษา indentation และ formatting
 */
public class HtmlUtils {
    
    /**
     * แปลง HTML เป็น plain text (แนะนำใช้สำหรับ PDF)
     * 
     * วิธีการ:
     * 1. แปลง <br> เป็น \n
     * 2. แปลง <p> เป็น paragraph (\n\n)
     * 3. ลบ HTML tags ทั้งหมด
     * 4. แปลง HTML entities เป็นตัวอักษรจริง
     * 5. Trim whitespace ส่วนเกิน
     * 
     * @param html HTML content จาก editor
     * @return Plain text สำหรับใส่ใน PDF
     */
    public static String htmlToPlainText(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        // Parse HTML
        Document doc = Jsoup.parse(html);
        
        // ลบ scripts, styles, และ comments
        doc.select("script, style, comment").remove();
        
        // แปลง HTML เป็น text พร้อมรักษา line breaks
        StringBuilder text = new StringBuilder();
        extractText(doc.body(), text);
        
        // Cleanup
        String result = text.toString()
            .replaceAll("&nbsp;", " ")           // แปลง &nbsp; เป็น space
            .replaceAll("\\s+\n", "\n")          // ลบ space ก่อน newline
            .replaceAll("\n{3,}", "\n\n")        // ลด newline มากเกิน 2 ให้เหลือ 2
            .replaceAll("\\[INDENT\\]", "    ")  // แปลง indent marker เป็น 4 spaces
            .trim();                              // ตัด whitespace หน้า-หลัง
        
        return result;
    }
    
    /**
     * Extract text จาก HTML node โดยรักษา formatting
     */
    private static void extractText(Element element, StringBuilder text) {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                // Text node - เพิ่ม text โดยตรง
                String nodeText = ((TextNode) node).text();
                if (!nodeText.trim().isEmpty()) {
                    text.append(nodeText);
                }
            } else if (node instanceof Element) {
                Element child = (Element) node;
                String tagName = child.tagName().toLowerCase();
                
                // จัดการ tags พิเศษ
                switch (tagName) {
                    case "br":
                        text.append("\n");
                        break;
                    case "p":
                    case "div":
                        // เพิ่ม newline ก่อน paragraph (ถ้ายังไม่มี)
                        if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                            text.append("\n");
                        }
                        // เพิ่ม indent marker สำหรับบรรทัดแรกของย่อหน้า (จะถูกแปลงเป็น 4 spaces ภายหลัง)
                        text.append("[INDENT]");
                        extractText(child, text);
                        text.append("\n");
                        break;
                    case "li":
                        text.append("• ");
                        extractText(child, text);
                        text.append("\n");
                        break;
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        // Heading - เพิ่ม newlines รอบๆ
                        if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                            text.append("\n");
                        }
                        extractText(child, text);
                        text.append("\n");
                        break;
                    default:
                        // Tags อื่นๆ - เพียงดึง text ออกมา
                        extractText(child, text);
                        break;
                }
            }
        }
    }
    
    /**
     * ลบ HTML tags ทั้งหมด (วิธีง่าย - ใช้ Jsoup Safelist)
     * 
     * @param html HTML content
     * @return Plain text (ไม่มี tags)
     */
    public static String stripHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.none())
            .replaceAll("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * ตรวจสอบว่า string เป็น HTML หรือไม่
     * 
     * @param text ข้อความที่ต้องการตรวจสอบ
     * @return true ถ้ามี HTML tags
     */
    public static boolean isHtml(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        // เช็คว่ามี HTML tags หรือไม่
        return text.matches("(?s).*<[a-zA-Z][^>]*>.*");
    }
    
    /**
     * ทดสอบ utility (สำหรับ debug)
     */
    public static void main(String[] args) {
        // ตัวอย่าง HTML จาก editor
        String html = """
            <p>ตามที่ สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์ (ETDA) ได้มีการพัฒนาระบบ <strong>Sarabun PDF API</strong></p>
            <p>เพื่อใช้ในการสร้างเอกสารราชการในรูปแบบ PDF ที่มีมาตรฐาน&nbsp;และสอดคล้องกับระเบียบสำนักนายกรัฐมนตรี นั้น</p>
            <br>
            <p>ในการนี้ ฝ่ายพัฒนาระบบได้ทำการปรับปรุงระบบดังกล่าวให้รองรับ Apache PDFBox แทน iText7</p>
            """;
        
        System.out.println("=== Original HTML ===");
        System.out.println(html);
        System.out.println("\n=== Converted to Plain Text ===");
        System.out.println(htmlToPlainText(html));
        System.out.println("\n=== Strip Tags Only ===");
        System.out.println(stripHtmlTags(html));
    }
}
