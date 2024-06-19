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
package org.graylog2.outputs.filter;

import org.graylog2.outputs.BlockingBatchedESOutput;
import org.graylog2.plugin.Message;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public record DefaultFilteredMessage(Message message, Set<String> outputs) implements FilteredMessage {
    public DefaultFilteredMessage {
        requireNonNull(message, "message cannot be null");
        requireNonNull(outputs, "outputs cannot be null");
    }

    @Override
    public boolean isIndexed() {
        // We consider a message indexed if it is written to the indexer output.
        return outputs.contains(BlockingBatchedESOutput.FILTER_KEY);
    }
}
