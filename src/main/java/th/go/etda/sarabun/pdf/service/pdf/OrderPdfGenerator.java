package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือคำสั่ง (Order)
 * 
 * BookNameId: 3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D
 * 
 * โครงสร้างเอกสาร:
 * - โลโก้ ETDA
 * - หัวข้อ "คำสั่ง"
 * - เลขที่คำสั่ง
 * - เรื่อง
 * - เนื้อหา
 * - สั่ง ณ วันที่
 * - ลายเซ็น
 * 
 * TODO: Implement ตามรูปแบบเอกสารจริง
 */
@Slf4j
@Component
public class OrderPdfGenerator extends PdfGeneratorBase {
    
    @Override
    public BookType getBookType() {
        return BookType.ORDER;
    }
    
    @Override
    public String getGeneratorName() {
        return "OrderPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        log.warn("OrderPdfGenerator not fully implemented yet");
        
        // TODO: Implement ตามรูปแบบเอกสารคำสั่งจริง
        
        List<PdfResult> results = new ArrayList<>();
        
        throw new UnsupportedOperationException("OrderPdfGenerator not fully implemented yet");
    }
}
