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
package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionEntry;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionError;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.IndexMapping;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuerySugggestionsES6 implements QuerySuggestionsService {

    private final JestClient jestClient;
    private final IndexLookup indexLookup;

    @Inject
    public QuerySugggestionsES6(JestClient jestClient, IndexLookup indexLookup) {
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;
    }

    @Override
    public SuggestionResponse suggest(SuggestionRequest req) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(req.streams(), req.timerange());
        final SearchSourceBuilder search = new SearchSourceBuilder()
                .query(QueryBuilders.prefixQuery(req.field(), req.input()))
                .size(0)
                .aggregation(AggregationBuilders.terms("fieldvalues").field(req.field()).size(10));

        final Search.Builder searchBuilder = new Search.Builder(search.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                .allowNoIndices(false)
                .ignoreUnavailable(false);

        try {
            final SearchResult result = JestUtils.execute(jestClient, searchBuilder.build(), () -> "Unable to perform aggregation: ");

            final TermsAggregation aggregation = result.getAggregations().getTermsAggregation("fieldvalues");
            final List<SuggestionEntry> entries = aggregation.getBuckets().stream().map(b -> new SuggestionEntry(b.getKeyAsString(), b.getCount())).collect(Collectors.toList());
            return SuggestionResponse.forSuggestions(req.field(), req.input(), entries);
        } catch (Exception e) {
            final SuggestionError err = SuggestionError.create(e.getClass().getSimpleName(), e.getMessage());
            return SuggestionResponse.forError(req.field(), req.input(), err);
        }
    }
}
