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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.WorkHandler;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class ProcessBufferProcessor implements WorkHandler<MessageEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private final Meter incomingMessages;
    private final Timer processTime;
    private final Meter outgoingMessages;

    protected final MetricRegistry metricRegistry;
    private DecodingProcessor decodingProcessor;

    public ProcessBufferProcessor(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;

        incomingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        processTime = metricRegistry.timer(name(ProcessBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event) throws Exception {
        // Decode the RawMessage to a Message object. The DecodingProcessor used to be a separate handler in the
        // ProcessBuffer. Due to performance problems discovered during 1.0.0 testing, we decided to move this here.
        // TODO The DecodingProcessor does not need to be a EventHandler. We decided to do it like this to keep the change as small as possible for 1.0.0.
        decodingProcessor.onEvent(event, 0L, false);

        final List<Message> messageList = event.getMessageList();
        if (messageList == null) {
            // skip message events which could not be decoded properly
            return;
        }

        for (final Message message : messageList) {
            dispatchMessage(message);
        }
    }

    private void dispatchMessage(final Message msg) {
        incomingMessages.mark();
        final Timer.Context tcx = processTime.time();


        LOG.debug("Starting to process message <{}>.", msg.getId());

        try {
            LOG.debug("Finished processing message <{}>. Writing to output buffer.", msg.getId());
            handleMessage(msg);
        } catch (Exception e) {
            LOG.warn("Unable to process message <{}>: {}", msg.getId(), e);
        } finally {
            outgoingMessages.mark();
            tcx.stop();
        }
    }

    protected abstract void handleMessage(Message msg);

    public void setDecodingProcessor(DecodingProcessor decodingProcessor) {
        this.decodingProcessor = decodingProcessor;
    }
}
