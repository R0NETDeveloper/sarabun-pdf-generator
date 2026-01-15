package th.go.etda.sarabun.pdf.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate Limit Configuration - ป้องกันการเรียก API รัวๆ
 * 
 * ใช้ Bucket4j (Token Bucket Algorithm):
 * - Global Limit: จำกัดทั้งระบบ (เช่น 100 requests/minute)
 * - Per-IP Limit: จำกัดต่อ IP (เช่น 20 requests/minute)
 * 
 * ตั้งค่าได้ใน application.properties:
 * - ratelimit.global.requests-per-minute=100
 * - ratelimit.per-ip.requests-per-minute=20
 * - ratelimit.per-ip.burst-capacity=30
 */
@Slf4j
@Component
public class RateLimitConfig {
    
    // ============================================
    // Configuration Properties
    // ============================================
    
    @Value("${ratelimit.global.requests-per-minute:100}")
    private int globalRequestsPerMinute;
    
    @Value("${ratelimit.per-ip.requests-per-minute:20}")
    private int perIpRequestsPerMinute;
    
    @Value("${ratelimit.per-ip.burst-capacity:30}")
    private int perIpBurstCapacity;
    
    @Value("${ratelimit.enabled:true}")
    private boolean enabled;
    
    // ============================================
    // Buckets Storage
    // ============================================
    
    private Bucket globalBucket;
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    
    // ============================================
    // Statistics (Thread-safe)
    // ============================================
    
    private final Map<String, Long> rejectedCountByIp = new ConcurrentHashMap<>();
    private long totalRejectedCount = 0;
    
    @PostConstruct
    public void init() {
        // สร้าง Global Bucket
        globalBucket = Bucket.builder()
            .addLimit(Bandwidth.classic(
                globalRequestsPerMinute, 
                Refill.greedy(globalRequestsPerMinute, Duration.ofMinutes(1))
            ))
            .build();
        
        log.info("RateLimitConfig initialized:");
        log.info("  - Enabled: {}", enabled);
        log.info("  - Global limit: {} requests/minute", globalRequestsPerMinute);
        log.info("  - Per-IP limit: {} requests/minute (burst: {})", perIpRequestsPerMinute, perIpBurstCapacity);
    }
    
    /**
     * ตรวจสอบว่า request สามารถผ่านได้หรือไม่
     * 
     * @param clientIp IP ของ client
     * @return true ถ้าผ่าน, false ถ้าถูก rate limit
     */
    public boolean tryConsume(String clientIp) {
        if (!enabled) {
            return true;
        }
        
        // 1. ตรวจสอบ Global Limit ก่อน
        if (!globalBucket.tryConsume(1)) {
            log.warn("Global rate limit exceeded! IP: {}", clientIp);
            totalRejectedCount++;
            return false;
        }
        
        // 2. ตรวจสอบ Per-IP Limit
        Bucket ipBucket = ipBuckets.computeIfAbsent(clientIp, this::createIpBucket);
        
        if (!ipBucket.tryConsume(1)) {
            log.warn("Per-IP rate limit exceeded! IP: {}", clientIp);
            rejectedCountByIp.merge(clientIp, 1L, Long::sum);
            totalRejectedCount++;
            return false;
        }
        
        return true;
    }
    
    /**
     * สร้าง Bucket สำหรับ IP
     */
    private Bucket createIpBucket(String ip) {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                perIpBurstCapacity,
                Refill.greedy(perIpRequestsPerMinute, Duration.ofMinutes(1))
            ))
            .build();
    }
    
    /**
     * ดูจำนวน tokens ที่เหลือ (สำหรับ debugging)
     */
    public long getAvailableTokens(String clientIp) {
        if (!enabled) {
            return Long.MAX_VALUE;
        }
        
        Bucket ipBucket = ipBuckets.get(clientIp);
        if (ipBucket == null) {
            return perIpBurstCapacity;
        }
        return ipBucket.getAvailableTokens();
    }
    
    /**
     * ดูสถิติ Rate Limit
     */
    public RateLimitStats getStats() {
        return new RateLimitStats(
            enabled,
            globalRequestsPerMinute,
            perIpRequestsPerMinute,
            perIpBurstCapacity,
            ipBuckets.size(),
            totalRejectedCount,
            globalBucket.getAvailableTokens()
        );
    }
    
    /**
     * ล้างข้อมูล IP buckets (สำหรับ cleanup)
     */
    public void clearIpBuckets() {
        int size = ipBuckets.size();
        ipBuckets.clear();
        log.info("Cleared {} IP buckets", size);
    }
    
    /**
     * ตรวจสอบว่า Rate Limit เปิดใช้งานหรือไม่
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    // ============================================
    // Inner Class - Statistics
    // ============================================
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RateLimitStats {
        private boolean enabled;
        private int globalLimit;
        private int perIpLimit;
        private int burstCapacity;
        private int activeIpBuckets;
        private long totalRejected;
        private long globalTokensAvailable;
    }
}
