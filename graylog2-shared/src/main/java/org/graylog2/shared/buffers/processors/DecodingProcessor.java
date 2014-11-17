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

package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;

import java.util.Map;

public class DecodingProcessor implements EventHandler<MessageEvent> {
    public interface Factory {
        public DecodingProcessor create(@Assisted("parseTime") Timer parseTime);
    }

    private final Map<String, Codec.Factory<? extends Codec>> codecFactory;
    private final Timer parseTime;

    @AssistedInject
    public DecodingProcessor(Map<String, Codec.Factory<? extends Codec>> codecFactory,
                             @Assisted("parseTime") Timer parseTime) {
        this.codecFactory = codecFactory;

        /*
        failures = localRegistry.meter("failures");
        incompleteMessages = localRegistry.meter("incompleteMessages");
        rawSize = localRegistry.meter("rawSize");
        */
        this.parseTime = parseTime;
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final RawMessage raw = event.getRaw();
        final Codec codec = codecFactory.get(raw.getPayloadType()).create(raw.getCodecConfig());

        /*
        final Message message;


        if (message == null) {
            failures.mark();
            LOG.warn("Could not decode message. Dropping message {}", rawMessage.getId());
            return;
        }
        if (!message.isComplete()) {
            incompleteMessages.mark();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping incomplete message. Parsed fields: [{}]", message.getFields());
            }
            return;
        }

        processedMessages.mark();
        */

        try (Timer.Context ignored = parseTime.time()) {
            event.setMessage(codec.decode(raw));
        } catch (RuntimeException e) {
            //failures.mark();
        }
    }
}
