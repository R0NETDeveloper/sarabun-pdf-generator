# 📊 การวิเคราะห์ความสามารถในการรองรับ Concurrent Requests และ Scalability

> **วันที่วิเคราะห์**: 22 มกราคม 2026  
> **ระบบ**: Sarabun PDF Generator API v1.0.0  
> **Framework**: Spring Boot 3.5.9 + Java 17

---

## 📝 สรุปผลการวิเคราะห์

### ✅ สถานะปัจจุบัน

| หัวข้อ | สถานะ | รายละเอียด |
|--------|-------|-----------|
| **Concurrency Model** | ✅ ใช้งานได้ | Servlet-based (Synchronous Blocking I/O) |
| **Rate Limiting** | ✅ มีแล้ว | Bucket4j - Token Bucket Algorithm |
| **Thread Pool** | ⚠️ ใช้ Default | Tomcat Default: 200 threads |
| **Connection Pool** | ⚠️ ไม่มี | ไม่ได้ใช้ Database |
| **Async Processing** | ❌ ยังไม่มี | ทุก request เป็น Synchronous |
| **Load Balancing** | ❌ ยังไม่มี | ต้องทำที่ Infrastructure level |

### 📊 ความสามารถรองรับ Request ปัจจุบัน

**📌 คำตอบคำถาม: "ระบบฉันรองรับกี่ request?"**

```
┌─────────────────────────────────────────────────────────────────┐
│ ระดับการรองรับ (ตามการตั้งค่าปัจจุบัน)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Rate Limit Layer:                                              │
│  ├─ Global: 100 requests/minute                                 │
│  └─ Per-IP: 100 requests/minute (burst 50)                      │
│                                                                 │
│  Tomcat Thread Pool (Default):                                  │
│  ├─ Max Threads: 200 concurrent requests                        │
│  ├─ Accept Count: 100 (request queue)                           │
│  └─ Max Connections: 10,000                                     │
│                                                                 │
│  Theoretical Max:                                               │
│  └─ ~100-150 concurrent requests ที่ระบบทำงานได้ดี             │
│     (จำกัดโดย Rate Limit และ PDF generation time)               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ 1. สถาปัตยกรรมปัจจุบัน

### 1.1 Concurrency Model

**ระบบใช้ Synchronous Blocking I/O (Traditional Servlet Model)**

```java
// Controller ปัจจุบัน - Synchronous
@PostMapping("/preview")
public ResponseEntity<ApiResponse<String>> previewPdf(
    @RequestBody GeneratePdfRequest request,
    HttpServletRequest httpRequest) {
    
    // ✅ มี Rate Limiting
    if (!rateLimitConfig.tryConsume(clientIp)) {
        return ResponseEntity.status(429).body(...);
    }
    
    // ⚠️ Synchronous - Thread จะรออยู่จนกว่า PDF generation เสร็จ
    ApiResponse<String> response = generatePdfService.previewPdf(request);
    
    return ResponseEntity.ok(response);
}
```

**การทำงาน:**
1. **1 Request = 1 Thread** ถือ request นั้นตลอดจนจบ
2. Thread จะ **ไม่ปล่อยให้ทำงานอื่น** จนกว่าจะ generate PDF เสร็จ
3. ใช้ **Tomcat Thread Pool** (default 200 threads)

**ข้อดี:**
- ✅ เขียนง่าย อ่านง่าย debug ง่าย
- ✅ Transaction consistency ดี
- ✅ เหมาะกับ CPU-intensive task (PDF generation)

**ข้อเสีย:**
- ⚠️ Thread แพง (1-2 MB memory/thread)
- ⚠️ จำกัดจำนวน concurrent requests ได้ไม่สูงมาก
- ⚠️ ถ้าทุก thread ถูกใช้หมด request ใหม่จะรอในคิว

---

### 1.2 Rate Limiting (มีอยู่แล้ว ✅)

**ใช้ Bucket4j - Token Bucket Algorithm**

```java
// RateLimitConfig.java
public class RateLimitConfig {
    // Global limit
    @Value("${ratelimit.global.requests-per-minute:100}")
    private int globalRequestsPerMinute = 100;
    
    // Per-IP limit
    @Value("${ratelimit.per-ip.requests-per-minute:100}")
    private int perIpRequestsPerMinute = 100;
    
    @Value("${ratelimit.per-ip.burst-capacity:50}")
    private int perIpBurstCapacity = 50;
}
```

**การทำงาน:**
```
Request → Global Bucket → Per-IP Bucket → Controller
           (100/min)      (100/min)
             ↓ pass         ↓ pass
             ↓              ↓
          ❌ 429 TOO MANY REQUESTS (ถ้าเกิน)
```

**การป้องกัน:**
- ✅ **Global Limit**: ป้องกันระบบล่มจาก traffic surge ทั้งหมด
- ✅ **Per-IP Limit**: ป้องกัน single user DOS attack
- ✅ **Burst Capacity**: อนุญาตให้ส่ง request รัวๆ ในช่วงสั้นๆ ได้

---

### 1.3 Thread Pool Configuration (ใช้ Default ⚠️)

**Tomcat Default Configuration:**

```yaml
# ปัจจุบันไม่ได้ config ใน application.properties
# ใช้ค่า default ของ Spring Boot Tomcat

Default Values:
├─ server.tomcat.threads.max = 200
├─ server.tomcat.threads.min-spare = 10
├─ server.tomcat.accept-count = 100 (request queue size)
├─ server.tomcat.max-connections = 10000
└─ server.tomcat.connection-timeout = 20000ms (20 seconds)
```

**ความหมาย:**
- **Max 200 threads**: รองรับ **200 concurrent requests** สูงสุด
- **Accept count 100**: ถ้า thread เต็มจะ queue ได้ 100 requests
- **Max connections 10,000**: รับ connection พร้อมกันได้สูงสุด (แต่ process ได้แค่ 200)

---

## 🧪 2. การทดสอบ Scenario: 100, 1000 Concurrent Users

### Scenario 1: 100 Concurrent Users

```
┌────────────────────────────────────────────────────────────┐
│ สถานการณ์: 100 users ยิง request พร้อมกัน                  │
└────────────────────────────────────────────────────────────┘

1. Request มาถึงระบบ: 100 requests พร้อมกัน
   │
   ├─ Rate Limit Check (Global)
   │  └─ ✅ PASS (100/minute อนุญาต)
   │
   ├─ Rate Limit Check (Per-IP)
   │  ├─ ถ้าจาก IP เดียว: ❌ REJECT บาง request (เกิน 100/min)
   │  └─ ถ้าจาก IP ต่างๆ: ✅ PASS
   │
   ├─ Tomcat Thread Pool
   │  ├─ จัดสรร 100 threads จาก pool (เหลืออีก 100 threads)
   │  └─ ✅ ทุก request ได้ process ทันที
   │
   └─ PDF Generation
      ├─ แต่ละ thread ทำงาน generate PDF (CPU intensive)
      ├─ ใช้เวลาประมาณ 1-3 วินาที/request
      └─ ✅ ระบบทำงานปกติ เสร็จทีละ request

**ผลลัพธ์:**
✅ ระบบรองรับได้สบาย
- Response time: 1-3 วินาที
- CPU usage: 50-70%
- Memory: เพิ่มขึ้นตามจำนวน PDF ที่กำลัง generate
```

### Scenario 2: 1000 Concurrent Users

```
┌────────────────────────────────────────────────────────────┐
│ สถานการณ์: 1000 users ยิง request พร้อมกัน                 │
└────────────────────────────────────────────────────────────┘

1. Request มาถึงระบบ: 1000 requests พร้อมกัน
   │
   ├─ Rate Limit Check (Global)
   │  ├─ ใน 1 นาที: อนุญาตแค่ 100 requests
   │  └─ ❌ REJECT 900 requests → 429 TOO MANY REQUESTS
   │
   ├─ 100 requests ที่ผ่าน Rate Limit
   │  │
   │  ├─ Tomcat Thread Pool
   │  │  ├─ จัดสรร 100 threads จาก pool
   │  │  └─ ✅ ทุก request ได้ process
   │  │
   │  └─ PDF Generation
   │     └─ ✅ ทำงานปกติ

**ผลลัพธ์:**
⚠️ Rate Limit ป้องกันระบบไว้
- ✅ ระบบไม่พัง
- ✅ 100 requests ทำงานได้ปกติ
- ⚠️ 900 requests ถูก reject (429 response)
- ⚠️ User experience ไม่ดี (ถูกปฏิเสธเยอะ)

**ถ้าปิด Rate Limit (อันตราย!):**
1000 requests → Tomcat Pool (200 threads)
├─ 200 requests: ✅ ได้ thread ทำงานทันที
├─ 100 requests: ⏳ รอใน accept queue
└─ 700 requests: ❌ Connection refused / Timeout

❌ ระบบล่ม / ช้ามาก / Out of Memory!
```

---

## 🎯 3. Async/Await ใน Java คืออะไร?

### 3.1 Java ไม่มี async/await แบบ JavaScript/C#

**JavaScript/C# มี async/await:**
```javascript
// JavaScript
async function generatePdf(request) {
    const result = await pdfService.generate(request);
    return result;
}
```

**Java ใช้วิธีอื่น:**
```java
// Java - Synchronous (ปัจจุบัน)
public String generatePdf(GeneratePdfRequest request) {
    return pdfService.generate(request);  // รอจนเสร็จ
}

// Java - Asynchronous (Future/CompletableFuture)
@Async
public CompletableFuture<String> generatePdfAsync(GeneratePdfRequest request) {
    String result = pdfService.generate(request);
    return CompletableFuture.completedFuture(result);
}
```

### 3.2 Java Async Patterns

**Option 1: Spring @Async + CompletableFuture**
```java
@Service
public class PdfService {
    
    @Async("pdfTaskExecutor")
    public CompletableFuture<String> generateAsync(GeneratePdfRequest request) {
        // ทำงานใน separate thread pool
        String pdf = generatePdf(request);
        return CompletableFuture.completedFuture(pdf);
    }
}

@Controller
public class PdfController {
    
    @PostMapping("/preview-async")
    public CompletableFuture<ResponseEntity<String>> previewAsync(
        @RequestBody GeneratePdfRequest request) {
        
        // Thread หลักปล่อยได้ทันที
        return pdfService.generateAsync(request)
            .thenApply(pdf -> ResponseEntity.ok(pdf));
    }
}
```

**Option 2: Virtual Threads (Java 21+)**
```java
// แนะนำถ้าอัปเกรดเป็น Java 21
// Virtual threads: lightweight, จำนวนมากได้, ไม่แพงเหมือน platform threads

@Configuration
public class ThreadConfig {
    
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

**Option 3: Reactive (Spring WebFlux)**
```java
// เปลี่ยนเป็น Non-blocking I/O
@RestController
public class PdfController {
    
    @PostMapping("/preview")
    public Mono<ResponseEntity<String>> previewPdf(
        @RequestBody GeneratePdfRequest request) {
        
        return pdfService.generatePdfReactive(request)
            .map(pdf -> ResponseEntity.ok(pdf));
    }
}
```

---

## ❓ 4. คำตอบคำถาม: ต้องใช้ Async/Await ไหม?

### 📌 คำตอบสำหรับ Sarabun PDF API: **ไม่จำเป็น (ยังไม่ต้อง)**

**เหตุผล:**

#### ✅ **Synchronous Model เหมาะกับ PDF Generation แล้ว**

```
PDF Generation = CPU-Intensive Task
├─ ไม่ใช่ I/O waiting (database query, API call)
├─ ใช้ CPU เต็มที่ตลอดเวลา (render, draw, encode)
└─ Async ไม่ช่วยเพิ่มความเร็ว

การเปรียบเทียบ:
┌─────────────────────────────────────────────────────────┐
│ Task Type          │ Blocking? │ Async ช่วย? │ แนะนำ    │
├─────────────────────────────────────────────────────────┤
│ PDF Generation     │ CPU       │ ❌ ไม่      │ Sync     │
│ Database Query     │ I/O       │ ✅ ช่วย     │ Async    │
│ External API Call  │ I/O       │ ✅ ช่วย     │ Async    │
│ File Upload/Read   │ I/O       │ ✅ ช่วย     │ Async    │
└─────────────────────────────────────────────────────────┘
```

#### ✅ **มี Rate Limiting ป้องกันแล้ว**

```java
// ระบบมีการป้องกันอยู่แล้ว
if (!rateLimitConfig.tryConsume(clientIp)) {
    return 429; // TOO MANY REQUESTS
}
```

#### ⚠️ **Async เพิ่มความซับซ้อนโดยไม่จำเป็น**

- Code ยากขึ้น
- Debug ยากขึ้น
- Error handling ซับซ้อนขึ้น
- ไม่ได้เพิ่มความเร็วจริงๆ สำหรับ CPU-bound task

---

## 🛠️ 5. แนวทางการปรับปรุงสำหรับ High Load

### 5.1 ระดับ Application (แนะนำทำ ✅)

#### **Option 1: ปรับ Thread Pool และ Rate Limit (แนะนำที่สุด)**

```properties
# application.properties

# ============================================
# Thread Pool Configuration
# ============================================
# เพิ่ม thread pool ให้เหมาะกับ server specs
server.tomcat.threads.max=500
server.tomcat.threads.min-spare=50
server.tomcat.accept-count=200
server.tomcat.max-connections=20000

# Connection timeout
server.tomcat.connection-timeout=30000

# Keep-alive timeout
server.tomcat.keep-alive-timeout=60000
server.tomcat.max-keep-alive-requests=100

# ============================================
# Rate Limiting - ปรับให้รองรับ load สูงขึ้น
# ============================================
ratelimit.enabled=true

# Global: เพิ่มเป็น 500-1000 requests/minute
ratelimit.global.requests-per-minute=500

# Per-IP: เพิ่มเป็น 200 requests/minute
ratelimit.per-ip.requests-per-minute=200
ratelimit.per-ip.burst-capacity=100

# ============================================
# Memory Management
# ============================================
# JVM Options (ตั้งตอน run)
# -Xms2g -Xmx4g -XX:+UseG1GC
```

**ผลลัพธ์:**
- รองรับ **500 concurrent requests** (เพิ่มจาก 200)
- Rate limit อนุญาต **500 requests/minute** (เพิ่มจาก 100)
- Server ต้องมี RAM มากขึ้น (แนะนำ 8-16 GB)

---

#### **Option 2: เพิ่ม Request Queue System (แนะนำสำหรับ scale ใหญ่)**

```java
/**
 * สร้าง Queue System สำหรับ PDF generation
 * - รับ request เข้า queue ก่อน (return job ID ทันที)
 * - ประมวลผลทีละน้อยตาม queue
 * - Client query ผลด้วย job ID
 */

@Service
public class PdfQueueService {
    
    private final Queue<PdfJob> jobQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, PdfJob> jobStatus = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    
    @PostConstruct
    public void init() {
        // สร้าง fixed thread pool สำหรับ PDF generation
        int threads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(threads);
        
        // เริ่ม worker threads
        for (int i = 0; i < threads; i++) {
            executor.submit(this::processQueue);
        }
    }
    
    // Submit job ทันที (ไม่รอ)
    public String submitJob(GeneratePdfRequest request) {
        String jobId = UUID.randomUUID().toString();
        
        PdfJob job = PdfJob.builder()
            .jobId(jobId)
            .request(request)
            .status(JobStatus.QUEUED)
            .submittedAt(LocalDateTime.now())
            .build();
        
        jobQueue.offer(job);
        jobStatus.put(jobId, job);
        
        return jobId;
    }
    
    // Check job status
    public PdfJob getJobStatus(String jobId) {
        return jobStatus.get(jobId);
    }
    
    // Worker thread
    private void processQueue() {
        while (!Thread.interrupted()) {
            try {
                PdfJob job = jobQueue.poll();
                if (job != null) {
                    processJob(job);
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void processJob(PdfJob job) {
        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            
            // Generate PDF
            String pdf = generatePdfService.previewPdf(job.getRequest());
            
            job.setStatus(JobStatus.COMPLETED);
            job.setResult(pdf);
            job.setCompletedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setError(e.getMessage());
        }
    }
}

// Controller
@RestController
@RequestMapping("/api/pdf")
public class PdfController {
    
    // Submit job (return ทันที)
    @PostMapping("/submit")
    public ResponseEntity<JobResponse> submitPdfJob(
        @RequestBody GeneratePdfRequest request) {
        
        String jobId = pdfQueueService.submitJob(request);
        
        return ResponseEntity.accepted()
            .body(new JobResponse(jobId, "Job submitted"));
    }
    
    // Check status
    @GetMapping("/status/{jobId}")
    public ResponseEntity<PdfJob> getJobStatus(@PathVariable String jobId) {
        PdfJob job = pdfQueueService.getJobStatus(jobId);
        
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(job);
    }
}
```

**ข้อดี:**
- ✅ รับ request ได้ไม่จำกัด (เก็บใน queue)
- ✅ ประมวลผลตามจำนวน CPU cores (ไม่ทำงานหนักเกินไป)
- ✅ Client ไม่ต้องรอ (polling หรือ webhook)
- ✅ ง่ายต่อการ scale (ย้าย queue ไป Redis/RabbitMQ ได้)

**ข้อเสีย:**
- ⚠️ Client ต้องเขียน polling logic
- ⚠️ Job status ต้องเก็บใน storage (memory/Redis)

---

#### **Option 3: Response Streaming (สำหรับ large PDF)**

```java
/**
 * Stream PDF กลับไปทีละ chunk
 * ลด memory usage และ response time
 */

@GetMapping("/preview-stream")
public ResponseEntity<StreamingResponseBody> previewPdfStream(
    @RequestBody GeneratePdfRequest request) {
    
    StreamingResponseBody stream = outputStream -> {
        // Generate PDF และ write ทีละ chunk
        try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
            pdfService.generatePdfToStream(request, pdfStream);
            
            byte[] pdfBytes = pdfStream.toByteArray();
            outputStream.write(pdfBytes);
            outputStream.flush();
        }
    };
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .body(stream);
}
```

---

### 5.2 ระดับ Infrastructure (แนะนำสำหรับ Production)

#### **Option 1: Horizontal Scaling (แนะนำที่สุด)**

```yaml
# Docker Compose - Multiple instances
version: '3.8'
services:
  pdf-api-1:
    image: sarabun-pdf-api:1.0.0
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    ports:
      - "8889:8888"
  
  pdf-api-2:
    image: sarabun-pdf-api:1.0.0
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    ports:
      - "8890:8888"
  
  pdf-api-3:
    image: sarabun-pdf-api:1.0.0
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    ports:
      - "8891:8888"
  
  # Load Balancer
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - pdf-api-1
      - pdf-api-2
      - pdf-api-3
```

```nginx
# nginx.conf - Load Balancer
upstream pdf_backend {
    least_conn;  # Route ไปที่ server ที่มี connection น้อยที่สุด
    server pdf-api-1:8888;
    server pdf-api-2:8888;
    server pdf-api-3:8888;
}

server {
    listen 80;
    
    location /api/pdf/ {
        proxy_pass http://pdf_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # Timeout settings
        proxy_connect_timeout 30s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
}
```

**ผลลัพธ์:**
- 3 instances = **รองรับ 1500 concurrent requests** (500×3)
- แต่ละ instance ทำงานเบาลง
- ถ้า 1 instance ล่ม ยังมี 2 ตัวทำงานอยู่

---

#### **Option 2: Kubernetes Auto-scaling**

```yaml
# kubernetes-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sarabun-pdf-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sarabun-pdf-api
  template:
    metadata:
      labels:
        app: sarabun-pdf-api
    spec:
      containers:
      - name: pdf-api
        image: sarabun-pdf-api:1.0.0
        ports:
        - containerPort: 8888
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8888
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8888
          initialDelaySeconds: 10
          periodSeconds: 5

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: sarabun-pdf-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sarabun-pdf-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**การทำงาน:**
- เริ่มต้น 3 pods
- ถ้า CPU > 70% หรือ Memory > 80% → Scale ขึ้นเป็น 4-10 pods
- ถ้า load ลดลง → Scale ลงกลับเหลือ 3 pods

---

#### **Option 3: CDN + Caching (ถ้า PDF ซ้ำบ่อย)**

```java
@Service
public class PdfCacheService {
    
    private final Cache<String, String> pdfCache;
    
    public PdfCacheService() {
        // Caffeine Cache - in-memory
        pdfCache = Caffeine.newBuilder()
            .maximumSize(1000)  // เก็บ 1000 PDFs
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();
    }
    
    public String getCachedOrGenerate(GeneratePdfRequest request) {
        String cacheKey = generateCacheKey(request);
        
        return pdfCache.get(cacheKey, key -> {
            log.info("Cache miss for {}, generating PDF...", key);
            return pdfService.generate(request);
        });
    }
    
    private String generateCacheKey(GeneratePdfRequest request) {
        // สร้าง hash จาก request content
        return DigestUtils.sha256Hex(
            JsonUtils.toJson(request)
        );
    }
}
```

---

## 📋 6. แผนการปรับปรุงแบบขั้นตอน

### 🎯 Phase 1: Quick Wins (1-2 วัน)

**1. ปรับ Thread Pool และ Rate Limit**

```properties
# application.properties - เพิ่มค่าเหล่านี้

# Thread Pool
server.tomcat.threads.max=500
server.tomcat.threads.min-spare=50
server.tomcat.accept-count=200

# Rate Limiting
ratelimit.global.requests-per-minute=500
ratelimit.per-ip.requests-per-minute=200
ratelimit.per-ip.burst-capacity=100
```

**2. เพิ่ม Health Monitoring**

```java
@Component
public class SystemHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RateLimitConfig rateLimitConfig;
    
    @Override
    public Health health() {
        RateLimitStats stats = rateLimitConfig.getStats();
        
        return Health.up()
            .withDetail("rateLimit", stats)
            .withDetail("threads", getThreadInfo())
            .withDetail("memory", getMemoryInfo())
            .build();
    }
    
    private Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return Map.of(
            "current", threadMXBean.getThreadCount(),
            "peak", threadMXBean.getPeakThreadCount(),
            "daemon", threadMXBean.getDaemonThreadCount()
        );
    }
    
    private Map<String, Object> getMemoryInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        
        return Map.of(
            "used", heap.getUsed() / (1024 * 1024) + "MB",
            "max", heap.getMax() / (1024 * 1024) + "MB",
            "usage", (heap.getUsed() * 100 / heap.getMax()) + "%"
        );
    }
}
```

**ผลลัพธ์:**
- ✅ รองรับ 500 concurrent requests (จากเดิม 200)
- ✅ Rate limit 500/minute (จากเดิม 100)
- ✅ มี monitoring สำหรับดู health

---

### 🚀 Phase 2: Horizontal Scaling (1 สัปดาห์)

**1. Setup Load Balancer (Nginx)**
- สร้าง 3-5 instances
- Nginx load balancing
- Shared session storage (Redis)

**2. Centralized Rate Limiting (Redis)**

```java
// ใช้ Redis แทน in-memory สำหรับ rate limiting
// ทำให้ rate limit ทำงานข้ามทุก instances

@Component
public class RedisRateLimitConfig {
    
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    
    public boolean tryConsume(String clientIp) {
        String key = "ratelimit:ip:" + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            // ตั้ง expiration 1 นาที
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        return count <= perIpRequestsPerMinute;
    }
}
```

**ผลลัพธ์:**
- ✅ รองรับ 1500+ concurrent requests (500×3)
- ✅ High availability (1 instance ล่มยังมีอีก 2)
- ✅ Rate limit ทำงานถูกต้องข้าม instances

---

### 🏆 Phase 3: Advanced (1 เดือน)

**1. เพิ่ม Queue System (RabbitMQ/Redis Queue)**
- Async job processing
- Job status tracking
- Webhook notification

**2. Caching Layer (Redis)**
- Cache PDF ที่ซ้ำ
- Cache fonts และ images

**3. Monitoring & Alerting (Prometheus + Grafana)**
- Real-time metrics
- Alert เมื่อ CPU/Memory สูง
- Dashboard สำหรับดู traffic patterns

**ผลลัพธ์:**
- ✅ รองรับ 10,000+ requests/minute
- ✅ Auto-scaling ตาม load
- ✅ Complete observability

---

## 📊 7. ตารางเปรียบเทียบ Solutions

| Solution | ความยาก | เวลา | Cost | Max Concurrent | แนะนำ |
|----------|---------|------|------|----------------|-------|
| **ปัจจุบัน (Default)** | ✅ ง่าย | 0 | ฟรี | 200 | ✅ สำหรับ POC/Dev |
| **ปรับ Thread Pool** | ✅ ง่าย | 1h | ฟรี | 500 | ✅ Quick win |
| **Queue System** | ⚠️ กลาง | 1w | ฟรี-น้อย | ไม่จำกัด | ✅ แนะนำ |
| **Horizontal Scaling** | ⚠️ กลาง | 1w | กลาง | 1500+ | ✅ แนะนำ |
| **Kubernetes Auto-scale** | ❌ ยาก | 1m | สูง | 10,000+ | ⚠️ Enterprise |
| **WebFlux (Reactive)** | ❌ ยากมาก | 2m | ฟรี | สูงมาก | ❌ Overkill |

---

## ✅ 8. สรุปและคำแนะนำ

### 📌 คำตอบโจทย์หลัก

1. **ระบบรองรับกี่ request?**
   - ปัจจุบัน: **100-200 concurrent requests**
   - หลังปรับ Thread Pool: **500 concurrent requests**
   - หลัง Horizontal Scale (3 instances): **1500+ concurrent requests**

2. **ต้องใช้ async/await ไหม?**
   - **ไม่จำเป็น** สำหรับ CPU-intensive task
   - Synchronous model เหมาะกับ PDF generation แล้ว
   - มี Rate Limiting ป้องกันระบบแล้ว

3. **ถ้ามีคน 100, 1000 ยิงพร้อมกัน?**
   - **100 คน**: ✅ รองรับได้สบาย
   - **1000 คน**: ⚠️ Rate limit จะ reject บางส่วน แต่ระบบไม่พัง

4. **วิธีป้องกันระบบพัง?**
   - ✅ **มีแล้ว**: Rate Limiting (Bucket4j)
   - ✅ **ควรทำ**: ปรับ Thread Pool
   - ✅ **แนะนำ**: Horizontal Scaling + Load Balancer
   - ✅ **Advanced**: Queue System + Caching

---

### 🎯 แนวทางที่แนะนำ

```
Step 1 (ทำเลย - 1 ชั่วโมง):
└─ ปรับ Thread Pool และ Rate Limit ใน application.properties

Step 2 (ถ้าต้อง scale จริง - 1 สัปดาห์):
├─ Setup Nginx Load Balancer
├─ Run 3-5 instances
└─ ใช้ Redis สำหรับ shared rate limiting

Step 3 (Optional - ถ้าต้องการ high throughput - 1 เดือน):
├─ เพิ่ม Queue System (RabbitMQ)
├─ Caching Layer (Redis)
└─ Monitoring (Prometheus + Grafana)
```

### ⚠️ สิ่งที่ไม่แนะนำ

- ❌ เปลี่ยนเป็น Reactive (WebFlux) → Overkill สำหรับ use case นี้
- ❌ ใช้ @Async ทุก method → ไม่ช่วยเพิ่มความเร็วสำหรับ CPU-bound task
- ❌ เพิ่ม thread เยอะเกินไป → อาจ OOM (Out of Memory)

### ✅ แนวทางที่ดีที่สุด

1. **เริ่มจาก Simple**: ปรับ config + monitoring
2. **Scale Horizontally**: เพิ่ม instances (ไม่ใช่เพิ่ม thread)
3. **Use Queue**: ถ้าต้องการ handle load สูงมาก
4. **Cache**: ถ้า PDF ซ้ำบ่อย
5. **Monitor**: ต้องมี metrics เพื่อตัดสินใจ

---

## 📚 9. Resources และเอกสารอ้างอิง

### Spring Boot Performance Tuning
- [Spring Boot Performance Tuning](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server)
- [Tomcat Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server.tomcat)

### Scaling Strategies
- [Horizontal vs Vertical Scaling](https://docs.aws.amazon.com/wellarchitected/latest/performance-efficiency-pillar/horizontal-scaling.html)
- [Kubernetes HPA](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)

### Rate Limiting
- [Bucket4j Documentation](https://bucket4j.com/)
- [Rate Limiting Patterns](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)

### Java Async Patterns
- [Spring @Async](https://spring.io/guides/gs/async-method/)
- [Virtual Threads (Java 21)](https://openjdk.org/jeps/444)
- [CompletableFuture Guide](https://www.baeldung.com/java-completablefuture)

---

## 📧 สรุปสำหรับ Management

> **TL;DR**
> 
> ระบบปัจจุบันรองรับ **100-200 concurrent users** ได้ดี มี Rate Limiting ป้องกันระบบพังแล้ว
> 
> **ถ้าต้องการรองรับ 1000+ concurrent users:**
> 1. ปรับ Thread Pool config (1 ชั่วโมง, ฟรี) → 500 users
> 2. Scale เป็น 3-5 instances + Load Balancer (1 สัปดาห์) → 1500+ users
> 3. เพิ่ม Queue System (Optional, 1 เดือน) → 10,000+ users
> 
> **ไม่จำเป็นต้องใช้ async/await** เพราะ PDF generation เป็น CPU-intensive task
> 
> **Budget estimate:**
> - Option 1 (Thread Pool): ฟรี
> - Option 2 (3 instances): ~3,000-5,000 บาท/เดือน (cloud hosting)
> - Option 3 (Full stack): ~10,000-20,000 บาท/เดือน

---

**เอกสารนี้สร้างโดย:** GitHub Copilot  
**วันที่:** 22 มกราคม 2026  
**เวอร์ชัน:** 1.0
