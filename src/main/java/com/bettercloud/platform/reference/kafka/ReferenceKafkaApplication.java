package com.bettercloud.platform.reference.kafka;

import com.bettercloud.platform.reference.kafka.config.ReportingConsumerFactory;
import com.bettercloud.platform.reference.kafka.models.avro.ReferenceMessage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * View Kafka Docs
 * https://docs.spring.io/spring-kafka/docs/1.3.2.RELEASE/reference/htmlsingle/
 */
@Slf4j
@SpringBootApplication
public class ReferenceKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceKafkaApplication.class, args);
    }

    /*
     *
     * PRODUCERS
     *
     */
    private static final String ACKS_DEFAULT = "1";
    private static final int RETRIES_DEFAULT = 1;

    @Value("${bootstrap.servers:localhost:9092}")
    private String bootstrapServers;
    @Value("${schema.registry.url:http://localhost:8081/}")
    private String schemaRegistryUrl;

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
    public KafkaTemplate<String, ReferenceMessage> getDefaultTemplate(Map<String, Object> properties) {
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerConfigs()));
    }

    @Bean
    public CommandLineRunner testProducer() {
        KafkaTemplate<String, ReferenceMessage> template = getDefaultTemplate(producerConfigs());
        return args -> {
            IntStream.range(0, 5).forEach(i -> {
                template.send("pf-ref-messages", UUID.randomUUID().toString(), ReferenceMessage.newBuilder()
                        .setType("TESTING")
                        .setTimestamp(System.currentTimeMillis())
                        .setPayload("Hello, World! " + i)
                        .build());
            });
        };
    }

    @Bean
    public CommandLineRunner metricsTest(List<ConsumerFactory> consumerFactories) {
        log.info("Reporting on {} kafka container factories", consumerFactories.size());
        Set<Consumer> consumers = Sets.newHashSet();
        return args -> {
            while (true) {
                log.info("Reporting on {} kafka consumer factories", consumerFactories.size());
                if (consumers.size() > 0) {
                    Map<MetricName, Metric> metrics = consumers.iterator().next().metrics();
                    log.info("");
//                    log.info("{}", c.listTopics());
                    metrics.keySet().forEach(key -> {
                        log.info("key={} group={} value={} tags={}", key.name(), key.group(), metrics.get(key).metricValue(), key.tags());
                    });
                    System.exit(0);
                }
                consumerFactories.stream()
                        .filter(c -> ReportingConsumerFactory.class.isAssignableFrom(c.getClass()))
                        .map(c -> ReportingConsumerFactory.class.cast(c))
                        .flatMap(c -> c.getConsumers().stream())
                        .forEach(c -> {
                            if (!consumers.contains(c)) {
                                consumers.add(c);
                            }
                        });
                Thread.sleep(2000);
            }
        };
    }
}
