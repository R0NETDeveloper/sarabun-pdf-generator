package th.go.etda.sarabun.pdf.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;

/**
 * Request Validator - ตรวจสอบความถูกต้องของ Request ก่อนสร้าง PDF
 * 
 * ป้องกันปัญหา:
 * 1. Request ขนาดใหญ่เกินไป → OutOfMemoryError
 * 2. Base64 ไม่ถูกต้อง → IllegalArgumentException
 * 3. HTML injection → Security issue
 * 4. ข้อมูลไม่ครบ → NullPointerException
 * 
 * ตั้งค่าได้ใน application.properties:
 * - validation.max-content-length=500000
 * - validation.max-base64-size=10485760
 * - validation.max-signers=20
 */
@Slf4j
@Service
public class RequestValidator {
    
    // ============================================
    // Configuration Properties
    // ============================================
    
    @Value("${validation.max-content-length:500000}")
    private int maxContentLength;  // 500KB default
    
    @Value("${validation.max-base64-size:10485760}")
    private int maxBase64Size;  // 10MB default
    
    @Value("${validation.max-signers:20}")
    private int maxSigners;
    
    @Value("${validation.max-recipients:100}")
    private int maxRecipients;
    
    @Value("${validation.max-attachments:50}")
    private int maxAttachments;
    
    @Value("${validation.enabled:true}")
    private boolean enabled;
    
    // ============================================
    // Patterns for validation
    // ============================================
    
    // Pattern สำหรับตรวจสอบ GUID format
    private static final Pattern GUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // Pattern สำหรับตรวจจับ script tags (XSS prevention)
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // ============================================
    // Main Validation Method
    // ============================================
    
    /**
     * ตรวจสอบ Request ทั้งหมด
     * 
     * @param request ข้อมูล request
     * @return ValidationResult ที่มีผลการตรวจสอบ
     */
    public ValidationResult validate(GeneratePdfRequest request) {
        if (!enabled) {
            return ValidationResult.success();
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 1. ตรวจสอบ bookNameId (required)
        if (request.getBookNameId() == null || request.getBookNameId().isBlank()) {
            errors.add("bookNameId is required");
        } else if (!isValidGuid(request.getBookNameId())) {
            errors.add("bookNameId must be a valid GUID format");
        }
        
        // 2. ตรวจสอบ documentMain
        validateDocumentMain(request.getDocumentMain(), errors, warnings);
        
        // 3. ตรวจสอบ documentSub (ถ้ามี)
        if (request.getDocumentSub() != null) {
            validateDocumentSub(request.getDocumentSub(), errors, warnings);
        }
        
        // 4. ตรวจสอบผู้ลงนาม
        validateSigners(request.getBookSigned(), "bookSigned", errors, warnings);
        validateSigners(request.getBookSubmited(), "bookSubmited", errors, warnings);
        validateSigners(request.getBookLearner(), "bookLearner", errors, warnings);
        
        // 5. ตรวจสอบผู้รับ
        validateRecipients(request.getToRecipients(), errors, warnings);
        
        // 6. ตรวจสอบ signature Base64 (ถ้ามี)
        validateSignatureBase64(request, errors, warnings);
        
        if (errors.isEmpty()) {
            if (!warnings.isEmpty()) {
                log.warn("Request validation passed with warnings: {}", warnings);
            }
            return ValidationResult.successWithWarnings(warnings);
        } else {
            log.error("Request validation failed: {}", errors);
            return ValidationResult.failure(errors, warnings);
        }
    }
    
    // ============================================
    // Validation Methods - Document Main
    // ============================================
    
    private void validateDocumentMain(GeneratePdfRequest.DocumentMain docMain, 
                                     List<String> errors, List<String> warnings) {
        if (docMain == null) {
            warnings.add("documentMain is null - may cause issues for some document types");
            return;
        }
        
        // ตรวจสอบ bookContent
        if (docMain.getBookContent() != null) {
            validateBookContent(docMain.getBookContent(), "documentMain.bookContent", errors, warnings);
        }
    }
    
    // ============================================
    // Validation Methods - Document Sub
    // ============================================
    
    private void validateDocumentSub(GeneratePdfRequest.DocumentSub docSub, 
                                    List<String> errors, List<String> warnings) {
        // ตรวจสอบ bookContent
        if (docSub.getBookContent() != null) {
            validateBookContent(docSub.getBookContent(), "documentSub.bookContent", errors, warnings);
        }
        
        // ตรวจสอบ attachments
        if (docSub.getAttachment() != null) {
            if (docSub.getAttachment().size() > maxAttachments) {
                errors.add(String.format("Too many attachments: %d (max: %d)", 
                    docSub.getAttachment().size(), maxAttachments));
            }
        }
    }
    
    // ============================================
    // Validation Methods - Book Content
    // ============================================
    
    private void validateBookContent(GeneratePdfRequest.BookContent content, 
                                    String fieldName, List<String> errors, List<String> warnings) {
        if (content.getContent() != null) {
            String contentStr = content.getContent();
            
            // ตรวจสอบขนาด content
            if (contentStr.length() > maxContentLength) {
                errors.add(String.format("%s.content too large: %d chars (max: %d)", 
                    fieldName, contentStr.length(), maxContentLength));
            }
            
            // ตรวจจับ XSS (script injection)
            if (SCRIPT_PATTERN.matcher(contentStr).find()) {
                errors.add(String.format("%s.content contains potentially malicious script tags", fieldName));
            }
        }
    }
    
    // ============================================
    // Validation Methods - Signers
    // ============================================
    
    private void validateSigners(List<GeneratePdfRequest.BookRelate> signers, 
                                String fieldName, List<String> errors, List<String> warnings) {
        if (signers == null || signers.isEmpty()) {
            return;
        }
        
        if (signers.size() > maxSigners) {
            errors.add(String.format("Too many %s: %d (max: %d)", fieldName, signers.size(), maxSigners));
        }
        
        for (int i = 0; i < signers.size(); i++) {
            GeneratePdfRequest.BookRelate signer = signers.get(i);
            
            // ตรวจสอบ signature Base64 (ถ้ามี)
            if (signer.getSignatureBase64() != null && !signer.getSignatureBase64().isBlank()) {
                validateBase64Field(signer.getSignatureBase64(), 
                    String.format("%s[%d].signatureBase64", fieldName, i), errors, warnings);
            }
        }
    }
    
    // ============================================
    // Validation Methods - Recipients
    // ============================================
    
    private void validateRecipients(List<GeneratePdfRequest.BookRecipient> recipients, 
                                   List<String> errors, List<String> warnings) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        
        if (recipients.size() > maxRecipients) {
            errors.add(String.format("Too many recipients: %d (max: %d)", recipients.size(), maxRecipients));
        }
    }
    
    // ============================================
    // Validation Methods - Signature Base64
    // ============================================
    
    private void validateSignatureBase64(GeneratePdfRequest request, 
                                        List<String> errors, List<String> warnings) {
        // ตรวจสอบทุก list ของผู้เกี่ยวข้อง
        validateSignerListBase64(request.getBookSigned(), "bookSigned", errors, warnings);
        validateSignerListBase64(request.getBookSubmited(), "bookSubmited", errors, warnings);
        validateSignerListBase64(request.getBookLearner(), "bookLearner", errors, warnings);
    }
    
    private void validateSignerListBase64(List<GeneratePdfRequest.BookRelate> signers, 
                                         String fieldName, List<String> errors, List<String> warnings) {
        if (signers == null) return;
        
        for (int i = 0; i < signers.size(); i++) {
            GeneratePdfRequest.BookRelate signer = signers.get(i);
            if (signer.getSignatureBase64() != null && !signer.getSignatureBase64().isBlank()) {
                validateBase64Field(signer.getSignatureBase64(), 
                    String.format("%s[%d].signatureBase64", fieldName, i), errors, warnings);
            }
        }
    }
    
    // ============================================
    // Utility Methods
    // ============================================
    
    /**
     * ตรวจสอบว่าเป็น GUID format หรือไม่
     */
    private boolean isValidGuid(String value) {
        return GUID_PATTERN.matcher(value).matches();
    }
    
    /**
     * ตรวจสอบ Base64 string
     */
    private void validateBase64Field(String base64Value, String fieldName, 
                                    List<String> errors, List<String> warnings) {
        // ลบ data URI prefix ถ้ามี
        String cleanBase64 = base64Value;
        if (base64Value.contains(",")) {
            cleanBase64 = base64Value.substring(base64Value.indexOf(",") + 1);
        }
        
        // ตรวจสอบขนาด
        if (cleanBase64.length() > maxBase64Size) {
            errors.add(String.format("%s too large: %d bytes (max: %d)", 
                fieldName, cleanBase64.length(), maxBase64Size));
            return;
        }
        
        // ตรวจสอบ format Base64
        try {
            // ทดสอบ decode เฉพาะส่วนแรก (ประหยัด memory)
            String sample = cleanBase64.length() > 1000 ? cleanBase64.substring(0, 1000) : cleanBase64;
            // ปัดให้หาร 4 ลงตัว
            int paddedLength = (sample.length() / 4) * 4;
            if (paddedLength > 0) {
                Base64.getDecoder().decode(sample.substring(0, paddedLength));
            }
        } catch (IllegalArgumentException e) {
            errors.add(String.format("%s is not valid Base64 format", fieldName));
        }
    }
    
    /**
     * Sanitize HTML content - ลบ script tags และ event handlers
     */
    public String sanitizeHtml(String html) {
        if (html == null) return null;
        
        // ลบ script tags และ event handlers
        return SCRIPT_PATTERN.matcher(html).replaceAll("");
    }
    
    /**
     * ตรวจสอบว่า validation เปิดใช้งานหรือไม่
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    // ============================================
    // Inner Class - Validation Result
    // ============================================
    
    @lombok.Data
    @lombok.Builder
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public static ValidationResult success() {
            return ValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
        }
        
        public static ValidationResult successWithWarnings(List<String> warnings) {
            return ValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(warnings != null ? warnings : new ArrayList<>())
                .build();
        }
        
        public static ValidationResult failure(List<String> errors, List<String> warnings) {
            return ValidationResult.builder()
                .valid(false)
                .errors(errors != null ? errors : new ArrayList<>())
                .warnings(warnings != null ? warnings : new ArrayList<>())
                .build();
        }
        
        public String getErrorMessage() {
            if (errors == null || errors.isEmpty()) {
                return "";
            }
            return String.join(", ", errors);
        }
    }
}
