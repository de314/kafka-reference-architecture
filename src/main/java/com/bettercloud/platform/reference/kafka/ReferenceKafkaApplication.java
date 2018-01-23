package com.bettercloud.platform.reference.kafka;

import com.bettercloud.platform.reference.kafka.config.ReportingConsumerFactory;
import com.bettercloud.platform.reference.kafka.models.avro.LoadMessage;
import com.bettercloud.platform.reference.kafka.models.avro.ReferenceMessage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.corba.se.impl.protocol.INSServerRequestDispatcher;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.Builder;
import lombok.Data;
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
import java.util.Optional;
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
    public KafkaTemplate getDefaultTemplate() {
        Map<String, Object> yoloProps = Maps.newHashMap(producerConfigs());
        yoloProps.put(ProducerConfig.ACKS_CONFIG, "0");
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(yoloProps));
    }

    @Bean
    public KafkaTemplate getDurableTemplate() {
        Map<String, Object> durableProperties = Maps.newHashMap(producerConfigs());
        durableProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(durableProperties));
    }

    @Bean
    public CommandLineRunner loadProducer(
            @Value("${pf.reference.kafka.sla.loadtest.iterations:10000}")
            int iterations
    ) {
        return args -> {
            KafkaTemplate<String, LoadMessage> throughputTemplate = getDefaultTemplate();
            KafkaTemplate<String, LoadMessage> slaTemplate = getDurableTemplate();

            int warmupIterations = 10000;

            RunConfig durableConfig = RunConfig.builder()
                    .template(slaTemplate)
                    .topic("pf-sla-durable")
                    .iterations(iterations)
                    .warmupIterations(warmupIterations)
                    .results(Lists.newArrayList())
                    .build();

            RunConfig throughputConfig = RunConfig.builder()
                    .template(throughputTemplate)
                    .topic("pf-sla-throughput")
                    .iterations(iterations)
                    .warmupIterations(warmupIterations)
                    .results(Lists.newArrayList())
                    .build();

            RunConfig yoloConfig = RunConfig.builder()
                    .template(throughputTemplate)
                    .topic("pf-sla-yolo")
                    .iterations(iterations)
                    .warmupIterations(warmupIterations)
                    .results(Lists.newArrayList())
                    .build();

            List<List<RunConfig>> runs = Lists.<List<RunConfig>>newArrayList(
                    Lists.newArrayList(durableConfig, throughputConfig, yoloConfig),
                    Lists.newArrayList(durableConfig, yoloConfig, throughputConfig),
                    Lists.newArrayList(throughputConfig, yoloConfig, durableConfig),
                    Lists.newArrayList(throughputConfig, durableConfig, yoloConfig),
                    Lists.newArrayList(yoloConfig, throughputConfig, durableConfig),
                    Lists.newArrayList(yoloConfig, durableConfig, throughputConfig)
            );

            runs.forEach(run -> runProducerLoadTest(run));

            log.info("Results:");
            log.info(yoloConfig.summary());
            log.info(throughputConfig.summary());
            log.info(durableConfig.summary());
        };
    }

    private static void runProducerLoadTest(List<RunConfig> configs) {
        configs.forEach(c -> c.getResults().add(runProducerLoadTest(c)));
    }

    private static LoadResults runProducerLoadTest(RunConfig config) {
        KafkaTemplate<String, LoadMessage> template = config.getTemplate();
        String topic = config.getTopic();
        int iterations = config.getIterations();
        int warmupIterations = config.getWarmupIterations();
        IntStream.range(0, warmupIterations).forEach(i -> template.send(topic, UUID.randomUUID().toString(), LoadMessage.newBuilder()
                .setIndex(i)
                .setStartTime(0)
                .setPublishTime(0)
                .setDiff(0)
                .setAvgLatency(0)
                .setMsgPerSec(0)
                .build())
        );
        long startTime = System.currentTimeMillis();
        IntStream.range(0, iterations).forEach(i -> {
            long curr = System.currentTimeMillis();
            int diff = (int) (curr - startTime);
            double latency = diff * 1.0 / i;
            template.send(topic, UUID.randomUUID().toString(), LoadMessage.newBuilder()
                    .setIndex(i)
                    .setStartTime(startTime)
                    .setPublishTime(curr)
                    .setDiff(diff)
                    .setAvgLatency(latency)
                    .setMsgPerSec(1000.0 / latency)
                    .build());
        });
        long endTime = System.currentTimeMillis();
        long diff = endTime - startTime;
        double avgLatency = diff * 1.0 / iterations;
        double msgPerSec = 1000.0 / avgLatency;
        return LoadResults.builder()
                .topic(topic)
                .iterations(iterations)
                .startTime(startTime)
                .endTime(endTime)
                .duration(diff)
                .avgLatency(avgLatency)
                .msgPerSec(msgPerSec)
                .build();
    }

    @Data
    @Builder
    private static class RunConfig {
        private KafkaTemplate<String, LoadMessage> template;
        private String topic;
        private int iterations;
        private int warmupIterations;
        private List<LoadResults> results = Lists.newArrayList();

        public String summary() {
            StringBuilder sb = new StringBuilder(this.getTopic());
            double avgLat = getResults().stream().map(r -> r.getAvgLatency()).reduce((a1, a2) -> a1 + a2)
                    .map(val -> val / getResults().size())
                    .orElse(-1.0);
            double avgMsgPerSec = getResults().stream().map(r -> r.getMsgPerSec()).reduce((a1, a2) -> a1 + a2)
                    .map(val -> val / getResults().size())
                    .orElse(-1.0);
            sb.append(" latency=").append(avgLat).append(" msg/sec=").append(avgMsgPerSec);
            return sb.toString();
        }
    }

    @Data
    @Builder
    private static class LoadResults {
        private String topic;
        private int iterations;
        private long startTime;
        private long endTime;
        private long duration;
        private double avgLatency;
        private double msgPerSec;
    }
}
