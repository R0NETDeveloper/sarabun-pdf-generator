# 📍 การตั้งค่าตำแหน่ง Signature Field

## ไฟล์ที่ต้องแก้ไข

```
src/main/java/th/go/etda/sarabun/pdf/service/SignatureFieldService.java
```

---

## 🎛️ ค่า Constants ที่ปรับได้

| ค่า                | Default | คำอธิบาย                      | หน่วย  |
| ------------------ | ------- | ----------------------------- | ------ |
| `SIGNATURE_WIDTH`  | 150f    | ความกว้างกล่องลายเซ็น         | points |
| `SIGNATURE_HEIGHT` | 50f     | ความสูงกล่องลายเซ็น           | points |
| `MARGIN_RIGHT`     | 85f     | ระยะห่างจากขอบขวา             | points |
| `MARGIN_LEFT`      | 85f     | ระยะห่างจากขอบซ้าย            | points |
| `MARGIN_BOTTOM`    | 142f    | ระยะห่างจากขอบล่าง            | points |
| `STACK_SPACING`    | 70f     | ระยะห่างระหว่างลายเซ็นซ้อนกัน | points |

---

## 📐 การแปลงหน่วย

```
1 point   = 0.353 mm
1 mm      = 2.83 points
1 inch    = 72 points
1 cm      = 28.3 points
```

### ตารางแปลงหน่วยสำเร็จรูป

| mm  | points |
| --- | ------ |
| 10  | 28.3   |
| 20  | 56.7   |
| 30  | 85     |
| 40  | 113.4  |
| 50  | 141.7  |
| 60  | 170    |

---

## 🖼️ ตำแหน่งบนหน้า A4

```
┌─────────────────────────────────────────┐ ← ขอบบน
│                                         │
│              หน้า A4                     │
│         (595 x 842 points)              │
│                                         │
│                                         │
│                                         │
│                                         │
│                                         │
│                                         │
│   ┌──────────┐           ┌──────────┐   │
│   │ Sig Left │           │ Sig Right│   │
│   └──────────┘           └──────────┘   │
│   ← MARGIN_LEFT    MARGIN_RIGHT →       │
│              ↑ MARGIN_BOTTOM            │
└─────────────────────────────────────────┘ ← ขอบล่าง
```

---

## 🔧 วิธีปรับตำแหน่ง

### 1. ขยับลายเซ็นไปทางขวา/ซ้าย

```java
// ขยับไปทางซ้าย (เพิ่มค่า MARGIN_RIGHT)
private static final float MARGIN_RIGHT = 100f;  // เดิม 85f

// ขยับไปทางขวา (ลดค่า MARGIN_RIGHT)
private static final float MARGIN_RIGHT = 60f;   // เดิม 85f
```

### 2. ขยับลายเซ็นขึ้น/ลง

```java
// ขยับขึ้น (เพิ่มค่า MARGIN_BOTTOM)
private static final float MARGIN_BOTTOM = 200f;  // เดิม 142f

// ขยับลง (ลดค่า MARGIN_BOTTOM)
private static final float MARGIN_BOTTOM = 100f;  // เดิม 142f
```

### 3. ปรับขนาดกล่องลายเซ็น

```java
// กล่องใหญ่ขึ้น
private static final float SIGNATURE_WIDTH = 200f;   // เดิม 150f
private static final float SIGNATURE_HEIGHT = 70f;   // เดิม 50f

// กล่องเล็กลง
private static final float SIGNATURE_WIDTH = 120f;   // เดิม 150f
private static final float SIGNATURE_HEIGHT = 40f;   // เดิม 50f
```

---

## 📋 ตัวอย่าง Preset ที่ใช้บ่อย

### Preset A: ตำแหน่งมาตรฐาน (Default)

```java
private static final float SIGNATURE_WIDTH = 150f;
private static final float SIGNATURE_HEIGHT = 50f;
private static final float MARGIN_RIGHT = 85f;
private static final float MARGIN_BOTTOM = 142f;
```

### Preset B: ลายเซ็นขนาดใหญ่ ตรงกลางล่าง

```java
private static final float SIGNATURE_WIDTH = 200f;
private static final float SIGNATURE_HEIGHT = 70f;
private static final float MARGIN_RIGHT = 197.5f;  // (595 - 200) / 2
private static final float MARGIN_BOTTOM = 100f;
```

### Preset C: ลายเซ็นใกล้ขอบขวามากขึ้น

```java
private static final float SIGNATURE_WIDTH = 150f;
private static final float SIGNATURE_HEIGHT = 50f;
private static final float MARGIN_RIGHT = 50f;
private static final float MARGIN_BOTTOM = 142f;
```

### Preset D: ลายเซ็นสูงขึ้น (กลางหน้า)

```java
private static final float SIGNATURE_WIDTH = 150f;
private static final float SIGNATURE_HEIGHT = 50f;
private static final float MARGIN_RIGHT = 85f;
private static final float MARGIN_BOTTOM = 300f;
```

---

## 🧪 การทดสอบ

หลังแก้ไขค่า ให้ restart Spring Boot แล้วทดสอบที่:

| Endpoint                                           | คำอธิบาย                         |
| -------------------------------------------------- | -------------------------------- |
| http://localhost:8888/api/pdf/test-html            | ทดสอบ 1 หน้า 1 ลายเซ็น           |
| http://localhost:8888/api/pdf/test-long-content    | ทดสอบหลายหน้า (auto หน้าสุดท้าย) |
| http://localhost:8888/api/pdf/test-multi-signature | ทดสอบ 2 ลายเซ็น (ซ้าย-ขวา)       |

---

## 📝 สูตรคำนวณตำแหน่ง

### ตำแหน่ง X (แนวนอน)

```java
// ลายเซ็นขวา
float x = pageWidth - SIGNATURE_WIDTH - MARGIN_RIGHT;
// ตัวอย่าง: 595 - 150 - 85 = 360

// ลายเซ็นซ้าย
float x = MARGIN_LEFT;
// ตัวอย่าง: 85

// ลายเซ็นกลาง
float x = (pageWidth - SIGNATURE_WIDTH) / 2;
// ตัวอย่าง: (595 - 150) / 2 = 222.5
```

### ตำแหน่ง Y (แนวตั้ง)

```java
// ลายเซ็นล่าง
float y = MARGIN_BOTTOM;
// ตัวอย่าง: 142

// ลายเซ็นกลางหน้า
float y = (pageHeight - SIGNATURE_HEIGHT) / 2;
// ตัวอย่าง: (842 - 50) / 2 = 396
```

---

## ⚠️ ข้อควรระวัง

1. **หน่วยเป็น points** - ไม่ใช่ mm หรือ cm
2. **Y = 0 คือขอบล่าง** - ไม่ใช่ขอบบน (ระบบ coordinate กลับหัว)
3. **หน้า A4 = 595 x 842 points** - ถ้าใช้ขนาดอื่นต้องคำนวณใหม่
4. **Restart Spring Boot** - หลังแก้ไข Java ต้อง restart ใหม่

---

## 📞 ติดต่อ

หากมีปัญหาหรือต้องการ Preset เพิ่มเติม สามารถแจ้งได้ครับ
