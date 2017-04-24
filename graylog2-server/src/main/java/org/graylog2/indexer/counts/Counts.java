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

import io.searchbox.client.JestClient;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;

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

        final String query = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery().buildAsBytes(XContentType.JSON))
                .toString();
        final Count request = new Count.Builder()
                .query(query)
                .addIndex(Arrays.asList(indexNames))
                .build();
        final CountResult result;
        try {
            result = jestClient.execute(request);
        } catch (IOException e) {
            LOG.error("Couldn't fetch message count", e);
            return -1L;
        }

        if (result.isSucceeded()) {
            return result.getCount().longValue();
        } else {
            LOG.error("Fetching message count failed: {}", result.getErrorMessage());
            return -1L;
        }
    }
}
