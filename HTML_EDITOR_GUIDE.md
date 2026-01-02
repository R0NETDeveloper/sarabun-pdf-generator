# HTML Editor Integration Guide

## รองรับ HTML จาก WYSIWYG Editors

Sarabun PDF API รองรับ HTML content จาก rich text editors เช่น **TinyMCE**, **CKEditor**, **Quill**, หรือ **FroalaEditor**

---

## วิธีการทำงาน

เมื่อส่ง HTML เข้ามา ระบบจะแปลงเป็น plain text อัตโนมัติก่อนสร้าง PDF:

### 1️⃣ **ตรวจสอบว่าเป็น HTML หรือไม่**
```java
HtmlUtils.isHtml(content) 
// → true ถ้ามี HTML tags
```

### 2️⃣ **แปลง HTML เป็น Plain Text**
```java
String plainText = HtmlUtils.htmlToPlainText(htmlContent);
```

### 3️⃣ **สร้าง PDF ด้วย plain text**
```java
pdfService.generateMainDocumentPdf(...)
```

---

## HTML Tags ที่รองรับ

| HTML Tag | การแปลง | ตัวอย่าง |
|----------|---------|---------|
| `<p>`, `<div>` | ย่อหน้าใหม่ (`\n\n`) | `<p>ข้อความ</p>` → `ข้อความ\n\n` |
| `<br>` | ขึ้นบรรทัดใหม่ (`\n`) | `ข้อความ<br>บรรทัดใหม่` → `ข้อความ\nบรรทัดใหม่` |
| `<li>` | bullet point | `<li>รายการ</li>` → `• รายการ` |
| `<h1>`-`<h6>` | heading (เพิ่ม newlines) | `<h2>หัวข้อ</h2>` → `\nหัวข้อ\n` |
| `<strong>`, `<b>` | ลบ tag (ใช้ text ธรรมดา) | `<strong>ตัวหนา</strong>` → `ตัวหนา` |
| `<em>`, `<i>` | ลบ tag (ใช้ text ธรรมดา) | `<em>ตัวเอียง</em>` → `ตัวเอียง` |
| `<u>`, `<span>` | ลบ tag | `<u>ขีดเส้นใต้</u>` → `ขีดเส้นใต้` |

---

## HTML Entities ที่รองรับ

| Entity | แปลงเป็น | ตัวอย่าง |
|--------|---------|---------|
| `&nbsp;` | space | `ข้อความ&nbsp;ช่องว่าง` → `ข้อความ ช่องว่าง` |
| `&amp;` | `&` | `A&amp;B` → `A&B` |
| `&lt;` | `<` | `&lt;tag&gt;` → `<tag>` |
| `&gt;` | `>` | |
| `&quot;` | `"` | |

---

## ตัวอย่างการใช้งาน

### ✅ Example 1: HTML จาก TinyMCE

**Input (HTML):**
```html
<p>ตามที่ สำนักงาน ETDA ได้มีการพัฒนา <strong>Sarabun PDF API</strong></p>
<br>
<p>เพื่อใช้ในการสร้างเอกสารราชการ&nbsp;ในรูปแบบ PDF นั้น</p>
```

**Output (Plain Text):**
```
ตามที่ สำนักงาน ETDA ได้มีการพัฒนา Sarabun PDF API

เพื่อใช้ในการสร้างเอกสารราชการ ในรูปแบบ PDF นั้น
```

---

### ✅ Example 2: HTML จาก CKEditor

**Input (HTML):**
```html
<h2>หัวข้อเรื่อง</h2>
<p>เนื้อหาย่อหน้าที่ 1 พร้อมกับ <em>ตัวเอียง</em> และ <strong>ตัวหนา</strong></p>
<ul>
  <li>รายการที่ 1</li>
  <li>รายการที่ 2</li>
</ul>
```

**Output (Plain Text):**
```
หัวข้อเรื่อง

เนื้อหาย่อหน้าที่ 1 พร้อมกับ ตัวเอียง และ ตัวหนา
• รายการที่ 1
• รายการที่ 2
```

---

### ✅ Example 3: Request JSON

```json
{
  "bookTitle": "ขอความอนุเคราะห์จัดส่งเอกสาร",
  "bookNo": "สธ 1234/2568",
  "dateThai": "29 ธันวาคม 2568",
  "department": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์",
  "recipients": "ผู้อำนวยการ ETDA",
  "bookContent": [
    {
      "bookContentTitle": "",
      "bookContent": "<p>ตามที่ <strong>ETDA</strong> ได้มีการพัฒนา Sarabun PDF API</p><br><p>จึงเรียนมาเพื่อโปรดทราบ</p>"
    }
  ]
}
```

---

## การทดสอบ

### 1️⃣ **ทดสอบด้วย test.html**

1. เปิด browser: http://localhost:8888/test.html
2. ดู PDF preview ด้านขวา
3. กด Refresh เพื่อดู PDF ใหม่

### 2️⃣ **ทดสอบด้วย Postman/cURL**

```bash
curl -X POST http://localhost:8888/api/pdf/preview \
  -H "Content-Type: application/json" \
  -d '{
    "bookTitle": "ทดสอบ HTML",
    "bookNo": "สธ 001/2568",
    "dateThai": "29 ธ.ค. 2568",
    "department": "ฝ่ายพัฒนา",
    "recipients": "ผู้อำนวยการ",
    "bookContent": [{
      "bookContentTitle": "",
      "bookContent": "<p>HTML จาก <strong>editor</strong></p>"
    }]
  }'
```

### 3️⃣ **ทดสอบ HtmlUtils (Unit Test)**

```bash
cd GeneratePDFBox
java -cp target/classes th.go.etda.sarabun.pdf.util.HtmlUtils
```

---

## หมายเหตุสำคัญ

### ⚠️ **ข้อจำกัด**

1. **ไม่รองรับ CSS Styles:**
   - `<span style="color: red;">` → ลบ styles ออก
   - PDF ใช้ font และสีเริ่มต้นเท่านั้น

2. **ไม่รองรับ images:**
   - `<img src="...">` → ไม่แสดงในภาพ PDF
   - ต้องแยกการจัดการ images ต่างหาก

3. **ไม่รองรับ tables:**
   - `<table>` → แปลงเป็น text ธรรมดา
   - อาจสร้างตารางใน PDF ในอนาคต

### ✅ **แนะนำ**

1. **ใช้ HTML tags เท่าที่จำเป็น:**
   - `<p>`, `<br>`, `<strong>`, `<em>` เท่านั้น
   - หลีกเลี่ยง nested tags ซับซ้อน

2. **ทดสอบ HTML ก่อนส่ง:**
   - ตรวจสอบว่า tags ปิดถูกต้อง
   - ลองดู preview ใน test.html

3. **Fallback สำหรับ plain text:**
   - ถ้าไม่มี HTML tags → ใช้ text ตามปกติ
   - ระบบรองรับทั้ง 2 แบบ

---

## API Reference

### `HtmlUtils.htmlToPlainText(String html)`

แปลง HTML เป็น plain text พร้อมรักษา line breaks

**Parameters:**
- `html` - HTML content จาก editor

**Returns:**
- Plain text string สำหรับใส่ใน PDF

**Example:**
```java
String html = "<p>Hello <strong>World</strong></p>";
String text = HtmlUtils.htmlToPlainText(html);
// → "Hello World\n\n"
```

### `HtmlUtils.isHtml(String text)`

ตรวจสอบว่า string มี HTML tags หรือไม่

**Parameters:**
- `text` - ข้อความที่ต้องการตรวจสอบ

**Returns:**
- `true` ถ้ามี HTML tags
- `false` ถ้าเป็น plain text

**Example:**
```java
HtmlUtils.isHtml("<p>Hello</p>")  // → true
HtmlUtils.isHtml("Plain text")     // → false
```

### `HtmlUtils.stripHtmlTags(String html)`

ลบ HTML tags ทั้งหมด (วิธีง่าย)

**Parameters:**
- `html` - HTML content

**Returns:**
- Plain text (ลบ tags ออกหมด, ไม่รักษา line breaks)

**Example:**
```java
String html = "<p>Hello</p><br><p>World</p>";
String text = HtmlUtils.stripHtmlTags(html);
// → "Hello World" (ไม่มี line breaks)
```

---

## ทดสอบเพิ่มเติม

### Test Case 1: Rich Text Editor

```java
String html = """
<p>ข้อความย่อหน้าที่ 1 พร้อมกับ <strong>ตัวหนา</strong></p>
<br>
<p>ข้อความย่อหน้าที่ 2 พร้อมกับ <em>ตัวเอียง</em></p>
""";
String text = HtmlUtils.htmlToPlainText(html);
// ตรวจสอบว่ามี line breaks ที่ถูกต้อง
```

### Test Case 2: HTML Entities

```java
String html = "ข้อความพร้อม&nbsp;ช่องว่าง และ &amp; symbol";
String text = HtmlUtils.htmlToPlainText(html);
// → "ข้อความพร้อม ช่องว่าง และ & symbol"
```

### Test Case 3: Lists

```java
String html = """
<ul>
  <li>รายการที่ 1</li>
  <li>รายการที่ 2</li>
  <li>รายการที่ 3</li>
</ul>
""";
String text = HtmlUtils.htmlToPlainText(html);
// → "• รายการที่ 1\n• รายการที่ 2\n• รายการที่ 3\n"
```

---

## สรุป

✅ **รองรับ HTML จาก editors ทุกชนิด**  
✅ **แปลง HTML เป็น plain text อัตโนมัติ**  
✅ **รักษา line breaks และ formatting**  
✅ **ลบ styles, scripts, และ comments**  
✅ **รองรับ Thai และ English**  
✅ **ใช้ Jsoup library (robust HTML parsing)**

---

**Created:** 29 December 2024  
**Version:** 1.0.0  
**Library:** Apache PDFBox 3.0.3 + Jsoup 1.18.3
