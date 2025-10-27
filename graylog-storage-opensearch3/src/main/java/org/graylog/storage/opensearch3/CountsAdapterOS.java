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
package org.graylog.storage.opensearch3;

import org.graylog2.indexer.counts.CountsAdapter;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;

public record CountsAdapterOS(OfficialOpensearchClient client) implements CountsAdapter {

    @Override
    public long totalCount(List<String> indices) {
        try {
            return client.sync().count(requestBuilder -> requestBuilder.index(indices)).count();
        } catch (IOException e) {
            throw new RuntimeException("Fetching message count failed for indices " + indices, e);
        }

    }
}
