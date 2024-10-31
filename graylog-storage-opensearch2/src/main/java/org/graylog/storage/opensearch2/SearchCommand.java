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
package org.graylog.storage.opensearch2;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

@AutoValue
abstract class SearchCommand {
    public abstract String query();
    public abstract Optional<Set<String>> streams();
    public abstract Optional<Sorting> sorting();
    public abstract Optional<String> filter();
    public abstract List<UsedSearchFilter> filters();
    public abstract Optional<TimeRange> range();
    public abstract OptionalInt limit();
    public abstract OptionalInt offset();
    public abstract OptionalLong batchSize();
    public abstract boolean highlight();

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static SearchCommand create(
            String query,
            Optional<Set<String>> streams,
            Optional<Sorting> sorting,
            Optional<String> filter,
            List<UsedSearchFilter> filters,
            Optional<TimeRange> range,
            OptionalInt limit,
            OptionalInt offset,
            OptionalLong batchSize,
            boolean highlight) {
        return new AutoValue_SearchCommand(query, streams, sorting, filter, filters, range, limit, offset, batchSize, highlight);
    }

    static SearchCommand from(SearchesConfig searchesConfig) {
        return create(searchesConfig.query(), Optional.empty(), Optional.ofNullable(searchesConfig.sorting()),
                Optional.ofNullable(searchesConfig.filter()), Collections.emptyList(), Optional.of(searchesConfig.range()),
                OptionalInt.of(searchesConfig.limit()), OptionalInt.of(searchesConfig.offset()),
                OptionalLong.empty(), true);
    }

    static SearchCommand from(ChunkCommand chunkCommand) {
        return create(chunkCommand.query(), chunkCommand.streams(), chunkCommand.sorting(),
                chunkCommand.filter(), chunkCommand.filters(), chunkCommand.range(), chunkCommand.limit(), chunkCommand.offset(),
                chunkCommand.batchSize(), chunkCommand.highlight());
    }
}
