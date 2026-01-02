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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.model.ApiResponse;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.service.GeneratePdfService;

/**
 * PDF Generation REST API Controller
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Api/Controllers/GeneratePdfController.cs
 * 
 * การเปลี่ยนแปลง:
 * - ใช้ Spring MVC annotations (@RestController, @RequestMapping)
 * - ส่ง ResponseEntity สำหรับ HTTP status control
 * - ใช้ @CrossOrigin สำหรับ CORS (แทน [EnableCors] ใน .NET)
 * 
 * Endpoints:
 * - POST /api/pdf/preview - สร้าง PDF preview พร้อมลายเซ็น
 * - GET /api/pdf/health - Health check
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
    
    /**
     * สร้าง PDF Preview
     * 
     * แปลงมาจาก: PreviewPDF() endpoint
     * 
     * รับ request body เป็น JSON และสร้าง PDF พร้อมลายเซ็น (ถ้าระบุ)
     * ส่งกลับ PDF ในรูปแบบ Base64 string
     * 
     * @param request ข้อมูลสำหรับสร้าง PDF
     * @return ApiResponse ที่มี PDF Base64
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<String>> previewPdf(@RequestBody GeneratePdfRequest request) {
        log.info("============ RECEIVED REQUEST ============");
        log.info("Raw Request Object: {}", request);
        log.info("bookNameId: {}", request.getBookNameId());
        log.info("bookTitle: {}", request.getBookTitle());
        log.info("bookNo: {}", request.getBookNo());
        log.info("dateThai: {}", request.getDateThai());
        log.info("department: {}", request.getDepartment());
        log.info("divisionName: {}", request.getDivisionName());
        log.info("bookContent size: {}", request.getBookContent() != null ? request.getBookContent().size() : 0);
        if (request.getBookContent() != null && !request.getBookContent().isEmpty()) {
            for (int i = 0; i < request.getBookContent().size(); i++) {
                var content = request.getBookContent().get(i);
                log.info("  bookContent[{}].title: {}", i, content.getBookContentTitle());
                log.info("  bookContent[{}].content: {}", i, content.getBookContent());
            }
        }
        log.info("==========================================");
        
        try {
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
     * Health check endpoint - ตรวจสอบสถานะ PDF service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("PDF Service is running", "สถานะปกติ"));
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
     * สร้างข้อมูลตัวอย่างสำหรับทดสอบ - ครบทุกเคส
     * 
     * ทดสอบ:
     * ✅ ข้อมูลพื้นฐาน (หัวหนังสือ, เลขที่, วันที่)
     * ✅ HTML content แปลงเป็น plain text
     * ✅ Multi-line recipients (เรียนหลายคน)
     * ✅ ผู้ลงนาม (bookSigned) - แสดงกรอบลายเซ็น
     * ✅ ผู้เกี่ยวข้อง (bookLearner, bookSubmited, bookReview)
     * ✅ SubDetail - ข้อมูลเพิ่มเติม
     */
    private GeneratePdfRequest createTestRequest() {
        GeneratePdfRequest request = new GeneratePdfRequest();
        
        // ========== ข้อมูลพื้นฐาน ==========
        request.setBookNameId("1");
        request.setBookTitle("ขอความอนุเคราะห์จัดส่งเอกสาร PDF สำหรับทดสอบการจัดทำหนังสือราชการด้วย Sarabun PDF API ด้วยการขึ้นบรรทัดใหม่อัตโนมัติ");
        request.setBookNo("ดศ (สพธอ) ๕๑๑.๐๕/ ๑๒๓๔");
        request.setDateThai("29 ธันวาคม 2568");
        request.setTimeThai("14:30 น.");
        request.setDepartment("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        request.setDivisionName("ฝ่ายพัฒนาระบบและเทคโนโลยีสารสนเทศ");
        request.setYear("2568");
        request.setSpeedLayer("ด่วนที่สุด");
        request.setSecretLayerId("ปกติ");
        
        // ========== ผู้รับหนังสือ (เรียน) - Multi-line ==========
        request.setRecipients(
            "ผู้อำนวยการสำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์\n"
        );
        
        // ========== เนื้อหาเอกสาร - ทดสอบ HTML & Plain Text + MULTI-PAGE ==========
        java.util.List<GeneratePdfRequest.BookContent> contentList = new java.util.ArrayList<>();
        
        // Content 1: HTML Format (จาก Rich Text Editor) - เนื้อหาระเบียบสำนักนายกรัฐมนตรี
        var content1 = new GeneratePdfRequest.BookContent();
        content1.setBookContentTitle("");
        content1.setBookContent(
            "<p>     ระเบียบสำนักนายกรัฐมนตรี ว่าด้วยงานสารบรรณ (ฉบับที่ ๒) พ.ศ. ๒๕๔๘ โดยที่เป็นการ<br>" +
            "สมควรแก้ไขเพิ่มเติมระเบียบสำนักนายกรัฐมนตรี ว่าด้วยงานสารบรรณ พ.ศ.๒๕๒๖ เพื่อให้เหมาะสมกับ<br>" +
            "สภาวการณ์ในปัจจุบันที่มีการปฏิบัติงานสารบรรณด้วยระบบสารบรรณอิเล็กทรอนิกส์ และเป็นการ<br>" +
            "สอดคล้องกับการบริการราชการแนวทางใหม่ที่มุ่งเน้นผลสัมฤทธิ์ ความคุ้มค่า และการลดขั้นตอนการปฏิบัติ<br>" +
            "งาน สมควรวางระบบงานสารบรรณให้เป็นการดำเนินงานที่มีระบบ มีความรวดเร็ว มีประสิทธิภาพ และลด<br>" +
            "ความซํ้าซ้อนในการปฏิบัติราชการ</p>" +
            "<p>    โดยที่เป็นการสมควรแก้ไขเพิ่มเติมระเบียบสำนักนายกรัฐมนตรีว่าด้วยงานสารบรรณ พ.ศ.<br>" +
            "๒๕๖๒ เพื่อระบุตำแหน่ง ประเภทตำแหน่ง และระดับตำแหน่งของข้าราชการพลเรือน และพนักงานส่วนท้อง<br>" +
            "ถิ่นให้สอดคล้องกับตำแหน่ง ประเภทตำแหน่ง และระดับตำแหน่งของข้าราชการพลเรือน หรือพนักงานส่วน<br>" +
            "ท้องถิ่นนั้นรวมทั้งกำหนดให้พนักงานราชการ และเจ้าหน้าที่ของรัฐอื่น มีหน้าที่ทำสำเนาหนังสือ และรับรอง<br>" +
            "สำเนาหนังสือนั้นได้ด้วย อาศัยตามความในมาตรา ๑๑</p>"
        );
        contentList.add(content1);
        
        // Content 2: Plain Text Format (ไม่มี HTML tags)
        var content2 = new GeneratePdfRequest.BookContent();
        content2.setBookContentTitle("หมายเหตุ");
        content2.setBookContent(
            "สามารถติดต่อสอบถามเพิ่มเติมได้ที่ฝ่ายพัฒนาระบบและเทคโนโลยีสารสนเทศ " +
            "หมายเลขโทรศัพท์ 02-123-4567 หรือ Email: info@etda.or.th"
        );
        contentList.add(content2);
        
        request.setBookContent(contentList);
        
        // ========== ผู้ลงนาม (bookSigned) - แสดงขวาล่าง ==========
        // รูปแบบ:
        //   จึงเรียนมาเพื่อทราบและพิจารณา
        //   (ลายเซ็น)
        //   (ชื่อผู้ลงนาม)
        //   (ตำแหน่งผู้ลงนาม)
        java.util.List<GeneratePdfRequest.BookRelate> signers = new java.util.ArrayList<>();
        
        GeneratePdfRequest.BookRelate signer = new GeneratePdfRequest.BookRelate();
        signer.setPrefixName("นาย");
        signer.setFirstname("สมชาย");
        signer.setLastname("ใจดี");
        signer.setPositionName("ผู้อำนวยการฝ่ายพัฒนาระบบและเทคโนโลยีสารสนเทศ");
        signer.setDepartmentName("สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์");
        signer.setEmail("somchai.j@etda.or.th");
        signers.add(signer);
        
        request.setBookSigned(signers);
        
        // ========== SubDetail - ข้อมูลเพิ่มเติม ==========
        GeneratePdfRequest.BookSubDetail subDetail = new GeneratePdfRequest.BookSubDetail();
        java.util.List<GeneratePdfRequest.BookSubDetail.SubDetailLearner> subDetailLearners = 
            new java.util.ArrayList<>();
        
        GeneratePdfRequest.BookSubDetail.SubDetailLearner subLearner = 
            new GeneratePdfRequest.BookSubDetail.SubDetailLearner();
        subLearner.setEmailEnding("@etda.or.th");
        subLearner.setDetail("สำหรับผู้ทราบเพิ่มเติม");
        subDetailLearners.add(subLearner);
        
        subDetail.setSubDetailLearner(subDetailLearners);
        request.setSubDetail(subDetail);
        
        // ========== หมายเหตุ ==========
        // ระบบจะแปลง HTML เป็น plain text อัตโนมัติ:
        // - <p>, <div> → แบ่งย่อหน้า
        // - <br> → ขึ้นบรรทัดใหม่
        // - <ul>, <li> → รายการ
        // - <strong>, <em> → ลบ tags (ใช้ text ธรรมดา)
        // - &nbsp;, &amp;, &lt;, &gt; → แปลงเป็นตัวอักษรจริง
        
        return request;
    }
}