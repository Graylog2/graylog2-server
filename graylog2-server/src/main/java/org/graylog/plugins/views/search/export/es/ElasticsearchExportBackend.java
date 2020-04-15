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
package org.graylog.plugins.views.search.export.es;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.SimpleMessage;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class ElasticsearchExportBackend implements ExportBackend {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchExportBackend.class);

    private static final String TIEBREAKER_FIELD = "gl2_message_id";

    private final JestClient jestClient;
    private final IndexLookup indexLookup;

    @Inject
    public ElasticsearchExportBackend(JestClient jestClient, IndexLookup indexLookup) {
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;
    }

    @Override
    public void run(MessagesRequest request, Consumer<SimpleMessageChunk> chunkCollector) {
        fetchResults(request, chunkCollector);
    }

    private void fetchResults(MessagesRequest request, Consumer<SimpleMessageChunk> chunkCollector) {
        Object[] searchAfterValues = null;
        boolean isFirstChunk = true;

        while (true) {
            List<SearchResult.Hit<Map, Void>> hits = search(request, searchAfterValues);

            if (hits.isEmpty()) {
                return;
            }

            boolean success = publishChunk(chunkCollector, hits, request.fieldsInOrder(), isFirstChunk);
            if (!success) {
                return;
            }

            searchAfterValues = lastHitSortFrom(hits);
            isFirstChunk = false;
        }
    }

    private List<SearchResult.Hit<Map, Void>> search(MessagesRequest request, Object[] searchAfterValues) {
        Search search = buildSearchRequest(request, searchAfterValues);

        SearchResult result = JestUtils.execute(jestClient, search, () -> "Failed to execute Search After request");

        return result.getHits(Map.class, false);
    }

    private Search buildSearchRequest(MessagesRequest request, Object[] searchAfterValues) {
        SearchSourceBuilder ssb = searchSourceBuilderFrom(request, searchAfterValues);

        Set<String> indices = indicesFor(request);

        return new Search.Builder(ssb.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .allowNoIndices(false)
                .ignoreUnavailable(false)
                .addIndex(indices)
                .build();
    }

    private SearchSourceBuilder searchSourceBuilderFrom(MessagesRequest request, Object[] searchAfterValues) {
        QueryBuilder query = queryFrom(request);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .size(request.chunkSize());

        addSort(ssb, request.sort());

        if (searchAfterValues != null) {
            ssb.searchAfter(searchAfterValues);
        }
        return ssb;
    }

    private QueryBuilder queryFrom(MessagesRequest request) {
        ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) request.queryString();

        QueryBuilder query = backendQuery.isEmpty() ?
                matchAllQuery() :
                queryStringQuery(backendQuery.queryString());//.allowLeadingWildcard(allowLeadingWildcardSearches);

        BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(request.timeRange())));

        request.additionalQueryString().map(qs -> (ElasticsearchQueryString) qs)
                .ifPresent(qs -> filter.filter(queryStringQuery(qs.queryString())));

        filter.filter(termsQuery(Message.FIELD_STREAMS, request.streams()));

        return filter;
    }

    private void addSort(SearchSourceBuilder ssb, LinkedHashSet<Sort> sorts) {
        for (Sort sort : sorts) {
            ssb.sort(SortBuilders.fieldSort(sort.field()).order(sort.order()));
        }

        ssb.sort(SortBuilders.fieldSort(TIEBREAKER_FIELD).order(SortOrder.ASC).unmappedType("string"));
    }

    private Set<String> indicesFor(MessagesRequest request) {
        return indexLookup.indexNamesForStreamsInTimeRange(request.streams(), request.timeRange());
    }

    private boolean publishChunk(Consumer<SimpleMessageChunk> chunkCollector, List<SearchResult.Hit<Map, Void>> hits, LinkedHashSet<String> desiredFieldsInOrder, boolean isFirstChunk) {
        SimpleMessageChunk hitsWithOnlyRelevantFields = buildHitsWithRelevantFields(hits, desiredFieldsInOrder);

        if (isFirstChunk) {
            hitsWithOnlyRelevantFields = hitsWithOnlyRelevantFields.toBuilder().isFirstChunk(true).build();
        }

        try {
            chunkCollector.accept(hitsWithOnlyRelevantFields);
            return true;
        } catch (Exception e) {
            LOG.warn("Chunk publishing threw exception. Stopping search after queries", e);
            return false;
        }
    }

    private SimpleMessageChunk buildHitsWithRelevantFields(List<SearchResult.Hit<Map, Void>> hits, LinkedHashSet<String> desiredFieldsInOrder) {
        LinkedHashSet<SimpleMessage> set = hits.stream()
                .map(h -> buildHitWithRelevantFields(desiredFieldsInOrder, h.source, h.index))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(desiredFieldsInOrder, set);
    }

    private SimpleMessage buildHitWithRelevantFields(Set<String> desiredFieldsInOrder, Map source, String index) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        for (String name : desiredFieldsInOrder) {
            fields.put(name, source.get(name));
        }

        // _id is needed, because the old decorators relies on it
        fields.put("_id", UUID.randomUUID().toString());

        return SimpleMessage.from(index, fields);
    }

    private Object[] lastHitSortFrom(List<SearchResult.Hit<Map, Void>> hits) {
        SearchResult.Hit<Map, Void> lastHit = hits.get(hits.size() - 1);

        return lastHit.sort.toArray(new Object[0]);
    }
}
