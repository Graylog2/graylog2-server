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
package org.graylog2.indexer.counts;

import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.jest.JestUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class Counts {
    private final JestClient jestClient;
    private final IndexSetRegistry indexSetRegistry;
    private final Duration esRequestTimeout;

    @Inject
    public Counts(JestClient jestClient, IndexSetRegistry indexSetRegistry, @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.jestClient = jestClient;
        this.indexSetRegistry = indexSetRegistry;
        this.esRequestTimeout = requestTimeout;
    }

    public long total() {
        return totalCount(indexSetRegistry.getManagedIndices());
    }

    public long total(final IndexSet indexSet) {
        return totalCount(indexSet.getManagedIndices());
    }

    private long totalCount(final String[] indexNames) {
        // Return 0 if there are no indices in the given index set. If we run the query with an empty index list,
        // Elasticsearch will count all documents in all indices and thus return a wrong count.
        if (indexNames.length == 0) {
            return 0L;
        }

        final List<String> indices = Arrays.asList(indexNames);
        final String query = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(0)
                .timeout(new TimeValue(esRequestTimeout.toMilliseconds(), TimeUnit.MILLISECONDS))
                .toString();
        final Search request = new Search.Builder(query)
                .addIndex(indices)
                .build();
        final MultiSearch multiSearch = new MultiSearch.Builder(request).build();
        final MultiSearchResult searchResult = JestUtils.execute(jestClient, multiSearch, () -> "Fetching message count failed for indices " + indices);
        final List<MultiSearchResult.MultiSearchResponse> responses = searchResult.getResponses();

        long total = 0L;
        for (MultiSearchResult.MultiSearchResponse response : responses) {
            if (response.isError) {
                throw JestUtils.specificException(() -> "Fetching message count failed for indices " + indices, response.error);
            }
            total += response.searchResult.getTotal();
        }

        return total;
    }
}
