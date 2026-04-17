package com.enterprise.upload.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder
public class ValidationResultResponse {
    private Long id;
    private String uploadId;
    private Integer validationRun;
    private String overallStatus;
    private BigDecimal qualityScore;
    private String virusScanStatus;
    private Integer totalTablesFound;
    private Integer totalTablesValid;
    private Long totalRows;
    private Long totalValidRows;
    private Long totalInvalidRows;
    private Integer totalWarnings;
    private Integer blockerCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationMs;
    private List<TableResultDetail> tableResults;

    @Data @Builder
    public static class TableResultDetail {
        private Long id;
        private String tableKey;
        private String tableName;
        private String status;
        private BigDecimal qualityScore;
        private Long rowCount;
        private Long validRows;
        private Long invalidRows;
        private Integer rulesChecked;
        private Integer rulesPassed;
        private Integer rulesFailed;
        private Integer blockerCount;
        private Map<String, Object> columnStats;
        private List<Map<String, Object>> sampleData;
        private List<RuleResultDetail> ruleResults;
    }

    @Data @Builder
    public static class RuleResultDetail {
        private String ruleCode;
        private String ruleType;
        private String severity;
        private String targetColumn;
        private Boolean passed;
        private Long affectedRows;
        private BigDecimal affectedPercent;
        private String errorMessage;
        private List<Map<String, Object>> errorSamples;
    }
}
