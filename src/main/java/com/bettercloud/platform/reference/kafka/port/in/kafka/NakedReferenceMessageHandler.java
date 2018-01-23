package com.bettercloud.platform.reference.kafka.port.in.kafka;

import com.bettercloud.platform.reference.kafka.models.avro.ReferenceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 1.3.2 Docs
 * https://docs.spring.io/spring-kafka/docs/1.3.2.RELEASE/reference/html/_reference.html#kafka
 *
 * TODO: Simple consumer
 * https://docs.spring.io/spring-kafka/reference/htmlsingle/#annotation-send-to
 */
@Slf4j
@Component
public class NakedReferenceMessageHandler {

    @KafkaListener(id = "pf-reference-naked-klistener", topics = "pf-ref-messages")
    public void onMessage(ReferenceMessage message) {
        log.info("Received {}", message);
    }
}
