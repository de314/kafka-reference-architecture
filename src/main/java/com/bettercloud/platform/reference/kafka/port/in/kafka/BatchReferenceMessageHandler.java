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
public class BatchReferenceMessageHandler {

    /*
     * If you want to use ConsumerRecords:
     *  - you cannot place this annotation at the class level and use @KafkaHandler at method level
     *
     * If you only need the payload
     *  - you can place this annotation at the class level and use @KafkaHandler at method level
     */
    @KafkaListener(id = "pf-reference-batch-klistener", topics = "pf-ref-messages", containerFactory = "batchContainerFactory")
    public void onMessage(List<ConsumerRecord<String, ReferenceMessage>> records) {
        log.info("Processing {} records", records.size());
        records.forEach(rec -> log.info("Handled {}@{}", rec.partition(), rec.offset()));
    }
}
