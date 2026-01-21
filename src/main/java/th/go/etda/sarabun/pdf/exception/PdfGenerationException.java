package th.go.etda.sarabun.pdf.exception;

/**
 * Custom exception สำหรับ PDF generation errors
 * 
 * ใช้แทน generic Exception เพื่อ:
 * - Error handling ที่ชัดเจนขึ้น
 * - Caller สามารถ catch เฉพาะ PDF errors ได้
 * - Debug ง่ายขึ้น - รู้ว่า error มาจาก PDF generation
 */
public class PdfGenerationException extends RuntimeException {
    
    public PdfGenerationException(String message) {
        super(message);
    }
    
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PdfGenerationException(Throwable cause) {
        super(cause);
    }
}
