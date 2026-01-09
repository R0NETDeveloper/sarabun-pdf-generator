package th.go.etda.sarabun.pdf.constant;

/**
 * ประเภทหนังสือราชการ
 * 
 * แปลงมาจาก: BookNameId constants ใน .NET
 */
public enum BookType {
    
    // หนังสือบันทึกข้อความ (Memo)
    MEMO("BB4A2F11-722D-449A-BCC5-22208C7A4DEC", "บันทึกข้อความ", "memo"),
    
    // หนังสือระเบียบ (Regulation)
    REGULATION("50792880-F85A-4343-9672-7B61AF828A5B", "หนังสือระเบียบ", "regulation"),
    
    // หนังสือประกาศ (Announcement)
    ANNOUNCEMENT("23065068-BB18-49EA-8CE7-22945E16CB6D", "หนังสือประกาศ", "announcement"),
    
    // หนังสือคำสั่ง (Order)
    ORDER("3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D", "หนังสือคำสั่ง", "order"),
    
    // หนังสือรับเข้า (Inbound)
    INBOUND("03241AA7-0E85-4C5C-A2CC-688212A79B84", "หนังสือรับเข้า", "inbound"),
    
    // หนังสือส่งออก (Outbound)
    OUTBOUND("90F72F0E-528D-4992-907A-F2C6B37AD9A5", "หนังสือส่งออก", "outbound"),
    
    // หนังสือประทับตรา (Stamp)
    STAMP("AF3E7697-6F7E-4AD8-B76C-E2134DB98747", "หนังสือประทับตรา", "stamp"),
    
    // หนังสือภายใต้กระทรวง (Ministry)
    MINISTRY("4B3EB169-6203-4A71-A3BD-A442FEAAA91F", "หนังสือภายใต้กระทรวง", "ministry"),
    
    // หนังสือข้อบังคับ (Rule)
    RULE("4AB1EC00-9E5E-4113-B577-D8ED46BA7728", "หนังสือข้อบังคับ", "rule"),
    
    // ประเภทที่ไม่รู้จัก (Unknown)
    UNKNOWN("", "ไม่ทราบประเภท", "unknown");
    
    private final String id;
    private final String thaiName;
    private final String code;
    
    BookType(String id, String thaiName, String code) {
        this.id = id;
        this.thaiName = thaiName;
        this.code = code;
    }
    
    public String getId() {
        return id;
    }
    
    public String getThaiName() {
        return thaiName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * ค้นหา BookType จาก BookNameId (UUID)
     * 
     * @param bookNameId UUID ของประเภทหนังสือ
     * @return BookType ที่ตรงกัน หรือ UNKNOWN ถ้าไม่พบ
     */
    public static BookType fromId(String bookNameId) {
        if (bookNameId == null || bookNameId.isEmpty()) {
            return UNKNOWN;
        }
        
        String normalizedId = bookNameId.toUpperCase().trim();
        
        for (BookType type : values()) {
            if (type.id.equalsIgnoreCase(normalizedId)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * ค้นหา BookType จาก code
     * 
     * @param code code ของประเภทหนังสือ (เช่น "memo", "outbound")
     * @return BookType ที่ตรงกัน หรือ UNKNOWN ถ้าไม่พบ
     */
    public static BookType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }
        
        for (BookType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * ตรวจสอบว่าเป็นหนังสือส่งออกหรือไม่
     */
    public boolean isOutbound() {
        return this == OUTBOUND;
    }
    
    /**
     * ตรวจสอบว่าเป็นหนังสือรับเข้าหรือไม่
     */
    public boolean isInbound() {
        return this == INBOUND;
    }
    
    /**
     * ตรวจสอบว่าเป็นบันทึกข้อความหรือไม่
     */
    public boolean isMemo() {
        return this == MEMO;
    }
    
    /**
     * ตรวจสอบว่าเป็นหนังสือที่ต้องสร้าง PDF หลักหรือไม่
     * หนังสือรับเข้า (INBOUND) ไม่ต้องสร้าง PDF หลัก
     */
    public boolean requiresMainPdf() {
        return this != INBOUND;
    }
    
    /**
     * ตรวจสอบว่าเป็นหนังสือที่ต้องสร้างบันทึกข้อความพ่วงหรือไม่
     * หนังสือส่งออก (OUTBOUND) ต้องสร้างบันทึกข้อความสำเนาเก็บด้วย
     */
    public boolean requiresMemoAttachment() {
        return this == OUTBOUND;
    }
}
