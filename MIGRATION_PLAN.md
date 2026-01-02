# 📋 แผนปรับปรุง PDF Service (Java) ให้ทำงานแบบ C#

## 🎯 สิ่งที่พบจาก C# Code

### 1. **หน้าหลัก/หน้ารอง (Multi-page)**
- C# ใช้ HTML + SelectPDF → **ขึ้นหน้าใหม่อัตโนมัติ** เมื่อเนื้อหายาว
- Java ใช้ PDFBox Direct Drawing → **ต้องเช็ค `yPosition` เอง**

### 2. **ลายเซ็นหลายคน**
- C# มี field `bookSigned` (array) → loop แสดงทีละคน
- Java มี `List<String> signatures` แต่วาดแบบธรรมดา

### 3. **หมายเลขหน้า (-1, -2, -3)**
- C# ใช้ `AddPageNumber()` จาก PDF library
- Java ต้องวาดเองทุกหน้า

### 4. **NextPageStatus**
- C# มี `boolean NextPageStatus` ใน `BookContent`
- ถ้า `true` = **บังคับขึ้นหน้าใหม่** ก่อนแสดง content ถัดไป

---

## ✅ สิ่งที่ต้องแก้ใน Java Project

### 🔧 1. เพิ่ม Multi-page Support
```java
// เพิ่มใน PdfService.java
private static final float MIN_Y_POSITION = MARGIN_BOTTOM + 100; // พื้นที่ขั้นต่ำ

// เช็คก่อนวาดทุกครั้ง:
if (yPosition < MIN_Y_POSITION) {
    // สร้างหน้าใหม่
    PDPage newPage = new PDPage(PDRectangle.A4);
    document.addPage(newPage);
    
    // ปิด content stream เก่า
    contentStream.close();
    
    // เปิด content stream ใหม่
    contentStream = new PDPageContentStream(document, newPage);
    yPosition = PAGE_HEIGHT - MARGIN_TOP;
    
    // วาดหมายเลขหน้า (-2, -3, ...)
    int pageNum = document.getNumberOfPages();
    drawPageNumber(contentStream, pageNum);
}
```

### 🔧 2. ปรับปรุง Signature Section (ลายเซ็นหลายคน)
```java
// แยกข้อมูลลายเซ็นเป็น Object
public static class SignatureInfo {
    private String prefixName;
    private String firstname;
    private String lastname;
    private String positionName;
    // getter/setter
}

// วาดลายเซ็นแบบ loop
for (SignatureInfo signer : signers) {
    // วาดช่องลายเซ็น (ไม่วาดกรอบ)
    String signerText = String.format("(%s %s %s)", 
        signer.getPrefixName(), 
        signer.getFirstname(), 
        signer.getLastname());
    
    yPosition = drawText(contentStream, signerText, ...);
    yPosition = drawText(contentStream, signer.getPositionName(), ...);
    yPosition -= SPACING_BETWEEN_SIGNERS;
    
    // เช็คว่าพอดีหรือไม่
    if (yPosition < MIN_Y_POSITION && hasMoreSigners) {
        createNewPage(); // ขึ้นหน้าใหม่
    }
}
```

### 🔧 3. เพิ่ม NextPageStatus Support
```java
// ใน GeneratePdfRequest.BookContent
private Boolean nextPageStatus; // เพิ่ม field นี้

// ใน PdfService.generateOfficialMemoPdf()
for (BookContent content : bookContents) {
    // เช็คว่าต้องขึ้นหน้าใหม่หรือไม่
    if (Boolean.TRUE.equals(content.getNextPageStatus())) {
        createNewPage();
        yPosition = PAGE_HEIGHT - MARGIN_TOP;
    }
    
    // วาดเนื้อหา
    yPosition = drawMultilineText(..., content.getBookContent(), ...);
}
```

### 🔧 4. Method สำหรับสร้างหน้าใหม่
```java
private PDPage createNewPage(PDDocument document, 
                             PDPageContentStream currentStream) throws IOException {
    // ปิด stream เก่า
    if (currentStream != null) {
        currentStream.close();
    }
    
    // สร้างหน้าใหม่
    PDPage newPage = new PDPage(PDRectangle.A4);
    document.addPage(newPage);
    
    // สร้าง stream ใหม่
    PDPageContentStream newStream = new PDPageContentStream(document, newPage);
    
    // วาดหมายเลขหน้า
    int pageNumber = document.getNumberOfPages();
    drawPageNumber(newStream, pageNumber, fontRegular);
    
    return newPage;
}

private void drawPageNumber(PDPageContentStream stream, 
                           int pageNumber, 
                           PDFont font) throws IOException {
    String pageText = "-" + pageNumber;
    float x = PAGE_WIDTH - MARGIN_RIGHT - 30;
    float y = PAGE_HEIGHT - MARGIN_TOP + 10;
    drawText(stream, pageText, font, FONT_SIZE_CONTENT, x, y);
}
```

---

## 📝 ขั้นตอนการทำ

### Phase 1: Multi-page Support (ลำดับความสำคัญสูง)
- [ ] เพิ่ม `createNewPage()` method
- [ ] เพิ่ม `drawPageNumber()` method
- [ ] เพิ่มการเช็ค `yPosition < MIN_Y_POSITION` ในทุกจุดที่วาด
- [ ] ทดสอบกับเนื้อหายาว ๆ

### Phase 2: NextPageStatus Support
- [ ] เพิ่ม `nextPageStatus` field ใน `BookContent`
- [ ] เพิ่มการเช็คใน loop `bookContent`
- [ ] ทดสอบกับ multiple contents

### Phase 3: Signature Enhancements
- [ ] แยก signature เป็น `SignatureInfo` object
- [ ] รองรับลายเซ็นหลายคน (loop)
- [ ] เช็คว่าลายเซ็นล้นหน้าหรือไม่
- [ ] ทดสอบกับ 1-5 ผู้ลงนาม

### Phase 4: Advanced Features
- [ ] เพิ่มการจัดการ header/footer ทุกหน้า
- [ ] เพิ่มการวาดโลโก้ทุกหน้า (ถ้าต้องการ)
- [ ] ปรับแต่ง spacing และ layout

---

## 🎨 ตัวอย่าง Flow ที่สมบูรณ์

```java
public String generateOfficialMemoPdf(...) {
    PDDocument document = new PDDocument();
    PDPage firstPage = new PDPage(PDRectangle.A4);
    document.addPage(firstPage);
    
    PDPageContentStream contentStream = 
        new PDPageContentStream(document, firstPage);
    
    float yPosition = PAGE_HEIGHT - MARGIN_TOP;
    
    // วาดหมายเลขหน้า -1
    drawPageNumber(contentStream, 1, fontRegular);
    
    // วาดหัวเรื่อง, เลขที่, วันที่
    yPosition = drawHeader(contentStream, ...);
    
    // วาดเนื้อหา (แบ่งหน้าอัตโนมัติ)
    for (BookContent content : bookContents) {
        // เช็ค NextPageStatus
        if (Boolean.TRUE.equals(content.getNextPageStatus())) {
            PDPage newPage = createNewPage(document, contentStream);
            contentStream = new PDPageContentStream(document, newPage);
            yPosition = PAGE_HEIGHT - MARGIN_TOP;
        }
        
        // วาดเนื้อหา
        String text = content.getBookContent();
        String[] lines = splitIntoLines(text, ...);
        
        for (String line : lines) {
            // เช็คว่าพอดีหรือไม่
            if (yPosition < MIN_Y_POSITION) {
                PDPage newPage = createNewPage(document, contentStream);
                contentStream = new PDPageContentStream(document, newPage);
                yPosition = PAGE_HEIGHT - MARGIN_TOP;
            }
            
            yPosition = drawText(contentStream, line, ...);
        }
    }
    
    // วาดลายเซ็น (เช็คพื้นที่)
    if (yPosition < MIN_Y_POSITION + 200) { // ต้องการพื้นที่สำหรับลายเซ็น
        PDPage newPage = createNewPage(document, contentStream);
        contentStream = new PDPageContentStream(document, newPage);
        yPosition = PAGE_HEIGHT - MARGIN_TOP;
    }
    
    drawSignatures(contentStream, signers, yPosition);
    
    contentStream.close();
    return convertToBase64(document);
}
```

---

## 🚀 จะเริ่มจากส่วนไหนก่อน?

1. **เริ่มจาก Multi-page** → ทำให้เนื้อหายาวไม่ล้นหน้า
2. **เพิ่ม NextPageStatus** → รองรับการบังคับขึ้นหน้าใหม่
3. **ปรับปรุง Signature** → รองรับหลายคน + ขึ้นหน้าใหม่ถ้าจำเป็น

ต้องการให้ฉันเริ่มทำตั้งแต่ Phase 1 เลยไหมครับ?
