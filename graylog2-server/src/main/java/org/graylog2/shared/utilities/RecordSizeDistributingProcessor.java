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
package org.graylog2.shared.utilities;

import com.google.protobuf.MessageLite;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import static org.graylog2.shared.utilities.InputMessageSizeDistributor.distribute;

/**
 * Serializes protobuf records into {@link RawMessage} objects, distributes the original request
 * size proportionally across them based on a weight function, and submits them for processing.
 */
public final class RecordSizeDistributingProcessor {

    private RecordSizeDistributingProcessor() {
    }

    /**
     * @param records          the journal records to process
     * @param totalRequestSize the original request's serialized size
     * @param weightExtractor  extracts the weight (e.g. LogRecord serialized size) from each record
     * @param createRawMessage factory to create a RawMessage from serialized bytes
     * @param input            the message input to submit messages to
     * @param log              logger from the calling class for trace-level size distribution logging
     */
    public static <T extends MessageLite> void processRecords(List<T> records,
                                                               long totalRequestSize,
                                                               ToLongFunction<T> weightExtractor,
                                                               Function<byte[], RawMessage> createRawMessage,
                                                               MessageInput input,
                                                               Logger log) {
        final List<Long> weights = records.stream()
                .map(weightExtractor::applyAsLong)
                .toList();
        final List<Long> sizes = distribute(totalRequestSize, weights);

        if (log.isTraceEnabled()) {
            log.trace("Distributing total input size {} across {} records: {}", totalRequestSize, records.size(), sizes);
        }

        for (int i = 0; i < records.size(); i++) {
            final RawMessage raw = createRawMessage.apply(records.get(i).toByteArray());
            raw.setInputMessageSize(sizes.get(i));
            input.processRawMessage(raw);
        }
    }
}