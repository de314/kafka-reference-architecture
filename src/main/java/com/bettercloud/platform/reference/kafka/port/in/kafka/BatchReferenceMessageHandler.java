package com.bettercloud.platform.reference.kafka.port.in.kafka;

import com.bettercloud.platform.reference.kafka.models.avro.ReferenceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
//@Component
//@KafkaListener(group = "pf-batch-reference-klistener", topics = "pf-ref-messages", containerFactory = "batchContainerFactory")
public class BatchReferenceMessageHandler {

    @KafkaHandler
    public void onMessages(List<ConsumerRecord<String, ReferenceMessage>> records) {
        log.info("Processing {} messages", records.size());
        records.forEach(rec -> log.info("Handled {}@{}", rec.partition(), rec.offset()));
    }
}
