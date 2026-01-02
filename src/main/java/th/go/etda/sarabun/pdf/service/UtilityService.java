package th.go.etda.sarabun.pdf.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility Service สำหรับฟังก์ชันช่วยเหลือต่างๆ
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Service/CenterService.cs
 * 
 * @author Migrated from .NET to Java
 */
@Slf4j
@Service
public class UtilityService {
    
    private static final String[] THAI_MONTHS = {
        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    };
    
    /**
     * แปลงวันที่เป็นรูปแบบไทย พ.ศ.
     * 
     * แปลงมาจาก: ToThaiDate() method
     * 
     * @param dateTime วันที่
     * @return วันที่รูปแบบไทย เช่น "15 มกราคม 2568"
     */
    public String toThaiDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear() + 543; // แปลง ค.ศ. เป็น พ.ศ.
        
        return String.format("%d %s %d", day, THAI_MONTHS[month - 1], year);
    }
    
    /**
     * แปลงวันที่และเวลาเป็นรูปแบบไทย
     * 
     * @param dateTime วันที่และเวลา
     * @return วันที่และเวลารูปแบบไทย เช่น "15 มกราคม 2568 เวลา 14:30 น."
     */
    public String toThaiDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        String date = toThaiDate(dateTime);
        String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        
        return String.format("%s เวลา %s น.", date, time);
    }
    
    /**
     * ตัดข้อความให้พอดีกับความกว้างที่กำหนด
     * 
     * แปลงมาจาก: WrapDivisionName() method
     * 
     * @param text ข้อความ
     * @param maxLength ความยาวสูงสุด
     * @return ข้อความที่ตัดแล้ว
     */
    public String wrapText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        int currentLength = 0;
        
        for (String word : words) {
            if (currentLength + word.length() + 1 > maxLength) {
                result.append("\n");
                currentLength = 0;
            }
            
            if (currentLength > 0) {
                result.append(" ");
                currentLength++;
            }
            
            result.append(word);
            currentLength += word.length();
        }
        
        return result.toString();
    }
    
    /**
     * ทำความสะอาด newlines ในข้อความ
     * 
     * แปลงมาจาก: NormalizeContactNewLines() method
     * 
     * @param text ข้อความ
     * @return ข้อความที่ทำความสะอาดแล้ว
     */
    public String normalizeNewLines(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replace("\r\n", "\n")
                  .replace("\r", "\n");
    }
}
