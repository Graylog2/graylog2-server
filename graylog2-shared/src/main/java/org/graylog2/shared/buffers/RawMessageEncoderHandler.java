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
package org.graylog2.shared.buffers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class RawMessageEncoderHandler implements WorkHandler<RawMessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(RawMessageEncoderHandler.class);
    private final Meter incomingMessages;

    @Inject
    public RawMessageEncoderHandler(MetricRegistry metricRegistry) {
        incomingMessages = metricRegistry.meter(name(RawMessageEncoderHandler.class, "incomingMessages"));
    }

    @Override
    public void onEvent(RawMessageEvent event) throws Exception {
        incomingMessages.mark();
        event.encodedRawMessage = event.rawMessage.encode();
        if (log.isTraceEnabled()) {
            log.trace("Serialized message {} for journal, size {} bytes",
                      event.rawMessage.getId(), event.encodedRawMessage.length);
        }
    }
}
