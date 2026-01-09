from kafka import KafkaConsumer
import json
import logging
from typing import Callable

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ValidationConsumer:
    
    def __init__(self, bootstrap_servers: str, group_id: str, topic: str):
        self.consumer = KafkaConsumer(
            topic,
            bootstrap_servers=bootstrap_servers,
            group_id=group_id,
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            auto_offset_reset='earliest',
            enable_auto_commit=True
        )
    
    def start(self, callback: Callable):
        """Start consuming messages"""
        
        logger.info("Starting Kafka consumer...")
        
        try:
            for message in self.consumer:
                logger.info(f"Received message: {message.value}")
                callback(message.value)
        except KeyboardInterrupt:
            logger.info("Consumer interrupted")
        finally:
            self.consumer.close()