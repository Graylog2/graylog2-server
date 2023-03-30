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

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionEntry;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionError;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.ScriptQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.script.Script;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.script.ScriptType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.SuggestBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.SuggestBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.term.TermSuggestion;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.graylog.storage.errors.ResponseError;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QuerySuggestionsES7 implements QuerySuggestionsService {

    private final ElasticsearchClient client;
    private final IndexLookup indexLookup;

    @Inject
    public QuerySuggestionsES7(ElasticsearchClient client, IndexLookup indexLookup) {
        this.client = client;
        this.indexLookup = indexLookup;
    }

    @Override
    public SuggestionResponse suggest(SuggestionRequest req) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(req.streams(), req.timerange());
        final TermSuggestionBuilder suggestionBuilder = SuggestBuilders.termSuggestion(req.field()).text(req.input()).size(req.size());
        final BoolQueryBuilder query = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery(Message.FIELD_STREAMS, req.streams()))
                .filter(TimeRangeQueryFactory.create(req.timerange()))
                .filter(QueryBuilders.existsQuery(req.field()))
                .filter(getPrefixQuery(req));
        final SearchSourceBuilder search = new SearchSourceBuilder()
                .query(query)
                .size(0)
                .aggregation(AggregationBuilders.terms("fieldvalues").field(req.field()).size(req.size()))
                .suggest(new SuggestBuilder().addSuggestion("corrections", suggestionBuilder));

        try {
            final SearchResponse result = client.singleSearch(new SearchRequest(affectedIndices.toArray(new String[]{})).source(search), "Failed to execute aggregation");
            final ParsedTerms fieldValues = result.getAggregations().get("fieldvalues");
            final List<SuggestionEntry> entries = fieldValues.getBuckets().stream().map(b -> new SuggestionEntry(b.getKeyAsString(), b.getDocCount())).collect(Collectors.toList());

            if(!entries.isEmpty()) {
                return SuggestionResponse.forSuggestions(req.field(), req.input(), entries, fieldValues.getSumOfOtherDocCounts());
            } else {
                TermSuggestion suggestion = result.getSuggest().getSuggestion("corrections");
                final List<SuggestionEntry> corrections = suggestion.getEntries().stream().flatMap(e -> e.getOptions().stream()).map(o -> new SuggestionEntry(o.getText().string(), o.getFreq())).collect(Collectors.toList());
                return SuggestionResponse.forSuggestions(req.field(), req.input(), corrections, null);
            }


        } catch (org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException exception) {
            final SuggestionError err = tryResponseException(exception)
                    .orElseGet(() -> parseException(exception));
            return SuggestionResponse.forError(req.field(), req.input(), err);
        }
    }


    private QueryBuilder getPrefixQuery(SuggestionRequest req) {
        return switch (req.fieldType()) {
            case TEXTUAL -> QueryBuilders.prefixQuery(req.field(), req.input());
            default -> getScriptedPrefixQuery(req);
        };
    }

    /**
     * Unlike prefix query, this scripted implementation works also for numerical fields.
     * TODO: would it make sense to switch between this scripted implementation and the standard prefix
     * query based on our information about the field type? Would it be faster?
     */
    private static ScriptQueryBuilder getScriptedPrefixQuery(SuggestionRequest req) {
        final Script script = new Script(ScriptType.INLINE, "painless",
                "String val = doc[params.field].value.toString(); return val.startsWith(params.input);",
                ImmutableMap.of("field", req.field(), "input", req.input()));
        return QueryBuilders.scriptQuery(script);
    }

    private Optional<SuggestionError> tryResponseException(org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException exception) {
        return client.parseResponseException(exception)
                .map(ResponseError::error)
                .flatMap(e -> e.rootCause().stream().findFirst())
                .map(e -> SuggestionError.create(e.type(), e.reason()));
    }

    private SuggestionError parseException(org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException exception) {
        final Throwable cause = getCause(exception);
        try {
            final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(cause.toString());
            return SuggestionError.create(parsed.type(), parsed.reason());
        } catch (final IllegalArgumentException iae) {
            return SuggestionError.create("Aggregation error", cause.getMessage());
        }
    }

    private Throwable getCause(Throwable exception) {
        if (exception.getCause() != null) {
            return getCause(exception.getCause());
        } else {
            return exception;
        }
    }
}
