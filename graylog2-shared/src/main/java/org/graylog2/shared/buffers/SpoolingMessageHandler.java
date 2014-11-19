/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpoolingMessageHandler implements EventHandler<RawMessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(SpoolingMessageHandler.class);

    private final List<RawMessageEvent> batch = Lists.newArrayList();
    private final Counter byteCounter;

    @Inject
    public SpoolingMessageHandler(MetricRegistry metrics) {
        byteCounter = metrics.counter(MetricRegistry.name(SpoolingMessageHandler.class,
                                                          "written_bytes"));
    }

    @Override
    public void onEvent(RawMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        batch.add(event);

        if (endOfBatch) {
            log.info("End of batch, journalling {} messages", batch.size());
            // write batch to journal

            long counter = 0;
            for (RawMessageEvent evt : batch) {
                log.info("Journalling message {}", evt.rawMessage.getId());
                // TODO actually write to journal
                final int size = evt.encodedRawMessage.length;
                counter += size;
                byteCounter.inc(size);
            }
            log.info("Processed batch, wrote {} bytes", counter);

            batch.clear();
        }
    }
}
