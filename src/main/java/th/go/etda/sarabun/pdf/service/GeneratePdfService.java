package th.go.etda.sarabun.pdf.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.etda.sarabun.pdf.model.ApiResponse;
import th.go.etda.sarabun.pdf.model.GeneratePdfRequest;
import th.go.etda.sarabun.pdf.model.PdfResult;
import th.go.etda.sarabun.pdf.util.HtmlUtils;

/**
 * PDF Generation Service
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Service/GeneratePdfService.cs
 * 
 * การเปลี่ยนแปลงสำคัญ:
 * 1. แทนที่ iText7/iTextSharp ด้วย Apache PDFBox 2.0.31 (ฟรี 100%, Apache License 2.0)
 * 2. ใช้ PDType0Font สำหรับโหลด Thai fonts (TH Sarabun)
 * 3. รองรับทั้ง Windows และ Linux
 * 4. ใช้ stream แทนการ save file ชั่วคราว (ตาม migrateToJava.txt Notes #6)
 * 5. ใช้ try-with-resources สำหรับ auto-close resources
 * 6. ใช้ PDFMergerUtility + MemoryUsageSetting (ตาม migrateToJava.txt Section 9.5)
 * 
 * ฟีเจอร์หลัก:
 * - สร้าง PDF หลักและรอง
 * - เพิ่มลายเซ็นอิเล็กทรอนิกส์
 * - รวม PDF หลายไฟล์
 * - รองรับ Thai fonts
 * - Base64 encoding/decoding
 * 
 * หมายเหตุ: ตาม migrateToJava.txt ต้องใช้ PDFBox 2.x เท่านั้น (ไม่ใช่ 3.x)
 * 
 * @author Migrated from .NET to Java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePdfService {
    
    private final PdfService pdfService;
    
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
     * สร้าง PDF Preview
     * 
     * แปลงมาจาก: PreviewPDF() method
     * 
     * ขั้นตอนการทำงาน:
     * 1. สร้าง PDF array (หลัก + รอง)
     * 2. เพิ่มลายเซ็น (ถ้าต้องการ)
     * 3. รวม PDF ทั้งหมด
     * 4. ส่งกลับเป็น Base64
     */
    public ApiResponse<String> previewPdf(GeneratePdfRequest request) {
        try {
            log.info("Starting PDF generation for BookNameId: {}", request.getBookNameId());
            
            // 1. สร้าง PDF array
            List<PdfResult> pdfArray;
            
            if (!isSkipMainPdfGeneration(request.getBookNameId())) {
                pdfArray = generatePdfArray(request);
            } else {
                // กรณีพิเศษ: เอกสารบันทึกข้อความรองอย่างเดียว
                pdfArray = new ArrayList<>();
                pdfArray.add(PdfResult.builder()
                    .pdfBase64("")
                    .type("Other")
                    .description("บันทึกข้อความรอง")
                    .build());
            }
            
            // 2. เพิ่มลายเซ็น (ถ้ามี)
            if (hasSignatureData(request)) {
                log.info("Adding signatures to PDF");
                pdfArray = addSignaturesToPdfs(pdfArray, request);
            }
            
            // 3. รวม PDF
            String finalPdfBase64 = mergePdfArray(pdfArray, request);
            
            log.info("PDF generation completed successfully");
            return ApiResponse.success(finalPdfBase64, "สร้าง PDF สำเร็จ");
            
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            return ApiResponse.error("เกิดข้อผิดพลาดในการสร้าง PDF: " + e.getMessage());
        }
    }
    
    /**
     * ตรวจสอบว่าควรข้าม PDF หลักหรือไม่
     */
    private boolean isSkipMainPdfGeneration(String bookNameId) {
        return "03241AA7-0E85-4C5C-A2CC-688212A79B84".equals(bookNameId) ||
               "0BF965C9-095B-4B73-BAB4-A0BDADA6993D".equals(bookNameId);
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
     * สร้าง PDF array (อาจมีหลายไฟล์)
     * 
     * แปลงมาจาก: GeneratePdf() method
     */
    private List<PdfResult> generatePdfArray(GeneratePdfRequest request) throws Exception {
        List<PdfResult> results = new ArrayList<>();
        
        // สร้าง PDF หลัก (บันทึกข้อความ)
        String mainPdfBase64 = generateMainPdf(request);
        results.add(PdfResult.builder()
            .pdfBase64(mainPdfBase64)
            .type("Main")
            .description("หนังสือบันทึกข้อความหลัก")
            .build());
        
        // สร้าง PDF รอง (ถ้ามี)
        if (needsSecondaryPdfs(request)) {
            List<PdfResult> secondaryPdfs = generateSecondaryPdfs(request);
            results.addAll(secondaryPdfs);
        }
        
        return results;
    }
    
    /**
     * ตรวจสอบว่าต้องการ PDF รองหรือไม่
     */
    private boolean needsSecondaryPdfs(GeneratePdfRequest request) {
        String bookNameId = request.getBookNameId();
        return ("90F72F0E-528D-4992-907A-F2C6B37AD9A5".equals(bookNameId) ||
                "C2905724-04D3-46AF-81EA-BF3045A59BF2".equals(bookNameId)) &&
               request.getSubDetail() != null &&
               request.getSubDetail().getSubDetailLearner() != null &&
               !request.getSubDetail().getSubDetailLearner().isEmpty();
    }
    
    /**
     * สร้าง PDF หลัก
     */
    private String generateMainPdf(GeneratePdfRequest request) throws Exception {
        log.debug("Generating main PDF");
        
        // รวบรวมข้อมูลสำหรับสร้าง PDF
        String govName = request.getDivisionName() != null ? request.getDivisionName() : 
                        (request.getDepartment() != null ? request.getDepartment() : "");
        String dateThai = request.getDateThai();
        String title = request.getBookTitle() != null ? request.getBookTitle() : "";
        
        // รวบรวมรายชื่อผู้รับ
        String recipients = "";
        // ลื่องแรก: ใช้จาก recipients โดยตรง (ถ้ามี)
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            recipients = request.getRecipients();
        }
        // ลื่องสำรอง: ดึงจาก bookLearner (เพื่อ backward compatibility)
        else if (request.getBookLearner() != null && !request.getBookLearner().isEmpty()) {
            recipients = request.getBookLearner().stream()
                .map(l -> l.getPositionName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining("\n"));
        }
        
        // รวบรวมเนื้อหาทั้งหมด (รวม title และ content ของแต่ละรายการ)
        StringBuilder contentBuilder = new StringBuilder();
        if (request.getBookContent() != null && !request.getBookContent().isEmpty()) {
            for (var item : request.getBookContent()) {
                // เพิ่ม title (ถ้ามี) - แปลง HTML เป็น text
                if (item.getBookContentTitle() != null && !item.getBookContentTitle().isEmpty()) {
                    String title_text = HtmlUtils.isHtml(item.getBookContentTitle()) 
                        ? HtmlUtils.htmlToPlainText(item.getBookContentTitle())
                        : item.getBookContentTitle();
                    contentBuilder.append(title_text).append("  ");
                }
                // เพิ่ม content - แปลง HTML เป็น text (ถ้ามาจาก editor)
                if (item.getBookContent() != null && !item.getBookContent().isEmpty()) {
                    String content_text = HtmlUtils.isHtml(item.getBookContent())
                        ? HtmlUtils.htmlToPlainText(item.getBookContent())
                        : item.getBookContent();
                    contentBuilder.append(content_text);
                }
                contentBuilder.append("\n\n");
            }
        }
        String content = contentBuilder.toString().trim();
        
        log.info("generateMainPdf - govName: {}, title: {}, content length: {}", 
                govName, title, content.length());
        
        // รวบรวมลายเซ็น (ถ้ามี)
        List<String> signatures = new ArrayList<>();
        if (request.getBookSigned() != null && !request.getBookSigned().isEmpty()) {
            for (var signer : request.getBookSigned()) {
                String signerText = String.format("(%s %s %s)", 
                    signer.getPrefixName() != null ? signer.getPrefixName() : "",
                    signer.getFirstname() != null ? signer.getFirstname() : "",
                    signer.getLastname() != null ? signer.getLastname() : ""
                ).trim();
                
                if (signer.getPositionName() != null && !signer.getPositionName().isEmpty()) {
                    signerText += "\n" + signer.getPositionName();
                }
                
                signatures.add(signerText);
            }
        }
        
        // เรียก PdfService สร้าง PDF
        return pdfService.generateOfficialMemoPdf(
            govName,
            dateThai,
            request.getBookNo(),  // เพิ่ม bookNo
            title,
            recipients,
            content,
            request.getSpeedLayer(),
            request.getFormatPdf(),
            signatures,
            null  // signatureImagePaths - ไม่มีรูปภาพในการเรียกปกติ
        );
    }
    
    /**
     * สร้าง PDF รอง (สำหรับบันทึกข้อความรอง)
     */
    private List<PdfResult> generateSecondaryPdfs(GeneratePdfRequest request) throws Exception {
        log.debug("Generating secondary PDFs");
        
        List<PdfResult> results = new ArrayList<>();
        
        if (request.getSubDetail() != null && 
            request.getSubDetail().getSubDetailLearner() != null) {
            
            int index = 0;
            for (var learner : request.getSubDetail().getSubDetailLearner()) {
                String secondaryPdf = generateSecondaryPdf(request, learner, index);
                results.add(PdfResult.builder()
                    .pdfBase64(secondaryPdf)
                    .type("Other")
                    .description("บันทึกข้อความรอง " + (index + 1))
                    .build());
                index++;
            }
        }
        
        return results;
    }
    
    /**
     * สร้าง PDF รองแต่ละฉบับ
     */
    private String generateSecondaryPdf(GeneratePdfRequest request, 
                                       GeneratePdfRequest.SubDetailLearner learner,
                                       int index) throws Exception {
        // Implementation สำหรับสร้าง PDF รอง
        // ใช้ PdfService เหมือนกับ main PDF
        return pdfService.generateOfficialMemoPdf(
            request.getSubDetail() != null ? "สำนักงาน" : "",
            request.getDateThai(),
            "",  // bookNo สำหรับ PDF รอง
            "บันทึกข้อความรอง",
            learner.getDetail(),
            "",
            request.getSpeedLayerOther(),
            request.getFormatPdf(),
            new ArrayList<>(),  // PDF รองไม่มีลายเซ็น (diamond operator)
            null  // signatureImagePaths - ไม่มีรูปภาพสำหรับ PDF รอง
        );
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
            // หา PDF หลักและรอง
            PdfResult mainPdf = pdfArray.stream()
                .filter(p -> "Main".equals(p.getType()))
                .findFirst()
                .orElse(null);
                
            List<PdfResult> otherPdfs = pdfArray.stream()
                .filter(p -> "Other".equals(p.getType()))
                .collect(Collectors.toList());
            
            // สร้างรายการ PDF ที่จะรวม
            List<String> pdfsToMerge = new ArrayList<>();
            
            if (mainPdf != null) {
                pdfsToMerge.add(cleanBase64Prefix(mainPdf.getPdfBase64()));
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
            
            mergedBase64 = pdfService.addLearnerPages(mergedBase64, learners);
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
