from sqlalchemy import Column, Integer, String, DateTime, Text, JSON, BigInteger
from datetime import datetime
from app.database import Base


class ValidationResult(Base):
    """Validation Result Model"""
    
    __tablename__ = "validation_results"
    
    id = Column(Integer, primary_key=True, index=True)
    upload_id = Column(String(36), unique=True, index=True, nullable=False)
    status = Column(String(50), nullable=False, index=True)  # PENDING, VALIDATING, VALID, INVALID, ERROR
    dataset_type = Column(String(100), nullable=False)
    
    # Basic stats
    row_count = Column(Integer)
    column_count = Column(Integer)
    file_size_mb = Column(Integer)
    
    # Schema validation
    schema_valid = Column(String(10))
    schema_errors = Column(JSON)
    
    # Data quality
    null_counts = Column(JSON)
    duplicate_count = Column(Integer)
    data_quality_score = Column(Integer)
    
    # Errors and warnings
    errors = Column(JSON)
    warnings = Column(JSON)
    
    # Summary
    validation_summary = Column(Text)
    
    # Processing info
    processing_time_seconds = Column(Integer)
    validator_version = Column(String(20))
    
    # Timestamps
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    started_at = Column(DateTime)
    completed_at = Column(DateTime)
    
    def __repr__(self):
        return f"<ValidationResult(upload_id={self.upload_id}, status={self.status})>"