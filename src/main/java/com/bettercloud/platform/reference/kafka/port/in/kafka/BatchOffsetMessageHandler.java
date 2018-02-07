package com.bettercloud.platform.reference.kafka.port.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.common.OffsetAndMetadata;
import kafka.coordinator.group.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import scala.Option;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BatchOffsetMessageHandler {

    private static final GroupMetadataManager.OffsetsMessageFormatter formatter = new GroupMetadataManager.OffsetsMessageFormatter();
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    @KafkaListener(id = "pf-offset-klistener", topics = "__consumer_offsets", containerFactory = "batchContainerFactory")
    public void onMessages(List<ConsumerRecord<byte[], byte[]>> records) {
        log.info("Processing {} messages", records.size());
        List<String> jsonOffsets = records.stream()
                .map(rec -> {
                    BaseKey baseKey = GroupMetadataManager$.MODULE$.readMessageKey(ByteBuffer.wrap(rec.key()));
                    if (baseKey != null && OffsetKey.class.isAssignableFrom(baseKey.getClass())) {
                        GroupTopicPartition key = OffsetKey.class.cast(baseKey).key();
                        OffsetAndMetadata offsetAndMetadata = GroupMetadataManager$.MODULE$.readOffsetMessageValue(ByteBuffer.wrap(rec.value()));
                        TopicPartition topicPartition = key.topicPartition();
                        return ConsumerOffset.builder()
                                .topic(topicPartition.topic())
                                .partition(topicPartition.partition())
                                .consumerGroup(key.group())
                                .consumerOffset(offsetAndMetadata.offset())
                                .commitTimestamp(offsetAndMetadata.commitTimestamp())
                                .expireTimestamp(offsetAndMetadata.expireTimestamp())
                                .build();
                    }
                    return null;
                })
                .map(co -> {
                    try {
                        return JSON_OBJECT_MAPPER.writeValueAsString(co);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(json -> json != null)
                .collect(Collectors.toList());
        jsonOffsets.forEach(System.out::println);
    }

    @Data
    @Builder
    private static class ConsumerOffset {
        private String topic;
        private int partition;
        private String consumerGroup;
        private long consumerOffset;
        private long commitTimestamp;
        private long expireTimestamp;
    }
}
