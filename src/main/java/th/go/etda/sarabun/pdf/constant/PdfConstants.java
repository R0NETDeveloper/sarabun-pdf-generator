package th.go.etda.sarabun.pdf.constant;

/**
 * PDF Constants - ค่าคงที่สำหรับการสร้าง PDF
 * 
 * รวมค่าคงที่ทั้งหมดไว้ที่เดียว เพื่อ:
 * - ง่ายต่อการหาและแก้ไข
 * - ป้องกันค่าซ้ำกันในหลายไฟล์
 * - แก้ที่เดียว ได้ผลทุกที่ (DRY principle)
 * 
 * ใช้งาน:
 * import static th.go.etda.sarabun.pdf.constant.PdfConstants.*;
 * 
 * @author Sarabun PDF Team
 */
public final class PdfConstants {
    
    // Prevent instantiation
    private PdfConstants() {
        throw new UnsupportedOperationException("Constants class - do not instantiate");
    }
    
    // ============================================
    // Base64 Prefix
    // ============================================
    public static final String BASE64_PDF_PREFIX = "data:application/pdf;base64,";
    
    // ============================================
    // Font Paths (relative to classpath)
    // ============================================
    public static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    public static final String FONT_BOLD_PATH = "fonts/THSarabunNew Bold.ttf";
    
    // ============================================
    // Page Settings (A4 in points)
    // 1 inch = 72 points
    // A4 = 210mm x 297mm = 595pt x 842pt
    // ============================================
    public static final float PAGE_WIDTH = 595f;
    public static final float PAGE_HEIGHT = 842f;
    
    // ============================================
    // Margins (in points)
    // ============================================
    public static final float MARGIN_TOP = 70f;
    public static final float MARGIN_BOTTOM = 70f;
    public static final float MARGIN_LEFT = 70f;
    public static final float MARGIN_RIGHT = 70f;
    
    // ============================================
    // Content Area (calculated from page & margins)
    // ============================================
    public static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;  // 455f
    public static final float CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM; // 702f
    
    // ============================================
    // Font Sizes (in points)
    // ============================================
    public static final float FONT_SIZE_HEADER = 24f;
    public static final float FONT_SIZE_FIELD = 18f;
    public static final float FONT_SIZE_FIELD_VALUE = 16f;
    public static final float FONT_SIZE_CONTENT = 16f;
    
    // ============================================
    // Spacing (in points)
    // ============================================
    public static final float SPACING_AFTER_HEADER = 30f;
    public static final float SPACING_BETWEEN_FIELDS = 5f;
    public static final float SPACING_BEFORE_CONTENT = 14f;
    public static final float SPACING_BEFORE_SIGNATURES = 40f;
    public static final float SPACING_BETWEEN_SIGNATURES = 20f;
    
    // ============================================
    // Logo Settings (in points)
    // ============================================
    public static final float LOGO_WIDTH = 120f;
    public static final float LOGO_HEIGHT = 40f;
    public static final float LOGO_SPACING = 30f;
    
    // ============================================
    // Multi-page Settings
    // ============================================
    /**
     * MIN_Y_OFFSET: ระยะห่างจาก MARGIN_BOTTOM ก่อนขึ้นหน้าใหม่
     * MIN_Y_POSITION = MARGIN_BOTTOM + MIN_Y_OFFSET
     */
    public static final float MIN_Y_OFFSET = 50f;
    
    /**
     * PAGE_NUMBER_Y_OFFSET: ระยะห่างของเลขหน้าจาก margin top
     */
    public static final float PAGE_NUMBER_Y_OFFSET = 40f;
    
    /**
     * SIGNATURE_RESERVE_HEIGHT: พื้นที่สำรองสำหรับช่องลงนาม
     */
    public static final float SIGNATURE_RESERVE_HEIGHT = 200f;
    
    // ============================================
    // Field Positions
    // ============================================
    /**
     * DATE_X_OFFSET: ระยะห่างจากขอบขวาสำหรับตำแหน่งวันที่
     * DATE_X_POSITION = PAGE_WIDTH - DATE_X_OFFSET
     */
    public static final float DATE_X_OFFSET = 320f;
    
    // ============================================
    // Debug Mode
    // ============================================
    /**
     * ENABLE_DEBUG_BORDERS: เปิด/ปิดการวาดกรอบแดงแสดงขอบเขตพื้นที่
     * - true: แสดงกรอบสีแดงรอบ content area (สำหรับ dev)
     * - false: ไม่แสดง (สำหรับ production)
     */
    public static final boolean ENABLE_DEBUG_BORDERS = true;
    
    // ============================================
    // Line Height Multipliers
    // ============================================
    public static final float LINE_HEIGHT_MULTIPLIER = 1.2f;
    public static final float PARAGRAPH_SPACING_MULTIPLIER = 1.5f;
    
    // ============================================
    // Common String Constants (ป้องกัน duplicate literals)
    // ============================================
    
    // PDF Types
    public static final String PDF_TYPE_OTHER = "Other";
    public static final String PDF_TYPE_MEMO = "Memo";
    public static final String PDF_TYPE_OUTBOUND = "Outbound";
    public static final String PDF_TYPE_STAMP = "Stamp";
    
    // Descriptions
    public static final String DESC_MEMO_COPY = "บันทึกข้อความ (สำเนาเก็บ)";
    
    // Filenames
    public static final String FILENAME_MEMO = "memo.pdf";
    
    // Labels (Thai)
    public static final String LABEL_REFER_TO = "อ้างถึง  ";
    public static final String LABEL_ATTACHMENT = "สิ่งที่ส่งมาด้วย  ";
    
    // Table HTML tag
    public static final String HTML_TAG_TABLE = "table";
    
    // Organization names
    public static final String ORG_ETDA = "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์";
}
