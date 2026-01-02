package th.go.etda.sarabun.pdf.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request/Response Models สำหรับ PDF Generation
 * 
 * แปลงมาจาก: ETDA.SarabunMultitenant.Model/GeneratePdfModel.cs
 * การเปลี่ยนแปลง:
 * - ใช้ Lombok เพื่อลด boilerplate code (@Data, @Builder)
 * - ใช้ Jackson annotations สำหรับ JSON serialization
 * - แปลง nullable types ของ C# เป็น Java wrapper types
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePdfRequest {
    
    // จาก BookModel.CreateBookReqModel
    private String actionCode;
    private String processId;
    private String bookTypeId;
    private String bookNameId;
    private String speedLayerId;
    private String secretLayerId;
    private String numReleaseChannelId;
    private String ministyId;
    private String departmentId;
    private String divisionId;
    private String organizeId;
    private String bookTitle;
    private String docTypeId;
    private Boolean isMultiBookName;
    private String divisionRemark;
    
    // จาก GeneratePdfModel.GenPdfReq
    private String guid;
    private String statusId;
    private BookSubDetail subDetail;
    private String divisionName;
    private String numRelease;
    private String speedLayer;
    private String speedLayerOther;
    private Boolean isShow;
    private String pdfBase64;
    private String type;
    private String year;
    private String endDoc;
    private String dateThai;
    private String timeThai;
    private String bookNo;
    private String department;
    private String recipients;      // ผู้รับหนังสือ (เรียน)
    private String formatPdf;
    
    // Lists
    private List<BookContent> bookContent;
    private List<BookRelate> bookLearner;
    private List<BookRelate> bookSubmited;
    private List<BookRelate> bookReview;
    private List<BookRelate> bookSigned;
    
    /**
     * BookContent Model
     * แปลงมาจาก: BookModel.BookContentModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookContent {
        @JsonProperty("GUId")
        private String guid;
        private String bookDetailId;
        private String bookNameId;
        private String bookNameName;
        private String bookContentNo;
        private String bookContentTitle;
        private String bookContent;
        private Boolean nextPageStatus;
    }
    
    /**
     * BookRelate Model
     * แปลงมาจาก: BookModel.BookRelateReqModel
     * ใช้สำหรับ Learner, Submited, Review, Signed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRelate {
        private String relatedId;
        private String settingRelateId;
        private String relatedType;
        private String relatedOptionOrder;
        private String departmentId;
        private String organizeId;
        private String relatedNo;
        private String relateOrganizeId;
        private String positionId;
        private String relatePositionId;
        private String personalId;
        private String personalOption;
        private String positionName;
        private String relatePersonalId;
        private String bookDetailId;
        private String bookRelateId;
        private Boolean signStatus;
        private String bookRelateDetailId;
        private String email;
        private String prefixName;
        private String firstname;
        private String lastname;
        private String departmentName;
        private String organizeName;
    }
    
    /**
     * BookSubDetail Model
     * แปลงมาจาก: BookModel.BookSubDetailModel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSubDetail {
        private List<SubDetailLearner> subDetailLearner;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SubDetailLearner {
            private String emailEnding;
            private String detail;
        }
    }
}
