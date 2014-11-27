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
package org.graylog2.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerProcessBufferProcessor extends ProcessBufferProcessor {
    private final Configuration configuration;

    public interface Factory {
        public ServerProcessBufferProcessor create(
                OutputBuffer outputBuffer,
                @Assisted("ordinal") final long ordinal,
                @Assisted("numberOfConsumers") final long numberOfConsumers
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerProcessBufferProcessor.class);
    private final OutputBuffer outputBuffer;
    private final Meter filteredOutMessages;
    private final List<MessageFilter> filterRegistry;


    @AssistedInject
    public ServerProcessBufferProcessor(MetricRegistry metricRegistry,
                                  Set<MessageFilter> filterRegistry,
                                  Configuration configuration,
                                  @Assisted("ordinal") final long ordinal,
                                  @Assisted("numberOfConsumers") final long numberOfConsumers,
                                  @Assisted OutputBuffer outputBuffer) {
        super(metricRegistry, ordinal, numberOfConsumers);
        this.configuration = configuration;

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
    protected void handleMessage(Message msg) {

        if (filterRegistry.size() == 0)
            throw new RuntimeException("Empty filter registry!");

        for (MessageFilter filter : filterRegistry) {
            Timer timer = metricRegistry.timer(name(filter.getClass(), "executionTime"));
            final Timer.Context timerContext = timer.time();

            try {
                LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                if (filter.filter(msg)) {
                    LOG.debug("Filter [{}] marked message <{}> to be discarded. Dropping message.", filter.getName(), msg.getId());
                    filteredOutMessages.mark();
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filter.getName() +"] on message <" + msg.getId() +">: ", e);
            } finally {
                timerContext.stop();
            }
        }

        if (configuration.isDisableOutputCache()) {
            LOG.debug("Finished processing message. Writing to output buffer.");
            for (;;) {
                try {
                    outputBuffer.insertFailFast(msg, null);
                    break;
                } catch (BufferOutOfCapacityException e) {
                    LOG.debug("Output buffer is full. Retrying.");
                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            LOG.debug("Finished processing message. Writing to output cache.");
            outputBuffer.insertCached(msg, null);
        }
    }

    // default visibility for tests
    List<MessageFilter> getFilterRegistry() {
        return filterRegistry;
    }
}
