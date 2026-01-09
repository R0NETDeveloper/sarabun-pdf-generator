package th.go.etda.sarabun.pdf.service.pdf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;

/**
 * Generator สำหรับ หนังสือภายใต้กระทรวง (Ministry)
 * 
 * BookNameId: 4B3EB169-6203-4A71-A3BD-A442FEAAA91F
 * 
 * โครงสร้างเอกสาร: คล้ายบันทึกข้อความ
 * - โลโก้ ETDA
 * - หัวข้อ "บันทึกข้อความ"
 * - ส่วนราชการ
 * - ที่ + วันที่
 * - เรื่อง
 * - เรียน
 * - เนื้อหา
 * - ลายเซ็น
 * 
 * ใช้ MemoPdfGenerator เพราะรูปแบบคล้ายกัน
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinistryPdfGenerator extends PdfGeneratorBase {
    
    private final MemoPdfGenerator memoPdfGenerator;
    
    @Override
    public BookType getBookType() {
        return BookType.MINISTRY;
    }
    
    @Override
    public String getGeneratorName() {
        return "MinistryPdfGenerator";
    }
    
    @Override
    public List<PdfResult> generate(GeneratePdfRequest request) throws Exception {
        log.info("=== {} generating PDF ===", getGeneratorName());
        log.info("Ministry document uses memo format");
        
        List<PdfResult> results = new ArrayList<>();
        
        // ใช้ MemoPdfGenerator เพราะรูปแบบคล้ายกัน
        String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
        results.add(createMainPdfResult(memoPdfBase64, "หนังสือภายใต้กระทรวง"));
        
        return results;
    }
}
