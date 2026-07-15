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


package org.graylog2.indexer.indices;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.utilities.lucene.InMemorySearchableEntity;
import org.graylog2.utilities.lucene.LuceneDocBuilder;
import org.joda.time.DateTime;

public record OutdatedIndex(@JsonProperty(FIELD_INDEX_NAME) String indexName,
                            @JsonProperty(FIELD_VERSION) String version,
                            @JsonProperty(FIELD_WARM_INDEX) boolean warmIndex,
                            @JsonProperty(FIELD_MANAGED_INDEX) boolean managedIndex,
                            @JsonProperty(FIELD_ACTIVE_WRITE_INDEX) String activeWriteIndex,
                            @JsonProperty(FIELD_BEGIN) DateTime begin,
                            @JsonProperty(FIELD_END) DateTime end)
        implements Comparable<OutdatedIndex>, InMemorySearchableEntity {

    public static final String FIELD_INDEX_NAME = "indexName";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_WARM_INDEX = "warmIndex";
    public static final String FIELD_MANAGED_INDEX = "managedIndex";
    public static final String FIELD_ACTIVE_WRITE_INDEX = "activeWriteIndex";
    public static final String FIELD_SYSTEM_INDEX = "systemIndex";
    public static final String FIELD_BEGIN = "begin";
    public static final String FIELD_END = "end";

    public OutdatedIndex(String indexName, String version, boolean warmIndex) {
        this(indexName, version, warmIndex, false, null, null, null);
    }

    public OutdatedIndex(String indexName, String version, boolean warmIndex, boolean managedIndex, String activeWriteIndex) {
        this(indexName, version, warmIndex, managedIndex, activeWriteIndex, null, null);
    }

    public OutdatedIndex asManaged(boolean managed) {
        return new OutdatedIndex(indexName, version, warmIndex, managed, activeWriteIndex, begin, end);
    }

    public OutdatedIndex asActiveWriteIndex(String isActiveWriteIndex) {
        return new OutdatedIndex(indexName, version, warmIndex, managedIndex, isActiveWriteIndex, begin, end);
    }

    public OutdatedIndex withRange(IndexRange range) {
        if (range == null || isUnknownRange(range)) {
            return new OutdatedIndex(indexName, version, warmIndex, managedIndex, activeWriteIndex, null, null);
        }
        return new OutdatedIndex(indexName, version, warmIndex, managedIndex, activeWriteIndex, range.begin(), range.end());
    }

    private static boolean isUnknownRange(IndexRange range) {
        return range.begin().getMillis() == 0L && range.end().getMillis() == 0L;
    }

    @JsonProperty(FIELD_SYSTEM_INDEX)
    public boolean isSystemIndex() {
        return indexName.startsWith(".");
    }

    @JsonIgnore
    @Override
    public void buildLuceneDoc(LuceneDocBuilder builder) {
        builder.stringVal(FIELD_INDEX_NAME, indexName);
        builder.stringVal(FIELD_VERSION, version);
        builder.boolVal(FIELD_WARM_INDEX, warmIndex);
        builder.boolVal(FIELD_MANAGED_INDEX, managedIndex);
        builder.stringVal(FIELD_ACTIVE_WRITE_INDEX, activeWriteIndex);
        builder.boolVal(FIELD_SYSTEM_INDEX, isSystemIndex());
        // dateVal is not null-safe, so only add the range fields when present.
        if (begin != null) {
            builder.dateVal(FIELD_BEGIN, begin.toDate());
        }
        if (end != null) {
            builder.dateVal(FIELD_END, end.toDate());
        }
    }

    @Override
    public int compareTo(OutdatedIndex other) {
        return this.indexName.compareTo(other.indexName);
    }
}
