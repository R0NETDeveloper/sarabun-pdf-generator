# README: Thai Font Files

โฟลเดอร์นี้สำหรับเก็บฟอนต์ภาษาไทย (TH Sarabun New)

## ไฟล์ที่ต้องการ:
1. `THSarabunNew.ttf` - ฟอนต์ธรรมดา
2. `THSarabunNew-Bold.ttf` - ฟอนต์ตัวหนา

## วิธีการเพิ่มฟอนต์:

### Option 1: คัดลอกจากโปรเจค .NET เดิม
```bash
copy ..\sarabun-multitenant-pdf\ETDA.SarabunMultitenant.Api\Fonts\*.ttf .\src\main\resources\fonts\
```

### Option 2: ดาวน์โหลดจาก Google Fonts
1. ไปที่ https://fonts.google.com/specimen/Sarabun
2. ดาวน์โหลดฟอนต์
3. คัดลอกไฟล์ .ttf มาไว้ที่นี่

### Option 3: ใช้ฟอนต์จากระบบ
PDFBox สามารถใช้ฟอนต์จากระบบได้ แต่อาจไม่แสดงภาษาไทยได้ดีบน Linux

## สำคัญ:
- ฟอนต์ TH Sarabun New เป็นฟอนต์ฟรีจาก SIPA (สำนักงานนโยบายและแผนการศึกษาไทย)
- สามารถใช้ได้ฟรีตาม GNU General Public License
