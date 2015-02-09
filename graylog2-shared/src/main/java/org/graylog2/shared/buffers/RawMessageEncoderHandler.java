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
package org.graylog2.shared.buffers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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
        event.setEncodedRawMessage(event.getRawMessage().encode());
        event.setMessageIdBytes(event.getRawMessage().getIdBytes());
        
        if (log.isTraceEnabled()) {
            log.trace("Serialized message {} for journal, size {} bytes",
                      event.getRawMessage().getId(), event.getEncodedRawMessage().length);
        }
        
        // clear for gc and to avoid promotion to tenured space
        event.setRawMessage(null);
    }
}
