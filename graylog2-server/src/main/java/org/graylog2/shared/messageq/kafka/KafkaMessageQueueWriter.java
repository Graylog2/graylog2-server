package org.graylog2.shared.messageq.kafka;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

@Singleton
public class KafkaMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageQueueWriter.class);
    private KafkaProducer<String, byte[]> producer;
    private final Properties props;
    private final String topic;

    private final CountDownLatch latch = new CountDownLatch(1);

    @Inject
    public KafkaMessageQueueWriter() {
        props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all"); //TODO tune maybe set to 1?
        props.put("batch.size", 1024 * 1024 * 2); // bytes!
        props.put("retries", 100000); //TODO
        props.put("linger.ms", 200); // time before we send out batches
        props.put("compression.type", "gzip");
        //props.put("buffer.memory", 1024 * 1024 * 32); //default 32MB?
        props.put("max.block.ms", Long.MAX_VALUE); // default 1 minute
        props.put("client.id", "xxx node id");
        //max.in.flight.requests.per.connection  // default 5
        //props.put("enable.idempotence", true); // exactly once delivery
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        this.topic = "message-input";
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting Kafka message queue writer service for topic: {}", topic);
        producer = new KafkaProducer<String, byte[]>(props);

        // Service is ready for writing
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        producer.close();

    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.info("Got interrupted", e);
            Thread.currentThread().interrupt();
            return;
        }
        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }
        for (final RawMessageEvent entry : entries) {

            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, entry.getEncodedRawMessage());
            final Future<RecordMetadata> future = producer.send(record);
        }

    }
}
