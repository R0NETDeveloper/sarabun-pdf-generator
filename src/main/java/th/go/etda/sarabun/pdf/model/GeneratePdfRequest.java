package th.go.etda.sarabun.pdf.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Model สำหรับ PDF Generation
 * 
 * แปลงมาจาก: 
 * - ETDA.SarabunMultitenant.Model/GeneratePdfModel.cs (GenPdfReq)
 * - ETDA.SarabunMultitenant.Model/BookModel.cs (CreateBookReqModel)
 * 
 * GenPdfReq extends CreateBookReqModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePdfRequest {
    
    // ============================================
    // จาก CreateBookReqModel (Base)
    // ============================================
    private String actionCode;              // รหัส Action
    private String processId;               // รหัส Process
    private String bookTypeId;              // รหัสประเภทหนังสือ
    private String bookNameId;              // รหัสชื่อหนังสือ (GUID สำคัญ!)
    private String speedLayerId;            // รหัสชั้นความเร็ว
    private String secretLayerId;           // รหัสชั้นความลับ
    private String numReleaseChannelId;     // รหัสช่องทางออกเลข
    private String ministyId;               // รหัสกระทรวง
    private String departmentId;            // รหัสหน่วยงาน
    private String divisionId;              // รหัสส่วนงาน
    private String organizeId;              // รหัสองค์กร
    private String bookTitle;               // ชื่อเรื่อง
    private String docTypeId;               // รหัสประเภทเอกสาร
    private Boolean isMultiBookName;        // มีหลายชื่อหนังสือ
    private String divisionRemark;          // หมายเหตุส่วนงาน
    private LocalDateTime createDate;       // วันที่สร้าง
    
    // === รายการข้อมูลย่อย ===
    private List<BookContent> bookContent;          // เนื้อหาหนังสือ
    private List<DocumentAttachment> attachment;   // ไฟล์แนบ
    private List<BookReferTo> bookReferTo;          // อ้างถึง
    private BookSettingRelate settingRelate;        // การตั้งค่าความสัมพันธ์
    private List<BookRelate> bookLearner;           // ผู้รับภายใน (เรียน) - ใช้กับบันทึกข้อความ
    private List<BookRecipient> bookRecipients;     // ผู้รับภายนอก (หน่วยงาน) - ใช้กับหนังสือส่งออก
    private List<BookRelate> bookSubmited;          // ผู้เสนอผ่าน
    private List<BookRelate> bookReview;            // ผู้ตรวจสอบ
    private List<BookRelate> bookSigned;            // ผู้ลงนาม
    
    private String address;                 // ที่อยู่
    private String contact;                 // ข้อมูลติดต่อ (string/HTML) - backward compatible
    private ContactInfo contactInfo;        // ข้อมูลติดต่อ (object) - ใหม่ ⭐
    private Boolean isShowSpeed;            // แสดงชั้นความเร็ว
    private String formatPdf;               // รูปแบบ PDF (A4, etc.)
    
    // ============================================
    // จาก GenPdfReq (Extended)
    // ============================================
    private String guid;                    // GUID ของเอกสาร
    private String statusId;                // รหัสสถานะ
    private BookSubDetail subDetail;        // ข้อมูลเอกสารรอง
    private String divisionName;            // ชื่อหน่วยงาน
    private String numRelease;              // เลขที่ออก
    private String speedLayer;              // ชั้นความเร็ว (หลัก)
    private String speedLayerOther;         // ชั้นความเร็ว (รอง)
    private Boolean isShow;                 // แสดงลายเซ็นหรือไม่
    private String pdfBase64;               // PDF เดิม (Base64)
    private String type;                    // ประเภท ("Main", "Other")
    private String year;                    // ปี พ.ศ.
    private String endDoc;                  // ข้อความท้ายเอกสาร (เช่น "ขอแสดงความนับถือ", "ควรมิควรแล้วแต่จะโปรดเกล้าฯ")
    private String salutation;              // คำขึ้นต้น (เช่น "ขอประทานกราบทูล", "ขอเรียน")
    private String salutationEnding;        // คำลงท้ายขึ้นต้น (เช่น "เรียนถึงคนนั้น", "ทราบ")
    private String dateThai;                // วันที่ภาษาไทย
    private String timeThai;                // เวลาภาษาไทย
    private String bookNo;                  // เลขที่หนังสือ
    private String department;              // ชื่อหน่วยงาน
    private String recipients;              // ผู้รับหนังสือ (เรียน) - สำหรับ backward compatibility
    
    // ============================================
    // Inner Classes
    // ============================================
    
    /**
     * BookContent Model
     * แปลงมาจาก: BookModel.BookContentModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookContent {
        @JsonProperty("guid")
        private String guid;                // GUID
        private String bookDetailId;        // รหัสรายละเอียด
        private String bookNameId;          // รหัสชื่อหนังสือ
        private String bookNameName;        // ชื่อหนังสือ
        private String bookContentNo;       // ลำดับเนื้อหา
        private String bookContentTitle;    // หัวข้อเนื้อหา
        private String bookContent;         // เนื้อหา (HTML)
        private Boolean nextPageStatus;     // ขึ้นหน้าใหม่
    }
    
    /**
     * BookRelate Model
     * แปลงมาจาก: BookModel.BookRelateReqModel
     * ใช้สำหรับ Learner, Submited, Review, Signed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRelate {
        private String relatedId;           // รหัสความสัมพันธ์
        private String settingRelateId;     // รหัสการตั้งค่า
        private String relatedType;         // ประเภทความสัมพันธ์
        private String relatedOptionOrder;  // ลำดับ
        private String departmentId;        // รหัสหน่วยงาน
        private String organizeId;          // รหัสองค์กร
        private String relatedNo;           // เลขที่
        private String relateOrganizeId;    // รหัสองค์กรที่เกี่ยวข้อง
        private String positionId;          // รหัสตำแหน่ง
        private String relatePositionId;    // รหัสตำแหน่งที่เกี่ยวข้อง
        private String personalId;          // รหัสบุคคล
        private String personalOption;      // ตัวเลือกบุคคล
        private String positionName;        // ชื่อตำแหน่ง ⭐
        private String relatePersonalId;    // รหัสบุคคลที่เกี่ยวข้อง
        private String bookDetailId;        // รหัสรายละเอียดหนังสือ
        private String bookRelateId;        // รหัสความสัมพันธ์หนังสือ
        private Boolean signStatus;         // สถานะการลงนาม
        private LocalDateTime signDate;     // วันที่ลงนาม
        private String bookRelateDetailId;  // รหัสรายละเอียด
        private String email;               // อีเมล ⭐
        private String prefixName;          // คำนำหน้า ⭐
        private String firstname;           // ชื่อ ⭐
        private String lastname;            // นามสกุล ⭐
        private String departmentName;      // ชื่อหน่วยงาน ⭐
        private String organizeName;        // ชื่อองค์กร
        private String signatureBase64;     // ลายเซ็น Base64 (เพิ่มเติม)
    }
    
    /**
     * BookSubDetail Model
     * แปลงมาจาก: BookModel.BookSubDetailModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSubDetail {
        @JsonProperty("guid")
        private String guid;                // GUID
        private String bookNo;              // เลขที่หนังสือ
        private String bookTypeId;          // รหัสประเภท
        private String speedLayerId;        // รหัสชั้นความเร็ว
        private String speedLayerName;      // ชื่อชั้นความเร็ว
        private String secretLayerId;       // รหัสชั้นความลับ
        private String secretLayerName;     // ชื่อชั้นความลับ
        private String numReleaseChannelId; // รหัสช่องทาง
        private String numReleaseChannelName; // ชื่อช่องทาง
        private String ministyId;           // รหัสกระทรวง
        private String ministyName;         // ชื่อกระทรวง
        private String departmentId;        // รหัสหน่วยงาน
        private String departmentName;      // ชื่อหน่วยงาน
        private String divisionId;          // รหัสส่วนงาน
        private String divisionName;        // ชื่อส่วนงาน
        private String organizeId;          // รหัสองค์กร
        private String organizeName;        // ชื่อองค์กร
        private String bookTitle;           // ชื่อเรื่อง
        private String docTypeId;           // รหัสประเภทเอกสาร
        private String docTypeName;         // ชื่อประเภทเอกสาร
        private Boolean isMultiBookName;    // มีหลายชื่อ
        private String divisionRemark;      // หมายเหตุ
        private String creatorId;           // ผู้สร้าง
        private LocalDateTime createDate;   // วันที่สร้าง
        
        private List<BookContent> bookContent;          // เนื้อหา
        private List<DocumentAttachment> attachment;   // ไฟล์แนบ
        private List<BookReferTo> bookReferTo;          // อ้างถึง
        private List<SubDetailLearner> subDetailLearner; // ผู้รับ (รอง)
        
        private String address;             // ที่อยู่
        private String contact;             // ข้อมูลติดต่อ
        private String docNo;               // ลำดับที่
    }
    
    /**
     * SubDetailLearner Model
     * แปลงมาจาก: BookModel.BookSubRelateModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubDetailLearner {
        @JsonProperty("guid")
        private String guid;                // GUID
        private String bookSubDetailId;     // รหัสเอกสารรอง
        private String relatedNo;           // ลำดับ
        private String ministryId;          // รหัสกระทรวง
        private String ministryName;        // ชื่อกระทรวง
        private String departmentId;        // รหัสหน่วยงาน
        private String departmentName;      // ชื่อหน่วยงาน ⭐
        private String emailTo;             // อีเมลถึง
        private String emailCC;             // อีเมล CC
        private String emailBCC;            // อีเมล BCC
        private String emailSubject;        // หัวเรื่องอีเมล
        private String beginSubject;        // ข้อความเปิด
        private String emailEnding;         // ข้อความปิด ⭐
        private String detail;              // รายละเอียดเพิ่มเติม
    }
    
    /**
     * DocumentAttachment Model
     * แปลงมาจาก: DocumentModel.DocumentModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentAttachment {
        private String id;                  // รหัส
        private String url;                 // URL ไฟล์
        private String urlId;               // รหัส URL
        private String name;                // ชื่อไฟล์
        private String documentType;        // ประเภทเอกสาร
        private String documentTypeName;    // ชื่อประเภทเอกสาร
        @Builder.Default
        private Boolean isAdd = false;      // เพิ่มใหม่
        @Builder.Default
        private Boolean isDelete = false;   // ลบ
        private String refId;               // รหัสอ้างอิง
        private String remark;              // หมายเหตุ ⭐ (สำหรับสิ่งที่ส่งมาด้วย)
    }
    
    /**
     * BookReferTo Model
     * แปลงมาจาก: BookModel.BookReferToModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookReferTo {
        @JsonProperty("guid")
        private String guid;                // GUID
        private String bookDetailId;        // รหัสรายละเอียด
        private String bookReferToNo;       // เลขที่อ้างถึง
        private String bookReferToName;     // ชื่อหนังสืออ้างถึง
        private LocalDateTime createDate;   // วันที่
    }
    
    /**
     * BookRecipient Model - ผู้รับหนังสือภายนอก (สำหรับ Outbound)
     * ใช้สำหรับหนังสือส่งออกที่ส่งไปยังหน่วยงานภายนอก
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRecipient {
        @JsonProperty("guid")
        private String guid;                // GuId
        private String recipientNo;         // ลำดับผู้รับ
        private String ministryId;          // รหัสกระทรวง
        private String ministryName;        // ชื่อกระทรวง
        private String departmentId;        // รหัสกรม
        private String departmentName;      // ชื่อกรม
        private String divisionId;          // รหัสหน่วยงาน
        private String divisionName;        // ชื่อหน่วยงาน
        private String organizeName;        // ชื่อองค์กร (สำหรับแสดงใน "เรียน") ⭐
        private String address;             // ที่อยู่
        private String postalCode;          // รหัสไปรษณีย์
        private String contactName;         // ชื่อผู้ติดต่อ
        private String contactPhone;        // เบอร์โทรศัพท์
        private String contactEmail;        // อีเมล
        private String deliveryMethod;      // วิธีการจัดส่ง
        private String remark;              // หมายเหตุ
        
        // === คำขึ้นต้น/คำลงท้าย (เฉพาะผู้รับรายนี้) ===
        private String salutation;          // คำขึ้นต้น เช่น "เรียน", "กราบเรียน" ⭐
        private String salutationContent;   // เนื้อหาต่อจากคำขึ้นต้น ⭐
        private String endDoc;              // คำลงท้าย เช่น "ขอแสดงความนับถือ" ⭐
    }

    /**
     * ContactInfo Model - ข้อมูลติดต่อ
     * 
     * รองรับ 2 แบบ:
     * 1. ส่งเป็น object โดยตรง (contactInfo)
     * 2. ส่งเป็น string/HTML (contact) แล้ว parse
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        private String department;      // ชื่อหน่วยงาน
        private String phone;           // เบอร์โทรศัพท์
        private String fax;             // โทรสาร
        private String email;           // อีเมล
        
        /**
         * ตรวจสอบว่ามีข้อมูลติดต่ออย่างน้อย 1 อย่างหรือไม่
         */
        public boolean hasAnyInfo() {
            return (department != null && !department.isEmpty()) ||
                   (phone != null && !phone.isEmpty()) ||
                   (fax != null && !fax.isEmpty()) ||
                   (email != null && !email.isEmpty());
        }
    }

    /**
     * BookSettingRelate Model
     * แปลงมาจาก: BookModel.BookSettingRelateModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSettingRelate {
        @JsonProperty("guid")
        private String guid;                // GUID
        private String name;                // ชื่อ
        private String creatorId;           // ผู้สร้าง
        private LocalDateTime createDate;   // วันที่สร้าง
        private LocalDateTime updateDate;   // วันที่แก้ไข
        private Boolean oneTimeStatus;      // ใช้ครั้งเดียว
        private Boolean deleteStatus;       // สถานะลบ
    }
}
