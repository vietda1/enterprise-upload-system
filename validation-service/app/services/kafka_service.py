import json
import logging
from typing import Callable, Dict, Any
from kafka import KafkaConsumer, KafkaProducer
from app.config import settings

logger = logging.getLogger(__name__)


class KafkaService:
    """Kafka Service for consuming and producing messages"""
    
    def __init__(self):
        self.consumer = None
        self.producer = None
    
    def create_consumer(self) -> KafkaConsumer:
        """Create Kafka consumer"""
        self.consumer = KafkaConsumer(
            settings.kafka_topic_upload_completed,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_consumer_group,
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            auto_offset_reset=settings.kafka_auto_offset_reset,
            enable_auto_commit=settings.kafka_enable_auto_commit
        )
        return self.consumer
    
    def create_producer(self) -> KafkaProducer:
        """Create Kafka producer"""
        self.producer = KafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        return self.producer
    
    def consume_messages(self, callback: Callable[[Dict[Any, Any]], None]):
        """Start consuming messages"""
        logger.info(f"Starting Kafka consumer for topic: {settings.kafka_topic_upload_completed}")
        
        try:
            for message in self.consumer:
                logger.info(f"Received message: {message.value}")
                try:
                    callback(message.value)
                except Exception as e:
                    logger.error(f"Error processing message: {str(e)}", exc_info=True)
        except KeyboardInterrupt:
            logger.info("Consumer interrupted")
        finally:
            self.close()
    
    def publish_message(self, topic: str, message: Dict[Any, Any]):
        """Publish message to Kafka"""
        try:
            future = self.producer.send(topic, message)
            future.get(timeout=10)
            logger.info(f"Published message to {topic}: {message}")
        except Exception as e:
            logger.error(f"Failed to publish message: {str(e)}", exc_info=True)
            raise
    
    def close(self):
        """Close Kafka connections"""
        if self.consumer:
            self.consumer.close()
        if self.producer:
            self.producer.close()