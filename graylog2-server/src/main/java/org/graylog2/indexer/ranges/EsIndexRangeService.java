/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.ranges;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.esplugin.IndexChangeMonitor;
import org.graylog2.indexer.esplugin.IndicesDeletedEvent;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.metrics.MetricUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class EsIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(EsIndexRangeService.class);

    private final LoadingCache<String, IndexRange> cache;
    private final Client client;
    private final ObjectMapper objectMapper;
    private final Indices indices;
    private final Deflector deflector;
    private final EventBus clusterEventBus;


    @Inject
    public EsIndexRangeService(Client client,
                               ObjectMapper objectMapper,
                               Indices indices,
                               Deflector deflector,
                               EventBus eventBus,
                               @ClusterEventBus EventBus clusterEventBus,
                               MetricRegistry metricRegistry) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.indices = indices;
        this.deflector = deflector;
        this.clusterEventBus = clusterEventBus;

        final CacheLoader<String, IndexRange> cacheLoader = new CacheLoader<String, IndexRange>() {
            @Override
            public IndexRange load(String indexName) throws Exception {
                final IndexRange indexRange = loadIndexRange(indexName);

                if (indexRange == null) {
                    throw new NotFoundException("Couldn't load index range for index " + indexName);
                }

                return indexRange;
            }
        };
        this.cache = CacheBuilder.<String, IndexRange>newBuilder()
                .recordStats()
                .build(cacheLoader);

        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(MetricRegistry.name(this.getClass(), "cache"), cache));

        // This sucks. We need to bridge Elasticsearch's and our own Guice injector.
        IndexChangeMonitor.setEventBus(eventBus);
        eventBus.register(this);
        clusterEventBus.register(this);
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        try {
            return cache.get(index);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof NotFoundException) {
                throw (NotFoundException) cause;
            } else {
                throw new NotFoundException(e.getCause());
            }
        }
    }

    private IndexRange loadIndexRange(String index) throws NotFoundException {
        final GetRequest request = new GetRequestBuilder(client, index)
                .setType(IndexMapping.TYPE_INDEX_RANGE)
                .setId(index)
                .request();

        final GetResponse r;
        try {
            r = client.get(request).actionGet();
        } catch (IndexMissingException | NoShardAvailableActionException e) {
            throw new NotFoundException(e);
        }

        if (!r.isExists()) {
            throw new NotFoundException("Index [" + index + "] not found.");
        }

        final IndexRange indexRange = parseSource(r.getIndex(), r.getSource());
        if (indexRange == null) {
            throw new NotFoundException("Index [" + index + "] not found.");
        }

        return indexRange;
    }

    @Nullable
    private IndexRange parseSource(String index, Map<String, Object> fields) {
        try {
            return IndexRange.create(
                    index,
                    parseFromDateString((String) fields.get(IndexRange.FIELD_BEGIN)),
                    parseFromDateString((String) fields.get(IndexRange.FIELD_END)),
                    parseFromDateString((String) fields.get(IndexRange.FIELD_CALCULATED_AT)),
                    (int) fields.get(IndexRange.FIELD_TOOK_MS)
            );
        } catch (Exception e) {
            LOG.debug("Couldn't create index range from fields: " + fields);
            return null;
        }
    }

    private DateTime parseFromDateString(String s) {
        return DateTime.parse(s);
    }

    @Override
    public SortedSet<IndexRange> find(DateTime begin, DateTime end) {
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (IndexRange indexRange : findAll()) {
            if (indexRange.begin().getMillis() >= begin.getMillis() && indexRange.end().getMillis() <= end.getMillis()) {
                indexRanges.add(indexRange);
            }
        }

        return indexRanges.build();
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (String index : deflector.getAllDeflectorIndexNames()) {
            try {
                indexRanges.add(cache.get(index));
            } catch (ExecutionException e) {
                LOG.warn("Couldn't load index range for index " + index, e.getCause());
            }
        }

        return indexRanges.build();
    }

    @Override
    public IndexRange calculateRange(String index) {
        final Stopwatch sw = Stopwatch.createStarted();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TimestampStats stats = timestampStatsOfIndex(index);
        final int duration = Ints.saturatedCast(sw.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, duration);
        return IndexRange.create(index, stats.min(), stats.max(), now, duration);
    }

    /**
     * Calculate stats (min, max, avg) about the message timestamps in the given index.
     *
     * @param index Name of the index to query.
     * @return the timestamp stats in the given index, or {@code null} if they couldn't be calculated.
     * @see org.elasticsearch.search.aggregations.metrics.stats.Stats
     */
    @VisibleForTesting
    protected TimestampStats timestampStatsOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg")
                .filter(FilterBuilders.existsFilter("timestamp"))
                .subAggregation(AggregationBuilders.stats("ts_stats").field("timestamp"));
        final SearchRequestBuilder srb = client.prepareSearch()
                .setIndices(index)
                .setSearchType(SearchType.COUNT)
                .addAggregation(builder);

        final SearchResponse response;
        try {
            response = client.search(srb.request()).actionGet();
        } catch (IndexMissingException e) {
            throw e;
        } catch (ElasticsearchException e) {
            LOG.error("Error while calculating timestamp stats in index <" + index + ">", e);
            throw new IndexMissingException(new Index(index));
        }

        final Filter f = response.getAggregations().get("agg");
        if (f.getDocCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return TimestampStats.EMPTY;
        }

        final Stats stats = f.getAggregations().get("ts_stats");
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT).withZoneUTC();
        final DateTime min = formatter.parseDateTime(stats.getMinAsString());
        final DateTime max = formatter.parseDateTime(stats.getMaxAsString());
        final DateTime avg = formatter.parseDateTime(stats.getAvgAsString());

        return TimestampStats.create(min, max, avg);
    }

    @Override
    public void save(IndexRange indexRange) {
        final byte[] source;
        try {
            source = objectMapper.writeValueAsBytes(indexRange);
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }

        final String indexName = indexRange.indexName();
        final boolean readOnly = indices.isReadOnly(indexName);

        if (readOnly) {
            indices.setReadWrite(indexName);
        }

        final IndexRequest request = client.prepareIndex()
                .setIndex(indexName)
                .setType(IndexMapping.TYPE_INDEX_RANGE)
                .setId(indexName)
                .setSource(source)
                .request();
        final IndexResponse response = client.index(request).actionGet();

        if (readOnly) {
            indices.setReadOnly(indexName);
        }

        if (response.isCreated()) {
            LOG.debug("Successfully saved index range: {}", indexRange);
        } else {
            LOG.debug("Successfully updated index range: {}", indexRange);
        }

        cache.put(indexName, indexRange);
        clusterEventBus.post(IndexRangeUpdatedEvent.create(indexName));
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexDeletion(IndicesDeletedEvent event) {
        for (String index : event.indices()) {
            cache.invalidate(index);
        }

        cache.cleanUp();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexRangeUpdate(IndexRangeUpdatedEvent event) {
        cache.refresh(event.indexName());
    }
}