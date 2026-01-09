package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือระเบียบ (Regulation)
 * 
 * BookNameId: 50792880-F85A-4343-9672-7B61AF828A5B
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA
 * - หัวข้อ "ระเบียบ"
 * - ชื่อระเบียบ
 * - เนื้อหา (ข้อ 1, 2, 3...)
 * - ลายเซ็น
 * 
 * TODO: Implement ตามรูปแบบเอกสารจริง
 */
@Slf4j
@Component
public class RegulationPdfGenerator extends PdfGeneratorBase {
    
    @Override
    public BookType getBookType() {
        return BookType.REGULATION;
    }
    
    @Override
    public String getGeneratorName() {
        return "RegulationPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        log.warn("RegulationPdfGenerator not fully implemented yet, using default memo format");
        
        // TODO: Implement ตามรูปแบบเอกสารระเบียบจริง
        // ปัจจุบันใช้รูปแบบบันทึกข้อความก่อน
        
        List<PdfResult> results = new ArrayList<>();
        
        // Placeholder - ใช้รูปแบบ Memo ก่อน
        String pdfBase64 = generatePlaceholderPdf(request, "หนังสือระเบียบ");
        results.add(createMainPdfResult(pdfBase64, "หนังสือระเบียบ"));
        
        return results;
    }
    
    /**
     * สร้าง PDF แบบ placeholder (ใช้รูปแบบ Memo ก่อน)
     */
    private String generatePlaceholderPdf(GeneratePdfRequest request, String documentType) throws Exception {
        // TODO: Implement ตามรูปแบบเอกสารจริง
        throw new UnsupportedOperationException("RegulationPdfGenerator not fully implemented yet. DocumentType: " + documentType);
    }
}
