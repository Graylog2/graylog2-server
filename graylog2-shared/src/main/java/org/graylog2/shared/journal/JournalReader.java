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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class JournalReader extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(JournalReader.class);
    private final Journal journal;
    private final EventBus eventBus;
    private final ProcessBuffer processBuffer;
    private final Semaphore journalFilled;
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private Thread executionThread;

    @Inject
    public JournalReader(Journal journal, EventBus eventBus, ProcessBuffer processBuffer, @Named("JournalSignal") Semaphore journalFilled) {
        this.journal = journal;
        this.eventBus = eventBus;
        this.processBuffer = processBuffer;
        this.journalFilled = journalFilled;

        eventBus.register(this);
    }

    @Subscribe
    public void listener(String notification) {
        if ("ProcessBufferInitialized".equals(notification)) {
            startLatch.countDown();
        }
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
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    @Override
    protected void run() throws Exception {

        // we need to wait for the ProcessBuffer to start its disruptor
        startLatch.await();

        while (isRunning()) {
            final List<Journal.JournalReadEntry> encodedRawMessages = journal.read();
            if (encodedRawMessages.isEmpty()) {
                log.info("No messages to read from Journal, waiting until the writer adds more messages.");
                // block until something is written to the journal again
                try {
                    journalFilled.acquire();
                } catch (InterruptedException ignored) {
                    // this can happen when we are blocked but the system wants to shut down. We don't have to do anything in that case.
                    continue;
                }
                log.info("Messages have been written to Journal, continuing to read.");
                // we don't care how many messages were inserted in the meantime, we'll read all of them eventually
                journalFilled.drainPermits();
            } else {
                log.info("Processing {} messages from journal.", encodedRawMessages.size());
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
