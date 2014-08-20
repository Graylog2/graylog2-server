/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings.providers;

import com.ning.http.client.AsyncHttpClient;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.searches.Searches;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class IndexerProvider implements Provider<Indexer> {
    private static Indexer indexer = null;

    @Inject
    public IndexerProvider(Configuration configuration,
                           Searches.Factory searchesFactory,
                           Counts.Factory countsFactory,
                           Cluster.Factory clusterFactory,
                           Indices.Factory indicesFactory,
                           AsyncHttpClient httpClient) {
        if (indexer == null) {
            indexer = new Indexer(configuration,
                    searchesFactory,
                    countsFactory,
                    clusterFactory,
                    indicesFactory,
                    httpClient);
        }
    }

    @Override
    public Indexer get() {
        return indexer;
    }
}
