package th.go.etda.sarabun.pdf.service.pdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * FontManager - จัดการ Font สำหรับ PDF Generation
 * 
 * ใช้ Font Bytes Caching เพื่อเพิ่ม Performance:
 * - โหลด font bytes ครั้งเดียวตอน startup
 * - ทุก PDDocument ใช้ bytes เดียวกัน (ไม่ต้องอ่านไฟล์ซ้ำ)
 * - ลด I/O และ memory allocation
 * 
 * หมายเหตุ: PDFont ต้องสร้างใหม่ทุก PDDocument เพราะ PDFBox ผูกกับ document
 * แต่ font bytes สามารถ cache ได้
 * 
 * การใช้งาน:
 * - ใช้ผ่าน static method: FontManager.getInstance()
 * - หรือ inject เป็น Spring Bean
 */
@Slf4j
@Component
public class FontManager {
    
    // ============================================
    // Singleton Instance (for static access)
    // ============================================
    private static FontManager instance;
    
    // ============================================
    // Font Paths
    // ============================================
    private static final String FONT_REGULAR_PATH = "fonts/THSarabunNew.ttf";
    private static final String FONT_BOLD_PATH = "fonts/THSarabunNew Bold.ttf";
    
    // ============================================
    // Cached Font Bytes (โหลดครั้งเดียว)
    // ============================================
    private byte[] regularFontBytes;
    private byte[] boldFontBytes;
    
    // ============================================
    // Statistics (Thread-safe counters)
    // ============================================
    private final AtomicLong regularFontLoadCount = new AtomicLong(0);
    private final AtomicLong boldFontLoadCount = new AtomicLong(0);
    
    /**
     * โหลด font bytes ตอน startup
     */
    @PostConstruct
    public void init() {
        log.info("FontManager initializing - loading font bytes into cache...");
        
        try {
            // โหลด Regular Font
            regularFontBytes = loadFontBytes(FONT_REGULAR_PATH);
            log.info("Cached regular font: {} bytes", regularFontBytes.length);
            
            // โหลด Bold Font
            boldFontBytes = loadFontBytes(FONT_BOLD_PATH);
            log.info("Cached bold font: {} bytes", boldFontBytes.length);
            
            // Set singleton instance
            instance = this;
            
            log.info("FontManager initialized successfully - fonts cached in memory");
            
        } catch (Exception e) {
            log.error("Failed to initialize FontManager: {}", e.getMessage());
            throw new RuntimeException("Cannot load Thai fonts - PDF generation will not work", e);
        }
    }
    
    /**
     * Get singleton instance (for static access from PdfGeneratorBase)
     */
    public static FontManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FontManager not initialized yet - Spring context may not be ready");
        }
        return instance;
    }
    
    /**
     * Check if instance is available
     */
    public static boolean isInstanceAvailable() {
        return instance != null;
    }
    
    /**
     * โหลด font bytes จาก classpath
     */
    private byte[] loadFontBytes(String fontPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(fontPath);
        
        if (!resource.exists()) {
            throw new IOException("Font file not found: " + fontPath);
        }
        
        try (InputStream is = resource.getInputStream()) {
            return is.readAllBytes();
        }
    }
    
    // ============================================
    // Public Methods - สร้าง PDFont จาก cached bytes
    // ============================================
    
    /**
     * สร้าง Regular Font (TH Sarabun New) สำหรับ PDDocument
     * 
     * @param document PDDocument ที่จะใช้ font
     * @return PDFont ที่พร้อมใช้งาน
     */
    public PDFont getRegularFont(PDDocument document) throws IOException {
        if (regularFontBytes == null) {
            throw new IllegalStateException("FontManager not initialized - regularFontBytes is null");
        }
        
        long loadCount = regularFontLoadCount.incrementAndGet();
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(regularFontBytes)) {
            PDFont font = PDType0Font.load(document, bais);
            log.debug("Created regular font for document (total loads: {})", loadCount);
            return font;
        }
    }
    
    /**
     * สร้าง Bold Font (TH Sarabun New Bold) สำหรับ PDDocument
     * 
     * @param document PDDocument ที่จะใช้ font
     * @return PDFont ที่พร้อมใช้งาน
     */
    public PDFont getBoldFont(PDDocument document) throws IOException {
        if (boldFontBytes == null) {
            throw new IllegalStateException("FontManager not initialized - boldFontBytes is null");
        }
        
        long loadCount = boldFontLoadCount.incrementAndGet();
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(boldFontBytes)) {
            PDFont font = PDType0Font.load(document, bais);
            log.debug("Created bold font for document (total loads: {})", loadCount);
            return font;
        }
    }
    
    /**
     * สร้างทั้ง Regular และ Bold Font พร้อมกัน
     * 
     * @param document PDDocument ที่จะใช้ font
     * @return FontPair ที่มีทั้ง regular และ bold
     */
    public FontPair getFonts(PDDocument document) throws IOException {
        return new FontPair(
            getRegularFont(document),
            getBoldFont(document)
        );
    }
    
    // ============================================
    // Statistics & Monitoring
    // ============================================
    
    /**
     * ดูสถิติการใช้งาน font
     */
    public FontStats getStats() {
        return new FontStats(
            regularFontBytes != null ? regularFontBytes.length : 0,
            boldFontBytes != null ? boldFontBytes.length : 0,
            regularFontLoadCount.get(),
            boldFontLoadCount.get()
        );
    }
    
    /**
     * ตรวจสอบว่า fonts พร้อมใช้งานหรือไม่
     */
    public boolean isReady() {
        return regularFontBytes != null && boldFontBytes != null;
    }
    
    /**
     * รีโหลด fonts (สำหรับกรณีที่ต้องการ refresh)
     */
    public void reload() {
        log.info("Reloading fonts...");
        init();
    }
    
    // ============================================
    // Inner Classes
    // ============================================
    
    /**
     * คู่ Font (Regular + Bold)
     */
    public static class FontPair {
        private final PDFont regular;
        private final PDFont bold;
        
        public FontPair(PDFont regular, PDFont bold) {
            this.regular = regular;
            this.bold = bold;
        }
        
        public PDFont getRegular() {
            return regular;
        }
        
        public PDFont getBold() {
            return bold;
        }
    }
    
    /**
     * สถิติการใช้งาน Font
     */
    public static class FontStats {
        private final int regularBytesSize;
        private final int boldBytesSize;
        private final long regularLoadCount;
        private final long boldLoadCount;
        
        public FontStats(int regularBytesSize, int boldBytesSize, 
                        long regularLoadCount, long boldLoadCount) {
            this.regularBytesSize = regularBytesSize;
            this.boldBytesSize = boldBytesSize;
            this.regularLoadCount = regularLoadCount;
            this.boldLoadCount = boldLoadCount;
        }
        
        public int getRegularBytesSize() {
            return regularBytesSize;
        }
        
        public int getBoldBytesSize() {
            return boldBytesSize;
        }
        
        public long getRegularLoadCount() {
            return regularLoadCount;
        }
        
        public long getBoldLoadCount() {
            return boldLoadCount;
        }
        
        public long getTotalLoadCount() {
            return regularLoadCount + boldLoadCount;
        }
        
        public int getTotalCacheSize() {
            return regularBytesSize + boldBytesSize;
        }
        
        @Override
        public String toString() {
            return String.format(
                "FontStats{cacheSize=%d bytes, regularLoads=%d, boldLoads=%d, totalLoads=%d}",
                getTotalCacheSize(), regularLoadCount, boldLoadCount, getTotalLoadCount()
            );
        }
    }
}
