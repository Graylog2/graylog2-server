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
package org.graylog.storage.elasticsearch7;

import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.datastream.Policy;
import org.graylog2.indexer.indices.Template;
import jakarta.annotation.Nonnull;

public class DataStreamAdapterES7 implements DataStreamAdapter {
    private static final String ERROR_MESSAGE = "Data Streams not supported in Elastic Search";

    @Override
    public boolean ensureDataStreamTemplate(@Nonnull String templateName, @Nonnull Template template, @Nonnull String timestampField) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void createDataStream(String dataStreamName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void applyIsmPolicy(@Nonnull String dataStreamName, @Nonnull Policy policy) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void setNumberOfReplicas(@Nonnull String dataStreamName, @Nonnull int replicas) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }
}
