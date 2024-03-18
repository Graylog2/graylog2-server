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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.ranges.IndexRange;

import java.util.Map;
import java.util.Set;

public record ExplainResults(String search_id, SearchResult search, Set<SearchError> searchErrors) {

    public record SearchResult(Map<String, QueryExplainResult> queries) {
    }

    public record QueryExplainResult(Map<String, ExplainResult> searchTypes) {
    }

    public record ExplainResult(String queryString, Set<IndexRangeResult> searchedIndexRanges) {
    }

    public record IndexRangeResult(String indexName, long begin, long end, boolean isWarmTiered) {
        public IndexRangeResult(String indexName, long begin, long end) {
            this(indexName, begin, end, MongoIndexSet.indexHasWarmInfix(indexName));
        }

        public static IndexRangeResult fromIndexRange(IndexRange indexRange) {
            return new IndexRangeResult(indexRange.indexName(), indexRange.begin().getMillis(), indexRange.end().getMillis());
        }
    }
}
