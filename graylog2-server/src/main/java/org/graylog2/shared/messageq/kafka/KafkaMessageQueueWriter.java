package org.graylog2.shared.messageq.kafka;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.transform;

@Singleton
public class KafkaMessageQueueWriter extends AbstractService implements MessageQueueWriter {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageQueueWriter.class);

    private KafkaJournal kafkaJournal;
    private Semaphore journalFilled;

    private static final Retryer<Void> JOURNAL_WRITE_RETRYER = RetryerBuilder.<Void>newBuilder()
            .retryIfException(new Predicate<Throwable>() {
                @Override
                public boolean apply(@Nullable Throwable input) {
                    LOG.error("Unable to write to journal - retrying with exponential back-off", input);
                    return true;
                }
            })
            .withWaitStrategy(WaitStrategies.exponentialWait(250, 1, TimeUnit.MINUTES))
            .withStopStrategy(StopStrategies.neverStop())
            .build();


    @Inject
    public KafkaMessageQueueWriter(KafkaJournal kafkaJournal,
                                   @Named("JournalSignal") Semaphore journalFilled) {
        this.kafkaJournal = kafkaJournal;
        this.journalFilled = journalFilled;
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    public void write(List<Entry> entries) throws MessageQueueException {
        final Converter converter = new Converter();
        final ArrayList<Journal.Entry> journalEntries = Lists.newArrayList(transform(entries, converter));

        try {
            writeToJournal(converter, journalEntries);
        } catch (Exception e) {
            LOG.error("Unable to write to journal - retrying", e);

            // Use retryer with exponential back-off to avoid spamming the logs.
            try {
                JOURNAL_WRITE_RETRYER.call(() -> {
                    writeToJournal(converter, journalEntries);
                    return null;
                });
            } catch (ExecutionException | RetryException ex) {
                throw new MessageQueueException("Retryer exception", ex);
            }
        }
    }

    private void writeToJournal(Converter converter, List<Journal.Entry> entries) {
        final long lastOffset = kafkaJournal.write(entries);
        LOG.debug("Processed batch, wrote {} bytes, last journal offset: {}, signalling reader.",
                converter.getBytesWritten(),
                lastOffset);
        journalFilled.release();
    }

    @Override
    public Entry createEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp) {
        return new KafkaMessageQueueEntry(value, Long.MIN_VALUE);
    }

    private class Converter implements Function<Entry, Journal.Entry> {
        private DateTime latestReceiveTime = new DateTime(0L, DateTimeZone.UTC);
        private long bytesWritten = 0;

        @Nullable
        @Override
        public Journal.Entry apply(@Nullable Entry input) {
            return new Journal.Entry(input.id(), input.value());
        }

        public long getBytesWritten() {
            // TODO
            return bytesWritten;
        }
        // TODO
        public DateTime getLatestReceiveTime() {
            return latestReceiveTime;
        }
    }
}
