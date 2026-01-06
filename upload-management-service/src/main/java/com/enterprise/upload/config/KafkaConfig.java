package com.enterprise.upload.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic uploadCompletedTopic() {
        return TopicBuilder.name("upload.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic validationCompletedTopic() {
        return TopicBuilder.name("validation.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic ingestionCompletedTopic() {
        return TopicBuilder.name("ingestion.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}