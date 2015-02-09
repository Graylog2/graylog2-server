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
package org.graylog2.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerProcessBufferProcessor extends ProcessBufferProcessor {
    private final Configuration configuration;
    private final ServerStatus serverStatus;
    private final Journal journal;

    private static final Logger LOG = LoggerFactory.getLogger(ServerProcessBufferProcessor.class);
    private final OutputBuffer outputBuffer;
    private final Meter filteredOutMessages;
    private final List<MessageFilter> filterRegistry;


    @Inject
    public ServerProcessBufferProcessor(MetricRegistry metricRegistry,
                                  Set<MessageFilter> filterRegistry,
                                  Configuration configuration,
                                  ServerStatus serverStatus,
                                  OutputBuffer outputBuffer,
                                  Journal journal) {
        super(metricRegistry);
        this.configuration = configuration;
        this.serverStatus = serverStatus;
        this.journal = journal;

        // we need to keep this sorted properly, so that the filters run in the correct order
        this.filterRegistry = Ordering.from(new Comparator<MessageFilter>() {
            @Override
            public int compare(MessageFilter filter1, MessageFilter filter2) {
                return ComparisonChain.start()
                        .compare(filter1.getPriority(), filter2.getPriority())
                        .compare(filter1.getName(), filter2.getName())
                        .result();
            }
        }).immutableSortedCopy(filterRegistry);

        this.outputBuffer = outputBuffer;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
    }

    @Override
    protected void handleMessage(@Nonnull Message msg) {

        if (filterRegistry.size() == 0)
            throw new RuntimeException("Empty filter registry!");

        for (final MessageFilter filter : filterRegistry) {
            final String timerName = name(filter.getClass(), "executionTime");
            final Timer timer = metricRegistry.timer(timerName);
            final Timer.Context timerContext = timer.time();

            try {
                LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                if (filter.filter(msg)) {
                    LOG.debug("Filter [{}] marked message <{}> to be discarded. Dropping message.", filter.getName(), msg.getId());
                    filteredOutMessages.mark();
                    journal.markJournalOffsetCommitted(msg.getJournalOffset());
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filter.getName() +"] on message <" + msg.getId() +">: ", e);
            } finally {
                final long elapsedNanos = timerContext.stop();
                msg.recordTiming(serverStatus, timerName, elapsedNanos);
            }
        }

        LOG.debug("Finished processing message. Writing to output buffer.");
        outputBuffer.insertBlocking(msg);
    }

    // default visibility for tests
    List<MessageFilter> getFilterRegistry() {
        return filterRegistry;
    }
}
