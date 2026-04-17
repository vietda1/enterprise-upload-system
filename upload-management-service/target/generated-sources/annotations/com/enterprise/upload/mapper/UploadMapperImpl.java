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
    date = "2026-04-17T21:50:06+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class UploadMapperImpl implements UploadMapper {

    @Override
    public UploadResponse toResponse(Upload upload) {
        if ( upload == null ) {
            return null;
        }

        UploadResponse.UploadResponseBuilder uploadResponse = UploadResponse.builder();

        uploadResponse.uploadId( upload.getUploadId() );
        uploadResponse.userId( upload.getUserId() );
        uploadResponse.departmentId( upload.getDepartmentId() );
        uploadResponse.fileName( upload.getFileName() );
        uploadResponse.originalFileName( upload.getOriginalFileName() );
        uploadResponse.fileSize( upload.getFileSize() );
        uploadResponse.fileType( upload.getFileType() );
        uploadResponse.mimeType( upload.getMimeType() );
        uploadResponse.objectKey( upload.getObjectKey() );
        uploadResponse.bucketName( upload.getBucketName() );
        uploadResponse.datasetType( upload.getDatasetType() );
        uploadResponse.targetDatabase( upload.getTargetDatabase() );
        uploadResponse.targetTable( upload.getTargetTable() );
        uploadResponse.description( upload.getDescription() );
        Map<String, Object> map = upload.getTags();
        if ( map != null ) {
            uploadResponse.tags( new LinkedHashMap<String, Object>( map ) );
        }
        uploadResponse.status( upload.getStatus() );
        uploadResponse.rejectionReason( upload.getRejectionReason() );
        uploadResponse.rejectedBy( upload.getRejectedBy() );
        uploadResponse.rejectedAt( upload.getRejectedAt() );
        uploadResponse.approvedBy( upload.getApprovedBy() );
        uploadResponse.approvedAt( upload.getApprovedAt() );
        uploadResponse.presignedUrlExpiresAt( upload.getPresignedUrlExpiresAt() );
        uploadResponse.createdAt( upload.getCreatedAt() );
        uploadResponse.updatedAt( upload.getUpdatedAt() );
        uploadResponse.completedAt( upload.getCompletedAt() );

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
        validationResultResponse.id( result.getId() );
        validationResultResponse.validationRun( result.getValidationRun() );
        validationResultResponse.qualityScore( result.getQualityScore() );
        validationResultResponse.totalTablesFound( result.getTotalTablesFound() );
        validationResultResponse.totalTablesValid( result.getTotalTablesValid() );
        validationResultResponse.totalRows( result.getTotalRows() );
        validationResultResponse.totalValidRows( result.getTotalValidRows() );
        validationResultResponse.totalInvalidRows( result.getTotalInvalidRows() );
        validationResultResponse.totalWarnings( result.getTotalWarnings() );
        validationResultResponse.blockerCount( result.getBlockerCount() );
        validationResultResponse.startedAt( result.getStartedAt() );
        validationResultResponse.completedAt( result.getCompletedAt() );
        validationResultResponse.durationMs( result.getDurationMs() );

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

        tableResultDetail.id( r.getId() );
        tableResultDetail.tableKey( r.getTableKey() );
        tableResultDetail.tableName( r.getTableName() );
        tableResultDetail.qualityScore( r.getQualityScore() );
        tableResultDetail.rowCount( r.getRowCount() );
        tableResultDetail.validRows( r.getValidRows() );
        tableResultDetail.invalidRows( r.getInvalidRows() );
        tableResultDetail.rulesChecked( r.getRulesChecked() );
        tableResultDetail.rulesPassed( r.getRulesPassed() );
        tableResultDetail.rulesFailed( r.getRulesFailed() );
        tableResultDetail.blockerCount( r.getBlockerCount() );
        Map<String, Object> map = r.getColumnStats();
        if ( map != null ) {
            tableResultDetail.columnStats( new LinkedHashMap<String, Object>( map ) );
        }
        List<Map<String, Object>> list = r.getSampleData();
        if ( list != null ) {
            tableResultDetail.sampleData( new ArrayList<Map<String, Object>>( list ) );
        }
        tableResultDetail.ruleResults( validationRuleResultListToRuleResultDetailList( r.getRuleResults() ) );

        tableResultDetail.status( r.getStatus() != null ? r.getStatus().name() : null );

        return tableResultDetail.build();
    }

    @Override
    public ValidationResultResponse.RuleResultDetail toRuleDetail(ValidationRuleResult r) {
        if ( r == null ) {
            return null;
        }

        ValidationResultResponse.RuleResultDetail.RuleResultDetailBuilder ruleResultDetail = ValidationResultResponse.RuleResultDetail.builder();

        ruleResultDetail.ruleCode( r.getRuleCode() );
        ruleResultDetail.targetColumn( r.getTargetColumn() );
        ruleResultDetail.passed( r.getPassed() );
        ruleResultDetail.affectedRows( r.getAffectedRows() );
        ruleResultDetail.affectedPercent( r.getAffectedPercent() );
        ruleResultDetail.errorMessage( r.getErrorMessage() );
        List<Map<String, Object>> list = r.getErrorSamples();
        if ( list != null ) {
            ruleResultDetail.errorSamples( new ArrayList<Map<String, Object>>( list ) );
        }

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
        datasetConfigResponse.id( config.getId() );
        datasetConfigResponse.code( config.getCode() );
        datasetConfigResponse.name( config.getName() );
        datasetConfigResponse.description( config.getDescription() );
        datasetConfigResponse.maxFileSizeMb( config.getMaxFileSizeMb() );
        datasetConfigResponse.isMultiTable( config.getIsMultiTable() );
        datasetConfigResponse.structureType( config.getStructureType() );
        datasetConfigResponse.defaultTargetDbType( config.getDefaultTargetDbType() );
        datasetConfigResponse.requiresApproval( config.getRequiresApproval() );
        datasetConfigResponse.regulatoryRef( config.getRegulatoryRef() );
        datasetConfigResponse.minQualityScore( config.getMinQualityScore() );
        datasetConfigResponse.isSensitive( config.getIsSensitive() );

        datasetConfigResponse.supportedFormats( splitFormats(config.getSupportedFormats()) );

        return datasetConfigResponse.build();
    }

    @Override
    public DatasetConfigResponse.TableInfo toTableInfo(DatasetTable t) {
        if ( t == null ) {
            return null;
        }

        DatasetConfigResponse.TableInfo.TableInfoBuilder tableInfo = DatasetConfigResponse.TableInfo.builder();

        tableInfo.id( t.getId() );
        tableInfo.tableKey( t.getTableKey() );
        tableInfo.tableName( t.getTableName() );
        tableInfo.description( t.getDescription() );
        tableInfo.sourcePath( t.getSourcePath() );
        tableInfo.targetTable( t.getTargetTable() );
        tableInfo.ingestOrder( t.getIngestOrder() );
        tableInfo.isRequired( t.getIsRequired() );

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
