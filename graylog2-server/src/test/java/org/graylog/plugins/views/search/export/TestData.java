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
package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.defaultTimeRange;

public class TestData {

    public static Query.Builder validQueryBuilderWith(SearchType searchType) {
        return validQueryBuilder().searchTypes(ImmutableSet.of(searchType));
    }

    public static Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString())
                .timerange(defaultTimeRange())
                .query(new BackendQuery.Fallback());
    }

    public static SimpleMessageChunk simpleMessageChunk(String fieldNames, Object[]... messageValues) {
        LinkedHashSet<SimpleMessage> messages = Arrays.stream(messageValues)
                .map(s -> simpleMessage(fieldNames, s))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(setFrom(fieldNames), messages);
    }

    public static SimpleMessageChunk simpleMessageChunkWithIndexNames(String fieldNames, Object[]... messageValues) {
        LinkedHashSet<SimpleMessage> messages = Arrays.stream(messageValues)
                .map(values -> simpleMessageWithIndexName(fieldNames, values))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(setFrom(fieldNames), messages);
    }

    private static SimpleMessage simpleMessageWithIndexName(String fieldNames, Object[] values) {
        String indexName = (String) values[0];
        Object[] fieldValues = Arrays.copyOfRange(values, 1, values.length);
        return simpleMessage(indexName, fieldNames, fieldValues);
    }

    private static LinkedHashSet<String> setFrom(String fieldNames) {
        return Arrays.stream(fieldNames.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static SimpleMessage simpleMessage(String indexName, String fieldNames, Object[] values) {
        LinkedHashSet<String> names = setFrom(fieldNames);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        int i = 0;
        for (String name : names) {
            fields.put(name, values[i++]);
        }
        return SimpleMessage.from(indexName, fields);
    }

    public static SimpleMessage simpleMessage(String fieldNames, Object[] values) {
        return simpleMessage("some-index", fieldNames, values);
    }

    public static RelativeRange relativeRange(int range) {
        try {
            return RelativeRange.create(range);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}
