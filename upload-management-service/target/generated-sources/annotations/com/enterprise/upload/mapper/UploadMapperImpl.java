package com.enterprise.upload.mapper;

import com.enterprise.upload.dto.response.DatasetConfigResponse;
import com.enterprise.upload.dto.response.UploadResponse;
import com.enterprise.upload.dto.response.ValidationResultResponse;
import com.enterprise.upload.model.DatasetConfig;
import com.enterprise.upload.model.DatasetTable;
import com.enterprise.upload.model.Upload;
import com.enterprise.upload.model.ValidationDatasetResult;
import com.enterprise.upload.model.ValidationRuleResult;
import com.enterprise.upload.model.ValidationTableResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-17T21:10:31+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UploadMapperImpl implements UploadMapper {

    @Override
    public UploadResponse toResponse(Upload upload) {
        if ( upload == null ) {
            return null;
        }

        UploadResponse.UploadResponseBuilder uploadResponse = UploadResponse.builder();

        uploadResponse.approvedAt( upload.getApprovedAt() );
        uploadResponse.approvedBy( upload.getApprovedBy() );
        uploadResponse.bucketName( upload.getBucketName() );
        uploadResponse.completedAt( upload.getCompletedAt() );
        uploadResponse.createdAt( upload.getCreatedAt() );
        uploadResponse.datasetType( upload.getDatasetType() );
        uploadResponse.departmentId( upload.getDepartmentId() );
        uploadResponse.description( upload.getDescription() );
        uploadResponse.fileName( upload.getFileName() );
        uploadResponse.fileSize( upload.getFileSize() );
        uploadResponse.fileType( upload.getFileType() );
        uploadResponse.mimeType( upload.getMimeType() );
        uploadResponse.objectKey( upload.getObjectKey() );
        uploadResponse.originalFileName( upload.getOriginalFileName() );
        uploadResponse.presignedUrlExpiresAt( upload.getPresignedUrlExpiresAt() );
        uploadResponse.rejectedAt( upload.getRejectedAt() );
        uploadResponse.rejectedBy( upload.getRejectedBy() );
        uploadResponse.rejectionReason( upload.getRejectionReason() );
        uploadResponse.status( upload.getStatus() );
        Map<String, Object> map = upload.getTags();
        if ( map != null ) {
            uploadResponse.tags( new LinkedHashMap<String, Object>( map ) );
        }
        uploadResponse.targetDatabase( upload.getTargetDatabase() );
        uploadResponse.targetTable( upload.getTargetTable() );
        uploadResponse.updatedAt( upload.getUpdatedAt() );
        uploadResponse.uploadId( upload.getUploadId() );
        uploadResponse.userId( upload.getUserId() );

        uploadResponse.uploadTags( extractTags(upload) );
        uploadResponse.latestValidation( mapLatestValidation(upload) );
        uploadResponse.latestIngestion( mapLatestIngestion(upload) );

        return uploadResponse.build();
    }

    @Override
    public ValidationResultResponse toValidationResponse(ValidationDatasetResult result) {
        if ( result == null ) {
            return null;
        }

        ValidationResultResponse.ValidationResultResponseBuilder validationResultResponse = ValidationResultResponse.builder();

        validationResultResponse.tableResults( validationTableResultListToTableResultDetailList( result.getTableResults() ) );
        validationResultResponse.blockerCount( result.getBlockerCount() );
        validationResultResponse.completedAt( result.getCompletedAt() );
        validationResultResponse.durationMs( result.getDurationMs() );
        validationResultResponse.id( result.getId() );
        validationResultResponse.qualityScore( result.getQualityScore() );
        validationResultResponse.startedAt( result.getStartedAt() );
        validationResultResponse.totalInvalidRows( result.getTotalInvalidRows() );
        validationResultResponse.totalRows( result.getTotalRows() );
        validationResultResponse.totalTablesFound( result.getTotalTablesFound() );
        validationResultResponse.totalTablesValid( result.getTotalTablesValid() );
        validationResultResponse.totalValidRows( result.getTotalValidRows() );
        validationResultResponse.totalWarnings( result.getTotalWarnings() );
        validationResultResponse.validationRun( result.getValidationRun() );

        validationResultResponse.uploadId( result.getUpload().getUploadId() );
        validationResultResponse.overallStatus( result.getOverallStatus() != null ? result.getOverallStatus().name() : null );
        validationResultResponse.virusScanStatus( result.getVirusScanStatus() != null ? result.getVirusScanStatus().name() : null );

        return validationResultResponse.build();
    }

    @Override
    public ValidationResultResponse.TableResultDetail toTableDetail(ValidationTableResult r) {
        if ( r == null ) {
            return null;
        }

        ValidationResultResponse.TableResultDetail.TableResultDetailBuilder tableResultDetail = ValidationResultResponse.TableResultDetail.builder();

        tableResultDetail.blockerCount( r.getBlockerCount() );
        Map<String, Object> map = r.getColumnStats();
        if ( map != null ) {
            tableResultDetail.columnStats( new LinkedHashMap<String, Object>( map ) );
        }
        tableResultDetail.id( r.getId() );
        tableResultDetail.invalidRows( r.getInvalidRows() );
        tableResultDetail.qualityScore( r.getQualityScore() );
        tableResultDetail.rowCount( r.getRowCount() );
        tableResultDetail.ruleResults( validationRuleResultListToRuleResultDetailList( r.getRuleResults() ) );
        tableResultDetail.rulesChecked( r.getRulesChecked() );
        tableResultDetail.rulesFailed( r.getRulesFailed() );
        tableResultDetail.rulesPassed( r.getRulesPassed() );
        List<Map<String, Object>> list1 = r.getSampleData();
        if ( list1 != null ) {
            tableResultDetail.sampleData( new ArrayList<Map<String, Object>>( list1 ) );
        }
        tableResultDetail.tableKey( r.getTableKey() );
        tableResultDetail.tableName( r.getTableName() );
        tableResultDetail.validRows( r.getValidRows() );

        tableResultDetail.status( r.getStatus() != null ? r.getStatus().name() : null );

        return tableResultDetail.build();
    }

    @Override
    public ValidationResultResponse.RuleResultDetail toRuleDetail(ValidationRuleResult r) {
        if ( r == null ) {
            return null;
        }

        ValidationResultResponse.RuleResultDetail.RuleResultDetailBuilder ruleResultDetail = ValidationResultResponse.RuleResultDetail.builder();

        ruleResultDetail.affectedPercent( r.getAffectedPercent() );
        ruleResultDetail.affectedRows( r.getAffectedRows() );
        ruleResultDetail.errorMessage( r.getErrorMessage() );
        List<Map<String, Object>> list = r.getErrorSamples();
        if ( list != null ) {
            ruleResultDetail.errorSamples( new ArrayList<Map<String, Object>>( list ) );
        }
        ruleResultDetail.passed( r.getPassed() );
        ruleResultDetail.ruleCode( r.getRuleCode() );
        ruleResultDetail.targetColumn( r.getTargetColumn() );

        ruleResultDetail.ruleType( r.getRuleType()  != null ? r.getRuleType().name()  : null );
        ruleResultDetail.severity( r.getSeverity()  != null ? r.getSeverity().name()  : null );

        return ruleResultDetail.build();
    }

    @Override
    public DatasetConfigResponse toDatasetConfigResponse(DatasetConfig config) {
        if ( config == null ) {
            return null;
        }

        DatasetConfigResponse.DatasetConfigResponseBuilder datasetConfigResponse = DatasetConfigResponse.builder();

        datasetConfigResponse.tables( datasetTableListToTableInfoList( config.getTables() ) );
        datasetConfigResponse.code( config.getCode() );
        datasetConfigResponse.defaultTargetDbType( config.getDefaultTargetDbType() );
        datasetConfigResponse.description( config.getDescription() );
        datasetConfigResponse.id( config.getId() );
        datasetConfigResponse.isMultiTable( config.getIsMultiTable() );
        datasetConfigResponse.isSensitive( config.getIsSensitive() );
        datasetConfigResponse.maxFileSizeMb( config.getMaxFileSizeMb() );
        datasetConfigResponse.minQualityScore( config.getMinQualityScore() );
        datasetConfigResponse.name( config.getName() );
        datasetConfigResponse.regulatoryRef( config.getRegulatoryRef() );
        datasetConfigResponse.requiresApproval( config.getRequiresApproval() );
        datasetConfigResponse.structureType( config.getStructureType() );

        datasetConfigResponse.supportedFormats( splitFormats(config.getSupportedFormats()) );

        return datasetConfigResponse.build();
    }

    @Override
    public DatasetConfigResponse.TableInfo toTableInfo(DatasetTable t) {
        if ( t == null ) {
            return null;
        }

        DatasetConfigResponse.TableInfo.TableInfoBuilder tableInfo = DatasetConfigResponse.TableInfo.builder();

        tableInfo.description( t.getDescription() );
        tableInfo.id( t.getId() );
        tableInfo.ingestOrder( t.getIngestOrder() );
        tableInfo.isRequired( t.getIsRequired() );
        tableInfo.sourcePath( t.getSourcePath() );
        tableInfo.tableKey( t.getTableKey() );
        tableInfo.tableName( t.getTableName() );
        tableInfo.targetTable( t.getTargetTable() );

        tableInfo.writeMode( t.getWriteMode() != null ? t.getWriteMode().name() : null );
        tableInfo.columnCount( t.getColumns() != null ? t.getColumns().size() : 0 );
        tableInfo.ruleCount( t.getValidationRules() != null ? t.getValidationRules().size() : 0 );

        return tableInfo.build();
    }

    protected List<ValidationResultResponse.TableResultDetail> validationTableResultListToTableResultDetailList(List<ValidationTableResult> list) {
        if ( list == null ) {
            return null;
        }

        List<ValidationResultResponse.TableResultDetail> list1 = new ArrayList<ValidationResultResponse.TableResultDetail>( list.size() );
        for ( ValidationTableResult validationTableResult : list ) {
            list1.add( toTableDetail( validationTableResult ) );
        }

        return list1;
    }

    protected List<ValidationResultResponse.RuleResultDetail> validationRuleResultListToRuleResultDetailList(List<ValidationRuleResult> list) {
        if ( list == null ) {
            return null;
        }

        List<ValidationResultResponse.RuleResultDetail> list1 = new ArrayList<ValidationResultResponse.RuleResultDetail>( list.size() );
        for ( ValidationRuleResult validationRuleResult : list ) {
            list1.add( toRuleDetail( validationRuleResult ) );
        }

        return list1;
    }

    protected List<DatasetConfigResponse.TableInfo> datasetTableListToTableInfoList(List<DatasetTable> list) {
        if ( list == null ) {
            return null;
        }

        List<DatasetConfigResponse.TableInfo> list1 = new ArrayList<DatasetConfigResponse.TableInfo>( list.size() );
        for ( DatasetTable datasetTable : list ) {
            list1.add( toTableInfo( datasetTable ) );
        }

        return list1;
    }
}
