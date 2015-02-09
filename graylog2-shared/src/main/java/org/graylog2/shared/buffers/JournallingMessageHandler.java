/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.lmax.disruptor.EventHandler;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.google.common.collect.Lists.transform;

public class JournallingMessageHandler implements EventHandler<RawMessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(JournallingMessageHandler.class);

    private final List<RawMessageEvent> batch = Lists.newArrayList();
    private final Counter byteCounter;
    private final Journal journal;
    private final Semaphore journalFilled;

    @Inject
    public JournallingMessageHandler(MetricRegistry metrics, Journal journal, @Named("JournalSignal") Semaphore journalFilled) {
        this.journal = journal;
        this.journalFilled = journalFilled;
        byteCounter = metrics.counter(MetricRegistry.name(JournallingMessageHandler.class, "written_bytes"));
    }

    @Override
    public void onEvent(RawMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        batch.add(event);

        if (endOfBatch) {
            log.debug("End of batch, journalling {} messages", batch.size());
            // write batch to journal

            final Converter converter = new Converter();
            // copy to avoid re-running this all the time
            final List<Journal.Entry> entries = Lists.newArrayList(transform(batch, converter));
            final long lastOffset = journal.write(entries);
            log.debug("Processed batch, wrote {} bytes, last journal offset: {}, signalling reader.",
                      converter.getBytesWritten(),
                      lastOffset);
            journalFilled.release();

            batch.clear();
        }
    }

    private class Converter implements Function<RawMessageEvent, Journal.Entry> {
        private long bytesWritten = 0;

        public long getBytesWritten() {
            return bytesWritten;
        }

        @Nullable
        @Override
        public Journal.Entry apply(RawMessageEvent input) {
            if (log.isTraceEnabled()) {
                log.trace("Journalling message {}", input.getMessageId());
            }
            final byte[] messageIdBytes = input.getMessageIdBytes();
            final byte[] encodedRawMessage = input.getEncodedRawMessage();

            // stats
            final int size = encodedRawMessage.length;
            bytesWritten += size;
            byteCounter.inc(size);

            // clear for gc and to avoid promotion to tenured space
            input.setMessageIdBytes(null);
            input.setEncodedRawMessage(null);
            // convert to journal entry
            return journal.createEntry(messageIdBytes, encodedRawMessage);
        }
    }
}
