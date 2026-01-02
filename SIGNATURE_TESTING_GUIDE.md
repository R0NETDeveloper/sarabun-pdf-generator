# คู่มือการทดสอบลายเซ็น PDF

## วิธีการทดสอบการวางรูปภาพลายเซ็น

### 1. เตรียมรูปภาพลายเซ็น

สร้างหรือใช้รูปภาพลายเซ็นของคุณ:
- **รูปแบบที่รองรับ**: PNG (แนะนำ), JPG
- **ขนาดแนะนำ**: 200x100 pixels หรือ 300x150 pixels
- **พื้นหลัง**: โปร่งใส (PNG) เพื่อความสวยงาม

วางไฟล์รูปภาพไว้ที่:
```
src/main/resources/images/signature.png
```

### 2. ทดสอบผ่าน Postman

**Endpoint**: `POST http://localhost:8888/api/generate-pdf/preview-with-signature`

**Request Body** (JSON):
```json
{
  "bookNo": "กค 0201.001/ว 23",
  "bookDate": "2024-01-15",
  "fromName": "สำนักพัฒนาระบบราชการ",
  "sendTo": "ผู้อำนวยการสำนักงาน ETDA",
  "subject": "ทดสอบลายเซ็นอิเล็กทรอนิกส์",
  "bookContent": [
    {
      "bookContentTitle": "",
      "bookContent": "<p>ด้วยสำนักงานพัฒนารัฐบาลดิจิทัล (องค์การมหาชน) มีความประสงค์จะทดสอบระบบลายเซ็นอิเล็กทรอนิกส์</p>"
    }
  ],
  "bookSigned": [
    {
      "prefixName": "นาย",
      "firstname": "สมชาย",
      "lastname": "ใจดี",
      "positionName": "ผู้อำนวยการสำนักงาน",
      "signatureImagePath": "classpath:images/signature.png"
    }
  ]
}
```

### 3. ดูผลลัพธ์

เปิด browser ไปที่:
```
http://localhost:8888/api/pdf/view
```

**คุณจะเห็น**:
- ช่องว่างสำหรับลายเซ็น (50 points)
- รูปภาพลายเซ็นวางอยู่ในตำแหน่งที่กำหนด (ขนาด 80x40 points)
- ชื่อผู้ลงนาม (มีวงเล็บครอบ)
- ตำแหน่งงาน

### 4. รูปแบบลายเซ็นที่ได้

```
จึงเรียนมาเพื่อทราบและพิจารณา

        [รูปภาพลายเซ็น]
         (นายสมชาย ใจดี)
       ผู้อำนวยการสำนักงาน
```

## การปรับแต่งขนาดและตำแหน่ง

แก้ไขใน `PdfService.java` method `drawSignatureImage`:

```java
// กำหนดขนาดรูปภาพลายเซ็น (ปรับได้ตามต้องการ)
float signatureWidth = 80f;  // ความกว้าง (เพิ่ม = ใหญ่ขึ้น)
float signatureHeight = 40f; // ความสูง (เพิ่ม = สูงขึ้น)
```

## การใช้ไฟล์จาก File System

ถ้าไม่ต้องการใส่ใน classpath สามารถใช้ path ธรรมดาได้:

```json
{
  "signatureImagePath": "C:/signatures/somchai_signature.png"
}
```

## การทดสอบแบบง่าย (ไม่ใช้รูปภาพ)

ถ้าไม่ระบุ `signatureImagePath` หรือไฟล์ไม่พบ:
- ระบบจะเจาะช่องว่าง 50 points สำหรับลายเซ็นมือเขียน
- แสดงชื่อและตำแหน่งตามปกติ

## Tips สำหรับรูปภาพลายเซ็นที่ดี

1. **ใช้พื้นหลังโปร่งใส** (PNG) เพื่อให้ดูเป็นธรรมชาติ
2. **ขนาดไม่ใหญ่เกิน** 300KB เพื่อความเร็วในการโหลด
3. **ความละเอียดพอดี** 150-300 DPI
4. **สีดำหรือน้ำเงิน** เหมือนลายเซ็นจริง
5. **Crop ให้พอดี** ตัดส่วนที่ไม่จำเป็นออก

## ตัวอย่างการสร้างลายเซ็นดิจิทัล

### ใช้เครื่องมือออนไลน์:
1. SignNow
2. DocuSign
3. Adobe Sign (Free Trial)

### หรือสร้างเองจาก:
1. เซ็นบนกระดาษ → ถ่ายรูป → ตัดพื้นหลัง
2. ใช้ Stylus/Apple Pencil เขียนบน tablet
3. ใช้ Microsoft Paint หรือ GIMP สร้างลายเซ็น

## Troubleshooting

### ❌ รูปภาพไม่ปรากฏ
- ตรวจสอบ path ว่าถูกต้อง
- ตรวจสอบไฟล์อยู่ใน `src/main/resources/images/`
- ลอง rebuild project: `mvn clean compile`

### ❌ รูปภาพบิดเบี้ยว
- ปรับ `signatureWidth` และ `signatureHeight` ให้เหมาะสมกับ aspect ratio

### ❌ รูปภาพ too small/too large
- แก้ไข constant ใน `drawSignatureImage` method

## ตัวอย่าง Code สำหรับการเพิ่มรูปหลายคน

```json
{
  "bookSigned": [
    {
      "prefixName": "นาย",
      "firstname": "สมชาย",
      "lastname": "ใจดี",
      "positionName": "ผู้อำนวยการ",
      "signatureImagePath": "classpath:images/signature1.png"
    },
    {
      "prefixName": "นาง",
      "firstname": "สมหญิง",
      "lastname": "รักดี",
      "positionName": "รองผู้อำนวยการ",
      "signatureImagePath": "classpath:images/signature2.png"
    }
  ]
}
```

## Next Steps

สำหรับการลงนามอิเล็กทรอนิกส์จริง (Digital Signature with Certificate):
- ต้องใช้ Digital Certificate (e.g., from CA)
- ต้อง implement PDF Signing ด้วย PDFBox Signature API
- ต้องเชื่อมต่อกับ HSM หรือ Smart Card

---

**หมายเหตุ**: ตอนนี้เป็นการวางรูปภาพลายเซ็นแบบง่าย ไม่ใช่ Digital Signature ที่มี cryptographic verification
