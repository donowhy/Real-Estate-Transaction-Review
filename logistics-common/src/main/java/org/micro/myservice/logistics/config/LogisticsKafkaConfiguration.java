package org.micro.myservice.logistics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class LogisticsKafkaConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ObjectMapper logisticsObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    ConsumerFactory<String, LogisticsEvent> logisticsConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<LogisticsEvent> deserializer = new JsonDeserializer<>(LogisticsEvent.class, objectMapper);
        deserializer.addTrustedPackages("org.micro.myservice.logistics.event");
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, LogisticsEvent> logisticsKafkaListenerContainerFactory(
            ConsumerFactory<String, LogisticsEvent> logisticsConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, LogisticsEvent>();
        factory.setConsumerFactory(logisticsConsumerFactory);
        return factory;
    }
}
