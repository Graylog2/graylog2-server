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

import com.google.inject.Inject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;

import java.util.Map;

public class DecodingProcessor implements EventHandler<MessageEvent> {
    private final Map<String, Codec.Factory<? extends Codec>> codecFactory;

    @Inject
    public DecodingProcessor(Map<String, Codec.Factory<? extends Codec>> codecFactory) {
        this.codecFactory = codecFactory;
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final RawMessage raw = event.getRaw();
        final Codec codec = codecFactory.get(raw.getPayloadType()).create(raw.getCodecConfig());

        event.setMessage(codec.decode(raw));
    }
}
