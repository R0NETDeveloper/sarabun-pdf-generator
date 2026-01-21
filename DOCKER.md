# Docker Deployment Guide

## Sarabun PDF API - Docker Deployment

> **OS:** Linux (Alpine) | **JRE:** Eclipse Temurin 17 | **Port:** 8889

---

## Quick Start

### Development Mode (ไม่ต้อง API Key)

**Windows PowerShell:**
```powershell
docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 -e SPRING_PROFILES_ACTIVE=dev sarabun-pdf-api:1.0.0
```

**Linux / Mac:**
```bash
docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 -e SPRING_PROFILES_ACTIVE=dev sarabun-pdf-api:1.0.0
```

### Production Mode (ต้องมี API Key)

**Windows PowerShell:**
```powershell
docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 -e API_KEY=your-secret-api-key-here sarabun-pdf-api:1.0.0
```

**Linux / Mac:**
```bash
docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 -e API_KEY=your-secret-api-key-here sarabun-pdf-api:1.0.0
```

---

## API Key Authentication

### สร้าง API Key

```bash
# Linux / Mac / Git Bash
openssl rand -hex 32

# ผลลัพธ์ตัวอย่าง: 7f3a9c2b8e4d1f6a5b0c9e8d7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a
```

### โหมดการทำงาน

| Mode | Environment Variable | API Key |
|------|---------------------|---------|
| **Development** | `-e SPRING_PROFILES_ACTIVE=dev` | ไม่ต้อง |
| **Production** | `-e API_KEY=xxx` | ต้องใส่ใน Header |

### ใช้ API Key ใน Request

```bash
# Production mode - ต้องใส่ X-API-Key header
curl -H "X-API-Key: your-secret-api-key-here" \
  http://localhost:8889/api/pdf/health

# Development mode - ไม่ต้องใส่
curl http://localhost:8889/api/pdf/health
```

### Endpoints ที่ไม่ต้อง API Key (Public)

| Endpoint | คำอธิบาย |
|----------|----------|
| `/actuator/**` | Health check |
| `/swagger-ui/**` | API documentation |
| `/api-docs/**` | OpenAPI spec |
| `/api-tester.html` | API tester tool |

---

## Rebuild (อัพเดทโค้ดใหม่)

### Development Mode

**Windows PowerShell:**
```powershell
docker stop sarabun-pdf-api; docker rm sarabun-pdf-api; docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 -e SPRING_PROFILES_ACTIVE=dev sarabun-pdf-api:1.0.0
```

**Linux / Mac:**
```bash
docker stop sarabun-pdf-api && docker rm sarabun-pdf-api && docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 -e SPRING_PROFILES_ACTIVE=dev sarabun-pdf-api:1.0.0
```

### Production Mode

**Windows PowerShell:**
```powershell
docker stop sarabun-pdf-api; docker rm sarabun-pdf-api; docker build -t sarabun-pdf-api:1.0.0 .; docker run -d --name sarabun-pdf-api -p 8889:8888 -e API_KEY=your-secret-api-key-here sarabun-pdf-api:1.0.0
```

**Linux / Mac:**
```bash
docker stop sarabun-pdf-api && docker rm sarabun-pdf-api && docker build -t sarabun-pdf-api:1.0.0 . && docker run -d --name sarabun-pdf-api -p 8889:8888 -e API_KEY=your-secret-api-key-here sarabun-pdf-api:1.0.0
```

---

## Docker Compose

### Development

```bash
# แก้ไข .env
API_SECURITY_ENABLED=false
SPRING_PROFILE=dev

# รัน
docker-compose up -d
```

### Production

```bash
# แก้ไข .env
API_SECURITY_ENABLED=true
SPRING_PROFILE=prod
API_KEY=your-secret-api-key-here

# รัน
docker-compose up -d
```

---

## คำสั่งพื้นฐาน

| คำสั่ง | คำอธิบาย |
|--------|----------|
| `docker ps` | ดู container ที่กำลังรัน |
| `docker logs sarabun-pdf-api` | ดู logs |
| `docker logs -f sarabun-pdf-api` | ดู logs แบบ realtime |
| `docker stop sarabun-pdf-api` | หยุด container |
| `docker start sarabun-pdf-api` | เริ่ม container |
| `docker restart sarabun-pdf-api` | restart container |
| `docker rm sarabun-pdf-api` | ลบ container |
| `docker exec -it sarabun-pdf-api sh` | เข้าไปใน container |

---

## URLs หลัง Deploy

| URL | คำอธิบาย | ต้อง API Key? |
|-----|----------|---------------|
| http://localhost:8889/actuator/health | Health Check | ไม่ |
| http://localhost:8889/swagger-ui.html | Swagger UI | ไม่ |
| http://localhost:8889/api-tester.html | API Tester | ไม่ |
| http://localhost:8889/api/pdf/preview | Generate PDF Preview | Production: ใช่ |
| http://localhost:8889/api/pdf/generate | Generate PDF | Production: ใช่ |

---

## ทดสอบ API

### Health Check

```bash
curl http://localhost:8889/actuator/health
```

### Generate PDF (Development)

```bash
curl -X POST http://localhost:8889/api/pdf/preview \
  -H "Content-Type: application/json" \
  -d '{
    "bookNameId": "BB4A2F11-722D-449A-BCC5-22208C7A4DEC",
    "memo": {
      "bookTitle": "ทดสอบระบบ",
      "dateThai": "13 มกราคม พ.ศ. 2569",
      "divisionName": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์"
    }
  }'
```

### Generate PDF (Production - ต้องมี API Key)

```bash
curl -X POST http://localhost:8889/api/pdf/preview \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-secret-api-key-here" \
  -d '{
    "bookNameId": "BB4A2F11-722D-449A-BCC5-22208C7A4DEC",
    "memo": {
      "bookTitle": "ทดสอบระบบ",
      "dateThai": "13 มกราคม พ.ศ. 2569",
      "divisionName": "สำนักงานพัฒนาธุรกรรมทางอิเล็กทรอนิกส์"
    }
  }'
```

---

## Configuration Files

| ไฟล์ | คำอธิบาย |
|------|----------|
| `.env` | Config หลัก (PORT, API_KEY, etc.) |
| `Dockerfile` | Docker build instructions |
| `docker-compose.yml` | Docker Compose config |
| `application.properties` | Spring Boot config |
| `application-dev.properties` | Development profile |
| `application-prod.properties` | Production profile |

---

## Environment Variables

| Variable | Default | คำอธิบาย |
|----------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Profile: `dev` หรือ `prod` |
| `API_KEY` | (empty) | API Key สำหรับ authentication |
| `API_SECURITY_ENABLED` | `true` | เปิด/ปิด API Key check |
| `SERVER_PORT` | `8888` | Port ภายใน container |
| `HOST_PORT` | `8889` | Port ที่เปิดให้ภายนอก |

---

## เปลี่ยน Port

แก้ไขไฟล์ `.env`:

```properties
HOST_PORT=9000    # เปลี่ยนจาก 8889 เป็น 9000
```

แล้วรัน rebuild

---

## Troubleshooting

### 1. Container ไม่ start (API Key error)

```bash
docker logs sarabun-pdf-api
```

ถ้าเห็น error:
```
API Security is enabled but api.key is not configured
```

**แก้ไข:** เพิ่ม `-e API_KEY=xxx` หรือใช้ `-e SPRING_PROFILES_ACTIVE=dev`

### 2. 401 Unauthorized

```json
{"success":false,"message":"Missing API Key. Please provide X-API-Key header."}
```

**แก้ไข:** เพิ่ม header `-H "X-API-Key: your-key"`

### 3. Port ถูกใช้งานอยู่แล้ว

```powershell
netstat -ano | findstr :8889
```

แก้ไข: เปลี่ยน port ใน `.env` → `HOST_PORT=9000`

### 4. Build ช้า / Cache error

```bash
docker build --no-cache -t sarabun-pdf-api:1.0.0 .
```

### 5. ลบทั้งหมด (reset)

```bash
docker stop sarabun-pdf-api; docker rm sarabun-pdf-api; docker rmi sarabun-pdf-api:1.0.0
```

---

## System Requirements

- Docker 20.10+
- Docker Desktop (Windows/Mac) หรือ Docker Engine (Linux)
- RAM: 512MB minimum (1GB recommended)
- Disk: 500MB สำหรับ image

---

## bookNameId ตามประเภทหนังสือ

| ประเภท | bookNameId |
|--------|------------|
| บันทึกข้อความ (Memo) | `BB4A2F11-722D-449A-BCC5-22208C7A4DEC` |
| หนังสือส่งออก (Outbound) | `90F72F0E-528D-4992-907A-F2C6B37AD9A5` |
| หนังสือรับเข้า (Inbound) | `03241AA7-0E85-4C5C-A2CC-688212A79B84` |
| หนังสือประกาศ (Announcement) | `23065068-BB18-49EA-8CE7-22945E16CB6D` |
| หนังสือคำสั่ง (Order) | `3FEDE42B-078A-4D2C-9B21-3EAD3E418F3D` |
| หนังสือระเบียบ (Regulation) | `50792880-F85A-4343-9672-7B61AF828A5B` |
| หนังสือข้อบังคับ (Rule) | `4AB1EC00-9E5E-4113-B577-D8ED46BA7728` |
| หนังสือประทับตรา (Stamp) | `AF3E7697-6F7E-4AD8-B76C-E2134DB98747` |
| หนังสือกระทรวง (Ministry) | `4B3EB169-6203-4A71-A3BD-A442FEAAA91F` |
