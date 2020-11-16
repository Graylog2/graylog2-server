/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

        // Set timestamp in event to retain access to it after we clear the raw message object below
        event.setMessageTimestamp(event.getRawMessage().getTimestamp());

        // clear for gc and to avoid promotion to tenured space
        event.setRawMessage(null);
    }
}
