package org.graylog2.shared.messageq.pulsar;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import org.apache.pulsar.client.api.ConsumerInterceptor;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static com.codahale.metrics.MetricRegistry.name;

public class PulsarMessageQueueReader extends AbstractIdleService implements MessageQueueReader {
    public interface Factory extends MessageQueueReader.Factory<PulsarMessageQueueReader> {
        @Override
        PulsarMessageQueueReader create(@Assisted("name") String name);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PulsarMessageQueueReader.class);

    private final String name;
    private final String topic;
    private final String serviceUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer readTimer;

    private PulsarClient client;
    private org.apache.pulsar.client.api.Consumer<byte[]> consumer;

    @Inject
    public PulsarMessageQueueReader(MetricRegistry metricRegistry, @Assisted("name") String name) {
        this.name = name;
        this.topic = name + "-message-queue"; // TODO: Make configurable
        this.serviceUrl = "pulsar://localhost:6650"; // TODO: Make configurable

        this.messageMeter = metricRegistry.meter(name("system.message-queue.pulsar", name, "reader.messages"));
        this.byteCounter = metricRegistry.counter(name("system.message-queue.pulsar", name, "reader.byte-count"));
        this.byteMeter = metricRegistry.meter(name("system.message-queue.pulsar", name, "reader.bytes"));
        this.readTimer = metricRegistry.timer(name("system.message-queue.pulsar", name, "reader.reads"));
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting pulsar message queue reader service: {}", name);

        this.client = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
        this.consumer = client.newConsumer(Schema.BYTES)
                .topic(topic)
                .subscriptionName(name)
                .intercept(new MessageInterceptor())
                .subscribe();

        // Service is ready for consuming
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (client != null) {
            client.close();
            client.shutdown();
        }
    }

    @Override
    public Entry createEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp) {
        return new PulsarMessageQueueEntry(id, key, value, timestamp);
    }

    @Override
    public List<Entry> read(long entries) throws MessageQueueException {
        return null;
    }

    @Override
    public void subscribe(Consumer<Envelope> consumer) throws MessageQueueException {

    }

    private class MessageInterceptor implements ConsumerInterceptor<byte[]> {
        @Override
        public void close() {

        }

        @Override
        public Message<byte[]> beforeConsume(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Message<byte[]> message) {
            final int length = message.getData().length;

            messageMeter.mark();
            byteCounter.inc(length);
            byteMeter.mark(length);

            return message;
        }

        @Override
        public void onAcknowledge(org.apache.pulsar.client.api.Consumer<byte[]> consumer, MessageId messageId, Throwable exception) {

        }

        @Override
        public void onAcknowledgeCumulative(org.apache.pulsar.client.api.Consumer<byte[]> consumer, MessageId messageId, Throwable exception) {

        }

        @Override
        public void onNegativeAcksSend(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Set<MessageId> messageIds) {

        }

        @Override
        public void onAckTimeoutSend(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Set<MessageId> messageIds) {

        }
    }
}
