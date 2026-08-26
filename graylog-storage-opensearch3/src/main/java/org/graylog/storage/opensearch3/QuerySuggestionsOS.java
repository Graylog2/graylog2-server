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

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionEntry;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionError;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog2.plugin.Message;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.BuiltinScriptLanguage;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.AggregateVariant;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregateBase;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Suggest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QuerySuggestionsOS implements QuerySuggestionsService {

    private final OfficialOpensearchClient client;
    private final IndexLookup indexLookup;

    @Inject
    public QuerySuggestionsOS(OfficialOpensearchClient client, IndexLookup indexLookup) {
        this.client = client;
        this.indexLookup = indexLookup;
    }

    @Override
    public SuggestionResponse suggest(SuggestionRequest req, Duration timeout) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(req.streams(), req.timerange());
        final Query boolQuery = buildQuery(req);

        final SearchRequest searchRequest = SearchRequest.of(sr -> sr
                .index(affectedIndices.stream().toList())
                .query(boolQuery)
                .size(0)
                .aggregations("fieldvalues", a -> a
                        .terms(t -> t.field(req.field()).size(req.size()))
                )
                .suggest(sg -> sg
                        .suggesters("corrections", fs -> fs
                                .text(req.input())
                                .term(ts -> ts
                                        .field(req.field())
                                        .size(req.size())
                                )
                        )
                )
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.Open)
                .cancelAfterTimeInterval(Time.of(t -> t.time(timeout.toMillis() + "ms")))
        );

        try {
            final SearchResponse<Void> result = client.syncWithoutErrorMapping().search(searchRequest, Void.class);
            return processResponse(req, result);
        } catch (OpenSearchException exception) {
            final SuggestionError err = extractError(exception);
            return SuggestionResponse.forError(req.field(), req.input(), err);
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute suggestion query", e);
        }
    }

    private Query buildQuery(SuggestionRequest req) {
        final Query streamsFilter = Query.of(q -> q.terms(t -> t
                .field(Message.FIELD_STREAMS)
                .terms(tf -> tf.value(req.streams().stream().map(FieldValue::of).toList()))
        ));

        final RangeQuery rangeQuery = TimeRangeQueryFactory.createTimeRangeQuery(req.timerange());
        final Query timeRangeFilter = Query.of(q -> q.range(rangeQuery));

        final Query existsFilter = Query.of(q -> q.exists(e -> e.field(req.field())));
        final Query prefixFilter = getPrefixQuery(req);

        return Query.of(q -> q.bool(b -> b
                .filter(streamsFilter, timeRangeFilter, existsFilter, prefixFilter)
        ));
    }

    private SuggestionResponse processResponse(SuggestionRequest req, SearchResponse<Void> result) {

        final Aggregate fieldValuesAgg = result.aggregations().get("fieldvalues");
        if (fieldValuesAgg != null) {
            AggregateVariant rawAggregation = fieldValuesAgg._get();
            if (!(rawAggregation instanceof TermsAggregateBase<?> fieldValues)) {
                throw new IllegalArgumentException("Aggregate must implement TermsAggregateBase");
            }
            final List<SuggestionEntry> entries = fieldValues.buckets().array().stream()
                    .map(b -> {
                        if (!(b instanceof MultiBucketBase bucket)) {
                            throw new IllegalArgumentException("Bucket must implement MultiBucketBase");
                        }
                        return new SuggestionEntry(OSSerializationUtils.getBucketKeyAsString(bucket), bucket.docCount());
                    })
                    .collect(Collectors.toList());

            if (!entries.isEmpty()) {
                return SuggestionResponse.forSuggestions(req.field(), req.input(), entries, fieldValues.sumOtherDocCount());
            }
        }

        final List<Suggest<Void>> corrections = result.suggest() != null ? result.suggest().get("corrections") : null;
        if (corrections != null) {
            final List<SuggestionEntry> correctionEntries = corrections.stream()
                    .flatMap(s -> s.term().options().stream())
                    .map(o -> new SuggestionEntry(o.text(), o.freq()))
                    .collect(Collectors.toList());
            return SuggestionResponse.forSuggestions(req.field(), req.input(), correctionEntries, null);
        }

        return SuggestionResponse.forSuggestions(req.field(), req.input(), List.of(), null);
    }

    private Query getPrefixQuery(SuggestionRequest req) {
        return switch (req.fieldType()) {
            case TEXTUAL -> Query.of(q -> q.prefix(p -> p.field(req.field()).value(req.input())));
            default -> getScriptedPrefixQuery(req);
        };
    }

    private Query getScriptedPrefixQuery(SuggestionRequest req) {
        return Query.of(q -> q.script(s -> s
                .script(sc -> sc
                        .inline(i -> i
                                .lang(l -> l.builtin(BuiltinScriptLanguage.Painless))
                                .source("String val = doc[params.field].value.toString(); return val.startsWith(params.input);")
                                .params(Map.of(
                                        "field", JsonData.of(req.field()),
                                        "input", JsonData.of(req.input())
                                ))
                        )
                )
        ));
    }

    private SuggestionError extractError(OpenSearchException exception) {
        final ErrorCause error = exception.response().error();
        String type = "Aggregation error";
        String reason = exception.getMessage();
        if (error != null) {
            final ErrorCause cause = error.rootCause().stream().findFirst().orElse(error);
            type = cause.type();
            if (cause.reason() != null) {
                reason = cause.reason();
            }
        }
        return SuggestionError.create(type, reason);
    }
}
