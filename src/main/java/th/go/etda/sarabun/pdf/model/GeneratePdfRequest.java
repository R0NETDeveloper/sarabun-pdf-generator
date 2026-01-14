package th.go.etda.sarabun.pdf.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Model สำหรับ PDF Generation (New Format)
 * 
 * โครงสร้างใหม่:
 * - bookNameId: GUID ประเภทเอกสาร
 * - documentMain: ข้อมูลเอกสารหลัก (บันทึกข้อความ)
 * - documentSub: ข้อมูลเอกสารรอง (หนังสือส่งออก)
 * - bookSigned, bookSubmited, bookLearner: ผู้เกี่ยวข้อง
 * - toRecipients: ผู้รับหนังสือภายนอก
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePdfRequest {
    
    // ============================================
    // Root Level Fields
    // ============================================
    private String bookNameId;              // รหัสชื่อหนังสือ (GUID สำคัญ!)
    
    // ============================================
    // เอกสารหลักและเอกสารรอง (New Format)
    // ============================================
    private DocumentMain documentMain;      // ข้อมูลเอกสารหลัก (บันทึกข้อความ)
    private DocumentSub documentSub;        // ข้อมูลเอกสารรอง (หนังสือส่งออก)
    
    // ============================================
    // ผู้เกี่ยวข้อง
    // ============================================
    private List<BookRelate> bookSigned;    // ผู้ลงนาม
    private List<BookRelate> bookSubmited;  // ผู้เสนอผ่าน  
    private List<BookRelate> bookLearner;   // ผู้รับภายใน (เรียน)
    
    // ============================================
    // ผู้รับหนังสือภายนอก (New Format)
    // ============================================
    private List<BookRecipient> toRecipients;   // ผู้รับหนังสือภายนอก (หน่วยงาน)
    
    // ============================================
    // Helper methods สำหรับดึงข้อมูลจาก documentMain
    // เพื่อ backward compatibility กับ generators อื่นๆ
    // ============================================
    
    public String getBookTitle() {
        return documentMain != null ? documentMain.getBookTitle() : null;
    }
    
    public String getBookNo() {
        return documentMain != null ? documentMain.getBookNo() : null;
    }
    
    public String getDateThai() {
        return documentMain != null ? documentMain.getDateThai() : null;
    }
    
    public String getDepartment() {
        return documentMain != null ? documentMain.getDepartment() : null;
    }
    
    public String getDivisionName() {
        return documentMain != null ? documentMain.getDivisionName() : null;
    }
    
    public String getAddress() {
        return documentMain != null ? documentMain.getAddress() : null;
    }
    
    public String getSpeedLayer() {
        return documentMain != null ? documentMain.getSpeedLayer() : null;
    }
    
    public String getSpeedLayerId() {
        return documentMain != null ? documentMain.getSpeedLayerId() : null;
    }
    
    public String getFormatPdf() {
        return documentMain != null ? documentMain.getFormatPdf() : null;
    }
    
    public BookContent getBookContent() {
        return documentMain != null ? documentMain.getBookContent() : null;
    }
    
    /**
     * ดึง bookContent จาก documentSub (สำหรับหนังสือส่งออก, ระเบียบ, คำสั่ง ฯลฯ)
     */
    public BookContent getSubBookContent() {
        return documentSub != null ? documentSub.getBookContent() : null;
    }
    
    // Alias: toRecipients เป็น bookRecipients
    public List<BookRecipient> getBookRecipients() {
        return toRecipients;
    }
    
    // Helper methods เพิ่มเติมสำหรับ backward compatibility
    public BookSubDetail getSubDetail() {
        // Map documentSub to BookSubDetail-like object
        return null; // TODO: implement if needed
    }
    
    public String getRecipients() {
        return null; // Not used in new format
    }
    
    public String getEndDoc() {
        return null; // Get from toRecipients instead
    }
    
    public String getContact() {
        return documentSub != null ? documentSub.getContact() : null;
    }
    
    public ContactInfo getContactInfo() {
        return null; // Not used in new format
    }
    
    // ============================================
    // Inner Classes
    // ============================================
    
    /**
     * DocumentMain Model - ข้อมูลเอกสารหลัก (บันทึกข้อความ)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMain {
        private String bookName;            // ชื่อประเภทหนังสือ (เช่น "หนังสือบันทึกข้อความ")
        private String bookTitle;           // ชื่อเรื่อง (สำหรับ backward compatibility)
        private String bookNo;              // เลขที่หนังสือ
        private String dateThai;            // วันที่ภาษาไทย
        private String department;          // ชื่อหน่วยงาน
        private String divisionName;        // ชื่อส่วนงาน
        private String address;             // ที่อยู่
        private String speedLayer;          // ชั้นความเร็ว
        private String speedLayerId;        // รหัสชั้นความเร็ว
        private String formatPdf;           // รูปแบบ PDF (A4, etc.)
        private BookContent bookContent;    // เนื้อหาหนังสือ (Object ไม่ใช่ Array)
    }
    
    /**
     * DocumentSub Model - ข้อมูลเอกสารรอง (หนังสือส่งออก)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSub {
        private String bookName;            // ชื่อประเภทหนังสือ (เช่น "หนังสือส่งออก")
        private String bookTitle;           // ชื่อเรื่อง (สำหรับ backward compatibility)
        private String bookNo;              // เลขที่หนังสือ
        private String dateThai;            // วันที่ภาษาไทย
        private String department;          // ชื่อหน่วยงาน
        private String divisionName;        // ชื่อส่วนงาน
        private String address;             // ที่อยู่
        private String contact;             // ข้อมูลติดต่อ
        private String speedLayer;          // ชั้นความเร็ว
        private String speedLayerId;        // รหัสชั้นความเร็ว
        private String year;                // ปี พ.ศ.
        private BookContent bookContent;            // เนื้อหาหนังสือ (Object ไม่ใช่ Array)
        private List<BookReferTo> bookReferTo;      // อ้างถึง
        private List<DocumentAttachment> attachment; // สิ่งที่ส่งมาด้วย
    }
    
    /**
     * BookContent Model - เนื้อหาหนังสือ (New Format - Object ไม่ใช่ Array)
     * 
     * โครงสร้าง:
     * - subject: หัวเรื่องเอกสาร (วาดหลัง "เรื่อง")
     * - content: เนื้อหากลาง (วาดหลัง "เรียน" หรือเนื้อความ)
     * - contentType: ประเภทเนื้อหา "text" (default) หรือ "html"
     * 
     * ตัวอย่าง:
     * {
     *   "subject": "ขออนุมัติจัดซื้ออุปกรณ์",
     *   "content": "<p>ด้วยกอง...</p><table>...</table>",
     *   "contentType": "html"
     * }
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookContent {
        @JsonProperty("subject")
        private String subject;             // หัวเรื่อง (วาดหลัง "เรื่อง")
        
        @JsonProperty("content")
        private String content;             // เนื้อหากลาง (HTML หรือ plain text)
        
        @JsonProperty("contentType")
        private String contentType;         // ประเภทเนื้อหา: "text" (default) หรือ "html"
        
        /**
         * ตรวจสอบว่าเนื้อหาเป็น HTML หรือไม่
         */
        public boolean isHtmlContent() {
            return "html".equalsIgnoreCase(contentType);
        }
        
        /**
         * ตรวจสอบว่ามี subject หรือไม่
         */
        public boolean hasSubject() {
            return subject != null && !subject.trim().isEmpty();
        }
        
        /**
         * ตรวจสอบว่ามี content หรือไม่
         */
        public boolean hasContent() {
            return content != null && !content.trim().isEmpty();
        }
    }
    
    /**
     * BookRelate Model - ผู้เกี่ยวข้อง (New Format)
     * ใช้สำหรับ bookLearner, bookSubmited, bookSigned
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRelate {
        private String relatedNo;           // ลำดับ
        private String prefixName;          // คำนำหน้า
        private String firstname;           // ชื่อ
        private String lastname;            // นามสกุล
        private String positionName;        // ชื่อตำแหน่ง
        private String departmentName;      // ชื่อหน่วยงาน
        private String email;               // อีเมล
        private String signatureBase64;     // ลายเซ็น Base64
    }
    
    /**
     * DocumentAttachment Model - สิ่งที่ส่งมาด้วย (New Format)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentAttachment {
        private String name;                // ชื่อไฟล์/รายการ
        private String remark;              // หมายเหตุ (เช่น "จำนวน 1 ฉบับ")
    }
    
    /**
     * BookReferTo Model - อ้างถึง (New Format)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookReferTo {
        private String bookReferToNo;       // เลขที่อ้างถึง
        private String bookReferToName;     // ชื่อหนังสืออ้างถึง
        private LocalDateTime createDate;   // วันที่
    }
    
    /**
     * BookRecipient Model - ผู้รับหนังสือภายนอก (New Format)
     * ใช้สำหรับ toRecipients
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRecipient {
        @JsonProperty("guid")
        private String guid;                // GuId
        private String recipientNo;         // ลำดับผู้รับ
        private String ministryName;        // ชื่อกระทรวง
        private String departmentName;      // ชื่อกรม
        private String organizeName;        // ชื่อองค์กร
        private String address;             // ที่อยู่
        private String postalCode;          // รหัสไปรษณีย์
        private String contactName;         // ชื่อผู้ติดต่อ
        private String contactEmail;        // อีเมล
        private String salutation;          // คำขึ้นต้น เช่น "เรียน", "กราบเรียน"
        private String salutationContent;   // เนื้อหาต่อจากคำขึ้นต้น (เช่น "ท่านปลัดกระทรวง...")
        private String endDoc;              // คำลงท้าย เช่น "ขอแสดงความนับถือ"
        
        // Helper method for backward compatibility
        public String getDivisionName() {
            return null; // Not used in new format
        }
    }
    
    /**
     * BookSubDetail Model - stub for backward compatibility
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSubDetail {
        private String docNo;               // เลขที่เอกสาร/ฉบับที่
        private List<SubDetailLearner> subDetailLearner;
    }
    
    /**
     * SubDetailLearner Model - stub for backward compatibility
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubDetailLearner {
        private String emailEnding;
        private String detail;
    }
    
    /**
     * ContactInfo Model - stub for backward compatibility
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
