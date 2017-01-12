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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Counts {
    private final Client c;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public Counts(Client client, IndexSetRegistry indexSetRegistry) {
        this.c = client;
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

        try {
            return c.prepareSearch(indexNames)
                    .setSize(0)
                    .get()
                    .getHits()
                    .getTotalHits();
        } catch (ElasticsearchException e) {
            return -1L;
        }
    }
}
