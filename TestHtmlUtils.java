package th.go.etda.sarabun.pdf;

public class TestHtmlUtils {
    public static void main(String[] args) {
        String html = "<p>ระเบียบสำนักนายกรัฐมนตรี ว่าด้วยงานสารบรรณ (ฉบับที่ ๒) พ.ศ. ๒๕๔๘</p><p>โดยที่เป็นการสมควรแก้ไขเพิ่มเติม</p>";
        
        System.out.println("=== Original HTML ===");
        System.out.println(html);
        
        System.out.println("\n=== After htmlToPlainText ===");
        String result = th.go.etda.sarabun.pdf.util.HtmlUtils.htmlToPlainText(html);
        System.out.println(result);
        
        System.out.println("\n=== Show each character ===");
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == ' ') {
                System.out.print("[SPACE]");
            } else if (c == '\n') {
                System.out.println("[NEWLINE]");
            } else {
                System.out.print(c);
            }
        }
        System.out.println();
    }
}
