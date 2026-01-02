# Sarabun PDF API - วิธีรัน

## เตรียมความพร้อม

### 1. ติดตั้ง Java 17
```bash
java -version
# ต้องเป็น Java 17 หรือสูงกว่า
```

### 2. ติดตั้ง Maven
```bash
mvn -version
```

---

## วิธีรันโปรเจค

### Windows:
```bash
# Terminal ปกติ (CMD)
mvn clean install
mvn spring-boot:run
```

### Linux/Mac:
```bash
./mvnw clean install
./mvnw spring-boot:run
```

---

## หลังรันสำเร็จ

เปิดเบราว์เซอร์:

- **Swagger UI:** http://localhost:8888/swagger-ui.html
- **API Docs:** http://localhost:8888/api-docs
- **Health Check:** http://localhost:8888/api/pdf/health

---

## ทดสอบ API

```bash
curl -X POST http://localhost:8888/api/pdf/preview \
  -H "Content-Type: application/json" \
  -d '{
    "bookTitle": "บันทึกข้อความทดสอบ",
    "divisionName": "กองเทคโนโลยีสารสนเทศ",
    "dateThai": "29 ธันวาคม 2568",
    "bookContent": [{"bookContent": "เนื้อหาทดสอบ"}]
  }'
```

---

## สร้าง JAR file

```bash
mvn clean package

# รัน JAR
java -jar target/sarabun-pdf-api-1.0.0.jar
```

---

## แก้ปัญหา

### Port ถูกใช้อยู่:
แก้ไขใน `src/main/resources/application.properties`:
```properties
server.port=8888  # เปลี่ยนเป็น port ว่าง
```

### Font ไม่แสดง:
ตรวจสอบว่ามีไฟล์ใน `src/main/resources/fonts/`:
- THSarabunNew.ttf
- THSarabunNew Bold.ttf
