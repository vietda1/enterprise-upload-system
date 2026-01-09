import pytest
import pandas as pd
import json
import io
from unittest.mock import Mock, MagicMock
from app.validators.csv_validator import CSVValidator
from app.validators.json_validator import JSONValidator


def test_csv_validator_valid_file():
    """Test CSV validator with valid file"""
    # Create mock MinIO client
    minio_client = Mock()
    
    # Create CSV validator
    validator = CSVValidator(minio_client, "test-bucket")
    
    # Create valid CSV data
    df = pd.DataFrame({
        "transaction_id": ["T001", "T002", "T003"],
        "amount": [100.50, 200.75, 150.25],
        "date": ["2024-01-01", "2024-01-02", "2024-01-03"],
        "customer_id": ["C001", "C002", "C003"]
    })
    
    csv_buffer = io.BytesIO()
    df.to_csv(csv_buffer, index=False)
    csv_data = csv_buffer.getvalue()
    
    # Validation rules
    rules = {
        "required_columns": ["transaction_id", "amount", "date", "customer_id"],
        "column_types": {
            "amount": "numeric"
        },
        "constraints": {
            "amount": {"min": 0, "max": 1000000}
        }
    }
    
    # Validate
    result = validator.validate(csv_data, rules)
    
    # Assertions
    assert result["status"] == "VALID"
    assert result["row_count"] == 3
    assert result["column_count"] == 4
    assert len(result["errors"]) == 0


def test_csv_validator_missing_columns():
    """Test CSV validator with missing required columns"""
    minio_client = Mock()
    validator = CSVValidator(minio_client, "test-bucket")
    
    # CSV without required column
    df = pd.DataFrame({
        "transaction_id": ["T001"],
        "amount": [100.50]
        # Missing: date, customer_id
    })
    
    csv_buffer = io.BytesIO()
    df.to_csv(csv_buffer, index=False)
    csv_data = csv_buffer.getvalue()
    
    rules = {
        "required_columns": ["transaction_id", "amount", "date", "customer_id"]
    }
    
    result = validator.validate(csv_data, rules)
    
    assert result["status"] == "INVALID"
    assert len(result["errors"]) > 0
    assert any("Missing required columns" in error for error in result["errors"])


def test_json_validator_valid_file():
    """Test JSON validator with valid file"""
    minio_client = Mock()
    validator = JSONValidator(minio_client, "test-bucket")
    
    # Valid JSON data
    data = [
        {"user_id": "U001", "email": "user1@example.com", "created_at": "2024-01-01"},
        {"user_id": "U002", "email": "user2@example.com", "created_at": "2024-01-02"}
    ]
    
    json_data = json.dumps(data).encode('utf-8')
    
    rules = {
        "required_fields": ["user_id", "email", "created_at"],
        "field_types": {
            "user_id": "string",
            "email": "string"
        }
    }
    
    result = validator.validate(json_data, rules)
    
    assert result["status"] == "VALID"
    assert result["row_count"] == 2
    assert len(result["errors"]) == 0


def test_json_validator_missing_fields():
    """Test JSON validator with missing required fields"""
    minio_client = Mock()
    validator = JSONValidator(minio_client, "test-bucket")
    
    # JSON with missing field
    data = [
        {"user_id": "U001", "email": "user1@example.com"}
        # Missing: created_at
    ]
    
    json_data = json.dumps(data).encode('utf-8')
    
    rules = {
        "required_fields": ["user_id", "email", "created_at"]
    }
    
    result = validator.validate(json_data, rules)
    
    assert result["status"] == "INVALID"
    assert len(result["errors"]) > 0