import logging
from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session
from minio import Minio

from app.models import ValidationResult
from app.schemas import ValidationStatus
from app.services.file_validator import FileValidator
from app.services.kafka_service import KafkaService
from app.config import settings

logger = logging.getLogger(__name__)


class ValidationService:
    """Validation Service"""
    
    def __init__(
        self, 
        db: Session, 
        minio_client: Minio, 
        kafka_service: KafkaService
    ):
        self.db = db
        self.file_validator = FileValidator(minio_client, settings.minio_bucket)
        self.kafka_service = kafka_service
    
    def validate_upload(self, upload_id: str, object_key: str, dataset_type: str):
        """Validate uploaded file"""
        logger.info(f"Starting validation for upload: {upload_id}")
        
        # Create validation record
        validation = ValidationResult(
            upload_id=upload_id,
            status=ValidationStatus.VALIDATING.value,
            dataset_type=dataset_type,
            created_at=datetime.now(datetime.timezone.utc),
            started_at=datetime.now(datetime.timezone.utc),
            validator_version=settings.app_version
        )
        self.db.add(validation)
        self.db.commit()
        
        start_time = datetime.now(datetime.timezone.utc),
        
        try:
            # Perform validation
            result = self.file_validator.validate_file(object_key, dataset_type)
            
            # Calculate processing time
            processing_time = (datetime.now(datetime.timezone.utc) - start_time).seconds
            
            # Update validation record
            validation.status = result["status"]
            validation.row_count = result.get("row_count")
            validation.column_count = result.get("column_count")
            validation.file_size_mb = result.get("file_size_mb")
            validation.schema_valid = str(result.get("schema_valid", False))
            validation.schema_errors = result.get("schema_errors", [])
            validation.null_counts = result.get("null_counts", {})
            validation.duplicate_count = result.get("duplicate_count", 0)
            validation.data_quality_score = result.get("data_quality_score", 0)
            validation.errors = result.get("errors", [])
            validation.warnings = result.get("warnings", [])
            validation.validation_summary = result.get("validation_summary", "")
            validation.processing_time_seconds = processing_time
            validation.completed_at = datetime.now(datetime.timezone.utc)
            
            self.db.commit()
            
            # Publish result to Kafka
            self._publish_validation_result(upload_id, result["status"], result)
            
            logger.info(f"Validation completed for {upload_id}: {result['status']}")
        
        except Exception as e:
            logger.error(f"Validation failed for {upload_id}: {str(e)}", exc_info=True)
            
            validation.status = ValidationStatus.ERROR.value
            validation.errors = [str(e)]
            validation.completed_at = datetime.now(datetime.timezone.utc)
            self.db.commit()
            
            self._publish_validation_result(upload_id, ValidationStatus.ERROR.value, {"errors": [str(e)]})
    
    def _publish_validation_result(self, upload_id: str, status: str, result: dict):
        """Publish validation result to Kafka"""
        message = {
            "uploadId": upload_id,
            "status": status,
            "dataQualityScore": result.get("data_quality_score", 0),
            "errors": result.get("errors", []),
            "timestamp": datetime.now(datetime.timezone.utc).isoformat()
        }
        
        self.kafka_service.publish_message(
            settings.kafka_topic_validation_completed,
            message
        )
    
    def get_validation_result(self, upload_id: str) -> Optional[ValidationResult]:
        """Get validation result"""
        return self.db.query(ValidationResult).filter(
            ValidationResult.upload_id == upload_id
        ).first()
    
    def retry_validation(self, upload_id: str):
        """Retry validation"""
        validation = self.get_validation_result(upload_id)
        if not validation:
            raise ValueError(f"Validation not found for upload: {upload_id}")
        
        # Reset status
        validation.status = ValidationStatus.PENDING.value
        validation.errors = None
        validation.warnings = None
        self.db.commit()
        
        logger.info(f"Validation retry triggered for: {upload_id}")