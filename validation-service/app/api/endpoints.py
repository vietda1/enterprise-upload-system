from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from typing import List
from minio import Minio

from app.database import get_db
from app.schemas import (
    ValidationRequest, 
    ValidationResultResponse, 
    ValidationStats,
    ValidationStatus
)
from app.services.validation_service import ValidationService
from app.services.kafka_service import KafkaService
from app.config import settings

router = APIRouter(prefix="/validations", tags=["Validation"])

# Dependency to get MinIO client
def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure
    )

# Dependency to get Kafka service
def get_kafka_service() -> KafkaService:
    return KafkaService()


@router.post("/trigger", status_code=202)
async def trigger_validation(
    request: ValidationRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    minio_client: Minio = Depends(get_minio_client),
    kafka_service: KafkaService = Depends(get_kafka_service)
):
    """Trigger file validation"""
    validation_service = ValidationService(db, minio_client, kafka_service)
    
    background_tasks.add_task(
        validation_service.validate_upload,
        request.upload_id,
        request.object_key,
        request.dataset_type
    )
    
    return {
        "message": "Validation triggered",
        "upload_id": request.upload_id
    }


@router.get("/{upload_id}", response_model=ValidationResultResponse)
async def get_validation_result(
    upload_id: str,
    db: Session = Depends(get_db),
    minio_client: Minio = Depends(get_minio_client),
    kafka_service: KafkaService = Depends(get_kafka_service)
):
    """Get validation result"""
    validation_service = ValidationService(db, minio_client, kafka_service)
    
    result = validation_service.get_validation_result(upload_id)
    if not result:
        raise HTTPException(status_code=404, detail="Validation not found")
    
    return result


@router.post("/{upload_id}/retry", status_code=202)
async def retry_validation(
    upload_id: str,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    minio_client: Minio = Depends(get_minio_client),
    kafka_service: KafkaService = Depends(get_kafka_service)
):
    """Retry validation"""
    validation_service = ValidationService(db, minio_client, kafka_service)
    
    try:
        validation_service.retry_validation(upload_id)
        
        # Get validation to extract object_key and dataset_type
        validation = validation_service.get_validation_result(upload_id)
        
        background_tasks.add_task(
            validation_service.validate_upload,
            upload_id,
            validation.object_key,
            validation.dataset_type
        )
        
        return {"message": "Validation retry triggered", "upload_id": upload_id}
    
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/", response_model=List[ValidationResultResponse])
async def list_validations(
    status: str = None,
    limit: int = 20,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """List validation results"""
    query = db.query(ValidationResult)
    
    if status:
        query = query.filter(ValidationResult.status == status)
    
    results = query.order_by(
        ValidationResult.created_at.desc()
    ).limit(limit).offset(offset).all()
    
    return results


@router.get("/stats/summary", response_model=ValidationStats)
async def get_validation_stats(db: Session = Depends(get_db)):
    """Get validation statistics"""
    from sqlalchemy import func
    
    total = db.query(func.count(ValidationResult.id)).scalar()
    valid = db.query(func.count(ValidationResult.id)).filter(
        ValidationResult.status == ValidationStatus.VALID.value
    ).scalar()
    invalid = db.query(func.count(ValidationResult.id)).filter(
        ValidationResult.status == ValidationStatus.INVALID.value
    ).scalar()
    error = db.query(func.count(ValidationResult.id)).filter(
        ValidationResult.status == ValidationStatus.ERROR.value
    ).scalar()
    
    avg_time = db.query(
        func.avg(ValidationResult.processing_time_seconds)
    ).scalar() or 0
    
    success_rate = (valid / total * 100) if total > 0 else 0
    
    return ValidationStats(
        total_validations=total,
        valid_count=valid,
        invalid_count=invalid,
        error_count=error,
        avg_processing_time=round(avg_time, 2),
        success_rate=round(success_rate, 2)
    )