/*
 * Copyright 2013-2014 TORCH GmbH
 *
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.filters.FilterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBufferProcessor implements EventHandler<MessageEvent> {

    public interface Factory {
        public ProcessBufferProcessor create(
                GraylogServer server,
                @Assisted("ordinal") final long ordinal,
                @Assisted("numberOfConsumers") final long numberOfConsumers
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private GraylogServer server;
    private final Meter incomingMessages;
    private final Timer processTime;
    private final Meter filteredOutMessages;
    private final Meter outgoingMessages;

    private final MetricRegistry metricRegistry;
    private final FilterRegistry filterRegistry;

    private final long ordinal;
    private final long numberOfConsumers;

    @AssistedInject
    public ProcessBufferProcessor(MetricRegistry metricRegistry,
                                  FilterRegistry filterRegistry,
                                  @Assisted GraylogServer server,
                                  @Assisted("ordinal") final long ordinal,
                                  @Assisted("numberOfConsumers") final long numberOfConsumers) {
        this.metricRegistry = metricRegistry;
        this.filterRegistry = filterRegistry;
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.server = server;

        incomingMessages = server.metrics().meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = server.metrics().meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        filteredOutMessages = server.metrics().meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        processTime = server.metrics().timer(name(ProcessBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }

        server.processBufferWatermark().decrementAndGet();
        
        incomingMessages.mark();
        final Timer.Context tcx = processTime.time();

        Message msg = event.getMessage();

        LOG.debug("Starting to process message <{}>.", msg.getId());

        for (MessageFilter filter : filterRegistry.all()) {
            Timer timer = metricRegistry.timer(name(filter.getClass(), "executionTime"));
            final Timer.Context timerContext = timer.time();

            try {
                LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                if (filter.filter(msg, server)) {
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

        LOG.debug("Finished processing message. Writing to output buffer.");
        server.getOutputBuffer().insertCached(msg, null);
        
        outgoingMessages.mark();
        tcx.stop();
    }

}
