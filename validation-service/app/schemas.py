from pydantic import BaseModel, Field
from typing import Optional, Dict, List, Any
from datetime import datetime
from enum import Enum


class ValidationStatus(str, Enum):
    """Validation Status Enum"""
    PENDING = "PENDING"
    VALIDATING = "VALIDATING"
    VALID = "VALID"
    INVALID = "INVALID"
    ERROR = "ERROR"


class UploadEvent(BaseModel):
    """Upload Completed Event"""
    uploadId: str
    objectKey: str
    datasetType: str
    userId: str
    department: Optional[str] = None
    targetDatabase: Optional[str] = None


class ValidationRequest(BaseModel):
    """Validation Request"""
    upload_id: str = Field(..., description="Upload ID")
    object_key: str = Field(..., description="MinIO object key")
    dataset_type: str = Field(..., description="Dataset type")


class ValidationResultResponse(BaseModel):
    """Validation Result Response"""
    id: int
    upload_id: str
    status: ValidationStatus
    dataset_type: str
    row_count: Optional[int] = None
    column_count: Optional[int] = None
    file_size_mb: Optional[float] = None
    schema_valid: Optional[bool] = None
    schema_errors: Optional[List[str]] = None
    data_quality_score: Optional[int] = None
    errors: Optional[List[str]] = None
    warnings: Optional[List[str]] = None
    validation_summary: Optional[str] = None
    processing_time_seconds: Optional[int] = None
    created_at: datetime
    completed_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True


class ValidationStats(BaseModel):
    """Validation Statistics"""
    total_validations: int
    valid_count: int
    invalid_count: int
    error_count: int
    avg_processing_time: float
    success_rate: float