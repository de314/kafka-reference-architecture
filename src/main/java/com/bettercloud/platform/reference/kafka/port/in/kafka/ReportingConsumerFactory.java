package com.bettercloud.platform.reference.kafka.port.in.kafka;

import com.google.common.collect.Lists;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReportingConsumerFactory implements ConsumerFactory {

    private final List<Consumer> consumers;
    private final ConsumerFactory delegate;

    public ReportingConsumerFactory(ConsumerFactory delegate) {
        this.delegate = delegate;
        this.consumers = Collections.synchronizedList(Lists.newArrayList());
    }

    public List<Consumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    @Override
    public Consumer createConsumer() {
        Consumer consumer = delegate.createConsumer();
        consumers.add(consumer);
        return consumer;
    }

    @Override
    public Consumer createConsumer(String clientIdSuffix) {
        Consumer consumer = delegate.createConsumer(clientIdSuffix);
        consumers.add(consumer);
        return consumer;
    }

    @Override
    public Consumer createConsumer(String groupId, String clientIdSuffix) {
        Consumer consumer = delegate.createConsumer(groupId, clientIdSuffix);
        consumers.add(consumer);
        return consumer;
    }

    @Override
    public boolean isAutoCommit() {
        return delegate.isAutoCommit();
    }

    @Override
    public Map<String, Object> getConfigurationProperties() {
        return delegate.getConfigurationProperties();
    }
}
