import io
import logging
from typing import Dict, Any
import pandas as pd
from app.validators.base_validator import BaseValidator

logger = logging.getLogger(__name__)


class ParquetValidator(BaseValidator):
    """Parquet File Validator"""
    
    def validate(self, file_data: bytes, rules: Dict[str, Any]) -> Dict[str, Any]:
        """Validate Parquet file"""
        try:
            # Read Parquet file
            df = pd.read_parquet(io.BytesIO(file_data))
            
            # Use CSV validator logic for data validation
            from app.validators.csv_validator import CSVValidator
            csv_validator = CSVValidator(self.minio_client, self.bucket_name)
            
            # Create temporary CSV for validation
            csv_buffer = io.BytesIO()
            df.to_csv(csv_buffer, index=False)
            csv_data = csv_buffer.getvalue()
            
            # Validate using CSV validator
            result = csv_validator.validate(csv_data, rules)
            result["file_size_mb"] = len(file_data) / (1024 * 1024)
            
            return result
        
        except Exception as e:
            logger.error(f"Parquet validation error: {str(e)}", exc_info=True)
            return {
                "status": "ERROR",
                "errors": [f"Parquet validation error: {str(e)}"]
            }