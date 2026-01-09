import logging
from app.services.kafka_service import KafkaService
from app.services.validation_service import ValidationService
from app.database import SessionLocal
from minio import Minio
from app.config import settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def main():
    logger.info("Starting Kafka consumer...")
    
    kafka_service = KafkaService()
    kafka_service.create_consumer()
    kafka_service.create_producer()
    
    minio_client = Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure
    )
    
    def handle_event(event: dict):
        db = SessionLocal()
        try:
            validation_service = ValidationService(db, minio_client, kafka_service)
            validation_service.validate_upload(
                event["uploadId"],
                event["objectKey"],
                event["datasetType"]
            )
        finally:
            db.close()
    
    try:
        kafka_service.consume_messages(handle_event)
    except KeyboardInterrupt:
        logger.info("Consumer stopped")
    finally:
        kafka_service.close()


if __name__ == "__main__":
    main()