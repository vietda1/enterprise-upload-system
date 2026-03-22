import logging
import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import init_db
from app.api.endpoints import router
from app.services.kafka_service import KafkaService
from app.services.validation_service import ValidationService
from app.database import SessionLocal
from minio import Minio

# Configure logging
logging.basicConfig(
    level=logging.INFO if not settings.debug else logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan"""
    # Startup
    logger.info("Starting Validation Service...")
    
    # Initialize database
    init_db()
    logger.info("Database initialized")
    
    # Start Kafka consumer in background thread
    #kafka_service = KafkaService()
    #kafka_service.create_consumer()
    #kafka_service.create_producer()
    
    # Create MinIO client
    """ minio_client = Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure
    ) """
    
    # Start consumer thread
    def consume_events():
        def handle_upload_event(event: dict):
            db = SessionLocal()
            try:
                #validation_service = ValidationService(db, minio_client, kafka_service)
                """ validation_service.validate_upload(
                    event["uploadId"],
                    event["objectKey"],
                    event["datasetType"]
                ) """
            finally:
                db.close()
        
        #kafka_service.consume_messages(handle_upload_event)
    
    consumer_thread = threading.Thread(target=consume_events, daemon=True)
    consumer_thread.start()
    logger.info("Kafka consumer started")
    
    yield
    
    # Shutdown
    logger.info("Shutting down...")
    #kafka_service.close()


# Create FastAPI app
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(router, prefix=settings.api_prefix)


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": settings.app_name,
        "version": settings.app_version
    }


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "docs": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.debug
    )