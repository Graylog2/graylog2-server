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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Ints;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class EsIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(EsIndexRangeService.class);

    private final Client client;
    private final ObjectMapper objectMapper;
    private final Indices indices;

    @Inject
    public EsIndexRangeService(Client client, ObjectMapper objectMapper, Indices indices) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.indices = indices;
    }

    @Override
    @Nullable
    public IndexRange get(String index) throws NotFoundException {
        final GetRequest request = new GetRequestBuilder(client, indices.getMetaIndexName())
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
            throw new NotFoundException("Metadata for index [" + index + "] not found.");
        }

        return parseSource(r.getSource());
    }

    @Nullable
    private IndexRange parseSource(Map<String, Object> fields) {
        try {
            return IndexRange.create(
                    (String) fields.get("index_name"),
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
                .setTypes(IndexMapping.TYPE_INDEX_RANGE)
                .setIndices(indices.getMetaIndexName())
                .setQuery(completeRangeQuery)
                .request();

        final SearchResponse response = client.search(request).actionGet();
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (SearchHit searchHit : response.getHits()) {
            final IndexRange indexRange = parseSource(searchHit.getSource());
            if (indexRange != null) {
                indexRanges.add(indexRange);
            }
        }

        return indexRanges.build();
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        final SearchRequest request = client.prepareSearch()
                .setTypes(IndexMapping.TYPE_INDEX_RANGE)
                .setIndices(indices.getMetaIndexName())
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(QueryBuilders.matchAllQuery())
                .request();

        final SearchResponse response = client.search(request).actionGet();
        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (SearchHit searchHit : response.getHits()) {
            final IndexRange indexRange = parseSource(searchHit.getSource());
            if (indexRange != null) {
                indexRanges.add(indexRange);
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

        LOG.info("Calculated range of index [{}] in [{}ms].", index, duration);
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
        final IndexRequest request = client.prepareIndex()
                .setIndex(indices.getMetaIndexName())
                .setType(IndexMapping.TYPE_INDEX_RANGE)
                .setId(indexName)
                .setRefresh(true)
                .setSource(source)
                .request();
        final IndexResponse response = client.index(request).actionGet();

        if (response.isCreated()) {
            LOG.debug("Successfully saved index range: {}", indexRange);
        } else {
            LOG.debug("Successfully updated index range: {}", indexRange);
        }
    }

    @Override
    public boolean delete(String index) {
        final DeleteRequest request = client.prepareDelete()
                .setIndex(indices.getMetaIndexName())
                .setType(IndexMapping.TYPE_INDEX_RANGE)
                .setId(index)
                .setRefresh(true)
                .request();
        final DeleteResponse response = client.delete(request).actionGet();

        return response.isFound();
    }
}