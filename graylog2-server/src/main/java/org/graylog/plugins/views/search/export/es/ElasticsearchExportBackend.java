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
import org.graylog.plugins.views.search.export.ChunkedResult;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
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
    public ChunkedResult run(MessagesRequest request, Consumer<LinkedHashSet<LinkedHashSet<String>>> chunkCollector) {
        request.ensureCompleteness();

        getChunksRecursively(request, chunkCollector, null);

        return new ChunkedResult();
    }

    private void getChunksRecursively(MessagesRequest request, Consumer<LinkedHashSet<LinkedHashSet<String>>> chunkCollector, Object[] searchAfterValues) {

        Search search = buildSearchRequest(request, searchAfterValues);

        SearchResult result = JestUtils.execute(jestClient, search, () -> "");

        List<SearchResult.Hit<Map, Void>> hits = result.getHits(Map.class, false);

        if (hits.isEmpty())
            return;

        publishChunk(chunkCollector, hits, request.fieldsInOrder().get());

        Object[] lastHitSort = lastHitSortFrom(hits);

        getChunksRecursively(request, chunkCollector, lastHitSort);
    }

    private Search buildSearchRequest(MessagesRequest request, Object[] searchAfterValues) {
        Set<String> indices = indicesFor(request);
        QueryBuilder query = queryFrom(request);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .size(CHUNK_SIZE);

        for (Sort sort : request.sort().get())
            ssb.sort(SortBuilders.fieldSort(sort.field()).order(sort.order()));

        ssb.sort(SortBuilders.fieldSort(TIEBREAKER_FIELD).order(SortOrder.ASC).unmappedType("string"));

        if (searchAfterValues != null)
            ssb.searchAfter(searchAfterValues);

        return new Search.Builder(ssb.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .allowNoIndices(false)
                .ignoreUnavailable(false)
                .addIndex(indices)
                .build();
    }

    private Object[] lastHitSortFrom(List<SearchResult.Hit<Map, Void>> hits) {
        SearchResult.Hit<Map, Void> lastHit = hits.get(hits.size() - 1);

        return lastHit.sort.toArray(new Object[0]);
    }

    private void publishChunk(Consumer<LinkedHashSet<LinkedHashSet<String>>> chunkCollector, List<SearchResult.Hit<Map, Void>> hits, Set<String> desiredFieldsInOrder) {
        LinkedHashSet<LinkedHashSet<String>> hitsWithOnlyRelevantFields = buildHitsWithRelevantFields(hits, desiredFieldsInOrder);

        chunkCollector.accept(hitsWithOnlyRelevantFields);
    }

    private LinkedHashSet<LinkedHashSet<String>> buildHitsWithRelevantFields(List<SearchResult.Hit<Map, Void>> hits, Set<String> desiredFieldsInOrder) {
        return hits.stream()
                .map(h -> buildHitWithRelevantFields(h, desiredFieldsInOrder))
                .collect(toCollection(LinkedHashSet::new));
    }

    private LinkedHashSet<String> buildHitWithRelevantFields(SearchResult.Hit<Map, Void> hit, Set<String> desiredFieldsInOrder) {
        return desiredFieldsInOrder.stream()
                .map(f -> (String) hit.source.get(f))
                .collect(toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public Set<String> indicesFor(MessagesRequest request) {
        return indexLookup.indexNamesForStreamsInTimeRange(request.streams().get(), request.timeRange().get());
    }

    private QueryBuilder queryFrom(MessagesRequest request) {
        ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) request.queryString().get();

        final QueryBuilder query = backendQuery.isEmpty() ?
                matchAllQuery() :
                queryStringQuery(backendQuery.queryString());//.allowLeadingWildcard(allowLeadingWildcardSearches);

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(request.timeRange().get())));
//TODO: find out, if we need the extra filter for dashboard widgets?
//        if (!isNullOrEmpty(filterString)) {
//            filter.filter(queryStringQuery(filterString));
//        }

        if (!request.streams().get().isEmpty())
            filter.filter(termsQuery(Message.FIELD_STREAMS, request.streams().get()));

        return filter;
    }
}
