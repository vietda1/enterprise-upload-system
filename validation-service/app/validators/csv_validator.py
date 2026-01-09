import io
import re
import logging
from typing import Dict, Any
import pandas as pd
from app.validators.base_validator import BaseValidator

logger = logging.getLogger(__name__)


class CSVValidator(BaseValidator):
    """CSV File Validator"""
    
    def validate(self, file_data: bytes, rules: Dict[str, Any]) -> Dict[str, Any]:
        """Validate CSV file"""
        errors = []
        warnings = []
        
        try:
            # Try reading CSV with different encodings
            df = self._read_csv_with_encoding(file_data)
            
            if df is None:
                return {
                    "status": "ERROR",
                    "errors": ["Failed to read CSV file with any supported encoding"]
                }
            
            # Basic stats
            row_count = len(df)
            column_count = len(df.columns)
            file_size_mb = len(file_data) / (1024 * 1024)
            
            # Clean column names (strip whitespace)
            df.columns = df.columns.str.strip()
            
            # Validate required columns
            required_cols = rules.get("required_columns", [])
            missing_cols = set(required_cols) - set(df.columns)
            if missing_cols:
                errors.append(f"Missing required columns: {', '.join(missing_cols)}")
            
            # Validate column types
            schema_errors = []
            column_types = rules.get("column_types", {})
            for col, expected_type in column_types.items():
                if col in df.columns:
                    if not self.check_column_type(df[col], expected_type):
                        schema_errors.append(
                            f"Column '{col}' has invalid type. Expected: {expected_type}"
                        )
            
            # Check for nulls
            null_counts = df.isnull().sum().to_dict()
            for col, null_count in null_counts.items():
                if null_count > 0:
                    null_percentage = (null_count / len(df)) * 100
                    if null_percentage > 50:
                        errors.append(f"Column '{col}' has {null_percentage:.1f}% null values")
                    elif null_percentage > 10:
                        warnings.append(f"Column '{col}' has {null_percentage:.1f}% null values")
            
            # Check for duplicates
            duplicate_count = df.duplicated().sum()
            if duplicate_count > 0:
                duplicate_percentage = (duplicate_count / len(df)) * 100
                if duplicate_percentage > 10:
                    errors.append(f"Found {duplicate_count} duplicate rows ({duplicate_percentage:.1f}%)")
                else:
                    warnings.append(f"Found {duplicate_count} duplicate rows ({duplicate_percentage:.1f}%)")
            
            # Validate constraints
            constraints = rules.get("constraints", {})
            for col, constraint in constraints.items():
                if col not in df.columns:
                    continue
                
                # Unique constraint
                if constraint.get("unique"):
                    if df[col].duplicated().any():
                        dup_count = df[col].duplicated().sum()
                        errors.append(f"Column '{col}' should be unique but has {dup_count} duplicates")
                
                # Min/Max constraints for numeric columns
                if pd.api.types.is_numeric_dtype(df[col]):
                    if "min" in constraint:
                        below_min = (df[col] < constraint["min"]).sum()
                        if below_min > 0:
                            errors.append(f"Column '{col}' has {below_min} values below minimum {constraint['min']}")
                    
                    if "max" in constraint:
                        above_max = (df[col] > constraint["max"]).sum()
                        if above_max > 0:
                            errors.append(f"Column '{col}' has {above_max} values above maximum {constraint['max']}")
                
                # Pattern constraint for string columns
                if "pattern" in constraint:
                    pattern = re.compile(constraint["pattern"])
                    invalid = df[col].apply(lambda x: not bool(pattern.match(str(x))) if pd.notna(x) else False).sum()
                    if invalid > 0:
                        errors.append(f"Column '{col}' has {invalid} values not matching pattern")
            
            # Check for empty dataframe
            if row_count == 0:
                errors.append("CSV file is empty (no data rows)")
            
            # Calculate quality score
            quality_score = self.calculate_quality_score(df, errors, warnings, null_counts)
            
            # Determine status
            status = "VALID" if not errors else "INVALID"
            
            # Generate summary
            summary = self._generate_summary(status, row_count, column_count, errors, warnings)
            
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
                "validation_summary": summary
            }
        
        except Exception as e:
            logger.error(f"CSV validation error: {str(e)}", exc_info=True)
            return {
                "status": "ERROR",
                "errors": [f"CSV validation error: {str(e)}"]
            }
    
    def _read_csv_with_encoding(self, file_data: bytes) -> pd.DataFrame:
        """Try to read CSV with different encodings"""
        encodings = ['utf-8', 'latin-1', 'cp1252', 'iso-8859-1']
        
        for encoding in encodings:
            try:
                df = pd.read_csv(
                    io.BytesIO(file_data),
                    encoding=encoding,
                    low_memory=False
                )
                logger.info(f"Successfully read CSV with {encoding} encoding")
                return df
            except Exception as e:
                logger.debug(f"Failed to read CSV with {encoding}: {str(e)}")
                continue
        
        return None
    
    def _generate_summary(
        self, 
        status: str, 
        row_count: int, 
        column_count: int,
        errors: list, 
        warnings: list
    ) -> str:
        """Generate validation summary"""
        summary = f"Validation Status: {status}\n"
        summary += f"Total Rows: {row_count:,}\n"
        summary += f"Total Columns: {column_count}\n"
        summary += f"Errors: {len(errors)}\n"
        summary += f"Warnings: {len(warnings)}\n"
        
        if errors:
            summary += "\nCritical Issues:\n"
            for error in errors[:5]:  # Show first 5 errors
                summary += f"- {error}\n"
            
            if len(errors) > 5:
                summary += f"... and {len(errors) - 5} more errors\n"
        
        return summary