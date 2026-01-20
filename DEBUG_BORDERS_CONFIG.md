# 🎨 Debug Borders Configuration Guide

> คู่มือการปรับค่าตำแหน่งต่างๆ ใน PDF Generator โดยอ้างอิงจากเส้น Debug

## 📍 ไฟล์ที่เกี่ยวข้อง

```
src/main/java/th/go/etda/sarabun/pdf/service/pdf/PdfGeneratorBase.java
src/main/java/th/go/etda/sarabun/pdf/service/pdf/TableRenderer.java
```

---

## 🔴 เส้นแดง (Red Border) - MARGINS

### ความหมาย

เส้นกรอบสี่เหลี่ยมสีแดงแสดง **ขอบเขตพื้นที่เนื้อหา** (Content Area)

### ค่าที่เกี่ยวข้อง

| ค่า             | ค่าปัจจุบัน | คำอธิบาย              |
| --------------- | ----------- | --------------------- |
| `MARGIN_TOP`    | 70f         | ระยะห่างจากขอบบน      |
| `MARGIN_BOTTOM` | 70f         | ระยะห่างจากขอบล่าง    |
| `MARGIN_LEFT`   | 70f         | ระยะห่างจากขอบซ้าย    |
| `MARGIN_RIGHT`  | 70f         | ระยะห่างจากขอบขวา     |
| `PAGE_WIDTH`    | 595f        | ความกว้างหน้า A4 (pt) |
| `PAGE_HEIGHT`   | 842f        | ความสูงหน้า A4 (pt)   |

### วิธีปรับ

```
┌──────────────────────────────────────┐
│           MARGIN_TOP = 70f           │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│M │                                │M │
│A │      Content Area              │A │
│R │      (455 x 702 pt)            │R │
│G │                                │G │
│I │                                │I │
│N │                                │N │
│  │                                │  │
│L │                                │R │
│E │                                │I │
│F │                                │G │
│T │                                │H │
│  │                                │T │
│= │                                │= │
│7 │                                │7 │
│0 │                                │0 │
│  └────────────────────────────────┘  │
│          MARGIN_BOTTOM = 70f         │
└──────────────────────────────────────┘
```

#### ปรับขอบซ้ายเข้ามา (เนื้อหาเลื่อนขวา)

```java
// ก่อน
protected static final float MARGIN_LEFT = 70f;

// หลัง - ขอบซ้ายห่างขึ้น เนื้อหาเลื่อนขวา
protected static final float MARGIN_LEFT = 90f;
```

#### ปรับขอบบนลง (เนื้อหาเริ่มต่ำลง)

```java
// ก่อน
protected static final float MARGIN_TOP = 70f;

// หลัง - ขอบบนห่างขึ้น เนื้อหาเริ่มต่ำลง
protected static final float MARGIN_TOP = 100f;
```

---

## 🟠 เส้นปะสีส้ม (Orange Dashed Line) - MIN_Y_POSITION

### ความหมาย

เส้นปะแนวนอนสีส้มแสดง **จุดที่ระบบจะขึ้นหน้าใหม่** เมื่อเนื้อหาลงมาถึงจุดนี้

### ค่าที่เกี่ยวข้อง

| ค่า              | สูตร                | ค่าปัจจุบัน | คำอธิบาย                        |
| ---------------- | ------------------- | ----------- | ------------------------------- |
| `MIN_Y_POSITION` | MARGIN_BOTTOM + 150 | 220f        | ระยะจากขอบล่างที่จะขึ้นหน้าใหม่ |

### วิธีปรับ

```
                    Page (842pt)
┌──────────────────────────────────────┐
│                                      │ ← PAGE_HEIGHT - MARGIN_TOP
│           เนื้อหาเอกสาร               │
│                                      │
│                                      │
│                                      │
│                                      │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │ ← MIN_Y_POSITION (เส้นส้ม)
│                                      │    ถ้าเนื้อหาลงมาถึงจุดนี้ = ขึ้นหน้าใหม่
│                                      │
│           (พื้นที่ว่าง)               │
└──────────────────────────────────────┘ ← MARGIN_BOTTOM
```

#### ให้ขึ้นหน้าใหม่ช้าลง (เส้นส้มลงมาอีก = เขียนได้มากขึ้น)

```java
// ก่อน - ขึ้นหน้าใหม่ที่ 220pt จากขอบล่าง
protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 150;

// หลัง - ขึ้นหน้าใหม่ที่ 170pt จากขอบล่าง (เขียนได้มากขึ้น 50pt)
protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 100;

// หลัง - ขึ้นหน้าใหม่ที่ 120pt จากขอบล่าง (เขียนได้มากขึ้น 100pt)
protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 50;
```

#### ให้ขึ้นหน้าใหม่เร็วขึ้น (เส้นส้มขึ้นมาอีก = เขียนได้น้อยลง)

```java
// ก่อน
protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 150;

// หลัง - ขึ้นหน้าใหม่ที่ 270pt จากขอบล่าง (เขียนได้น้อยลง 50pt)
protected static final float MIN_Y_POSITION = MARGIN_BOTTOM + 200;
```

### ตารางอ้างอิง

| ค่า                   | คำนวณ    | ระยะจากขอบล่าง | พื้นที่เขียน |
| --------------------- | -------- | -------------- | ------------ |
| `MARGIN_BOTTOM + 200` | 70 + 200 | 270pt          | น้อยสุด      |
| `MARGIN_BOTTOM + 150` | 70 + 150 | **220pt**      | ปัจจุบัน     |
| `MARGIN_BOTTOM + 100` | 70 + 100 | 170pt          | มากขึ้น 50pt |
| `MARGIN_BOTTOM + 80`  | 70 + 80  | 150pt          | มากขึ้น 70pt |
| `MARGIN_BOTTOM + 50`  | 70 + 50  | 120pt          | มากสุด       |

---

## 🔵 เส้นน้ำเงิน (Blue Line) - PAGE_NUMBER_Y_OFFSET

### ความหมาย

เส้นแนวนอนสีน้ำเงินแสดง **ตำแหน่งแสดงเลขหน้า** (- ๒ -)

### ค่าที่เกี่ยวข้อง

| ค่า                    | ค่าปัจจุบัน | คำอธิบาย                      |
| ---------------------- | ----------- | ----------------------------- |
| `PAGE_NUMBER_Y_OFFSET` | 40f         | ระยะห่างจาก MARGIN_TOP ขึ้นไป |

### สูตรคำนวณ

```
ตำแหน่งเลขหน้า Y = PAGE_HEIGHT - MARGIN_TOP + PAGE_NUMBER_Y_OFFSET
                 = 842 - 70 + 40
                 = 812pt จากขอบล่าง
                 = 30pt จากขอบบน
```

### วิธีปรับ

```
┌──────────────────────────────────────┐ ← PAGE_HEIGHT (842pt)
│                  ↑                   │
│    PAGE_NUMBER_Y_OFFSET (40pt)       │
│                  ↓                   │
│ ─────────── - ๒ - ──────────────────│ ← เส้นน้ำเงิน (ตำแหน่งเลขหน้า)
│                  ↑                   │
│             MARGIN_TOP               │
│                  ↓                   │
├──────────────────────────────────────┤ ← PAGE_HEIGHT - MARGIN_TOP
│                                      │
│           เนื้อหาเอกสาร               │
│                                      │
```

#### เลขหน้าชิดขอบบนมากขึ้น (ขึ้นไปอีก)

```java
// ก่อน - 30pt จากขอบบน
protected static final float PAGE_NUMBER_Y_OFFSET = 40f;

// หลัง - 20pt จากขอบบน (ชิดขอบบนมากขึ้น)
protected static final float PAGE_NUMBER_Y_OFFSET = 50f;
```

#### เลขหน้าห่างจากขอบบน (ลงมาอีก)

```java
// ก่อน - 30pt จากขอบบน
protected static final float PAGE_NUMBER_Y_OFFSET = 40f;

// หลัง - 45pt จากขอบบน (ลงมาอีก)
protected static final float PAGE_NUMBER_Y_OFFSET = 25f;
```

### ตารางอ้างอิง

| PAGE_NUMBER_Y_OFFSET | ระยะจากขอบบน | หมายเหตุ     |
| -------------------- | ------------ | ------------ |
| 55f                  | 15pt         | ชิดขอบบนมาก  |
| 50f                  | 20pt         | ชิดขอบบน     |
| **40f**              | **30pt**     | **ปัจจุบัน** |
| 30f                  | 40pt         | กลางๆ        |
| 20f                  | 50pt         | ลงมามาก      |
| 15f                  | 55pt         | ค่าเดิม      |

---

## ⚙️ Debug Mode - เปิด/ปิดเส้น Debug

### ค่าที่เกี่ยวข้อง

```java
protected static final boolean ENABLE_DEBUG_BORDERS = true;  // เปิด
protected static final boolean ENABLE_DEBUG_BORDERS = false; // ปิด
```

### เปิด Debug (สำหรับ Development)

```java
protected static final boolean ENABLE_DEBUG_BORDERS = true;
```

**ผลลัพธ์**: แสดงเส้นแดง, ส้ม, น้ำเงิน

### ปิด Debug (สำหรับ Production)

```java
protected static final boolean ENABLE_DEBUG_BORDERS = false;
```

**ผลลัพธ์**: ไม่แสดงเส้นใดๆ

---

## 📐 สรุปภาพรวม

```
┌────────────────────────────────────────────────────────────┐
│                         A4 Page                            │
│                      (595 x 842 pt)                        │
│                                                            │
│  ↑ 30pt                                                    │
│  ├────────────────── - ๒ - ────────────────────┤ 🔵 เลขหน้า│
│  ↓ MARGIN_TOP = 70pt                                       │
│  ┌────────────────────────────────────────────┐            │
│  │                                            │            │
│  │              Content Area                  │            │
│  │              (455 x 702 pt)                │ 🔴 Margins │
│  │                                            │            │
│  │                                            │            │
│  ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤ 🟠 MIN_Y   │
│  │                                            │    (220pt) │
│  │         (พื้นที่สำรอง 150pt)               │            │
│  │                                            │            │
│  └────────────────────────────────────────────┘            │
│  ↑ MARGIN_BOTTOM = 70pt                                    │
└────────────────────────────────────────────────────────────┘
```

---

## 🔄 ต้อง Sync กับ TableRenderer

⚠️ **สำคัญ**: เมื่อแก้ไขค่าใน `PdfGeneratorBase.java` ต้องแก้ไขค่าใน `TableRenderer.java` ด้วย

| ค่า                    | PdfGeneratorBase | TableRenderer |
| ---------------------- | ---------------- | ------------- |
| `PAGE_NUMBER_Y_OFFSET` | ✅ ต้องตรงกัน    | ✅ ต้องตรงกัน |
| `MARGIN_TOP`           | ✅ ต้องตรงกัน    | ✅ ต้องตรงกัน |

---

## 🚀 หลังแก้ไขต้อง Rebuild

```bash
docker stop sarabun-pdf-api
docker rm sarabun-pdf-api
docker build -t sarabun-pdf-api:1.0.0 .
docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

---

_Last Updated: 20 มกราคม 2569_
