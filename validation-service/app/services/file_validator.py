import logging
from typing import Dict, Any
from minio import Minio
from app.validators.csv_validator import CSVValidator
from app.validators.json_validator import JSONValidator
from app.validators.parquet_validator import ParquetValidator
from app.rules.validation_rules import DatasetValidationRules

logger = logging.getLogger(__name__)


class FileValidator:
    """Main File Validator Service"""
    
    def __init__(self, minio_client: Minio, bucket_name: str):
        self.minio_client = minio_client
        self.bucket_name = bucket_name
        
        # Initialize validators
        self.csv_validator = CSVValidator(minio_client, bucket_name)
        self.json_validator = JSONValidator(minio_client, bucket_name)
        self.parquet_validator = ParquetValidator(minio_client, bucket_name)
    
    def validate_file(self, object_key: str, dataset_type: str) -> Dict[str, Any]:
        """Main validation orchestrator"""
        try:
            logger.info(f"Starting validation for {object_key} ({dataset_type})")
            
            # Download file
            file_data = self._download_file(object_key)
            
            # Get validation rules
            rules = DatasetValidationRules.get_rules(dataset_type)
            if not rules:
                return {
                    "status": "ERROR",
                    "errors": [f"No validation rules found for dataset type: {dataset_type}"]
                }
            
            # Determine file type and validate
            if object_key.endswith('.csv'):
                result = self.csv_validator.validate(file_data, rules)
            elif object_key.endswith('.json'):
                result = self.json_validator.validate(file_data, rules)
            elif object_key.endswith('.parquet'):
                result = self.parquet_validator.validate(file_data, rules)
            else:
                return {
                    "status": "ERROR",
                    "errors": ["Unsupported file format"]
                }
            
            logger.info(f"Validation completed for {object_key}: {result['status']}")
            return result
        
        except Exception as e:
            logger.error(f"Validation failed for {object_key}: {str(e)}", exc_info=True)
            return {
                "status": "ERROR",
                "errors": [f"Validation failed: {str(e)}"]
            }
    
    def _download_file(self, object_key: str) -> bytes:
        """Download file from MinIO"""
        response = self.minio_client.get_object(self.bucket_name, object_key)
        data = response.read()
        response.close()
        response.release_conn()
        return data