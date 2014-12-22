/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.journal;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.metrics.HdrHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;

import static com.codahale.metrics.MetricRegistry.name;

public class JournalReader extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(JournalReader.class);
    private final Journal journal;
    private final ProcessBuffer processBuffer;
    private final Semaphore journalFilled;
    private final MetricRegistry metricRegistry;
    private Histogram requestedReadCount;
    private final Counter readBlocked;
    private Thread executionThread;

    @Inject
    public JournalReader(Journal journal,
                         ProcessBuffer processBuffer,
                         @Named("JournalSignal") Semaphore journalFilled,
                         MetricRegistry metricRegistry) {
        this.journal = journal;
        this.processBuffer = processBuffer;
        this.journalFilled = journalFilled;
        this.metricRegistry = metricRegistry;
        readBlocked = metricRegistry.counter(name(this.getClass(), "readBlocked"));
    }

    @Override
    protected void startUp() throws Exception {
        executionThread = Thread.currentThread();
    }

    @Override
    protected void triggerShutdown() {
        executionThread.interrupt();
    }

    @Override
    protected void run() throws Exception {
        try {
            requestedReadCount = metricRegistry.register(name(this.getClass(), "requestedReadCount"), new HdrHistogram(processBuffer.getRingBufferSize(), 3));
        } catch (IllegalArgumentException e) {
            log.warn("Metric already exists", e);
            throw e;
        }

        while (isRunning()) {
            // approximate count to read from the journal to backfill the processing chain
            final long remainingCapacity = processBuffer.getRemainingCapacity();
            requestedReadCount.update(remainingCapacity);
            final List<Journal.JournalReadEntry> encodedRawMessages = journal.read(remainingCapacity);
            if (encodedRawMessages.isEmpty()) {
                log.debug("No messages to read from Journal, waiting until the writer adds more messages.");
                // block until something is written to the journal again
                try {
                    readBlocked.inc();
                    journalFilled.acquire();
                } catch (InterruptedException ignored) {
                    // this can happen when we are blocked but the system wants to shut down. We don't have to do anything in that case.
                    continue;
                }
                log.debug("Messages have been written to Journal, continuing to read.");
                // we don't care how many messages were inserted in the meantime, we'll read all of them eventually
                journalFilled.drainPermits();
            } else {
                log.debug("Processing {} messages from journal.", encodedRawMessages.size());
                for (final Journal.JournalReadEntry encodedRawMessage : encodedRawMessages) {
                    final RawMessage rawMessage = RawMessage.decode(encodedRawMessage.getPayload(),
                                                                    encodedRawMessage.getOffset());
                    if (rawMessage == null) {
                        // never insert null objects into the ringbuffer, as that is useless
                        continue;
                    }

                    processBuffer.insertBlocking(rawMessage);
                }
            }
        }
        log.info("Stopping.");
    }


}
