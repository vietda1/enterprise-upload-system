package com.enterprise.upload.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class DatasetConfigResponse {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private List<String> supportedFormats;
    private Integer maxFileSizeMb;
    private Boolean isMultiTable;
    private String structureType;
    private String defaultTargetDbType;
    private Boolean requiresApproval;
    private String regulatoryRef;
    private BigDecimal minQualityScore;
    private Boolean isSensitive;
    private List<TableInfo> tables;

    @Data @Builder
    public static class TableInfo {
        private Integer id;
        private String tableKey;
        private String tableName;
        private String description;
        private String sourcePath;
        private String targetTable;
        private String writeMode;
        private Integer ingestOrder;
        private Boolean isRequired;
        private Integer columnCount;
        private Integer ruleCount;
    }
}
