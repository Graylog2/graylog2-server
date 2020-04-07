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
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class ElasticsearchExportBackend implements ExportBackend {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchExportBackend.class);

    private static final int CHUNK_SIZE = 1000;
    private static final String TIEBREAKER_FIELD = "gl2_message_id";

    private final JestClient jestClient;
    private final IndexLookup indexLookup;

    @Inject
    public ElasticsearchExportBackend(JestClient jestClient, IndexLookup indexLookup) {
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;
    }

    @Override
    public void run(MessagesRequest request, Consumer<LinkedHashSet<SimpleMessage>> chunkCollector, Runnable onDone) {
        request.ensureCompleteness();

        fetchResults(request, chunkCollector);

        onDone.run();
    }

    private void fetchResults(MessagesRequest request, Consumer<LinkedHashSet<SimpleMessage>> chunkCollector) {
        Object[] searchAfterValues = null;

        while (true) {
            List<SearchResult.Hit<Map, Void>> hits = search(request, searchAfterValues);

            if (hits.isEmpty()) {
                return;
            }

            boolean success = publishChunk(chunkCollector, hits, request.fieldsInOrder().get());
            if (!success) {
                return;
            }

            searchAfterValues = lastHitSortFrom(hits);
        }
    }

    private List<SearchResult.Hit<Map, Void>> search(MessagesRequest request, Object[] searchAfterValues) {
        Search search = buildSearchRequest(request, searchAfterValues);

        SearchResult result = JestUtils.execute(jestClient, search, () -> "");

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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private SearchSourceBuilder searchSourceBuilderFrom(MessagesRequest request, Object[] searchAfterValues) {
        QueryBuilder query = queryFrom(request);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .size(CHUNK_SIZE);

        addSort(ssb, request.sort().get());

        if (searchAfterValues != null) {
            ssb.searchAfter(searchAfterValues);
        }
        return ssb;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private QueryBuilder queryFrom(MessagesRequest request) {
        ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) request.queryString().get();

        QueryBuilder query = backendQuery.isEmpty() ?
                matchAllQuery() :
                queryStringQuery(backendQuery.queryString());//.allowLeadingWildcard(allowLeadingWildcardSearches);

        BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(request.timeRange().get())));

        request.additionalQueryString().map(qs -> (ElasticsearchQueryString) qs)
                .ifPresent(qs -> filter.filter(queryStringQuery(qs.queryString())));

        filter.filter(termsQuery(Message.FIELD_STREAMS, request.streams().get()));

        return filter;
    }

    private void addSort(SearchSourceBuilder ssb, LinkedHashSet<Sort> sorts) {
        for (Sort sort : sorts) {
            ssb.sort(SortBuilders.fieldSort(sort.field()).order(sort.order()));
        }

        ssb.sort(SortBuilders.fieldSort(TIEBREAKER_FIELD).order(SortOrder.ASC).unmappedType("string"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Set<String> indicesFor(MessagesRequest request) {
        return indexLookup.indexNamesForStreamsInTimeRange(request.streams().get(), request.timeRange().get());
    }

    private boolean publishChunk(Consumer<LinkedHashSet<SimpleMessage>> chunkCollector, List<SearchResult.Hit<Map, Void>> hits, Set<String> desiredFieldsInOrder) {
        LinkedHashSet<SimpleMessage> hitsWithOnlyRelevantFields = buildHitsWithRelevantFields(hits, desiredFieldsInOrder);

        try {
            chunkCollector.accept(hitsWithOnlyRelevantFields);
            return true;
        } catch (Exception e) {
            LOG.warn("Chunk publishing threw exception. Stopping search after queries", e);
            return false;
        }
    }

    private LinkedHashSet<SimpleMessage> buildHitsWithRelevantFields(List<SearchResult.Hit<Map, Void>> hits, Set<String> desiredFieldsInOrder) {
        return hits.stream()
                .map(h -> buildHitWithRelevantFields(h, desiredFieldsInOrder))
                .collect(toCollection(LinkedHashSet::new));
    }

    private SimpleMessage buildHitWithRelevantFields(SearchResult.Hit<Map, Void> hit, Set<String> desiredFieldsInOrder) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        for (String name : desiredFieldsInOrder) {
            fields.put(name, hit.source.get(name));
        }

        return SimpleMessage.from(fields);
    }

    private Object[] lastHitSortFrom(List<SearchResult.Hit<Map, Void>> hits) {
        SearchResult.Hit<Map, Void> lastHit = hits.get(hits.size() - 1);

        return lastHit.sort.toArray(new Object[0]);
    }
}
