package com.pal.dipesh.razorpay.common.config;

import com.pal.dipesh.razorpay.common.enums.EventAggregateType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
    Map<String, String> topics
) {
    public String topicFor(EventAggregateType aggregateType) {
        String topic = topics.get(aggregateType.name().toLowerCase());

        if (topic == null) {
            throw new IllegalArgumentException("No Kafka topic is configured for aggregate type " + aggregateType.name());
        }

        return topic;
    }
}
