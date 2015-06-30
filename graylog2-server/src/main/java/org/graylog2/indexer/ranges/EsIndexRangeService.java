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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Ints;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

public class EsIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(EsIndexRangeService.class);

    private final Client client;
    private final ActivityWriter activityWriter;
    private final Searches searches;
    private final ObjectMapper objectMapper;

    @Inject
    public EsIndexRangeService(Client client, ActivityWriter activityWriter, Searches searches, ObjectMapper objectMapper) {
        this.client = client;
        this.activityWriter = activityWriter;
        this.searches = searches;
        this.objectMapper = objectMapper;
    }

    @Override
    @Nullable
    public IndexRange get(String index) throws NotFoundException {
        final GetRequest request = new GetRequestBuilder(client, index)
                .setType(IndexMapping.TYPE_META)
                .setId(index)
                .request();

        final GetResponse r;
        try {
            r = client.get(request).actionGet();
        } catch (NoShardAvailableActionException e) {
            throw new NotFoundException(e);
        }

        if (!r.isExists()) {
            throw new NotFoundException("Index [" + index + "] not found.");
        }

        return parseSource(r.getIndex(), r.getSource());
    }

    @Nullable
    private IndexRange parseSource(String index, Map<String, Object> fields) {
        try {
            return IndexRange.create(
                    index,
                    parseFromDateString((String) fields.get("begin")),
                    parseFromDateString((String) fields.get("end")),
                    parseFromDateString((String) fields.get("calculated_at")),
                    (int) fields.get("took_ms")
            );
        } catch (Exception e) {
            return null;
        }
    }

    private DateTime parseFromDateString(String s) {
        return DateTime.parse(s);
    }

    @Override
    public SortedSet<IndexRange> find(DateTime begin, DateTime end) {
        final RangeQueryBuilder beginRangeQuery = QueryBuilders.rangeQuery("begin").gte(begin.getMillis());
        final RangeQueryBuilder endRangeQuery = QueryBuilders.rangeQuery("end").lte(end.getMillis());
        final BoolQueryBuilder completeRangeQuery = QueryBuilders.boolQuery()
                .must(beginRangeQuery)
                .must(endRangeQuery);
        final SearchRequest request = client.prepareSearch()
                .setTypes(IndexMapping.TYPE_META)
                .setQuery(completeRangeQuery)
                .request();

        final SearchResponse response = client.search(request).actionGet();
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (SearchHit searchHit : response.getHits()) {
            final IndexRange indexRange = parseSource(searchHit.getIndex(), searchHit.getSource());
            if (indexRange != null) {
                indexRanges.add(indexRange);
            }
        }

        return indexRanges.build();
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        final SearchRequest request = client.prepareSearch()
                .setTypes(IndexMapping.TYPE_META)
                .setQuery(QueryBuilders.matchAllQuery())
                .request();

        final SearchResponse response = client.search(request).actionGet();
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (SearchHit searchHit : response.getHits()) {
            final IndexRange indexRange = parseSource(searchHit.getIndex(), searchHit.getSource());
            if (indexRange != null) {
                indexRanges.add(indexRange);
            }
        }

        return indexRanges.build();
    }

    @Override
    public void destroy(String index) {
        final DeleteRequest request = client.prepareDelete()
                .setIndex(index)
                .setId(index)
                .setType(IndexMapping.TYPE_META)
                .setRefresh(true)
                .request();
        final DeleteResponse response = client.delete(request).actionGet();

        if (response.isFound()) {
            String msg = "Removed range meta-information of [" + index + "]";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRange.class));
        } else {
            LOG.warn("Couldn't find meta-information of index [{}]", index);
        }
    }

    @Override
    public void destroyAll() {
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        final SearchRequest searchRequest = client.prepareSearch()
                .setTypes(IndexMapping.TYPE_META)
                .setSearchType(SearchType.SCAN)
                .setScroll(scroll)
                .setQuery(QueryBuilders.matchAllQuery())
                .request();
        final SearchResponse searchResponse = client.search(searchRequest).actionGet();

        final SearchScrollRequest scrollRequest = client.prepareSearchScroll(searchResponse.getScrollId())
                .setScroll(scroll)
                .request();
        final SearchResponse scrollResponse = client.searchScroll(scrollRequest).actionGet();

        final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (SearchHit hit : scrollResponse.getHits().hits()) {
            final DeleteRequest deleteRequest = client.prepareDelete(hit.index(), hit.type(), hit.id()).request();
            bulkRequestBuilder.add(deleteRequest);
        }

        final BulkRequest bulkRequest = bulkRequestBuilder.request();
        final BulkResponse bulkResponse = client.bulk(bulkRequest).actionGet();

        if (bulkResponse.hasFailures()) {
            LOG.warn("Couldn't remove meta-information of all indices");
            LOG.debug("Bulk delete error details: {}", bulkResponse.buildFailureMessage());
        } else {
            String msg = "Removed range meta-information of all indices";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRange.class));
        }
    }

    @Override
    public IndexRange calculateRange(String index) {
        final Stopwatch sw = Stopwatch.createStarted();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TimestampStats stats = firstNonNull(
                searches.timestampStatsOfIndex(index),
                TimestampStats.create(new DateTime(0L, DateTimeZone.UTC), now, new DateTime(0L, DateTimeZone.UTC))
        );
        final int duration = Ints.saturatedCast(sw.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, duration);
        return IndexRange.create(index, stats.min(), stats.max(), now, duration);
    }

    @Override
    public void save(IndexRange indexRange) {
        final byte[] source;
        try {
            source = objectMapper.writeValueAsBytes(indexRange);
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }

        final IndexRequest request = client.prepareIndex()
                .setIndex(indexRange.indexName())
                .setType(IndexMapping.TYPE_META)
                .setId(indexRange.indexName())
                .setRefresh(true)
                .setSource(source)
                .request();
        final IndexResponse response = client.index(request).actionGet();
        if (response.isCreated()) {
            LOG.debug("Successfully saved index range {}", indexRange);
        } else {
            LOG.warn("Couldn't safe index range for index [{}]: {}", indexRange.indexName(), indexRange);
        }
    }
}