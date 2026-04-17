package com.enterprise.upload.mapper;

import com.enterprise.upload.dto.response.*;
import com.enterprise.upload.model.*;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UploadMapper {

    // ── Upload → UploadResponse ───────────────────────────────────────────────

    @Mapping(target = "uploadTags",         expression = "java(extractTags(upload))")
    @Mapping(target = "latestValidation",   expression = "java(mapLatestValidation(upload))")
    @Mapping(target = "latestIngestion",    expression = "java(mapLatestIngestion(upload))")
    UploadResponse toResponse(Upload upload);

    default List<String> extractTags(Upload upload) {
        if (upload.getUploadTags() == null) return List.of();
        return upload.getUploadTags().stream().map(UploadTag::getTag).toList();
    }

    default UploadResponse.ValidationSummary mapLatestValidation(Upload upload) {
        if (upload.getValidationResults() == null || upload.getValidationResults().isEmpty()) return null;
        ValidationDatasetResult r = upload.getValidationResults().get(0);
        return UploadResponse.ValidationSummary.builder()
            .id(r.getId())
            .overallStatus(r.getOverallStatus() != null ? r.getOverallStatus().name() : null)
            .qualityScore(r.getQualityScore() != null ? r.getQualityScore().doubleValue() : null)
            .totalRows(r.getTotalRows())
            .totalValidRows(r.getTotalValidRows())
            .totalInvalidRows(r.getTotalInvalidRows())
            .blockerCount(r.getBlockerCount())
            .completedAt(r.getCompletedAt())
            .build();
    }

    default UploadResponse.IngestionSummary mapLatestIngestion(Upload upload) {
        if (upload.getIngestionJobs() == null || upload.getIngestionJobs().isEmpty()) return null;
        IngestionJob j = upload.getIngestionJobs().get(0);
        return UploadResponse.IngestionSummary.builder()
            .jobId(j.getJobId())
            .status(j.getStatus() != null ? j.getStatus().name() : null)
            .rowsInserted(j.getRowsInserted())
            .rowsFailed(j.getRowsFailed())
            .completedAt(j.getCompletedAt())
            .build();
    }

    // ── ValidationDatasetResult → ValidationResultResponse ───────────────────

    @Mapping(target = "uploadId",     expression = "java(result.getUpload().getUploadId())")
    @Mapping(target = "overallStatus", expression = "java(result.getOverallStatus() != null ? result.getOverallStatus().name() : null)")
    @Mapping(target = "virusScanStatus", expression = "java(result.getVirusScanStatus() != null ? result.getVirusScanStatus().name() : null)")
    @Mapping(target = "tableResults", source = "tableResults")
    ValidationResultResponse toValidationResponse(ValidationDatasetResult result);

    @Mapping(target = "status", expression = "java(r.getStatus() != null ? r.getStatus().name() : null)")
    ValidationResultResponse.TableResultDetail toTableDetail(ValidationTableResult r);

    @Mapping(target = "ruleType",  expression = "java(r.getRuleType()  != null ? r.getRuleType().name()  : null)")
    @Mapping(target = "severity",  expression = "java(r.getSeverity()  != null ? r.getSeverity().name()  : null)")
    ValidationResultResponse.RuleResultDetail toRuleDetail(ValidationRuleResult r);

    // ── DatasetConfig → DatasetConfigResponse ────────────────────────────────

    @Mapping(target = "supportedFormats", expression = "java(splitFormats(config.getSupportedFormats()))")
    @Mapping(target = "tables", source = "tables")
    DatasetConfigResponse toDatasetConfigResponse(DatasetConfig config);

    default List<String> splitFormats(String formats) {
        if (formats == null || formats.isBlank()) return List.of();
        return Arrays.asList(formats.split(","));
    }

    @Mapping(target = "writeMode",    expression = "java(t.getWriteMode() != null ? t.getWriteMode().name() : null)")
    @Mapping(target = "columnCount",  expression = "java(t.getColumns() != null ? t.getColumns().size() : 0)")
    @Mapping(target = "ruleCount",    expression = "java(t.getValidationRules() != null ? t.getValidationRules().size() : 0)")
    DatasetConfigResponse.TableInfo toTableInfo(DatasetTable t);
}
