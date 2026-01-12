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
        
        List<String> allPdfsToMerge = new ArrayList<>();
        List<String> recipientDescriptions = new ArrayList<>();
        
        // 1. สร้าง PDF หนังสือภายใต้กระทรวง แยกตามผู้รับแต่ละหน่วยงาน (ใช้ bookRecipients)
        if (request.getBookRecipients() != null && !request.getBookRecipients().isEmpty()) {
            for (int i = 0; i < request.getBookRecipients().size(); i++) {
                GeneratePdfRequest.BookRecipient recipient = request.getBookRecipients().get(i);
                
                // สร้าง PDF สำหรับผู้รับแต่ละหน่วยงาน
                String ministryPdfBase64 = generateMinistryPdfForRecipient(request, recipient);
                allPdfsToMerge.add(ministryPdfBase64);
                
                String recipientName = buildRecipientName(recipient);
                recipientDescriptions.add("หนังสือภายใต้กระทรวง ถึง " + recipientName);
                
                log.info("Generated ministry PDF {} for: {}", i + 1, recipientName);
            }
            
            // 2. สร้าง PDF บันทึกข้อความ (สำเนาเก็บ) - แค่ 1 ฉบับ (เฉพาะเมื่อมี bookRecipients)
            String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
            allPdfsToMerge.add(memoPdfBase64);
            recipientDescriptions.add("บันทึกข้อความ (สำเนาเก็บ)");
        } else {
            // Fallback: ถ้าไม่มี bookRecipients ใช้ MemoPdfGenerator (แค่ 1 ฉบับ ไม่ต้องสำเนาเก็บ)
            String memoPdfBase64 = memoPdfGenerator.generateMemoPdf(request);
            allPdfsToMerge.add(memoPdfBase64);
            recipientDescriptions.add("หนังสือภายใต้กระทรวง");
        }
        
        log.info("Total PDFs to merge: {} ({} ministry + 1 memo)", 
                allPdfsToMerge.size(), allPdfsToMerge.size() - 1);
        
        // 3. รวมทุก PDF เป็น 1 ไฟล์
        String mergedPdfBase64 = mergePdfFiles(allPdfsToMerge);
        
        // สร้าง description สรุป
        String description = String.join(" + ", recipientDescriptions);
        
        log.info("Merged PDF created successfully: {}", description);
        
        // คืน PdfResult เดียว (รวมแล้ว)
        List<PdfResult> results = new ArrayList<>();
        results.add(createMainPdfResult(mergedPdfBase64, description));
        
        return results;
    }
    
    /**
     * สร้างชื่อผู้รับจาก BookRecipient
     */
    private String buildRecipientName(GeneratePdfRequest.BookRecipient recipient) {
        if (recipient.getOrganizeName() != null && !recipient.getOrganizeName().isEmpty()) {
            return recipient.getOrganizeName();
        }
        if (recipient.getDivisionName() != null && !recipient.getDivisionName().isEmpty()) {
            return recipient.getDivisionName();
        }
        if (recipient.getDepartmentName() != null && !recipient.getDepartmentName().isEmpty()) {
            return recipient.getDepartmentName();
        }
        if (recipient.getMinistryName() != null && !recipient.getMinistryName().isEmpty()) {
            return recipient.getMinistryName();
        }
        return "ผู้รับ";
    }
    
    /**
     * สร้าง PDF หนังสือภายใต้กระทรวงสำหรับผู้รับเฉพาะราย (หน่วยงานภายนอก)
     */
    private String generateMinistryPdfForRecipient(GeneratePdfRequest request, 
                                                    GeneratePdfRequest.BookRecipient recipient) throws Exception {
        // ใช้ MemoPdfGenerator แต่ override ค่าบางอย่าง
        // สำหรับตอนนี้ใช้ generateMemoPdf ก่อน (ในอนาคตอาจเพิ่ม generateMemoPdfWithOverride)
        log.info("Generating ministry for recipient: {}", recipient.getOrganizeName());
        return memoPdfGenerator.generateMemoPdf(request);
    }
}
