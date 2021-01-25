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
package org.graylog2.indexer.searches;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedSet;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.timeranges.TimeRanges;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@Singleton
public class Searches {
    private static final Pattern filterStreamIdPattern = Pattern.compile("^(.+[^\\p{Alnum}])?streams:([\\p{XDigit}]+)");

    private final IndexRangeService indexRangeService;
    private final Timer esRequestTimer;
    private final Histogram esTimeRangeHistogram;
    private final Counter esTotalSearchesCounter;
    private final StreamService streamService;
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final SearchesAdapter searchesAdapter;

    @Inject
    public Searches(IndexRangeService indexRangeService,
                    MetricRegistry metricRegistry,
                    StreamService streamService,
                    Indices indices,
                    IndexSetRegistry indexSetRegistry,
                    SearchesAdapter searchesAdapter) {
        this.indexRangeService = requireNonNull(indexRangeService, "indexRangeService");

        this.esRequestTimer = metricRegistry.timer(name(Searches.class, "elasticsearch", "requests"));
        this.esTimeRangeHistogram = metricRegistry.histogram(name(Searches.class, "elasticsearch", "ranges"));
        this.esTotalSearchesCounter = metricRegistry.counter(name(Searches.class, "elasticsearch", "total-searches"));
        this.streamService = requireNonNull(streamService, "streamService");
        this.indices = requireNonNull(indices, "indices");
        this.indexSetRegistry = requireNonNull(indexSetRegistry, "indexSetRegistry");
        this.searchesAdapter = searchesAdapter;
    }

    public CountResult count(String query, TimeRange range) {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) {
        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return CountResult.empty();
        }

        final CountResult result = searchesAdapter.count(affectedIndices, query, range, filter);

        recordEsMetrics(result.tookMs(), range);

        return result;
    }

    public ScrollResult scroll(String query, TimeRange range, int limit, int offset, List<String> fields, String filter, int batchSize) {
        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        final Set<String> indexWildcards = indexSetRegistry.getForIndices(affectedIndices).stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());

        final Sorting sorting = new Sorting("_doc", Sorting.Direction.ASC);

        ScrollCommand.Builder scrollCommandBuilder = ScrollCommand.builder()
                .query(query)
                .range(range)
                .offset(offset)
                .fields(fields)
                .filter(filter)
                .sorting(sorting)
                .indices(indexWildcards);

        scrollCommandBuilder = limit != ScrollCommand.NO_LIMIT ? scrollCommandBuilder.limit(limit) : scrollCommandBuilder;
        scrollCommandBuilder = batchSize != ScrollCommand.NO_BATCHSIZE ? scrollCommandBuilder.batchSize(batchSize) : scrollCommandBuilder;

        final ScrollResult result = searchesAdapter.scroll(scrollCommandBuilder.build());

        recordEsMetrics(result.tookMs(), range);

        return result;
    }

    public SearchResult search(String query, TimeRange range, int limit, int offset, Sorting sorting) {
        return search(query, null, range, limit, offset, sorting);
    }

    public SearchResult search(String query, String filter, TimeRange range, int limit, int offset, Sorting sorting) {
        final SearchesConfig searchesConfig = SearchesConfig.builder()
                .query(query)
                .filter(filter)
                .range(range)
                .limit(limit)
                .offset(offset)
                .sorting(sorting)
                .build();

        return search(searchesConfig);
    }

    public SearchResult search(SearchesConfig config) {
        final Set<IndexRange> indexRanges = determineAffectedIndicesWithRanges(config.range(), config.filter());

        final Set<String> indices = extractIndexNamesFromIndexRanges(indexRanges);
        final SearchResult result = searchesAdapter.search(indices, indexRanges, config);
        recordEsMetrics(result.tookMs(), config.range());
        return result;
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) {
        return fieldStats(field, query, null, range, true, true, true);
    }

    public FieldStatsResult fieldStats(String field,
                                       String query,
                                       String filter,
                                       TimeRange range,
                                       boolean includeCardinality,
                                       boolean includeStats,
                                       boolean includeCount) {
        final Set<String> indices = indicesContainingField(determineAffectedIndices(range, filter), field);

        final FieldStatsResult result = searchesAdapter.fieldStats(query, filter, range, indices, field, includeCardinality, includeStats, includeCount);

        recordEsMetrics(result.tookMs(), range);
        return result;
    }

    private Set<String> indicesContainingField(Set<String> strings, String field) {
        return indices.getAllMessageFieldsForIndices(strings.toArray(new String[0]))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().contains(field))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private void recordEsMetrics(long tookMs, TimeRange range) {
        esTotalSearchesCounter.inc();

        esRequestTimer.update(tookMs, TimeUnit.MILLISECONDS);

        if (range != null) {
            esTimeRangeHistogram.update(TimeRanges.toSeconds(range));
        }
    }

    /**
     * Extracts the last stream id from the filter string passed as part of the elasticsearch query. This is used later
     * to pass to possibly existing message decorators for stream-specific configurations.
     *
     * The assumption is that usually (when listing/searching messages for a stream) only a single stream filter is passed.
     * When this is not the case, only the last stream id will be taked into account.
     *
     * This is currently a workaround. A better solution would be to pass the stream id which is supposed to be the scope
     * for a search query as a separate parameter.
     *
     * @param filter the filter string like "streams:xxxyyyzzz"
     * @return the optional stream id
     */
    public static Optional<String> extractStreamId(String filter) {
        if (isNullOrEmpty(filter)) {
            return Optional.empty();
        }
        final Matcher streamIdMatcher = filterStreamIdPattern.matcher(filter);
        if (streamIdMatcher.find()) {
            return Optional.of(streamIdMatcher.group(2));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    Set<String> determineAffectedIndices(TimeRange range, @Nullable String filter) {
        return extractIndexNamesFromIndexRanges(determineAffectedIndicesWithRanges(range, filter));
    }

    private Set<String> extractIndexNamesFromIndexRanges(Set<IndexRange> indexRanges) {
        return indexRanges.stream()
                .map(IndexRange::indexName)
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    Set<IndexRange> determineAffectedIndicesWithRanges(TimeRange range, @Nullable String filter) {
        final Optional<String> streamId = extractStreamId(filter);
        IndexSet indexSet = null;
        // if we are searching in a stream, we are further restricting the indices using the currently
        // configure index set of that stream.
        // later on we will also test against each index range (we load all of them) to see if there are
        // additional index ranges that match, this can happen with restored archives or when the index set for
        // a stream has changed: a stream only knows about its currently configured index set, no the history
        if (streamId.isPresent()) {
            try {
                final Stream stream = streamService.load(streamId.get());
                indexSet = stream.getIndexSet();
            } catch (NotFoundException ignored) {
            }
        }

        final ImmutableSortedSet.Builder<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(range.getFrom(), range.getTo());
        final Set<String> affectedIndexNames = indexRanges.stream().map(IndexRange::indexName).collect(Collectors.toSet());
        final Set<IndexSet> eventIndexSets = indexSetRegistry.getForIndices(affectedIndexNames).stream()
                .filter(indexSet1 -> IndexSetConfig.TemplateType.EVENTS.equals(indexSet1.getConfig().indexTemplateType().orElse(IndexSetConfig.TemplateType.MESSAGES)))
                .collect(Collectors.toSet());
        for (IndexRange indexRange : indexRanges) {
            // if we aren't in a stream search, we look at all the ranges matching the time range.
            if (indexSet == null && filter == null) {
                // Don't include the index range if it's for an event index set to avoid sorting issues.
                // See the following issues for details:
                // - https://github.com/Graylog2/graylog2-server/issues/6384
                // - https://github.com/Graylog2/graylog2-server/issues/6490
                if (eventIndexSets.stream().anyMatch(set -> set.isManagedIndex(indexRange.indexName()))) {
                    continue;
                }
                indices.add(indexRange);
                continue;
            }

            // A range applies to this search if either: the current index set of the stream matches or a previous index set matched.
            final boolean streamInIndexRange = streamId.isPresent() && indexRange.streamIds() != null && indexRange.streamIds().contains(streamId.get());
            final boolean streamInCurrentIndexSet = indexSet != null && indexSet.isManagedIndex(indexRange.indexName());

            if (streamInIndexRange) {
                indices.add(indexRange);
            }
            if (streamInCurrentIndexSet) {
                indices.add(indexRange);
            }
        }

        return indices.build();
    }
}
