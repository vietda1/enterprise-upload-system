package com.msb.upload.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.upload-completed:upload.completed}")
    private String uploadCompletedTopic;

    @Value("${kafka.topics.validation-completed:validation.completed}")
    private String validationCompletedTopic;

    @Value("${kafka.topics.ingestion-completed:ingestion.completed}")
    private String ingestionCompletedTopic;

    @Bean
    public NewTopic uploadCompletedTopic() {
        return TopicBuilder.name(uploadCompletedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic validationCompletedTopic() {
        return TopicBuilder.name(validationCompletedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ingestionCompletedTopic() {
        return TopicBuilder.name(ingestionCompletedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
