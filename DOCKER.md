# 🐳 Docker Deployment Guide

## Sarabun PDF API - Docker Deployment

> **OS:** Linux (Alpine) | **JRE:** Eclipse Temurin 17 | **Port:** 8889

---

## 🚀 Quick Start (Copy & Paste ได้เลย!)

### **Windows PowerShell:**

```powershell
# รันครั้งแรก (Build + Run)
docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

### **Windows CMD:**

```cmd
docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

### **Linux / Mac:**

```bash
docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

---

## 🔄 Rebuild (อัพเดทโค้ดใหม่)

### **Windows PowerShell:**

```powershell
# หยุด → ลบ → Build ใหม่ → รัน
docker stop sarabun-pdf-api; docker rm sarabun-pdf-api; docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

### **Windows CMD:**

```cmd
docker stop sarabun-pdf-api && docker rm sarabun-pdf-api && docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

### **Linux / Mac:**

```bash
docker stop sarabun-pdf-api && docker rm sarabun-pdf-api && docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 sarabun-pdf-api:1.0.0
```

### **หรือใช้ Script (แนะนำ):**

```powershell
# Windows PowerShell
.\docker-rebuild.ps1
```

---

## ⚙️ คำสั่งพื้นฐาน

| คำสั่ง                               | คำอธิบาย                 |
| ------------------------------------ | ------------------------ |
| `docker ps`                          | ดู container ที่กำลังรัน |
| `docker logs sarabun-pdf-api`        | ดู logs                  |
| `docker logs -f sarabun-pdf-api`     | ดู logs แบบ realtime     |
| `docker stop sarabun-pdf-api`        | หยุด container           |
| `docker start sarabun-pdf-api`       | เริ่ม container          |
| `docker restart sarabun-pdf-api`     | restart container        |
| `docker rm sarabun-pdf-api`          | ลบ container             |
| `docker exec -it sarabun-pdf-api sh` | เข้าไปใน container       |

---

## 🌐 URLs หลัง Deploy

| URL                                       | คำอธิบาย            |
| ----------------------------------------- | ------------------- |
| http://localhost:8889/actuator/health     | ✅ Health Check     |
| http://localhost:8889/swagger-ui.html     | 📖 Swagger UI       |
| http://localhost:8889/api-tester.html     | 🧪 API Tester       |
| http://localhost:8889/api/v1/pdf/generate | 📄 Generate PDF API |

---

## 🔍 ทดสอบ API

### Health Check:

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8889/actuator/health"

# หรือ curl
curl http://localhost:8889/actuator/health
```

### Generate PDF:

```powershell
$body = @'
{
  "bookNameId": "BB4A2F11-722D-449A-BCC5-22208C7A4DEC",
  "bookTitle": "ทดสอบระบบ",
  "dateThai": "13 มกราคม พ.ศ. 2569",
  "divisionName": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์",
  "bookContent": [{ "bookContent": "<p>เนื้อหาทดสอบ</p>" }],
  "bookSigned": [{ "positionName": "ผู้อำนวยการ", "prefixName": "นาย", "firstname": "ทดสอบ", "lastname": "ระบบ" }]
}
'@
Invoke-RestMethod -Uri "http://localhost:8889/api/v1/pdf/generate" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
```

---

## 📁 ไฟล์ Configuration

| ไฟล์                 | คำอธิบาย                                |
| -------------------- | --------------------------------------- |
| `.env`               | ⚙️ Config หลัก (PORT, IMAGE_NAME, etc.) |
| `Dockerfile`         | 🐳 Docker build instructions            |
| `docker-compose.yml` | 🚢 Docker Compose config                |
| `docker-rebuild.ps1` | 🔄 Script rebuild (Windows PowerShell)  |
| `docker-rebuild.sh`  | 🔄 Script rebuild (Linux/Mac)           |

---

## 🔧 เปลี่ยน Port

แก้ไขไฟล์ `.env`:

```properties
HOST_PORT=9000    # เปลี่ยนจาก 8889 เป็น 9000
```

แล้วรัน rebuild:

```powershell
.\docker-rebuild.ps1
```

---

## 📊 ตรวจสอบ Resource

```powershell
# ดู CPU/Memory usage
docker stats sarabun-pdf-api

# ดูขนาด image
docker images sarabun-pdf-api
```

---

## ❌ Troubleshooting

### 1. Container ไม่ start

```powershell
docker logs sarabun-pdf-api
```

### 2. Port ถูกใช้งานอยู่แล้ว

```powershell
# ดู process ที่ใช้ port
netstat -ano | findstr :8889

# หรือเปลี่ยน port ใน .env
HOST_PORT=9000
```

### 3. Build ช้า / Cache error

```powershell
# Build โดยไม่ใช้ cache
docker build --no-cache -t sarabun-pdf-api:1.0.0 .
```

### 4. ลบ container และ image ทั้งหมด (reset)

```powershell
docker stop sarabun-pdf-api; docker rm sarabun-pdf-api; docker rmi sarabun-pdf-api:1.0.0
```

---

## 📋 System Requirements

-   Docker 20.10+
-   Docker Desktop (Windows/Mac) หรือ Docker Engine (Linux)
-   RAM: 512MB minimum (1GB recommended)
-   Disk: 500MB สำหรับ image

---

## 🏷️ bookNameId ตามประเภทหนังสือ

| ประเภท                       | bookNameId                             |
| ---------------------------- | -------------------------------------- |
| บันทึกข้อความ (Memo)         | `BB4A2F11-722D-449A-BCC5-22208C7A4DEC` |
| หนังสือส่งออก (Outbound)     | `90F72F0E-528D-4992-907A-F2C6B37AD9A5` |
| หนังสือประกาศ (Announcement) | `23065068-BB18-49EA-8CE7-22945E16CB6D` |
| หนังสือคำสั่ง (Order)        | `3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D` |
| หนังสือระเบียบ (Regulation)  | `50792880-F85A-4343-9672-7B61AF828A5B` |
| หนังสือข้อบังคับ (Rule)      | `4AB1EC00-9E5E-4113-B577-D8ED46BA7728` |
