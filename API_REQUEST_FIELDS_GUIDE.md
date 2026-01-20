# 📋 PDF API Request Fields Guide
## คู่มือการใช้งาน JSON Request สำหรับ PDF Generation API

---

## 📌 สารบัญ
1. [โครงสร้าง JSON Request](#โครงสร้าง-json-request)
2. [บันทึกข้อความ (Memo)](#1-บันทึกข้อความ-memo)
3. [หนังสือส่งออก (Outbound)](#2-หนังสือส่งออก-outbound)
4. [หนังสือประทับตรา (Stamp)](#3-หนังสือประทับตรา-stamp)
5. [หนังสือกระทรวง (Ministry)](#4-หนังสือกระทรวง-ministry)
6. [หนังสือประกาศ (Announcement)](#5-หนังสือประกาศ-announcement)
7. [หนังสือระเบียบ (Regulation)](#6-หนังสือระเบียบ-regulation)
8. [หนังสือคำสั่ง (Order)](#7-หนังสือคำสั่ง-order)
9. [หนังสือข้อบังคับ (Rule)](#8-หนังสือข้อบังคับ-rule)
10. [สรุป Field ทั้งหมด](#สรุป-field-ทั้งหมด)

---

## โครงสร้าง JSON Request

```json
{
  "bookNameId": "GUID ประเภทเอกสาร",
  "documentMain": { ... },
  "documentSub": { ... },
  "bookSigned": [ ... ],
  "bookSubmited": [ ... ],
  "bookLearner": [ ... ],
  "toRecipients": [ ... ]
}
```

### BookNameId (รหัสประเภทเอกสาร) ⭐ **จำเป็น**

| ประเภทเอกสาร | BookNameId |
|-------------|------------|
| บันทึกข้อความ (Memo) | `BB4A2F11-722D-449A-BCC5-22208C7A4DEC` |
| หนังสือส่งออก (Outbound) | `90F72F0E-528D-4992-907A-F2C6B37AD9A5` |
| หนังสือประทับตรา (Stamp) | `AF3E7697-6F7E-4AD8-B76C-E2134DB98747` |
| หนังสือกระทรวง (Ministry) | `4B3EB169-6203-4A71-A3BD-A442FEAAA91F` |
| หนังสือประกาศ (Announcement) | `23065068-BB18-49EA-8CE7-22945E16CB6D` |
| หนังสือระเบียบ (Regulation) | `50792880-F85A-4343-9672-7B61AF828A5B` |
| หนังสือคำสั่ง (Order) | `3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D` |
| หนังสือข้อบังคับ (Rule) | `4AB1EC00-9E5E-4113-B577-D8ED46BA7728` |

---

## 1. บันทึกข้อความ (Memo)
**BookNameId:** `BB4A2F11-722D-449A-BCC5-22208C7A4DEC`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│  [LOGO]              บันทึกข้อความ                       │
├─────────────────────────────────────────────────────────┤
│  ส่วนราชการ: {documentMain.divisionName}                │
│  ที่: {documentMain.bookNo}    วันที่: {documentMain.dateThai} │
│  เรื่อง: {documentMain.bookTitle}                       │
│  ─────────────────────────────────────────              │
│  เรียน: {bookLearner[].positionName}                    │
│                                                         │
│         {documentMain.bookContent.content}              │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    │ {bookSigned[0].  │                  │
│                    │  firstname lastname}│               │
│                    │ {positionName}   │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentMain.divisionName` | ส่วนราชการ | ✅ | ชื่อหน่วยงาน/ส่วนราชการ |
| `documentMain.department` | ส่วนราชการ (fallback) | ❌ | ใช้ถ้าไม่มี divisionName |
| `documentMain.bookNo` | ที่ (เลขที่หนังสือ) | ✅ | เช่น "สพธอ. 0101/ว 2568" (แปลงเป็นเลขไทย) |
| `documentMain.dateThai` | วันที่ | ✅ | เช่น "8 มกราคม พ.ศ. 2569" (แปลงเป็นเลขไทย) |
| `documentMain.bookTitle` | เรื่อง | ✅ | หัวข้อเรื่องของหนังสือ |
| `documentMain.speedLayer` | ชั้นความเร็ว | ❌ | เช่น "ด่วนที่สุด" (แสดงมุมขวาบน) |
| `documentMain.bookContent.content` | เนื้อหา | ✅ | เนื้อความหลักของหนังสือ |
| `documentMain.bookContent.contentType` | ประเภทเนื้อหา | ❌ | "text" หรือ "html" (default: text) |
| `bookLearner[].positionName` | เรียน | ✅ | ตำแหน่งผู้รับ (แสดงหลังคำว่า "เรียน") |
| `bookSigned[].prefixName` | ช่องลงนาม | ✅ | คำนำหน้า (เช่น "นาย") |
| `bookSigned[].firstname` | ช่องลงนาม | ✅ | ชื่อ |
| `bookSigned[].lastname` | ช่องลงนาม | ✅ | นามสกุล |
| `bookSigned[].positionName` | ช่องลงนาม | ✅ | ตำแหน่ง |
| `bookSigned[].signatureBase64` | ลายเซ็น | ❌ | รูปลายเซ็น Base64 (ถ้ามี) |

### ไฟล์ที่สร้าง (Generate Endpoint)
- **1 ไฟล์**: บันทึกข้อความ (รวม Submit + Learner pages)

---

## 2. หนังสือส่งออก (Outbound)
**BookNameId:** `90F72F0E-528D-4992-907A-F2C6B37AD9A5`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                                          [LOGO]  [ชั้นความเร็ว]│
│  ที่: {documentSub.bookNo}                               │
│  {documentSub.address}                                   │
│  {documentSub.dateThai}                                  │
├─────────────────────────────────────────────────────────┤
│  เรื่อง: {documentSub.bookTitle}                        │
│  ─────────────────────────────────────────              │
│  เรียน: {toRecipients[].salutationContent}              │
│  อ้างถึง: {documentSub.bookReferTo[].bookReferToName}   │
│  สิ่งที่ส่งมาด้วย: {documentSub.attachment[].name}      │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│                    {toRecipients[].endDoc}              │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    │ {bookSigned[0]}  │                  │
│                    └─────────────────┘                  │
│  {documentSub.contact}                                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentSub.bookNo` | ที่ (เลขที่หนังสือ) | ✅ | เลขที่หนังสือส่งออก |
| `documentSub.address` | ที่อยู่หน่วยงาน | ❌ | ที่อยู่ผู้ส่ง (บรรทัดที่ 2) |
| `documentSub.dateThai` | วันที่ | ✅ | วันที่ภาษาไทย |
| `documentSub.bookTitle` | เรื่อง | ✅ | หัวข้อเรื่อง |
| `documentSub.speedLayer` | ชั้นความเร็ว | ❌ | เช่น "ด่วนที่สุด" |
| `documentSub.contact` | ข้อมูลติดต่อ | ❌ | แสดงท้ายเอกสาร |
| `documentSub.bookContent.subject` | หัวข้อ | ❌ | (ไม่ใช้ใน Outbound) |
| `documentSub.bookContent.content` | เนื้อหา | ✅ | เนื้อความหลัก |
| `documentSub.bookContent.contentType` | ประเภท | ❌ | "text" หรือ "html" |
| `documentSub.bookReferTo[].bookReferToName` | อ้างถึง | ❌ | รายการอ้างถึง |
| `documentSub.attachment[].name` | สิ่งที่ส่งมาด้วย | ❌ | รายการสิ่งที่ส่งมาด้วย |
| `documentSub.attachment[].remark` | หมายเหตุ | ❌ | เช่น "จำนวน 1 ฉบับ" |
| `toRecipients[].salutation` | คำขึ้นต้น | ❌ | เช่น "เรียน", "กราบเรียน" |
| `toRecipients[].salutationContent` | เรียน | ✅ | เช่น "ท่านปลัดกระทรวง..." |
| `toRecipients[].organizeName` | ชื่อองค์กร | ❌ | ใช้สำหรับชื่อไฟล์ |
| `toRecipients[].endDoc` | คำลงท้าย | ❌ | เช่น "ขอแสดงความนับถือ" |
| `bookSigned[].*` | ช่องลงนาม | ✅ | ผู้ลงนาม |

### ไฟล์ที่สร้าง (Generate Endpoint)
- **N ไฟล์**: หนังสือส่งออก (แยกตามจำนวน toRecipients)
- **1 ไฟล์**: บันทึกข้อความ (สำเนาเก็บ + Submit + Learner)

---

## 3. หนังสือประทับตรา (Stamp)
**BookNameId:** `AF3E7697-6F7E-4AD8-B76C-E2134DB98747`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                                          [LOGO]         │
│  ที่: {documentSub.bookNo}                               │
│  ถึง: {toRecipients[].salutationContent}                │
├─────────────────────────────────────────────────────────┤
│         {documentSub.bookContent.content}               │
│                                                         │
│                    ┌───────────────────┐                │
│                    │   [ตราประทับ]      │                │
│                    │   {department}     │                │
│                    │   {dateThai}       │                │
│                    └───────────────────┘                │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentSub.bookNo` | ที่ (เลขที่หนังสือ) | ✅ | เลขที่หนังสือ |
| `documentSub.dateThai` | วันที่ (ในตรา) | ✅ | แสดงในวงกลมตราประทับ |
| `documentSub.bookContent.content` | เนื้อหา | ✅ | เนื้อความหลัก |
| `toRecipients[].salutationContent` | ถึง | ✅ | ผู้รับหนังสือ |
| `documentMain.department` | ชื่อหน่วยงาน (ในตรา) | ✅ | แสดงในวงกลมตราประทับ |

### ไฟล์ที่สร้าง (Generate Endpoint)
- **N ไฟล์**: หนังสือประทับตรา (แยกตามจำนวน toRecipients)
- **1 ไฟล์**: บันทึกข้อความ (สำเนาเก็บ + Submit + Learner)

---

## 4. หนังสือกระทรวง (Ministry)
**BookNameId:** `4B3EB169-6203-4A71-A3BD-A442FEAAA91F`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                                          [LOGO]         │
│  ที่: {documentSub.bookNo}                               │
│  {documentSub.address}                                   │
│  {documentSub.dateThai}                                  │
├─────────────────────────────────────────────────────────┤
│  เรื่อง: {documentSub.bookTitle}                        │
│  ─────────────────────────────────────────              │
│  เรียน: {toRecipients[].salutationContent}              │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│                    {toRecipients[].endDoc}              │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    │ {bookSigned[0]}  │                  │
│                    │ ปลัดกระทรวง...    │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้
(เหมือนกับ Outbound แต่ใช้สำหรับหนังสือระดับกระทรวง)

| Field | ตำแหน่งในเอกสาร | จำเป็น |
|-------|----------------|--------|
| `documentSub.bookNo` | ที่ | ✅ |
| `documentSub.address` | ที่อยู่ | ❌ |
| `documentSub.dateThai` | วันที่ | ✅ |
| `documentSub.bookTitle` | เรื่อง | ✅ |
| `documentSub.bookContent.content` | เนื้อหา | ✅ |
| `toRecipients[].salutationContent` | เรียน | ✅ |
| `toRecipients[].endDoc` | คำลงท้าย | ❌ |
| `bookSigned[].*` | ช่องลงนาม | ✅ |

---

## 5. หนังสือประกาศ (Announcement)
**BookNameId:** `23065068-BB18-49EA-8CE7-22945E16CB6D`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                      [LOGO]                             │
│                                                         │
│                 ประกาศ{documentSub.department}          │
│                 เรื่อง {documentSub.bookTitle}          │
│                 ────────────────────                    │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│                 ประกาศ ณ วันที่ {documentSub.dateThai}  │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    │ {bookLearner[0]} │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentSub.department` | หัวข้อ (ประกาศ...) | ✅ | ต่อท้าย "ประกาศ" |
| `documentSub.bookTitle` | เรื่อง | ✅ | หัวข้อเรื่องประกาศ |
| `documentSub.dateThai` | ประกาศ ณ วันที่ | ✅ | วันที่ประกาศ |
| `documentSub.bookContent.subject` | หัวเรื่อง | ❌ | แสดงหลัง "เรื่อง" |
| `documentSub.bookContent.content` | เนื้อหา | ✅ | เนื้อความประกาศ |
| `bookLearner[].*` | ช่องลงนาม | ✅ | ผู้ลงนามในประกาศ |

### ไฟล์ที่สร้าง (Generate Endpoint)
- **1 ไฟล์**: หนังสือประกาศ
- **1 ไฟล์**: บันทึกข้อความ (สำเนาเก็บ + Submit + Learner)

---

## 6. หนังสือระเบียบ (Regulation)
**BookNameId:** `50792880-F85A-4343-9672-7B61AF828A5B`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                      [LOGO]                             │
│                                                         │
│              ระเบียบ{documentSub.department}            │
│              ว่าด้วย {documentSub.bookTitle}            │
│              ฉบับที่ {edition}                          │
│              พ.ศ. {documentSub.year}                    │
│              ────────────────────                       │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│              ประกาศ ณ วันที่ {documentSub.dateThai}     │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentSub.department` | หัวข้อ (ระเบียบ...) | ✅ | ต่อท้าย "ระเบียบ" |
| `documentSub.bookTitle` | ว่าด้วย | ✅ | หัวข้อเรื่องระเบียบ |
| `documentSub.bookNo` | ฉบับที่ | ❌ | ดึงเลขฉบับจาก bookNo |
| `documentSub.year` | พ.ศ. | ❌ | ปี พ.ศ. |
| `documentSub.dateThai` | ประกาศ ณ วันที่ | ✅ | วันที่ประกาศ |
| `documentSub.bookContent.subject` | หัวเรื่อง | ❌ | แสดงหลัง "ว่าด้วย" |
| `documentSub.bookContent.content` | เนื้อหา | ✅ | เนื้อความระเบียบ |
| `bookLearner[].*` | ช่องลงนาม | ✅ | ผู้ลงนาม |

---

## 7. หนังสือคำสั่ง (Order)
**BookNameId:** `3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                      [LOGO]                             │
│                                                         │
│              คำสั่ง{documentSub.department}             │
│              ที่ {documentSub.bookNo}                   │
│              เรื่อง {documentSub.bookTitle}             │
│              ────────────────────                       │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│              สั่ง ณ วันที่ {documentSub.dateThai}       │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้

| Field | ตำแหน่งในเอกสาร | จำเป็น | หมายเหตุ |
|-------|----------------|--------|----------|
| `documentSub.department` | หัวข้อ (คำสั่ง...) | ✅ | ต่อท้าย "คำสั่ง" |
| `documentSub.bookNo` | ที่ | ✅ | เลขที่คำสั่ง เช่น "ที่ 1/2569" |
| `documentSub.bookTitle` | เรื่อง | ✅ | หัวข้อเรื่อง |
| `documentSub.dateThai` | สั่ง ณ วันที่ | ✅ | วันที่สั่ง |
| `documentSub.bookContent.subject` | หัวเรื่อง | ❌ | แสดงหลัง "เรื่อง" |
| `documentSub.bookContent.content` | เนื้อหา | ✅ | เนื้อความคำสั่ง |
| `bookLearner[].*` | ช่องลงนาม | ✅ | ผู้ออกคำสั่ง |

---

## 8. หนังสือข้อบังคับ (Rule)
**BookNameId:** `4AB1EC00-9E5E-4113-B577-D8ED46BA7728`

### ตัวอย่างเอกสาร
```
┌─────────────────────────────────────────────────────────┐
│                      [LOGO]                             │
│                                                         │
│            ข้อบังคับ{documentSub.department}            │
│            ว่าด้วย {documentSub.bookTitle}              │
│            ฉบับที่ {edition}                            │
│            พ.ศ. {documentSub.year}                      │
│            ────────────────────                         │
│                                                         │
│         {documentSub.bookContent.content}               │
│                                                         │
│            ประกาศ ณ วันที่ {documentSub.dateThai}       │
│                                                         │
│                    ┌─────────────────┐                  │
│                    │ [ช่องลงนาม]      │                  │
│                    └─────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### Fields ที่ใช้
(เหมือนกับ Regulation)

| Field | ตำแหน่งในเอกสาร | จำเป็น |
|-------|----------------|--------|
| `documentSub.department` | หัวข้อ (ข้อบังคับ...) | ✅ |
| `documentSub.bookTitle` | ว่าด้วย | ✅ |
| `documentSub.year` | พ.ศ. | ❌ |
| `documentSub.dateThai` | ประกาศ ณ วันที่ | ✅ |
| `documentSub.bookContent.content` | เนื้อหา | ✅ |
| `bookLearner[].*` | ช่องลงนาม | ✅ |

---

## สรุป Field ทั้งหมด

### 📦 Root Level

| Field | Type | คำอธิบาย | ใช้กับเอกสาร |
|-------|------|----------|-------------|
| `bookNameId` | String | GUID ประเภทเอกสาร (⭐ จำเป็น) | ทุกประเภท |
| `documentMain` | Object | ข้อมูลเอกสารหลัก (บันทึกข้อความ) | ทุกประเภท |
| `documentSub` | Object | ข้อมูลเอกสารรอง | ทุกประเภท (ยกเว้น Memo) |
| `bookSigned` | Array | ผู้ลงนาม | Memo, Outbound, Stamp, Ministry |
| `bookSubmited` | Array | ผู้เสนอผ่าน | ทุกประเภท (หน้า Submit) |
| `bookLearner` | Array | ผู้รับภายใน/ผู้ลงนาม | ทุกประเภท |
| `toRecipients` | Array | ผู้รับภายนอก | Outbound, Stamp, Ministry |

---

### 📄 documentMain (ข้อมูลบันทึกข้อความ)

| Field | Type | คำอธิบาย | ตำแหน่งในเอกสาร |
|-------|------|----------|----------------|
| `bookName` | String | ชื่อประเภทหนังสือ | (ไม่แสดง) |
| `bookTitle` | String | ชื่อเรื่อง | "เรื่อง:" |
| `bookNo` | String | เลขที่หนังสือ | "ที่:" (แปลงเป็นเลขไทย) |
| `dateThai` | String | วันที่ภาษาไทย | "วันที่:" (แปลงเป็นเลขไทย) |
| `department` | String | ชื่อหน่วยงาน | "ส่วนราชการ:" |
| `divisionName` | String | ชื่อส่วนงาน | "ส่วนราชการ:" (ใช้ก่อน department) |
| `address` | String | ที่อยู่ | (ใช้ใน Outbound) |
| `speedLayer` | String | ชั้นความเร็ว | มุมขวาบน (เช่น "ด่วนที่สุด") |
| `speedLayerId` | String | รหัสชั้นความเร็ว | (ไม่แสดง) |
| `formatPdf` | String | รูปแบบ PDF | (สำหรับ config) |
| `bookContent` | Object | เนื้อหาหนังสือ | ดูตาราง bookContent |

---

### 📄 documentSub (ข้อมูลเอกสารรอง)

| Field | Type | คำอธิบาย | ใช้กับเอกสาร |
|-------|------|----------|-------------|
| `bookName` | String | ชื่อประเภท | (ไม่แสดง) |
| `bookTitle` | String | ชื่อเรื่อง | "เรื่อง:" หรือ "ว่าด้วย" |
| `bookNo` | String | เลขที่หนังสือ | "ที่:" |
| `dateThai` | String | วันที่ | "วันที่:" หรือ "ประกาศ ณ วันที่" |
| `department` | String | ชื่อหน่วยงาน | หัวเอกสาร (เช่น "ประกาศสพธอ.") |
| `divisionName` | String | ชื่อส่วนงาน | (ไม่ใช้) |
| `address` | String | ที่อยู่ | บรรทัดที่ 2 (Outbound) |
| `contact` | String | ข้อมูลติดต่อ | ท้ายเอกสาร |
| `speedLayer` | String | ชั้นความเร็ว | มุมขวาบน |
| `year` | String | ปี พ.ศ. | Regulation, Rule |
| `bookContent` | Object | เนื้อหา | ดูตาราง bookContent |
| `bookReferTo` | Array | อ้างถึง | Outbound |
| `attachment` | Array | สิ่งที่ส่งมาด้วย | Outbound |

---

### 📝 bookContent (เนื้อหา)

| Field | Type | คำอธิบาย | ตัวอย่าง |
|-------|------|----------|---------|
| `subject` | String | หัวเรื่อง | "ขออนุมัติจัดซื้อ..." |
| `content` | String | เนื้อความหลัก | ข้อความหรือ HTML |
| `contentType` | String | ประเภทเนื้อหา | "text" หรือ "html" |

**ตัวอย่าง HTML content:**
```html
<p>ด้วยสำนักงาน...</p>
<table>
  <tr><th>ลำดับ</th><th>รายการ</th></tr>
  <tr><td>1</td><td>คอมพิวเตอร์</td></tr>
</table>
```

---

### 👤 bookSigned / bookSubmited / bookLearner (ผู้เกี่ยวข้อง)

| Field | Type | คำอธิบาย | ตำแหน่งในเอกสาร |
|-------|------|----------|----------------|
| `relatedNo` | String | ลำดับ | (สำหรับเรียงลำดับ) |
| `prefixName` | String | คำนำหน้า | ช่องลงนาม ("นาย", "นาง") |
| `firstname` | String | ชื่อ | ช่องลงนาม |
| `lastname` | String | นามสกุล | ช่องลงนาม |
| `positionName` | String | ตำแหน่ง | ช่องลงนาม / "เรียน" |
| `departmentName` | String | หน่วยงาน | ช่องลงนาม |
| `email` | String | อีเมล | (ไม่แสดง - สำหรับ reference) |
| `signatureBase64` | String | ลายเซ็น Base64 | ช่องลงนาม (ถ้ามี) |

**การใช้งาน:**
- `bookSigned`: ผู้ลงนามในหนังสือหลัก (Memo, Outbound, Stamp, Ministry)
- `bookSubmited`: ผู้เสนอผ่าน (แสดงในหน้า Submit แยก)
- `bookLearner`: ผู้รับภายใน (Memo: "เรียน", อื่นๆ: ผู้ลงนาม)

---

### 📬 toRecipients (ผู้รับหนังสือภายนอก)

| Field | Type | คำอธิบาย | ตำแหน่งในเอกสาร |
|-------|------|----------|----------------|
| `guid` | String | รหัสผู้รับ | (ไม่แสดง) |
| `recipientNo` | String | ลำดับ | (สำหรับเรียงลำดับ) |
| `ministryName` | String | ชื่อกระทรวง | (ไม่แสดง - สำหรับ reference) |
| `departmentName` | String | ชื่อกรม | (ไม่แสดง - สำหรับ reference) |
| `organizeName` | String | ชื่อองค์กร | ชื่อไฟล์ PDF |
| `address` | String | ที่อยู่ | (ไม่แสดง) |
| `postalCode` | String | รหัสไปรษณีย์ | (ไม่แสดง) |
| `contactName` | String | ชื่อผู้ติดต่อ | (ไม่แสดง) |
| `contactEmail` | String | อีเมล | (ไม่แสดง) |
| `salutation` | String | คำขึ้นต้น | "เรียน", "กราบเรียน" |
| `salutationContent` | String | เนื้อหาคำขึ้นต้น | "ท่านปลัดกระทรวง..." |
| `endDoc` | String | คำลงท้าย | "ขอแสดงความนับถือ" |

---

### 📎 attachment (สิ่งที่ส่งมาด้วย)

| Field | Type | คำอธิบาย |
|-------|------|----------|
| `name` | String | ชื่อรายการ |
| `remark` | String | หมายเหตุ (เช่น "จำนวน 1 ฉบับ") |

---

### 📌 bookReferTo (อ้างถึง)

| Field | Type | คำอธิบาย |
|-------|------|----------|
| `bookReferToNo` | String | เลขที่อ้างถึง |
| `bookReferToName` | String | ชื่อหนังสืออ้างถึง |
| `createDate` | DateTime | วันที่ |

---

## 🔧 ตัวอย่าง JSON Request แบบเต็ม

```json
{
  "bookNameId": "90F72F0E-528D-4992-907A-F2C6B37AD9A5",
  "documentMain": {
    "bookName": "บันทึกข้อความ",
    "bookTitle": "ขออนุมัติส่งหนังสือเชิญประชุม",
    "bookNo": "สพธอ. 0101/ว 2568",
    "dateThai": "8 มกราคม พ.ศ. 2569",
    "department": "สพธอ.",
    "divisionName": "กองพัฒนาระบบ",
    "speedLayer": "ด่วนที่สุด",
    "bookContent": {
      "subject": "ขออนุมัติ",
      "content": "ด้วยสำนักงาน...",
      "contentType": "html"
    }
  },
  "documentSub": {
    "bookName": "หนังสือส่งออก",
    "bookTitle": "ขอเชิญประชุม",
    "bookNo": "สพธอ. 0101/2568",
    "dateThai": "8 มกราคม พ.ศ. 2569",
    "department": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์",
    "address": "ศูนย์ราชการฯ อาคารบี",
    "contact": "โทร. 02-123-4567",
    "bookContent": {
      "subject": "",
      "content": "<p>ด้วยสำนักงานจะจัดประชุม...</p>",
      "contentType": "html"
    },
    "bookReferTo": [
      { "bookReferToNo": "1", "bookReferToName": "หนังสือ กค. 0101/100" }
    ],
    "attachment": [
      { "name": "กำหนดการประชุม", "remark": "1 ฉบับ" }
    ]
  },
  "bookSigned": [
    {
      "relatedNo": "1",
      "prefixName": "นาย",
      "firstname": "ประสิทธิ์",
      "lastname": "พัฒนากิจ",
      "positionName": "ผู้อำนวยการ",
      "departmentName": "สพธอ.",
      "email": "prasit@etda.or.th",
      "signatureBase64": ""
    }
  ],
  "bookSubmited": [
    {
      "relatedNo": "1",
      "prefixName": "นาย",
      "firstname": "วิชัย",
      "lastname": "เสนอผ่าน",
      "positionName": "หัวหน้าฝ่าย",
      "departmentName": "กองพัฒนา"
    }
  ],
  "bookLearner": [
    {
      "relatedNo": "1",
      "prefixName": "นาย",
      "firstname": "สมชาย",
      "lastname": "ใจดี",
      "positionName": "หัวหน้างาน",
      "departmentName": "กองพัฒนา"
    }
  ],
  "toRecipients": [
    {
      "guid": "R001",
      "recipientNo": "1",
      "ministryName": "กระทรวงดิจิทัลฯ",
      "departmentName": "สำนักงานปลัด",
      "organizeName": "กระทรวงดิจิทัลฯ",
      "address": "ศูนย์ราชการฯ",
      "postalCode": "10210",
      "contactName": "ปลัดกระทรวง",
      "contactEmail": "contact@mdes.go.th",
      "salutation": "เรียน",
      "salutationContent": "ท่านปลัดกระทรวงดิจิทัลฯ",
      "endDoc": "ขอแสดงความนับถือ"
    }
  ]
}
```

---

## 📊 สรุปความจำเป็นของ Field ตามประเภทเอกสาร

| Field | Memo | Outbound | Stamp | Ministry | ประกาศ | ระเบียบ | คำสั่ง | ข้อบังคับ |
|-------|:----:|:--------:|:-----:|:--------:|:------:|:------:|:-----:|:--------:|
| bookNameId | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| documentMain | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| documentSub | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| bookSigned | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| bookSubmited | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| bookLearner | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| toRecipients | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |

**หมายเหตุ:**
- ✅ = จำเป็น/ใช้งานหลัก
- ❌ = ไม่จำเป็น/ไม่ใช้งาน (แต่ส่งได้)
- `bookSubmited` ใช้สำหรับสร้างหน้า "เสนอผ่าน" แยก (ทุกประเภท)

---

*สร้างโดยอัตโนมัติ - Last updated: January 2026*
