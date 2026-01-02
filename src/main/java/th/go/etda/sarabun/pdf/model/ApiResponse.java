package th.go.etda.sarabun.pdf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Generic Response Model
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Model/BaseModel.cs ResponseData<T>
 * การเปลี่ยนแปลง:
 * - ใช้ Generic Type <T> เหมือนเดิม
 * - แปลง HttpStatusCode เป็น HttpStatus ของ Spring
 * - เพิ่ม success flag เพื่อความชัดเจน
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    @JsonProperty("isOK")
    @Builder.Default
    private Boolean isOk = true;
    
    @Builder.Default
    private Integer statusCode = 200;
    
    private String message;
    
    private T data;
    
    /**
     * สร้าง Response สำเร็จ
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .isOk(true)
                .statusCode(HttpStatus.OK.value())
                .data(data)
                .build();
    }
    
    /**
     * สร้าง Response สำเร็จพร้อม message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .isOk(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * สร้าง Response ที่มี error
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .isOk(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build();
    }
    
    /**
     * สร้าง Response ที่มี error พร้อม status code
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .isOk(false)
                .statusCode(status.value())
                .message(message)
                .build();
    }
}
