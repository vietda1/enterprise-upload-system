import pytest
from app.models import ValidationResult
from app.schemas import ValidationStatus


def test_health_check(client):
    """Test health check endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_get_validation_not_found(client):
    """Test get validation result - not found"""
    response = client.get("/api/v1/validations/non-existent-id")
    assert response.status_code == 404


def test_get_validation_stats(client, db_session):
    """Test get validation statistics"""
    # Create some test data
    validation1 = ValidationResult(
        upload_id="test-1",
        status=ValidationStatus.VALID.value,
        dataset_type="CSV_TRANSACTION",
        data_quality_score=90
    )
    validation2 = ValidationResult(
        upload_id="test-2",
        status=ValidationStatus.INVALID.value,
        dataset_type="CSV_TRANSACTION",
        data_quality_score=50
    )
    
    db_session.add(validation1)
    db_session.add(validation2)
    db_session.commit()
    
    response = client.get("/api/v1/validations/stats/summary")
    assert response.status_code == 200
    
    data = response.json()
    assert data["total_validations"] == 2
    assert data["valid_count"] == 1
    assert data["invalid_count"] == 1