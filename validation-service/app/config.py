from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    """Application Settings"""
    
    # Application
    app_name: str = "validation-service"
    app_version: str = "1.0.0"
    environment: str = "development"
    debug: bool = False
    
    # API
    api_prefix: str = "/api/v1"
    
    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "validation-service-group"
    kafka_topic_upload_completed: str = "upload.completed"
    kafka_topic_validation_completed: str = "validation.completed"
    kafka_auto_offset_reset: str = "earliest"
    kafka_enable_auto_commit: bool = True
    
    # MinIO
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_bucket: str = "enterprise-uploads"
    minio_secure: bool = False
    
    # PostgreSQL
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "upload_db"
    postgres_user: str = "postgres"
    postgres_password: str = "postgres"
    
    # Validation Rules
    max_file_size_mb: int = 500
    allowed_csv_encodings: List[str] = ["utf-8", "latin-1", "cp1252"]
    max_rows_preview: int = 100
    
    @property
    def database_url(self) -> str:
        """Get database URL"""
        return f"postgresql://{self.postgres_user}:{self.postgres_password}@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()