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

package org.graylog.storage.opensearch3.views;

import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MutableSearchRequestBuilder {

    Query query;
    Integer from;
    Integer size;
    TrackHits trackTotalHits;
    List<String> indices;
    Boolean allowNoIndices;
    Boolean ignoreUnavailable;
    List<ExpandWildcard> expandWildcards;
    Time cancelAfterTimeInterval;
    String preference;
    List<SortOptions> sort;
    SourceConfig source;
    Highlight highlight;
    Map<String, Aggregation> aggregations = new HashMap<>();
    List<FieldValue> searchAfter;

    public MutableSearchRequestBuilder query(Query query) {
        this.query = query;
        return this;
    }

    public Query query() {
        return query;
    }

    public MutableSearchRequestBuilder from(Integer from) {
        this.from = from;
        return this;
    }

    public MutableSearchRequestBuilder size(Integer size) {
        this.size = size;
        return this;
    }

    public MutableSearchRequestBuilder trackTotalHits(TrackHits trackTotalHits) {
        this.trackTotalHits = trackTotalHits;
        return this;
    }

    public MutableSearchRequestBuilder index(List<String> indices) {
        this.indices = indices;
        return this;
    }

    public MutableSearchRequestBuilder allowNoIndices(Boolean allowNoIndices) {
        this.allowNoIndices = allowNoIndices;
        return this;
    }

    public MutableSearchRequestBuilder ignoreUnavailable(Boolean ignoreUnavailable) {
        this.ignoreUnavailable = ignoreUnavailable;
        return this;
    }

    public MutableSearchRequestBuilder expandWildcards(ExpandWildcard... expandWildcards) {
        this.expandWildcards = Arrays.asList(expandWildcards);
        return this;
    }

    public MutableSearchRequestBuilder expandWildcards(List<ExpandWildcard> expandWildcards) {
        this.expandWildcards = expandWildcards;
        return this;
    }

    public MutableSearchRequestBuilder cancelAfterTimeInterval(Time cancelAfterTimeInterval) {
        this.cancelAfterTimeInterval = cancelAfterTimeInterval;
        return this;
    }

    public MutableSearchRequestBuilder preference(String preference) {
        this.preference = preference;
        return this;
    }

    public MutableSearchRequestBuilder sort(List<SortOptions> sort) {
        this.sort = sort;
        return this;
    }

    public MutableSearchRequestBuilder sort(SortOptions... sort) {
        if (this.sort == null) {
            this.sort = new ArrayList<>();
        }
        this.sort.addAll(Arrays.asList(sort));
        return this;
    }

    public MutableSearchRequestBuilder source(SourceConfig source) {
        this.source = source;
        return this;
    }

    public MutableSearchRequestBuilder highlight(Highlight highlight) {
        this.highlight = highlight;
        return this;
    }

    public Highlight highlight() {
        return highlight;
    }

    public MutableSearchRequestBuilder aggregations(Map<String, Aggregation> aggregations) {
        this.aggregations = aggregations;
        return this;
    }

    public MutableSearchRequestBuilder aggregation(String name, Aggregation aggregation) {
//        if (this.aggregations.containsKey(name)) {
//            throw new IllegalArgumentException("Aggregation already exists: " + name);
//        }
        this.aggregations.put(name, aggregation);
        return this;
    }

    public MutableSearchRequestBuilder searchAfter(List<FieldValue> searchAfter) {
        this.searchAfter = searchAfter;
        return this;
    }

    SearchRequest build() {
        return SearchRequest.of(b -> {
            if (query != null) {
                b.query(query);
            }
            if (from != null) {
                b.from(from);
            }
            if (size != null) {
                b.size(size);
            }
            if (trackTotalHits != null) {
                b.trackTotalHits(trackTotalHits);
            }
            if (indices != null) {
                b.index(indices);
            }
            if (allowNoIndices != null) {
                b.allowNoIndices(allowNoIndices);
            }
            if (ignoreUnavailable != null) {
                b.ignoreUnavailable(ignoreUnavailable);
            }
            if (expandWildcards != null) {
                b.expandWildcards(expandWildcards);
            }
            if (sort != null) {
                b.sort(sort);
            }
            if (source != null) {
                b.source(source);
            }
            if (highlight != null) {
                b.highlight(highlight);
            }
            if (aggregations != null) {
                b.aggregations(aggregations);
            }
            if (searchAfter != null) {
                b.searchAfter(searchAfter);
            }
            return b;
        });
    }

    public MutableSearchRequestBuilder copy() {
        return new MutableSearchRequestBuilder()
                .query(query)
                .from(from)
                .size(size)
                .trackTotalHits(trackTotalHits)
                .index(indices)
                .allowNoIndices(allowNoIndices)
                .ignoreUnavailable(ignoreUnavailable)
                .expandWildcards(expandWildcards)
                .sort(sort)
                .source(source)
                .highlight(highlight)
                .aggregations(aggregations)
                .searchAfter(searchAfter);
    }

    @Override
    public String toString() {
        return build().toJsonString();
    }
}
