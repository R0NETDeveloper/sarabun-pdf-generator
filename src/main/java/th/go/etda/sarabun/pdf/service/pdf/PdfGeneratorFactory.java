package th.go.etda.sarabun.pdf.service.pdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;

/**
 * Factory สำหรับเลือก PDF Generator ที่เหมาะสมตามประเภทเอกสาร
 * 
 * ใช้ Strategy Pattern เพื่อให้สามารถเพิ่ม Generator ใหม่ได้ง่าย
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfGeneratorFactory {
    
    private final MemoPdfGenerator memoPdfGenerator;
    private final OutboundPdfGenerator outboundPdfGenerator;
    private final RegulationPdfGenerator regulationPdfGenerator;
    private final AnnouncementPdfGenerator announcementPdfGenerator;
    private final OrderPdfGenerator orderPdfGenerator;
    private final InboundPdfGenerator inboundPdfGenerator;
    private final StampPdfGenerator stampPdfGenerator;
    private final MinistryPdfGenerator ministryPdfGenerator;
    private final RulePdfGenerator rulePdfGenerator;
    
    private Map<BookType, PdfGeneratorBase> generatorMap;
    
    @PostConstruct
    public void init() {
        generatorMap = new HashMap<>();
        
        // ลงทะเบียน generators
        generatorMap.put(BookType.MEMO, memoPdfGenerator);
        generatorMap.put(BookType.OUTBOUND, outboundPdfGenerator);
        generatorMap.put(BookType.REGULATION, regulationPdfGenerator);
        generatorMap.put(BookType.ANNOUNCEMENT, announcementPdfGenerator);
        generatorMap.put(BookType.ORDER, orderPdfGenerator);
        generatorMap.put(BookType.INBOUND, inboundPdfGenerator);
        generatorMap.put(BookType.STAMP, stampPdfGenerator);
        generatorMap.put(BookType.MINISTRY, ministryPdfGenerator);
        generatorMap.put(BookType.RULE, rulePdfGenerator);
        
        log.info("PdfGeneratorFactory initialized with {} generators", generatorMap.size());
        
        // Log registered generators
        generatorMap.forEach((type, generator) -> {
            log.debug("Registered: {} -> {}", type.getCode(), generator.getGeneratorName());
        });
    }
    
    /**
     * ดึง Generator ตาม BookType
     * 
     * @param bookType ประเภทเอกสาร
     * @return PdfGeneratorBase ที่เหมาะสม
     * @throws IllegalArgumentException ถ้าไม่พบ Generator
     */
    public PdfGeneratorBase getGenerator(BookType bookType) {
        if (bookType == null || bookType == BookType.UNKNOWN) {
            log.warn("Unknown book type, falling back to MemoPdfGenerator");
            return memoPdfGenerator;
        }
        
        PdfGeneratorBase generator = generatorMap.get(bookType);
        
        if (generator == null) {
            log.warn("No generator found for book type: {}, falling back to MemoPdfGenerator", bookType);
            return memoPdfGenerator;
        }
        
        log.debug("Selected generator: {} for book type: {}", generator.getGeneratorName(), bookType.getCode());
        return generator;
    }
    
    /**
     * ดึง Generator ตาม BookNameId (UUID)
     * 
     * @param bookNameId UUID ของประเภทเอกสาร
     * @return PdfGeneratorBase ที่เหมาะสม
     */
    public PdfGeneratorBase getGenerator(String bookNameId) {
        BookType bookType = BookType.fromId(bookNameId);
        return getGenerator(bookType);
    }
    
    /**
     * ดึง Generator ตาม code
     * 
     * @param code code ของประเภทเอกสาร (เช่น "memo", "outbound")
     * @return PdfGeneratorBase ที่เหมาะสม
     */
    public PdfGeneratorBase getGeneratorByCode(String code) {
        BookType bookType = BookType.fromCode(code);
        return getGenerator(bookType);
    }
    
    /**
     * ดึง Memo Generator โดยตรง (สำหรับกรณีที่ต้องการสร้างบันทึกข้อความพ่วง)
     */
    public MemoPdfGenerator getMemoGenerator() {
        return memoPdfGenerator;
    }
    
    /**
     * ดึงรายการ BookType ทั้งหมดที่รองรับ
     */
    public List<BookType> getSupportedBookTypes() {
        return List.copyOf(generatorMap.keySet());
    }
    
    /**
     * ตรวจสอบว่า BookType นี้รองรับหรือไม่
     */
    public boolean isSupported(BookType bookType) {
        return generatorMap.containsKey(bookType);
    }
    
    /**
     * ตรวจสอบว่า BookNameId นี้รองรับหรือไม่
     */
    public boolean isSupported(String bookNameId) {
        BookType bookType = BookType.fromId(bookNameId);
        return bookType != BookType.UNKNOWN && generatorMap.containsKey(bookType);
    }
}
