from typing import Dict, List, Any


class DatasetValidationRules:
    """Dataset Validation Rules Configuration"""
    
    RULES = {
        "CSV_TRANSACTION": {
            "name": "Transaction Data",
            "required_columns": ["transaction_id", "amount", "date", "customer_id"],
            "column_types": {
                "transaction_id": "string",
                "amount": "numeric",
                "date": "datetime",
                "customer_id": "string"
            },
            "constraints": {
                "amount": {"min": 0, "max": 1000000},
                "transaction_id": {"unique": True}
            },
            "allow_extra_columns": True
        },
        "JSON_USER_PROFILE": {
            "name": "User Profile Data",
            "required_fields": ["user_id", "email", "created_at"],
            "field_types": {
                "user_id": "string",
                "email": "string",
                "created_at": "string"
            },
            "constraints": {
                "email": {"pattern": r"^[\w\.-]+@[\w\.-]+\.\w+$"}
            }
        },
        "PARQUET_ANALYTICS": {
            "name": "Analytics Events",
            "required_columns": ["event_id", "event_type", "timestamp", "user_id"],
            "column_types": {
                "event_id": "string",
                "event_type": "string",
                "timestamp": "datetime",
                "user_id": "string"
            }
        },
        "CSV_SALES": {
            "name": "Sales Data",
            "required_columns": ["sale_id", "product_id", "quantity", "price", "sale_date"],
            "column_types": {
                "sale_id": "string",
                "product_id": "string",
                "quantity": "numeric",
                "price": "numeric",
                "sale_date": "datetime"
            },
            "constraints": {
                "quantity": {"min": 1},
                "price": {"min": 0}
            }
        },
        "CSV_INVENTORY": {
            "name": "Inventory Data",
            "required_columns": ["product_id", "quantity", "location", "last_updated"],
            "column_types": {
                "product_id": "string",
                "quantity": "numeric",
                "location": "string",
                "last_updated": "datetime"
            }
        }
    }
    
    @classmethod
    def get_rules(cls, dataset_type: str) -> Dict[str, Any]:
        """Get validation rules for dataset type"""
        return cls.RULES.get(dataset_type, {})
    
    @classmethod
    def get_all_types(cls) -> List[str]:
        """Get all dataset types"""
        return list(cls.RULES.keys())
    
    @classmethod
    def is_valid_type(cls, dataset_type: str) -> bool:
        """Check if dataset type is valid"""
        return dataset_type in cls.RULES