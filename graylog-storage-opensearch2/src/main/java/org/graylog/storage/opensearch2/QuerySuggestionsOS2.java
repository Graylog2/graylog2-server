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

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionEntry;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionError;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.storage.errors.ResponseError;
import org.graylog2.plugin.Message;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.MsearchRequest;
import org.opensearch.client.opensearch.core.msearch.MultisearchBody;
import org.opensearch.client.opensearch.core.search.Suggest;
import org.opensearch.client.opensearch.core.search.Suggester;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QuerySuggestionsOS2 implements QuerySuggestionsService {
    private static final String AGG_FIELD_VALUES = "fieldvalues";
    private static final String AGG_CORRECTIONS = "corrections";

    private final OpenSearchClient client;
    private final IndexLookup indexLookup;

    @Inject
    public QuerySuggestionsOS2(OpenSearchClient client, IndexLookup indexLookup) {
        this.client = client;
        this.indexLookup = indexLookup;
    }

    @Override
    public SuggestionResponse suggest(SuggestionRequest req) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(req.streams(), req.timerange());
        final var suggestionBuilder = Suggester.of(suggesterBuilder -> suggesterBuilder
                .suggesters(AGG_CORRECTIONS, builder -> builder
                        .text(req.input())
                        .term(termSuggesterBuilder -> termSuggesterBuilder
                                .field(req.field())
                                .size(req.size()))));
        final var query = Query.of(queryBuilder -> queryBuilder.bool(boolBuilder -> boolBuilder
                .filter(filterBuilder -> filterBuilder.terms(termsBuilder -> termsBuilder.field(Message.FIELD_STREAMS)
                        .terms(termsQueryBuilder -> termsQueryBuilder.value(req.streams().stream().map(FieldValue::of).toList()))))
                .filter(filterBuilder -> filterBuilder.range(TimeRangeQueryFactory.create(req.timerange())))
                .filter(filterBuilder -> filterBuilder.exists(existsBuilder -> existsBuilder.field(req.field())))
                .filter(getPrefixQuery(req))));
        final var searchRequest = MultisearchBody.of(builder -> builder
                .size(0)
                .query(query)
                .suggest(suggestionBuilder)
                .aggregations(AGG_FIELD_VALUES, aggBuilder -> aggBuilder
                        .terms(termsBuilder -> termsBuilder
                                .field(req.field())
                                .size(req.size()))));

        final MsearchRequest msearchRequest = new MsearchRequest.Builder().searches(searchesBuilder -> searchesBuilder
                        .header(headerBuilder -> headerBuilder.index(affectedIndices.stream().toList()))
                        .body(searchRequest))
                .build();
        try {
            final var resultItem = client.search(msearchRequest, "Failed to execute aggregation");
            final var result = resultItem.result();
            final var rawAgg = result.aggregations().get(AGG_FIELD_VALUES);
            final var fieldValues = rawAgg.isSterms() ? rawAgg.sterms() : rawAgg.lterms();
            final List<SuggestionEntry> entries = fieldValues.buckets().array()
                    .stream()
                    .map(b -> new SuggestionEntry(b instanceof StringTermsBucket sBucket ? sBucket.key() : ((LongTermsBucket) b).key(), b.docCount()))
                    .collect(Collectors.toList());

            if (!entries.isEmpty()) {
                return SuggestionResponse.forSuggestions(req.field(), req.input(), entries, fieldValues.sumOtherDocCount());
            } else {
                final var suggestion = result.suggest().get(AGG_CORRECTIONS);
                final List<SuggestionEntry> corrections = suggestion.stream()
                        .filter(Suggest::isTerm)
                        .map(Suggest::term)
                        .flatMap(e -> e.options().stream()).map(o -> new SuggestionEntry(o.text(), o.freq()))
                        .collect(Collectors.toList());
                return SuggestionResponse.forSuggestions(req.field(), req.input(), corrections, null);
            }


        } catch (org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException exception) {
            final SuggestionError err = tryResponseException(exception)
                    .orElseGet(() -> parseException(exception));
            return SuggestionResponse.forError(req.field(), req.input(), err);
        }
    }

    private Query getPrefixQuery(SuggestionRequest req) {
        return switch (req.fieldType()) {
            case TEXTUAL ->
                    Query.of(queryBuilder -> queryBuilder.prefix(prefixBuilder -> prefixBuilder.field(req.field()).value(req.input())));
            default -> getScriptedPrefixQuery(req);
        };
    }

    /**
     * Unlike prefix query, this scripted implementation works also for numerical fields.
     * TODO: would it make sense to switch between this scripted implementation and the standard prefix
     * query based on our information about the field type? Would it be faster?
     */
    private static Query getScriptedPrefixQuery(SuggestionRequest req) {
        return Query.of(queryBuilder -> queryBuilder
                .script(scriptQueryBuilder -> scriptQueryBuilder
                        .script(scriptBuilder -> scriptBuilder
                                .inline(inlineBuilder -> inlineBuilder
                                        .lang("painless")
                                        .source("String val = doc[params.field].value.toString(); return val.startsWith(params.input);")
                                        .params(Map.of("field", JsonData.of(req.field()), "input", JsonData.of(req.input())))))));
    }

    private Optional<SuggestionError> tryResponseException(org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException exception) {
        return client.parseResponseException(exception)
                .map(ResponseError::error)
                .flatMap(e -> e.rootCause().stream().findFirst())
                .map(e -> SuggestionError.create(e.type(), e.reason()));
    }

    private SuggestionError parseException(org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException exception) {
        final Throwable cause = getCause(exception);
        try {
            final ParsedOpenSearchException parsed = ParsedOpenSearchException.from(cause.toString());
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
