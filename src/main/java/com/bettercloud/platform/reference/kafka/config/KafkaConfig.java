package com.bettercloud.platform.reference.kafka.config;

import com.google.common.collect.Maps;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Map;
import java.util.UUID;

/**
 * View All docs
 * https://docs.spring.io/spring-kafka/docs/1.3.2.RELEASE/reference/htmlsingle/
 * <p>
 * - Lifecycle Management: https://docs.spring.io/spring-kafka/reference/htmlsingle/#kafkalistener-lifecycle
 * <p>
 * TODO: Seeking consumer
 * https://docs.spring.io/spring-kafka/reference/htmlsingle/#seek
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    private static final int ACKS_DEFAULT = 1;
    private static final int RETRIES_DEFAULT = 1;
    private static final int DEFAULT_MAX_PARTITION_FETCH_SIZE_BYTES = 250000;
    private static final int DEFAULT_SESSION_TIMEOUT_MS = 120000;
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 130000;

    @Value("${bootstrap.servers:localhost:9092}")
    private String bootstrapServers;
    @Value("${schema.registry.url:http://localhost:8081/}")
    private String schemaRegistryUrl;

    /*
     *
     * CONSUMERS
     *
     */
    @Value("${kakfa.consumer.concurrency:1}")
    private int listenerConcurrency;

    @Bean
    public Map<String, Object> baseConsumerConfigs() {
        final Map<String, Object> properties = Maps.newHashMap();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        // defaults to be overridden by Listener
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "generic-kafka-consumer-" + UUID.randomUUID());
        // defaults
        // TODO: Add others and override appropriately
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, DEFAULT_SESSION_TIMEOUT_MS);
        properties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, DEFAULT_REQUEST_TIMEOUT_MS);
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, DEFAULT_MAX_PARTITION_FETCH_SIZE_BYTES);

        return properties;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfigs());
    }

    @Bean
    @Primary
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> defaultContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(listenerConcurrency);
        return factory;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> batchContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(listenerConcurrency);
        factory.setBatchListener(true);
        return factory;
    }

    /*
     *
     * PRODUCERS
     *
     */
    @Bean
    public ProducerFactory<Integer, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        final Map<String, Object> props = Maps.newConcurrentMap();
        props.put(ProducerConfig.ACKS_CONFIG, ACKS_DEFAULT);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.RETRIES_CONFIG, RETRIES_DEFAULT);
        return props;
    }

    @Bean
    public KafkaTemplate<String, Object> getDefaultTemplate(Map<String, Object> properties) {
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerConfigs()));
    }

    @Bean
    public KafkaTemplate<Integer, String> kafkaTemplate() {
        return new KafkaTemplate<Integer, String>(producerFactory());
    }
}
