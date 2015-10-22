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
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.esplugin.IndicesDeletedEvent;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.shared.metrics.MetricUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@Singleton
@Deprecated
public class EsIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(EsIndexRangeService.class);

    private final LoadingCache<String, IndexRange> cache;
    private final Client client;
    private final Deflector deflector;
    private final Indices indices;

    @Inject
    public EsIndexRangeService(Client client,
                               Deflector deflector,
                               Indices indices,
                               EventBus eventBus,
                               @ClusterEventBus EventBus clusterEventBus,
                               MetricRegistry metricRegistry) {
        this.client = requireNonNull(client);
        this.deflector = requireNonNull(deflector);
        this.indices = requireNonNull(indices);

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
                .setType("index_range")
                .setId(index)
                .request();

        final GetResponse r;
        try {
            r = client.get(request).actionGet();
        } catch (IndexMissingException | NoShardAvailableActionException e) {
            throw new NotFoundException(e);
        }

        if (!r.isExists()) {
            throw new NotFoundException("Couldn't find index range for index " + index);
        }

        final IndexRange indexRange = parseSource(r.getIndex(), r.getSource());
        if (indexRange == null) {
            throw new NotFoundException("Couldn't parse index range for index " + index);
        }

        return indexRange;
    }

    @Nullable
    private IndexRange parseSource(String index, Map<String, Object> fields) {
        try {
            return EsIndexRange.create(
                    index,
                    parseFromDateString((String) fields.get(EsIndexRange.FIELD_BEGIN)),
                    parseFromDateString((String) fields.get(EsIndexRange.FIELD_END)),
                    parseFromDateString((String) fields.get(EsIndexRange.FIELD_CALCULATED_AT)),
                    (int) fields.get(EsIndexRange.FIELD_TOOK_MS)
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
            if (indexRange.begin().getMillis() <= end.getMillis() && indexRange.end().getMillis() >= begin.getMillis()) {
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
                final Throwable cause = e.getCause();
                if (cause instanceof NotFoundException) {
                    LOG.debug("Couldn't find index range for index " + index);
                } else {
                    LOG.warn("Couldn't load index range for index " + index, cause);
                }
            }
        }

        return indexRanges.build();
    }

    @Override
    public IndexRange calculateRange(String index) {
        final Stopwatch sw = Stopwatch.createStarted();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TimestampStats stats = indices.timestampStatsOfIndex(index);
        final int duration = Ints.saturatedCast(sw.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, duration);
        return EsIndexRange.create(index, stats.min(), stats.max(), now, duration);
    }

    @Override
    public void save(IndexRange indexRange) {
        throw new UnsupportedOperationException();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexDeletion(IndicesDeletedEvent event) {
        for (String index : event.indices()) {
            cache.invalidate(index);
        }

        cache.cleanUp();
    }
}