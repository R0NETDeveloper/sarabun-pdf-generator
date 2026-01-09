package th.go.etda.sarabun.pdf.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.constant.BookType;
import th.go.etda.sarabun.pdf.model.ApiResponse;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.service.pdf.PdfGeneratorBase;
import th.go.etda.sarabun.pdf.service.pdf.PdfGeneratorFactory;

/**
 * PDF Generation Service - ใช้ Factory Pattern
 * 
 * รองรับประเภทเอกสาร 9 ประเภท:
 * - Memo (บันทึกข้อความ) - BB4A2F11-722D-449A-BCC5-22208C7A4DEC
 * - Regulation (หนังสือระเบียบ) - 50792880-F85A-4343-9672-7B61AF828A5B
 * - Announcement (หนังสือประกาศ) - 23065068-BB18-49EA-8CE7-22945E16CB6D
 * - Order (หนังสือคำสั่ง) - 3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D
 * - Inbound (หนังสือรับเข้า) - 03241AA7-0E85-4C5C-A2CC-688212A79B84
 * - Outbound (หนังสือส่งออก) - 90F72F0E-528D-4992-907A-F2C6B37AD9A5
 * - Stamp (หนังสือประทับตรา) - AF3E7697-6F7E-4AD8-B76C-E2134DB98747
 * - Ministry (หนังสือภายใต้กระทรวง) - 4B3EB169-6203-4A71-A3BD-A442FEAAA91F
 * - Rule (หนังสือข้อบังคับ) - 4AB1EC00-9E5E-4113-B577-D8ED46BA7728
 * 
 * ข้อดีของ Factory Pattern:
 * 1. แยก concern ชัดเจน - แต่ละ Generator ดูแลเอกสารประเภทเดียว
 * 2. ง่ายต่อการ maintain - แก้ไข 1 ประเภทไม่กระทบอื่น
 * 3. ง่ายต่อการเพิ่มประเภทใหม่ - สร้าง Generator ใหม่แล้วลงทะเบียนใน Factory
 * 4. ทดสอบง่าย - Unit test แยกกันได้
 * 
 * @author Migrated from .NET to Java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePdfService {
    
    private final PdfService pdfService;
    private final PdfGeneratorFactory generatorFactory;
    
    // Constants จากโค้ดเดิม - BookNameId ที่ต้องจัดการพิเศษ
    private static final Set<String> SPECIAL_BOOK_NAME_IDS = Set.of(
        "90F72F0E-528D-4992-907A-F2C6B37AD9A5",
        "50792880-F85A-4343-9672-7B61AF828A5B",
        "23065068-BB18-49EA-8CE7-22945E16CB6D",
        "3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D",
        "4AB1EC00-9E5E-4113-B577-D8ED46BA7728",
        "4B3EB169-6203-4A71-A3BD-A442FEAAA91F",
        "AF3E7697-6F7E-4AD8-B76C-E2134DB98747",
        "03241AA7-0E85-4C5C-A2CC-688212A79B84",
        "C2905724-04D3-46AF-81EA-BF3045A59BF2",
        "11B56C3B-1C8E-4574-8B5D-72659BE74E6A",
        "8F6A1804-C340-49B4-9BFB-FE523E640AA1",
        "63E72391-0261-4C71-A8DE-F23CBB3033A9",
        "DD65959E-06BD-40C9-9793-211DB2084A65",
        "1AD2CD13-D938-4DD9-9407-9E922BA4652E",
        "969D9478-5A0E-42CF-8BBE-6B188C71588B",
        "0BF965C9-095B-4B73-BAB4-A0BDADA6993D"
    );
    
    /**
     * สร้าง PDF Preview - ใช้ Factory Pattern
     * 
     * ขั้นตอนการทำงาน:
     * 1. ดึง Generator ที่เหมาะสมจาก Factory
     * 2. สร้าง PDF array (หลัก + รอง)
     * 3. เพิ่มลายเซ็น (ถ้าต้องการ)
     * 4. รวม PDF ทั้งหมด
     * 5. ส่งกลับเป็น Base64
     */
    public ApiResponse<String> previewPdf(GeneratePdfRequest request) {
        try {
            String bookNameId = request.getBookNameId();
            BookType bookType = BookType.fromId(bookNameId);
            
            log.info("Starting PDF generation for BookNameId: {} ({})", bookNameId, bookType.getThaiName());
            
            // 1. ดึง Generator ที่เหมาะสมจาก Factory
            PdfGeneratorBase generator = generatorFactory.getGenerator(bookType);
            log.info("Using generator: {}", generator.getGeneratorName());
            
            // 2. สร้าง PDF array
            List<PdfResult> pdfArray;
            
            if (!bookType.requiresMainPdf()) {
                // กรณีหนังสือรับเข้า - ไม่ต้องสร้าง PDF หลัก
                pdfArray = new ArrayList<>();
                pdfArray.add(PdfResult.builder()
                    .pdfBase64("")
                    .type("Other")
                    .description("บันทึกข้อความรอง")
                    .build());
            } else {
                // ใช้ Generator สร้าง PDF
                pdfArray = generator.generate(request);
            }
            
            // 3. เพิ่มลายเซ็น (ถ้ามี)
            if (hasSignatureData(request)) {
                log.info("Adding signatures to PDF");
                pdfArray = addSignaturesToPdfs(pdfArray, request);
            }
            
            // 4. รวม PDF
            String finalPdfBase64 = mergePdfArray(pdfArray, request);
            
            log.info("PDF generation completed successfully using {}", generator.getGeneratorName());
            return ApiResponse.success(finalPdfBase64, "สร้าง PDF สำเร็จ (" + bookType.getThaiName() + ")");
            
        } catch (UnsupportedOperationException e) {
            log.error("Unsupported document type: ", e);
            return ApiResponse.error("ยังไม่รองรับประเภทเอกสารนี้: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            return ApiResponse.error("เกิดข้อผิดพลาดในการสร้าง PDF: " + e.getMessage());
        }
    }
    
    /**
     * ดึงรายการประเภทเอกสารที่รองรับ
     */
    public List<BookType> getSupportedBookTypes() {
        return generatorFactory.getSupportedBookTypes();
    }
    
    /**
     * ตรวจสอบว่า BookNameId นี้รองรับหรือไม่
     */
    public boolean isSupported(String bookNameId) {
        return generatorFactory.isSupported(bookNameId);
    }
    
    /**
     * ตรวจสอบว่ามีข้อมูลลายเซ็นหรือไม่
     */
    private boolean hasSignatureData(GeneratePdfRequest request) {
        return (request.getBookSigned() != null && !request.getBookSigned().isEmpty()) ||
               (request.getBookSubmited() != null && !request.getBookSubmited().isEmpty()) ||
               (request.getBookLearner() != null && !request.getBookLearner().isEmpty());
    }
        
    /**
     * เพิ่มลายเซ็นให้กับ PDFs
     * 
     * แปลงมาจาก: AddSignatureFieldsToPdf() method
     */
    private List<PdfResult> addSignaturesToPdfs(List<PdfResult> pdfArray, 
                                                GeneratePdfRequest request) {
        log.debug("Adding signatures to PDFs");
        
        List<PdfResult> results = new ArrayList<>();
        
        for (int i = 0; i < pdfArray.size(); i++) {
            PdfResult pdf = pdfArray.get(i);
            
            try {
                String signedPdf = addSignatureFieldsToPdf(
                    pdf.getPdfBase64(),
                    request,
                    pdf.getType(),
                    i
                );
                
                results.add(PdfResult.builder()
                    .pdfBase64(signedPdf)
                    .type(pdf.getType())
                    .description(pdf.getDescription())
                    .build());
                    
            } catch (Exception e) {
                log.warn("Failed to add signature to PDF {}: {}", i, e.getMessage());
                // ถ้าเพิ่มลายเซ็นไม่ได้ ให้ใช้ PDF เดิม
                results.add(pdf);
            }
        }
        
        return results;
    }
    
    /**
     * เพิ่มลายเซ็นให้กับ PDF
     */
    private String addSignatureFieldsToPdf(String pdfBase64, 
                                          GeneratePdfRequest request,
                                          String type,
                                          int index) throws Exception {
        // แปลง Base64 เป็น bytes
        byte[] pdfBytes = decodeBase64Pdf(pdfBase64);
        
        // สร้างไฟล์ชั่วคราว
        Path tempDir = Files.createTempDirectory("sarabun_pdf");
        Path inputFile = tempDir.resolve("input.pdf");
        Path outputFile = tempDir.resolve("output.pdf");
        
        try {
            // เขียนไฟล์
            Files.write(inputFile, pdfBytes);
            
            // เรียก PdfService เพื่อเพิ่มลายเซ็น
            pdfService.addSignatureFields(
                inputFile.toFile(),
                outputFile.toFile(),
                buildSignatureFields(request, type)
            );
            
            // อ่านผลลัพธ์
            byte[] resultBytes = Files.readAllBytes(outputFile);
            return  Base64.getEncoder().encodeToString(resultBytes);
            
        } finally {
            // ลบไฟล์ชั่วคราว
            deleteQuietly(inputFile);
            deleteQuietly(outputFile);
            deleteQuietly(tempDir);
        }
    }
    
    /**
     * สร้างรายการฟิลด์ลายเซ็น
     */
    private List<SignatureFieldInfo> buildSignatureFields(GeneratePdfRequest request, String type) {
        List<SignatureFieldInfo> fields = new ArrayList<>();
        
        boolean isSpecialCase = SPECIAL_BOOK_NAME_IDS.contains(request.getBookNameId()) && 
                               "Other".equals(type);
        
        if (isSpecialCase) {
            // กรณีพิเศษ: เพิ่มเฉพาะ Learner
            if (request.getBookLearner() != null) {
                for (var learner : request.getBookLearner()) {
                    fields.add(SignatureFieldInfo.builder()
                        .fieldName("Learner_" + learner.getEmail())
                        .type("เรียน")
                        .name(String.format("%s%s %s", 
                            learner.getPrefixName() != null ? learner.getPrefixName() : "",
                            learner.getFirstname(),
                            learner.getLastname()))
                        .email(learner.getEmail())
                        .position(learner.getPositionName())
                        .build());
                }
            }
        } else {
            // กรณีปกติ: เพิ่มทุก type
            addSignedFields(fields, request);
            addSubmitedFields(fields, request);
            addLearnerFields(fields, request);
        }
        
        return fields;
    }
    
    private void addSignedFields(List<SignatureFieldInfo> fields, GeneratePdfRequest request) {
        if (request.getBookSigned() != null) {
            for (var signed : request.getBookSigned()) {
                fields.add(SignatureFieldInfo.builder()
                    .fieldName("Sign_" + signed.getEmail())
                    .type("ลงนาม")
                    .name(String.format("%s%s %s", 
                        signed.getPrefixName() != null ? signed.getPrefixName() : "",
                        signed.getFirstname(),
                        signed.getLastname()))
                    .email(signed.getEmail())
                    .position(signed.getPositionName())
                    .build());
            }
        }
    }
    
    private void addSubmitedFields(List<SignatureFieldInfo> fields, GeneratePdfRequest request) {
        if (request.getBookSubmited() != null) {
            for (var submited : request.getBookSubmited()) {
                fields.add(SignatureFieldInfo.builder()
                    .fieldName("Submit_" + submited.getEmail())
                    .type("เสนอผ่าน")
                    .name(String.format("%s%s %s", 
                        submited.getPrefixName() != null ? submited.getPrefixName() : "",
                        submited.getFirstname(),
                        submited.getLastname()))
                    .email(submited.getEmail())
                    .position(submited.getPositionName())
                    .build());
            }
        }
    }
    
    private void addLearnerFields(List<SignatureFieldInfo> fields, GeneratePdfRequest request) {
        if (request.getBookLearner() != null) {
            for (var learner : request.getBookLearner()) {
                fields.add(SignatureFieldInfo.builder()
                    .fieldName("Learner_" + learner.getEmail())
                    .type("เรียน")
                    .name(String.format("%s%s %s", 
                        learner.getPrefixName() != null ? learner.getPrefixName() : "",
                        learner.getFirstname(),
                        learner.getLastname()))
                    .email(learner.getEmail())
                    .position(learner.getPositionName())
                    .build());
            }
        }
    }
    
    /**
     * รวม PDF array
     * 
     * แปลงมาจาก: MergeMultiplePdfFiles() method
     */
    private String mergePdfArray(List<PdfResult> pdfArray, GeneratePdfRequest request) throws Exception {
        if (pdfArray.isEmpty()) {
            throw new IllegalArgumentException("PDF array is empty");
        }
        
        String mergedBase64;
        
        if (pdfArray.size() == 1) {
            // มี PDF เดียว
            mergedBase64 = pdfArray.get(0).getPdfBase64();
        } else {
            // หา PDF หลัก (Main = หนังสือส่งออก หรือ บันทึกข้อความ)
            PdfResult mainPdf = pdfArray.stream()
                .filter(p -> "Main".equals(p.getType()))
                .findFirst()
                .orElse(null);
            
            // หา PDF บันทึกข้อความ (Memo = สำหรับ merge กับหนังสือส่งออก)
            PdfResult memoPdf = pdfArray.stream()
                .filter(p -> "Memo".equals(p.getType()))
                .findFirst()
                .orElse(null);
                
            List<PdfResult> otherPdfs = pdfArray.stream()
                .filter(p -> "Other".equals(p.getType()))
                .collect(Collectors.toList());
            
            // สร้างรายการ PDF ที่จะรวม (ตามลำดับ: Main -> Memo -> Other)
            List<String> pdfsToMerge = new ArrayList<>();
            
            if (mainPdf != null) {
                pdfsToMerge.add(cleanBase64Prefix(mainPdf.getPdfBase64()));
            }
            
            // เพิ่ม Memo (บันทึกข้อความ) ต่อจาก Main (หนังสือส่งออก)
            if (memoPdf != null) {
                pdfsToMerge.add(cleanBase64Prefix(memoPdf.getPdfBase64()));
                log.info("Adding Memo PDF to merge (บันทึกข้อความ + หนังสือส่งออก)");
            }
            
            for (PdfResult other : otherPdfs) {
                pdfsToMerge.add(cleanBase64Prefix(other.getPdfBase64()));
            }
            
            // รวม PDFs
            mergedBase64 = mergePdfFiles(pdfsToMerge);
        }
        
        // ===== เพิ่มหน้า "เสนอผ่าน" (ขึ้นหน้าใหม่) =====
        if (request.getBookSubmited() != null && !request.getBookSubmited().isEmpty()) {
            log.info("Adding Submit pages for {} submiters", request.getBookSubmited().size());
            
            List<PdfService.SignerInfo> submiters = request.getBookSubmited().stream()
                .map(s -> PdfService.SignerInfo.builder()
                    .prefixName(s.getPrefixName())
                    .firstname(s.getFirstname())
                    .lastname(s.getLastname())
                    .positionName(s.getPositionName())
                    .departmentName(s.getDepartmentName())
                    .email(s.getEmail())
                    .signatureBase64(s.getSignatureBase64())
                    .build())
                .collect(Collectors.toList());
            
            mergedBase64 = pdfService.addSubmitPages(mergedBase64, submiters);
        }
        
        // ===== เพิ่มหน้า "ผู้เรียน/รับทราบ" (ขึ้นหน้าใหม่) =====
        if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            log.info("Adding Learner pages for {} learners", request.getBookLearner().size());
            
            List<PdfService.SignerInfo> learners = request.getBookLearner().stream()
                .map(l -> PdfService.SignerInfo.builder()
                    .prefixName(l.getPrefixName())
                    .firstname(l.getFirstname())
                    .lastname(l.getLastname())
                    .positionName(l.getPositionName())
                    .departmentName(l.getDepartmentName())
                    .email(l.getEmail())
                    .signatureBase64(l.getSignatureBase64())
                    .build())
                .collect(Collectors.toList());
            
            // สร้าง signers สำหรับแสดง "เรียน ชื่อผู้ลงนาม" ที่ด้านบน
            List<PdfService.SignerInfo> signersForDisplay = null;
            if (request.getBookSigned() != null && !request.getBookSigned().isEmpty()) {
                signersForDisplay = request.getBookSigned().stream()
                    .map(s -> PdfService.SignerInfo.builder()
                        .prefixName(s.getPrefixName())
                        .firstname(s.getFirstname())
                        .lastname(s.getLastname())
                        .positionName(s.getPositionName())
                        .build())
                    .collect(Collectors.toList());
            }
            
            mergedBase64 = pdfService.addLearnerPages(mergedBase64, learners, signersForDisplay);
        }
        
        return mergedBase64;
    }
    
    /**
     * รวม PDF files โดยใช้ PDFBox 2.x
     * 
     * ใช้ PDFMergerUtility + MemoryUsageSetting ตาม migrateToJava.txt Section 8.3 และ 9.5
     */
    private String mergePdfFiles(List<String> base64Pdfs) throws Exception {
        if (base64Pdfs == null || base64Pdfs.isEmpty()) {
            throw new IllegalArgumentException("PDF list is empty");
        }
        
        if (base64Pdfs.size() == 1) {
            return base64Pdfs.get(0);
        }
        
        PDFMergerUtility merger = new PDFMergerUtility();
        
        for (String base64Pdf : base64Pdfs) {
            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
            merger.addSource(new ByteArrayInputStream(pdfBytes));
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outputStream);
        
        // ใช้ MemoryUsageSetting ตาม migrateToJava.txt Section 9.5
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        
        log.info("Merged {} PDFs successfully using PDFMergerUtility", base64Pdfs.size());
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    // Utility methods
    
    private byte[] decodeBase64Pdf(String base64) {
        String cleanBase64 = cleanBase64Prefix(base64);
        return Base64.getDecoder().decode(cleanBase64);
    }
    
    private String cleanBase64Prefix(String base64) {
        if (base64.startsWith("data:application/pdf;base64,")) {
            return base64.substring("data:application/pdf;base64,".length());
        }
        return base64;
    }
    
    private void deleteQuietly(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", path, e);
        }
    }
    
    /**
     * Inner class สำหรับเก็บข้อมูลฟิลด์ลายเซ็น
     */
    @lombok.Data
    @lombok.Builder
    public static class SignatureFieldInfo {
        private String fieldName;
        private String type;
        private String name;
        private String email;
        private String position;
    }
}
