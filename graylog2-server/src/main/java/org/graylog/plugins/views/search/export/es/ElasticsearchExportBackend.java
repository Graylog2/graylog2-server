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
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ChunkedResult;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class ElasticsearchExportBackend implements ExportBackend {

    private final JestClient jestClient;
    private final IndexLookup indexLookup;

    @Inject
    public ElasticsearchExportBackend(JestClient jestClient, IndexLookup indexLookup) {
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;
    }

    @Override
    public ChunkedResult run(MessagesRequest request) {
        request.ensureCompleteness();

        Set<String> indices = indicesFor(request);
        QueryBuilder query = queryFrom(request);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(query);//.searchAfter(null);
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .allowNoIndices(false)
                .ignoreUnavailable(false)
                .addIndex(indices)
                .build();
        SearchResult execute = JestUtils.execute(jestClient, search, null);

        return new ChunkedResult();
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
                .filter(termsQuery(Message.FIELD_STREAMS, request.streams().get()))
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(request.timeRange().get())));
//TODO: find out, if we need the extra filter for dashboard widgets?
//        if (!isNullOrEmpty(filterString)) {
//            filter.filter(queryStringQuery(filterString));
//        }

        return filter;
    }
}
