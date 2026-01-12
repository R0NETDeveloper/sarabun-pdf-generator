package th.go.etda.sarabun.pdf.constant;

/**
 * ประเภทกรอบลายเซ็น (Sign Box Type)
 * 
 * ใช้สำหรับแสดงข้อความในกรอบลายเซ็น เพื่อระบุประเภทการลงนาม
 */
public final class SignBoxType {
    
    private SignBoxType() {
        // Prevent instantiation
    }
    
    /**
     * ลงนาม - สำหรับผู้ลงนาม (bookSigned)
     * ใช้กับ: บันทึกข้อความ, หนังสือประกาศ, หนังสือคำสั่ง, หนังสือระเบียบ, หนังสือข้อบังคับ
     */
    public static final String SIGN = "ลงนาม";
    
    /**
     * เสนอผ่าน - สำหรับผู้เสนอผ่าน (bookSubmited)
     * ใช้กับ: หน้าเสนอผ่าน
     */
    public static final String SUBMIT = "เสนอผ่าน";
    
    /**
     * เรียน - สำหรับผู้รับหนังสือ (bookLearner)
     * ใช้กับ: หนังสือส่งออก, หนังสือประทับตรา, หนังสือภายใต้กระทรวง
     */
    public static final String LEARNER = "เรียน";
}
