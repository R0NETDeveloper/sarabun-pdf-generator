# 📄 PDF Generation Architecture

## 🎯 สถาปัตยกรรมระบบ

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Browser)                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Rich Text Editor (TinyMCE/CKEditor/Quill)          │   │
│  │  ผู้ใช้พิมพ์ข้อความ → Editor สร้าง HTML string      │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ส่ง JSON: { "bookContent": "<p>ข้อความ...</p>" }          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼ HTTP POST /api/pdf/preview
┌─────────────────────────────────────────────────────────────┐
│                 GeneratePdfController                       │
│  รับ request → เรียก Service → ส่ง PDF กลับ                │
└─────────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ HtmlToPdfService│ │GeneratePdfService│ │SignatureField  │
│                 │ │                   │ │Service         │
│ OpenHTMLToPDF   │ │ PDFBox (เดิม)    │ │ PDFBox         │
│ แปลง HTML→PDF   │ │ สร้าง PDF ตรงๆ   │ │ เพิ่มช่องลายเซ็น│
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │               │               │
          └───────────────┼───────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Final PDF with Signature Field                 │
│  เปิดใน Acrobat → คลิกช่องลายเซ็น → เซ็นได้เลย              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Services

### 1. HtmlToPdfService

**หน้าที่:** แปลง HTML + CSS เป็น PDF

```java
@Service
public class HtmlToPdfService {

    // แปลง HTML string เป็น PDF bytes
    public byte[] convertHtmlToPdf(String html);

    // สร้าง HTML สำหรับหนังสือราชการ
    public String buildOfficialDocumentHtml(
        String bookNo,
        String bookTitle,
        String dateThai,
        String department,
        String recipients,
        String content,
        String signerName,
        String signerPosition,
        String speedLabel
    );
}
```

**Library:** OpenHTMLToPDF (LGPL)

-   รองรับ CSS 2.1 + บางส่วน CSS3
-   รองรับ Thai fonts
-   ใช้ PDFBox เป็น backend

---

### 2. SignatureFieldService

**หน้าที่:** เพิ่มช่องลายเซ็นดิจิทัลใน PDF

```java
@Service
public class SignatureFieldService {

    // เพิ่ม Signature Field
    public byte[] addSignatureField(
        byte[] pdfBytes,
        String fieldName,
        int pageNumber,
        float x, float y,
        float width, float height,
        String reason
    );

    // เพิ่มช่องลายเซ็นตำแหน่งมาตรฐาน (กลาง-ขวาล่าง)
    public byte[] addDefaultSignatureField(byte[] pdfBytes, String signerName);
}
```

**Library:** Apache PDFBox (Apache 2.0)

-   สร้าง AcroForm + PDSignatureField
-   รองรับ Digital Certificate

---

### 3. GeneratePdfService (เดิม)

**หน้าที่:** สร้าง PDF ด้วย PDFBox โดยตรง

-   วาดข้อความ, รูปภาพ ด้วย PDPageContentStream
-   ควบคุม layout ละเอียด
-   รวม PDF หลายหน้า

---

## 📁 ไฟล์ที่สร้าง/แก้ไข

| ไฟล์                         | การเปลี่ยนแปลง                           |
| ---------------------------- | ---------------------------------------- |
| `pom.xml`                    | เพิ่ม OpenHTMLToPDF dependency           |
| `HtmlToPdfService.java`      | **สร้างใหม่** - แปลง HTML เป็น PDF       |
| `SignatureFieldService.java` | **สร้างใหม่** - เพิ่มช่องลายเซ็น         |
| `GeneratePdfController.java` | เพิ่ม test endpoint `/api/pdf/test-html` |
| `test-template.html`         | **สร้างใหม่** - ตัวอย่าง HTML template   |

---

## 🚀 วิธีทดสอบ

### 1. รัน Spring Boot

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\mvnw.cmd spring-boot:run
```

### 2. เปิด Browser

```
http://localhost:8888/api/pdf/test-html
```

### 3. ผลลัพธ์

-   PDF จะแสดงใน browser
-   มีช่องลายเซ็น (Signature Field)
-   เปิดใน Adobe Acrobat แล้วคลิกเซ็นได้

---

## 📋 Dependencies

| Library       | Version | License    | หน้าที่         |
| ------------- | ------- | ---------- | --------------- |
| Apache PDFBox | 3.0.3   | Apache 2.0 | สร้าง/แก้ไข PDF |
| OpenHTMLToPDF | 1.0.10  | LGPL       | แปลง HTML→PDF   |
| Jsoup         | 1.18.3  | MIT        | Parse HTML      |

---

## 🔐 License สรุป

| License    | ใช้ Commercial ได้? | ต้องเปิด Source?       |
| ---------- | ------------------- | ---------------------- |
| Apache 2.0 | ✅                  | ❌                     |
| LGPL       | ✅                  | ❌ (ถ้าไม่แก้ library) |
| MIT        | ✅                  | ❌                     |

**สรุป:** ใช้ได้ทั้งหมดในโปรเจค Commercial/ราชการ โดยไม่ต้องเปิด source code
