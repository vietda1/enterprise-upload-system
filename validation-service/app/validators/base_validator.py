import io
import logging
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
import pandas as pd
from minio import Minio

logger = logging.getLogger(__name__)


class BaseValidator(ABC):
    """Base Validator Class"""
    
    def __init__(self, minio_client: Minio, bucket_name: str):
        self.minio_client = minio_client
        self.bucket_name = bucket_name
    
    @abstractmethod
    def validate(self, file_data: bytes, rules: Dict[str, Any]) -> Dict[str, Any]:
        """Validate file data"""
        pass
    
    def download_file(self, object_key: str) -> bytes:
        """Download file from MinIO"""
        try:
            response = self.minio_client.get_object(self.bucket_name, object_key)
            data = response.read()
            response.close()
            response.release_conn()
            return data
        except Exception as e:
            logger.error(f"Failed to download file {object_key}: {str(e)}")
            raise
    
    def check_column_type(self, series: pd.Series, expected_type: str) -> bool:
        """Check if column matches expected type"""
        if expected_type == "numeric":
            return pd.api.types.is_numeric_dtype(series)
        elif expected_type == "string":
            return pd.api.types.is_string_dtype(series) or pd.api.types.is_object_dtype(series)
        elif expected_type == "datetime":
            return pd.api.types.is_datetime64_any_dtype(series)
        return True
    
    def calculate_quality_score(
        self, 
        df: Optional[pd.DataFrame], 
        errors: list, 
        warnings: list, 
        null_counts: Optional[Dict] = None
    ) -> int:
        """Calculate data quality score (0-100)"""
        score = 100
        
        # Deduct for errors (critical)
        score -= len(errors) * 20
        
        # Deduct for warnings
        score -= len(warnings) * 5
        
        # Deduct for null values
        if df is not None and null_counts:
            total_nulls = sum(null_counts.values())
            total_cells = df.shape[0] * df.shape[1]
            null_percentage = (total_nulls / total_cells) * 100 if total_cells > 0 else 0
            score -= int(null_percentage)
        
        return max(0, min(100, score))