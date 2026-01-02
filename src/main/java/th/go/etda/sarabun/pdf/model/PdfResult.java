package th.go.etda.sarabun.pdf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF Result Model สำหรับเก็บผลลัพธ์ของ PDF แต่ละชนิด
 * 
 * แปลงมาจาก: GeneratePdfService.cs - PdfResult class
 * การเปลี่ยนแปลง:
 * - แปลงจาก internal class เป็น standalone model
 * - ใช้ Lombok เพื่อลด boilerplate code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfResult {
    
    /**
     * PDF ในรูปแบบ Base64 string
     * รูปแบบ: "data:application/pdf;base64,..." หรือ plain base64
     */
    private String pdfBase64;
    
    /**
     * ประเภทของ PDF
     * - "Main" = PDF หลัก
     * - "Other" = PDF รอง/บันทึกข้อความรอง
     */
    private String type;
    
    /**
     * คำอธิบายของ PDF นี้
     */
    private String description;
}
