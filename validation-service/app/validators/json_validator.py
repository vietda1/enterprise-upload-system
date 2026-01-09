import json
import re
import logging
from typing import Dict, Any
from app.validators.base_validator import BaseValidator

logger = logging.getLogger(__name__)


class JSONValidator(BaseValidator):
    """JSON File Validator"""
    
    def validate(self, file_data: bytes, rules: Dict[str, Any]) -> Dict[str, Any]:
        """Validate JSON file"""
        errors = []
        warnings = []
        
        try:
            # Parse JSON
            data = json.loads(file_data)
            
            required_fields = rules.get("required_fields", [])
            field_types = rules.get("field_types", {})
            constraints = rules.get("constraints", {})
            
            # Handle array of objects
            if isinstance(data, list):
                row_count = len(data)
                
                # Validate each record
                for idx, record in enumerate(data):
                    if not isinstance(record, dict):
                        errors.append(f"Record {idx}: Expected object, got {type(record).__name__}")
                        continue
                    
                    # Check required fields
                    missing = set(required_fields) - set(record.keys())
                    if missing:
                        errors.append(f"Record {idx}: Missing required fields {missing}")
                    
                    # Validate field types
                    for field, expected_type in field_types.items():
                        if field in record:
                            if not self._check_field_type(record[field], expected_type):
                                errors.append(f"Record {idx}: Field '{field}' has invalid type")
                    
                    # Validate constraints
                    for field, constraint in constraints.items():
                        if field in record and "pattern" in constraint:
                            pattern = re.compile(constraint["pattern"])
                            if not pattern.match(str(record[field])):
                                errors.append(f"Record {idx}: Field '{field}' doesn't match pattern")
            
            # Handle single object
            else:
                row_count = 1
                
                # Check required fields
                missing = set(required_fields) - set(data.keys())
                if missing:
                    errors.append(f"Missing required fields: {missing}")
                
                # Validate field types
                for field, expected_type in field_types.items():
                    if field in data:
                        if not self._check_field_type(data[field], expected_type):
                            errors.append(f"Field '{field}' has invalid type")
            
            file_size_mb = len(file_data) / (1024 * 1024)
            quality_score = self.calculate_quality_score(None, errors, warnings)
            status = "VALID" if not errors else "INVALID"
            
            return {
                "status": status,
                "row_count": row_count,
                "file_size_mb": round(file_size_mb, 2),
                "data_quality_score": quality_score,
                "errors": errors,
                "warnings": warnings,
                "validation_summary": f"Validated {row_count} JSON record(s). Status: {status}"
            }
        
        except json.JSONDecodeError as e:
            return {
                "status": "ERROR",
                "errors": [f"Invalid JSON: {str(e)}"]
            }
        except Exception as e:
            logger.error(f"JSON validation error: {str(e)}", exc_info=True)
            return {
                "status": "ERROR",
                "errors": [f"JSON validation error: {str(e)}"]
            }
    
    def _check_field_type(self, value: Any, expected_type: str) -> bool:
        """Check if field value matches expected type"""
        if expected_type == "string":
            return isinstance(value, str)
        elif expected_type == "number" or expected_type == "numeric":
            return isinstance(value, (int, float))
        elif expected_type == "boolean":
            return isinstance(value, bool)
        elif expected_type == "array":
            return isinstance(value, list)
        elif expected_type == "object":
            return isinstance(value, dict)
        return True