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
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.PrefixQueryBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.suggest.SuggestBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.suggest.SuggestBuilders;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.IndexMapping;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QuerySuggestionsES6 implements QuerySuggestionsService {

    private final JestClient jestClient;
    private final IndexLookup indexLookup;

    @Inject
    public QuerySuggestionsES6(JestClient jestClient, IndexLookup indexLookup) {
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;
    }

    @Override
    public SuggestionResponse suggest(SuggestionRequest req) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(req.streams(), req.timerange());
        final PrefixQueryBuilder prefixQuery = QueryBuilders.prefixQuery(req.field(), req.input());
        final BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(prefixQuery);
        req.filteringQuery().map(QueryBuilders::queryStringQuery).ifPresent(mainQuery::filter);
        final SearchSourceBuilder search = new SearchSourceBuilder()
                .query(mainQuery)
                .size(0)
                .aggregation(AggregationBuilders.terms("fieldvalues").field(req.field()).size(req.size()))
                .suggest(new SuggestBuilder()
                        .addSuggestion("corrections",
                                SuggestBuilders.termSuggestion(req.field()).text(req.input()).size(req.size())));

        final Search.Builder searchBuilder = new Search.Builder(search.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                .allowNoIndices(false)
                .ignoreUnavailable(false);

        try {
            final SearchResult result = JestUtils.execute(jestClient, searchBuilder.build(), () -> "Unable to perform aggregation: ");

            final TermsAggregation aggregation = result.getAggregations().getTermsAggregation("fieldvalues");
            final List<SuggestionEntry> entries = aggregation.getBuckets().stream().map(b -> new SuggestionEntry(b.getKeyAsString(), b.getCount())).collect(Collectors.toList());
            if(!entries.isEmpty()) {
                return SuggestionResponse.forSuggestions(req.field(), req.input(), entries, aggregation.getSumOtherDocCount());
            } else {
                final List<SuggestionEntry> corrections = Optional.of(result.getJsonObject())
                        .map(o -> o.get("suggest"))
                        .map(o -> o.get("corrections"))
                        .map(o -> o.get(0))
                        .map(o -> o.get("options"))
                        .map(options -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(options.elements(), Spliterator.ORDERED), false)
                                .map(option -> new SuggestionEntry(option.get("text").textValue(), option.get("freq").longValue()))
                                .collect(Collectors.toList()))
                        .orElseGet(Collections::emptyList);
                return SuggestionResponse.forSuggestions(req.field(), req.input(), corrections, null);
            }
        } catch (Exception e) {
            final SuggestionError err = SuggestionError.create(e.getClass().getSimpleName(), e.getMessage());
            return SuggestionResponse.forError(req.field(), req.input(), err);
        }
    }
}
