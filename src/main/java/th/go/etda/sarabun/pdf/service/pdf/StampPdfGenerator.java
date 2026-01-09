package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือประทับตรา (Stamp)
 * 
 * BookNameId: AF3E7697-6F7E-4AD8-B76C-E2134DB98747
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA
 * - ตราประทับ
 * - เลขที่
 * - เรื่อง
 * - เรียน
 * - เนื้อหา
 * - ประทับตราแทนลายเซ็น
 * 
 * TODO: Implement ตามรูปแบบเอกสารจริง
 */
@Slf4j
@Component
public class StampPdfGenerator extends PdfGeneratorBase {
    
    @Override
    public BookType getBookType() {
        return BookType.STAMP;
    }
    
    @Override
    public String getGeneratorName() {
        return "StampPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        log.warn("StampPdfGenerator not fully implemented yet");
        
        // TODO: Implement ตามรูปแบบเอกสารประทับตราจริง
        
        List<PdfResult> results = new ArrayList<>();
        
        throw new UnsupportedOperationException("StampPdfGenerator not fully implemented yet");
    }
}
