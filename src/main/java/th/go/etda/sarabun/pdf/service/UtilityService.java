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
    

}
