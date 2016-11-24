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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Client;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

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
        final SearchRequest request = c.prepareSearch(indexSetRegistry.getManagedIndicesNames())
                .setSize(0)
                .request();
        return c.search(request).actionGet().getHits().totalHits();
    }

    public long total(final IndexSetConfig indexSetConfig) {
        final Optional<IndexSet> indexSet = indexSetRegistry.get(indexSetConfig.id());

        final String[] names = indexSet.orElseThrow(() -> new IllegalStateException("Index set <" + indexSetConfig.id() + "> missing"))
                .getManagedIndicesNames();

        final SearchRequest request = c.prepareSearch(names)
                .setSize(0)
                .request();
        return c.search(request).actionGet().getHits().totalHits();
    }
}
