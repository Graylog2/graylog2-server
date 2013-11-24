/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.radio.buffers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.radio.Radio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ProcessBufferProcessor implements EventHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private Radio radio;
    private final Meter incomingMessages;
    private final Timer processTime;
    private final Meter filteredOutMessages;
    private final Meter outgoingMessages;

    private final long ordinal;
    private final long numberOfConsumers;

    public ProcessBufferProcessor(Radio radio, final long ordinal, final long numberOfConsumers) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.radio = radio;

        incomingMessages = radio.metrics().meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = radio.metrics().meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        filteredOutMessages = radio.metrics().meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        processTime = radio.metrics().timer(name(ProcessBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
            if ((sequence % numberOfConsumers) != ordinal) {
                return;
            }

            radio.processBufferWatermark().decrementAndGet();

            incomingMessages.mark();
            final Timer.Context tcx = processTime.time();

            Message msg = event.getMessage();

            LOG.debug("Starting to process message <{}>.", msg.getId());

            radio.getTransport().send(msg);
            radio.getThroughputCounter().add(1);

            LOG.debug("Message <{}> written to RadioTransport.", msg.getId());

            outgoingMessages.mark();
            tcx.stop();
        } catch(Exception e) {
            LOG.error("Error in buffer processor.", e);
        }
    }
}
