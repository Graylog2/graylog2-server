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

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
public class Counts {
    private static final Logger LOG = LoggerFactory.getLogger(Counts.class);

    private final JestClient jestClient;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public Counts(JestClient jestClient, IndexSetRegistry indexSetRegistry) {
        this.jestClient = jestClient;
        this.indexSetRegistry = indexSetRegistry;
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
                .toString();
        final Search request = new Search.Builder(query)
                .addIndex(indices)
                .build();
        final MultiSearch multiSearch = new MultiSearch.Builder(request).build();
        final MultiSearchResult searchResult = JestUtils.execute(jestClient, multiSearch, () -> "Fetching message count failed for indices " + indices);
        final JsonNode responses = searchResult.getJsonObject().path("responses");
        if (!responses.isArray()) {
            LOG.debug("\"responses\" array not found: {}", responses.toString());
            throw new ElasticsearchException("Expected list of results not found");
        }

        if (responses.size() != 1) {
            throw new ElasticsearchException("Expected list of results to contain exactly 1 item, actual size: " + responses.size());
        }

        return responses.path(0).path("hits").path("total").asLong(-1L);
    }
}
