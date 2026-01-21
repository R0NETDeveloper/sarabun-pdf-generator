# 📝 Quill Editor Configuration สำหรับระบบ PDF Generator

> เอกสารนี้อธิบายการ Config Quill Editor ให้ตรงกับ PDF Output

## 🎯 หลักการ: HTML เป็น Source of Truth

```
Quill Editor → HTML Content → HtmlContentRenderer → PDF
     ↑                              ↑
  CSS เหมือนกัน              CSS word-break
  Width เท่ากัน              ตัดคำอัตโนมัติ
```

---

## 📐 ค่า Dimension ที่สำคัญ

| รายการ | ค่า (pt) | ค่า (px @ 96dpi) |
|--------|----------|------------------|
| PDF A4 Width | 595pt | 793px |
| PDF A4 Height | 842pt | 1123px |
| Margin ซ้าย/ขวา | 70pt | 93px |
| **Content Width** | **455pt** | **607px** |
| Margin บน/ล่าง | 70pt | 93px |

---

## 🔧 CSS Configuration สำหรับ Quill Editor

### 1. Editor Container (จำลอง A4)

```css
/* Container หลักจำลองหน้า A4 */
.editor-page-container {
    width: 595pt;           /* A4 width */
    min-height: 842pt;      /* A4 height */
    padding: 70pt;          /* Margins เหมือน PDF */
    background: white;
    box-shadow: 0 0 10px rgba(0,0,0,0.1);
    margin: 20px auto;
}
```

### 2. Quill Editor Content Area

```css
/* สำคัญมาก: ต้องตรงกับ PDF */
.ql-editor {
    width: 455pt;           /* Content width = 595 - 70 - 70 */
    min-height: 700pt;
    
    /* Font ตรงกับ PDF */
    font-family: 'TH Sarabun New', 'THSarabunNew', 'Sarabun', sans-serif;
    font-size: 16pt;
    line-height: 1.4;
    
    /* Word-wrap เหมือน PDF */
    word-break: break-word;
    overflow-wrap: break-word;
    word-wrap: break-word;
    
    /* Reset padding */
    padding: 0;
    
    /* Text justify เหมือน PDF */
    text-align: justify;
}

/* Paragraph spacing */
.ql-editor p {
    margin: 0 0 8pt 0;
    word-break: break-word;
}

/* Table styles */
.ql-editor table {
    width: 100%;
    border-collapse: collapse;
    margin: 10pt 0;
    font-size: 14pt;
    table-layout: fixed;    /* บังคับไม่ให้ขยายเกิน */
}

.ql-editor th,
.ql-editor td {
    border: 1px solid #000;
    padding: 6pt 8pt;
    text-align: left;
    vertical-align: top;
    word-break: break-word;
}

.ql-editor th {
    background-color: #f0f0f0;
    font-weight: bold;
    text-align: center;
}

/* List styles */
.ql-editor ul,
.ql-editor ol {
    margin: 8pt 0;
    padding-left: 24pt;
}

.ql-editor li {
    margin-bottom: 4pt;
}
```

### 3. Font Loading (สำคัญ!)

```html
<!-- โหลด TH Sarabun New font -->
<style>
@font-face {
    font-family: 'TH Sarabun New';
    src: url('/fonts/THSarabunNew.ttf') format('truetype');
    font-weight: normal;
    font-style: normal;
}

@font-face {
    font-family: 'TH Sarabun New';
    src: url('/fonts/THSarabunNew-Bold.ttf') format('truetype');
    font-weight: bold;
    font-style: normal;
}
</style>
```

---

## 🚀 Quill Initialization

```javascript
// Quill configuration
const quill = new Quill('#editor', {
    theme: 'snow',
    modules: {
        toolbar: [
            [{ 'header': [1, 2, 3, false] }],
            ['bold', 'italic', 'underline'],
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            [{ 'indent': '-1'}, { 'indent': '+1' }],
            [{ 'align': [] }],
            ['link', 'image'],
            ['clean']
        ]
    },
    placeholder: 'พิมพ์เนื้อหาที่นี่...'
});
```

---

## 📤 การส่งข้อมูลไป API

### Request Format

```javascript
// ดึง HTML จาก Quill
const htmlContent = quill.root.innerHTML;

// สร้าง request body
const requestBody = {
    bookNameId: "officialMemo",
    formatPdf: "A4",
    memo: {
        bookName: "บันทึกข้อความ",
        bookTitle: "เรื่อง...",
        bookNo: "สผ 0101/ว 2568",
        dateThai: "8 มกราคม พ.ศ. 2569",
        department: "สำนักงาน...",
        divisionName: "กอง...",
        bookContent: {
            subject: "หัวข้อเรื่อง",
            content: htmlContent,       // ← HTML จาก Quill
            contentType: "html"         // ← สำคัญมาก!
        }
    },
    // ... ส่วนอื่นๆ
};

// ส่งไป API
fetch('/api/pdf/preview', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestBody)
});
```

### ⚠️ สิ่งสำคัญ

| Field | ค่า | หมายเหตุ |
|-------|-----|----------|
| `content` | HTML string | ดึงจาก `quill.root.innerHTML` |
| `contentType` | `"html"` | **ต้องเป็น "html" เสมอ!** |

---

## ✅ Checklist ก่อน Deploy

- [ ] โหลด font TH Sarabun New แล้ว
- [ ] Editor width = 455pt (หรือ 607px)
- [ ] ใช้ CSS word-break: break-word
- [ ] ส่ง contentType: "html" ใน request
- [ ] ทดสอบข้อความยาวไม่มี space (ภาษาไทย)
- [ ] ทดสอบตารางกว้างเกิน

---

## 🔗 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/pdf/preview` | POST | Preview PDF (Base64) |
| `/api/pdf/generate` | POST | Generate PDF (File) |
| `/api/pdf/health` | GET | Health check |

---

## 📝 ตัวอย่าง HTML Content

```html
<p>        ด้วยสำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์มีความประสงค์จะดำเนินการพัฒนาระบบสารบรรณอิเล็กทรอนิกส์ เพื่อรองรับการทำงานในยุคดิจิทัล</p>

<p>        จึงเรียนมาเพื่อโปรดพิจารณาอนุมัติ</p>

<table>
    <thead>
        <tr>
            <th>ลำดับ</th>
            <th>รายการ</th>
            <th>จำนวนเงิน</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>ค่าพัฒนาระบบ</td>
            <td style="text-align: right;">100,000 บาท</td>
        </tr>
    </tbody>
</table>
```

---

## 🐛 Troubleshooting

### ปัญหา: ข้อความล้นออกนอกขอบ
**สาเหตุ**: ไม่มี CSS word-break  
**แก้ไข**: เพิ่ม `word-break: break-word;` ใน .ql-editor

### ปัญหา: Font ไม่ตรงกับ PDF
**สาเหตุ**: ไม่ได้โหลด TH Sarabun New  
**แก้ไข**: โหลด font และใส่ใน font-family

### ปัญหา: ตัดบรรทัดไม่ตรงกับ PDF
**สาเหตุ**: Width ไม่ตรงกัน  
**แก้ไข**: ใช้ width: 455pt ใน editor

---

*Last Updated: 20 มกราคม 2569*
