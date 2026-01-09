import pandas as pd
import json
from typing import Dict, List, Tuple, Any
from minio import Minio
import io

class FileValidator:
    
    def __init__(self, minio_client: Minio, bucket_name: str):
        self.minio_client = minio_client
        self.bucket_name = bucket_name
    
    def validate_file(self, object_key: str, dataset_type: str) -> Dict[str, Any]:
        """Main validation orchestrator"""
        
        try:
            # Download file from MinIO
            file_data = self._download_file(object_key)
            
            # Determine file type and validate accordingly
            if object_key.endswith('.csv'):
                return self._validate_csv(file_data, dataset_type)
            elif object_key.endswith('.json'):
                return self._validate_json(file_data, dataset_type)
            elif object_key.endswith('.parquet'):
                return self._validate_parquet(file_data, dataset_type)
            else:
                return {
                    "status": "ERROR",
                    "errors": ["Unsupported file format"]
                }
        
        except Exception as e:
            return {
                "status": "ERROR",
                "errors": [f"Validation failed: {str(e)}"]
            }
    
    def _download_file(self, object_key: str) -> bytes:
        """Download file from MinIO"""
        response = self.minio_client.get_object(self.bucket_name, object_key)
        return response.read()
    
    def _validate_csv(self, file_data: bytes, dataset_type: str) -> Dict[str, Any]:
        """Validate CSV file"""
        
        errors = []
        warnings = []
        
        try:
            # Read CSV
            df = pd.read_csv(io.BytesIO(file_data))
            
            # Get validation rules
            rules = DatasetValidationRules.get_rules(dataset_type)
            
            # Basic stats
            row_count = len(df)
            column_count = len(df.columns)
            file_size_mb = len(file_data) / (1024 * 1024)
            
            # Validate required columns
            required_cols = rules.get("required_columns", [])
            missing_cols = set(required_cols) - set(df.columns)
            if missing_cols:
                errors.append(f"Missing required columns: {missing_cols}")
            
            # Validate column types
            schema_errors = []
            column_types = rules.get("column_types", {})
            for col, expected_type in column_types.items():
                if col in df.columns:
                    if not self._check_column_type(df[col], expected_type):
                        schema_errors.append(
                            f"Column '{col}' has invalid type. Expected: {expected_type}"
                        )
            
            # Check for nulls
            null_counts = df.isnull().sum().to_dict()
            for col, null_count in null_counts.items():
                if null_count > 0:
                    warnings.append(f"Column '{col}' has {null_count} null values")
            
            # Check for duplicates
            duplicate_count = df.duplicated().sum()
            if duplicate_count > 0:
                warnings.append(f"Found {duplicate_count} duplicate rows")
            
            # Validate constraints
            constraints = rules.get("constraints", {})
            for col, constraint in constraints.items():
                if col in df.columns:
                    if "unique" in constraint and constraint["unique"]:
                        if df[col].duplicated().any():
                            errors.append(f"Column '{col}' should be unique but has duplicates")
                    
                    if "min" in constraint:
                        if (df[col] < constraint["min"]).any():
                            errors.append(f"Column '{col}' has values below minimum {constraint['min']}")
                    
                    if "max" in constraint:
                        if (df[col] > constraint["max"]).any():
                            errors.append(f"Column '{col}' has values above maximum {constraint['max']}")
            
            # Calculate data quality score
            quality_score = self._calculate_quality_score(
                df, errors, warnings, null_counts
            )
            
            # Determine status
            status = "VALID" if not errors else "INVALID"
            
            return {
                "status": status,
                "row_count": row_count,
                "column_count": column_count,
                "file_size_mb": round(file_size_mb, 2),
                "schema_valid": len(schema_errors) == 0,
                "schema_errors": schema_errors,
                "null_counts": {k: int(v) for k, v in null_counts.items()},
                "duplicate_count": int(duplicate_count),
                "data_quality_score": quality_score,
                "errors": errors,
                "warnings": warnings,
                "validation_summary": self._generate_summary(
                    status, row_count, errors, warnings
                )
            }
        
        except Exception as e:
            return {
                "status": "ERROR",
                "errors": [f"CSV validation error: {str(e)}"]
            }
    
    def _validate_json(self, file_data: bytes, dataset_type: str) -> Dict[str, Any]:
        """Validate JSON file"""
        
        try:
            data = json.loads(file_data)
            
            rules = DatasetValidationRules.get_rules(dataset_type)
            required_fields = rules.get("required_fields", [])
            
            errors = []
            
            # Handle array of objects
            if isinstance(data, list):
                row_count = len(data)
                for idx, record in enumerate(data):
                    missing = set(required_fields) - set(record.keys())
                    if missing:
                        errors.append(f"Record {idx}: Missing fields {missing}")
            else:
                row_count = 1
                missing = set(required_fields) - set(data.keys())
                if missing:
                    errors.append(f"Missing required fields: {missing}")
            
            status = "VALID" if not errors else "INVALID"
            
            return {
                "status": status,
                "row_count": row_count,
                "file_size_mb": round(len(file_data) / (1024 * 1024), 2),
                "errors": errors,
                "warnings": [],
                "validation_summary": f"Validated {row_count} JSON records"
            }
        
        except Exception as e:
            return {
                "status": "ERROR",
                "errors": [f"JSON validation error: {str(e)}"]
            }
    
    def _validate_parquet(self, file_data: bytes, dataset_type: str) -> Dict[str, Any]:
        """Validate Parquet file"""
        
        try:
            df = pd.read_parquet(io.BytesIO(file_data))
            
            # Similar validation as CSV
            return self._validate_csv(file_data, dataset_type)
        
        except Exception as e:
            return {
                "status": "ERROR",
                "errors": [f"Parquet validation error: {str(e)}"]
            }
    
    def _check_column_type(self, series: pd.Series, expected_type: str) -> bool:
        """Check if column matches expected type"""
        
        if expected_type == "numeric":
            return pd.api.types.is_numeric_dtype(series)
        elif expected_type == "string":
            return pd.api.types.is_string_dtype(series) or pd.api.types.is_object_dtype(series)
        elif expected_type == "datetime":
            return pd.api.types.is_datetime64_any_dtype(series)
        return True
    
    def _calculate_quality_score(self, df: pd.DataFrame, errors: List, 
                                 warnings: List, null_counts: Dict) -> int:
        """Calculate data quality score (0-100)"""
        
        score = 100
        
        # Deduct for errors
        score -= len(errors) * 20
        
        # Deduct for warnings
        score -= len(warnings) * 5
        
        # Deduct for null values
        total_nulls = sum(null_counts.values())
        total_cells = df.shape[0] * df.shape[1]
        null_percentage = (total_nulls / total_cells) * 100 if total_cells > 0 else 0
        score -= int(null_percentage)
        
        return max(0, min(100, score))
    
    def _generate_summary(self, status: str, row_count: int, 
                         errors: List, warnings: List) -> str:
        """Generate validation summary"""
        
        summary = f"Validation Status: {status}\n"
        summary += f"Total Rows: {row_count}\n"
        summary += f"Errors: {len(errors)}\n"
        summary += f"Warnings: {len(warnings)}\n"
        
        if errors:
            summary += "\nCritical Issues:\n"
            for error in errors[:5]:  # Show first 5 errors
                summary += f"- {error}\n"
        
        return summary