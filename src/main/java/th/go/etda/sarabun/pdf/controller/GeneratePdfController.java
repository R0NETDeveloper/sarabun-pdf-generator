package th.go.etda.sarabun.pdf.controller;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.config.RateLimitConfig;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.ApiResponse;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.service.GeneratePdfService;
import th.go.etda.sarabun.pdf.service.RequestValidator;

/**
 * PDF Generation REST API Controller
 * 
 * รองรับ 9 ประเภทเอกสาร (ใช้ Factory Pattern):
 * - Memo (บันทึกข้อความ)
 * - Regulation (หนังสือระเบียบ)
 * - Announcement (หนังสือประกาศ)
 * - Order (หนังสือคำสั่ง)
 * - Inbound (หนังสือรับเข้า)
 * - Outbound (หนังสือส่งออก)
 * - Stamp (หนังสือประทับตรา)
 * - Ministry (หนังสือภายใต้กระทรวง)
 * - Rule (หนังสือข้อบังคับ)
 * 
 * Endpoints:
 * - POST /api/pdf/preview - สร้าง PDF preview พร้อมลายเซ็น
 * - GET /api/pdf/health - Health check
 * - GET /api/pdf/book-types - รายการประเภทเอกสารที่รองรับ
 * 
 * @author Migrated from .NET to Java
 */
@Slf4j
@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GeneratePdfController {
    
    private final GeneratePdfService generatePdfService;
    private final RateLimitConfig rateLimitConfig;
    private final RequestValidator requestValidator;
    
    /**
     * สร้าง PDF Preview (V1 - เดิม)
     * 
     * แปลงมาจาก: PreviewPDF() endpoint
     * 
     * รับ request body เป็น JSON และสร้าง PDF พร้อมลายเซ็น (ถ้าระบุ)
     * ส่งกลับ PDF ในรูปแบบ Base64 string
     * 
     * @param request ข้อมูลสำหรับสร้าง PDF
     * @param httpRequest HTTP request สำหรับดึง client IP
     * @return ApiResponse ที่มี PDF Base64
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<String>> previewPdf(
            @RequestBody GeneratePdfRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        log.info("============ PDF PREVIEW REQUEST from {} ============", clientIp);
        
        try {
            // 1. ตรวจสอบ Rate Limit
            if (!rateLimitConfig.tryConsume(clientIp)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("คำขอมากเกินไป กรุณารอสักครู่แล้วลองใหม่ (Rate limit exceeded)"));
            }
            
            // 2. ตรวจสอบ Input Validation
            RequestValidator.ValidationResult validationResult = requestValidator.validate(request);
            if (!validationResult.isValid()) {
                log.warn("Validation failed: {}", validationResult.getErrorMessage());
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("ข้อมูลไม่ถูกต้อง: " + validationResult.getErrorMessage()));
            }
            
            // 3. สร้าง PDF
            ApiResponse<String> response = generatePdfService.previewPdf(request);
            
            if (response.getIsOk()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
            }
            
        } catch (Exception e) {
            log.error("Error in previewPdf endpoint: ", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("เกิดข้อผิดพลาดภายในระบบ: " + e.getMessage()));
        }
    }
    
    /**
     * ดึง Client IP จาก request (รองรับ proxy/load balancer)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For อาจมีหลาย IP คั่นด้วย comma, ใช้ตัวแรก
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Health check endpoint - ตรวจสอบสถานะ PDF service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthInfo>> health() {
        HealthInfo info = new HealthInfo();
        info.status = "PDF Service is running";
        info.fontCacheReady = th.go.etda.sarabun.pdf.service.pdf.FontManager.isInstanceAvailable();
        
        if (info.fontCacheReady) {
            var stats = th.go.etda.sarabun.pdf.service.pdf.FontManager.getInstance().getStats();
            info.fontCacheSize = stats.getTotalCacheSize();
            info.fontLoadCount = stats.getTotalLoadCount();
        }
        
        // เพิ่มข้อมูล Rate Limit
        info.rateLimitEnabled = rateLimitConfig.isEnabled();
        info.rateLimitStats = rateLimitConfig.getStats();
        
        return ResponseEntity.ok(ApiResponse.success(info, "สถานะปกติ"));
    }
    
    /**
     * ข้อมูล Health Check
     */
    @lombok.Data
    public static class HealthInfo {
        private String status;
        private boolean fontCacheReady;
        private int fontCacheSize;
        private long fontLoadCount;
        private boolean rateLimitEnabled;
        private RateLimitConfig.RateLimitStats rateLimitStats;
    }
    
    /**
     * ดึงรายการประเภทเอกสารที่รองรับ
     */
    @GetMapping("/book-types")
    public ResponseEntity<ApiResponse<java.util.List<BookTypeInfo>>> getBookTypes() {
        java.util.List<BookTypeInfo> types = new java.util.ArrayList<>();
        for (BookType type : BookType.values()) {
            if (type != BookType.UNKNOWN) {
                types.add(new BookTypeInfo(type.getId(), type.getCode(), type.getThaiName()));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(types, "รายการประเภทเอกสาร"));
    }
    
    /**
     * Inner class สำหรับข้อมูลประเภทเอกสาร
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BookTypeInfo {
        private String id;
        private String code;
        private String thaiName;
    }
    
    /**
     * 🚀 ดู PDF โดยตรงใน browser (ไม่ต้องคัดลอก Base64)
     * 
     * เปิดได้ที่: http://localhost:8888/api/pdf/view
     * ใช้สำหรับทดสอบรวดเร็ว - แก้โค้ดแล้ว refresh เลย!
     */
    @GetMapping("/view")
    public ResponseEntity<byte[]> viewPdf() {
        log.info("============ VIEW PDF REQUEST ============");
        
        try {
            // สร้าง request ตัวอย่างสำหรับทดสอบ
            GeneratePdfRequest request = createTestRequest();
            
            // สร้าง PDF
            ApiResponse<String> response = generatePdfService.previewPdf(request);
            
            if (response.getIsOk() && response.getData() != null) {
                // ลบ prefix "data:application/pdf;base64," ถ้ามี
                String base64Data = response.getData();
                if (base64Data.startsWith("data:application/pdf;base64,")) {
                    base64Data = base64Data.substring("data:application/pdf;base64,".length());
                }
                
                // แปลง Base64 เป็น byte array
                byte[] pdfBytes = Base64.getDecoder().decode(base64Data);
                
                // ตั้งค่า headers สำหรับแสดง PDF
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentLength(pdfBytes.length);
                headers.set("Content-Disposition", "inline; filename=test.pdf");
                
                log.info("PDF generated successfully, size: {} bytes", pdfBytes.length);
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } else {
                log.error("Failed to generate PDF: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (Exception e) {
            log.error("Error viewing PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * สร้างข้อมูลตัวอย่างสำหรับทดสอบ - NEW FORMAT
     */
    private GeneratePdfRequest createTestRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        
        // ========== BookNameId ==========
        request.setBookNameId("90F72F0E-528D-4992-907A-F2C6B37AD9A5"); // Outbound
        
        // ========== DocumentMain (บันทึกข้อความ) ==========
        GeneratePdfRequest.DocumentMain docMain = new GeneratePdfRequest.DocumentMain();
        docMain.setBookName("หนังสือบันทึกข้อความ");
        docMain.setBookTitle("ขอความอนุเคราะห์จัดส่งเอกสาร");
        docMain.setBookNo("สพธอ. 0102/2568");
        docMain.setDateThai("29 ธันวาคม 2568");
        docMain.setDepartment("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        docMain.setDivisionName("ฝ่ายพัฒนาระบบและเทคโนโลยีสารสนเทศ");
        docMain.setSpeedLayer("ด่วนที่สุด");
        docMain.setFormatPdf("A4");
        
        // Content for documentMain (New Format - Object ไม่ใช่ Array)
        GeneratePdfRequest.BookContent mainContent = new GeneratePdfRequest.BookContent();
        mainContent.setSubject("ขอเชิญเข้าร่วมประชุมสัมมนา");
        mainContent.setContent("        ด้วยสำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์จะจัดการประชุมสัมมนา ในหัวข้อ \"การพัฒนาระบบสารบรรณอิเล็กทรอนิกส์\" ในวันที่ 15 มกราคม 2569 ณ ห้องประชุมใหญ่ ชั้น 5\n\n        จึงเรียนมาเพื่อโปรดพิจารณาส่งผู้แทนเข้าร่วมประชุมสัมมนาดังกล่าวด้วย จะขอบคุณยิ่ง");
        mainContent.setContentType("text");
        docMain.setBookContent(mainContent);
        
        request.setDocumentMain(docMain);
        
        // ========== DocumentSub (หนังสือส่งออก) ==========
        GeneratePdfRequest.DocumentSub docSub = new GeneratePdfRequest.DocumentSub();
        docSub.setBookName("หนังสือส่งออก");
        docSub.setBookTitle("ขอเชิญเข้าร่วมประชุมสัมมนา กับ ETDA");
        docSub.setDateThai("29 ธันวาคม 2568");
        docSub.setDepartment("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        docSub.setDivisionName("ฝ่ายพัฒนาระบบและเทคโนโลยีสารสนเทศ");
        docSub.setAddress("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์\nศูนย์ราชการเฉลิมพระเกียรติฯ (อาคารบี)");
        docSub.setContact("โทร. 02-123-4567\nอีเมล info@etda.or.th");
        docSub.setSpeedLayer("ด่วนที่สุด");
        
        // Content for documentSub (New Format - Object ไม่ใช่ Array)
        GeneratePdfRequest.BookContent subContent = new GeneratePdfRequest.BookContent();
        subContent.setSubject("ขอเชิญเข้าร่วมประชุมสัมมนา กับ ETDA");
        subContent.setContent("        ด้วยสำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์จะจัดการประชุมสัมมนา ในหัวข้อ \"การพัฒนาระบบสารบรรณอิเล็กทรอนิกส์\" ในวันที่ 15 มกราคม 2569 ณ ห้องประชุมใหญ่ ชั้น 5\n\n        จึงเรียนมาเพื่อโปรดพิจารณาส่งผู้แทนเข้าร่วมประชุมสัมมนาดังกล่าวด้วย จะขอบคุณยิ่ง");
        subContent.setContentType("text");
        docSub.setBookContent(subContent);
        
        // Attachments for documentSub
        java.util.List<GeneratePdfRequest.DocumentAttachment> attachments = new java.util.ArrayList<>();
        GeneratePdfRequest.DocumentAttachment attach1 = new GeneratePdfRequest.DocumentAttachment();
        attach1.setName("กำหนดการประชุม");
        attach1.setRemark("จำนวน 1 ฉบับ");
        attachments.add(attach1);
        docSub.setAttachment(attachments);
        
        request.setDocumentSub(docSub);
        
        // ========== ผู้ลงนาม (bookSigned) ==========
        java.util.List<GeneratePdfRequest.BookRelate> signers = new java.util.ArrayList<>();
        GeneratePdfRequest.BookRelate signer = new GeneratePdfRequest.BookRelate();
        signer.setPrefixName("นาย");
        signer.setFirstname("สมชาย");
        signer.setLastname("ใจดี");
        signer.setPositionName("ผู้อำนวยการ");
        signer.setDepartmentName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        signer.setEmail("somchai@etda.or.th");
        signers.add(signer);
        request.setBookSigned(signers);
        
        // ========== ผู้รับภายใน (bookLearner) ==========
        java.util.List<GeneratePdfRequest.BookRelate> learners = new java.util.ArrayList<>();
        GeneratePdfRequest.BookRelate learner = new GeneratePdfRequest.BookRelate();
        learner.setPrefixName("นาย");
        learner.setFirstname("วิชัย");
        learner.setLastname("รับเรื่อง");
        learner.setPositionName("หัวหน้างานพัฒนา");
        learner.setDepartmentName("ฝ่ายพัฒนาระบบ");
        learners.add(learner);
        request.setBookLearner(learners);
        
        // ========== ผู้รับภายนอก (toRecipients) ==========
        java.util.List<GeneratePdfRequest.BookRecipient> recipients = new java.util.ArrayList<>();
        GeneratePdfRequest.BookRecipient recipient = new GeneratePdfRequest.BookRecipient();
        recipient.setGuid("R001");
        recipient.setRecipientNo("1");
        recipient.setMinistryName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipient.setDepartmentName("สำนักงานปลัดกระทรวง");
        recipient.setOrganizeName("กระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipient.setSalutation("เรียน");
        recipient.setSalutationContent("ท่านปลัดกระทรวงดิจิทัลเพื่อเศรษฐกิจและสังคม");
        recipient.setEndDoc("ขอแสดงความนับถือ");
        recipients.add(recipient);
        request.setToRecipients(recipients);
        
        return request;
    }
}