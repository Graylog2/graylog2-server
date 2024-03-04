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
package org.graylog2.plugin;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingResult;
import org.graylog2.indexer.messages.IndexingResultCallback;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * A Message that is used for System purposes like restoring Archives.
 * The message has the following properties:
 * <ul>
 *  <li>A size of 0, so its traffic is not accounted</li>
 *  <li>A single predetermined IndexSet</li>
 *  <li>No streams, so it will only be routed to the {@link org.graylog2.outputs.DefaultMessageOutput}</li>
 * </ul>
 */
public class SystemMessage extends Message {
    private final IndexSet indexSet;
    private final IndexingResultCallback resultCallback;

    public SystemMessage(IndexSet indexSet, Map<String, Object> fields, @Nullable IndexingResultCallback resultCallback) {
        super(fields);
        this.indexSet = indexSet;
        this.resultCallback = resultCallback;
    }

    public void runIndexingResultCallback(IndexingResult result) {
        if (resultCallback != null) {
            resultCallback.accept(result);
        }
    }

    @Override
    public Set<IndexSet> getIndexSets() {
        return Set.of(indexSet);
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public Set<Stream> getStreams() {
        return Set.of();
    }

    @Override
    public Object getMessageQueueId() {
        return null;
    }

}
