package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือรับเข้า (Inbound)
 * 
 * BookNameId: 03241AA7-0E85-4C5C-A2CC-688212A79B84
 * 
 * โครงสร้างเอกสาร:
 * 1. PDF ตั้งต้น (base64Pdf) - เอกสารจากภายนอกที่รับเข้ามา [บังคับต้องมี]
 * 2. หน้าผู้เรียน (bookLearner) - เพิ่มต่อท้าย PDF ตั้งต้น
 * 
 * หมายเหตุ:
 * - ไม่สร้างเนื้อหาจาก documentMain
 * - ไม่มีผู้ลงนาม (bookSigned)
 * - ไม่มีผู้เสนอ (bookSubmited)  
 * - เลขหน้าต่อเนื่องจาก PDF ตั้งต้น
 */
@Slf4j
@Component
public class InboundPdfGenerator extends PdfGeneratorBase {
    
    public InboundPdfGenerator() {
        // No dependencies needed
    }
    
    @Override
    public BookType getBookType() {
        return BookType.INBOUND;
    }
    
    @Override
    public String getGeneratorName() {
        return "InboundPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        
        List<PdfResult> results = new ArrayList<>();
        
        // สร้าง PDF หนังสือรับเข้า (base64Pdf + หน้าผู้เรียน)
        String inboundPdfBase64 = generateInboundPdf(request);
        results.add(createMainPdfResult(inboundPdfBase64, "หนังสือรับเข้า"));
        
        return results;
    }
    
    /**
     * สร้าง PDF หนังสือรับเข้า
     * 
     * Flow:
     * 1. รับ PDF ตั้งต้นจาก base64Pdf (บังคับ)
     * 2. เพิ่มหน้าผู้เรียน (bookLearner) ต่อท้าย
     * 3. Return PDF รวม
     */
    public String generateInboundPdf(GeneratePdfRequest request) throws Exception {
        String base64Pdf = request.getBase64Pdf();
        
        // ====== 1. ตรวจสอบและโหลด PDF ตั้งต้น (บังคับ) ======
        if (base64Pdf == null || base64Pdf.trim().isEmpty()) {
            throw new IllegalArgumentException("หนังสือรับเข้าต้องมี base64Pdf เป็นเอกสารตั้งต้น");
        }
        
        // ลบ prefix ถ้ามี (data:application/pdf;base64,...)
        String cleanBase64 = base64Pdf.trim();
        if (cleanBase64.contains(",")) {
            cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
        }
        
        // ตรวจสอบ length พื้นฐาน
        if (cleanBase64.length() < 100) {
            throw new IllegalArgumentException("base64Pdf ไม่ถูกต้อง (ขนาดน้อยเกินไป)");
        }
        
        byte[] basePdfBytes;
        int basePdfPageCount;
        
        try {
            basePdfBytes = Base64.getDecoder().decode(cleanBase64);
            
            // นับจำนวนหน้า PDF ตั้งต้น
            try (PDDocument basePdf = PDDocument.load(basePdfBytes)) {
                basePdfPageCount = basePdf.getNumberOfPages();
                log.info("Base PDF loaded successfully: {} pages, {} bytes", 
                        basePdfPageCount, basePdfBytes.length);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("base64Pdf ไม่ถูกต้อง: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("ไม่สามารถอ่าน PDF ตั้งต้นได้: " + e.getMessage());
        }
        
        // ====== 2. เพิ่มหน้าผู้เรียน (ถ้ามี) ======
        String resultBase64 = Base64.getEncoder().encodeToString(basePdfBytes);
        
        if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            log.info("Adding {} learner pages to inbound document", request.getBookLearner().size());
            
            // แปลง bookLearner เป็น SignerInfo list
            List<SignerInfo> learners = request.getBookLearner().stream()
                .map(l -> SignerInfo.builder()
                    .prefixName(l.getPrefixName())
                    .firstname(l.getFirstname())
                    .lastname(l.getLastname())
                    .positionName(l.getPositionName())
                    .departmentName(l.getDepartmentName())
                    .email(l.getEmail())
                    .build())
                .collect(Collectors.toList());
            
            // ใช้ bookNo จาก documentMain (ถ้ามี)
            String bookNo = "";
            if (request.getDocumentMain() != null && request.getDocumentMain().getBookNo() != null) {
                bookNo = request.getDocumentMain().getBookNo();
            }
            
            // เพิ่มหน้าผู้เรียน (ใช้ method จาก PdfGeneratorBase)
            // Note: signers = null เพราะหนังสือรับเข้าไม่มีผู้ลงนาม
            resultBase64 = addLearnerPages(resultBase64, learners, null, bookNo);
            
            log.info("Learner pages added successfully");
        } else {
            log.info("No bookLearner data, returning base PDF only");
        }
        
        return resultBase64;
    }
}
