package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

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
 * หมายเหตุ: หนังสือรับเข้าไม่ต้องสร้าง PDF หลัก
 * เพราะเป็นการรับหนังสือจากภายนอกเข้ามา
 * 
 * ใช้สำหรับกรณีพิเศษที่ต้องสร้าง PDF รอง
 */
@Slf4j
@Component
public class InboundPdfGenerator extends PdfGeneratorBase {
    
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
        log.info("Inbound document does not require main PDF generation");
        
        List<PdfResult> results = new ArrayList<>();
        
        // หนังสือรับเข้าไม่ต้องสร้าง PDF หลัก
        // ส่งกลับ empty result หรือ placeholder
        results.add(PdfResult.builder()
            .pdfBase64("")
            .type("Other")
            .description("หนังสือรับเข้า (ไม่ต้องสร้าง PDF หลัก)")
            .build());
        
        return results;
    }
    
    /**
     * ตรวจสอบว่าต้องสร้าง PDF หลักหรือไม่
     */
    public boolean requiresMainPdf() {
        return false;
    }
}
